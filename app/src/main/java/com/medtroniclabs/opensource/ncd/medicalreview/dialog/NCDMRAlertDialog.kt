package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.NcdMrAlertDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class NCDMRAlertDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: NcdMrAlertDialogBinding
    private var callback: DialogCallback? = null

    interface DialogCallback {
        fun onYesClicked()
        fun onConfirmDiagnosisClicked(isBp: Boolean)
    }

    fun setDialogCallback(callback: DialogCallback) {
        this.callback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = NcdMrAlertDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        isCancelable = false
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    companion object {
        const val TAG = "NCDMRAlertDialog"
        private const val KEY_MESSAGE = "MESSAGE"
        private const val KEY_TITLE = "TITLE"
        private const val KEY_CONFIRM = "KEY_CONFIRM"
        private const val KEY_NO = "KEY_NO"
        private const val KEY_SHOW_YES = "KEY_SHOW_YES"
        private const val KEY_TEXT_YES = "KEY_TEXT_YES"
        private const val KEY_TEXT_NO = "KEY_TEXT_NO"
        private const val KEY_TEXT_CF = "KEY_TEXT_CF"
        private const val KEY_SHOW_NO = "KEY_SHOW_NO"
        private const val KEY_SHOW_CLOSE = "KEY_SHOW_CLOSE"

        fun newInstance(
            title: String = "Alert",          // Default title
            message: String? = "", // Default message
            showYesNoClose: Triple<Boolean, Boolean, Boolean> = Triple(
                true,
                true,
                true
            ), // Show all by default
            yesText: String = "Yes",          // Default Yes button text
            noText: String = "No",            // Default No button text
            showConfirm: Boolean = false, // Default: don't show confirm button
            cfTest: String = "",
            callback: DialogCallback? = null  // Default callback is null
        ) = NCDMRAlertDialog().apply {
            arguments = Bundle().apply {
                putString(KEY_MESSAGE, message)
                putString(KEY_TITLE, title)
                putBoolean(KEY_SHOW_YES, showYesNoClose.first)
                putBoolean(KEY_SHOW_NO, showYesNoClose.second)
                putBoolean(KEY_SHOW_CLOSE, showYesNoClose.third)
                putBoolean(KEY_CONFIRM, showConfirm)
                putString(KEY_TEXT_CF, cfTest)
                putString(KEY_TEXT_YES, yesText)
                putString(KEY_TEXT_NO, noText)
            }
            if (callback != null) {
                setDialogCallback(callback)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnNo.id -> {
                dismiss()
            }

            binding.btnYes.id -> {
                callback?.onYesClicked()
                dismiss()
            }

            binding.ivClose.id -> {
                dismiss()
            }

            binding.btnConfirmDiagnosis.id -> {
                callback?.onConfirmDiagnosisClicked(
                    binding.btnConfirmDiagnosis.text.isNotBlank() && binding.btnConfirmDiagnosis.text.trim()
                        .toString().equals(
                        getString(
                            R.string.add_new_reading
                        ), true
                    )
                )
                dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        // Set title and message from arguments
        binding.apply {
            arguments?.let { args ->
                tvTitle.text = args.getString(KEY_TITLE)
                tvContent.text = args.getString(KEY_MESSAGE)

                // Set button texts
                btnYes.text = args.getString(KEY_TEXT_YES)
                btnNo.text = args.getString(KEY_TEXT_NO)
                btnConfirmDiagnosis.text = args.getString(KEY_TEXT_CF)?.takeIf { it.isNotBlank() } ?: btnConfirmDiagnosis.text

                // Show or hide buttons based on arguments
                btnYes.visibility = if (args.getBoolean(KEY_SHOW_YES)) View.VISIBLE else View.GONE
                btnNo.visibility = if (args.getBoolean(KEY_SHOW_NO)) View.VISIBLE else View.GONE
                ivClose.visibility =
                    if (args.getBoolean(KEY_SHOW_CLOSE)) View.VISIBLE else View.GONE
                btnConfirmDiagnosis.visibility =
                    if (args.getBoolean(KEY_CONFIRM)) View.VISIBLE else View.GONE

                // Add click listeners
                ivClose.safeClickListener(this@NCDMRAlertDialog)
                btnYes.safeClickListener(this@NCDMRAlertDialog)
                btnNo.safeClickListener(this@NCDMRAlertDialog)
                btnConfirmDiagnosis.safeClickListener(this@NCDMRAlertDialog)
            }
        }
    }
}
