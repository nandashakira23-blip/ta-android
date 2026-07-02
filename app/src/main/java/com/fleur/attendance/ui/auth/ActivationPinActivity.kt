package com.fleur.attendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.R
import com.fleur.attendance.databinding.ActivityActivationPinBinding

class ActivationPinActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivationPinBinding

    private lateinit var nik: String
    private lateinit var employeeName: String
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nik = intent.getStringExtra("nik") ?: ""
        employeeName = intent.getStringExtra("employeeName") ?: "Karyawan"
        email = intent.getStringExtra("email") ?: ""

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.tvNik.text = nik
        binding.tvEmployeeName.text = employeeName
        binding.etPin.requestFocus()
    }

    private fun setupClickListeners() {
        binding.btnActivate.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            val confirmPin = binding.etConfirmPin.text.toString().trim()

            if (validatePin(pin, confirmPin)) {
                goToFaceEnrollment(pin)
            }
        }
    }

    private fun validatePin(pin: String, confirmPin: String): Boolean {
        var isValid = true

        if (pin.isEmpty()) {
            binding.tilPin.error = getString(R.string.validation_pin_required)
            isValid = false
        } else if (pin.length != 6 || !pin.all { it.isDigit() }) {
            binding.tilPin.error = getString(R.string.validation_pin_invalid)
            isValid = false
        } else {
            binding.tilPin.error = null
        }

        if (confirmPin.isEmpty()) {
            binding.tilConfirmPin.error = getString(R.string.validation_pin_required)
            isValid = false
        } else if (confirmPin != pin) {
            binding.tilConfirmPin.error = getString(R.string.validation_pin_mismatch)
            isValid = false
        } else {
            binding.tilConfirmPin.error = null
        }

        return isValid
    }

    private fun goToFaceEnrollment(pin: String) {
        showLoading(true)
        val intent = Intent(this, FaceEnrollmentActivity::class.java).apply {
            putExtra("nik", nik)
            putExtra("employeeName", employeeName)
            putExtra("email", email)
            putExtra("pin", pin)
            putExtra("isReactivation", false)
        }
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnActivate.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
