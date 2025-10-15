package com.scheduler.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.scheduler.app.R
import com.scheduler.app.databinding.FragmentLoginBinding
import com.scheduler.app.util.Resource
import com.scheduler.app.util.hide
import com.scheduler.app.util.show
import com.scheduler.app.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // Skip login screen if already authenticated
        if (viewModel.isLoggedIn()) {
            navigateToHome()
            return
        }

        setupClickListeners()
        observeLoginResult()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(username, password)
        }

        binding.tvGoToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observeLoginResult() {
        viewModel.loginResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.show()
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.hide()
                    binding.btnLogin.isEnabled = true
                    navigateToHome()
                }
                is Resource.Error -> {
                    binding.progressBar.hide()
                    binding.btnLogin.isEnabled = true
                    binding.root.showSnackbar(resource.message)
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
