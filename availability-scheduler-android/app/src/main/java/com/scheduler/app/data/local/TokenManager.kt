package com.scheduler.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUsername(username: String) = prefs.edit().putString(KEY_USERNAME, username).apply()

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val PREFS_NAME = "scheduler_prefs"
        private const val KEY_TOKEN   = "jwt_token"
        private const val KEY_USERNAME = "username"
    }
}
