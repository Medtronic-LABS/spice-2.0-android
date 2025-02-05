package com.medtroniclabs.opensource.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.databinding.PrescriptionReasonDialogueBinding
import com.medtroniclabs.opensource.formgeneration.extension.fetchString
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class DeleteReasonDialog() : DialogFragment(), View.OnClickListener {

    var callback: ((isPositiveResult: Boolean, comments: String?) -> Unit)? = null

    constructor(callback: (isPositiveResult: Boolean, comments: String?) -> Unit) : this() {
        this.callback = callback
    }

    var isNegativeButtonNeed: Boolean = false

    companion object {

        const val TAG = "CommentsAlertDialog"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_MESSAGE = "KEY_MESSAGE"
        private const val KEY_OKAY_BUTTON = "KEY_OKAY_BUTTON"
        private const val KEY_CANCEL_BUTTON = "KEY_CANCEL_BUTTON"
        private const val KEY_SHOW_COMMENT = "KEY_SHOW_COMMENT"
        private const val IS_NEGATIVE_BUTTON_NEEDED = "IS_NEGATIVE_BUTTON_NEEDED"
        private const val ERROR_MESSAGE = "ERROR_MESSAGE"

        fun newInstance(
            context: Context,
            title: String? = null,
            isNegativeButtonNeed: Boolean,
            buttonText: Pair<String, String> = Pair(
                context.getString(R.string.ok),
                context.getString(R.string.cancel)
            ),
            showComment: Boolean = true,
            callback: ((isPositiveResult: Boolean, comments: String?) -> Unit),
            message: Pair<String?, String?> = Pair(null, null),
        ): DeleteReasonDialog {

            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_MESSAGE, message.first)
            args.putString(KEY_OKAY_BUTTON, buttonText.first)
            args.putString(KEY_CANCEL_BUTTON, buttonText.second)
            args.putBoolean(KEY_SHOW_COMMENT, showComment)
            args.putBoolean(IS_NEGATIVE_BUTTON_NEEDED, isNegativeButtonNeed)
            args.putString(ERROR_MESSAGE, message.second)
            val fragment = DeleteReasonDialog(callback)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: PrescriptionReasonDialogueBinding


    override fun onStart() {
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PrescriptionReasonDialogueBinding.inflate(inflater, container, false)
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
        setupView()
        setupClickListeners()
    }

    private fun readArguments() {
        arguments?.getBoolean(IS_NEGATIVE_BUTTON_NEEDED)?.let {
            isNegativeButtonNeed = it
        }
    }

    private fun setupView() {

       binding.tvTitle.text = requireArguments().getString(KEY_TITLE)
        binding.tvSubTitle.text = requireArguments().getString(KEY_MESSAGE)
        binding.btnOkay.text = requireArguments().getString(KEY_OKAY_BUTTON)
        binding.btnCancel.text = requireArguments().getString(KEY_CANCEL_BUTTON)
      //  binding.tvErrorMessage.text = requireArguments().getString(ERROR_MESSAGE)
        arguments?.getBoolean(KEY_SHOW_COMMENT)?.let {
            binding.commentsGroup.visibility = if (it) View.VISIBLE else View.GONE
        }
        if (isNegativeButtonNeed) {
            binding.btnCancel.visibility = View.VISIBLE
        } else {
            binding.btnCancel.visibility = View.GONE
        }

        binding.tvCommentLbl.markMandatory()
    }

    private fun setupClickListeners() {
        binding.ivClose.safeClickListener(this)
        binding.btnOkay.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.ivClose.id -> {
                dismiss()
            }
            binding.btnOkay.id -> {
                if(binding.commentsGroup.visibility == View.VISIBLE) {
                    if (binding.etComments.text.isNullOrEmpty()){
                        binding.tvErrorMessage.visible()
                    }else{
                        binding.tvErrorMessage.gone()
                        callback?.invoke(true, binding.etComments.fetchString())
                        dismiss()
                    }
                } else {
                    callback?.invoke(true, binding.etComments.fetchString())
                    dismiss()
                }

            }
            binding.btnCancel.id -> {
                dismiss()
                callback?.invoke(false, null)
            }
        }
    }
}
