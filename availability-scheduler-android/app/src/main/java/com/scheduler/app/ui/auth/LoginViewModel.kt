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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<Resource<AuthResponse>>()
    val loginResult: LiveData<Resource<AuthResponse>> = _loginResult

    fun login(username: String, password: String) {
        if (!validateInput(username, password)) return
        viewModelScope.launch {
            _loginResult.value = Resource.Loading
            _loginResult.value = authRepository.login(username, password)
        }
    }

    fun isLoggedIn() = authRepository.isLoggedIn()

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isBlank()) {
            _loginResult.value = Resource.Error("Username is required")
            return false
        }
        if (password.isBlank()) {
            _loginResult.value = Resource.Error("Password is required")
            return false
        }
        return true
    }
}
