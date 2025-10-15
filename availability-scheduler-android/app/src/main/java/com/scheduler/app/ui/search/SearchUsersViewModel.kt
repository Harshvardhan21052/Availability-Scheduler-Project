package com.scheduler.app.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scheduler.app.data.model.UserResponse
import com.scheduler.app.data.repository.AvailabilityRepository
import com.scheduler.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchUsersViewModel @Inject constructor(
    private val repository: AvailabilityRepository
) : ViewModel() {

    private val _users = MutableLiveData<Resource<List<UserResponse>>>()
    val users: LiveData<Resource<List<UserResponse>>> = _users

    private var searchJob: Job? = null

    // Debounced search — waits 400ms after user stops typing
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _users.value = Resource.Success(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _users.value = Resource.Loading
            _users.value = repository.searchUsers(query)
        }
    }
}
