package com.medtroniclabs.opensource.ncd.medicalreview.prescription.dialog

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.FragmentNcdDeleteConfirmationDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDDeleteConfirmationDialog() : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentNcdDeleteConfirmationDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdDeleteConfirmationDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    private var callback: ((isPositiveResult: Boolean, reason: String?, otherReason: String?) -> Unit)? =
        null
    var isNegativeButtonNeed: Boolean = false

    constructor(callback: (isPositiveResult: Boolean, reason: String?, otherReason: String?) -> Unit) : this() {
        this.callback = callback
    }

    companion object {

        const val TAG = "NCDDeleteConfirmationDialog"

        private const val KEY_TITLE = "KEY_TITTLE"
        private const val KEY_MESSAGE = "KEY_MESSAGE"
        private const val KEY_OK_BUTTON = "KEY_OK_BUTTON"
        private const val KEY_CANCEL_BUTTON = "KEY_CANCEL_BUTTON"
        private const val KEY_NEGATIVE_BUTTON = "KEY_NEGATIVE_BUTTON"

        fun newInstance(
            title: String,
            message: String,
            callback: ((isPositiveResult: Boolean, reason: String?, otherReason: String?) -> Unit),
            context: Context,
            okButton: String = context.getString(R.string.ok),
            cancelButton: String = context.getString(R.string.cancel),
            isNegativeButton: Boolean
        ): NCDDeleteConfirmationDialog {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_MESSAGE, message)
            args.putString(KEY_OK_BUTTON, okButton)
            args.putString(KEY_CANCEL_BUTTON, cancelButton)
            args.putBoolean(KEY_NEGATIVE_BUTTON, isNegativeButton)
            val fragment = NCDDeleteConfirmationDialog(callback)
            fragment.arguments = args
            return fragment
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        setupClickListeners()
        setupView()
    }

    private fun setupClickListeners() {
        binding.btnOkay.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun setupView() {
        binding.tvTitle.text = requireArguments().getString(KEY_TITLE)
        binding.tvDeleteMessage.text = requireArguments().getString(KEY_MESSAGE)
        binding.tvDeleteMessage.markMandatory()
        binding.btnOkay.text = requireArguments().getString(KEY_OK_BUTTON)
        binding.btnCancel.text = requireArguments().getString(KEY_CANCEL_BUTTON)
        binding.ivClose.gone()
        if (isNegativeButtonNeed) {
            binding.btnCancel.visible()
        } else {
            binding.btnCancel.visible()
        }
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnOkay.id -> {

            }

            binding.btnCancel.id -> {
                dismiss()
            }
        }
    }

}