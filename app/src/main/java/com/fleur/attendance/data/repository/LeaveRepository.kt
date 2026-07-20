package com.fleur.attendance.data.repository

import android.content.Context
import com.fleur.attendance.data.api.ApiConfig
import com.fleur.attendance.data.api.ApiService
import com.fleur.attendance.data.model.ApiResponse
import com.fleur.attendance.data.model.LeaveRequestPayload
import com.fleur.attendance.data.model.LeaveRequestResponse
import com.fleur.attendance.data.model.LeaveRequestsResponse
import com.fleur.attendance.data.model.PendingCountData
import com.fleur.attendance.data.model.ReplacementCandidatesResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class LeaveRepository(private val context: Context) {

    private val apiService: ApiService = ApiConfig.getApiService()

    /**
     * Ambil pesan error asli dari body respons API (JSON {success, message, code}).
     * Server mengirim alasan spesifik; jangan ditelan jadi pesan generik.
     */
    private fun parseErrorMessage(errorBody: okhttp3.ResponseBody?, defaultMessage: String): String {
        return try {
            val errorJson = errorBody?.string()
            if (errorJson != null) {
                val jsonObject = JSONObject(errorJson)
                val message = jsonObject.optString("message", "")
                val code = jsonObject.optString("code", "")
                when (code) {
                    "LEAVE_DATE_OVERLAP" -> "Tanggal pengajuan bentrok dengan pengajuan lain yang masih aktif"
                    "REPLACEMENT_NOT_FOUND" -> "Karyawan pengganti tidak ditemukan atau tidak aktif"
                    "INVALID_REPLACEMENT_EMPLOYEE" -> "Karyawan pengganti tidak boleh sama dengan pengaju"
                    "SICK_NOTE_REQUIRED" -> "Surat keterangan sakit wajib dilampirkan"
                    "MISSING_PARTIAL_TIME" -> "Jam mulai dan jam selesai wajib diisi"
                    "MISSING_LEAVE_DATA" -> "Tanggal dan alasan wajib diisi"
                    "INVALID_LEAVE_TYPE" -> "Jenis harus cuti, izin, atau sakit"
                    "INVALID_LEAVE_REQUEST_TYPE" -> "Tipe pengajuan harus Terencana atau Mendadak"
                    "REPLACEMENT_REQUIRED_FOR_PLANNED" -> "Pengajuan Terencana wajib memilih karyawan pengganti"
                    "SUBSTITUTE_NOT_ALLOWED_FOR_URGENT" -> "Pengajuan Mendadak tidak memilih pengganti; pengganti ditentukan Manager"
                    "INVALID_DATE_RANGE" -> "Rentang tanggal tidak valid"
                    "UNAUTHORIZED", "TOKEN_EXPIRED" -> "Sesi telah berakhir. Silakan login kembali"
                    else -> if (message.isNotEmpty()) message else defaultMessage
                }
            } else {
                defaultMessage
            }
        } catch (e: Exception) {
            defaultMessage
        }
    }

    fun getLeaveRequests(
        onSuccess: (LeaveRequestsResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getLeaveRequests().enqueue(object : Callback<LeaveRequestsResponse> {
            override fun onResponse(
                call: Call<LeaveRequestsResponse>,
                response: Response<LeaveRequestsResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError(parseErrorMessage(response.errorBody(), "Gagal mengambil riwayat Absensi"))
                }
            }

            override fun onFailure(call: Call<LeaveRequestsResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun getReplacementRequests(
        onSuccess: (LeaveRequestsResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getReplacementRequests().enqueue(object : Callback<LeaveRequestsResponse> {
            override fun onResponse(
                call: Call<LeaveRequestsResponse>,
                response: Response<LeaveRequestsResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError(parseErrorMessage(response.errorBody(), "Gagal mengambil daftar approval pengganti"))
                }
            }

            override fun onFailure(call: Call<LeaveRequestsResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun getPendingReplacementCount(
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getPendingReplacementCount().enqueue(object : Callback<ApiResponse<PendingCountData>> {
            override fun onResponse(
                call: Call<ApiResponse<PendingCountData>>,
                response: Response<ApiResponse<PendingCountData>>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.body()?.data?.pending ?: 0)
                } else {
                    onError(parseErrorMessage(response.errorBody(), "Gagal mengambil jumlah permintaan pengganti"))
                }
            }

            override fun onFailure(call: Call<ApiResponse<PendingCountData>>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun getReplacementCandidates(
        query: String? = null,
        onSuccess: (ReplacementCandidatesResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.getReplacementCandidates(query).enqueue(object : Callback<ReplacementCandidatesResponse> {
            override fun onResponse(
                call: Call<ReplacementCandidatesResponse>,
                response: Response<ReplacementCandidatesResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError(parseErrorMessage(response.errorBody(), "Gagal mengambil daftar karyawan pengganti"))
                }
            }

            override fun onFailure(call: Call<ReplacementCandidatesResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun createLeaveRequest(
        payload: LeaveRequestPayload,
        attachmentFile: File? = null,
        onSuccess: (LeaveRequestResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val kategori = if (!payload.jamMulai.isNullOrBlank() || !payload.jamSelesai.isNullOrBlank()) {
            "hourly"
        } else {
            "full_day"
        }

        val attachmentPart = attachmentFile?.let { file ->
            val mediaType = when (file.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                else -> "image/jpeg"
            }
            val fileBody = file.asRequestBody(mediaType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("lampiran", file.name, fileBody)
        }

        apiService.createLeaveRequest(
            jenis = payload.jenis.toRequestBody("text/plain".toMediaTypeOrNull()),
            leaveType = payload.leaveType.toRequestBody("text/plain".toMediaTypeOrNull()),
            kategori = kategori.toRequestBody("text/plain".toMediaTypeOrNull()),
            tanggalMulai = payload.tanggalMulai.toRequestBody("text/plain".toMediaTypeOrNull()),
            tanggalSelesai = payload.tanggalSelesai.toRequestBody("text/plain".toMediaTypeOrNull()),
            jamMulai = payload.jamMulai?.toRequestBody("text/plain".toMediaTypeOrNull()),
            jamSelesai = payload.jamSelesai?.toRequestBody("text/plain".toMediaTypeOrNull()),
            idPengganti = payload.idPengganti?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
            alasan = payload.alasan.toRequestBody("text/plain".toMediaTypeOrNull()),
            lampiran = attachmentPart
        ).enqueue(object : Callback<LeaveRequestResponse> {
            override fun onResponse(
                call: Call<LeaveRequestResponse>,
                response: Response<LeaveRequestResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError(parseErrorMessage(response.errorBody(), "Gagal mengirim pengajuan Absensi"))
                }
            }

            override fun onFailure(call: Call<LeaveRequestResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun approveReplacementRequest(
        requestId: Int,
        note: String? = null,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.approveReplacementRequest(requestId, note).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(
                call: Call<ApiResponse<Any>>,
                response: Response<ApiResponse<Any>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError("Gagal menyetujui permintaan pengganti")
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun rejectReplacementRequest(
        requestId: Int,
        note: String? = null,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.rejectReplacementRequest(requestId, note).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(
                call: Call<ApiResponse<Any>>,
                response: Response<ApiResponse<Any>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError("Gagal menolak permintaan pengganti")
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }

    fun cancelLeaveRequest(
        requestId: Int,
        onSuccess: (ApiResponse<Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.cancelLeaveRequest(requestId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(
                call: Call<ApiResponse<Any>>,
                response: Response<ApiResponse<Any>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let(onSuccess) ?: onError("Tidak ada respons dari server")
                } else {
                    onError("Gagal membatalkan pengajuan")
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }
}
