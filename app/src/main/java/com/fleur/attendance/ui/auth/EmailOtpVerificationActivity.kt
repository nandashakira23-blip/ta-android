package com.fleur.attendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.R
import com.fleur.attendance.data.api.LegacyApiAdapter
import com.fleur.attendance.databinding.ActivityEmailOtpVerificationBinding

class EmailOtpVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailOtpVerificationBinding
    private lateinit var apiAdapter: LegacyApiAdapter
    private lateinit var nik: String
    private lateinit var email: String
    private lateinit var employeeName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiAdapter = LegacyApiAdapter(this)
        nik = intent.getStringExtra("nik") ?: ""
        email = intent.getStringExtra("email") ?: ""
        employeeName = intent.getStringExtra("employeeName") ?: "Karyawan"

        binding.tvEmailInfo.text = getString(R.string.otp_email_info, email)
        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6 || !otp.all { it.isDigit() }) {
                binding.tilOtp.error = getString(R.string.validation_otp_invalid)
                return@setOnClickListener
            }
            binding.tilOtp.error = null
            verifyOtp(otp)
        }
    }

    private fun verifyOtp(otp: String) {
        showLoading(true)
        apiAdapter.verifyEmailOtp(
            nik = nik,
            email = email,
            otp = otp,
            onSuccess = { response ->
                runOnUiThread {
                    showLoading(false)
                    if (response.success) {
                        Toast.makeText(this, "OTP valid. Lanjut buat PIN.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, ActivationPinActivity::class.java)
                        intent.putExtra("nik", nik)
                        intent.putExtra("email", email)
                        intent.putExtra("employeeName", employeeName)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnVerifyOtp.isEnabled = !show
    }
}
