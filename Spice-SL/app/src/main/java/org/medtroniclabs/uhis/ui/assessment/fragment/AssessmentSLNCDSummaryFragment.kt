package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.formatDecimalValue
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.databinding.FragmentAssessmentSLNCDSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.Screening.BMI
import org.medtroniclabs.uhis.mappingkey.Screening.Height
import org.medtroniclabs.uhis.mappingkey.Screening.Weight
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.MenuConstants.NCD_MENU_ID
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AlcoholConsumption
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DiagnosedWithDiabetes
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RegularSmoker
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.WaistCircumference
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.otherConcerningSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.signsAndSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.symptomsDTO
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

@AndroidEntryPoint
class AssessmentSLNCDSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentAssessmentSLNCDSummaryBinding
    private val viewModel: AssessmentViewModel by activityViewModels()
    private var riskFactors: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAssessmentSLNCDSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun initView() {
        getClinicTakenData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = AssessmentDefinedParams.IsClinicTaken
            view.addViewElements(
                it,
                false,
                viewModel.otherAssessmentDetails,
                Pair(AssessmentDefinedParams.IsClinicTaken, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback,
            )
            binding.llClinicTakenGroup.addView(view)
        }
        binding.btnDone.safeClickListener(this)
        viewModel.setUserJourney(AnalyticsDefinedParams.NCDASSESSMENTSUMMARY)
    }

    private fun getClinicTakenData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.otherAssessmentDetails[AssessmentDefinedParams.IsClinicTaken] = selectedID as String
        }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            updateStatusBar()
            createSummaryView(createListSummaryData(it))
        }
    }

    private fun createListSummaryData(data: String): MutableList<AssessmentSummaryModel>? =
        viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.filter {
                it.isSummary == true
            }?.map { formLayout ->
                AssessmentSummaryModel(
                    title = formLayout.titleSummary ?: formLayout.title,
                    id = formLayout.id,
                    cultureValue = formLayout.titleCulture,
                    value = AssessmentCommonUtils.getValueOfKeyFromMap(
                        StringConverter.stringToMap(data),
                        formLayout.id,
                        menuType = NCD_MENU_ID,
                    ),
                )
            }?.toMutableList()

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnDone -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
                viewModel.updateOtherAssessmentDetails()
            }
        }
    }

    private fun updateStatusBar() {
        when (viewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                viewModel.nearestFacilityLiveData.value?.data?.let { siteList ->
                    loadPhuSitesList(siteList)
                }
                binding.clinicTakenGroup.visible()
                binding.phuReferredGroup.visible()
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
                binding.riskResultLayout.text = getString(R.string.referred_for_ncd_assessment)
            }

            else -> {
                binding.clinicTakenGroup.gone()
                binding.phuReferredGroup.gone()
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.green_attention_color)
                binding.riskResultLayout.text = getString(R.string.no_refferral_treatment_required)
            }
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
                        val selectedId = it[DefinedParams.ID] as String?
                        if (selectedId != DefinedParams.DEFAULT_ID) {
                            viewModel.otherAssessmentDetails[AssessmentDefinedParams.ReferredPHUSiteID] = selectedId.toString()
                        } else {
                            if (viewModel.otherAssessmentDetails.containsKey(AssessmentDefinedParams.ReferredPHUSiteID)) {
                                viewModel.otherAssessmentDetails.remove(AssessmentDefinedParams.ReferredPHUSiteID)
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

    private fun createSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>?) {
        listSummaryData?.let { summaryData ->
            binding.emptyErrorMessage.visibility = View.GONE
            binding.parentLayout.visibility = View.VISIBLE
            binding.parentLayout.removeAllViews()
            composeSummaryView(summaryData)
        } ?: kotlin.run {
            showErrorInSummary()
        }
    }

    private fun composeSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>) {
        getStatus(viewModel.referralStatus)?.let {
            bindSummaryView(
                getString(R.string.patient_status),
                it,
            )
        }

        listSummaryData.forEach { item ->
            item.value?.let {
                when (item.id) {
                    DiagnosedWithDiabetes, RegularSmoker, AlcoholConsumption -> showRiskFactors(listSummaryData)
                    Height, Weight, WaistCircumference -> bindSummaryView(item.title, CommonUtils.getDecimalFormatted(formatDecimalValue(it)))
                    BMI -> bindSummaryView(item.title, renderBMISummary(listSummaryData))
                    symptomsDTO -> renderDangerSigns(listSummaryData)
                    else -> bindSummaryView(item.title, it)
                }
            }
        }
    }

    private fun renderBMISummary(listSummaryData: MutableList<AssessmentSummaryModel>): String? {
        var bmiText = getString(R.string.seperator_hyphen)
        val height = listSummaryData.filter { it.id?.contains(Height) == true }.firstOrNull()?.value
        val weight = listSummaryData.filter { it.id?.contains(Weight) == true }.firstOrNull()?.value

        if (height != null && weight != null) {
            val bmi = CommonUtils.getBMIForNcd(height.toDouble(), weight.toDouble())
            CommonUtils
                .getBMIInformation(requireContext(), bmi?.toDoubleOrNull())
                ?.let { info ->
                    bmiText = "$bmi (${info.first})"
                }
        }
        return bmiText
    }

    private fun renderDangerSigns(summaryData: MutableList<AssessmentSummaryModel>) {
        /*
        val otherConcernSymptoms = viewModel.assessmentStringLiveData.value?.let {
            val firstNcdString = it.substringAfter("\"ncd\":{")
                .substringBeforeLast("},\"bmiCategory")
                .plus("}}")
            val firstNcdObject = JSONObject("{\"ncd\":{$firstNcdString}")
                .optJSONObject(ncd)
                ?.optJSONObject(signsAndSymptoms)
             firstNcdObject?.optString(otherConcerningSymptoms)
        }*/
        val otherConcernSymptoms = viewModel.assessmentStringLiveData.value?.let {
            val jsonObject = JSONObject(it)
            val feverObject = jsonObject.optJSONObject(MenuConstants.NCD_MENU_ID)?.optJSONObject(
                signsAndSymptoms,
            )
            feverObject?.optString(otherConcerningSymptoms)
        }

        summaryData.find { it.id == symptomsDTO }?.let { item ->
            val result = if (!otherConcernSymptoms.isNullOrBlank()) {
                requireContext().getString(R.string.other_value, item.value, otherConcernSymptoms)
            } else {
                item.value
            }
            bindSummaryView(
                getString(R.string.symptoms),
                result ?: getString(R.string.seperator_hyphen),
            )
        }
    }

    private fun showRiskFactors(listSummaryData: MutableList<AssessmentSummaryModel>) {
        if (!riskFactors) {
            val risks = mutableListOf<String>()

            listSummaryData.forEach { assessment ->
                when (assessment.id) {
                    DiagnosedWithDiabetes -> if (assessment.value.equals(DefinedParams.YES, true)) risks.add(getString(R.string.diabetes_or_hypertension))
                    RegularSmoker -> if (assessment.value.equals(DefinedParams.YES, true)) risks.add(getString(R.string.tobacco_consumption))
                    AlcoholConsumption -> if (assessment.value.equals(DefinedParams.YES, true)) risks.add(getString(R.string.alcohol_consumption))
                }
            }
            riskFactors = true
            if (risks.isNotEmpty()) {
                bindSummaryView(getString(R.string.ncd_risk_factor), risks.joinToString(", "))
            }
        }
    }

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }

    fun getStatus(referralStatus: String?): String? =
        when (referralStatus) {
            ReferralStatus.Referred.name -> getString(R.string.referred)
            ReferralStatus.OnTreatment.name -> getString(R.string.on_treatment)
            ReferralStatus.Recovered.name -> getString(R.string.recovered)
            else -> {
                null
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

    companion object {
        const val TAG = "AssessmentSLNCDSummaryFragment"

        fun newInstance(): AssessmentSLNCDSummaryFragment = AssessmentSLNCDSummaryFragment()
    }

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()
}
