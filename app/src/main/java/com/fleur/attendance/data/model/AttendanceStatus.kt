package com.fleur.attendance.data.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.fleur.attendance.R

/**
 * Enum untuk status presensi sesuai dengan database server
 * Status: present, late, absent, leave, sick, holiday
 */
enum class AttendanceStatusType(
    val value: String,
    @StringRes val labelResId: Int,
    @ColorRes val colorResId: Int,
    val icon: String
) {
    PRESENT(
        value = "present",
        labelResId = R.string.status_present,
        colorResId = R.color.status_present,
        icon = "✓"
    ),
    LATE(
        value = "late",
        labelResId = R.string.status_late,
        colorResId = R.color.status_late,
        icon = "⏰"
    ),
    ABSENT(
        value = "absent",
        labelResId = R.string.status_absent,
        colorResId = R.color.status_absent,
        icon = "✗"
    ),
    LEAVE(
        value = "leave",
        labelResId = R.string.status_leave,
        colorResId = R.color.status_leave,
        icon = "📋"
    ),
    SICK(
        value = "sick",
        labelResId = R.string.status_sick,
        colorResId = R.color.status_sick,
        icon = "🏥"
    ),
    HOLIDAY(
        value = "holiday",
        labelResId = R.string.status_holiday,
        colorResId = R.color.status_holiday,
        icon = "🎉"
    );

    companion object {
        fun fromValue(value: String): AttendanceStatusType {
            return values().find { it.value.equals(value, ignoreCase = true) } ?: ABSENT
        }

        fun getLabel(value: String): String {
            return when (fromValue(value)) {
                PRESENT -> "Hadir"
                LATE -> "Terlambat"
                ABSENT -> "Tidak Hadir"
                LEAVE -> "Izin"
                SICK -> "Sakit"
                HOLIDAY -> "Libur"
            }
        }

        fun getColor(value: String): String {
            return when (fromValue(value)) {
                PRESENT -> "#4CAF50"  // Green
                LATE -> "#FF9800"     // Orange
                ABSENT -> "#F44336"   // Red
                LEAVE -> "#2196F3"    // Blue
                SICK -> "#9C27B0"     // Purple
                HOLIDAY -> "#00BCD4"  // Cyan
            }
        }
    }
}

/**
 * Helper class untuk menghitung dan memformat data presensi
 */
object AttendanceHelper {
    
    /**
     * Format menit ke format jam:menit
     */
    fun formatMinutesToHoursMinutes(minutes: Int?): String {
        if (minutes == null || minutes == 0) return "0 menit"
        
        val hours = minutes / 60
        val mins = minutes % 60
        
        return when {
            hours > 0 && mins > 0 -> "$hours jam $mins menit"
            hours > 0 -> "$hours jam"
            else -> "$mins menit"
        }
    }

    /**
     * Cek apakah karyawan terlambat
     */
    fun isLate(lateMinutes: Int?): Boolean {
        return lateMinutes != null && lateMinutes > 0
    }

    /**
     * Cek apakah karyawan pulang lebih awal
     */
    fun isEarlyLeave(earlyLeaveMinutes: Int?): Boolean {
        return earlyLeaveMinutes != null && earlyLeaveMinutes > 0
    }

    /**
     * Cek apakah ada lembur
     */
    fun hasOvertime(overtimeMinutes: Int?): Boolean {
        return overtimeMinutes != null && overtimeMinutes > 0
    }

    /**
     * Hitung total jam kerja efektif (dalam jam desimal)
     */
    fun calculateEffectiveWorkHours(effectiveWorkMinutes: Int?): Double {
        return (effectiveWorkMinutes ?: 0) / 60.0
    }

    /**
     * Get status badge text
     */
    fun getStatusBadgeText(
        status: String,
        lateMinutes: Int? = null,
        earlyLeaveMinutes: Int? = null,
        overtimeMinutes: Int? = null
    ): String {
        val statusType = AttendanceStatusType.fromValue(status)
        val baseLabel = AttendanceStatusType.getLabel(status)
        
        return when (statusType) {
            AttendanceStatusType.LATE -> {
                if (lateMinutes != null && lateMinutes > 0) {
                    "$baseLabel (${formatMinutesToHoursMinutes(lateMinutes)})"
                } else {
                    baseLabel
                }
            }
            AttendanceStatusType.PRESENT -> {
                when {
                    hasOvertime(overtimeMinutes) -> 
                        "$baseLabel + Lembur (${formatMinutesToHoursMinutes(overtimeMinutes)})"
                    isEarlyLeave(earlyLeaveMinutes) -> 
                        "$baseLabel (Pulang Awal ${formatMinutesToHoursMinutes(earlyLeaveMinutes)})"
                    else -> baseLabel
                }
            }
            else -> baseLabel
        }
    }

    /**
     * Get detailed work summary
     */
    fun getWorkSummary(
        totalWorkMinutes: Int?,
        approvedLeaveMinutes: Int?,
        effectiveWorkMinutes: Int?,
        overtimeMinutes: Int?,
        lateMinutes: Int?,
        earlyLeaveMinutes: Int?
    ): WorkSummary {
        return WorkSummary(
            totalWork = formatMinutesToHoursMinutes(totalWorkMinutes),
            approvedLeave = formatMinutesToHoursMinutes(approvedLeaveMinutes),
            effectiveWork = formatMinutesToHoursMinutes(effectiveWorkMinutes),
            overtime = formatMinutesToHoursMinutes(overtimeMinutes),
            late = formatMinutesToHoursMinutes(lateMinutes),
            earlyLeave = formatMinutesToHoursMinutes(earlyLeaveMinutes),
            hasLate = isLate(lateMinutes),
            hasEarlyLeave = isEarlyLeave(earlyLeaveMinutes),
            hasOvertime = hasOvertime(overtimeMinutes)
        )
    }
}

/**
 * Data class untuk summary pekerjaan
 */
data class WorkSummary(
    val totalWork: String,
    val approvedLeave: String,
    val effectiveWork: String,
    val overtime: String,
    val late: String,
    val earlyLeave: String,
    val hasLate: Boolean,
    val hasEarlyLeave: Boolean,
    val hasOvertime: Boolean
)
