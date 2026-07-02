package com.fleur.attendance.data.repository

import android.content.Context
import android.util.Log
import com.fleur.attendance.data.api.ApiConfig
import com.fleur.attendance.data.api.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.fleur.attendance.data.model.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

/**
 * AttendanceRepository - Handle attendance operations
 * 
 * This repository handles:
 * - Clock in/out
 * - Attendance status
 * - Attendance history
 * - Attendance summary
 */
class AttendanceRepository(private val context: Context) {
    
    private val apiService: ApiService = ApiConfig.getApiService()
    
    /**
     * Parse error message from API response body
     */
    private fun parseErrorMessage(errorBody: okhttp3.ResponseBody?, defaultMessage: String): String {
        return try {
            val errorJson = errorBody?.string()
            if (errorJson != null) {
                val jsonObject = JSONObject(errorJson)
                val message = jsonObject.optString("message", "")
                val code = jsonObject.optString("code", "")
                
                // Return user-friendly message based on error code
                when (code) {
                    "NO_PHOTO" -> "Foto diperlukan untuk absensi"
                    "NO_LOCATION" -> "Lokasi tidak tersedia. Pastikan GPS aktif"
                    "NO_OFFICE_LOCATION" -> "Lokasi kantor belum dikonfigurasi"
                    "LOCATION_INVALID" -> {
                        val data = jsonObject.optJSONObject("data")
                        val distance = data?.optDouble("distance", 0.0)?.toInt() ?: 0
                        "Anda berada di luar area kantor (${distance}m dari kantor)"
                    }
                    "NO_FACE_REFERENCE" -> "Foto referensi wajah belum terdaftar. Silakan aktivasi akun terlebih dahulu"
                    "NO_FACES_DETECTED" -> "Wajah tidak terdeteksi pada foto. Pastikan wajah terlihat jelas"
                    "FACE_NO_MATCH" -> "Wajah tidak cocok dengan data referensi"
                    "OUTSIDE_CHECKIN_WINDOW" -> "Check-in hanya bisa dilakukan pada jam yang telah ditentukan"
                    "OUTSIDE_CHECKOUT_WINDOW" -> "Check-out hanya bisa dilakukan pada jam yang telah ditentukan"
                    "ALREADY_CHECKED_IN" -> "Anda sudah melakukan check-in hari ini"
                    "ALREADY_CHECKED_OUT" -> "Anda sudah melakukan check-out hari ini"
                    "NO_CHECK_IN" -> "Anda harus check-in terlebih dahulu sebelum check-out"
                    "BREAK_ALREADY_STARTED" -> "Istirahat masih berlangsung"
                    "NO_ACTIVE_BREAK" -> "Tidak ada istirahat yang sedang berlangsung"
                    "BREAK_LOCATION_INVALID" -> {
                        val data = jsonObject.optJSONObject("data")
                        val distance = data?.optDouble("distance", 0.0)?.toInt() ?: 0
                        "Anda belum berada di area kantor (${distance}m dari kantor)"
                    }
                    "UNAUTHORIZED" -> "Sesi telah berakhir. Silakan login kembali"
                    "TOKEN_EXPIRED" -> "Sesi telah berakhir. Silakan login kembali"
                    else -> if (message.isNotEmpty()) message else defaultMessage
                }
            } else {
                defaultMessage
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error parsing error body", e)
            defaultMessage
        }
    }
    
    /**
     * Clock in with face photo and location
     */
    fun clockIn(
        latitude: Double,
        longitude: Double,
        facePhotoFiles: List<File>,
        onSuccess: (ClockInResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val latBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Multi-frame: send every captured frame under the "photo" field; the server picks the best match.
        val photoParts = facePhotoFiles.map { file ->
            MultipartBody.Part.createFormData(
                "photo",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        apiService.clockIn(latBody, lngBody, photoParts)
            .enqueue(object : Callback<ClockInResponse> {
                override fun onResponse(
                    call: Call<ClockInResponse>,
                    response: Response<ClockInResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal melakukan clock in"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<ClockInResponse>, t: Throwable) {
                    val errorMessage = when {
                        t.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Koneksi timeout. Periksa koneksi internet Anda"
                        t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                            "Tidak dapat terhubung ke server. Periksa koneksi internet"
                        else -> "Terjadi kesalahan jaringan: ${t.message}"
                    }
                    onError(errorMessage)
                }
            })
    }
    
    /**
     * Clock out with face photo and location
     */
    fun clockOut(
        latitude: Double,
        longitude: Double,
        facePhotoFiles: List<File>,
        onSuccess: (ClockOutResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val latBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Multi-frame: send every captured frame under the "photo" field; the server picks the best match.
        val photoParts = facePhotoFiles.map { file ->
            MultipartBody.Part.createFormData(
                "photo",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        apiService.clockOut(latBody, lngBody, photoParts)
            .enqueue(object : Callback<ClockOutResponse> {
                override fun onResponse(
                    call: Call<ClockOutResponse>,
                    response: Response<ClockOutResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal melakukan clock out"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<ClockOutResponse>, t: Throwable) {
                    val errorMessage = when {
                        t.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Koneksi timeout. Periksa koneksi internet Anda"
                        t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                            "Tidak dapat terhubung ke server. Periksa koneksi internet"
                        else -> "Terjadi kesalahan jaringan: ${t.message}"
                    }
                    onError(errorMessage)
                }
            })
    }
    
    /**
     * Validate face real-time (untuk preview sebelum clock in)
     */
    fun validateFaceRealtime(
        facePhotoFile: File,
        onSuccess: (FaceValidationResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val photoBody = facePhotoFile.asRequestBody("image/*".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData(
            "photo",
            facePhotoFile.name,
            photoBody
        )
        
        apiService.validateFaceRealtime(photoPart)
            .enqueue(object : Callback<FaceValidationResponse> {
                override fun onResponse(
                    call: Call<FaceValidationResponse>,
                    response: Response<FaceValidationResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal validasi wajah"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<FaceValidationResponse>, t: Throwable) {
                    onError(t.message ?: "Kesalahan jaringan")
                }
            })
    }
    
    /**
     * Get current attendance status
     */
    fun getAttendanceStatus(
        employeeId: Int,
        onSuccess: (AttendanceStatusResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getAttendanceStatus(employeeId)
            .enqueue(object : Callback<AttendanceStatusResponse> {
                override fun onResponse(
                    call: Call<AttendanceStatusResponse>,
                    response: Response<AttendanceStatusResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal mengambil status absensi"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<AttendanceStatusResponse>, t: Throwable) {
                    onError(t.message ?: "Kesalahan jaringan")
                }
            })
    }
    
    /**
     * Get today's attendance
     */
    fun getTodayAttendance(
        onSuccess: (TodayAttendanceResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getTodayAttendance()
            .enqueue(object : Callback<TodayAttendanceResponse> {
                override fun onResponse(
                    call: Call<TodayAttendanceResponse>,
                    response: Response<TodayAttendanceResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal mengambil data absensi hari ini"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<TodayAttendanceResponse>, t: Throwable) {
                    onError(t.message ?: "Kesalahan jaringan")
                }
            })
    }

    fun startBreak(
        onSuccess: (BreakActionResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.startBreak().enqueue(object : Callback<BreakActionResponse> {
            override fun onResponse(
                call: Call<BreakActionResponse>,
                response: Response<BreakActionResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Gagal memulai istirahat"
                    )
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<BreakActionResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun endBreak(
        latitude: Double,
        longitude: Double,
        facePhotoFiles: List<File>,
        onSuccess: (BreakActionResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val latBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Multi-frame: send every captured frame under the "photo" field; the server picks the best match.
        val photoParts = facePhotoFiles.map { file ->
            MultipartBody.Part.createFormData(
                "photo",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        apiService.endBreak(latBody, lngBody, photoParts).enqueue(object : Callback<BreakActionResponse> {
            override fun onResponse(
                call: Call<BreakActionResponse>,
                response: Response<BreakActionResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Gagal menyelesaikan istirahat"
                    )
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<BreakActionResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }
    
    /**
     * Get attendance history with filters
     */
    fun getAttendanceHistory(
        startDate: String? = null,
        endDate: String? = null,
        limit: Int? = null,
        offset: Int? = null,
        onSuccess: (AttendanceHistoryResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getAttendanceHistory(startDate, endDate, limit, offset)
            .enqueue(object : Callback<AttendanceHistoryResponse> {
                override fun onResponse(
                    call: Call<AttendanceHistoryResponse>,
                    response: Response<AttendanceHistoryResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal mengambil riwayat absensi"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<AttendanceHistoryResponse>, t: Throwable) {
                    onError(t.message ?: "Kesalahan jaringan")
                }
            })
    }
    
    /**
     * Get attendance summary for a month
     */
    fun getAttendanceSummary(
        month: Int? = null,
        year: Int? = null,
        onSuccess: (AttendanceSummaryResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getAttendanceSummary(month, year)
            .enqueue(object : Callback<AttendanceSummaryResponse> {
                override fun onResponse(
                    call: Call<AttendanceSummaryResponse>,
                    response: Response<AttendanceSummaryResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { onSuccess(it) }
                            ?: onError("Tidak ada respons dari server")
                    } else {
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal mengambil ringkasan absensi"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<AttendanceSummaryResponse>, t: Throwable) {
                    onError(t.message ?: "Kesalahan jaringan")
                }
            })
    }
}
