package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import org.json.JSONObject
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.DefaultID
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentBdNcdSummaryBinding
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.findValueByKey
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getSpinnerDisplayValue
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CULTURE_VALUE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_TEST_OUTCOME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_UPAZILA
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NAME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REFERRAL_FACILITY_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REFERRED_SITE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ReferredPHUSiteID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TYPE_OF_GLASS
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class BDEyeCareAssessmentSummaryFragment : BaseFragment() {
    private val viewModel: AssessmentViewModel by activityViewModels()
    private lateinit var binding: FragmentBdNcdSummaryBinding

    companion object {
        const val TAG = "BDEyeCareAssessmentSummaryFragment"

        fun newInstance(): BDEyeCareAssessmentSummaryFragment = BDEyeCareAssessmentSummaryFragment()
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
        viewModel.otherAssessmentDetails[REFERRED_SITE] = SecuredPreference.getOrganizationFhirId()

        binding.btnDone.setOnClickListener {
            viewModel.updateOtherAssessmentDetails()
        }
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            val json = JSONObject(it)
            updateStatusBar(json)
            val items = createNCDSummaryData(json, isTranslationEnabled)
            createSummaryView(items, isTranslationEnabled)
        }
    }

    private fun createSummaryView(
        listSummaryData: MutableList<AssessmentSummaryModel>?,
        isTranslationEnabled: Boolean,
    ) {
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

    private fun createNCDSummaryData(
        json: JSONObject,
        isTranslationEnabled: Boolean,
    ): MutableList<AssessmentSummaryModel>? =
        viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.filter { it.isSummary == true }
            ?.mapNotNull { formLayout ->
                val value = getValueFromJson(formLayout, json, isTranslationEnabled)

                value?.let {
                    AssessmentSummaryModel(
                        title = formLayout.titleSummary ?: formLayout.title,
                        id = formLayout.id,
                        cultureValue = formLayout.titleSummaryCulture ?: formLayout.titleCulture,
                        value = it,
                    )
                }
            }?.toMutableList()

    private fun getValueFromJson(
        layout: FormLayout,
        jsonObject: JSONObject,
        isTranslationEnabled: Boolean,
    ): String? {
        val selectedValue = findValueByKey(jsonObject, layout.id)?.toString()

        return if (layout.id == EYE_TEST_OUTCOME || layout.id == TYPE_OF_GLASS) {
            layout.optionsList
                ?.find { (it[ID] as String) == selectedValue }
                ?.let { item ->
                    if (isTranslationEnabled) {
                        item[CULTURE_VALUE] as String
                    } else {
                        item[NAME] as String
                    }
                }
        } else {
            selectedValue
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
        binding.parentLayout.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                title,
                value,
                valueTextColor,
                requireContext(),
            ),
        )
    }

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()
}
