package com.blankpages.trackify.performance

import com.blankpages.trackify.domain.PerformanceConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Koordinator Trackify Performance Governor (TPG).
 *
 * Satu objek menerima sinyal frame dan query, lalu menerbitkan satu anggaran
 * kerja. Tidak ada thread UI yang diblok, tidak ada timer, dan tidak ada
 * dependensi Android — sehingga seluruh logika bisa diuji di JVM.
 */
class TrackifyGovernor(
    private val tracker: FrameHealthTracker = FrameHealthTracker(),
    private val budgetPolicy: AdaptiveBudgetPolicy = AdaptiveBudgetPolicy,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    @Volatile
    private var config: PerformanceConfig? = null

    @Volatile
    var frameHealth: FrameHealth = FrameHealth.INITIAL
        private set

    @Volatile
    var budget: AdaptiveBudget = AdaptiveBudget.INITIAL
        private set

    private val listeners = CopyOnWriteArrayList<(AdaptiveBudget) -> Unit>()
    private val lastReported = ConcurrentHashMap<String, Long>()

    /** Profil perangkat dari Fase 10. Aman dipanggil berulang; hanya perubahan yang dipublikasi. */
    fun bind(newConfig: PerformanceConfig) {
        if (config == newConfig) return
        config = newConfig
        publish(budgetPolicy.budgetFor(newConfig, frameHealth))
    }

    /** Dipanggil FrameHealthMonitor. Mengembalikan kesehatan frame terbaru. */
    fun recordFrameDeltaNanos(deltaNanos: Long): FrameHealth {
        val previousGrade = frameHealth.grade
        val health = tracker.recordFrameDeltaNanos(deltaNanos)
        frameHealth = health
        if (health.grade != previousGrade) {
            config?.let { publish(budgetPolicy.budgetFor(it, health)) }
        }
        return health
    }

    /**
     * Dipanggil bila pemanggil mengukur durasi sendiri. Mengembalikan verdict
     * hanya untuk query yang belum dilaporkan dalam jendela [REPORT_WINDOW_MS].
     */
    fun recordQuery(sql: String, durationMs: Long): QueryVerdict? =
        report(QueryGuardPolicy.evaluate(QuerySignal(sql, durationMs), budget))

    /**
     * Dipanggil RoomQueryGuard. Tidak ada durasi dari `QueryCallback`, jadi yang
     * dinilai adalah bentuk query berisiko, bukan klaim waktu eksekusi.
     */
    fun recordQueryShape(sql: String): QueryVerdict? = report(QueryGuardPolicy.evaluateShape(sql))

    private fun report(verdict: QueryVerdict): QueryVerdict? {
        if (!verdict.slow) return null

        val now = clockMs()
        val previous = lastReported.putIfAbsent(verdict.signature, now)
        if (previous != null && now - previous < REPORT_WINDOW_MS) return null
        lastReported[verdict.signature] = now
        return verdict
    }

    fun addListener(listener: (AdaptiveBudget) -> Unit) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: (AdaptiveBudget) -> Unit) {
        listeners.remove(listener)
    }

    fun snapshot(): GovernorSnapshot = GovernorSnapshot(
        tierName = budget.tier.name,
        frameHealth = frameHealth,
        budget = budget,
        reportedSignatures = lastReported.size,
    )

    private fun publish(next: AdaptiveBudget) {
        if (budget == next) return
        budget = next
        listeners.forEach { runCatching { it(next) } }
    }

    companion object {
        private const val REPORT_WINDOW_MS = 60_000L
    }
}

data class GovernorSnapshot(
    val tierName: String,
    val frameHealth: FrameHealth,
    val budget: AdaptiveBudget,
    val reportedSignatures: Int,
)
