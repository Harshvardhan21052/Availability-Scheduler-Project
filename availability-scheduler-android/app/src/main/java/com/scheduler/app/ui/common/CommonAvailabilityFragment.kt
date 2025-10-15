package com.scheduler.app.ui.common

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.scheduler.app.R
import com.scheduler.app.databinding.FragmentCommonAvailabilityBinding
import com.scheduler.app.util.Resource
import com.scheduler.app.util.hide
import com.scheduler.app.util.show
import com.scheduler.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class CommonAvailabilityFragment : Fragment(R.layout.fragment_common_availability) {

    private var _binding: FragmentCommonAvailabilityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommonAvailabilityViewModel by viewModels()

    private var selectedDate = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommonAvailabilityBinding.bind(view)

        binding.btnPickDate.setOnClickListener { pickDate() }

        binding.btnFind.setOnClickListener {
            val usernames = binding.etUsernames.text.toString().trim()
            viewModel.findCommonSlots(usernames, selectedDate)
        }

        observeResult()
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, y, m, d ->
                selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                binding.tvSelectedDate.text = selectedDate
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).also { it.datePicker.minDate = System.currentTimeMillis() }.show()
    }

    private fun observeResult() {
        viewModel.result.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.show()
                    binding.tvResult.hide()
                }
                is Resource.Success -> {
                    binding.progressBar.hide()
                    val slots = resource.data
                    if (slots.isEmpty()) {
                        binding.tvResult.text = "No common free slots found for this date."
                    } else {
                        val sb = StringBuilder("Common free slots:\n\n")
                        slots.forEach { sb.append("🟢  ${it.startTime}  –  ${it.endTime}\n\n") }
                        binding.tvResult.text = sb.toString().trimEnd()
                    }
                    binding.tvResult.show()
                }
                is Resource.Error -> {
                    binding.progressBar.hide()
                    binding.root.showSnackbar(resource.message)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
