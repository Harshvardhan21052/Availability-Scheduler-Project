package com.scheduler.app.data.repository

import com.scheduler.app.data.local.TokenManager
import com.scheduler.app.data.model.AuthResponse
import com.scheduler.app.data.model.LoginRequest
import com.scheduler.app.data.model.SignupRequest
import com.scheduler.app.data.remote.ApiService
import com.scheduler.app.util.Resource
import com.scheduler.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun signup(username: String, password: String): Resource<AuthResponse> {
        val result = safeApiCall { apiService.signup(SignupRequest(username, password)) }
        if (result is Resource.Success) saveSession(result.data)
        return result
    }

    suspend fun login(username: String, password: String): Resource<AuthResponse> {
        val result = safeApiCall { apiService.login(LoginRequest(username, password)) }
        if (result is Resource.Success) saveSession(result.data)
        return result
    }

    fun logout() = tokenManager.clear()

    fun isLoggedIn() = tokenManager.isLoggedIn()

    fun getUsername() = tokenManager.getUsername() ?: ""

    private fun saveSession(response: AuthResponse) {
        tokenManager.saveToken(response.token)
        tokenManager.saveUsername(response.username)
    }
}
