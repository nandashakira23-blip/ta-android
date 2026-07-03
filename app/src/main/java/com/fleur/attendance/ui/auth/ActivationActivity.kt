package com.fleur.attendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.R
import com.fleur.attendance.data.api.LegacyApiAdapter
import com.fleur.attendance.databinding.ActivityActivationBinding

class ActivationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivationBinding
    private var nik: String = ""
    private lateinit var employeeName: String
    private lateinit var apiAdapter: LegacyApiAdapter
    private var isReactivation: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiAdapter = LegacyApiAdapter(this)
        nik = intent.getStringExtra("nik") ?: ""
        employeeName = intent.getStringExtra("employeeName") ?: "Karyawan"
        isReactivation = intent.getBooleanExtra("isReactivation", false)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        binding.etNikActivation.setText(nik)
        binding.tvEmployeeName.text = employeeName
        if (nik.isBlank()) {
            binding.etNikActivation.requestFocus()
        } else {
            binding.etEmail.requestFocus()
        }
        
        // Update UI text based on activation type
        if (isReactivation) {
            binding.tvActivationTitle.text = getString(R.string.reactivation_title)
            binding.tvActivationWelcome.text = getString(R.string.activation_email_reactivation_welcome)
            binding.btnActivate.text = getString(R.string.activation_email_continue)
        }
    }
    
    private fun setupClickListeners() {
        binding.btnActivate.setOnClickListener {
            val nikInput = binding.etNikActivation.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            if (validateNik(nikInput) && validateEmail(email)) {
                validateNikAndContinue(nikInput, email)
            }
        }

        // "Sudah punya akun? Login" -> go to the Login screen
        binding.tvHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun validateNik(nikValue: String): Boolean {
        return when {
            nikValue.isEmpty() -> {
                binding.tilNikActivation.error = getString(R.string.validation_nik_required)
                false
            }
            nikValue.length != 16 -> {
                binding.tilNikActivation.error = getString(R.string.validation_nik_invalid)
                false
            }
            else -> {
                binding.tilNikActivation.error = null
                true
            }
        }
    }
    
    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.validation_email_required)
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.validation_email_invalid)
            return false
        }

        binding.tilEmail.error = null
        return true
    }

    private fun validateNikAndContinue(nikInput: String, email: String) {
        showLoading(true)
        apiAdapter.checkNik(
            nikInput,
            onSuccess = { response ->
                runOnUiThread {
                    if (!response.success || response.data == null || !response.data.exists) {
                        showLoading(false)
                        binding.tilNikActivation.error = getString(R.string.login_nik_not_registered)
                        return@runOnUiThread
                    }
                    if (response.data.isActivated) {
                        showLoading(false)
                        binding.tilNikActivation.error = getString(R.string.login_already_activated)
                        return@runOnUiThread
                    }
                    binding.tilNikActivation.error = null
                    employeeName = response.data.employee?.nama ?: employeeName
                    requestOtpAndContinue(nikInput, email)
                }
            },
            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    binding.tilNikActivation.error = error
                }
            }
        )
    }

    private fun requestOtpAndContinue(nikInput: String, email: String) {
        apiAdapter.requestEmailOtp(
            nik = nikInput,
            email = email,
            onSuccess = { response ->
                runOnUiThread {
                    showLoading(false)
                    if (!response.success) {
                        binding.tilEmail.error = response.message
                        return@runOnUiThread
                    }
                    binding.tilEmail.error = null
                    val intent = Intent(this, EmailOtpVerificationActivity::class.java).apply {
                        putExtra("nik", nikInput)
                        putExtra("email", email)
                        putExtra("employeeName", employeeName)
                    }
                    startActivity(intent)
                    finish()
                }
            },
            onError = { error ->
                runOnUiThread {
                    showLoading(false)
                    binding.tilEmail.error = error
                }
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) com.fleur.attendance.utils.LoadingOverlay.show(this) else com.fleur.attendance.utils.LoadingOverlay.hide(this)
        binding.btnActivate.isEnabled = !show
    }
}
