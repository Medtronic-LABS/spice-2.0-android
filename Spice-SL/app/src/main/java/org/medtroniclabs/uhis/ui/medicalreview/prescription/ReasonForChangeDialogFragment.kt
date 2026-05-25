package org.medtroniclabs.uhis.ui.medicalreview.prescription

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.setDialogWidthAndHeightAsWrapPercent
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams.MEDICATIONS
import org.medtroniclabs.uhis.common.DefinedParams.PRESCRIBED_MEDICINE
import org.medtroniclabs.uhis.common.DefinedParams.REGIMEN
import org.medtroniclabs.uhis.databinding.FragmentReasonForChangeDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseDialogFragment

class ReasonForChangeDialogFragment(
    private val callback: ReasonChangeCallback?,
) : BaseDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentReasonForChangeDialogBinding

    companion object {
        const val TAG = "ReasonForChangeDialogFragment"

        fun newInstance(
            name: String? = null,
            regimen: Int? = null,
            prescribedMedicine: Boolean = false,
            callback: ReasonChangeCallback?,
        ): ReasonForChangeDialogFragment {
            val args = Bundle().apply {
                putString(MEDICATIONS, name)
                regimen?.let { putInt(REGIMEN, it + 1) }
                putBoolean(PRESCRIBED_MEDICINE, prescribedMedicine)
            }
            return ReasonForChangeDialogFragment(callback).apply {
                arguments = args
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentReasonForChangeDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
        binding.btnCancel.setOnClickListener(this)
        binding.btnOkay.setOnClickListener(this)
    }

    private fun setListener() {
        binding.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.etReason.doAfterTextChanged { input ->
            input?.let {
                isEnableRefer(if (it.trim().isNotEmpty()) it.trim().toString() else null)
            }
        }
    }

    private fun isEnableRefer(reason: String?) {
        binding.btnOkay.isEnabled = !reason.isNullOrEmpty()
    }

    private fun isAlreadyPrescribedMedicine(): Boolean = arguments?.getBoolean(PRESCRIBED_MEDICINE, false) ?: false

    private fun getRegimenLine(): String =
        if (isAlreadyPrescribedMedicine()) {
            if (arguments?.getInt(REGIMEN) != null) {
                when (arguments?.getInt(REGIMEN)) {
                    1 -> "1st"
                    2 -> "2nd"
                    3 -> "3rd"
                    else -> "${arguments?.getInt(REGIMEN)}th"
                }
            } else {
                "1st"
            }
        } else {
            "1st"
        }

    private fun initView() {
        binding.tvReferredReasonLabel.markMandatory()
        binding.tvName.text = arguments?.getString(MEDICATIONS) ?: getString(R.string.seperator_hyphen)
        binding.tvRegimenLine.text = getRegimenLine()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnCancel, R.id.ivClose -> {
                dismiss()
            }
            R.id.btnOkay -> {
                val changeReason = binding.etReason.text.toString()
                callback?.onReasonProvided(changeReason)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 65 else 90
        } else {
            if (isLandscape) 65 else 90
        }
        setDialogWidthAndHeightAsWrapPercent(width)
    }

    interface ReasonChangeCallback {
        fun onReasonProvided(changeReason: String)
    }
}
