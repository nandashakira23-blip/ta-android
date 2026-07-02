package com.fleur.attendance.data.repository

import android.content.Context
import android.util.Log
import com.fleur.attendance.data.api.ApiConfig
import com.fleur.attendance.data.api.ApiService
import com.fleur.attendance.data.api.TokenManager
import com.fleur.attendance.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

/**
 * AuthRepository - Handle authentication operations
 * 
 * This repository handles:
 * - Login/Logout
 * - Activation
 * - Token refresh
 * - Token management
 */
class AuthRepository(private val context: Context) {
    
    private val apiService: ApiService = ApiConfig.getApiService()
    private val tokenManager: TokenManager = TokenManager.getInstance(context)
    
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
                
                when (code) {
                    "NIK_NOT_FOUND" -> "NIK tidak ditemukan dalam sistem"
                    "ALREADY_ACTIVATED" -> "Akun sudah diaktivasi sebelumnya"
                    "NOT_ACTIVATED" -> "Akun belum diaktivasi. Silakan aktivasi terlebih dahulu"
                    "INVALID_PIN" -> "PIN yang dimasukkan salah"
                    "ACCOUNT_LOCKED" -> "Akun terkunci. Hubungi admin untuk bantuan"
                    "NO_FACE_DETECTED" -> "Wajah tidak terdeteksi pada foto. Pastikan wajah terlihat jelas"
                    "MULTIPLE_FACES" -> "Terdeteksi lebih dari satu wajah. Pastikan hanya ada satu wajah"
                    "FACE_QUALITY_LOW" -> "Kualitas foto wajah kurang baik. Pastikan pencahayaan cukup"
                    "UNAUTHORIZED" -> "Sesi telah berakhir. Silakan login kembali"
                    "TOKEN_EXPIRED" -> "Sesi telah berakhir. Silakan login kembali"
                    else -> if (message.isNotEmpty()) message else defaultMessage
                }
            } else {
                defaultMessage
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error parsing error body", e)
            defaultMessage
        }
    }
    
    /**
     * Check if NIK exists and activation status
     */
    fun checkNik(
        nik: String,
        onSuccess: (CheckNikResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = CheckNikRequest(nik)
        
        apiService.checkNik(request).enqueue(object : Callback<CheckNikResponse> {
            override fun onResponse(
                call: Call<CheckNikResponse>,
                response: Response<CheckNikResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it) }
                        ?: onError("Tidak ada respons dari server")
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Gagal memeriksa NIK"
                    )
                    onError(errorMessage)
                }
            }
            
            override fun onFailure(call: Call<CheckNikResponse>, t: Throwable) {
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Koneksi timeout. Periksa koneksi internet Anda"
                    t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet"
                    else -> "Terjadi kesalahan jaringan"
                }
                onError(errorMessage)
            }
        })
    }
    
    /**
     * Activate account with face photo
     */
    fun activate(
        nik: String,
        pin: String,
        email: String?,
        facePhotoFiles: List<File>,
        onSuccess: (ActivateResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        if (facePhotoFiles.isEmpty()) {
            onError("Aktivasi membutuhkan foto wajah referensi")
            return
        }

        val nikBody = nik.toRequestBody("text/plain".toMediaTypeOrNull())
        val pinBody = pin.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())

        // Multi-pose burst: send every captured frame; the server keeps the clear ones as references.
        val photoParts = facePhotoFiles.mapIndexed { i, file ->
            MultipartBody.Part.createFormData(
                "face_photo_${i + 1}",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        apiService.activate(nikBody, pinBody, emailBody, photoParts)
            .enqueue(object : Callback<ActivateResponse> {
                override fun onResponse(
                    call: Call<ActivateResponse>,
                    response: Response<ActivateResponse>
                ) {
                    Log.d("AuthRepository", "=== ACTIVATE RESPONSE ===")
                    Log.d("AuthRepository", "Response code: ${response.code()}")
                    Log.d("AuthRepository", "Response successful: ${response.isSuccessful}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("AuthRepository", "Response body: $body")
                        
                        if (body?.success == true && body.data != null) {
                            try {
                                Log.d("AuthRepository", "Saving tokens...")
                                // Save tokens
                                val tokens = body.data.tokens
                                tokenManager.saveTokens(
                                    tokens.accessToken,
                                    tokens.refreshToken,
                                    tokens.expiresIn
                                )
                                Log.d("AuthRepository", "Tokens saved")
                                
                                Log.d("AuthRepository", "Saving employee info...")
                                // Save employee info
                                val employee = body.data.employee
                                tokenManager.saveEmployeeInfo(
                                    employee.id,
                                    employee.nik,
                                    employee.nama
                                )
                                Log.d("AuthRepository", "Employee info saved")
                                
                                Log.d("AuthRepository", "Calling onSuccess callback...")
                                onSuccess(body)
                                Log.d("AuthRepository", "onSuccess callback completed")
                            } catch (e: Exception) {
                                Log.e("AuthRepository", "Error saving tokens/employee info", e)
                                e.printStackTrace()
                                onError("Aktivasi berhasil tapi gagal menyimpan data. Error: ${e.message}")
                            }
                        } else {
                            Log.e("AuthRepository", "Response not successful or no data")
                            onError(body?.message ?: "Aktivasi gagal")
                        }
                    } else {
                        Log.e("AuthRepository", "Response not successful: ${response.code()}")
                        val errorMessage = parseErrorMessage(
                            response.errorBody(),
                            "Gagal melakukan aktivasi"
                        )
                        onError(errorMessage)
                    }
                }
                
                override fun onFailure(call: Call<ActivateResponse>, t: Throwable) {
                    Log.e("AuthRepository", "=== ACTIVATE FAILURE ===")
                    Log.e("AuthRepository", "Error: ${t.message}", t)
                    
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

    fun verifyEmailOtp(
        nik: String,
        email: String,
        otp: String,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = VerifyEmailOtpRequest(nik = nik, email = email, otp = otp)
        apiService.verifyEmailOtp(request).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it) } ?: onError("Tidak ada respons dari server")
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Gagal verifikasi OTP"
                    )
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true ->
                        "Koneksi timeout. Periksa koneksi internet Anda"
                    t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet"
                    else -> "Terjadi kesalahan jaringan"
                }
                onError(errorMessage)
            }
        })
    }

    fun requestEmailOtp(
        nik: String,
        email: String,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = RequestEmailOtpRequest(nik = nik, email = email)
        apiService.requestEmailOtp(request).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it) } ?: onError("Tidak ada respons dari server")
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Gagal mengirim OTP"
                    )
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true ->
                        "Koneksi timeout. Periksa koneksi internet Anda"
                    t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet"
                    else -> "Terjadi kesalahan jaringan"
                }
                onError(errorMessage)
            }
        })
    }
    
    /**
     * Login with NIK and PIN
     */
    fun login(
        nik: String,
        pin: String,
        onSuccess: (LoginResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = LoginRequest(nik, pin)
        
        apiService.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(
                call: Call<LoginResponse>,
                response: Response<LoginResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        // Save tokens
                        val tokens = body.data.tokens
                        tokenManager.saveTokens(
                            tokens.accessToken,
                            tokens.refreshToken,
                            tokens.expiresIn
                        )
                        
                        // Save employee info
                        val employee = body.data.employee
                        tokenManager.saveEmployeeInfo(
                            employee.id,
                            employee.nik,
                            employee.nama
                        )
                        
                        onSuccess(body)
                    } else {
                        onError(body?.message ?: "Login gagal")
                    }
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Gagal melakukan login"
                    )
                    onError(errorMessage)
                }
            }
            
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Koneksi timeout. Periksa koneksi internet Anda"
                    t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet"
                    else -> "Terjadi kesalahan jaringan"
                }
                onError(errorMessage)
            }
        })
    }
    
    /**
     * Refresh access token
     */
    fun refreshToken(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val refreshToken = tokenManager.getRefreshToken()
        
        if (refreshToken.isNullOrEmpty()) {
            onError("Sesi tidak valid. Silakan login kembali")
            return
        }
        
        val request = RefreshTokenRequest(refreshToken)
        
        apiService.refreshToken(request).enqueue(object : Callback<RefreshTokenResponse> {
            override fun onResponse(
                call: Call<RefreshTokenResponse>,
                response: Response<RefreshTokenResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        // Update access token
                        val newAccessToken = body.data.accessToken
                        val newRefreshToken = body.data.refreshToken ?: refreshToken
                        val expiresIn = body.data.expiresIn
                        
                        tokenManager.saveTokens(
                            newAccessToken,
                            newRefreshToken,
                            expiresIn
                        )
                        
                        onSuccess()
                    } else {
                        onError(body?.message ?: "Gagal memperbarui sesi")
                    }
                } else {
                    val errorMessage = parseErrorMessage(
                        response.errorBody(),
                        "Sesi telah berakhir. Silakan login kembali"
                    )
                    onError(errorMessage)
                }
            }
            
            override fun onFailure(call: Call<RefreshTokenResponse>, t: Throwable) {
                val errorMessage = when {
                    t.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Koneksi timeout. Periksa koneksi internet Anda"
                    t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet"
                    else -> "Terjadi kesalahan jaringan"
                }
                onError(errorMessage)
            }
        })
    }
    
    /**
     * Logout - clear all tokens
     */
    fun logout() {
        tokenManager.clearTokens()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
    
    /**
     * Check if token needs refresh
     */
    fun needsTokenRefresh(): Boolean {
        return tokenManager.isTokenExpired()
    }
    
    /**
     * Get employee info
     */
    fun getEmployeeInfo(): Triple<Int, String?, String?> {
        return Triple(
            tokenManager.getEmployeeId(),
            tokenManager.getEmployeeNik(),
            tokenManager.getEmployeeName()
        )
    }
}
