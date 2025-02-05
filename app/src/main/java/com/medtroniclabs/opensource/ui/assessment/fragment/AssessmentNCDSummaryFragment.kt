package com.medtroniclabs.opensource.ui.assessment.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.fragment.app.activityViewModels
import com.google.gson.internal.LinkedTreeMap
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.takeIfNotNull
import com.medtroniclabs.opensource.appextensions.triggerOneTimeWorker
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.databinding.CardLayoutBinding
import com.medtroniclabs.opensource.databinding.FragmentAssessmentNCDSummaryBinding
import com.medtroniclabs.opensource.databinding.SummaryLayoutBinding
import com.medtroniclabs.opensource.formgeneration.FormSummaryReporter
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.mappingkey.Screening.Avg_Blood_pressure
import com.medtroniclabs.opensource.mappingkey.Screening.Avg_Diastolic
import com.medtroniclabs.opensource.mappingkey.Screening.Avg_Systolic
import com.medtroniclabs.opensource.mappingkey.Screening.bp_log
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDFormViewModel
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.assessment.MedicalComplianceResponse
import com.medtroniclabs.opensource.ui.assessment.SymptomResponse
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class AssessmentNCDSummaryFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentAssessmentNCDSummaryBinding
    private val viewModel: AssessmentViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private lateinit var formSummaryReporter: FormSummaryReporter
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssessmentNCDSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    private fun initView() {
        binding.btnDone.safeClickListener(this)
        showPatientInfoCard()
        formSummaryReporter = FormSummaryReporter(requireContext(), binding.llFamilyRoot)
    }

    private fun showPatientInfoCard() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { screeningDetailsModel ->
            binding.llPatientInfo.visibility = View.VISIBLE

            binding.patientName.tvKey.text = getString(R.string.name)
            screeningDetailsModel?.name?.let { name ->
                binding.patientName.tvValue.text = name
            }

            binding.nationalId.tvKey.text = getString(R.string.national_id)
            screeningDetailsModel.identityValue?.let { identityValue ->
                binding.nationalId.tvValue.text = identityValue
            }

            binding.gender.tvKey.text = getString(R.string.gender)
            screeningDetailsModel?.gender?.let {
                binding.gender.tvValue.text = it.replaceFirstChar(Char::titlecase)
            }

            binding.dobAge.tvKey.text = getString(R.string.age)
            screeningDetailsModel?.age?.let { age ->
                binding.dobAge.tvValue.text = CommonUtils.getDecimalFormatted(age)
            }
            binding.programId.root.visible()
            binding.programId.tvKey.text = getString(R.string.patient_id)
            screeningDetailsModel.programId.let { programId ->
                binding.programId.tvValue.text =
                    programId.takeIfNotNull(getString(R.string.hyphen_symbol))
            }
        }
    }

    private fun attachObserver() {
        ncdFormViewModel.ncdFormResponse.value?.data?.let { form ->
            viewModel.assessmentSaveResponse.value?.data?.let {
                StringConverter.convertStringToMap(it.first.assessmentDetails)?.let { map ->
                    formSummaryReporter.populateAssessmentSummary(
                        form,
                        map,
                        false
                    )
                    calculateOtherMetrics(form, map)
                }

                it.second?.let { onlineResponseMap ->
                    if (onlineResponseMap.containsKey(DefinedParams.RiskLevel) && onlineResponseMap.containsKey(
                            DefinedParams.RiskMessage
                        )
                    )
                        updateRedRiskCodes(
                            onlineResponseMap[DefinedParams.RiskColorCode]?.toString(),
                            onlineResponseMap[DefinedParams.RiskMessage]?.toString()
                        )

                    if (onlineResponseMap.containsKey(DefinedParams.ProvisionalTreatmentPlan)) {
                        onlineResponseMap[DefinedParams.ProvisionalTreatmentPlan]?.let { treatmentPlanMap ->
                            if (treatmentPlanMap is Map<*, *> && treatmentPlanMap.containsKey(DefinedParams.TreatmentPlan)) {
                                treatmentPlanMap[DefinedParams.TreatmentPlan]?.let { list ->
                                    if (list is ArrayList<*>)
                                        addCardView(list)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addCardView(treatmentPlanMap: ArrayList<*>) {
        val cardBinding = CardLayoutBinding.inflate(layoutInflater)
        cardBinding.apply {
            cardTitle.text = getString(R.string.treatment_plan)
            viewCardBG.setBackgroundColor(requireContext().getColor(R.color.cobalt_blue))
            cardTitle.setTextColor(requireContext().getColor(R.color.white))
        }

        cardBinding.llFamilyRoot.let { layout ->
            treatmentPlanMap.forEach {
                if (it is Map<*, *>) {
                    layout.addView(
                        inflateChildView(
                            it[DefinedParams.label].toString(),
                            it[DefinedParams.Value].toString()
                        )
                    )
                }
            }
        }

        if (cardBinding.llFamilyRoot.childCount > 0)
            binding.llFamilyRoot.addView(cardBinding.root)
    }

    private fun inflateChildView(labelKey: String, value: String): View {
        val summaryBinding = SummaryLayoutBinding.inflate(layoutInflater)
        summaryBinding.apply {
            tvKey.text = labelKey
            tvRowSeparator.text = requireContext().getString(R.string.separator_colon)
            tvValue.text = value
        }
        return summaryBinding.root
    }

    private fun calculateOtherMetrics(
        serverData: List<FormLayout>,
        map: Map<String, Any>
    ) {
        showBloodPressure(map)
        showBloodGlucoseValue(serverData, map)
        showBMIValue(serverData, map)
        showFurtherAssessment(map)
        showCVDRiskValue(map)
        showSymptoms(map)
        showMedicalCompliance(map)
        showPHQ4Score(serverData, map)
        showPHQ9Score(serverData, map)
        showGAD7Score(serverData, map)
        showPregnancyANC(map)
    }

    private fun showBloodPressure(map: Map<String, Any>) {
        val subMap = map[bp_log] as? Map<String, Any> ?: return
        val systolic = subMap[Avg_Systolic]
        val diastolic = subMap[Avg_Diastolic]

        if (subMap.containsKey(Avg_Blood_pressure) && systolic != null && diastolic != null) {
            showBindingValue(
                getString(R.string.average_bp_text),
                getString(
                    R.string.average_mmhg_string,
                    CommonUtils.getDecimalFormatted(systolic),
                    CommonUtils.getDecimalFormatted(diastolic)
                )
            )
        }
    }

    private fun showPregnancyANC(map: Map<String, Any>) {
        if (map.containsKey(AssessmentDefinedParams.pregnancyAnc)) {
            patientDetailViewModel.patientDetailsLiveData.value?.data?.let { screeningDetailsModel ->
                screeningDetailsModel.isPregnant?.let { isPregnant ->
                    showBindingValue(
                        getString(R.string.pregnancy_status),
                        isPregnantOrNot(isPregnant)
                    )
                }
                screeningDetailsModel.pregnancyDetails?.lastMenstrualPeriod?.let { lastMenstrualPeriod ->
                    showBindingValue(
                        getString(R.string.gestational_period),
                        gestationalWeeks(lastMenstrualPeriod)
                    )
                }
            }
            showBindingValue(
                getString(R.string.pregnancy_signs),
                getPregnancySymptoms(map)
            )
        }
    }

    private fun getPregnancySymptoms(map: Map<String, Any>): String {
        if (map.containsKey(AssessmentDefinedParams.pregnancyAnc)) {
            val subMap = map[AssessmentDefinedParams.pregnancyAnc] as Map<String, Any>
            if (subMap.containsKey(AssessmentDefinedParams.PregnancySymptoms)) {
                val list = subMap[AssessmentDefinedParams.PregnancySymptoms] as ArrayList<*>
                if (subMap.containsKey(AssessmentDefinedParams.pregnancyOtherSymptoms)) {
                    val otherSymptom = subMap[AssessmentDefinedParams.pregnancyOtherSymptoms] as String?
                    return getDialogValue(list, otherSymptom)
                }
                return getDialogValue(list)
            }
        }
        return getString(R.string.separator_hyphen)
    }

    private fun getDialogValue(value: Any?, otherSymptoms: String? = null): String {
        val result = StringBuilder()
        if (value is ArrayList<*>) {
            value.forEach { map ->
                CommonUtils.getListActual(map)?.let {
                    result.append(it)
                    result.append(getString(R.string.comma_symbol))
                }
            }
        }
        if (result.isNotEmpty()) {
            otherSymptoms?.let {
                return result.delete(result.length - 2, result.length).append(getString(R.string.separator_hyphen_space)).append(it)
                    .toString()
            }
            return result.delete(result.length - 2, result.length).toString()
        }
        return getString(R.string.empty_space)
    }

    private fun gestationalWeeks(date: String?): String {
        val lastMenstrualDate = date?.let { DateUtils.getLastMenstrualDate(it) }
        val gestationWeek = lastMenstrualDate?.let { DateUtils.calculateGestationalAge(it).first.toInt() }
        gestationWeek?.let { weeks ->
            return if (weeks < AssessmentDefinedParams.PregnancyANCMaxValue) {
                getString(R.string.gestational_weeks, weeks)
            } else {
                getString(R.string.gestational_weeks, AssessmentDefinedParams.PregnancyANCMaxValue)
            }
        } ?: kotlin.run {
            return getString(R.string.hyphen_symbol)
        }
    }

    private fun isPregnantOrNot(isPregnant: Boolean?): String {
        return if (isPregnant == true) {
            getString(R.string.positive)
        } else {
            getString(R.string.negative)
        }
    }

    private fun showPHQ9Score(serverData: List<FormLayout>, map: Map<String, Any>) {
        if (map.containsKey(AssessmentDefinedParams.PHQ9.lowercase())) {
            val subMap = map[AssessmentDefinedParams.PHQ9.lowercase()] as Map<String, Any>
            if (subMap.isNotEmpty()) {
                if (subMap.containsKey(Screening.PHQ4_Score)) {
                    val phq9Score = subMap[Screening.PHQ4_Score]
                    if (phq9Score is Number) {
                        showBindingValue(
                            getString(R.string.phq9_score),
                            StringConverter.getPHQ9ReadableName(
                                score = phq9Score.toInt(),
                                requireContext()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showGAD7Score(serverData: List<FormLayout>, map: Map<String, Any>) {
        if (map.containsKey(AssessmentDefinedParams.GAD7.lowercase())) {
            val subMap = map[AssessmentDefinedParams.GAD7.lowercase()] as Map<String, Any>
            if (subMap.isNotEmpty()) {
                if (subMap.containsKey(Screening.PHQ4_Score)) {
                    val gad7Score = subMap[Screening.PHQ4_Score]
                    if (gad7Score is Number) {
                        showBindingValue(
                            getString(R.string.gad7_score),
                            StringConverter.getGAD7ReadableName(
                                score = gad7Score.toInt(),
                                requireContext()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showMedicalCompliance(map: Map<String, Any>) {
        if (map.containsKey(AssessmentDefinedParams.compliance)) {
            val linkedTreeMapList: List<ArrayList<LinkedTreeMap<String, String>>> = map.filter {
                it.key.equals(AssessmentDefinedParams.compliance, true)
            }.map { it.value as ArrayList<LinkedTreeMap<String, String>> }
            val symptomResponseList: ArrayList<MedicalComplianceResponse> = arrayListOf()
            linkedTreeMapList.flatten().forEach { linkedTreeMap ->
                val name = linkedTreeMap[DefinedParams.NAME] as? String ?: getString(R.string.empty_space)
                val culture = linkedTreeMap[DefinedParams.cultureValue] as? String
                val otherCompliance = linkedTreeMap[AssessmentDefinedParams.other_compliance] as? String

                val complianceResponse = MedicalComplianceResponse(
                    name = name,
                    otherCompliance = otherCompliance,
                    cultureValue = culture
                )
                symptomResponseList.add(complianceResponse)
            }
            showBindingValue(
                getString(R.string.medical_adherence),
                getSelectedMedicalComplianceText(symptomResponseList)
            )
        }
    }

    private fun showSymptoms(map: Map<String, Any>) {
        val linkedTreeMapList: List<ArrayList<LinkedTreeMap<String, String>>> = map.filter {
            it.key.equals(AssessmentDefinedParams.symptomsDTO, true)
        }.map { it.value as ArrayList<LinkedTreeMap<String, String>> }
        val symptomResponseList: ArrayList<SymptomResponse> = arrayListOf()

        linkedTreeMapList.flatten().forEach { linkedTreeMap ->
            val name = linkedTreeMap[DefinedParams.NAME] as? String ?: getString(R.string.empty_space)
            val culture = linkedTreeMap[DefinedParams.cultureValue] as? String
            val type = linkedTreeMap[Screening.type] as? String
            val otherSymptom = linkedTreeMap[AssessmentDefinedParams.other_symptom] as? String

            val symptomResponse = SymptomResponse(
                name = name,
                type = type,
                otherSymptom = otherSymptom,
                cultureValue = culture
            )
            symptomResponseList.add(symptomResponse)
        }
        showBindingValue(
            getString(R.string.symptoms),
            getSelectedSymptomsText(symptomResponseList)
        )
    }

    private fun getSelectedMedicalComplianceText(list: ArrayList<MedicalComplianceResponse>): String {
        val resultString = StringBuilder()
        list.forEachIndexed { index, medicalComplianceResponse ->
            resultString.append(
                medicalComplianceResponse.cultureValue ?: medicalComplianceResponse.name
            )
            if (list.size > 1 && index == 0) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
            }
            if (!medicalComplianceResponse.otherCompliance.isNullOrBlank()) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
                resultString.append(
                    medicalComplianceResponse.otherCompliance.trim().capitalizeFirstChar()
                )
            }
            if (index > 0 && index != list.size - 1) {
                resultString.append(getString(R.string.comma_symbol))
            }
            resultString.append(getString(R.string.empty_space))
        }
        return resultString.toString()
    }

    private fun getSelectedSymptomsText(list: ArrayList<SymptomResponse>): String {
        val resultString = StringBuilder()
        list.forEachIndexed { index, symptomResponse ->
            resultString.append(symptomResponse.cultureValue ?: symptomResponse.name)
            if (!symptomResponse.otherSymptom.isNullOrBlank()) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
                resultString.append(symptomResponse.otherSymptom.trim().capitalizeFirstChar())
            } else if (symptomResponse.name.startsWith(
                    AssessmentDefinedParams.NoSymptoms,
                    true
                ) && !symptomResponse.type.isNullOrBlank()
            ) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
                resultString.append(symptomResponse.type.trim().capitalizeFirstChar())
            }
            if (index != list.size - 1) {
                resultString.append(getString(R.string.comma_symbol))
            }
            resultString.append(getString(R.string.empty_space))
        }
        return resultString.toString()
    }

    private fun showCVDRiskValue(map: Map<String, Any>) {
        if (map.containsKey(Screening.CVD_Risk_Score_Display)) {
            val cvdRiskScoreDisplay = map[Screening.CVD_Risk_Score_Display]
            (map[Screening.CVD_Risk_Score] as? Long)?.let { cvdRiskScore ->
                if (cvdRiskScoreDisplay is String) {
                    showBindingValue(
                        getString(R.string.cvd_risk_level),
                        "$cvdRiskScoreDisplay",
                        CommonUtils.cvdRiskColorCode(
                            cvdRiskScore,
                            context = requireContext()
                        )
                    )
                }
            }
        }
    }

    private fun checkAssessmentCondition(map: Map<String, Any>): Boolean {
        var visibility = false

        if (!visibility && map.containsKey(Screening.ReferAssessment)) {
            visibility = map[Screening.ReferAssessment] as? Boolean ?: false
        }
        return visibility
    }

    private fun showFurtherAssessment(map: Map<String, Any>) {
        val assessmentCondition = checkAssessmentCondition(map)
        binding.riskResultLayout.isEnabled = assessmentCondition
        if (assessmentCondition) {
            setReferForFurtherAssessment()
        } else {
            binding.riskResultLayout.text = getString(R.string.no_assessment_required)
        }
//        binding.riskResultLayout.setVisible(assessmentCondition)
    }

    private fun setReferForFurtherAssessment() {
        val text = getString(R.string.referred_for_nfurther_assessment)
        binding.riskResultLayout.text = text
    }

    private fun showBMIValue(serverData: List<FormLayout>, map: Map<String, Any>) {
        val subMap = map[Screening.BioMetrics] as Map<String, Any>
        if (subMap.containsKey(Screening.BMI)) {
            val bmiValue = subMap[Screening.BMI]

            if (bmiValue is Double) {
                val bmiInfo = CommonUtils.getBMIInformation(requireContext(), bmiValue)
                val bmiFormattedValue = CommonUtils.getDecimalFormatted(bmiValue)
                if (bmiInfo == null) {
                    showBindingValue(
                        getString(R.string.bmi),
                        bmiFormattedValue
                    )
                } else {
                    val spannableStringBuilder =
                        SpannableStringBuilder().append(bmiFormattedValue)
                            .color(ContextCompat.getColor(requireContext(), bmiInfo.second)) {
                                append(" (${bmiInfo.first})")
                            }
                    showBindingValue(
                        getString(R.string.bmi),
                        spannableStringBuilder
                    )
                }
            }
        }
    }

    private fun showBloodGlucoseValue(serverData: List<FormLayout>, map: Map<String, Any>) {

        FormResultComposer.findGroupIdForNCD(serverData, Screening.Glucose_Type)?.let {
            (map[it] as? Map<*, *>?)?.let { subMap ->
                if (subMap.containsKey(Screening.Glucose_Type)) {
                    val type = subMap[Screening.Glucose_Type] as String
                    val glucoseValue = (subMap[Screening.Glucose_Value] as Double)
                    var unitType: String? = null
                    if (subMap.containsKey(Screening.BloodGlucoseID + Screening.unitMeasurement_KEY)) {
                        val unitTypeKey =
                            subMap[Screening.BloodGlucoseID + Screening.unitMeasurement_KEY]
                        if (unitTypeKey is String) {
                            unitType = unitTypeKey
                        }
                    }
                    if (type.lowercase().equals(Screening.rbs, true)) {
                        showBindingValue(
                            getString(R.string.blood_glucose_rbs),
                            "${CommonUtils.getDecimalFormatted(glucoseValue)} ${
                                CommonUtils.getGlucoseUnit(
                                    unitType,
                                    true
                                )
                            }"
                        )
                    } else if (type.lowercase().equals(Screening.fbs, true)) {
                        showBindingValue(
                            getString(R.string.blood_glucose_fbs),
                            "${CommonUtils.getDecimalFormatted(glucoseValue)} ${
                                CommonUtils.getGlucoseUnit(
                                    unitType,
                                    true
                                )
                            }"
                        )
                    }
                }
            }
        }
    }

    private fun showBindingValue(
        title: String,
        value: SpannableStringBuilder,
        valueTextColor: Int? = null
    ) {
        val summaryBinding = SummaryLayoutBinding.inflate(layoutInflater)
        summaryBinding.tvKey.text = title
        summaryBinding.tvValue.text = value
        valueTextColor?.let {
            summaryBinding.tvValue.setTextColor(it)
        }
        binding.root.findViewWithTag<LinearLayout>(formSummaryReporter.getFormResultView())
            ?.addView(summaryBinding.root)
    }

    private fun showPHQ4Score(serverData: List<FormLayout>, map: Map<String, Any>) {
        if (map.containsKey(Screening.PHQ4.lowercase())) {
            FormResultComposer.findGroupIdForNCD(serverData, Screening.PHQ4_Score)?.let {
                val subMap = map[it] as Map<String, Any>
                if (subMap.containsKey(Screening.PHQ4_Score)) {
                    val phq4Score = subMap[Screening.PHQ4_Score]
                    if (phq4Score is Number) {
                        showBindingValue(
                            getString(R.string.phq4_score),
                            StringConverter.getPHQ4ReadableName(
                                score = phq4Score.toInt(),
                                requireContext()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showBindingValue(title: String, value: String, valueTextColor: Int? = null) {
        val summaryBinding = SummaryLayoutBinding.inflate(layoutInflater)
        summaryBinding.tvKey.text = title
        summaryBinding.tvValue.text = value
        valueTextColor?.let {
            summaryBinding.tvValue.setTextColor(it)
        }
        binding.root.findViewWithTag<LinearLayout>(formSummaryReporter.getFormResultView())
            ?.addView(summaryBinding.root)
    }

    private fun updateRedRiskCodes(riskCode: String?, riskMessage: String?) {
        if (riskMessage.isNullOrBlank())
            binding.clRedRisk.visibility = View.GONE
        else {
            binding.clRedRisk.visibility = View.VISIBLE
            val color = Color.parseColor(riskCode ?: "#E4CC76")
            binding.clRedRisk.backgroundTintList =
                ColorStateList.valueOf(color)
            binding.tvRedRiskStatus.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            binding.tvRedRiskStatus.gravity = Gravity.CENTER
            binding.ivRedRisk.visibility = View.GONE
            binding.tvRedRiskStatus.text = riskMessage ?: ""
        }
    }

    private fun updateRedRiskDetails(riskLevel: String?, riskMessage: String?) {
        binding.clRedRisk.visibility =
            if (riskMessage.isNullOrBlank() || riskLevel.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvRedRiskStatus.text = riskMessage ?: ""
        riskLevel?.let {
            when (it) {
                DefinedParams.RedRiskLow -> {
                    binding.ivRedRisk.setImageResource(R.drawable.ic_red_risk_green)
                    binding.clRedRisk.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_red_risk_green)
                }
                DefinedParams.RedRiskModerate -> {
                    binding.ivRedRisk.setImageResource(R.drawable.ic_red_risk_orange)
                    binding.clRedRisk.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_red_risk_orange)
                }
                DefinedParams.RedRiskHigh -> {
                    binding.ivRedRisk.setImageResource(R.drawable.ic_red_risk_red)
                    binding.clRedRisk.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_red_risk)
                }
            }
        }
    }

    companion object {
        const val TAG = "AssessmentNCDSummaryFragment"
        fun newInstance(): AssessmentNCDSummaryFragment {
            return AssessmentNCDSummaryFragment()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnDone.id -> {
                requireContext().triggerOneTimeWorker()
                (activity as? BaseActivity)?.startActivityWithoutSplashScreen()
            }
        }
    }
}