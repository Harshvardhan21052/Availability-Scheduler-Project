package com.scheduler.app.ui.home

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.scheduler.app.R
import com.scheduler.app.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.tvWelcome.text = "Welcome, ${viewModel.getUsername()} 👋"

        binding.cardMySlots.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myAvailabilityFragment)
        }
        binding.cardCommon.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_commonAvailabilityFragment)
        }
        binding.cardSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchUsersFragment)
        }

        setupMenu()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        viewModel.logout()
                        findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
