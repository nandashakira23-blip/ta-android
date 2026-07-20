package com.fleur.attendance.utils

import android.content.Context
import android.content.SharedPreferences
import com.fleur.attendance.data.api.TokenManager
import com.fleur.attendance.data.model.Employee

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    // Device-level flags that must survive logout (clearSession only clears "user_session").
    private val appPrefs: SharedPreferences = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
    private val tokenManager: TokenManager = TokenManager.getInstance(context)
    
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_EMPLOYEE_ID = "employee_id"
        private const val KEY_EMPLOYEE_NIK = "employee_nik"
        private const val KEY_EMPLOYEE_NAME = "employee_name"
        private const val KEY_EMPLOYEE_POSITION_ID = "employee_position_id"
        private const val KEY_EMPLOYEE_WORK_SCHEDULE_ID = "employee_work_schedule_id"
        private const val KEY_EMPLOYEE_PHOTO = "employee_photo"
        private const val KEY_EMPLOYEE_EMAIL = "employee_email"
        private const val KEY_EMPLOYEE_PHONE = "employee_phone"
        private const val KEY_EMPLOYEE_JENIS_KELAMIN = "employee_jenis_kelamin"
        private const val KEY_EMPLOYEE_TANGGAL_LAHIR = "employee_tanggal_lahir"
        private const val KEY_EMPLOYEE_ADDRESS = "employee_address"
        private const val KEY_SAVED_NIK = "saved_nik"
    }
    
    fun saveUserSession(employee: Employee) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putInt(KEY_EMPLOYEE_ID, employee.id)
        editor.putString(KEY_EMPLOYEE_NIK, employee.nik)
        editor.putString(KEY_EMPLOYEE_NAME, employee.nama)
        employee.email?.let { editor.putString(KEY_EMPLOYEE_EMAIL, it) }
        employee.phone?.let { editor.putString(KEY_EMPLOYEE_PHONE, it) }
        employee.jabatan?.let { editor.putInt(KEY_EMPLOYEE_POSITION_ID, it.id) }
        employee.workScheduleId?.let { editor.putInt(KEY_EMPLOYEE_WORK_SCHEDULE_ID, it) }
        employee.fotoReferensi?.let { editor.putString(KEY_EMPLOYEE_PHOTO, it) }
        employee.jenisKelamin?.let { editor.putString(KEY_EMPLOYEE_JENIS_KELAMIN, it) }
        employee.tanggalLahir?.let { editor.putString(KEY_EMPLOYEE_TANGGAL_LAHIR, it) }
        employee.address?.let { editor.putString(KEY_EMPLOYEE_ADDRESS, it) }
        editor.apply()
        
        // Also save to TokenManager
        tokenManager.saveEmployeeInfo(employee.id, employee.nik, employee.nama)
    }
    
    fun isLoggedIn(): Boolean {
        // Check both old session and new token
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) || tokenManager.isLoggedIn()
    }
    
    fun getEmployeeId(): Int {
        val id = prefs.getInt(KEY_EMPLOYEE_ID, -1)
        val tokenId = tokenManager.getEmployeeId()
        
        // Return valid ID, prefer session over token
        return when {
            id > 0 -> id
            tokenId > 0 -> tokenId
            else -> -1
        }
    }
    
    fun getEmployeeNik(): String? {
        val sessionNik = prefs.getString(KEY_EMPLOYEE_NIK, null)
        val tokenNik = tokenManager.getEmployeeNik()
        
        // Return non-null NIK, prefer session over token
        return sessionNik ?: tokenNik
    }
    
    fun getEmployeeName(): String? {
        val sessionName = prefs.getString(KEY_EMPLOYEE_NAME, null)
        val tokenName = tokenManager.getEmployeeName()
        
        // Return non-null name, prefer session over token
        return sessionName ?: tokenName
    }
    
    fun getEmployeeEmail(): String? {
        return prefs.getString(KEY_EMPLOYEE_EMAIL, null)
    }
    
    fun getEmployeePhone(): String? {
        return prefs.getString(KEY_EMPLOYEE_PHONE, null)
    }
    
    fun getEmployeePositionId(): Int {
        return prefs.getInt(KEY_EMPLOYEE_POSITION_ID, -1)
    }
    
    fun getEmployeeWorkScheduleId(): Int {
        return prefs.getInt(KEY_EMPLOYEE_WORK_SCHEDULE_ID, -1)
    }
    
    fun getEmployeePhoto(): String? {
        return prefs.getString(KEY_EMPLOYEE_PHOTO, null)
    }
    
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
        
        // Also clear tokens
        tokenManager.clearTokens()
    }
    
    // Remember Me functions
    fun saveNik(nik: String) {
        prefs.edit().putString(KEY_SAVED_NIK, nik).apply()
    }
    
    fun getSavedNik(): String? {
        return prefs.getString(KEY_SAVED_NIK, null)
    }
    
    fun clearSavedNik() {
        prefs.edit().remove(KEY_SAVED_NIK).apply()
    }

    // Device-level flag: has this device ever completed activation/login?
    // Decides the first screen after splash (Activation for a brand-new device, Login otherwise).
    // Stored in a separate prefs file so it persists across logout (clearSession()).
    fun setEverActivated() {
        appPrefs.edit().putBoolean("ever_activated", true).apply()
    }

    fun hasEverActivated(): Boolean {
        return appPrefs.getBoolean("ever_activated", false)
    }
}
