package com.scheduler.app.data.remote

import com.scheduler.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // ── Availability ──────────────────────────────────────────────────────────

    @POST("availability")
    suspend fun createSlot(@Body request: BusySlotRequest): Response<BusySlotResponse>

    @GET("availability/my")
    suspend fun getMySlots(): Response<List<BusySlotResponse>>

    @PUT("availability/{id}")
    suspend fun updateSlot(
        @Path("id") id: Long,
        @Body request: BusySlotRequest
    ): Response<BusySlotResponse>

    @DELETE("availability/{id}")
    suspend fun deleteSlot(@Path("id") id: Long): Response<Void>

    @GET("availability/common")
    suspend fun getCommonAvailability(
        @Query("usernames") usernames: String,  // comma-separated: "alice,bob"
        @Query("date") date: String             // "yyyy-MM-dd"
    ): Response<List<TimeSlotResponse>>

    // ── Users ─────────────────────────────────────────────────────────────────

    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<UserResponse>>
}
