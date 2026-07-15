package org.medtroniclabs.uhis.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentNcdCurrentMedicationBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDCurrentMedicationViewModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil

@AndroidEntryPoint
class NCDCurrentMedicationFragment : BaseFragment() {
    private lateinit var binding: FragmentNcdCurrentMedicationBinding
    private lateinit var currentMedicationTagListCustomView: TagListCustomView
    private val viewModel: NCDCurrentMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNcdCurrentMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDCurrentMedicationFragment"

        fun newInstance() = NCDCurrentMedicationFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
                    value = item.value,
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
                },
            )
            currentMedicationTagListCustomView.addChipItemList(complaintList, viewModel.chips)
        }
    }

    private fun showNotes() {
        if (viewModel.chips.firstOrNull {
                it.name.equals(
                    DefinedParams.OTHER,
                    ignoreCase = true,
                )
            } != null
        ) {
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
            viewModel.chips.any { it.name.equals(DefinedParams.OTHER, ignoreCase = true) }
        val commentsNotBlank = binding.etOtherCurrentMedication.text?.isNotBlank() == true

        val adheringValid = validateAdheringCurrentMed()
        val drugAllergiesValid = validateDrugAllergies()

        val isChipValid = if (isMandatory) {
            validateChips(
                hasChips,
                hasOtherChip,
                commentsNotBlank,
                binding.etOtherCurrentMedication,
            )
        } else {
            validateOptionalChips(
                hasChips,
                hasOtherChip,
                commentsNotBlank,
                binding.etOtherCurrentMedication,
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
            viewFocus,
        )
    }

    private fun validateTextField(
        condition: Boolean?,
        textField: AppCompatEditText,
        errorView: TextView,
    ): Boolean =
        when (condition) {
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

    private fun validateAdheringCurrentMed(): Pair<Boolean, AppCompatEditText?> {
        val isValid = validateTextField(
            viewModel.adheringCurrentMed,
            binding.etCommentCurrentMedication,
            binding.tvErrorCurrentMedications,
        )
        return Pair(isValid, if (isValid) null else binding.etCommentCurrentMedication)
    }

    private fun validateDrugAllergies(): Pair<Boolean, AppCompatEditText?> {
        val isValid = validateTextField(
            viewModel.drugAllergies,
            binding.etCommentAllergies,
            binding.tvErrorAllergiesToDrugs,
        )
        return Pair(isValid, if (isValid) null else binding.etCommentAllergies)
    }

    private fun validateChips(
        hasChips: Boolean,
        hasOtherChip: Boolean,
        commentsNotBlank: Boolean,
        editText: AppCompatEditText,
    ): Pair<Boolean, AppCompatEditText?> =
        if (hasChips) {
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

    private fun validateOptionalChips(
        hasChips: Boolean,
        hasOtherChip: Boolean,
        commentsNotBlank: Boolean,
        editText: AppCompatEditText,
    ): Pair<Boolean, AppCompatEditText?> =
        if (!hasChips && binding.etOtherCurrentMedication.text?.isBlank() == true) {
            hideError(binding.tvErrorOtherCurrentMedications)
            Pair(true, null)
        } else {
            validateChips(hasChips, hasOtherChip, commentsNotBlank, editText)
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
