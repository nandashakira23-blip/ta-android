package com.fleur.attendance.ui.attendance.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fleur.attendance.R
import com.fleur.attendance.data.model.AttendanceHistoryItem
import com.fleur.attendance.databinding.ItemAttendanceHistoryBinding
import com.fleur.attendance.utils.getWorkSummaryText
import com.fleur.attendance.utils.hasIssues
import com.fleur.attendance.utils.setAttendanceStatus
import java.text.SimpleDateFormat
import java.util.*

class AttendanceHistoryAdapter : ListAdapter<AttendanceHistoryItem, AttendanceHistoryAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemAttendanceHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: AttendanceHistoryItem) {
            binding.apply {
                // Format date
                tvDate.text = formatDate(item.date)
                
                // Set clock type badge
                when {
                    item.hasClockIn && item.hasClockOut -> {
                        tvClockType.text = "Full Day"
                        tvClockType.setBackgroundResource(R.drawable.badge_clock_in_bg)
                    }
                    item.hasClockIn -> {
                        tvClockType.text = "Clock In"
                        tvClockType.setBackgroundResource(R.drawable.badge_clock_in_bg)
                    }
                    item.hasClockOut -> {
                        tvClockType.text = "Clock Out"
                        tvClockType.setBackgroundResource(R.drawable.badge_clock_out_bg)
                    }
                    else -> {
                        tvClockType.text = "No Data"
                        tvClockType.setBackgroundResource(R.drawable.badge_clock_out_bg)
                    }
                }
                
                // Show both check-in and check-out times if available
                val timeText = buildString {
                    if (item.hasClockIn && item.clockIn != null) {
                        append("Masuk: ${formatTime(item.clockIn.time)}")
                    }
                    if (item.hasClockOut && item.clockOut != null) {
                        if (isNotEmpty()) append("\n")
                        append("Keluar: ${formatTime(item.clockOut.time)}")
                    }
                    if (isEmpty()) append("--:--")
                }
                tvTime.text = timeText
                
                // Location status based on clock in distance
                if (item.hasClockIn && item.clockIn != null) {
                    val distance = item.clockIn.location.distance
                    val isValid = item.clockIn.location.isValid
                    
                    tvLocationStatus.text = if (isValid) "Dalam Area" else "Luar Area"
                    tvDistance.text = distance?.let { "${it.toInt()}m" } ?: "--"
                    
                    // Set location status color
                    val statusColor = if (isValid) {
                        R.color.success_green
                    } else {
                        R.color.warning_yellow
                    }
                    tvLocationStatus.setTextColor(itemView.context.getColor(statusColor))
                } else {
                    tvLocationStatus.text = "Tidak ada data lokasi"
                    tvDistance.text = "--"
                    tvLocationStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                }
                
                // Badge: sedang menggantikan siapa (dari data pengganti yang disetujui)
                if (!item.menggantikan.isNullOrBlank()) {
                    tvSubstitute.text = "Menggantikan ${item.menggantikan}"
                    tvSubstitute.visibility = View.VISIBLE
                } else {
                    tvSubstitute.visibility = View.GONE
                }

                // Show attendance status with enhanced info using extension function
                tvVerificationMethod.setAttendanceStatus(
                    status = item.status,
                    lateMinutes = item.lateMinutes,
                    earlyLeaveMinutes = item.earlyLeaveMinutes,
                    overtimeMinutes = item.overtimeMinutes
                )
                
                // Show work summary if available
                val workSummary = item.getWorkSummaryText()
                if (workSummary != "-") {
                    // You can add a TextView in layout to show this
                    // tvWorkSummary.text = workSummary
                    // tvWorkSummary.visibility = View.VISIBLE
                }
                
                // Highlight if has issues
                if (item.hasIssues()) {
                    // You can add visual indicator for issues
                    // cardView.strokeColor = itemView.context.getColor(R.color.warning_yellow)
                }
            }
        }
        private fun formatDate(dateString: String): String {
            return try {
                val witaTimeZone = java.util.TimeZone.getTimeZone("Asia/Makassar")
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                inputFormat.timeZone = witaTimeZone
                outputFormat.timeZone = witaTimeZone
                
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
        
        private fun formatTime(timeString: String): String {
            return try {
                // Handle multiple formats:
                // 1. "2026-01-16 08:00:00" (correct format)
                // 2. "Fri Jan 16 2026 00:00:00 GMT+0700 (...) 08:00:00" (buggy format)
                
                // Try to extract time part (HH:mm:ss)
                val timePart = when {
                    // If contains space, split and get last part
                    timeString.contains(" ") -> {
                        val parts = timeString.split(" ")
                        // Get the part that looks like time (HH:mm:ss)
                        parts.lastOrNull { it.matches(Regex("\\d{2}:\\d{2}:\\d{2}")) } ?: parts.last()
                    }
                    else -> timeString
                }
                
                // Extract HH:mm from HH:mm:ss
                if (timePart.length >= 5) {
                    timePart.substring(0, 5)
                } else {
                    "--:--"
                }
            } catch (e: Exception) {
                "--:--"
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<AttendanceHistoryItem>() {
        override fun areItemsTheSame(oldItem: AttendanceHistoryItem, newItem: AttendanceHistoryItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: AttendanceHistoryItem, newItem: AttendanceHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
