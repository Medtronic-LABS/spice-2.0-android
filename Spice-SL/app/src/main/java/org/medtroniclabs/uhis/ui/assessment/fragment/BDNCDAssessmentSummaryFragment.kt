package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import org.json.JSONArray
import org.json.JSONObject
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.CVD_RISK_SCORE_DISPLAY
import org.medtroniclabs.uhis.common.DefinedParams.DefaultID
import org.medtroniclabs.uhis.databinding.FragmentBdNcdSummaryBinding
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.findValueByKey
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getSpinnerDisplayValue
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_BLOOD_PRESSURE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BMI
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BMI_CATEGORY
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG_DETAILS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CULTURE_VALUE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CVD_RISK
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_UPAZILA
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_UNIT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HBA1CUnit
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MMHG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NAME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NCD_SYMPTOMS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REFERRAL_FACILITY_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ReferredPHUSiteID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.WEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hba1c
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class BDNCDAssessmentSummaryFragment : BaseFragment() {
    private val viewModel: AssessmentViewModel by activityViewModels()
    lateinit var binding: FragmentBdNcdSummaryBinding

    companion object {
        const val TAG = "BDNCDAssessmentSummaryFragment"

        fun newInstance(): BDNCDAssessmentSummaryFragment = BDNCDAssessmentSummaryFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBdNcdSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.TBSummaryAssessement)
    }

    private fun initView() {
        if (viewModel.referralStatus == ReferralStatus.Referred.name) {
            viewModel.otherAssessmentDetails[AssessmentDefinedParams.NextFollowupDate] = DateUtils.convertDateTimeToDate(
                DateUtils.getDateAfterDays(5),
                DateUtils.DATE_ddMMyyyy,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                inUTC = true,
            )
        }

        binding.btnDone.setOnClickListener {
            viewModel.updateOtherAssessmentDetails()
        }
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            val json = JSONObject(it)
            updateStatusBar(json)
            val items = createNCDSummaryData(json)
            createSummaryView(items)
        }
    }

    private fun createSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>?) {
        listSummaryData?.let { summaryData ->
            binding.parentLayout.removeAllViews()

            getStatus(viewModel.referralStatus)?.let {
                bindSummaryView(
                    getString(R.string.patient_status),
                    it,
                )
            }

            summaryData.forEach { item ->
                val displayValue =
                    getSpinnerDisplayValue(
                        item.id.toString(),
                        item.value,
                        isTranslationEnabled,
                        viewModel.formLayoutsLiveData.value
                            ?.data
                            ?.formLayout,
                    ) ?: item.value
                bindSummaryView(if (isTranslationEnabled) item.cultureValue else item.title, displayValue)
            }
        }
    }

    private fun getStatus(referralStatus: String?): String? =
        when (referralStatus) {
            ReferralStatus.Referred.name -> getString(R.string.referred)
            ReferralStatus.OnTreatment.name -> getString(R.string.on_treatment)
            ReferralStatus.Recovered.name -> getString(R.string.recovered)
            else -> {
                null
            }
        }

    private fun updateStatusBar(json: JSONObject) {
        when (viewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                val referralTypeSite = findValueByKey(json, REFERRAL_FACILITY_TYPE) as String
                viewModel.otherAssessmentDetails[REFERRAL_FACILITY_TYPE] = referralTypeSite

                if (CommonUtils.isFoOrPo()) {
                    binding.labelPhuReferred.gone()
                    binding.etPhuChange.gone()
                } else {
                    viewModel.nearestFacilityLiveData.value?.data?.let { siteList ->
                        loadPhuSitesList(siteList)
                    }
                    binding.labelPhuReferred.visible()
                    binding.etPhuChange.visible()
                }
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)

                if (referralTypeSite == FACILITY_TYPE_UPAZILA) {
                    binding.riskResultLayout.text = getString(R.string.referred_for_nfurther_assessment_Upazila)
                } else {
                    binding.riskResultLayout.text = getString(R.string.referred_for_nfurther_assessment_cc)
                }
            }

            else -> {
                binding.labelPhuReferred.gone()
                binding.etPhuChange.gone()
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.green_attention_color)
                binding.riskResultLayout.text = getString(R.string.no_refferral_treatment_required)
            }
        }
    }

    private fun createNCDSummaryData(json: JSONObject): MutableList<AssessmentSummaryModel>? =
        viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.filter { it.isSummary == true }
            ?.mapNotNull { formLayout ->
                val value = getValueFromJson(formLayout.id, json)

                value?.let {
                    AssessmentSummaryModel(
                        title = formLayout.titleSummary ?: formLayout.title,
                        id = formLayout.id,
                        cultureValue = formLayout.titleSummaryCulture ?: formLayout.titleCulture,
                        value = it,
                    )
                }
            }?.toMutableList()

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()

    private fun getValueFromJson(
        id: String,
        jsonObject: JSONObject,
    ): String? {
        return when (id) {
            NCD_SYMPTOMS -> {
                val list = mutableListOf<String>()
                findValueByKey(jsonObject, id)?.let {
                    val jsonArray = it as JSONArray

                    for (i in 0 until jsonArray.length()) {
                        val sign = jsonArray.getJSONObject(i)
                        val name = sign.optString(NAME)
                        val cultureValue = sign.optString(CULTURE_VALUE)
                        if (isTranslationEnabled && cultureValue.isNotBlank()) {
                            list.add(cultureValue)
                        } else {
                            list.add(name)
                        }
                    }

                    return list.joinToString(", ")
                }
            }

            HEIGHT, WEIGHT -> findValueByKey(jsonObject, id)?.toString()

            GLUCOSE -> {
                val glucoseValue = findValueByKey(jsonObject, id)
                val unit = findValueByKey(jsonObject, GLUCOSE_UNIT)
                val type = findValueByKey(jsonObject, GLUCOSE_TYPE)
                if (unit != null && type != null && glucoseValue != null) {
                    "${glucoseValue as Double} ${unit as String} (${type as String})"
                } else {
                    null
                }
            }

            hba1c -> {
                val hba1 = findValueByKey(jsonObject, id)
                val unit = findValueByKey(jsonObject, HBA1CUnit)
                if (unit != null && hba1 != null) {
                    "${hba1 as Double} ${unit as String}"
                } else {
                    null
                }
            }

            BMI -> {
                val bmi = findValueByKey(jsonObject, id)
                val bmiCategory = findValueByKey(jsonObject, BMI_CATEGORY)
                if (bmiCategory != null && bmi != null) {
                    "${bmi as Double} (${bmiCategory as String})"
                } else {
                    null
                }
            }

            BP_LOG_DETAILS -> {
                val bp = findValueByKey(jsonObject, AVG_BLOOD_PRESSURE) as? String
                "$bp $MMHG"
            }

            CVD_RISK -> {
                val cvdRiskLevel = findValueByKey(jsonObject, CVD_RISK_SCORE_DISPLAY)
                if (cvdRiskLevel != null) {
                    cvdRiskLevel as String
                } else {
                    null
                }
            }

            else -> findValueByKey(jsonObject, id)?.toString()
        }
    }

    private fun loadPhuSitesList(healthFacilityList: ArrayList<Map<String, Any>>) {
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(healthFacilityList)
        binding.etPhuChange.adapter = adapter
        binding.etPhuChange.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = it[DefinedParams.id] as String?
                        if (selectedId != DefaultID) {
                            viewModel.otherAssessmentDetails[ReferredPHUSiteID] = selectedId.toString()
                        } else {
                            if (viewModel.otherAssessmentDetails.containsKey(ReferredPHUSiteID)) {
                                viewModel.otherAssessmentDetails.remove(ReferredPHUSiteID)
                            }
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun bindSummaryView(
        title: String?,
        value: String?,
        valueTextColor: Int? = null,
    ) {
        if (title != null && value != null) {
            binding.parentLayout.addView(
                AssessmentCommonUtils.addViewSummaryLayout(
                    title,
                    value,
                    valueTextColor,
                    requireContext(),
                ),
            )
        }
    }
}
