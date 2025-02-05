package com.medtroniclabs.opensource.ui.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.DialogGeneralSuccessBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class GeneralSuccessDialog(private val callback: () -> Unit) : DialogFragment() {

    companion object {
        const val TAG = "GeneralSuccessDialog"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_MESSAGE = "KEY_MESSAGE"
        private const val KEY_OKAY_BUTTON = "KEY_OKAY_BUTTON"

        fun newInstance(
            title: String,
            message: String,
            okayButton: String,
            callback: () -> Unit
        ): GeneralSuccessDialog {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_MESSAGE, message)
            args.putString(KEY_OKAY_BUTTON, okayButton)
            val fragment = GeneralSuccessDialog(callback)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: DialogGeneralSuccessBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogGeneralSuccessBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        readArguments()
        setupClickListeners()
    }

    private fun readArguments() {
        requireArguments().let {
            it.getString(KEY_TITLE)?.let { title ->
                binding.tvTitle.text = title
            }
            it.getString(KEY_MESSAGE)?.let { message ->
                binding.successMessage.text = message
            }
            it.getString(KEY_OKAY_BUTTON)?.let { okayBtn ->
                binding.btnOkay.text = okayBtn
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnOkay.safeClickListener {
            dismiss()
            callback.invoke()
        }
        binding.ivClose.safeClickListener {
            dismiss()
            callback.invoke()
        }
    }

    override fun onStart() {
        super.onStart()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 50 else 60
        } else {
            if (isLandscape) 50 else 60
        }
        setWidth(width)
    }
}