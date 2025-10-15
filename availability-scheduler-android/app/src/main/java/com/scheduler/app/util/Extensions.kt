package com.scheduler.app.util

import android.view.View
import com.google.gson.Gson
import com.scheduler.app.data.model.ApiErrorResponse
import com.google.android.material.snackbar.Snackbar
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

// ── Network ───────────────────────────────────────────────────────────────────

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Resource.Success(body)
            } else {
                // 204 No Content or unexpected null
                @Suppress("UNCHECKED_CAST")
                Resource.Success(Unit as T)
            }
        } else {
            Resource.Error(response.errorBody().parseErrorMessage())
        }
    } catch (e: IOException) {
        Resource.Error("Cannot reach server. Make sure the backend is running on port 8080.")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unexpected error")
    }
}

suspend fun safeDeleteCall(call: suspend () -> Response<Void>): Resource<Unit> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            Resource.Success(Unit)
        } else {
            Resource.Error(response.errorBody().parseErrorMessage())
        }
    } catch (e: IOException) {
        Resource.Error("Cannot reach server. Make sure the backend is running on port 8080.")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unexpected error")
    }
}

private fun ResponseBody?.parseErrorMessage(): String {
    return try {
        val json = this?.string() ?: return "Unknown error"
        Gson().fromJson(json, ApiErrorResponse::class.java).message
    } catch (e: Exception) {
        "An unexpected error occurred"
    }
}

// ── View ──────────────────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}
