package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.LabourDeliveryMetaEntity
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentNeonateBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.AddAgparScoreDialog
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.adapter.AgparScoreAdapter

class NeonateFragment : BaseFragment() {
    private lateinit var binding: FragmentNeonateBinding
    private lateinit var cgNeonateOutcome: TagListCustomView
    private lateinit var cgSignSymptomsObserved: TagListCustomView
    private lateinit var agparScoreAdapter: AgparScoreAdapter
    private lateinit var viewGender: SingleSelectionCustomView
    private lateinit var viewStateOfBaby: SingleSelectionCustomView

    private val viewModel: LabourDeliveryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNeonateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        attachObserver()
        initializeGenderLabel()
        initializeStateOfBabyLabel()
    }

    private fun attachObserver() {
        viewModel.labourDeliveryMetaList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        initializeNeonateOutcomeItems(listItems)
                        initializeSignsSymptomsItems(listItems)
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        viewModel.apgarScoreLiveData.observe(viewLifecycleOwner) {
            agparScoreAdapter.submitData(it)
        }

        viewModel.gestationalDate.observe(viewLifecycleOwner) { it ->
            viewModel.gestationalAge = (
                (
                    viewModel.lastMensurationDate?.let { it1 ->
                        DateUtils.calculateGestationalAgeWeeks(
                            it1,
                            it,
                        )
                    }
                ).toString()
            )
            when {
                viewModel.gestationalAge.isNullOrBlank() || viewModel.gestationalAge!!.contains("-") -> {
                    binding.tvGestationalAge.text = getString(R.string.empty__)
                    viewModel.gestationalAge = null
                }
                else -> {
                    val gestationalAge = viewModel.gestationalAge?.toLongOrNull()
                    val weeks = gestationalAge?.let {
                        DateUtils.formatGestationalAge(it, requireContext())
                    } ?: getString(R.string.hyphen_symbol)

                    val result = if (gestationalAge != null && gestationalAge < 36) {
                        weeks.plus(getString(R.string._36_weeks))
                    } else {
                        weeks
                    }

                    binding.tvGestationalAge.text = result
                }
            }
        }
    }

    private fun initializeSignsSymptomsItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.type == MedicalReviewTypeEnums.Neonate.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value,
                ),
            )
        }
        cgSignSymptomsObserved.addChipItemList(chipItemList)
    }

    private fun initializeNeonateOutcomeItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.category == MedicalReviewTypeEnums.NeonateOutcome.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value,
                ),
            )
        }
        cgNeonateOutcome.addChipItemList(chipItemList)
    }

    private fun initializeStateOfBabyLabel() {
        getStateOfBabyFlowData().let {
            viewStateOfBaby = SingleSelectionCustomView(binding.root.context)
            viewStateOfBaby.tag = TAG
            viewStateOfBaby.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.stateOfBaby,
                Pair(DefinedParams.StateOfBaby, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                stateOfBabySingleSelectionCallback,
            )
            binding.stateOfBabyGroup.addView(viewStateOfBaby)
        }
    }

    companion object {
        const val TAG = "NeonateFragment"

        fun newInstance(): NeonateFragment = NeonateFragment()
    }

    private fun initializeGenderLabel() {
        getGenderFlowData().let {
            viewGender = SingleSelectionCustomView(binding.root.context)
            viewGender.tag = TAG
            viewGender.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.genderFlow,
                Pair(DefinedParams.GENDER, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                genderSingleSelectionCallback,
            )
            binding.genderLabelGroup.addView(viewGender)
        }
    }

    private var genderSingleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.genderFlow[DefinedParams.GENDER] = selectedID as String
            viewModel.validateSubmitButtonState()
        }

    private var stateOfBabySingleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.stateOfBaby[DefinedParams.StateOfBaby] = selectedID as String
            viewModel.validateSubmitButtonState()
        }

    private fun getGenderFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.male), getString(R.string.male)))
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.female),
                getString(R.string.female),
            ),
        )
        return flowList
    }

    private fun getStateOfBabyFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.normal),
                getString(R.string.normal),
            ),
        )
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.abnormal),
                getString(R.string.abnormal),
            ),
        )
        return flowList
    }

    private fun getOptionMap(value: String): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.ID] = value
        map[DefinedParams.NAME] = value
        return map
    }

    private fun initUI() {
        binding.tvGenderLabel.markMandatory()
        binding.tvNeonateOutcomeLabel.markMandatory()
        if (viewModel.isDirectPnc) {
            val constraintLayout = binding.tvGenderLabel // Replace with your actual ConstraintLayout ID
            val params = constraintLayout.layoutParams as ConstraintLayout.LayoutParams
            params.topToBottom = binding.etNameError.id
            constraintLayout.layoutParams = params
            constraintLayout.requestLayout()
            binding.tvNeonateOutcomeLabel.text = getString(R.string.name)
            binding.etName.visible()
            binding.directPncGroup.gone()
            binding.cgNeonateOutcome.gone()
        } else {
            binding.directPncGroup.gone()
            binding.genderPncGroup.gone()
            binding.etName.gone()
            binding.etNameError.gone()
        }
        agparScoreAdapter = AgparScoreAdapter { rowType, columnType, selectedScore ->
            viewModel.agparRowIdentifier = rowType
            viewModel.agparColumnIdentifier = columnType
            viewModel.agparSelectedScore = selectedScore
            showAgparScoreDialog()
        }
        binding.rvAgparScores.adapter = agparScoreAdapter
        viewModel.getAgparScoreData()

        binding.etBirthWeight.filters = arrayOf(DecimalInputFilter())
        binding.etBirthWeight.doAfterTextChanged {
            val birthWeight = it?.trim().toString().toDoubleOrNull()
            if (birthWeight != null) {
                viewModel.neonateBirthWeight = birthWeight.toString()
                binding.tvBirthWeightCal.text =
                    CommonUtils.birthWeight(birthWeight, requireContext())
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.neonateBirthWeight = null
                binding.tvBirthWeightCal.text = ""
                viewModel.validateSubmitButtonState()
            }
        }
        binding.etName.doAfterTextChanged {
            val name = it?.trim().toString()
            viewModel.name = name
            viewModel.validateSubmitButtonState()
            // Validating the direct pnc child name contains only alphabets
            if (viewModel.isDirectPnc && !CommonUtils.isAlphabetsWithSpace(name)) {
                binding.etNameError.apply {
                    visible()
                    text =
                        getString(R.string.entered_values_should_contain_only_alphabets)
                    requestFocus()
                }
            } else {
                binding.etNameError.apply {
                    gone()
                }
            }
        }

        cgNeonateOutcome =
            TagListCustomView(binding.root.context, binding.cgNeonateOutcome) { _, _, _ ->
                cgNeonateOutcome.getSelectedTags().let {
                    if (it.isNotEmpty()) {
                        viewModel.neonateOutcome = it[0].value
                        if (viewModel.neonateOutcome == MedicalReviewDefinedParams.FreshStillBirth ||
                            viewModel.neonateOutcome == MedicalReviewDefinedParams.MaceratedStillBirth ||
                            viewModel.neonateOutcome.isNullOrEmpty()
                        ) {
                            binding.directPncGroup.gone()
                            binding.genderPncGroup.gone()
                            binding.neonateOvercomePncGroup.visible()
                            binding.tvGenderError.gone()
                            viewModel.apply {
                                neonateBirthWeight = null
                                binding.tvBirthWeightCal.text = ""
                                binding.etBirthWeight.text?.clear()
                                viewStateOfBaby.resetSingleSelectionChildViews()
                                viewGender.resetSingleSelectionChildViews()
                                viewModel.genderFlow.clear()
                                viewModel.stateOfBaby.clear()
                                gestationalAge = null
                                binding.tvGestationalAge.text = ""
                                cgSignSymptomsObserved.clearSelection()
                                neonateSignsAndSymptoms = listOf()
                            }
                        } else {
                            binding.directPncGroup.visible()
                            binding.genderPncGroup.visible()
                        }
                    } else {
                        viewModel.neonateOutcome = null
                    }
                }
                viewModel.validateSubmitButtonState()
            }
        cgSignSymptomsObserved =
            TagListCustomView(
                binding.root.context,
                binding.cgSignsSymptomsObserved,
            ) { _, _, _ ->
                viewModel.neonateSignsAndSymptoms = cgSignSymptomsObserved.getSelectedTags()
                viewModel.validateSubmitButtonState()
            }
    }

    private fun showAgparScoreDialog() {
        AddAgparScoreDialog.newInstance().show(childFragmentManager, AddAgparScoreDialog.TAG)
    }

    fun validateInput(): Boolean {
        var isValid = false
        if (validateCgNeonateOutcome() &&
            validateTextView(
                viewModel.genderFlow,
                binding.tvGenderError,
            ) &&
            validateTextView(viewModel.stateOfBaby, binding.tvStateOfBabyError)
        ) {
            isValid = true
        }
        return isValid
    }

    private fun validateTextView(
        flowData: HashMap<String, Any>,
        textView: AppCompatTextView,
    ): Boolean {
        var isValidOrNot = true
        if (flowData.isEmpty()) {
            textView.isVisible = true
            isValidOrNot = false
        } else {
            textView.isVisible = false
        }
        return isValidOrNot
    }

    private fun validateCgNeonateOutcome(): Boolean {
        var isValidOrNot = true
        if (cgNeonateOutcome.getSelectedTags().isEmpty()) {
            binding.cgNeonateOutcomeError.isVisible = true
            isValidOrNot = false
        } else {
            binding.cgNeonateOutcomeError.isVisible = false
        }
        return isValidOrNot
    }

    fun validate(): Boolean {
        // Initialize validation result as true
        var isValid = true

        // Validate Gender
        if (viewModel.genderFlow[DefinedParams.GENDER] == null) {
            if (viewModel.neonateOutcome == MedicalReviewDefinedParams.FreshStillBirth ||
                viewModel.neonateOutcome == MedicalReviewDefinedParams.MaceratedStillBirth ||
                viewModel.neonateOutcome.isNullOrEmpty()
            ) {
                binding.tvGenderError.isVisible = false
                isValid = true
            } else {
                binding.tvGenderError.isVisible = true
                binding.tvGenderLabel.requestFocus()
                isValid = false
            }
        } else {
            if (viewModel.neonateOutcome == MedicalReviewDefinedParams.FreshStillBirth ||
                viewModel.neonateOutcome == MedicalReviewDefinedParams.MaceratedStillBirth ||
                viewModel.neonateOutcome.isNullOrEmpty()
            ) {
                binding.tvGenderError.isVisible = false
                isValid = true
            } else {
                binding.tvGenderError.isVisible = false
            }
        }

        // Validate Neonate Outcome
        if (viewModel.neonateOutcome == null) {
            binding.cgNeonateOutcomeError.isVisible = true
            binding.cgNeonateOutcomeError.requestFocus()
            isValid = false
        } else {
            binding.cgNeonateOutcomeError.isVisible = false
        }

        // Return final validation result
        return isValid
    }

    class DecimalInputFilter : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int,
        ): CharSequence? {
            val newInput = StringBuilder(dest).replace(dstart, dend, source?.subSequence(start, end).toString())
            val newText = newInput.toString()
            val pattern = Regex("^\\d{0,2}(\\.\\d?)?$")
            return if (newText.matches(pattern)) {
                null // Accept the input
            } else {
                "" // Reject the input
            }
        }
    }
}
