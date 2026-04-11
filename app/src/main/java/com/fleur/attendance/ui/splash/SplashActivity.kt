package com.fleur.attendance.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.fleur.attendance.databinding.ActivitySplashBinding
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
                // User is logged in, go to MainActivity
                Log.d("SplashActivity", "Going to MainActivity")
                navigateToMain()
            } else {
                // User not logged in, go to LoginActivity
                Log.d("SplashActivity", "Going to LoginActivity")
                navigateToLogin()
            }
            
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error navigating to next screen", e)
            // Fallback to login
            navigateToLogin()
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
    
    override fun onDestroy() {
        super.onDestroy()
        binding = null
        sessionManager = null
        Log.d("SplashActivity", "SplashActivity destroyed")
    }
}
