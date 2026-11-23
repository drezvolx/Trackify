package com.blankpages.trackify.domain

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: String,
    val isWorkingDay: Boolean = true,
    val packagesCarried: Int = 0,
    val packagesDelivered: Int = 0,
    val ratePerPackage: Double = 2500.0,
    val bonus: Double = 0.0,
    val expense: Double = 0.0,
    val notes: String = "",
) {
    @Ignore val safeCarried: Int = if (isWorkingDay) packagesCarried.coerceAtLeast(0) else 0

    @Ignore val safeDelivered: Int = if (isWorkingDay) packagesDelivered.coerceIn(0, safeCarried) else 0

    @Ignore val netIncome: Double = if (isWorkingDay) {
        (safeDelivered * ratePerPackage.coerceAtLeast(0.0)) +
            bonus.coerceAtLeast(0.0) -
            expense.coerceAtLeast(0.0)
    } else {
        0.0
    }

    @Ignore val remainingPackages: Int = if (isWorkingDay) (safeCarried - safeDelivered).coerceAtLeast(0) else 0

    @Ignore val deliveryProgress: Float = if (isWorkingDay && safeCarried > 0) {
        (safeDelivered.toFloat() / safeCarried).coerceIn(0f, 1f)
    } else {
        0f
    }

    fun sanitized(): DailyLog {
        val cleanCarried = if (isWorkingDay) packagesCarried.coerceAtLeast(0) else 0
        val cleanDelivered = if (isWorkingDay) packagesDelivered.coerceIn(0, cleanCarried) else 0
        return copy(
            date = date.trim(),
            packagesCarried = cleanCarried,
            packagesDelivered = cleanDelivered,
            ratePerPackage = ratePerPackage.coerceAtLeast(0.0),
            bonus = bonus.coerceAtLeast(0.0),
            expense = expense.coerceAtLeast(0.0),
            notes = notes.trim(),
        )
    }
}

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val landmarkNote: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String = "",
) {
    fun trimmed(): SavedLocation = copy(
        customerName = customerName.trim(),
        landmarkNote = landmarkNote.trim(),
        photoPath = photoPath.trim(),
    )
}

@Entity(tableName = "courier_notes")
data class CourierNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val updatedAt: String = "",
    val linkedLocationId: Int? = null,
    val isResolved: Boolean = false,
) {
    fun trimmed(fallbackDate: String): CourierNote = copy(
        title = title.trim(),
        content = content.trim(),
        updatedAt = updatedAt.ifBlank { fallbackDate },
    )
}

data class MonthlySummary(val totalWorkingDays: Int = 0, val totalDelivered: Int = 0, val totalNetIncome: Double = 0.0)

fun List<DailyLog>.toMonthlySummary(): MonthlySummary {
    var workingDays = 0
    var delivered = 0
    var netIncome = 0.0

    for (log in this) {
        if (!log.isWorkingDay) continue
        workingDays++
        delivered += log.safeDelivered
        netIncome += log.netIncome
    }

    return MonthlySummary(
        totalWorkingDays = workingDays,
        totalDelivered = delivered,
        totalNetIncome = netIncome,
    )
}
