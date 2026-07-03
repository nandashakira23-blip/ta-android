package com.fleur.attendance.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.databinding.ActivitySplashBinding
import com.fleur.attendance.ui.auth.CekStatusActivity
import com.fleur.attendance.ui.auth.LoginActivity
import com.fleur.attendance.ui.main.MainActivity
import com.fleur.attendance.utils.SessionManager

class SplashActivity : AppCompatActivity() {
    private var binding: ActivitySplashBinding? = null
    private var sessionManager: SessionManager? = null
    
    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("SplashActivity", "=== STARTING SPLASH ACTIVITY ===")
        
        try {
            binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
            
            sessionManager = SessionManager(this)
            
            Log.d("SplashActivity", "Splash initialized, waiting ${SPLASH_DELAY}ms...")
            
            // Navigate after delay
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNextScreen()
            }, SPLASH_DELAY)
            
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error in splash onCreate", e)
            // If splash fails, go directly to login
            navigateToLogin()
        }
    }
    
    private fun navigateToNextScreen() {
        try {
            Log.d("SplashActivity", "Navigating to next screen...")
            
            val isLoggedIn = sessionManager?.isLoggedIn() ?: false
            Log.d("SplashActivity", "User logged in: $isLoggedIn")
            
            if (isLoggedIn) {
                // Verifikasi ke server: kalau akun sudah di-reset admin (tidak aktif lagi),
                // paksa logout di perangkat ini -> balik ke aktivasi (jangan tetap login).
                verifyAccountStillActive()
            } else {
                // Not logged in -> show the activation-status check screen (routes to Login/Activation)
                Log.d("SplashActivity", "Going to CekStatusActivity")
                navigateToCekStatus()
            }
            
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error navigating to next screen", e)
            // Fallback to login
            navigateToLogin()
        }
    }
    
    // Cek ke server apakah akun masih aktif. Kalau sudah di-reset admin -> logout paksa di perangkat ini.
    private fun verifyAccountStillActive() {
        val nik = sessionManager?.getEmployeeNik()
        if (nik.isNullOrBlank()) { navigateToMain(); return }
        try {
            val apiAdapter = com.fleur.attendance.data.api.LegacyApiAdapter(this)
            apiAdapter.checkNik(nik,
                onSuccess = { response ->
                    val data = response.data
                    if (response.success && data != null && data.exists && data.isActivated) {
                        navigateToMain()
                    } else {
                        Log.d("SplashActivity", "Akun sudah di-reset/tidak aktif -> logout paksa")
                        sessionManager?.clearSession()
                        navigateToCekStatus()
                    }
                },
                onError = { err ->
                    // Gagal cek (mis. offline) -> jangan kunci user, tetap masuk
                    Log.w("SplashActivity", "Gagal verifikasi akun: $err")
                    navigateToMain()
                }
            )
        } catch (e: Exception) {
            Log.e("SplashActivity", "verifyAccountStillActive error", e)
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error navigating to MainActivity", e)
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error navigating to LoginActivity", e)
            // Last resort - just finish
            finish()
        }
    }

    private fun navigateToCekStatus() {
        try {
            val intent = Intent(this, CekStatusActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error navigating to CekStatusActivity", e)
            navigateToLogin()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        binding = null
        sessionManager = null
        Log.d("SplashActivity", "SplashActivity destroyed")
    }
}
