package com.fleur.attendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.R
import com.fleur.attendance.data.api.LegacyApiAdapter
import com.fleur.attendance.databinding.ActivityActivationBinding

/**
 * Aktivasi akun dalam SATU layar:
 * input email -> Kirim OTP -> masukkan Kode OTP -> Lanjut Daftar Wajah.
 *
 * Layar OTP terpisah (EmailOtpVerificationActivity) sudah digabung ke sini
 * supaya tidak ada tampilan ganda (email + OTP jadi satu halaman).
 */
class ActivationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivationBinding
    private var nik: String = ""
    private lateinit var employeeName: String
    private lateinit var apiAdapter: LegacyApiAdapter
    private var isReactivation: Boolean = false

    // Status OTP dalam layar ini
    private var otpSent: Boolean = false
    private var otpEmail: String = ""
    private var isCoolingDown: Boolean = false
    private var resendTimer: CountDownTimer? = null

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

        if (isReactivation) {
            binding.tvActivationTitle.text = getString(R.string.reactivation_title)
            binding.tvActivationWelcome.text = getString(R.string.activation_email_reactivation_welcome)
        }

        // Kalau email diubah setelah OTP terkirim, wajib kirim ulang OTP ke email baru.
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (otpSent && s?.toString()?.trim() != otpEmail) {
                    otpSent = false
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSendOtp.setOnClickListener { onSendOtp() }
        binding.btnActivate.setOnClickListener { onContinue() }

        // "Sudah punya akun? Login" -> ke layar Login
        binding.tvHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Step 1: kirim OTP ke email
    private fun onSendOtp() {
        val nikInput = binding.etNikActivation.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        if (!validateNik(nikInput) || !validateEmail(email)) return

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
                    binding.tvEmployeeName.text = employeeName
                    requestOtp(nikInput, email)
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

    private fun requestOtp(nikInput: String, email: String) {
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
                    otpSent = true
                    otpEmail = email
                    binding.etOtp.requestFocus()
                    Toast.makeText(this, getString(R.string.otp_email_info, email), Toast.LENGTH_LONG).show()
                    startResendCooldown()
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

    // Step 2: verifikasi OTP lalu lanjut ke pembuatan PIN
    private fun onContinue() {
        val nikInput = binding.etNikActivation.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val otp = binding.etOtp.text.toString().trim()

        if (!validateNik(nikInput) || !validateEmail(email)) return

        if (!otpSent || email != otpEmail) {
            binding.tilOtp.error = getString(R.string.activation_send_otp_first)
            return
        }
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            binding.tilOtp.error = getString(R.string.validation_otp_invalid)
            return
        }
        binding.tilOtp.error = null
        verifyOtpAndContinue(nikInput, email, otp)
    }

    private fun verifyOtpAndContinue(nikInput: String, email: String, otp: String) {
        showLoading(true)
        apiAdapter.verifyEmailOtp(
            nik = nikInput,
            email = email,
            otp = otp,
            onSuccess = { response ->
                runOnUiThread {
                    showLoading(false)
                    if (!response.success) {
                        binding.tilOtp.error = response.message
                        return@runOnUiThread
                    }
                    binding.tilOtp.error = null
                    val intent = Intent(this, ActivationPinActivity::class.java).apply {
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
                    binding.tilOtp.error = error
                }
            }
        )
    }

    private fun startResendCooldown() {
        resendTimer?.cancel()
        isCoolingDown = true
        resendTimer = object : CountDownTimer(30_000L, 1_000L) {
            override fun onTick(msLeft: Long) {
                binding.btnSendOtp.isEnabled = false
                binding.btnSendOtp.text =
                    getString(R.string.activation_resend_otp_countdown, (msLeft / 1000).toInt())
            }

            override fun onFinish() {
                isCoolingDown = false
                binding.btnSendOtp.isEnabled = true
                binding.btnSendOtp.text = getString(R.string.activation_resend_otp)
            }
        }.start()
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

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) com.fleur.attendance.utils.LoadingOverlay.show(this) else com.fleur.attendance.utils.LoadingOverlay.hide(this)
        binding.btnActivate.isEnabled = !show
        binding.btnSendOtp.isEnabled = !show && !isCoolingDown
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }
}
