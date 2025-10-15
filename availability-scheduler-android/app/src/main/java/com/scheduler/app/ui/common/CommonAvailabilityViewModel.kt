package com.scheduler.app.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scheduler.app.data.model.TimeSlotResponse
import com.scheduler.app.data.repository.AvailabilityRepository
import com.scheduler.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommonAvailabilityViewModel @Inject constructor(
    private val repository: AvailabilityRepository
) : ViewModel() {

    private val _result = MutableLiveData<Resource<List<TimeSlotResponse>>>()
    val result: LiveData<Resource<List<TimeSlotResponse>>> = _result

    fun findCommonSlots(usernamesInput: String, date: String) {
        val usernames = usernamesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (usernames.isEmpty()) {
            _result.value = Resource.Error("Enter at least one username")
            return
        }
        if (date.isEmpty()) {
            _result.value = Resource.Error("Please select a date")
            return
        }

        viewModelScope.launch {
            _result.value = Resource.Loading
            _result.value = repository.getCommonAvailability(usernames.joinToString(","), date)
        }
    }
}
