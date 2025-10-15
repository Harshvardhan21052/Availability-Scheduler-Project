package com.scheduler.app.ui.availability

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.scheduler.app.R
import com.scheduler.app.data.model.BusySlotResponse
import com.scheduler.app.databinding.DialogSlotBinding
import com.scheduler.app.databinding.FragmentMyAvailabilityBinding
import com.scheduler.app.util.Resource
import com.scheduler.app.util.hide
import com.scheduler.app.util.show
import com.scheduler.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class MyAvailabilityFragment : Fragment(R.layout.fragment_my_availability) {

    private var _binding: FragmentMyAvailabilityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AvailabilityViewModel by viewModels()
    private lateinit var adapter: BusySlotAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyAvailabilityBinding.bind(view)

        setupRecyclerView()
        observeSlots()
        observeActionResult()

        binding.fabAddSlot.setOnClickListener { showSlotDialog(null) }
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadSlots() }

        viewModel.loadSlots()
    }

    private fun setupRecyclerView() {
        adapter = BusySlotAdapter(
            onEdit   = { slot -> showSlotDialog(slot) },
            onDelete = { slot -> confirmDelete(slot) }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun observeSlots() {
        viewModel.slots.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefresh.isRefreshing = false
            when (resource) {
                is Resource.Loading -> binding.progressBar.show()
                is Resource.Success -> {
                    binding.progressBar.hide()
                    val slots = resource.data
                    adapter.submitList(slots)
                    if (slots.isEmpty()) {
                        binding.tvEmpty.show()
                        binding.recyclerView.hide()
                    } else {
                        binding.tvEmpty.hide()
                        binding.recyclerView.show()
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.hide()
                    binding.root.showSnackbar(resource.message)
                }
            }
        }
    }

    private fun observeActionResult() {
        viewModel.actionResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> binding.root.showSnackbar("Saved successfully")
                is Resource.Error   -> binding.root.showSnackbar(resource.message)
                else -> {}
            }
        }
    }

    // ── Slot Dialog (Add / Edit) ───────────────────────────────────────────────

    private fun showSlotDialog(existing: BusySlotResponse?) {
        val dialogBinding = DialogSlotBinding.inflate(layoutInflater)
        var selectedDate      = existing?.date      ?: ""
        var selectedStartTime = existing?.startTime ?: ""
        var selectedEndTime   = existing?.endTime   ?: ""

        // Pre-fill for edits
        dialogBinding.tvSelectedDate.text      = selectedDate.ifEmpty { "Tap to pick" }
        dialogBinding.tvSelectedStart.text     = selectedStartTime.ifEmpty { "Tap to pick" }
        dialogBinding.tvSelectedEnd.text       = selectedEndTime.ifEmpty { "Tap to pick" }

        dialogBinding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(),
                { _, y, m, d ->
                    selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                    dialogBinding.tvSelectedDate.text = selectedDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).also { it.datePicker.minDate = System.currentTimeMillis() }.show()
        }

        dialogBinding.btnPickStart.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(requireContext(),
                { _, h, m ->
                    selectedStartTime = "%02d:%02d".format(h, m)
                    dialogBinding.tvSelectedStart.text = selectedStartTime
                },
                cal.get(Calendar.HOUR_OF_DAY), 0, true
            ).show()
        }

        dialogBinding.btnPickEnd.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(requireContext(),
                { _, h, m ->
                    selectedEndTime = "%02d:%02d".format(h, m)
                    dialogBinding.tvSelectedEnd.text = selectedEndTime
                },
                cal.get(Calendar.HOUR_OF_DAY) + 1, 0, true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Add Busy Slot" else "Edit Busy Slot")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                when {
                    selectedDate.isEmpty()      -> binding.root.showSnackbar("Please pick a date")
                    selectedStartTime.isEmpty() -> binding.root.showSnackbar("Please pick start time")
                    selectedEndTime.isEmpty()   -> binding.root.showSnackbar("Please pick end time")
                    else -> {
                        if (existing == null) {
                            viewModel.createSlot(selectedDate, selectedStartTime, selectedEndTime)
                        } else {
                            viewModel.updateSlot(existing.id, selectedDate, selectedStartTime, selectedEndTime)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(slot: BusySlotResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Slot")
            .setMessage("Delete ${slot.date} ${slot.startTime}–${slot.endTime}?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteSlot(slot.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
