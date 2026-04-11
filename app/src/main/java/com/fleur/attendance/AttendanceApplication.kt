package com.fleur.attendance

import android.app.Application
import com.fleur.attendance.data.api.TokenManager

class AttendanceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Load saved token into ApiConfig on app start
            val tokenManager = TokenManager.getInstance(this)
            tokenManager.loadToken()
        } catch (e: Exception) {
            // Log error but don't crash the app
            android.util.Log.e("AttendanceApplication", "Error loading token on app start", e)
        }
    }
}
