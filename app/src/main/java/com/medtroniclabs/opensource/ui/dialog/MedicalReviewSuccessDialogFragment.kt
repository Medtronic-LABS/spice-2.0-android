package com.medtroniclabs.opensource.ui.dialog

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.FragmentMedicalReviewSucessDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener

class MedicalReviewSuccessDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentMedicalReviewSucessDialogBinding
    private var onDismissListener: OnDialogDismissListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as? OnDialogDismissListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicalReviewSucessDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "MedicalReviewSuccessDialogFragment"
        const val ISENROLLED = "ISENROLLED"
        fun newInstance(isEnroll:Boolean = false) = MedicalReviewSuccessDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ISENROLLED, isEnroll)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeListeners()
    }

    private fun getIsEnroll(): Boolean {
        return arguments?.getBoolean(ISENROLLED) ?: false
    }

    private fun initializeListeners() {
        binding.btnEnroll.setVisible(CommonUtils.isNonCommunity() && (!getIsEnroll()))
        binding.btnDone.safeClickListener(this)
        binding.ivClose.safeClickListener(this)
        binding.btnEnroll.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id, binding.ivClose.id -> {
                onDismissListener?.onDialogDismissListener()
                dismiss()
            }
            binding.btnEnroll.id -> {
                onDismissListener?.onDialogDismissListener(true)
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
            if (isLandscape) 50 else 60
        } else {
            if (isLandscape) 50 else 60
        }
        setWidth(width)
    }

}