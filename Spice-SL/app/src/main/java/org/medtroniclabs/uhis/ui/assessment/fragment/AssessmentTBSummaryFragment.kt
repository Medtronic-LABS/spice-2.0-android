package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.DefinedParams.HouseholdHead
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.databinding.FragmentAssessmentTBSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.MemberRegistration
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getValueOfKeyFromMap
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DateOfOnset
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HasCoughLastedLonger
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HasNightSweatsTB
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HasWeightLoss
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.PreviouslyTreatedForTB
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ReferredPHUSiteID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RelationshipToIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SleepLocation
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasCough
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasFever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.otherRelationshipIC
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class AssessmentTBSummaryFragment : Fragment(), View.OnClickListener {
    private val viewModel: AssessmentViewModel by activityViewModels()
    lateinit var binding: FragmentAssessmentTBSummaryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentTBSummaryBinding.inflate(inflater, container, false)
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
        binding.btnDone.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
                viewModel.updateOtherAssessmentDetails()
            }
        }
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            updateStatusBar()
            createSummaryView(createTBListSummaryData(it))
        }
    }

    private fun createTBListSummaryData(data: String): MutableList<AssessmentSummaryModel>? =
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
                    value = getValueOfKeyFromMap(
                        StringConverter.stringToMap(data),
                        formLayout.id,
                        TB,
                    ),
                )
            }?.toMutableList()

    private fun createSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>?) {
        listSummaryData?.let { summaryData ->
            binding.emptyErrorMessage.visibility = View.GONE
            binding.parentLayout.visibility = View.VISIBLE
            binding.parentLayout.removeAllViews()
            composeTbSummaryView(summaryData)
        } ?: kotlin.run {
            showErrorInSummary()
        }
    }

    private fun composeTbSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>) {
        val isContactTrace = listSummaryData.any { it.id == RelationshipToIC }
        if (isContactTrace) {
            binding.tvTitle.text = binding.root.context.getString(R.string.contact_tracing)
            val stringBuilder = StringBuilder()
            listSummaryData.forEach { item ->
                item.value?.let {
                    when (item.id) {
                        RelationshipToIC -> {
                            val displayValue = when {
                                it.equals(MemberRegistration.OTHER_RELATION, ignoreCase = true) -> {
                                    val data =
                                        listSummaryData.firstOrNull { data -> data.id == otherRelationshipIC }
                                    val title = data?.title?.takeIf { title -> title.isNotBlank() }
                                        ?: item.title
                                    val value = data?.value?.takeIf { value -> value.isNotBlank() }

                                    if (value != null) "$title - $value" else getString(R.string.hyphen_symbol)
                                }

                                it.equals(
                                    HouseholdHead,
                                    ignoreCase = true,
                                ) -> getString(R.string.household_head)

                                else -> it
                            }
                            bindTbSummaryView(item.title, displayValue)
                        }
                        SleepLocation -> bindTbSummaryView(removeTextInBrackets(item.title), it)
                        PreviouslyTreatedForTB, hasCough -> bindTbSummaryView(item.title, capitalizeYesNo(it))
                        HasCoughLastedLonger, HasNightSweatsTB, hasFever, HasWeightLoss ->
                            stringBuilder.appendWithComma(removeLastChar(item.title))
                        DateOfOnset -> {
                            bindTbSummaryView(
                                getString(R.string.date_of_onset),
                                DateUtils.convertDateFormat(
                                    it,
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                    DateUtils.DATE_ddMMyyyy,
                                ),
                            )
                        }
                    }
                }
            }
            bindTbSummaryView(getString(R.string.tb_symptoms), stringBuilder.toString())
        } else {
            getStatus(viewModel.referralStatus)?.let {
                bindTbSummaryView(
                    getString(R.string.patient_status),
                    it,
                )
            }

            listSummaryData.forEach { item ->
                item.value?.let {
                    when (item.id) {
                        hasCough -> bindTbSummaryView(item.title, capitalizeYesNo(it))
                        HasCoughLastedLonger, HasNightSweatsTB, hasFever, HasWeightLoss ->
                            bindTbSummaryView(removeLastChar(item.title), it)
                        DateOfOnset -> {
                            bindTbSummaryView(
                                getString(R.string.date_of_onset),
                                DateUtils.convertDateFormat(
                                    it,
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                    DateUtils.DATE_ddMMyyyy,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun StringBuilder.appendWithComma(text: String) {
        if (this.isNotEmpty()) append(", ")
        append(text)
    }

    private fun removeTextInBrackets(input: String?): String =
        input?.let {
            if (it.isNotEmpty()) it.replace(Regex("\\(.*?\\)"), "").trim() else ""
        } ?: ""

    private fun removeLastChar(input: String?): String =
        input?.let {
            if (it.isNotEmpty()) it.dropLast(1) else ""
        } ?: ""

    private fun capitalizeYesNo(value: String): String = value.lowercase().replaceFirstChar { it.uppercase() }

    private fun bindTbSummaryView(
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

    private fun updateStatusBar() {
        when (viewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                viewModel.nearestFacilityLiveData.value?.data?.let { siteList ->
                    loadPhuSitesList(siteList)
                }
                binding.labelPhuReferred.visible()
                binding.etPhuChange.visible()
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
                binding.riskResultLayout.text = getString(R.string.referred_for_further_assessment)
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

    private fun getStatus(referralStatus: String?): String? =
        when (referralStatus) {
            ReferralStatus.Referred.name -> getString(R.string.referred)
            ReferralStatus.OnTreatment.name -> getString(R.string.on_treatment)
            ReferralStatus.Recovered.name -> getString(R.string.recovered)
            else -> {
                null
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
                        if (selectedId != DEFAULT_ID) {
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

    companion object {
        const val TAG = "AssessmentTBSummaryFragment"

        fun newInstance(): AssessmentTBSummaryFragment = AssessmentTBSummaryFragment()
    }

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()
}
