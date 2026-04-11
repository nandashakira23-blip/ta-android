package com.fleur.attendance.data.repository

import android.content.Context
import com.fleur.attendance.data.api.ApiConfig
import com.fleur.attendance.data.api.ApiService
import com.fleur.attendance.data.model.LeaveRequestPayload
import com.fleur.attendance.data.model.LeaveRequestResponse
import com.fleur.attendance.data.model.LeaveRequestsResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class LeaveRepository(private val context: Context) {

    private val apiService: ApiService = ApiConfig.getApiService()

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
                    onError("Gagal mengambil riwayat izin")
                }
            }

            override fun onFailure(call: Call<LeaveRequestsResponse>, t: Throwable) {
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
            kategori = kategori.toRequestBody("text/plain".toMediaTypeOrNull()),
            tanggalMulai = payload.tanggalMulai.toRequestBody("text/plain".toMediaTypeOrNull()),
            tanggalSelesai = payload.tanggalSelesai.toRequestBody("text/plain".toMediaTypeOrNull()),
            jamMulai = payload.jamMulai?.toRequestBody("text/plain".toMediaTypeOrNull()),
            jamSelesai = payload.jamSelesai?.toRequestBody("text/plain".toMediaTypeOrNull()),
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
                    onError("Gagal mengirim pengajuan izin")
                }
            }

            override fun onFailure(call: Call<LeaveRequestResponse>, t: Throwable) {
                onError(t.message ?: "Kesalahan jaringan")
            }
        })
    }
}
