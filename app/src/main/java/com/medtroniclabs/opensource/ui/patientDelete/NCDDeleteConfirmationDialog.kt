package com.medtroniclabs.opensource.ui.patientDelete

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.NcdDeleteConfirmationDialogueBinding
import com.medtroniclabs.opensource.formgeneration.extension.fetchString
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.patientDelete.viewModel.NCDPatientDeleteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDDeleteConfirmationDialog() : DialogFragment(), View.OnClickListener {

    private lateinit var reasonListCustomView: TagListCustomView
    private val viewModel: NCDPatientDeleteViewModel by activityViewModels()

    private var callback: ((isPositiveResult: Boolean, reason: String?, otherReason: String?) -> Unit)? =
        null
    var isNegativeButtonNeed: Boolean = false

    constructor(callback: (isPositiveResult: Boolean, reason: String?, otherReason: String?) -> Unit) : this() {
        this.callback = callback
    }

    companion object {

        const val TAG = "NCDDeleteConfirmationDialog"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_MESSAGE = "KEY_MESSAGE"
        private const val KEY_OKAY_BUTTON = "KEY_OKAY_BUTTON"
        private const val KEY_CANCEL_BUTTON = "KEY_CANCEL_BUTTON"
        private const val IS_NEGATIVE_BUTTON_NEEDED = "IS_NEGATIVE_BUTTON_NEEDED"

        fun newInstance(
            title: String,
            message: String,
            callback: ((isPositiveResult: Boolean, reason: String?, otherReason: String?) -> Unit),
            context: Context,
            isNegativeButtonNeed: Boolean,
            okayButton: String = context.getString(R.string.ok),
            cancelButton: String = context.getString(R.string.cancel)
        ): NCDDeleteConfirmationDialog {

            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_MESSAGE, message)
            args.putString(KEY_OKAY_BUTTON, okayButton)
            args.putString(KEY_CANCEL_BUTTON, cancelButton)
            args.putBoolean(IS_NEGATIVE_BUTTON_NEEDED, isNegativeButtonNeed)
            val fragment = NCDDeleteConfirmationDialog(callback)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: NcdDeleteConfirmationDialogueBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NcdDeleteConfirmationDialogueBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readArguments()
        setupView()
        setupClickListeners()
        viewModel.getDeleteReasonList()
        initializeTagView()
        attachObserver()
    }

    private fun initializeTagView() {
        reasonListCustomView = TagListCustomView(
            requireContext(),
            binding.cgDeleteReason,
            otherCallBack = { selectedName, isChecked ->
                if (selectedName.startsWith(
                        DefinedParams.Other.lowercase(),
                        ignoreCase = true
                    )
                ) {
                    if (isChecked) {
                        binding.etOtherReason.visible()
                        binding.tvReasonErrorMessage.gone()
                    } else {
                        binding.etOtherReason.gone()
                        binding.tvReasonErrorMessage.gone()
                        binding.etOtherReason.setText("")
                    }
                }
            },
            otherSingleSelect = true
        )
    }

    private fun attachObserver() {
        viewModel.deleteReasonList.observe(viewLifecycleOwner) { list ->
            list?.let {
                loadDeleteReasonList(it)
            }
        }
    }

    private fun loadDeleteReasonList(list: List<ShortageReasonEntity>) {
        val chipItems = ArrayList<ChipViewItemModel>()
        list.forEachIndexed { index, element ->
            chipItems.add(
                ChipViewItemModel(
                    id = (index + 1).toLong(),
                    value = element.type,
                    name = element.name,
                    cultureValue = element.displayValue,
                )
            )
        }
        reasonListCustomView.addChipItemList(chipItems, null, diagnosisGrouping(chipItems))
    }

    private fun diagnosisGrouping(list: List<ChipViewItemModel>?): HashMap<String, MutableList<ChipViewItemModel>>? {
        return list?.groupByTo(HashMap(), { it.type.toString() }, { it })
    }

    private fun readArguments() {
        arguments?.getBoolean(IS_NEGATIVE_BUTTON_NEEDED)?.let {
            isNegativeButtonNeed = it
        }
    }

    private fun setupView() {
        binding.labelHeader.titleView.text = requireArguments().getString(KEY_TITLE)
        binding.tvDeleteMessage.text = requireArguments().getString(KEY_MESSAGE)
        binding.tvDeleteMessage.markMandatory()
        binding.btnOkay.text = requireArguments().getString(KEY_OKAY_BUTTON)
        binding.btnCancel.text = requireArguments().getString(KEY_CANCEL_BUTTON)
        binding.btnCancel.setVisible(isNegativeButtonNeed)
        binding.labelHeader.ivClose.safeClickListener(this)
    }

    private fun setupClickListeners() {
        binding.btnOkay.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun deleteArchive() {
        val chipValue = getChipValue(reasonListCustomView.getSelectedTags()[0])
        if (binding.etOtherReason.text.isNullOrEmpty())
            callback?.invoke(true, chipValue, null)
        else
            callback?.invoke(true, chipValue, binding.etOtherReason.fetchString())
        dismiss()
    }

    private fun getChipValue(any: Any): String {
        return if (any is ChipViewItemModel)
            any.name
        else
            any.toString()
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (reasonListCustomView.getSelectedTags().isNotEmpty()) {
            val selectedReason = getChipValue(reasonListCustomView.getSelectedTags()[0])
            if (selectedReason.contains(DefinedParams.Other)) {
                if (binding.etOtherReason.text.isNullOrBlank()) {
                    isValid = false
                    binding.tvReasonErrorMessage.text = getString(R.string.valid_reason)
                    binding.tvReasonErrorMessage.visible()
                } else {
                    binding.tvReasonErrorMessage.gone()
                }
            }
        } else {
            binding.tvReasonErrorMessage.text = getString(R.string.reason_error)
            binding.tvReasonErrorMessage.visible()
            isValid = false
        }

        return isValid
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnOkay.id -> {
                if (validateInputs()) {
                    deleteArchive()
                }
            }

            binding.btnCancel.id -> {
                dismiss()
            }

            binding.labelHeader.ivClose.id -> {
                dismiss()
            }
        }
    }

}
