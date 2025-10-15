package com.scheduler.app.ui.home

import androidx.lifecycle.ViewModel
import com.scheduler.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun getUsername(): String = authRepository.getUsername()

    fun logout() = authRepository.logout()
}
