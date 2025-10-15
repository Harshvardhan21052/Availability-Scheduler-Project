package com.scheduler.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.scheduler.app.R
import com.scheduler.app.databinding.FragmentSignupBinding
import com.scheduler.app.util.Resource
import com.scheduler.app.util.hide
import com.scheduler.app.util.show
import com.scheduler.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupFragment : Fragment(R.layout.fragment_signup) {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignupViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignupBinding.bind(view)

        setupClickListeners()
        observeSignupResult()
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.signup(username, password)
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeSignupResult() {
        viewModel.signupResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.show()
                    binding.btnSignup.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.hide()
                    binding.btnSignup.isEnabled = true
                    // Signup auto-logs in — go straight to home
                    findNavController().navigate(R.id.action_signupFragment_to_homeFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.hide()
                    binding.btnSignup.isEnabled = true
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
