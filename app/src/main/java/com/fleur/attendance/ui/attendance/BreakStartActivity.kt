package com.fleur.attendance.ui.attendance

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.R
import com.fleur.attendance.data.repository.AttendanceRepository
import com.fleur.attendance.databinding.ActivityBreakStartBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Mulai Istirahat" confirmation screen.
 * Shows current time + the daily break allowance, then starts a break via the existing API.
 * Allowance is passed from Home (which already has the attendance/break status).
 */
class BreakStartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBreakStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreakStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val allowance = intent.getIntExtra("allowance", 60)
        binding.tvCurrentTime.text = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        binding.tvMaxDuration.text = "$allowance menit"
        binding.tvBreakNote.text = "Anda dapat istirahat maksimal $allowance menit per hari"

        binding.tvBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener { startBreak() }
        setupBottomNav()
    }

    private fun startBreak() {
        binding.btnConfirm.isEnabled = false
        AttendanceRepository(this).startBreak(
            onSuccess = { response ->
                runOnUiThread {
                    Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                    finish() // back to Home, which refreshes its status on resume
                }
            },
            onError = { error ->
                runOnUiThread {
                    binding.btnConfirm.isEnabled = true
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { goHome(); true }
                R.id.nav_attendance -> {
                    startActivity(Intent(this, AttendanceHistoryActivity::class.java)); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, com.fleur.attendance.ui.profile.ProfileActivity::class.java)); true
                }
                else -> false
            }
        }
    }

    private fun goHome() {
        val i = Intent(this, com.fleur.attendance.ui.main.MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(i)
        finish()
    }
}
