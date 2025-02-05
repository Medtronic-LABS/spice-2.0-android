package com.medtroniclabs.opensource.ui.medicalreview.investigation.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.DialogConfirmationMarkAsReviewedBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class MarkAsReviewedConfirmationDialog(private val callback: (userConfirmed: Boolean) -> Unit) :
    DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogConfirmationMarkAsReviewedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogConfirmationMarkAsReviewedBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setClickListener()
    }

    private fun setClickListener() {
        binding.apply {
            btnCancel.safeClickListener(this@MarkAsReviewedConfirmationDialog)
            btnConfirm.safeClickListener(this@MarkAsReviewedConfirmationDialog)
            ivClose.safeClickListener(this@MarkAsReviewedConfirmationDialog)
        }
    }

    override fun onStart() {
        super.onStart()
        handleOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientation()
    }

    private fun handleOrientation() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = when {
            isTablet && isLandscape -> 50
            else -> 100
        }
        val height = when {
            isTablet && isLandscape -> 50
            else -> 100
        }
        setDialogPercent(width, height)
    }

    companion object {
        const val TAG = "MarkAsReviewedConfirmationDialog"
        fun newInstance(callback: (userConfirmed: Boolean) -> Unit): MarkAsReviewedConfirmationDialog {
            return MarkAsReviewedConfirmationDialog(callback)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnCancel, R.id.ivClose -> dismiss()
            R.id.btnConfirm -> {
                dismiss()
                callback.invoke(true)
            }
        }
    }
}