package com.blankpages.trackify.performance

/** Satu pengukuran query Room. `sql` sudah berisi placeholder yang terikat. */
data class QuerySignal(val sql: String, val durationMs: Long)

/** Hasil klasifikasi. [signature] dipakai untuk menekan log duplikat. */
data class QueryVerdict(val slow: Boolean, val signature: String, val suggestion: String?)

/**
 * Aturan murni untuk menerjemahkan query lambat menjadi saran yang bisa
 * ditindaklanjuti. Tidak ada IO, tidak ada logging, tidak ada dependensi Room.
 */
object QueryGuardPolicy {

    /** Evaluasi berbasis durasi, dipakai bila pemanggil benar-benar mengukur waktu. */
    fun evaluate(signal: QuerySignal, budget: AdaptiveBudget): QueryVerdict {
        if (signal.durationMs < budget.queryWarnMs) {
            return QueryVerdict(slow = false, signature = signatureOf(signal.sql), suggestion = null)
        }
        return QueryVerdict(
            slow = true,
            signature = signatureOf(signal.sql),
            suggestion = shapeSuggestion(signal.sql)
                ?: "Query melewati ambang anggaran; periksa rencana query dan seleksi kolom.",
        )
    }

    /**
     * Evaluasi tanpa timing. Dipakai RoomQueryGuard karena `QueryCallback` tidak
     * menyediakan waktu mulai/selesai. Yang dilaporkan adalah potensi masalah,
     * bukan klaim bahwa query benar-benar lambat.
     */
    fun evaluateShape(sql: String): QueryVerdict {
        val suggestion = shapeSuggestion(sql)
        return QueryVerdict(slow = suggestion != null, signature = signatureOf(sql), suggestion = suggestion)
    }

    fun signatureOf(sql: String): String = WHITESPACE.replace(sql, " ").trim().take(MAX_SIGNATURE)

    private fun shapeSuggestion(sql: String): String? {
        val upper = sql.uppercase()
        return when {
            "LIKE" in upper && "ORDER BY" in upper ->
                "Prefix LIKE + ORDER BY: gunakan rentang date >= start AND date < end " +
                    "supaya index primary key terpakai."

            "LIKE" in upper ->
                "Prefix LIKE: pertimbangkan rentang setara daripada LIKE '...%'."

            "ORDER BY" in upper && "courier_notes" in upper ->
                "courier_notes ORDER BY tanpa index komposit: evaluasi index " +
                    "(isResolved, isPinned, id) pada rilis berikutnya."

            "ORDER BY" in upper && "note_photos" in upper ->
                "note_photos ORDER BY: evaluasi index komposit (noteId, sortOrder, id)."

            "UPDATE" in upper && "linkedLocationId" in sql ->
                "UPDATE linkedLocationId tanpa index: evaluasi index pada kolom tersebut."

            else -> null
        }
    }

    private val WHITESPACE = Regex("\\s+")
    private const val MAX_SIGNATURE = 160
}
