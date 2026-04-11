package com.fleur.attendance.data.model

import com.google.gson.annotations.SerializedName

// ===== BASE RESPONSE MODELS =====

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val code: String?
)

// ===== AUTHENTICATION RESPONSE MODELS =====

data class CheckNikResponse(
    val success: Boolean,
    val message: String,
    val data: CheckNikData?
)

data class CheckNikData(
    val exists: Boolean,
    @SerializedName("is_activated")
    val isActivated: Boolean,
    @SerializedName("has_face_reference")
    val hasFaceReference: Boolean,
    val employee: EmployeeBasicInfo?
)

data class ActivateResponse(
    val success: Boolean,
    val message: String,
    val data: ActivateData?
)

data class ActivateData(
    val employee: EmployeeBasicInfo,
    val tokens: TokenData,
    @SerializedName("face_enrollment")
    val faceEnrollment: FaceEnrollmentResult,
    @SerializedName("requires_email_verification")
    val requiresEmailVerification: Boolean = false
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(
    val employee: Employee,
    val tokens: TokenData,
    @SerializedName("work_schedule")
    val workSchedule: WorkSchedule?,
    val shift: ShiftInfo? = null
)

data class TokenData(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Int
)

data class RefreshTokenResponse(
    val success: Boolean,
    val message: String,
    val data: RefreshTokenData?
)

data class RefreshTokenData(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Int
)

// ===== EMPLOYEE MODELS =====

data class Employee(
    val id: Int,
    val nik: String,
    val nama: String,
    val email: String?,
    val phone: String?,
    @SerializedName("profile_picture")
    val profilePicture: String?,
    val jabatan: JobInfo?,
    @SerializedName("is_activated")
    val isActivated: Boolean,
    @SerializedName("foto_referensi")
    val fotoReferensi: String?,
    @SerializedName("face_enrollment_completed")
    val faceEnrollmentCompleted: Boolean,
    @SerializedName("work_schedule_id")
    val workScheduleId: Int?,
    @SerializedName("email_verified")
    val emailVerified: Boolean = false,
    @SerializedName("shift_id")
    val shiftId: Int? = null,
    @SerializedName("created_at")
    val createdAt: String?
)

data class EmployeeBasicInfo(
    val id: Int,
    val nik: String,
    val nama: String
)

data class JobInfo(
    val id: Int,
    @SerializedName(value = "nama_jabatan", alternate = ["name"])
    val namaJabatan: String,
    val deskripsi: String?
)

// ===== ATTENDANCE RESPONSE MODELS =====

data class ClockInResponse(
    val success: Boolean,
    val message: String,
    val data: ClockInData?
)

data class ClockInData(
    @SerializedName("attendanceId")
    val attendanceId: Int,
    @SerializedName("clockInTime")
    val clockInTime: String,
    val status: String,
    val location: LocationValidation,
    @SerializedName("faceMatch")
    val faceMatch: FaceMatchResult
)

data class ClockOutResponse(
    val success: Boolean,
    val message: String,
    val data: ClockOutData?
)

data class ClockOutData(
    @SerializedName("attendanceId")
    val attendanceId: Int,
    @SerializedName("clockOutTime")
    val clockOutTime: String,
    val status: String,
    @SerializedName("workDuration")
    val workDuration: String,
    @SerializedName("approvedLeaveMinutes")
    val approvedLeaveMinutes: Int? = null,
    @SerializedName("effectiveWorkMinutes")
    val effectiveWorkMinutes: Int? = null,
    @SerializedName("overtimeMinutes")
    val overtimeMinutes: Int? = null,
    @SerializedName("lateMinutes")
    val lateMinutes: Int? = null,
    @SerializedName("earlyLeaveMinutes")
    val earlyLeaveMinutes: Int? = null,
    val location: LocationValidation,
    @SerializedName("faceMatch")
    val faceMatch: FaceMatchResult
)

data class LocationValidation(
    val distance: Double,
    @SerializedName("isValid")
    val isValid: Boolean
)

data class FaceMatchResult(
    @SerializedName("isMatch")
    val isMatch: Boolean,
    val similarity: Double,
    val confidence: String,
    @SerializedName("facesDetected")
    val facesDetected: Int = 0
)

data class AttendanceStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: AttendanceStatus?
)

data class AttendanceStatus(
    val date: String,
    @SerializedName("hasCheckedIn")
    val hasCheckedIn: Boolean,
    @SerializedName("hasCheckedOut")
    val hasCheckedOut: Boolean,
    @SerializedName("checkIn")
    val checkIn: AttendanceRecord?,
    @SerializedName("checkOut")
    val checkOut: AttendanceRecord?,
    @SerializedName("workDuration")
    val workDuration: String?,
    @SerializedName("canCheckIn")
    val canCheckIn: Boolean,
    @SerializedName("canCheckOut")
    val canCheckOut: Boolean,
    @SerializedName("nextAction")
    val nextAction: String,
    @SerializedName("workSchedule")
    val workSchedule: WorkScheduleInfo?
)

data class WorkScheduleInfo(
    @SerializedName(value = "nama", alternate = ["name"])
    val nama: String,
    @SerializedName(value = "jam_masuk", alternate = ["start_time"])
    val jamMasuk: String?,
    @SerializedName(value = "jam_keluar", alternate = ["end_time"])
    val jamKeluar: String?,
    @SerializedName(value = "batas_absen_masuk_awal", alternate = ["clock_in_start"])
    val clockInStart: String?,
    @SerializedName(value = "batas_absen_masuk_akhir", alternate = ["clock_in_end"])
    val clockInEnd: String?,
    @SerializedName(value = "batas_absen_keluar_awal", alternate = ["clock_out_start"])
    val clockOutStart: String?,
    @SerializedName(value = "batas_absen_keluar_akhir", alternate = ["clock_out_end"])
    val clockOutEnd: String?,
    @SerializedName(value = "hari_kerja", alternate = ["work_days"])
    val hariKerja: List<String>?
)

data class TodayAttendanceResponse(
    val success: Boolean,
    val message: String,
    val data: TodayAttendanceData?
)

data class TodayAttendanceData(
    val date: String,
    @SerializedName(value = "hasCheckedIn", alternate = ["has_clocked_in"])
    val hasCheckedIn: Boolean,
    @SerializedName(value = "hasCheckedOut", alternate = ["has_clocked_out"])
    val hasCheckedOut: Boolean,
    @SerializedName(value = "checkIn", alternate = ["clock_in"])
    val checkIn: AttendanceRecord?,
    @SerializedName(value = "checkOut", alternate = ["clock_out"])
    val checkOut: AttendanceRecord?,
    @SerializedName(value = "workDuration", alternate = ["work_duration"])
    val workDuration: String?,
    @SerializedName("canCheckIn")
    val canCheckIn: Boolean,
    @SerializedName("canCheckOut")
    val canCheckOut: Boolean
)

data class AttendanceRecord(
    val time: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double,
    val similarity: Double?,
    val photo: String?
)

// ===== ATTENDANCE HISTORY RESPONSE MODELS =====

data class AttendanceHistoryResponse(
    val success: Boolean,
    val message: String,
    val data: AttendanceHistoryList?
)

data class AttendanceHistoryList(
    val records: List<AttendanceHistoryItem>,
    val pagination: PaginationInfo
)

data class AttendanceHistoryItem(
    val id: Int,
    val date: String,
    val clockIn: ClockInOutDetail?,
    val clockOut: ClockInOutDetail?,
    val status: String,
    val statusLabel: String,
    val hasClockIn: Boolean,
    val hasClockOut: Boolean,
    @SerializedName("total_work_minutes")
    val totalWorkMinutes: Int? = null,
    @SerializedName("approved_leave_minutes")
    val approvedLeaveMinutes: Int? = null,
    @SerializedName("effective_work_minutes")
    val effectiveWorkMinutes: Int? = null,
    @SerializedName("overtime_minutes")
    val overtimeMinutes: Int? = null,
    @SerializedName("late_minutes")
    val lateMinutes: Int? = null,
    @SerializedName("early_leave_minutes")
    val earlyLeaveMinutes: Int? = null,
    @SerializedName("leave_request_id")
    val leaveRequestId: Int? = null,
    val keterangan: String? = null
)

data class ClockInOutDetail(
    val time: String,
    val location: LocationDetail,
    val photo: String?
)

data class LocationDetail(
    val latitude: Double?,
    val longitude: Double?,
    val distance: Double?,
    val isValid: Boolean
)

data class PaginationInfo(
    val total: Int,
    val limit: Int,
    val offset: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)

data class AttendanceSummaryResponse(
    val success: Boolean,
    val message: String,
    val data: AttendanceSummaryData?
)

data class AttendanceSummaryData(
    val month: Int,
    val year: Int,
    @SerializedName("total_days")
    val totalDays: Int,
    @SerializedName("present_days")
    val presentDays: Int,
    @SerializedName("absent_days")
    val absentDays: Int,
    @SerializedName("late_days")
    val lateDays: Int,
    @SerializedName("total_work_hours")
    val totalWorkHours: String,
    @SerializedName("average_work_hours")
    val averageWorkHours: String,
    @SerializedName("daily_records")
    val dailyRecords: List<DailyAttendanceRecord>
)

data class DailyAttendanceRecord(
    val tanggal: String,
    @SerializedName("jam_masuk")
    val jamMasuk: String?,
    @SerializedName("jam_keluar")
    val jamKeluar: String?,
    @SerializedName("work_duration")
    val workDuration: String?,
    val status: String,
    @SerializedName("total_work_minutes")
    val totalWorkMinutes: Int? = null,
    @SerializedName("approved_leave_minutes")
    val approvedLeaveMinutes: Int? = null,
    @SerializedName("effective_work_minutes")
    val effectiveWorkMinutes: Int? = null,
    @SerializedName("overtime_minutes")
    val overtimeMinutes: Int? = null,
    @SerializedName("late_minutes")
    val lateMinutes: Int? = null,
    @SerializedName("early_leave_minutes")
    val earlyLeaveMinutes: Int? = null,
    @SerializedName("leave_request_id")
    val leaveRequestId: Int? = null
)

// ===== EMPLOYEE PROFILE RESPONSE MODELS =====

data class EmployeeProfileResponse(
    val success: Boolean,
    val message: String,
    val data: EmployeeProfile?
)

data class EmployeeProfile(
    val employee: Employee,
    @SerializedName("work_schedule")
    val workSchedule: WorkSchedule?,
    @SerializedName("face_enrollment")
    val faceEnrollment: FaceEnrollmentInfo?,
    val shift: ShiftInfo? = null
)

data class FaceEnrollmentInfo(
    val completed: Boolean,
    @SerializedName("has_reference")
    val hasReference: Boolean,
    @SerializedName("enrollment_date")
    val enrollmentDate: String?
)

// ===== WORK SCHEDULE RESPONSE MODELS =====

data class WorkScheduleResponse(
    val success: Boolean,
    val message: String? = null,
    val data: WorkSchedulePayload?
)

data class WorkSchedulePayload(
    @SerializedName("hasSchedule")
    val hasSchedule: Boolean? = null,
    @SerializedName("hasWorkToday")
    val hasWorkToday: Boolean? = null,
    val today: String? = null,
    @SerializedName("approved_leave")
    val approvedLeave: ApprovedLeaveInfo? = null,
    val schedule: WorkScheduleToday? = null,
    val shift: ShiftInfo? = null
)

data class WorkSchedule(
    val id: Int,
    @SerializedName(value = "nama", alternate = ["name"])
    val nama: String,
    @SerializedName(value = "jam_masuk", alternate = ["start_time"])
    val jamMasuk: String?,
    @SerializedName(value = "jam_keluar", alternate = ["end_time"])
    val jamKeluar: String?,
    @SerializedName(value = "batas_absen_masuk_awal", alternate = ["clock_in_start"])
    val clockInStart: String?,
    @SerializedName(value = "batas_absen_masuk_akhir", alternate = ["clock_in_end"])
    val clockInEnd: String?,
    @SerializedName(value = "batas_absen_keluar_awal", alternate = ["clock_out_start"])
    val clockOutStart: String?,
    @SerializedName(value = "batas_absen_keluar_akhir", alternate = ["clock_out_end"])
    val clockOutEnd: String?,
    @SerializedName(value = "hari_kerja", alternate = ["work_days"])
    val hariKerja: List<String>,
    @SerializedName("is_active")
    val isActive: Boolean
)

data class WorkScheduleToday(
    val id: Int? = null,
    @SerializedName(value = "nama", alternate = ["name"])
    val nama: String? = null,
    @SerializedName(value = "jam_masuk", alternate = ["start_time"])
    val jamMasuk: String? = null,
    @SerializedName(value = "jam_keluar", alternate = ["end_time"])
    val jamKeluar: String? = null,
    @SerializedName(value = "batas_absen_masuk_awal", alternate = ["clock_in_start"])
    val clockInStart: String? = null,
    @SerializedName(value = "batas_absen_masuk_akhir", alternate = ["clock_in_end"])
    val clockInEnd: String? = null,
    @SerializedName(value = "batas_absen_keluar_awal", alternate = ["clock_out_start"])
    val clockOutStart: String? = null,
    @SerializedName(value = "batas_absen_keluar_akhir", alternate = ["clock_out_end"])
    val clockOutEnd: String? = null,
    @SerializedName(value = "hari_kerja", alternate = ["work_days"])
    val hariKerja: List<String>? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("approved_leave")
    val approvedLeave: ApprovedLeaveInfo? = null,
    val shift: ShiftInfo? = null
)

data class ShiftInfo(
    val id: Int? = null,
    @SerializedName(value = "nama", alternate = ["name"])
    val nama: String? = null,
    @SerializedName(value = "jam_masuk", alternate = ["start_time"])
    val jamMasuk: String? = null,
    @SerializedName(value = "jam_keluar", alternate = ["end_time"])
    val jamKeluar: String? = null
)

data class ApprovedLeaveInfo(
    val id: Int,
    val jenis: String,
    @SerializedName("jam_mulai")
    val jamMulai: String? = null,
    @SerializedName("jam_selesai")
    val jamSelesai: String? = null,
    @SerializedName(value = "durasi_menit", alternate = ["duration_minutes"])
    val durasiMenit: Int? = null,
    val status: String? = null
)

// ===== PIN MANAGEMENT RESPONSE MODELS =====

data class ValidatePinResponse(
    val success: Boolean,
    val message: String,
    val data: ValidatePinData?
)

data class ValidatePinData(
    @SerializedName("pin_valid")
    val pinValid: Boolean,
    @SerializedName("validated_at")
    val validatedAt: String
)

data class ChangePinResponse(
    val success: Boolean,
    val message: String,
    val data: ChangePinData?
)

data class ChangePinData(
    @SerializedName("pin_changed")
    val pinChanged: Boolean,
    @SerializedName("changed_at")
    val changedAt: String
)

// ===== FACE RECOGNITION RESPONSE MODELS =====

data class FaceEnrollmentResponse(
    val success: Boolean,
    val message: String,
    val data: FaceEnrollmentResult?
)

data class FaceEnrollmentResult(
    @SerializedName("faces_detected")
    val facesDetected: Int,
    @SerializedName("enrollment_completed")
    val enrollmentCompleted: Boolean,
    @SerializedName("photo_saved")
    val photoSaved: String
)

data class FaceReenrollmentResponse(
    val success: Boolean,
    val message: String,
    val data: FaceReenrollmentResult?
)

data class FaceReenrollmentResult(
    @SerializedName("faces_detected")
    val facesDetected: Int,
    @SerializedName("reference_id")
    val referenceId: Int? = null,
    @SerializedName("old_photo")
    val oldPhoto: String?,
    @SerializedName("new_photo")
    val newPhoto: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class FaceRecognitionStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: FaceRecognitionStatus?
)

data class FaceRecognitionStatus(
    val enabled: Boolean,
    @SerializedName("has_reference")
    val hasReference: Boolean,
    @SerializedName("enrollment_completed")
    val enrollmentCompleted: Boolean
)

data class EmployeeReferencePhotoResponse(
    val success: Boolean,
    val message: String? = null,
    val data: EmployeeReferencePhoto?
)

data class EmployeeReferencePhoto(
    @SerializedName("has_reference")
    val hasReference: Boolean,
    @SerializedName(value = "photo_url", alternate = ["photo_path", "foto_url", "foto_path"])
    val photoUrl: String?,
    @SerializedName("upload_date")
    val uploadDate: String?
)

// ===== LOCATION RESPONSE MODELS =====

data class OfficeLocationResponse(
    val success: Boolean,
    val message: String,
    val data: OfficeLocation?
)

data class OfficeLocation(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("radiusMeters")
    val radiusMeters: Double,
    val address: String?
)

data class ValidateLocationResponse(
    val success: Boolean,
    val message: String,
    val data: LocationValidationResult?
)

data class LocationValidationResult(
    @SerializedName("isValid")
    val isValid: Boolean,
    val distance: Double,
    @SerializedName("allowedRadius")
    val allowedRadius: Int,
    @SerializedName("officeLocation")
    val officeLocation: OfficeLocationCoords?,
    @SerializedName("userLocation")
    val userLocation: OfficeLocationCoords?
)

data class OfficeLocationCoords(
    val latitude: Double,
    val longitude: Double
)

// ===== SYSTEM RESPONSE MODELS =====

data class HealthResponse(
    val success: Boolean,
    val message: String,
    val timestamp: String,
    val version: String?
)

// ===== FACE VALIDATION REALTIME RESPONSE =====

data class FaceValidationResponse(
    val success: Boolean,
    val message: String? = null,
    val data: FaceValidationData?
)

data class FaceValidationData(
    @SerializedName("facesDetected")
    val facesDetected: Int,
    @SerializedName("isMatch")
    val isMatch: Boolean,
    val similarity: Double,
    val confidence: String,
    val threshold: Double,
    val message: String
)

data class Point(
    val x: Double,
    val y: Double
)

// ===== UPDATE PROFILE RESPONSE =====

data class UpdateProfileResponse(
    val success: Boolean,
    val message: String,
    val data: UpdateProfileData?
)

data class UpdateProfileData(
    val employee: Employee
)

// ===== LEAVE REQUEST RESPONSE MODELS =====

data class LeaveRequestResponse(
    val success: Boolean,
    val message: String,
    val data: LeaveRequestItem? = null
)

data class LeaveRequestsResponse(
    val success: Boolean,
    val message: String,
    val data: List<LeaveRequestItem> = emptyList()
)

data class LeaveRequestItem(
    val id: Int,
    val jenis: String,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String,
    @SerializedName("jam_mulai")
    val jamMulai: String? = null,
    @SerializedName("jam_selesai")
    val jamSelesai: String? = null,
    @SerializedName("durasi_menit")
    val durasiMenit: Int? = null,
    val alasan: String,
    val lampiran: String? = null,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)
