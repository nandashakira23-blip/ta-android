package com.fleur.attendance.data.api

import com.fleur.attendance.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    // ===== AUTHENTICATION ENDPOINTS (No JWT Required) =====
    
    @POST("api/auth/check-nik")
    fun checkNik(@Body request: CheckNikRequest): Call<CheckNikResponse>
    
    @Multipart
    @POST("api/auth/activate")
    fun activate(
        @Part("nik") nik: RequestBody,
        @Part("pin") pin: RequestBody,
        @Part("email") email: RequestBody? = null,
        @Part facePhoto1: MultipartBody.Part,
        @Part facePhoto2: MultipartBody.Part,
        @Part facePhoto3: MultipartBody.Part
    ): Call<ActivateResponse>
    
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    
    @POST("api/auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    @POST("api/auth/request-email-verification")
    fun requestEmailVerification(): Call<ApiResponse<Any>>

    @POST("api/auth/request-email-otp")
    fun requestEmailOtp(@Body request: RequestEmailOtpRequest): Call<ApiResponse<Any>>

    @POST("api/auth/verify-email-otp")
    fun verifyEmailOtp(@Body request: VerifyEmailOtpRequest): Call<ApiResponse<Any>>
    
    // ===== ATTENDANCE ENDPOINTS (JWT Auto-Injected) =====
    
    @Multipart
    @POST("api/attendance/checkin")
    fun clockIn(
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part photo: MultipartBody.Part
    ): Call<ClockInResponse>
    
    @Multipart
    @POST("api/attendance/checkout")
    fun clockOut(
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part photo: MultipartBody.Part
    ): Call<ClockOutResponse>
    
    @GET("api/attendance/status/{karyawan_id}")
    fun getAttendanceStatus(@Path("karyawan_id") karyawanId: Int): Call<AttendanceStatusResponse>
    
    @GET("api/attendance/history")
    fun getAttendanceHistory(
        @Query("tanggal_mulai") startDate: String? = null,
        @Query("tanggal_selesai") endDate: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Call<AttendanceHistoryResponse>
    
    @GET("api/attendance/summary")
    fun getAttendanceSummary(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Call<AttendanceSummaryResponse>
    
    @GET("api/attendance/today")
    fun getTodayAttendance(): Call<TodayAttendanceResponse>
    
    // ===== EMPLOYEE PROFILE ENDPOINTS (JWT Auto-Injected) =====
    
    @GET("api/auth/profile/{id}")
    fun getEmployeeProfile(@Path("id") employeeId: Int): Call<EmployeeProfileResponse>
    
    @Multipart
    @PUT("api/auth/profile/{id}")
    fun updateEmployeeProfile(
        @Path("id") employeeId: Int,
        @Part("email") email: RequestBody?,
        @Part("phone") phone: RequestBody?,
        @Part profile_picture: MultipartBody.Part?
    ): Call<UpdateProfileResponse>
    
    @GET("api/schedule/today/{karyawan_id}")
    fun getEmployeeWorkSchedule(@Path("karyawan_id") karyawanId: Int): Call<WorkScheduleResponse>
    
    @GET("api/employee/face-reference")
    fun getEmployeeFaceReference(): Call<EmployeeReferencePhotoResponse>

    @GET("api/leave-requests")
    fun getLeaveRequests(): Call<LeaveRequestsResponse>

    @Multipart
    @POST("api/leave-requests")
    fun createLeaveRequest(
        @Part("jenis") jenis: RequestBody,
        @Part("kategori") kategori: RequestBody,
        @Part("tanggal_mulai") tanggalMulai: RequestBody,
        @Part("tanggal_selesai") tanggalSelesai: RequestBody,
        @Part("jam_mulai") jamMulai: RequestBody?,
        @Part("jam_selesai") jamSelesai: RequestBody?,
        @Part("alasan") alasan: RequestBody,
        @Part lampiran: MultipartBody.Part?
    ): Call<LeaveRequestResponse>
    
    // ===== PIN MANAGEMENT ENDPOINTS (JWT Auto-Injected) =====
    
    @POST("api/pin/change")
    fun changePin(@Body request: ChangePinRequest): Call<ChangePinResponse>
    
    // ===== FACE RECOGNITION ENDPOINTS (JWT Auto-Injected) =====
    
    @Multipart
    @POST("api/activation/upload-face")
    fun faceEnrollment(
        @Part reference: MultipartBody.Part
    ): Call<FaceEnrollmentResponse>
    
    @Multipart
    @POST("api/face/re-enroll")
    fun faceReenrollment(
        @Part("current_pin") currentPin: RequestBody,
        @Part("face_photo") facePhoto: MultipartBody.Part
    ): Call<FaceReenrollmentResponse>
    
    @GET("api/face/status")
    fun getFaceRecognitionStatus(): Call<FaceRecognitionStatusResponse>
    
    // ===== LOCATION ENDPOINTS (JWT Auto-Injected) =====
    
    @GET("api/settings/office-location")
    fun getOfficeLocation(): Call<OfficeLocationResponse>
    
    @POST("api/validation/location")
    fun validateLocation(@Body request: ValidateLocationRequest): Call<ValidateLocationResponse>
    
    // ===== SYSTEM ENDPOINTS (No JWT Required) =====
    
    @GET("api/health")
    fun healthCheck(): Call<HealthResponse>
    
    // ===== FACE DETECTION REALTIME =====
    
    @Multipart
    @POST("api/attendance/validate-face")
    fun validateFaceRealtime(
        @Part photo: MultipartBody.Part
    ): Call<FaceValidationResponse>
}
