package com.medtroniclabs.opensource.ui.assessment.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isGone
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.calculateBloodGlucose
import com.medtroniclabs.opensource.common.CommonUtils.calculateCVDRiskFactor
import com.medtroniclabs.opensource.common.CommonUtils.calculateProvisionalDiagnosis
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.NON_COMMUNITY
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.data.model.SymptomModel
import com.medtroniclabs.opensource.databinding.FragmentAssessmentNCDBinding
import com.medtroniclabs.opensource.db.entity.MedicalComplianceEntity
import com.medtroniclabs.opensource.db.entity.RiskClassificationModel
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.extension.dp
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.extension.textSizeSsp
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.formgeneration.utility.CheckBoxDialog
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.BloodPressureViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDFormViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.assessment.ComplianceSpinnerAdapter
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import com.medtroniclabs.opensource.ui.common.GeneralInfoDialog
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import java.lang.reflect.Type

class AssessmentNCDFragment : BaseFragment(), FormEventListener, View.OnClickListener {

    private lateinit var binding: FragmentAssessmentNCDBinding
    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()
    private val bpViewModel: BloodPressureViewModel by activityViewModels()
    private var assessmentJSON: List<FormLayout>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssessmentNCDBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFormGenerator()
        attachObserver()
        loadSymptomsAndCompliance()
    }

    private fun initializeFormGenerator() {
        binding.btnSubmit.safeClickListener(this)
        formGenerator = FormGenerator(
            requireContext(), binding.llForm, listener = this, scrollView = binding.scrollView, translate = SecuredPreference.getIsTranslationEnabled()
        ) { map, id ->
            when (id) {
                Screening.Weight, Screening.Height -> {
                    bpViewModel.renderBMIValue(requireContext(), formGenerator, map)
                }
            }
        }
        viewModel.assessmentType = requireArguments().getString(Screening.type)
        viewModel.assessmentType?.let {
            ncdFormViewModel.getNCDForm(
                DefinedParams.Assessment,
                workFlow = it
            )
        }
        bpViewModel.getRiskEntityList()
    }

    private fun getPatientDetails() {
        patientDetailViewModel.origin = requireArguments().getString(DefinedParams.ORIGIN)
        requireArguments().getString(DefinedParams.FhirId)?.let { id ->
            if (CommonUtils.isChp() && requireArguments().getBoolean(MenuConstants.FOLLOW_UP)) {
                patientDetailViewModel.getPatients(id)
            } else {
                patientDetailViewModel.getPatients(
                    id,
                    origin = patientDetailViewModel.origin?.lowercase()
                )
            }
        }

    }

    private fun loadSymptomsAndCompliance() {
        binding.symptomCard.root.visible()
        binding.symptomCard.cardSymptomTitle.text = getString(R.string.symptoms)
        binding.symptomCard.symptomsSpinner.tvTitle.text =
            getString(R.string.since_the_last_assessment_symptom_info)
        binding.symptomCard.symptomsSpinner.tvTitle.markMandatory()
        binding.symptomCard.symptomsSpinner.etUserInput.safeClickListener {
            SymptomsChooseDialog.newInstance()
                .show(childFragmentManager, SymptomsChooseDialog.TAG)
        }
        binding.symptomCard.complianceSpinner.tvTitle.text =
            getString(R.string.complaince_title)
        binding.symptomCard.complianceSpinner.tvTitle.markMandatory()
        viewModel.getMedicationParentComplianceList()
        binding.symptomCard.complianceSpinner.etUserInput
        binding.symptomCard.complianceSpinner.etUserInput.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    pos: Int,
                    p3: Long
                ) {
                    binding.symptomCard.complianceSpinner.etUserInput.adapter?.let { adapter ->
                        adapter as ComplianceSpinnerAdapter
                        adapter.getData(pos)?.let {
                            viewModel.selectedMedication.value = if (it.id > 0) it else null
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }

            }
        binding.symptomCard.otherHypertensionSymptom.tvTitle.text =
            getString(R.string.other_hypertension_symptom)
        binding.symptomCard.otherHypertensionSymptom.tvTitle.markMandatory()
        binding.symptomCard.otherDiabetesSymptom.tvTitle.text =
            getString(R.string.other_diabetes_symptom)
        binding.symptomCard.otherDiabetesSymptom.tvTitle.markMandatory()
        binding.symptomCard.newWorseningHypertensionSymptom.tvTitle.text =
            getString(R.string.new_or_worsening_hypertension)
        binding.symptomCard.newWorseningHypertensionSymptom.tvTitle.markMandatory()
        binding.symptomCard.newWorseningDiabetesSymptom.tvTitle.text =
            getString(R.string.new_or_worsening_diabetes)
        binding.symptomCard.newWorseningDiabetesSymptom.tvTitle.markMandatory()
    }

    private fun attachObserver() {
        ncdFormViewModel.ncdFormResponse.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resources.data?.let {
                        getPatientDetails()
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.selectedMedication.observe(viewLifecycleOwner) { model ->
            hideComplianceOptions()
            model?.let { selectedModel ->
                if (selectedModel.childExists) {
                    viewModel.getMedicationChildComplianceList(selectedModel.id)
                } else hideComplianceOptions()
                addOrRemoveValuesFromResultMap(selectedModel)
            } ?: kotlin.run {
                hideComplianceOptions()
                viewModel.complianceMap?.clear()
            }
            binding.symptomCard.otherComplianceReason.setText(getString(R.string.empty))
            binding.symptomCard.otherComplianceReason.gone()
        }
        viewModel.medicationChildComplianceResponse.observe(viewLifecycleOwner) { list ->
            loadData(list)
        }
        viewModel.selectedSymptoms.observe(viewLifecycleOwner) { selectedSymptoms ->
            loadSymptomsData(selectedSymptoms)
        }
        viewModel.medicationParentComplianceResponse.observe(viewLifecycleOwner) { list ->
            val adapter = ComplianceSpinnerAdapter(
                requireContext(),
                true
            )
            val complianceList = ArrayList(list)
            complianceList.add(
                0,
                MedicalComplianceEntity(
                    id = DefinedParams.DefaultID.toLong(),
                    name = getString(R.string.please_select)
                )
            )
            adapter.setData(complianceList)
            binding.symptomCard.complianceSpinner.etUserInput.adapter = adapter
        }
        patientDetailViewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resourceData ->
            when (resourceData.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    // error exit activity
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceData.data?.let { patientDetails ->
                        if (patientDetails.gender.equals(
                                Screening.Female,
                                true
                            ) && patientDetails.isPregnant == true
                        ) {
                            ncdFormViewModel.ncdFormResponse.value?.data?.let {
                                formGenerator.populateViews(it)
                                assessmentJSON = it
                            }
                        } else {
                            ncdFormViewModel.ncdFormResponse.value?.data?.filterNot {
                                it.id.equals(
                                    AssessmentDefinedParams.pregnancyAnc,
                                    true
                                ) || it.family.equals(AssessmentDefinedParams.pregnancyAnc, true)
                            }?.let {
                                formGenerator.populateViews(it)
                                assessmentJSON = it
                            }
                        }
                    }
                }
            }
        }
        bpViewModel.getRiskEntityListLiveData.observe(viewLifecycleOwner) {

        }
        viewModel.mentalHealthQuestions.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { localMHResponse ->
                        localMHResponse.forEach {
                            formGenerator.loadMentalHealthQuestions(it.value)
                        }
                    }
                }
            }
        }
    }

    private fun autoPopulateDetails(data: PatientListRespModel?) {
        parseDiagnosisData(data)
        showPatientInfoCard(data)
    }

    private fun parseDiagnosisData(data: PatientListRespModel?) {
        val allMentalHealthForms = listOf(
            Screening.PHQ4,
            AssessmentDefinedParams.PHQ9,
            AssessmentDefinedParams.GAD7
        )
        data?.mentalHealthLevels?.let { types ->
            if (types.isEmpty()) {
                formGenerator.showMHView(false, allMentalHealthForms)
            } else {
                val (showMH, hideMH) = allMentalHealthForms.partition { it in types }
                formGenerator.showMHView(true, showMH)
                formGenerator.showMHView(false, hideMH)
            }
        } ?: run {
            formGenerator.showMHView(false, allMentalHealthForms)
        }
    }

    private fun showPatientInfoCard(screeningDetailsModel: PatientListRespModel?) {
        viewModel.bioDataMap = HashMap()
        viewModel.bioMetric = HashMap()
        binding.llPatientInfo.visible()

        binding.patientName.tvKey.text = getString(R.string.name)
        screeningDetailsModel?.name?.let { name ->
            binding.patientName.tvValue.text = name
        }
        binding.nationalId.tvKey.text = getString(R.string.national_id)
        viewModel.bioDataMap?.apply {
            screeningDetailsModel?.firstName?.let { firstName ->
                this[Screening.First_Name] = firstName
            }
            screeningDetailsModel?.middleName?.let { middleName ->
                this[AssessmentDefinedParams.Middle_Name] = middleName
            }
            screeningDetailsModel?.lastName?.let { lastName ->
                this[Screening.lastName] = lastName
            }
            screeningDetailsModel?.phoneNumber?.let { phoneNumber ->
                this[Screening.phoneNumber] = phoneNumber
            }
            screeningDetailsModel?.phoneNumberCategory?.let { phoneNumberCategory ->
                this[AssessmentDefinedParams.phoneNumberCategory] = phoneNumberCategory
            }
            screeningDetailsModel?.landmark?.let { landmark ->
                this[AssessmentDefinedParams.landmark] = landmark
            }
            screeningDetailsModel?.identityType?.let { identityType ->
                screeningDetailsModel.identityValue?.let { identityValue ->
                    binding.nationalId.tvValue.text = identityValue
                    this[Screening.identityType] = identityType
                    this[Screening.identityValue] = identityValue
                }
            }
        }

        binding.gender.tvKey.text = getString(R.string.gender)
        screeningDetailsModel?.gender?.let { gender ->
            binding.gender.tvValue.text = gender.replaceFirstChar(Char::titlecase)
            viewModel.bioMetric?.apply {
                this[DefinedParams.Gender] = gender
            }
            if (gender.equals(Screening.Female, true) && screeningDetailsModel.isPregnant == true) {
                autoPopulatePregnancyAnc(screeningDetailsModel)
                formGenerator.showHideCardFamily(true, AssessmentDefinedParams.pregnancyAnc)
            } else {
                formGenerator.showHideCardFamily(false, AssessmentDefinedParams.pregnancyAnc)
            }
        }

        binding.dobAge.tvKey.text = getString(R.string.age)
        screeningDetailsModel?.age?.let { age ->
            binding.dobAge.tvValue.text = CommonUtils.getDecimalFormatted(age)
            viewModel.bioMetric?.apply {
                this[Screening.Age] = age.toString()
            }
        }
        screeningDetailsModel?.height?.let { height ->
            viewModel.bioMetric?.apply {
                this[Screening.Height] = height.toString()
            }
        }
        screeningDetailsModel?.weight?.let { weight ->
            viewModel.bioMetric?.apply {
                this[Screening.Weight] = weight.toString()
            }
        }
        screeningDetailsModel?.weight?.takeIf { it > 0 }?.let { weight ->
            screeningDetailsModel.height?.takeIf { it > 0 }?.let { height ->
                viewModel.bioMetric?.apply {
                    this[Screening.Weight] = weight.toString()
                    this[Screening.BMI] = CommonUtils.getBMIForNcd(
                        height,
                        weight
                    )?.toDoubleOrNull() ?: 0.0
                }
            }
        }
        showHeightWeight(
            screeningDetailsModel?.height?.takeIf { it > 0 },
            screeningDetailsModel?.weight?.takeIf { it > 0 }
        )
    }

    private fun autoPopulatePregnancyAnc(screeningDetailsModel: PatientListRespModel) {
        screeningDetailsModel.let { details ->
            details.isPregnant?.let { isPregnantOrNot ->
                formGenerator.getViewByTag(AssessmentDefinedParams.PregnancyStatus)?.let { view ->
                    if (view is TextView) {
                        populatePregnancyStatus(view, isPregnantOrNot)
                    }
                }
            } ?: kotlin.run {
                formGenerator.getViewByTag(AssessmentDefinedParams.PregnancySymptoms + AssessmentDefinedParams.rootSuffix)
                    ?.isGone()
            }
            details.pregnancyDetails?.lastMenstrualPeriod?.let { mensrualDate ->
                calculateGestationalPeriod(mensrualDate)
            }
        }
    }

    private fun calculateGestationalPeriod(mensrualDate: String) {
        formGenerator.getViewByTag(AssessmentDefinedParams.GestationalPeriod)?.let { view ->
            val lastMenstrualDate = mensrualDate.let { DateUtils.getLastMenstrualDate(it) }
            val gestationWeek =
                lastMenstrualDate.let { DateUtils.calculateGestationalAge(it).first.toInt() }
            gestationWeek.let { weeks ->
                val totalWeeks = if (weeks < AssessmentDefinedParams.PregnancyANCMaxValue) {
                    weeks
                } else {
                    AssessmentDefinedParams.PregnancyANCMaxValue
                }
                if (view is TextView) {
                    view.text = getString(R.string.gestational_weeks, totalWeeks)
                }
            }
        }
    }

    private fun populatePregnancyStatus(view: TextView, isPregnantOrNot: Boolean) {
        if (isPregnantOrNot) {
            view.text = getString(R.string.positive)
            formGenerator.getViewByTag(AssessmentDefinedParams.PregnancySymptoms + AssessmentDefinedParams.rootSuffix)
                ?.isVisible()
        } else {
            view.text = getString(R.string.negative)
            formGenerator.getViewByTag(AssessmentDefinedParams.PregnancySymptoms + AssessmentDefinedParams.rootSuffix)
                ?.isGone()
        }
    }

    private fun loadSymptomsData(selectedSymptoms: List<SymptomModel>?) {
        if (selectedSymptoms.isNullOrEmpty()) {
            binding.symptomCard.symptomsSpinner.etUserInput.text = getString(R.string.empty)
            binding.symptomCard.otherHypertensionSymptom.llRoot.gone()
            binding.symptomCard.otherHypertensionSymptom.etUserInput.text?.clear()
            binding.symptomCard.otherDiabetesSymptom.llRoot.gone()
            binding.symptomCard.otherDiabetesSymptom.etUserInput.text?.clear()
            binding.symptomCard.newWorseningHypertensionSymptom.llRoot.gone()
            binding.symptomCard.newWorseningHypertensionSymptom.etUserInput.text?.clear()
            binding.symptomCard.newWorseningDiabetesSymptom.llRoot.gone()
            binding.symptomCard.newWorseningDiabetesSymptom.etUserInput.text?.clear()
        } else
            binding.symptomCard.symptomsSpinner.etUserInput.text =
                getSelectedSymptomsText(selectedSymptoms)
    }

    private fun getSelectedSymptomsText(selectedSymptoms: List<SymptomModel>): String {
        val otherSelected =
            selectedSymptoms.filter { it.symptom.startsWith(DefinedParams.Other, true) }
        val noSymptomsCount =
            selectedSymptoms.filter {
                it.symptom.startsWith(
                    AssessmentDefinedParams.NoSymptoms,
                    true
                )
            }.size
        val newWorseningSymptoms =
            selectedSymptoms.filter {
                it.symptom.startsWith(
                    getString(R.string.new_or_worsening_symptoms),
                    true
                )
            }
        val stringBuilder: StringBuilder = StringBuilder()
        if (noSymptomsCount == selectedSymptoms.size) {
            stringBuilder.append(getString(R.string.no))
        } else {
            if (otherSelected.isEmpty()) {
                stringBuilder.append((selectedSymptoms.size - noSymptomsCount))
            } else {
                stringBuilder.append((selectedSymptoms.size - noSymptomsCount) - otherSelected.size)
            }
        }
        if (newWorseningSymptoms.isNotEmpty()) {
            newWorseningSymptomsType(newWorseningSymptoms)
        } else {
            binding.symptomCard.newWorseningHypertensionSymptom.llRoot.gone()
            binding.symptomCard.newWorseningHypertensionSymptom.etUserInput.setText(getString(R.string.empty))
            binding.symptomCard.newWorseningDiabetesSymptom.llRoot.gone()
            binding.symptomCard.newWorseningDiabetesSymptom.etUserInput.setText(getString(R.string.empty))
        }
        stringBuilder.append(getString(R.string.empty_space))

        if (otherSelected.isNotEmpty()) {
            stringBuilder.append(getString(R.string.other_selected))
            if (otherSelected.any {
                    it.type.equals(
                        AssessmentDefinedParams.Compliance_Type_Diabetes,
                        true
                    )
                }) {
                binding.symptomCard.otherDiabetesSymptom.llRoot.visible()
            } else {
                binding.symptomCard.otherDiabetesSymptom.llRoot.gone()
                binding.symptomCard.otherDiabetesSymptom.etUserInput.setText(getString(R.string.empty))
            }
            if (otherSelected.any {
                    it.type.equals(
                        AssessmentDefinedParams.Compliance_Type_Hypertension,
                        true
                    )
                }) {
                binding.symptomCard.otherHypertensionSymptom.llRoot.visible()
            } else {
                binding.symptomCard.otherHypertensionSymptom.llRoot.gone()
                binding.symptomCard.otherHypertensionSymptom.etUserInput.setText(getString(R.string.empty))
            }
        } else {
            stringBuilder.append(getString(R.string.symptoms_selected))
            binding.symptomCard.otherDiabetesSymptom.llRoot.gone()
            binding.symptomCard.otherDiabetesSymptom.etUserInput.setText(getString(R.string.empty))
            binding.symptomCard.otherHypertensionSymptom.llRoot.gone()
            binding.symptomCard.otherHypertensionSymptom.etUserInput.setText(getString(R.string.empty))
        }
        return stringBuilder.toString()
    }

    private fun newWorseningSymptomsType(newWorseningSymptoms: List<SymptomModel>) {
        if (newWorseningSymptoms.any {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Diabetes,
                    true
                )
            }) {
            binding.symptomCard.newWorseningDiabetesSymptom.llRoot.visible()
        } else {
            binding.symptomCard.newWorseningDiabetesSymptom.llRoot.gone()
            binding.symptomCard.newWorseningDiabetesSymptom.etUserInput.setText(getString(R.string.empty))
        }
        if (newWorseningSymptoms.any {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Hypertension,
                    true
                )
            }) {
            binding.symptomCard.newWorseningHypertensionSymptom.llRoot.visible()
        } else {
            binding.symptomCard.newWorseningHypertensionSymptom.llRoot.gone()
            binding.symptomCard.newWorseningHypertensionSymptom.etUserInput.setText(getString(R.string.empty))
        }
    }

    private fun loadData(medicalComplianceList: List<MedicalComplianceEntity>?) {
        medicalComplianceList?.let { list ->
            binding.symptomCard.childCompliance.clearCheck()
            if (list.isNotEmpty()) {
                binding.symptomCard.childCompliance.visible()
                loadRadioButtons(list, binding.symptomCard.childCompliance, 2)
            } else {
                binding.symptomCard.childCompliance.gone()
                binding.symptomCard.tvChildErrorMessage.gone()
            }
        }
    }

    private fun loadRadioButtons(
        list: List<MedicalComplianceEntity>,
        complianceRadioGroup: RadioGroup,
        parent: Int
    ) {
        complianceRadioGroup.removeAllViews()
        list.forEachIndexed { index, model ->
            val radioButton = RadioButton(complianceRadioGroup.context)
            radioButton.id = index
            radioButton.tag = model.id
            radioButton.setPadding(20.dp, 0, 20.dp, 0)
            val colorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(-android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_checked)
                ), intArrayOf(
                    ContextCompat.getColor(
                        complianceRadioGroup.context,
                        R.color.purple
                    ),  // disabled
                    ContextCompat.getColor(complianceRadioGroup.context, R.color.purple) // enabled
                )
            )

            radioButton.buttonTintList = colorStateList
            radioButton.invalidate()
            radioButton.textSizeSsp = AssessmentDefinedParams.SSP16
            radioButton.text = model.displayValue ?: model.name
            radioButton.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            radioButton.setTextColor(
                ContextCompat.getColor(
                    complianceRadioGroup.context,
                    R.color.navy_blue
                )
            )
            radioButton.typeface =
                ResourcesCompat.getFont(complianceRadioGroup.context, R.font.inter_regular)
            complianceRadioGroup.addView(radioButton)
        }

        complianceRadioGroup.setOnCheckedChangeListener { _, id ->
            if (id > DefinedParams.DefaultID.toLong()) {
                val selectedModel = list[id]
                if (parent == 1) {
                    showOrHideCompliances(selectedModel)
                    binding.symptomCard.otherComplianceReason.setText(getString(R.string.empty))
                    binding.symptomCard.otherComplianceReason.gone()
                } else if (parent == 2) {
                    if (selectedModel.name.equals(DefinedParams.Other, true)) {
                        binding.symptomCard.otherComplianceReason.visible()
                    } else {
                        binding.symptomCard.otherComplianceReason.setText(getString(R.string.empty))
                        binding.symptomCard.otherComplianceReason.gone()
                    }
                }
                addOrRemoveValuesFromResultMap(selectedModel)
            }
        }
    }

    private fun showOrHideCompliances(selectedModel: MedicalComplianceEntity) {
        if (selectedModel.childExists) {
            viewModel.getMedicationChildComplianceList(selectedModel.id)
        } else hideComplianceOptions()
    }

    private fun hideComplianceOptions() {
        viewModel.complianceMap?.clear()
        binding.symptomCard.childCompliance.clearCheck()
        binding.symptomCard.childCompliance.gone()
        binding.symptomCard.otherComplianceReason.gone()
        binding.symptomCard.otherComplianceReason.setText("")
        binding.symptomCard.tvChildErrorMessage.gone()
    }

    private fun addOrRemoveValuesFromResultMap(
        selectedModel: MedicalComplianceEntity
    ) {
        val map: HashMap<String, Any>
        if (viewModel.complianceMap != null) {
            map = HashMap()
            if (selectedModel.parentComplianceId == null) {
                viewModel.complianceMap?.clear()
            } else {
                val newList = ArrayList<HashMap<String, Any>>()
                viewModel.complianceMap?.forEach { compMap ->
                    if (compMap.containsKey(AssessmentDefinedParams.complianceId) && map[AssessmentDefinedParams.complianceId] == selectedModel.parentComplianceId) {
                        newList.add(compMap)
                    }
                }
                val childList = viewModel.complianceMap?.filter { compMap ->
                    compMap[AssessmentDefinedParams.complianceId] != selectedModel.parentComplianceId
                }

                if (!childList.isNullOrEmpty()) {
                    viewModel.complianceMap?.clear()
                }
                viewModel.complianceMap?.addAll(newList)
            }
            if (selectedModel.childExists) {
                map[AssessmentDefinedParams.is_child_exists] = true
            }
            map[AssessmentDefinedParams.complianceId] = selectedModel.id
            map[DefinedParams.NAME] = selectedModel.name
            selectedModel.value?.let { value ->
                map[DefinedParams.Value] = value
            }
            selectedModel.displayValue?.let { cultureValue ->
                map[DefinedParams.cultureValue] = cultureValue
            }
            viewModel.complianceMap?.add(map)
        } else {
            viewModel.complianceMap = ArrayList()
            map = HashMap()
            map[AssessmentDefinedParams.complianceId] = selectedModel.id
            map[DefinedParams.NAME] = selectedModel.name
            selectedModel.value?.let { value ->
                map[DefinedParams.Value] = value
            }
            selectedModel.displayValue?.let { cultureValue ->
                map[DefinedParams.cultureValue] = cultureValue
            }
            if (selectedModel.childExists) {
                map[AssessmentDefinedParams.is_child_exists] = true
            }
            viewModel.complianceMap?.add(map)
        }
    }

    private fun showErrorMessage(message: String, view: TextView) {
        if (binding.symptomCard.root.isVisible())
            formGenerator.scrollToView(binding.scrollView, binding.symptomCard.viewSymptomCardBG)
        view.visible()
        view.text = message
    }

    private fun hideErrorMessage(view: TextView) {
        view.gone()
    }

    private fun checkComplianceDiabetes(other: List<SymptomModel>): Boolean? {
        var isValid: Boolean? = null
        if (other.any { it.type.equals(AssessmentDefinedParams.Compliance_Type_Diabetes, true) }) {
            hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
            val otherDiabetesText =
                binding.symptomCard.otherDiabetesSymptom.etUserInput.text
            if (otherDiabetesText.isNullOrBlank()) {
                isValid = false
                showErrorMessage(
                    getString(R.string.default_user_input_error),
                    binding.symptomCard.otherDiabetesSymptom.tvErrorMessage
                )
            } else {
                val filteredOther = other.first { symptomOther ->
                    symptomOther.type.equals(AssessmentDefinedParams.Compliance_Type_Diabetes, true)
                }
                filteredOther.otherSymptom = otherDiabetesText.toString()
                hideErrorMessage(binding.symptomCard.otherDiabetesSymptom.tvErrorMessage)
            }
        } else {
            hideErrorMessage(binding.symptomCard.otherDiabetesSymptom.tvErrorMessage)
        }
        return isValid
    }

    private fun checkComplianceHypertension(other: List<SymptomModel>): Boolean? {
        var isValid: Boolean? = null
        if (other.any {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Hypertension,
                    true
                )
            }) {
            hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
            val otherHyperTensionText =
                binding.symptomCard.otherHypertensionSymptom.etUserInput.text
            if (otherHyperTensionText.isNullOrBlank()) {
                isValid = false
                showErrorMessage(
                    getString(R.string.default_user_input_error),
                    binding.symptomCard.otherHypertensionSymptom.tvErrorMessage
                )
            } else {
                val filteredOther = other.first { symptomOther ->
                    symptomOther.type.equals(
                        AssessmentDefinedParams.Compliance_Type_Hypertension,
                        true
                    )
                }
                filteredOther.otherSymptom = otherHyperTensionText.toString()
                hideErrorMessage(binding.symptomCard.otherHypertensionSymptom.tvErrorMessage)
            }
        } else {
            hideErrorMessage(binding.symptomCard.otherHypertensionSymptom.tvErrorMessage)
        }
        return isValid
    }

    private fun checkNewWorseningDiabetes(newWorsening: List<SymptomModel>): Boolean? {
        var isValid: Boolean? = null
        if (newWorsening.any {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Diabetes,
                    true
                )
            }) {
            hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
            val newWorseningDiabetesText =
                binding.symptomCard.newWorseningDiabetesSymptom.etUserInput.text
            if (newWorseningDiabetesText.isNullOrBlank()) {
                isValid = false
                showErrorMessage(
                    getString(R.string.default_user_input_error),
                    binding.symptomCard.newWorseningDiabetesSymptom.tvErrorMessage
                )
            } else {
                val filteredNewWorsening = newWorsening.first { symptomNewWorsening ->
                    symptomNewWorsening.type.equals(
                        AssessmentDefinedParams.Compliance_Type_Diabetes,
                        true
                    )
                }
                filteredNewWorsening.otherSymptom = newWorseningDiabetesText.toString()
                hideErrorMessage(binding.symptomCard.newWorseningDiabetesSymptom.tvErrorMessage)
            }
        } else {
            hideErrorMessage(binding.symptomCard.newWorseningDiabetesSymptom.tvErrorMessage)
        }
        return isValid
    }

    private fun validateSymptom(): Boolean {
        var isValidData = true
        if (!viewModel.selectedSymptoms.value.isNullOrEmpty()) {
            viewModel.selectedSymptoms.value?.let {
                val selectedSymptom = viewModel.selectedSymptoms.value
                if (!selectedSymptom.isNullOrEmpty()) {
                    val other = selectedSymptom.filter {
                        it.symptom.startsWith(
                            DefinedParams.Other,
                            true
                        )
                    }
                    if (other.isNotEmpty()) {
                        checkComplianceDiabetes(other)?.let {
                            isValidData = it
                        }
                        checkComplianceHypertension(other)?.let {
                            isValidData = it
                        }
                    } else
                        hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
                    val newWorsening = selectedSymptom.filter {
                        it.symptom.startsWith(
                            getString(R.string.new_or_worsening_symptoms),
                            true
                        )
                    }
                    if (newWorsening.isNotEmpty()) {
                        checkNewWorseningDiabetes(newWorsening)?.let {
                            isValidData = it
                        }
                        checkNewWorseningHypertension(newWorsening)?.let {
                            isValidData = it
                        }
                    } else
                        hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
                } else
                    hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
            }
        } else {
            isValidData = false
            showErrorMessage(
                getString(R.string.error_message_spinner),
                binding.symptomCard.symptomsSpinner.tvErrorMessage
            )
        }
        return isValidData
    }

    private fun getSymptomsList(list: List<SymptomModel>): ArrayList<HashMap<String, Any>> {
        val resultList = ArrayList<HashMap<String, Any>>()
        list.forEach {
            val map = HashMap<String, Any>()
            map[DefinedParams.NAME] = it.symptom
            map[AssessmentDefinedParams.id] = it.id
            it.otherSymptom?.let { other ->
                map[AssessmentDefinedParams.other_symptom] = other
            }
            it.type?.let { type ->
                map[Screening.type] = type
            }
            it.value?.let { value ->
                map[DefinedParams.Value] = value
            }
            it.cultureValue?.let { cultureValue ->
                map[DefinedParams.cultureValue] = cultureValue
            }
            resultList.add(map)
        }

        return resultList
    }

    private fun checkNewWorseningHypertension(newWorsening: List<SymptomModel>): Boolean? {
        var isValid: Boolean? = null
        if (newWorsening.any {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Hypertension,
                    true
                )
            }) {
            hideErrorMessage(binding.symptomCard.symptomsSpinner.tvErrorMessage)
            val newWorseningHyperTensionText =
                binding.symptomCard.newWorseningHypertensionSymptom.etUserInput.text
            if (newWorseningHyperTensionText.isNullOrBlank()) {
                isValid = false
                showErrorMessage(
                    getString(R.string.default_user_input_error),
                    binding.symptomCard.newWorseningHypertensionSymptom.tvErrorMessage
                )
            } else {
                val filteredNewWorsening = newWorsening.first { symptomNewWorsening ->
                    symptomNewWorsening.type.equals(
                        AssessmentDefinedParams.Compliance_Type_Hypertension,
                        true
                    )
                }
                filteredNewWorsening.otherSymptom = newWorseningHyperTensionText.toString()
                hideErrorMessage(binding.symptomCard.newWorseningHypertensionSymptom.tvErrorMessage)
            }
        } else {
            hideErrorMessage(binding.symptomCard.newWorseningHypertensionSymptom.tvErrorMessage)
        }
        return isValid
    }

    private fun proceedToSaveAssessment(
        resultHashMap: HashMap<String, Any>,
        serverData: List<FormLayout?>?,
        unitGenericType: String
    ) {
        if (viewModel.complianceMap != null) {
            viewModel.complianceMap?.let { complianceList ->
                // Set compliance list in resultHashMap
                // Find compliance item with name "Other" (case-insensitive) and add "other_compliance" if reason is non-empty
                binding.symptomCard.otherComplianceReason.text?.trim()?.toString()
                    ?.takeIf { it.isNotEmpty() }?.let { reason ->
                        complianceList.find { data ->
                            (data[DefinedParams.NAME] as? String)?.equals(
                                DefinedParams.Other,
                                ignoreCase = true
                            ) == true
                        }?.put(AssessmentDefinedParams.other_compliance, reason)
                    }

                resultHashMap[AssessmentDefinedParams.compliance] = complianceList
            }
        }
        if (!viewModel.selectedSymptoms.value.isNullOrEmpty()) {
            viewModel.selectedSymptoms.value?.let {
                resultHashMap[AssessmentDefinedParams.symptomsDTO] =
                    getSymptomsList(it)
            }
        }
        saveAssessmentBaseValues(resultHashMap, serverData, unitGenericType)
    }

    private fun otherComplianceCheck(complianceMap: ArrayList<HashMap<String, Any>>): Boolean? {
        var isValid: Boolean? = null
        if (binding.symptomCard.otherComplianceReason.isVisible()) {
            val reason = binding.symptomCard.otherComplianceReason.text
            if (reason.isNullOrBlank()) {
                isValid = false
                showErrorMessage(
                    getString(R.string.default_user_input_error),
                    binding.symptomCard.tvChildErrorMessage
                )
            } else {
                complianceMap[1][AssessmentDefinedParams.other_compliance] =
                    reason.toString()
                hideErrorMessage(binding.symptomCard.tvChildErrorMessage)
            }
        } else
            hideErrorMessage(binding.symptomCard.tvChildErrorMessage)
        return isValid
    }

    private fun saveAssessmentBaseValues(
        resultHashMap: HashMap<String, Any>,
        serverData: List<FormLayout?>?,
        unitGenericType: String
    ) {
        viewModel.bioDataMap?.let { bioData ->
            if (bioData.isNotEmpty()) {
                resultHashMap[DefinedParams.BioData] = bioData
            }
        }
        viewModel.bioMetric?.let { bioMetric ->
            if (bioMetric.isNotEmpty()) {
                resultHashMap[Screening.BioMetrics] = bioMetric
            }
        }
        processValuesAndProceed(resultHashMap, serverData, unitGenericType)
    }

    private fun validateCompliance(): Boolean {
        var isValidData = true
        val complianceMap = viewModel.complianceMap
        if (complianceMap.isNullOrEmpty()) {
            isValidData = false
            showErrorMessage(
                getString(R.string.default_user_input_error),
                binding.symptomCard.complianceSpinner.tvErrorMessage
            )
        } else {
            hideErrorMessage(binding.symptomCard.complianceSpinner.tvErrorMessage)

            val selectedModel = complianceMap[0]
            if (selectedModel.containsKey(AssessmentDefinedParams.is_child_exists) && (selectedModel[AssessmentDefinedParams.is_child_exists] as? Boolean) == true) {
                if (complianceMap.size < 2) {
                    isValidData = false
                    showErrorMessage(
                        getString(R.string.default_user_input_error),
                        binding.symptomCard.tvChildErrorMessage
                    )
                } else
                    otherComplianceCheck(complianceMap)?.let {
                        isValidData = it
                    }
            }
        }

        return isValidData
    }

    private fun processValuesAndProceed(
        resultHashMap: HashMap<String, Any>,
        serverData: List<FormLayout?>?,
        unitGenericType: String
    ) {
        val map = HashMap<String, Any>()
        map.putAll(resultHashMap)
        calculateBloodGlucose(map, true) { (fbs, rbs) ->
            if (fbs != null) {
                viewModel.setFbsBloodGlucose(fbs)
            }
            if (rbs != null) {
                viewModel.setRbsBloodGlucose(rbs)
            }
        }
        CommonUtils.calculateBMI(map)
        CommonUtils.calculateAverageBloodPressure(map)
        var isConfirmDiagnosis = false
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { searchResponse ->
            isConfirmDiagnosis = searchResponse.isConfirmDiagnosis
            searchResponse.id?.let { id ->
                map[AssessmentDefinedParams.memberReference] = id
            }
            searchResponse.patientId?.let { id ->
                map[AssessmentDefinedParams.patientReference] = id
            }
            searchResponse.dateOfBirth?.let { dateOfBirth ->
                map[Screening.DateOfBirth] = dateOfBirth
            }
            searchResponse.isRegularSmoker?.let { isRegularSmoker ->
                map[Screening.is_regular_smoker] = isRegularSmoker
            }
            searchResponse.gender?.let { gender ->
                map[DefinedParams.Gender] = gender
            }
        }
        map[AssessmentDefinedParams.assessmentType] = NON_COMMUNITY
        map[AssessmentDefinedParams.assessmentProcessType] =
            AssessmentDefinedParams.africa_uppercase
        map[AssessmentDefinedParams.assessmentTakenOn] = DateUtils.getTodayDateDDMMYYYY()
        map[AssessmentDefinedParams.assessmentOrganizationId] =
            SecuredPreference.getOrganizationFhirId()
        if (viewModel.assessmentType.equals(AssessmentDefinedParams.ncd, true)) {
            assessmentJSON?.first { it.viewType.equals(ViewType.VIEW_TYPE_FORM_BP, true) }?.let {
                bpViewModel.calculateBPValues(it, map)
            }
        }
        calculateProvisionalDiagnosis(
            map, isConfirmDiagnosis,
            bpViewModel.getSystolicAverage(), bpViewModel.getDiastolicAverage(),
            viewModel.getFbsBloodGlucose(), viewModel.getRbsBloodGlucose(),
            CommonUtils.getMeasurementTypeValues(map)
        )
        val resultOne = bpViewModel.getRiskEntityListLiveData.value
        val baseType: Type = object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
        if (resultOne?.isNotEmpty() == true) {
            val resultList =
                Gson().fromJson<ArrayList<RiskClassificationModel>>(
                    resultOne[0].nonLabEntity,
                    baseType
                )
            if (viewModel.assessmentType.equals(AssessmentDefinedParams.ncd, true)) {
                calculateCVDRiskFactor(
                    map,
                    ArrayList(resultList),
                    bpViewModel.getSystolicAverage()
                )
            }
        }
        map[Screening.UnitMeasurement] = SecuredPreference.getUnitMeasurementType()
        if (map.containsKey(Screening.MentalHealthDetails))
            viewModel.setPhQ4Score(CommonUtils.calculatePHQScore(map))

        if (map.containsKey(AssessmentDefinedParams.PHQ9_Mental_Health))
            CommonUtils.calculatePHQScore(map, type = AssessmentDefinedParams.PHQ9)

        if (map.containsKey(AssessmentDefinedParams.GAD7_Mental_Health))
            CommonUtils.calculatePHQScore(map, type = AssessmentDefinedParams.GAD7)
        calculateFurtherAssessment(map, unitGenericType, serverData)
        try {
            var result = serverData?.let {
                FormResultComposer().groupValues(
                    context = requireContext(),
                    serverData = it,
                    map,
                    bmiCategoryGroupId = Screening.bp_log
                )
            }
            val organizationId = hashMapOf<String, Any>(
                AssessmentDefinedParams.organizationId to SecuredPreference.getOrganizationFhirId(),
                AssessmentDefinedParams.userId to SecuredPreference.getUserFhirId(),
                AssessmentDefinedParams.modifiedDate to System.currentTimeMillis()
                    .convertToUtcDateTime(),
                AssessmentDefinedParams.spiceUserId to SecuredPreference.getUserId()
            )
            val provenance = hashMapOf<String, Any>(
                DefinedParams.Provenance to organizationId
            )
            val bioMetric = result?.second?.get(Screening.BioMetrics) as HashMap<String, Any>
            if (result?.second?.containsKey(Screening.bp_log) == true) {
                val bpLog = result?.second?.get(Screening.bp_log) as HashMap<String, Any>
                if (bpLog.containsKey(AssessmentDefinedParams.Temperature)) {
                    result.second[AssessmentDefinedParams.Temperature] =
                        bpLog[AssessmentDefinedParams.Temperature] as Double
                }
                bioMetric[Screening.BMI] = bpLog[Screening.BMI] as Double
                // if height weight is change we need to set it
                bioMetric.let { bioMetric ->
                    listOf(Screening.Height, Screening.Weight, Screening.BMI).forEach { key ->
                        bpLog[key]?.let { value ->
                            bioMetric[key] = value as Double
                        }
                    }
                }
                bpLog.apply {
                    remove(AssessmentDefinedParams.Temperature)
                    remove(Screening.Weight)
                    remove(Screening.Height)
                    remove(Screening.BMI)
                }
            }
            if (result?.second?.containsKey(AssessmentDefinedParams.PHQ9.lowercase()) == true) {
                val phq9 =
                    result?.second?.get(AssessmentDefinedParams.PHQ9.lowercase()) as HashMap<String, Any>
                if (phq9.isEmpty()) {
                    result?.second?.apply {
                        remove(AssessmentDefinedParams.PHQ9.lowercase())
                    }
                } else {
                    val score = phq9[AssessmentDefinedParams.PHQ9_Score] as Int
                    phq9[Screening.PHQ4_Score] = score
                    phq9.remove(AssessmentDefinedParams.PHQ9_Score)

                    val riskLevel = phq9[AssessmentDefinedParams.PHQ9_Risk_Level] as String
                    phq9[Screening.RiskLevel] = riskLevel
                    phq9.remove(AssessmentDefinedParams.PHQ9_Risk_Level)

                    val mentalHealth =
                        phq9[AssessmentDefinedParams.PHQ9_Mental_Health] as ArrayList<*>
                    phq9[Screening.MentalHealthDetails] = mentalHealth
                    phq9.remove(AssessmentDefinedParams.PHQ9_Mental_Health)
                }
            }
            if (result?.second?.containsKey(Screening.PHQ4.lowercase()) == true) {
                val phq4 = result?.second?.get(Screening.PHQ4.lowercase()) as HashMap<String, Any>
                if (phq4.isEmpty()) {
                    result?.second?.apply {
                        remove(Screening.PHQ4.lowercase())
                    }
                }
            }
            if (result?.second?.containsKey(AssessmentDefinedParams.GAD7.lowercase()) == true) {
                val gad7 =
                    result?.second?.get(AssessmentDefinedParams.GAD7.lowercase()) as HashMap<String, Any>
                if (gad7.isEmpty()) {
                    result?.second?.apply {
                        remove(AssessmentDefinedParams.GAD7.lowercase())
                    }
                } else {
                    val score = gad7[AssessmentDefinedParams.GAD7_Score] as Int
                    gad7[Screening.PHQ4_Score] = score
                    gad7.remove(AssessmentDefinedParams.GAD7_Score)

                    val riskLevel = gad7[AssessmentDefinedParams.GAD7_Risk_Level] as String
                    gad7[Screening.RiskLevel] = riskLevel
                    gad7.remove(AssessmentDefinedParams.GAD7_Risk_Level)

                    val mentalHealth =
                        gad7[AssessmentDefinedParams.GAD7_Mental_Health] as ArrayList<*>
                    gad7[Screening.MentalHealthDetails] = mentalHealth
                    gad7.remove(AssessmentDefinedParams.GAD7_Mental_Health)
                }
            }
            result?.second?.apply {
                this[AssessmentDefinedParams.encounter] = provenance
                this[AssessmentDefinedParams.assessmentType] = NON_COMMUNITY
                remove(DefinedParams.Gender)
                remove(Screening.is_regular_smoker)
                remove(Screening.DateOfBirth)
                remove(Screening.referredReasons)
            }

            // TODO we have to verified it, needed or not once we are working for anc and pregnancy
            val isPregnant =
                patientDetailViewModel.patientDetailsLiveData.value?.data?.isPregnant
            if (isPregnant == true) {
                CommonUtils.addAncEnableOrNot(
                    result.second,
                    key = Screening.pregnancyAnc,
                    true
                )
            }
            result = Pair(StringConverter.convertGivenMapToString(result.second), result.second)
            result.first?.let {
                viewModel.saveAssessmentInformation(it, false, false)
            }
        } catch (_: Exception) {
            //Exception - Catch block
        }
    }

    private fun calculateFurtherAssessment(
        map: HashMap<String, Any>,
        unitGenericType: String,
        serverData: List<FormLayout?>?
    ) {
        val assessmentConditionResult = CommonUtils.checkAssessmentCondition(
            bpViewModel.getSystolicAverage(),
            bpViewModel.getDiastolicAverage(),
            viewModel.getPhQ4Score(),
            Pair(viewModel.getFbsBloodGlucose(), viewModel.getRbsBloodGlucose()),
            unitGenericType,
            getPregnancySymptomCount(map),
            Pair(null, map),
            serverData
        )
        map[Screening.ReferAssessment] =
            if (assessmentConditionResult.first) Screening.PositiveValue else Screening.NegativeValue

        if (assessmentConditionResult.second.isNotEmpty())
            map[Screening.referredReasons] = assessmentConditionResult.second
    }

    private fun getPregnancySymptomCount(resultHashMap: HashMap<String, Any>): Int {
        if (resultHashMap.containsKey(Screening.PregnancySymptoms)) {
            return CommonUtils.calculatePregnancySymptomCount(resultHashMap[Screening.PregnancySymptoms] as java.util.ArrayList<Map<*, *>>)
        }
        return 0
    }

    private fun showHeightWeight(height: Double?, weight: Double?) {
        formGenerator.getViewByTag(Screening.Height)?.let { editText ->
            if (editText is AppCompatEditText) {
                val formattedText = CommonUtils.getDecimalFormatted(height)
                editText.setText(formattedText)
            }
        }
        formGenerator.getViewByTag(Screening.Weight)?.let { editText ->
            if (editText is AppCompatEditText) {
                val formattedText = CommonUtils.getDecimalFormatted(weight)
                editText.setText(formattedText)
            }
        }
    }

    companion object {
        const val TAG: String = "AssessmentNCDFragment"
        fun newInstance(): AssessmentNCDFragment {
            return AssessmentNCDFragment()
        }
    }

    override fun loadLocalCache(id: String, localDataCache: Any, selectedParent: Long?) {
        if (localDataCache is String) {
            when (localDataCache) {
                Screening.PHQ4, AssessmentDefinedParams.PHQ9, AssessmentDefinedParams.GAD7 -> {
                    viewModel.fetchMentalHealthQuestions(localDataCache)
                }

                AssessmentDefinedParams.Fetch_MH_Questions -> {
                    formGenerator.fetchMHQuestions(
                        id,
                        viewModel.mentalHealthQuestions.value?.data?.get(id)
                    )
                }
            }
        }
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        serverViewModel: FormLayout,
        resultMap: Any?
    ) {
        CheckBoxDialog.newInstance(id, resultMap) { map ->
            formGenerator.validateCheckboxDialogue(id, serverViewModel, map)
        }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?
    ) {
        informationList?.let { informationList ->
            GeneralInfoDialog.newInstance(
                title,
                description,
                informationList
            ).show(childFragmentManager, GeneralInfoDialog.TAG)
        }
    }

    override fun onFormSubmit(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {
    }

    override fun onRenderingComplete() {
        patientDetailViewModel.patientDetailsLiveData.value?.data.let {
            autoPopulateDetails(data = it)
        }
    }

    override fun onUpdateInstruction(id: String, selectedId: Any?) {
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(serverData: FormLayout?) {

    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout?>?,
        resultHashMap: HashMap<String, Any>
    ) {
        /*
       Never used
        */
    }


    private fun proceedFormSubmission(v: View) {
        formGenerator.formSubmitAction(v)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSubmit.id -> {
                val unitGenericType = SecuredPreference.getUnitMeasurementType()

                if (binding.symptomCard.root.isVisible()) {
                    val assessment = formGenerator.formSubmitAction(v)
                    val compliance = validateCompliance()
                    val symptoms = validateSymptom()

                    if (symptoms && compliance && assessment)
                        formGenerator.let {
                            proceedToSaveAssessment(
                                it.getResultMap(),
                                it.getServerData(),
                                unitGenericType
                            )
                        }
                } else {
                    formGenerator.let {
                        if (it.formSubmitAction(v)) {
                            saveAssessmentBaseValues(
                                it.getResultMap(),
                                it.getServerData(),
                                unitGenericType
                            )
                        }
                    }
                }
            }
        }
    }

    fun getCurrentAnsweredStatus(): Boolean {
        return (viewModel.selectedSymptoms.value?.size ?: 0) > 0 ||
                !viewModel.complianceMap.isNullOrEmpty() ||
                formGenerator.getResultMap().isNotEmpty()
    }
}