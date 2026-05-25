package org.medtroniclabs.uhis.ui.household.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.EnterSignatureorInitial
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.saveBitmapAsJpeg
import org.medtroniclabs.uhis.appextensions.setTextChangeListener
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.FhirMemberID
import org.medtroniclabs.uhis.common.DefinedParams.MEMBER_ID
import org.medtroniclabs.uhis.common.DefinedParams.VILLAGE_ID
import org.medtroniclabs.uhis.common.DefinedParams.isCreateHouseholdForPhu
import org.medtroniclabs.uhis.databinding.DialogConsentSignatureBinding
import org.medtroniclabs.uhis.signature.view.SignatureView
import org.medtroniclabs.uhis.ui.household.HouseholdActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW
import org.medtroniclabs.uhis.ui.household.viewmodel.ConsentFormViewModel

class ConsentSignatureDialogFragment : DialogFragment() {
    private lateinit var binding: DialogConsentSignatureBinding
    private var isSigned: Boolean = false
    private val viewModel: ConsentFormViewModel by activityViewModels()

    companion object {
        const val TAG = "ConsentSignatureDialogFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogConsentSignatureBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        setListeners()
        initObserver()
        viewModel.disableConfirm()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun initializeView() {
        isCancelable = false
        viewModel.setUserJourney(EnterSignatureorInitial)
    }

    private fun setListeners() {
        binding.etUserInitial.setTextChangeListener {
            viewModel.enableForInitial((it != null && it.trim().isNotEmpty()))
        }

        binding.signatureView.setOnSignedListener(signListener)
        binding.btnClearSign.setOnClickListener {
            binding.signatureView.clear()
        }

        binding.clTitleCard.ivClose.setOnClickListener {
            viewModel.setUserJourney(AnalyticsDefinedParams.SIGNATUREDIALOGUEDISMISS)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            viewModel.setUserJourney(AnalyticsDefinedParams.SIGNATUREDIALOGUECANCEL)
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            viewModel.setUserJourney(AnalyticsDefinedParams.SIGNATUREDIALOGUECONFIRM)
            onConfirm()
        }
    }

    private fun initObserver() {
        viewModel.enableConfirmLiveData.observe(viewLifecycleOwner) {
            binding.btnConfirm.isEnabled = (it.first || it.second)
        }
    }

    private fun onConfirm() {
        val initialSigned = validateSignOrInitial()
        if (!initialSigned.first && !initialSigned.second) {
            return
        }

        var fileName: String? = null
        var initial: String? = null

        if (initialSigned.first) {
            initial = binding.etUserInitial.text
                .toString()
                .trim()
        }

        if (initialSigned.second) {
            val signatureBitmap = binding.signatureView.getSignatureBitmap()
            fileName = "${System.currentTimeMillis()}"
            val isSaved = requireActivity().saveBitmapAsJpeg(signatureBitmap, fileName)
            if (!isSaved) { // Not saved image
                return
            }
        }

        dismiss()
        requireActivity().finish()
        val intent = Intent(requireContext(), HouseholdActivity::class.java)
        intent.putExtra(VILLAGE_ID, arguments?.getLong(VILLAGE_ID, -1L))
        intent.putExtra(MEMBER_ID, arguments?.getLong(MEMBER_ID, -1L))
        intent.putExtra(FhirMemberID, arguments?.getLong(FhirMemberID, -1L))
        intent.putExtra(IS_PHU_WALK_INS_FLOW, arguments?.getBoolean(IS_PHU_WALK_INS_FLOW, false))
        intent.putExtra(isCreateHouseholdForPhu, arguments?.getBoolean(isCreateHouseholdForPhu, false))
        intent.putExtra(DefinedParams.KeySignature, fileName)
        intent.putExtra(DefinedParams.KEY_INITIAL, initial)
        startActivity(intent)
    }

    private val signListener = object : SignatureView.OnSignedListener {
        override fun onStartSigning() {
            viewModel.enableForSignature(true)
            binding.tvErrorSignature.visibility = View.GONE
        }

        override fun onSigned() {
            isSigned = true
            binding.tvErrorSignature.visibility = View.GONE
        }

        override fun onClear() {
            viewModel.enableForSignature(false)
            isSigned = false
            binding.tvErrorSignature.visibility = View.GONE
        }
    }

    private fun validateSignOrInitial(): Pair<Boolean, Boolean> {
        binding.tvErrorSignature.gone()
        val hasValidInitial = binding.etUserInitial.text
            .toString()
            .trim()
            .isNotEmpty()
        val isSigned = isSigned()

        if (!hasValidInitial && !isSigned) {
            binding.tvErrorSignature.visible()
        }

        return Pair(hasValidInitial, isSigned)
    }

    private fun isSigned(): Boolean {
        var signed = true
        if (!isSigned) {
            signed = false
            binding.tvErrorSignature.visibility = View.VISIBLE
        }
        return signed
    }
}
