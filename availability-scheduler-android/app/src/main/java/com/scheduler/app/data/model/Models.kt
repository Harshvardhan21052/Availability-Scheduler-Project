package com.scheduler.app.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class SignupRequest(
    val username: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class BusySlotRequest(
    val date: String,         // "yyyy-MM-dd"
    val startTime: String,    // "HH:mm"
    val endTime: String       // "HH:mm"
)

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class AuthResponse(
    val token: String,
    val username: String,
    val message: String
)

@Parcelize
data class BusySlotResponse(
    val id: Long,
    val date: String,         // "yyyy-MM-dd"
    val startTime: String,    // "HH:mm"
    val endTime: String       // "HH:mm"
) : Parcelable

data class TimeSlotResponse(
    val startTime: String,    // "HH:mm"
    val endTime: String       // "HH:mm"
)

data class UserResponse(
    val id: Long,
    val username: String
)

data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    @SerializedName("fieldErrors")
    val fieldErrors: Map<String, String>?
)
