package com.fleur.attendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.R
import com.fleur.attendance.data.api.LegacyApiAdapter
import com.fleur.attendance.databinding.ActivityLoginBinding
import com.fleur.attendance.ui.main.MainActivity
import com.fleur.attendance.utils.SessionManager

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var apiAdapter: LegacyApiAdapter
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        apiAdapter = LegacyApiAdapter(this)
        sessionManager = SessionManager(this)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Prefill NIK if passed from the activation-status check (CekStatusActivity)
        intent.getStringExtra("nik")?.takeIf { it.isNotBlank() }?.let { binding.etNik.setText(it) }
        // Set focus to NIK input
        binding.etNik.requestFocus()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val nik = binding.etNik.text.toString().trim()
            val pin = binding.etPin.text.toString().trim()
            
            if (validateInput(nik, pin)) {
                performLogin(nik, pin)
            }
        }
        
        binding.tvForgotPin.setOnClickListener {
            startActivationActivity("", employeeName = "Karyawan", isReactivation = false)
        }
    }
    
    private fun validateInput(nik: String, pin: String): Boolean {
        var isValid = true
        
        // Validate NIK
        if (nik.isEmpty()) {
            binding.tilNik.error = getString(R.string.validation_nik_required)
            isValid = false
        } else if (nik.length != 16) {
            binding.tilNik.error = getString(R.string.validation_nik_invalid)
            isValid = false
        } else {
            binding.tilNik.error = null
        }
        
        // Validate PIN
        if (pin.isEmpty()) {
            binding.tilPin.error = getString(R.string.validation_pin_required)
            isValid = false
        } else if (pin.length != 6) {
            binding.tilPin.error = getString(R.string.validation_pin_invalid)
            isValid = false
        } else {
            binding.tilPin.error = null
        }
        
        return isValid
    }
    
    private fun performLogin(nik: String, pin: String) {
        Log.d(TAG, "Starting performLogin with NIK: $nik")
        showLoading(true)
        
        // First check NIK
        apiAdapter.checkNik(nik,
            onSuccess = { response ->
                Log.d(TAG, "NIK check successful")
                
                if (response.success && response.data != null) {
                    if (!response.data.exists) {
                        showLoading(false)
                        showError(getString(R.string.login_nik_not_registered))
                    } else if (response.data.isActivated) {
                        // Proceed with login
                        Log.d(TAG, "Proceeding with PIN login")
                        loginWithPin(nik, pin)
                    } else {
                        // Redirect to activation flow
                        Log.d(TAG, "Redirecting to activation flow")
                        showLoading(false)
                        val employeeName = response.data.employee?.nama ?: "Karyawan"
                        startActivationActivity(nik, employeeName, isReactivation = false)
                    }
                } else {
                    Log.e(TAG, "NIK not found")
                    showLoading(false)
                    showError(getString(R.string.login_nik_not_registered))
                }
            },
            onError = { error ->
                Log.e(TAG, "CheckNik error: $error")
                showLoading(false)
                showError(error)
            }
        )
    }
    
    private fun loginWithPin(nik: String, pin: String) {
        Log.d(TAG, "Starting login with NIK: $nik")
        
        apiAdapter.login(nik, pin,
            onSuccess = { response ->
                showLoading(false)
                
                if (response.success && response.data != null) {
                    Log.d(TAG, "Login successful")
                    sessionManager.saveUserSession(response.data.employee)
                    sessionManager.setEverActivated() // device has a known account -> default to Login next time

                    // Navigate to main activity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    showError(response.message)
                }
            },
            onError = { error ->
                showLoading(false)
                Log.e(TAG, "Login error: $error")
                showError(error)
            }
        )
    }
    
    private fun startActivationActivity(nik: String, employeeName: String = "", isReactivation: Boolean = false) {
        val intent = Intent(this, ActivationActivity::class.java)
        intent.putExtra("nik", nik)
        intent.putExtra("employeeName", employeeName)
        intent.putExtra("isReactivation", isReactivation)
        startActivity(intent)
    }
    
    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (show) com.fleur.attendance.utils.LoadingOverlay.show(this) else com.fleur.attendance.utils.LoadingOverlay.hide(this)
        binding.btnLogin.isEnabled = !show
    }
    
    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    companion object {
        private const val TAG = "LoginActivity"
    }
}
