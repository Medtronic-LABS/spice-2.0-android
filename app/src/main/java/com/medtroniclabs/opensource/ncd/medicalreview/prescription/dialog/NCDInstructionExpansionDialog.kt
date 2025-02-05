package com.medtroniclabs.opensource.ncd.medicalreview.prescription.dialog

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.MedicationResponse
import com.medtroniclabs.opensource.databinding.FragmentNcdInstructionExpansionDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.customGetParcelable
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.viewmodel.NCDPrescriptionViewModel


class NCDInstructionExpansionDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentNcdInstructionExpansionDialogBinding
    private var model: MedicationResponse? = null
    private val prescriptionViewModel: NCDPrescriptionViewModel by activityViewModels()
    companion object {
        const val TAG = "NCDInstructionExpansionDialog"
        const val KEY_MODEL = "KEY_MODEL"
        fun newInstance(model: MedicationResponse): NCDInstructionExpansionDialog {
            val fragment = NCDInstructionExpansionDialog()
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_MODEL, model)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdInstructionExpansionDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.setDecorFitsSystemWindows(false)
        }
        binding.root.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
                binding.root.setPadding(0, 0, 0, imeHeight)
                windowInsets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemGestures())
            }
            windowInsets
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        readArguments()
        initView()
    }

    private fun readArguments() {
        arguments?.let { args ->
            if(args.containsKey(KEY_MODEL))
                model = args.customGetParcelable(KEY_MODEL)
        }
    }

    private fun initView() {
        binding.btnDone.safeClickListener(this)
        binding.ivClose.safeClickListener(this)
        binding.etInstruction.setText(model?.instruction_entered ?: "")
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                model?.instruction_entered = binding.etInstruction.text.toString()
                model?.isInstructionUpdated = true
                prescriptionViewModel.reloadInstruction.value = true
                dismiss()
            }
            binding.ivClose.id -> {
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
        setWidth(width)
    }


}