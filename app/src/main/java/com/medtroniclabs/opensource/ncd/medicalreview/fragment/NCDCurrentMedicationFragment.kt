package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentNcdCurrentMedicationBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDCurrentMedicationViewModel
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class NCDCurrentMedicationFragment : BaseFragment() {

    private lateinit var binding: FragmentNcdCurrentMedicationBinding
    private lateinit var currentMedicationTagListCustomView: TagListCustomView
    private val viewModel: NCDCurrentMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdCurrentMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDCurrentMedicationFragment"
        fun newInstance() =
            NCDCurrentMedicationFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getChips()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.getChipItems.observe(viewLifecycleOwner) {
            val complaintList = it.map { item ->
                ChipViewItemModel(
                    id = item.id,
                    name = item.name,
                    cultureValue = item.displayValue,
                    type = item.type,
                    value = item.value
                )
            } as ArrayList<ChipViewItemModel>
            initView(complaintList)
        }
    }

    private fun initView(complaintList: ArrayList<ChipViewItemModel>) {
        binding.apply {
            initializeMandatoryFields()
            resetViewStates()
            setupTextWatchers()
            handleRadioButtons()
            initializeTagListCustomView(complaintList)
        }
    }

    private fun initializeMandatoryFields() {
        binding.apply {
            tvCurrentMedicationsTitle.markMandatory()
            tvCurrentMedicationDetailsTitle.markMandatory()
            tvAllergiesToDrugsTitle.markMandatory()
            MotherNeonateUtil.initTextWatcherForString(binding.etOtherCurrentMedication) {
                viewModel.comments = it
            }
        }
    }

    private fun resetViewStates() {
        binding.apply {
            etCommentAllergies.gone()
            etCommentCurrentMedication.gone()
            etOtherCurrentMedication.gone()
            viewModel.adheringCurrentMed = null
            viewModel.drugAllergies = null
            rgCurrentMedications.clearCheck()
            rgAllergiesToDrugs.clearCheck()
            viewModel.adheringMedComment = null
            viewModel.allergiesComment = null
            viewModel.comments = ""
        }
    }

    private fun setupTextWatchers() {
        binding.apply {
            MotherNeonateUtil.initTextWatcherForString(etCommentCurrentMedication) {
                viewModel.adheringMedComment = it
            }

            MotherNeonateUtil.initTextWatcherForString(etCommentAllergies) {
                viewModel.allergiesComment = it
            }
        }
    }

    private fun handleRadioButtons() {
        binding.apply {
            // Handle the radio buttons for Current Medication Adhering
            rgCurrentMedications.setOnCheckedChangeListener { _, checkedId ->
                val isAdhering = (checkedId == R.id.rbCurrentMedicationYes)
                viewModel.adheringCurrentMed = isAdhering
                etCommentCurrentMedication.setVisible(isAdhering)

                // Clear the comment when "No" is selected
                if (!isAdhering) {
                    viewModel.adheringMedComment = null
                }
            }

            // Handle the radio buttons for Drug Allergies
            rgAllergiesToDrugs.setOnCheckedChangeListener { _, checkedId ->
                val hasAllergies = (checkedId == R.id.rbAllergiesToDrugsYes)
                viewModel.drugAllergies = hasAllergies
                etCommentAllergies.setVisible(hasAllergies)

                // Clear the comment when "No" is selected
                if (!hasAllergies) {
                    viewModel.allergiesComment = null
                }
            }
        }
    }

    private fun initializeTagListCustomView(complaintList: ArrayList<ChipViewItemModel>) {
        binding.apply {
            currentMedicationTagListCustomView = TagListCustomView(
                requireContext(),
                currentMedicationTags,
                callBack = { _, _, _ ->
                    viewModel.chips.clear()
                    viewModel.chips =
                        ArrayList(currentMedicationTagListCustomView.getSelectedTags())
                    showNotes()
                }
            )
            currentMedicationTagListCustomView.addChipItemList(complaintList, viewModel.chips)
        }
    }


    private fun showNotes() {
        if (viewModel.chips.firstOrNull {
                it.name.equals(
                    DefinedParams.Other,
                    ignoreCase = true
                )
            } != null) {
            binding.etOtherCurrentMedication.visible()
        } else {
            binding.etOtherCurrentMedication.gone()
            binding.etOtherCurrentMedication.setText(R.string.empty)
            viewModel.comments = ""
            binding.tvErrorOtherCurrentMedications.gone()
        }
    }

    fun validateInput(isMandatory: Boolean = true): Pair<Boolean, AppCompatEditText?> {
        val hasChips = viewModel.chips.isNotEmpty()
        val hasOtherChip =
            viewModel.chips.any { it.name.equals(DefinedParams.Other, ignoreCase = true) }
        val commentsNotBlank = binding.etOtherCurrentMedication.text?.isNotBlank() == true

        val adheringValid = validateAdheringCurrentMed()
        val drugAllergiesValid = validateDrugAllergies()

        val isChipValid = if (isMandatory) {
            validateChips(
                hasChips,
                hasOtherChip,
                commentsNotBlank,
                binding.etOtherCurrentMedication
            )
        } else {
            validateOptionalChips(
                hasChips,
                hasOtherChip,
                commentsNotBlank,
                binding.etOtherCurrentMedication
            )
        }

        val viewFocus = when {
            adheringValid.second != null -> {
                adheringValid.second
            }

            drugAllergiesValid.second != null -> {
                drugAllergiesValid.second
            }

            isChipValid.second != null -> {
                isChipValid.second
            }

            else -> {
                null
            }
        }

        return Pair(
            (isChipValid.first && adheringValid.first && drugAllergiesValid.first),
            viewFocus
        )
    }

    private fun validateTextField(
        condition: Boolean?,
        textField: AppCompatEditText,
        errorView: TextView
    ): Boolean {
        return when (condition) {
            true -> {
                if (textField.text.isNullOrBlank()) {
                    showError(errorView)
                    false
                } else {
                    hideError(errorView)
                    true
                }
            }

            false -> {
                hideError(errorView)
                true
            }

            else -> {
                showError(errorView)
                false
            }
        }
    }

    private fun validateAdheringCurrentMed(): Pair<Boolean, AppCompatEditText?> {
        val isValid = validateTextField(
            viewModel.adheringCurrentMed,
            binding.etCommentCurrentMedication,
            binding.tvErrorCurrentMedications
        )
        return Pair(isValid, if (isValid) null else binding.etCommentCurrentMedication)
    }

    private fun validateDrugAllergies(): Pair<Boolean, AppCompatEditText?> {
        val isValid = validateTextField(
            viewModel.drugAllergies,
            binding.etCommentAllergies,
            binding.tvErrorAllergiesToDrugs
        )
        return Pair(isValid, if (isValid) null else binding.etCommentAllergies)
    }

    private fun validateChips(
        hasChips: Boolean,
        hasOtherChip: Boolean,
        commentsNotBlank: Boolean,
        editText: AppCompatEditText
    ): Pair<Boolean, AppCompatEditText?> {
        return if (hasChips) {
            if (hasOtherChip) {
                if (!commentsNotBlank) {
                    showError(binding.tvErrorOtherCurrentMedications)
                    Pair(false, editText)
                } else {
                    hideError(binding.tvErrorOtherCurrentMedications)
                    Pair(true, null)
                }
            } else {
                hideError(binding.tvErrorOtherCurrentMedications)
                Pair(true, null)
            }
        } else {
            showError(binding.tvErrorOtherCurrentMedications)
            Pair(false, editText)
        }
    }

    private fun validateOptionalChips(
        hasChips: Boolean,
        hasOtherChip: Boolean,
        commentsNotBlank: Boolean,
        editText: AppCompatEditText
    ): Pair<Boolean, AppCompatEditText?> {
        return if (!hasChips && binding.etOtherCurrentMedication.text?.isBlank() == true) {
            hideError(binding.tvErrorOtherCurrentMedications)
            Pair(true, null)
        } else {
            validateChips(hasChips, hasOtherChip, commentsNotBlank, editText)
        }
    }

    private fun showError(errorView: TextView) {
        errorView.apply {
            visible()
            text = getString(R.string.error_label)
        }
    }

    private fun hideError(errorView: TextView) {
        errorView.gone()
    }
}