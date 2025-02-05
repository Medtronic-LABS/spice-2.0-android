package com.medtroniclabs.opensource.ncd.screening.fragment

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.text.color
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.triggerOneTimeWorker
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.getDialogValue
import com.medtroniclabs.opensource.common.CommonUtils.getGlucoseUnit
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.common.StringConverter.getPHQ4ReadableName
import com.medtroniclabs.opensource.databinding.FragmentScreeningSummaryBinding
import com.medtroniclabs.opensource.databinding.SummaryLayoutBinding
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.formgeneration.FormSummaryReporter
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.mappingkey.Screening.ReferAssessment
import com.medtroniclabs.opensource.mappingkey.Screening.SuicidalIdeation
import com.medtroniclabs.opensource.ncd.data.HivSummaryModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDFormViewModel
import com.medtroniclabs.opensource.ncd.screening.utils.ReferredReason
import com.medtroniclabs.opensource.ncd.screening.viewmodel.GeneralDetailsViewModel
import com.medtroniclabs.opensource.ncd.screening.viewmodel.ScreeningFormBuilderViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreeningSummaryFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentScreeningSummaryBinding
    private lateinit var formSummaryReporter: FormSummaryReporter
    private val viewModel: ScreeningFormBuilderViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private val generalDetailsViewModel: GeneralDetailsViewModel by activityViewModels()
    private val adapter by lazy { CustomSpinnerAdapter(requireContext()) }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentScreeningSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {

        const val TAG = "ScreeningSummaryFragment"

        fun newInstance(): ScreeningSummaryFragment {
            return ScreeningSummaryFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        validateToEnableNext()
        setListeners()
    }

    private fun attachObserver() {
        ncdFormViewModel.ncdFormResponse.value?.data?.let { form ->
            viewModel.screeningSaveResponse.value?.data?.let {
                StringConverter.convertStringToMap(it.screeningDetails)?.let { map ->
                    formSummaryReporter.populateSummary(
                        form,
                        map,
                        SecuredPreference.getIsTranslationEnabled()
                    )
                    calculateOtherMetrics(form, map)
                }
            }
        }

        generalDetailsViewModel.getSitesLiveData.observe(viewLifecycleOwner) {
            loadSiteDetails(ArrayList(it))
        }

        viewModel.screeningUpdateResponse.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    (activity as? BaseActivity)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as? BaseActivity)?.hideLoading()
                    requireContext().triggerOneTimeWorker()
                    replaceFragmentIfExists<StatsFragment>(
                        R.id.screeningParentLayout,
                        bundle = null,
                        tag = StatsFragment.TAG
                    )
                    viewModel.screeningUpdateResponse.postError()
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity)?.hideLoading()
                }
            }
        }
    }

    private fun setListeners() {
        binding.etSiteChange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
            ) {
                adapter.getData(pos)?.let {
                    processSiteSelection(it)
                    validateToEnableNext()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun validateToEnableNext() {
        binding.btnNext.isEnabled =
            generalDetailsViewModel.siteDetail.siteId != -1L
    }

    private fun loadSiteDetails(data: ArrayList<HealthFacilityEntity>?) {
        val list = arrayListOf<Map<String, Any>>(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultSelectID
            )
        )
        var defaultPosition = 0
        data?.mapIndexed { index, site ->
            hashMapOf(
                DefinedParams.ID to site.id,
                DefinedParams.NAME to site.name,
                DefinedParams.TenantId to site.tenantId,
                DefinedParams.FhirId to (site.fhirId ?: 0)
            ).also {
                if (generalDetailsViewModel.siteDetail.siteId == site.fhirId?.toLongOrNull()) {
                    defaultPosition = index + 1
                }
            }
        }?.let { list.addAll(it) }
        adapter.setData(list)
        binding.etSiteChange.post {
            binding.etSiteChange.setSelection(defaultPosition, false)
        }
        binding.etSiteChange.adapter = adapter
    }

    private fun calculateOtherMetrics(
        serverData: List<FormLayout>,
        map: Map<String, Any>
    ) {
        showBloodGlucoseValue(serverData, map)
        showBMIValue(serverData, map)
        showDiabetesSymptoms(serverData, map)
        showPHQ4Score(serverData, map)
        showFurtherAssessment(map)
        showMentalHealthRelatedMetrics(map)
        showCVDRiskValue(map)
        showPregnancyOrNot(serverData, map)
        showPregnancySymptomsSignsChanges(serverData, map)
        showHIVRelatedMetrics(map, serverData)
    }

    private fun showHIVRelatedMetrics(resultMap: Map<String, Any>, serverData: List<FormLayout>) {
        val hivParams = serverData.filter { data ->
            (data.ageCondition?.isNotEmpty() == true && data.workflowType?.contains(
                ReferredReason.HIV
            ) == true) && (data.viewType == ViewType.VIEW_TYPE_FORM_CARD_FAMILY)
        }.sortedBy { it.familyOrder }.map { it.id }
        hivParams.forEach { item ->
            if (resultMap.containsKey(item) && resultMap[item] is LinkedTreeMap<*, *>) {
                (resultMap[item] as? LinkedTreeMap<*, *>)?.let { map ->
                    if (map.isNotEmpty()) {
                        val keyLabel = serverData.filter { it.id == item }[0].title
                        val summaryValue = ArrayList<HivSummaryModel>()
                        for ((key, value) in map) {
                            value?.let { result ->
                                if (result is String && result.equals(DefinedParams.yes, true) && key is String) {
                                    serverData.filter { it.id == key }[0].let { listForm ->
                                        listForm.orderId?.let { orderIndex ->
                                            summaryValue.add(
                                                HivSummaryModel(
                                                    orderId = orderIndex,
                                                    summaryValue = listForm.titleSummary ?: "-"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        showBindingValue(
                            keyLabel,
                            if (summaryValue.size > 0) CommonUtils.convertListToString(
                                ArrayList(summaryValue
                                    .sortedBy { it.orderId }
                                    .map { it.summaryValue })
                            ) else getString(
                                R.string.none
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showDiabetesSymptoms(serverData: List<FormLayout>, map: Map<String, Any>) {
        val groupId = FormResultComposer.findGroupIdForNCD(serverData, Screening.diabetes) ?: return
        val subMap = map[groupId] as? Map<*, *> ?: return

        val diabetesData = (subMap[Screening.diabetes] as? ArrayList<Map<String, String>>)?.mapNotNull { it[DefinedParams.NAME] } ?: return
        if (diabetesData.isEmpty()) return

        val formLayout = serverData.firstOrNull { it.id.equals(Screening.diabetes, true) } ?: return
        showBindingValue(
            translateTitle(formLayout.titleCulture, formLayout.title),
            getDialogValue(
                subMap[Screening.diabetes],
                subMap[Screening.diabetesOtherSymptoms] as? String?
            )
        )
    }

    private fun translateTitle(titleCulture: String?, title: String): String {
        return if (SecuredPreference.getIsTranslationEnabled()) titleCulture ?: title else title
    }

    private fun showPregnancySymptomsSignsChanges(
        serverData: List<FormLayout>,
        map: Map<String, Any>
    ) {
        FormResultComposer.findGroupIdForNCD(serverData, Screening.PregnancySymptoms)?.let {
            if (map[it] is Map<*, *>) {
                val subMap = map[it] as? Map<*, *>
                subMap?.let {
                    if (subMap.containsKey(Screening.PregnancySymptoms)) {
                        showBindingValue(
                            getString(R.string.pregnancy_signs),
                            getDialogValue(
                                subMap[Screening.PregnancySymptoms],
                                subMap[Screening.PregnancyOtherSymptoms] as? String?
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showPregnancyOrNot(serverData: List<FormLayout>, map: Map<String, Any>) {
        FormResultComposer.findGroupIdForNCD(serverData, Screening.isPregnant)?.let {
            if (map[it] is Map<*, *>) {
                val subMap = map[it] as? Map<*, *>
                subMap?.let {
                    if (subMap.containsKey(Screening.isPregnant)) {
                        val isPregnant = subMap[Screening.isPregnant] as Boolean
                        showBindingValue(
                            getString(R.string.pregnancy_status),
                            getValueOfType(isPregnant)
                        )
                        if (isPregnant) {
                            showLastMensturalDate(map)
                            showGestationalAge(map)
                        }
                    }
                }
            }
        }
    }

    private fun showLastMensturalDate(map: Map<String, Any>) {
        (map[Screening.pregnancyAnc] as? Map<*, *>)?.let { pregnancyAnc ->
            if (pregnancyAnc.containsKey(Screening.lastMenstrualPeriod)) {
                val lmb = (pregnancyAnc[Screening.lastMenstrualPeriod] as? String)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { date ->
                        DateUtils.convertDateFormat(
                            date, DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DateUtils.DATE_ddMMyyyy
                        )
                    } ?: getString(R.string.hyphen_symbol)
                showBindingValue(getString(R.string.last_menstrual_period), lmb)
            }
        }
    }

    private fun showGestationalAge(map: Map<String, Any>) {
        (map[Screening.pregnancyAnc] as? Map<*, *>)?.let { pregnancyAnc ->
            if (pregnancyAnc.containsKey(Screening.GestationalAge)) {
                val weeks = (pregnancyAnc[Screening.GestationalAge] as? Number?)?.toInt()
                val value = weeks?.let {
                    if (it > 1) "$it ${getString(R.string.weeks)}" else "$it ${getString(R.string.week)}"
                } ?: getString(R.string.hyphen_symbol)
                showBindingValue(getString(R.string.gestational_period), value)
            }
        }
    }

    private fun getValueOfType(pregnant: Boolean): String {
        return if (pregnant) {
            getString(R.string.positive)
        } else {
            getString(R.string.negative)
        }
    }
    private fun showMentalHealthRelatedMetrics(
        map: Map<String, Any>
    ) {

        if (map.containsKey(Screening.CAGEAID)) {
            val cageAid = map[Screening.CAGEAID]
            var cageAidColor: Int? = null
            if (cageAid is Double && cageAid >= 2) {
                cageAidColor = getColor(requireContext(), R.color.medium_high_risk_color)
            }
            showBindingValue(
                getString(R.string.cage_aid),
                CommonUtils.getDecimalFormatted(cageAid),
                cageAidColor
            )
        }

        if (map.containsKey(SuicidalIdeation)) {
            showBindingValue(
                getString(R.string.suicidal_ideation),
                map[SuicidalIdeation].toString(),
                getColor(requireContext(), R.color.medium_high_risk_color)
            )
        }
    }

    private fun showFurtherAssessment(map: Map<String, Any>) {
        val assessmentCondition = checkAssessmentCondition(map)
        binding.riskResultLayout.isEnabled = assessmentCondition
        if (assessmentCondition) {
            setReferForFurtherAssessment()
        } else {
            binding.riskResultLayout.text = getString(R.string.no_assessment_required)
        }
        binding.riskResultLayout.visibility = View.VISIBLE
    }

    private fun setReferForFurtherAssessment() {
        val text = getString(R.string.referred_for_nfurther_assessment)
        binding.riskResultLayout.text = text
    }

    private fun checkAssessmentCondition(map: Map<String, Any>): Boolean {
        var visibility = false

        if (!visibility && map.containsKey(ReferAssessment)) {
            visibility = map[ReferAssessment] as? Boolean ?: false
        }
        return visibility
    }

    private fun showPHQ4Score(serverData: List<FormLayout>, map: Map<String, Any>) {
        FormResultComposer.findGroupIdForNCD(serverData, Screening.MentalHealthDetails)?.let {
            val subMap = map[it] as Map<String, Any>
            if (subMap.containsKey(Screening.PHQ4_Score)) {
                val phq4Score = subMap[Screening.PHQ4_Score]
                if (phq4Score is Long) {
                    showBindingValue(
                        getString(R.string.phq4_score),
                        getPHQ4ReadableName(score = phq4Score.toInt(), requireContext())
                    )
                }
            }
        }
    }

    private fun showBloodGlucoseValue(serverData: List<FormLayout>, map: Map<String, Any>) {

        FormResultComposer.findGroupIdForNCD(serverData, Screening.Glucose_Type)?.let {
            val subMap = map[it] as Map<String, Any>
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
                if (type.lowercase() == Screening.rbs) {
                    showBindingValue(
                        getString(R.string.blood_glucose_rbs),
                        "${CommonUtils.getDecimalFormatted(glucoseValue)} ${
                            getGlucoseUnit(
                                unitType,
                                true
                            )
                        }"
                    )
                } else if (type.lowercase() == Screening.fbs) {
                    showBindingValue(
                        getString(R.string.blood_glucose_fbs),
                        "${CommonUtils.getDecimalFormatted(glucoseValue)} ${
                            getGlucoseUnit(
                                unitType,
                                true
                            )
                        }"
                    )
                }
            }
        }
    }

    private fun showBMIValue(serverData: List<FormLayout>, map: Map<String, Any>) {
        FormResultComposer.findGroupIdForNCD(serverData, Screening.BMI)?.let {
            val subMap = map[it] as Map<String, Any>
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
                                .color(getColor(requireContext(), bmiInfo.second)) {
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

    private fun initView() {
        binding.labelSiteChange.markMandatory()
        binding.etSiteChange.tag = null
        binding.etSiteChange.visibility = View.VISIBLE
        binding.llSiteChangeHolder.visibility = View.VISIBLE
        formSummaryReporter = FormSummaryReporter(requireContext(), binding.llFamilyRoot)
        generalDetailsViewModel.getSites(true)
        binding.btnNext.safeClickListener(this)
        (activity as? BaseActivity)?.hiddenBackButton()
    }

    private fun processSiteSelection(map: Map<String, Any>) {
        generalDetailsViewModel.siteDetail.apply {
            siteName = map[DefinedParams.NAME] as? String ?: ""
            siteId = map[DefinedParams.FhirId]?.toString()?.toLongOrNull() ?: -1L
            tenantId = map[DefinedParams.TenantId]?.toString()?.toLongOrNull() ?: -1L
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnNext.id -> {
                val siteDetail = Gson().toJson(generalDetailsViewModel.siteDetail)
                viewModel.updatePatientScreeningInformation(siteDetail)
            }
        }
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
}