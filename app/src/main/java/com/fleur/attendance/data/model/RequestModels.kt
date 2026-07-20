package com.fleur.attendance.data.model

import com.google.gson.annotations.SerializedName

// ===== AUTHENTICATION REQUEST MODELS =====

data class CheckNikRequest(
    val nik: String
)

data class LoginRequest(
    val nik: String,
    val pin: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class VerifyEmailOtpRequest(
    val nik: String,
    val email: String,
    val otp: String
)

data class RequestEmailOtpRequest(
    val nik: String,
    val email: String
)

// ===== PIN MANAGEMENT REQUEST MODELS =====

data class ValidatePinRequest(
    val pin: String
)

data class ChangePinRequest(
    @SerializedName("current_pin") 
    val currentPin: String,
    @SerializedName("new_pin") 
    val newPin: String
)

// ===== LOCATION REQUEST MODELS =====

data class ValidateLocationRequest(
    val latitude: Double,
    val longitude: Double
)

data class BreakEndRequest(
    val latitude: Double,
    val longitude: Double
)

// ===== LEAVE REQUEST MODELS =====

data class LeaveRequestPayload(
    val jenis: String,
    @SerializedName("leave_type")
    val leaveType: String = "planned",
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String,
    @SerializedName("jam_mulai")
    val jamMulai: String? = null,
    @SerializedName("jam_selesai")
    val jamSelesai: String? = null,
    @SerializedName("id_pengganti")
    val idPengganti: Int? = null,
    val alasan: String
)

data class PendingCountData(
    val pending: Int = 0
)
