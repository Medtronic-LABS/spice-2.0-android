package com.medtroniclabs.opensource.ui.boarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.Validator
import com.medtroniclabs.opensource.databinding.FragmentEmailBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.boarding.viewmodel.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentEmailBinding
    private val viewModel: ForgotPasswordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "EmailFragment"
        fun newInstance() = ResetPasswordFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvEmailError.gone()
        initView()
    }

    private fun initView() {
        binding.btnSubmit.safeClickListener(this)
        binding.tvGoBack.safeClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnSubmit -> {
                validateEmailInputs()
            }

            R.id.tvGoBack -> {
                activity?.finish()
            }
        }
    }

    private fun validateEmailInputs() {
        val emailOrPhoneNumber = binding.etEmail.text.toString().trim()

        if (emailOrPhoneNumber.isBlank()) {
            binding.tvEmailError.visible()
            binding.tvEmailError.text = getString(R.string.email_cannot_be_empty)
            return
        }

        if (emailOrPhoneNumber.contains(DefinedParams.AT_CHAR)) {
            if (!Validator.isEmailValid(emailOrPhoneNumber)) {
                binding.tvEmailError.visible()
                binding.tvEmailError.text = getString(R.string.email_phone_invalid)
                return
            }
        } else {
            if (!(Validator.isValidMobileNumber(emailOrPhoneNumber))) {
                binding.tvEmailError.visible()
                binding.tvEmailError.text = getString(R.string.email_phone_invalid)
                return
            }
        }
        binding.tvEmailError.gone()
        withNetworkAvailability(online = { viewModel.resetEmail(emailOrPhoneNumber) })
    }
}

