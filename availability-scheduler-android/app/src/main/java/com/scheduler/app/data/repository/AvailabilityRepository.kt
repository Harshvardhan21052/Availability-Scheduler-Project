package com.scheduler.app.data.repository

import com.scheduler.app.data.model.BusySlotRequest
import com.scheduler.app.data.model.BusySlotResponse
import com.scheduler.app.data.model.TimeSlotResponse
import com.scheduler.app.data.model.UserResponse
import com.scheduler.app.data.remote.ApiService
import com.scheduler.app.util.Resource
import com.scheduler.app.util.safeApiCall
import com.scheduler.app.util.safeDeleteCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvailabilityRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getMySlots(): Resource<List<BusySlotResponse>> =
        safeApiCall { apiService.getMySlots() }

    suspend fun createSlot(request: BusySlotRequest): Resource<BusySlotResponse> =
        safeApiCall { apiService.createSlot(request) }

    suspend fun updateSlot(id: Long, request: BusySlotRequest): Resource<BusySlotResponse> =
        safeApiCall { apiService.updateSlot(id, request) }

    suspend fun deleteSlot(id: Long): Resource<Unit> =
        safeDeleteCall { apiService.deleteSlot(id) }

    suspend fun getCommonAvailability(
        usernames: String,
        date: String
    ): Resource<List<TimeSlotResponse>> =
        safeApiCall { apiService.getCommonAvailability(usernames, date) }

    suspend fun searchUsers(query: String): Resource<List<UserResponse>> =
        safeApiCall { apiService.searchUsers(query) }
}
