package org.medtroniclabs.uhis.ui.mypatients.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.setWidth
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.FragmentAddWeightDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener

class AddPresumptiveTBNoDialog : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentAddWeightDialogBinding

    var listener: OnPresumptiveTBEnteredListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddWeightDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "AddPresumptiveTBNoDialog"
        const val Data = "Data"

        fun newInstance(
            text: String? = null,
            title: String,
            hint: String,
            length: Int,
            inputType: Int,
        ): AddPresumptiveTBNoDialog {
            val fragment = AddPresumptiveTBNoDialog()
            fragment.arguments = Bundle().apply {
                putString(Data, text)
                putString(DefinedParams.TITLE, title)
                putString(DefinedParams.HINT, hint)
                putInt(DefinedParams.LENGTH, length)
                putInt(DefinedParams.INPUT_TYPE, inputType)
            }
            return fragment
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        with(binding) {
            with(arguments) {
                val title = this?.getString(DefinedParams.TITLE).orEmpty().ifBlank { getString(R.string.hyphen_symbol) }
                val hint = this?.getString(DefinedParams.HINT).orEmpty().ifBlank { getString(R.string.hyphen_symbol) }
                val length = this?.getInt(DefinedParams.LENGTH) ?: 20
                val inputType = this?.getInt(DefinedParams.INPUT_TYPE) ?: InputType.TYPE_CLASS_NUMBER
                tvTitle.text = title
                tvWeightLabel.text = title
                etWeight.hint = hint
                etWeight.apply {
                    this.inputType = inputType
                    filters = arrayOf(InputFilter.LengthFilter(length))
                }
            }
            btnCancel.safeClickListener(this@AddPresumptiveTBNoDialog)
            etWeight.addTextChangedListener(textWatcher)
            btnOkay.safeClickListener(this@AddPresumptiveTBNoDialog)
            ivClose.safeClickListener(this@AddPresumptiveTBNoDialog)
            try {
                arguments?.getString(Data)?.let { text ->
                    if (text.isNotBlank()) {
                        etWeight.setText(text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Optionally show a toast or log the error
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnOkay.id -> handleOkayClick()
            binding.btnCancel.id, binding.ivClose.id -> dismiss()
        }
    }

    private fun handleOkayClick() {
        val tbNumber = binding.etWeight.text
            .toString()
            .trim()
        if (tbNumber.isNotEmpty()) {
            listener?.onPresumptiveTBEntered(tbNumber)
            dismiss()
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) {
            // Not needed for your use case
        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int,
        ) {
            // Not needed for your use case
        }

        override fun afterTextChanged(s: Editable?) {
            // Call the method to check if any EditText field is filled
            val hasString = (s?.trim()?.count() ?: 0) > 0
            binding.btnOkay.isEnabled = hasString
        }
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

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogSize()
    }
}

interface OnPresumptiveTBEnteredListener {
    fun onPresumptiveTBEntered(tbNumber: String)
}
