package com.fleur.attendance.data.api

import android.content.Context
import com.fleur.attendance.data.model.*
import com.fleur.attendance.data.repository.*
import com.fleur.attendance.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * LegacyApiAdapter - Compatibility layer for old API calls
 * 
 * This adapter provides backward compatibility with old code
 * while using the new JWT-based API internally
 */
class LegacyApiAdapter(private val context: Context) {
    
    private val authRepo = AuthRepository(context)
    private val attendanceRepo = AttendanceRepository(context)
    private val employeeRepo = EmployeeRepository(context)
    private val pinRepo = PinRepository(context)
    private val faceRepo = FaceRepository(context)
    private val locationRepo = LocationRepository(context)
    private val sessionManager = SessionManager(context)
    
    /**
     * Get current employee ID from session
     */
    private fun getEmployeeId(): Int {
        return sessionManager.getEmployeeId()
    }
    
    /**
     * Check NIK (old signature)
     */
    fun checkNik(
        nik: String,
        onSuccess: (CheckNikResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        authRepo.checkNik(nik, onSuccess, onError)
    }
    
    /**
     * Login (old signature)
     */
    fun login(
        nik: String,
        pin: String,
        onSuccess: (LoginResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        authRepo.login(nik, pin, onSuccess, onError)
    }
    
    /**
     * Activate (old signature with file)
     */
    fun activate(
        nik: String,
        pin: String,
        email: String?,
        facePhotoFiles: List<File>,
        onSuccess: (ActivateResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        authRepo.activate(nik, pin, email, facePhotoFiles, onSuccess, onError)
    }

    fun verifyEmailOtp(
        nik: String,
        email: String,
        otp: String,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        authRepo.verifyEmailOtp(nik, email, otp, onSuccess, onError)
    }

    fun requestEmailOtp(
        nik: String,
        email: String,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        authRepo.requestEmailOtp(nik, email, onSuccess, onError)
    }
    
    /**
     * Clock In (old signature)
     */
    fun clockIn(
        latitude: Double,
        longitude: Double,
        photoFiles: List<File>,
        onSuccess: (ClockInResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        attendanceRepo.clockIn(latitude, longitude, photoFiles, onSuccess, onError)
    }
    
    /**
     * Clock Out (old signature)
     */
    fun clockOut(
        latitude: Double,
        longitude: Double,
        photoFiles: List<File>,
        onSuccess: (ClockOutResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        attendanceRepo.clockOut(latitude, longitude, photoFiles, onSuccess, onError)
    }
    
    /**
     * Get Attendance Status (old signature)
     */
    fun getAttendanceStatus(
        onSuccess: (AttendanceStatusResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val employeeId = getEmployeeId()
        attendanceRepo.getAttendanceStatus(employeeId, onSuccess, onError)
    }
    
    /**
     * Get Today Attendance (old signature)
     */
    fun getTodayAttendance(
        onSuccess: (TodayAttendanceResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        attendanceRepo.getTodayAttendance(onSuccess, onError)
    }
    
    /**
     * Get Attendance History (old signature - returns list directly)
     */
    fun getEmployeeAttendanceHistory(
        startDate: String? = null,
        endDate: String? = null,
        limit: Int? = null,
        offset: Int? = null,
        onSuccess: (List<AttendanceHistoryItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        attendanceRepo.getAttendanceHistory(startDate, endDate, limit, offset,
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    onSuccess(response.data.records)
                } else {
                    onError(response.message)
                }
            },
            onError = onError
        )
    }
    
    /**
     * Get Attendance Summary (old signature)
     */
    fun getAttendanceSummary(
        month: Int? = null,
        year: Int? = null,
        onSuccess: (AttendanceSummaryResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        attendanceRepo.getAttendanceSummary(month, year, onSuccess, onError)
    }
    
    /**
     * Get Employee Profile (old signature)
     */
    fun getEmployeeProfile(
        employeeId: Int,
        onSuccess: (EmployeeProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        employeeRepo.getProfile(employeeId, 
            onSuccess = { response ->
                response.data?.let { onSuccess(it) }
                    ?: onError("No profile data")
            },
            onError = onError
        )
    }
    
    /**
     * Update Employee Profile (self-service)
     */
    fun updateEmployeeProfile(
        employeeId: Int,
        email: String?,
        phone: String?,
        jenisKelamin: String?,
        tanggalLahir: String?,
        address: String?,
        profilePicture: File?,
        onSuccess: (UpdateProfileData) -> Unit,
        onError: (String) -> Unit
    ) {
        employeeRepo.updateProfile(
            employeeId, email, phone, jenisKelamin, tanggalLahir, address, profilePicture,
            onSuccess = { response ->
                response.data?.let { onSuccess(it) }
                    ?: onError("No data returned")
            },
            onError = onError
        )
    }
    
    /**
     * Get base URL for image loading
     */
    fun getBaseUrl(): String {
        return ApiConfig.getBaseUrl()
    }
    
    /**
     * Get Work Schedule (old signature)
     */
    fun getWorkSchedule(
        onSuccess: (WorkScheduleResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val employeeId = getEmployeeId()
        employeeRepo.getWorkSchedule(employeeId, onSuccess, onError)
    }
    
    /**
     * Validate PIN (old signature) - Removed as endpoint no longer exists
     * PIN validation is now handled during login process
     */
    
    /**
     * Change PIN (old signature)
     */
    fun changePin(
        currentPin: String,
        newPin: String,
        onSuccess: (ChangePinResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        pinRepo.changePin(currentPin, newPin, onSuccess, onError)
    }
    
    /**
     * Face Enrollment (old signature)
     */
    fun faceEnrollment(
        facePhotoFile: File,
        onSuccess: (FaceEnrollmentResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        faceRepo.enrollFace(facePhotoFile, onSuccess, onError)
    }
    
    /**
     * Face Re-enrollment (old signature)
     */
    fun faceReenrollment(
        currentPin: String,
        facePhotoFile: File,
        onSuccess: (FaceReenrollmentResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        faceRepo.reenrollFace(currentPin, facePhotoFile, onSuccess, onError)
    }
    
    /**
     * Get Office Location (old signature)
     */
    fun getOfficeLocation(
        onSuccess: (OfficeLocationResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        locationRepo.getOfficeLocation(onSuccess, onError)
    }
    
    /**
     * Validate Location (old signature)
     */
    fun validateLocation(
        latitude: Double,
        longitude: Double,
        onSuccess: (ValidateLocationResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        locationRepo.validateLocation(latitude, longitude, onSuccess, onError)
    }
    
    /**
     * Logout
     */
    fun logout() {
        authRepo.logout()
    }
    
    /**
     * Check if logged in
     */
    fun isLoggedIn(): Boolean {
        return authRepo.isLoggedIn()
    }
}
