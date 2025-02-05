package com.medtroniclabs.opensource.ui.boarding.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams.passwordRegexPattern
import com.medtroniclabs.opensource.databinding.FragmentConfirmPasswordBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.boarding.ForgetPasswordActivity
import com.medtroniclabs.opensource.ui.boarding.viewmodel.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ConfirmPasswordFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentConfirmPasswordBinding
    private val viewModel: ForgotPasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfirmPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "ConfirmPasswordFragment"
        fun newInstance() = ConfirmPasswordFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.btnSubmit.safeClickListener(this)
        binding.tvGoBack.safeClickListener(this)
    }

    private fun validateLoginInputs() {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        binding.tvPasswordError.gone()
        binding.tvConfirmPasswordError.gone()

        //password blank check
        if (password.isBlank()) {
            binding.tvPasswordError.visible()
            binding.tvPasswordError.text = getString(R.string.password_cannot_be_empty)
            return
        }

        //confirmPassword blank check
        if (confirmPassword.isBlank()) {
            binding.tvConfirmPasswordError.visible()
            binding.tvConfirmPasswordError.text =
                getString(R.string.confirm_password_cannot_be_empty)
            return
        }

        //Both password equal check
        if (!password.equals(confirmPassword, false)) {
            binding.tvConfirmPasswordError.visibility = View.VISIBLE
            binding.tvConfirmPasswordError.text = getString(R.string.password_does_not_match)
            return
        }

        //Regex check
        if (!isPasswordValid(password)) {
            showErrorDialog(
                title = getString(R.string.alert),
                message = getString(R.string.password_error_message)
            )
            return
        }

        viewModel.resetPassword(password)
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = passwordRegexPattern
        val regex = Regex(passwordRegex)
        return regex.matches(password)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnSubmit -> {
                hideKeyboard()
                validateLoginInputs()
            }

            R.id.tvGoBack -> {
              if(activity!= null && activity is ForgetPasswordActivity) {
                  (activity as ForgetPasswordActivity?)?.redirectToLogin()
              }
            }
        }
    }

    private fun hideKeyboard() {
        val hide =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let { v ->
            hide.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

}