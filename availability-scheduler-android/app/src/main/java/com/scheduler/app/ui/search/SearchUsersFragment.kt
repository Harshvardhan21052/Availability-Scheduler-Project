package com.scheduler.app.ui.search

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.scheduler.app.R
import com.scheduler.app.databinding.FragmentSearchUsersBinding
import com.scheduler.app.util.Resource
import com.scheduler.app.util.hide
import com.scheduler.app.util.show
import com.scheduler.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchUsersFragment : Fragment(R.layout.fragment_search_users) {

    private var _binding: FragmentSearchUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchUsersViewModel by viewModels()
    private lateinit var adapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchUsersBinding.bind(view)

        adapter = UserAdapter()
        binding.recyclerView.adapter = adapter

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.search(text.toString())
        }

        observeUsers()
    }

    private fun observeUsers() {
        viewModel.users.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.progressBar.show()
                is Resource.Success -> {
                    binding.progressBar.hide()
                    val users = resource.data
                    adapter.submitList(users)
                    if (users.isEmpty() && binding.etSearch.text?.isNotEmpty() == true) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
