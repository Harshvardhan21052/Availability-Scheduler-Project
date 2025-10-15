package com.scheduler.app.ui.availability

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scheduler.app.data.model.BusySlotRequest
import com.scheduler.app.data.model.BusySlotResponse
import com.scheduler.app.data.repository.AvailabilityRepository
import com.scheduler.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val repository: AvailabilityRepository
) : ViewModel() {

    private val _slots = MutableLiveData<Resource<List<BusySlotResponse>>>()
    val slots: LiveData<Resource<List<BusySlotResponse>>> = _slots

    private val _actionResult = MutableLiveData<Resource<Unit>>()
    val actionResult: LiveData<Resource<Unit>> = _actionResult

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadSlots() {
        viewModelScope.launch {
            _slots.value = Resource.Loading
            _slots.value = repository.getMySlots()
        }
    }

    // ── Create ────────────────────────────────────────────────────────────────

    fun createSlot(date: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            val result = repository.createSlot(BusySlotRequest(date, startTime, endTime))
            _actionResult.value = when (result) {
                is Resource.Success -> { loadSlots(); Resource.Success(Unit) }
                is Resource.Error   -> result
                Resource.Loading    -> Resource.Loading
            }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    fun updateSlot(id: Long, date: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            val result = repository.updateSlot(id, BusySlotRequest(date, startTime, endTime))
            _actionResult.value = when (result) {
                is Resource.Success -> { loadSlots(); Resource.Success(Unit) }
                is Resource.Error   -> result
                Resource.Loading    -> Resource.Loading
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteSlot(id: Long) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            val result = repository.deleteSlot(id)
            _actionResult.value = when (result) {
                is Resource.Success -> { loadSlots(); Resource.Success(Unit) }
                is Resource.Error   -> result
                Resource.Loading    -> Resource.Loading
            }
        }
    }
}
