package com.scheduler.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scheduler.app.data.model.AuthResponse
import com.scheduler.app.data.repository.AuthRepository
import com.scheduler.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _signupResult = MutableLiveData<Resource<AuthResponse>>()
    val signupResult: LiveData<Resource<AuthResponse>> = _signupResult

    fun signup(username: String, password: String) {
        if (!validateInput(username, password)) return
        viewModelScope.launch {
            _signupResult.value = Resource.Loading
            _signupResult.value = authRepository.signup(username, password)
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isBlank()) {
            _signupResult.value = Resource.Error("Username is required")
            return false
        }
        if (username.length < 3) {
            _signupResult.value = Resource.Error("Username must be at least 3 characters")
            return false
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _signupResult.value = Resource.Error("Username may only contain letters, numbers, and underscores")
            return false
        }
        if (password.length < 6) {
            _signupResult.value = Resource.Error("Password must be at least 6 characters")
            return false
        }
        return true
    }
}
