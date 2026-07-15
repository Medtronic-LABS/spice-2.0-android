package org.medtroniclabs.uhis.ui.assessment.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.CommonUtils.getDaysValue
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.databinding.FragmentAssessmentOtherSymptomSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants.OTHER_SYMPTOMS
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Amoxicillin
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AssessmentNotes
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Dispensed
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Fever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysOfFever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ReferredPHUSiteID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasFever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.otherConcerningSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.otherSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.signsAndSymptoms
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtPositive
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtTest
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

@AndroidEntryPoint
class AssessmentOtherSymptomSummaryFragment : BaseFragment(), View.OnClickListener {
    lateinit var binding: FragmentAssessmentOtherSymptomSummaryBinding
    private val viewModel: AssessmentViewModel by activityViewModels()
    private var datePickerDialog: DatePickerDialog? = null
    private var isValid: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentOtherSymptomSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.OtherSymptomsSummary)
    }

    private fun setListeners() {
        binding.btnDone.safeClickListener(this)
        binding.etNotes.addTextChangedListener { input ->
            input?.let {
                val resultValue = input.trim().toString()
                if (resultValue.isNotBlank()) {
                    viewModel.otherAssessmentDetails[AssessmentNotes] = resultValue
                }
            }
        }
        binding.etNextFollowUpDate.safeClickListener(this)
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            updateStatusBar()
            createSummaryView(createListSummaryData(it))
        }

        viewModel.nearestFacilityLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun updateStatusBar() {
        when (viewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                viewModel.nearestFacilityLiveData.value?.data?.let { siteList ->
                    loadPhuSitesList(siteList)
                }
                binding.phuReferredGroup.visibility = View.VISIBLE
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
                binding.riskResultLayout.text = getString(R.string.referred_for_further_assessment)
            }

            ReferralStatus.OnTreatment.name -> {
                binding.coughMalariaGroup.visibility = View.VISIBLE
                val date = DateUtils.getDateAfterDays(
                    viewModel.referralReason
                        ?.mapNotNull { viewModel.treatmentDays[it] }
                        ?.minOrNull() ?: 3,
                )
                binding.etNextFollowUpDate.text = date
                if (date.isNotEmpty()) {
                    updateFollowUpDate(date)
                }
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.red_risk_moderate)
                binding.riskResultLayout.text = getString(R.string.patient_on_treatment)
            }

            else -> {
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
                        viewModel.otherAssessmentDetails[ReferredPHUSiteID] = selectedId?.toLong() ?: -1L
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
            renderSummaryView(summaryData)
        } ?: kotlin.run {
            showErrorInSummary()
        }
    }

    private fun renderSummaryView(summaryData: MutableList<AssessmentSummaryModel>) {
        // Na to Not applicable Ui level change
        summaryData.forEach { model ->
            if (model.value == AssessmentDefinedParams.NA) {
                model.value = AssessmentDefinedParams.NotApplicable
            }
        }
        bindSummaryView(
            getString(R.string.patient_status),
            getStatus(viewModel.referralStatus) ?: getString(R.string.seperator_hyphen),
        )
        summaryData
            .filter { it.title?.lowercase() != AssessmentDefinedParams.General_Danger_Signs.lowercase() }
            .forEach { item ->
                when (item.id) {
                    hasFever -> {
                        val rdtResult = viewModel.assessmentStringLiveData.value?.let {
                            val jsonObject = JSONObject(it)
                            val feverObject =
                                jsonObject.optJSONObject(OTHER_SYMPTOMS)?.optJSONObject(
                                    Fever,
                                )
                            feverObject?.optString(RdtTest)
                        }
                        if (item.value == DefinedParams.YES && rdtResult == RdtPositive) {
                            bindSummaryView(
                                item.title,
                                requireContext().getString(
                                    R.string.nutrition_summary,
                                    item.value,
                                    getString(R.string.malaria),
                                ),
                            )
                        } else {
                            bindSummaryView(item.title, item.value)
                        }
                    }

                    Amoxicillin.lowercase() -> {
                        if (item.value == Dispensed) {
                            bindSummaryView(Dispensed, item.title)
                        }
                    }

                    NoOfDaysOfFever -> {
                        item.noOfDays?.let { maxDays ->
                            item.value?.let { enteredDays ->
                                bindSummaryView(
                                    item.title,
                                    getDaysValue(enteredDays, maxDays, requireContext()),
                                )
                            }
                        } ?: kotlin.run {
                            bindSummaryView(item.title, item.value)
                        }
                    }

                    else -> {
                        bindSummaryView(item.title, item.value)
                    }
                }
            }
        renderDangerSigns(summaryData)
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

    private fun renderDangerSigns(summaryData: MutableList<AssessmentSummaryModel>) {
        val otherConcernSymptoms = viewModel.assessmentStringLiveData.value?.let {
            val jsonObject = JSONObject(it)
            val feverObject = jsonObject.optJSONObject(OTHER_SYMPTOMS)?.optJSONObject(
                signsAndSymptoms,
            )
            feverObject?.optString(otherConcerningSymptoms)
        }
        summaryData.find { it.id == otherSymptoms }?.let { item ->
            val result = if (!otherConcernSymptoms.isNullOrBlank()) {
                requireContext().getString(R.string.other_value, item.value, otherConcernSymptoms)
            } else {
                item.value
            }
            bindSummaryView(
                getString(R.string.other_signs_and_symptoms),
                result ?: getString(R.string.seperator_hyphen),
            )
        }
    }

    private fun bindSummaryView(
        title: String?,
        value: String?,
        valueTextColor: Int? = null,
    ) {
        value?.let { result ->
            binding.parentLayout.addView(
                AssessmentCommonUtils.addViewSummaryLayout(
                    title,
                    result,
                    valueTextColor,
                    requireContext(),
                ),
            )
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
                        OTHER_SYMPTOMS,
                    ),
                    noOfDays = formLayout.noOfDays,
                )
            }?.toMutableList()

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                withLocationCheck({
                    viewModel.fetchCurrentLocation(requireContext())
                    binding.etNextFollowUpDate.text?.let {
                        updateFollowUpDate(it.trim().toString())
                    }
                    viewModel.updateOtherAssessmentDetails()
                    viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
                })
            }

            binding.etNextFollowUpDate.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.etNextFollowUpDate.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.etNextFollowUpDate.text.toString())
        }
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.etNextFollowUpDate.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy,
                    )
                updateFollowUpDate(
                    binding.etNextFollowUpDate.text
                        .toString()
                        .trim(),
                )
                datePickerDialog = null
            }
        }
    }

    private fun updateFollowUpDate(date: String) {
        if (date.isNotEmpty()) {
            viewModel.otherAssessmentDetails[AssessmentDefinedParams.NextFollowupDate] =
                DateUtils.convertDateTimeToDate(
                    date,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true,
                )
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()
}
