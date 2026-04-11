package com.fleur.attendance.utils

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fleur.attendance.R
import com.fleur.attendance.data.model.AttendanceHelper
import com.fleur.attendance.data.model.AttendanceHistoryItem
import com.fleur.attendance.data.model.AttendanceStatusType
import com.fleur.attendance.data.model.DailyAttendanceRecord

/**
 * Extension functions untuk memudahkan handling attendance status di UI
 */

/**
 * Set status badge dengan warna dan text yang sesuai
 */
fun TextView.setAttendanceStatus(
    status: String,
    lateMinutes: Int? = null,
    earlyLeaveMinutes: Int? = null,
    overtimeMinutes: Int? = null
) {
    val statusType = AttendanceStatusType.fromValue(status)
    val badgeText = AttendanceHelper.getStatusBadgeText(status, lateMinutes, earlyLeaveMinutes, overtimeMinutes)
    
    text = badgeText
    
    // Set background color
    val colorResId = when (statusType) {
        AttendanceStatusType.PRESENT -> R.color.status_present
        AttendanceStatusType.LATE -> R.color.status_late
        AttendanceStatusType.ABSENT -> R.color.status_absent
        AttendanceStatusType.LEAVE -> R.color.status_leave
        AttendanceStatusType.SICK -> R.color.status_sick
        AttendanceStatusType.HOLIDAY -> R.color.status_holiday
    }
    
    setBackgroundColor(ContextCompat.getColor(context, colorResId))
    setTextColor(Color.WHITE)
}

/**
 * Get status color dari string status
 */
fun Context.getAttendanceStatusColor(status: String): Int {
    val statusType = AttendanceStatusType.fromValue(status)
    val colorResId = when (statusType) {
        AttendanceStatusType.PRESENT -> R.color.status_present
        AttendanceStatusType.LATE -> R.color.status_late
        AttendanceStatusType.ABSENT -> R.color.status_absent
        AttendanceStatusType.LEAVE -> R.color.status_leave
        AttendanceStatusType.SICK -> R.color.status_sick
        AttendanceStatusType.HOLIDAY -> R.color.status_holiday
    }
    return ContextCompat.getColor(this, colorResId)
}

/**
 * Get status label dari string status
 */
fun Context.getAttendanceStatusLabel(status: String): String {
    return AttendanceStatusType.getLabel(status)
}

/**
 * Extension untuk AttendanceHistoryItem
 */
fun AttendanceHistoryItem.getStatusBadgeText(): String {
    return AttendanceHelper.getStatusBadgeText(
        status = this.status,
        lateMinutes = this.lateMinutes,
        earlyLeaveMinutes = this.earlyLeaveMinutes,
        overtimeMinutes = this.overtimeMinutes
    )
}

fun AttendanceHistoryItem.getWorkSummaryText(): String {
    val parts = mutableListOf<String>()
    
    // Total work
    if (totalWorkMinutes != null && totalWorkMinutes > 0) {
        parts.add("Kerja: ${AttendanceHelper.formatMinutesToHoursMinutes(totalWorkMinutes)}")
    }
    
    // Effective work
    if (effectiveWorkMinutes != null && effectiveWorkMinutes > 0) {
        parts.add("Efektif: ${AttendanceHelper.formatMinutesToHoursMinutes(effectiveWorkMinutes)}")
    }
    
    // Late
    if (lateMinutes != null && lateMinutes > 0) {
        parts.add("Terlambat: ${AttendanceHelper.formatMinutesToHoursMinutes(lateMinutes)}")
    }
    
    // Early leave
    if (earlyLeaveMinutes != null && earlyLeaveMinutes > 0) {
        parts.add("Pulang Awal: ${AttendanceHelper.formatMinutesToHoursMinutes(earlyLeaveMinutes)}")
    }
    
    // Overtime
    if (overtimeMinutes != null && overtimeMinutes > 0) {
        parts.add("Lembur: ${AttendanceHelper.formatMinutesToHoursMinutes(overtimeMinutes)}")
    }
    
    // Approved leave
    if (approvedLeaveMinutes != null && approvedLeaveMinutes > 0) {
        parts.add("Izin: ${AttendanceHelper.formatMinutesToHoursMinutes(approvedLeaveMinutes)}")
    }
    
    return if (parts.isEmpty()) "-" else parts.joinToString(" • ")
}

/**
 * Extension untuk DailyAttendanceRecord
 */
fun DailyAttendanceRecord.getStatusBadgeText(): String {
    return AttendanceHelper.getStatusBadgeText(
        status = this.status,
        lateMinutes = this.lateMinutes,
        earlyLeaveMinutes = this.earlyLeaveMinutes,
        overtimeMinutes = this.overtimeMinutes
    )
}

fun DailyAttendanceRecord.getWorkSummaryText(): String {
    val parts = mutableListOf<String>()
    
    // Work duration
    if (!workDuration.isNullOrEmpty() && workDuration != "0 menit") {
        parts.add("Kerja: $workDuration")
    }
    
    // Effective work
    if (effectiveWorkMinutes != null && effectiveWorkMinutes > 0) {
        parts.add("Efektif: ${AttendanceHelper.formatMinutesToHoursMinutes(effectiveWorkMinutes)}")
    }
    
    // Late
    if (lateMinutes != null && lateMinutes > 0) {
        parts.add("Terlambat: ${AttendanceHelper.formatMinutesToHoursMinutes(lateMinutes)}")
    }
    
    // Early leave
    if (earlyLeaveMinutes != null && earlyLeaveMinutes > 0) {
        parts.add("Pulang Awal: ${AttendanceHelper.formatMinutesToHoursMinutes(earlyLeaveMinutes)}")
    }
    
    // Overtime
    if (overtimeMinutes != null && overtimeMinutes > 0) {
        parts.add("Lembur: ${AttendanceHelper.formatMinutesToHoursMinutes(overtimeMinutes)}")
    }
    
    // Approved leave
    if (approvedLeaveMinutes != null && approvedLeaveMinutes > 0) {
        parts.add("Izin: ${AttendanceHelper.formatMinutesToHoursMinutes(approvedLeaveMinutes)}")
    }
    
    return if (parts.isEmpty()) "-" else parts.joinToString(" • ")
}

/**
 * Check if attendance record has any issues
 */
fun AttendanceHistoryItem.hasIssues(): Boolean {
    return AttendanceHelper.isLate(lateMinutes) || 
           AttendanceHelper.isEarlyLeave(earlyLeaveMinutes) ||
           status == AttendanceStatusType.ABSENT.value
}

fun DailyAttendanceRecord.hasIssues(): Boolean {
    return AttendanceHelper.isLate(lateMinutes) || 
           AttendanceHelper.isEarlyLeave(earlyLeaveMinutes) ||
           status == AttendanceStatusType.ABSENT.value
}

/**
 * Get icon for status
 */
fun String.getAttendanceStatusIcon(): String {
    return AttendanceStatusType.fromValue(this).icon
}
