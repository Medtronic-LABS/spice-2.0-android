package com.medtroniclabs.opensource.ncd.screening.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.DialogESignatureBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.registration.viewmodel.TermsAndConditionsViewModel
import com.medtroniclabs.opensource.ncd.screening.utils.SignatureInterface
import com.medtroniclabs.opensource.signature.view.SignatureView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ESignatureDialog(private val signatureInterface: SignatureInterface) : DialogFragment(),
    View.OnClickListener {

    private lateinit var binding: DialogESignatureBinding
    private val viewModel: TermsAndConditionsViewModel by viewModels()

    private var isSigned: Boolean = false

    companion object {
        const val TAG = "ESignatureDialog"
        fun newInstance(signatureInterface: SignatureInterface): ESignatureDialog {
            return ESignatureDialog(signatureInterface)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogESignatureBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        attachObservers()
        setListeners()
    }

    private fun attachObservers() {
        binding.etUserInitial.addTextChangedListener { patientInitial ->
            if (patientInitial.isNullOrBlank()) {
                viewModel.patientInitial.value = null
            } else {
                viewModel.patientInitial.value = patientInitial.trim().toString()
            }
            enableSubmitBtn()
        }
    }

    private fun enableSubmitBtn() {
        binding.btnConfirm.isEnabled = (viewModel.patientInitial.value != null) || isSigned
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleConfiguration()

    }

    override fun onStart() {
        super.onStart()
        handleConfiguration()
    }

    private fun handleConfiguration() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        when {
            isTablet && isLandscape -> setDialogPercent(55, 95)
            isTablet -> setDialogPercent(85, 55)
            else -> setDialogPercent(95, 65)
        }
    }

    private fun setListeners() {
        binding.btnClearSign.safeClickListener(this)
        binding.clTitleCard.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnConfirm.safeClickListener(this)
        binding.signatureView.setOnSignedListener(signListener)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnClearSign.id -> binding.signatureView.clear()
            binding.btnCancel.id -> dismiss()
            binding.clTitleCard.ivClose.id -> dismiss()
            binding.btnConfirm.id -> {
                if (isSigned()) {
                    signatureInterface.applySignature(
                        binding.signatureView.getSignatureBitmap(),
                        viewModel.patientInitial.value
                    )
                    dismiss()
                } else {
                    signatureInterface.applySignature(null, viewModel.patientInitial.value)
                    dismiss()
                }
                viewModel.patientInitial.value = null
            }
        }
    }

    private val signListener = object : SignatureView.OnSignedListener {
        override fun onStartSigning() {
            binding.tvErrorSignature.visibility = View.GONE
        }

        override fun onSigned() {
            isSigned = true
            binding.tvErrorSignature.visibility = View.GONE
            enableSubmitBtn()
        }

        override fun onClear() {
            isSigned = false
            binding.tvErrorSignature.visibility = View.GONE
            enableSubmitBtn()
        }
    }

    private fun isSigned(): Boolean {
        var signed = true
        if (!isSigned) {
            signed = false
        }
        return signed
    }
}