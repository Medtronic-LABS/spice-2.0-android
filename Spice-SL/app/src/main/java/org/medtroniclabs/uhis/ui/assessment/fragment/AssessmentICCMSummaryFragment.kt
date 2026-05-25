package org.medtroniclabs.uhis.ui.assessment.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import org.json.JSONObject
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.convertStringToIntString
import org.medtroniclabs.uhis.common.CommonUtils.getOptionMap
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.getDateAfterDays
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ICCM
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_DIARRHEA_NOTIFIABLE_CONDITION
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_FEVER_NOTIFIABLE_CONDITION
import org.medtroniclabs.uhis.common.DefinedParams.OtherNotifiableConditionsForDiarrhoea
import org.medtroniclabs.uhis.common.DefinedParams.OtherNotifiableConditionsForFever
import org.medtroniclabs.uhis.common.DefinedParams.True
import org.medtroniclabs.uhis.common.DefinedParams.YES
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.databinding.FragmentAssessmentIccmSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapterCustomLayout
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentActivity
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.addViewSummaryLayout
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getNutritionStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getValueOfKeyFromMap
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ACT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Amoxicillin
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AssessmentNotes
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BreathPerMinute
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Dispensed
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_BREATHING
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_MONTH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_BREATHING
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_MONTH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.General_Danger_Signs
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IsClinicTaken
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.JellyWaterDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NextFollowupDate
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysOfCough
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysOfFever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.OrsDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ReferredPHUSiteID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SssDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ZincDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasCough
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasFever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasOedemaOfBothFeet
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isBreastfeed
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isVomiting
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.muacCode
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Diarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.DiarrhoeaSigns
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtPositive
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.otherSigns
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.cbs.fragment.CbsCallResultFragment

class AssessmentICCMSummaryFragment : BaseFragment(), View.OnClickListener {
    private val viewModel: AssessmentViewModel by activityViewModels()
    lateinit var binding: FragmentAssessmentIccmSummaryBinding
    private var datePickerDialog: DatePickerDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentIccmSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setListeners()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.ICCMSummaryAssessment)
        attachObserversForCbs()
    }

    private fun attachObserversForCbs() {
        viewModel.userProfileLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as? BaseActivity)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as? BaseActivity)?.hideLoading()
                    resourceState.data?.let {
                        viewModel.assessmentStringLiveData.value?.let { result ->
                            createSummaryViewCbs(createListSummaryData(result))
                        }
                        viewModel.memberDetailsLiveData.value
                            ?.data
                            ?.villageId
                            ?.takeIf { it.isNotBlank() }
                            ?.toLongOrNull()
                            ?.let(viewModel::getHealthFacilityBasedOnVillageId)
                    }
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity)?.hideLoading()
                }
            }
        }
        viewModel.patientHealthFacility.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    binding.callSupervisor.gone()
                    setEmergencyPHUPhoneNumber()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun setEmergencyPHUPhoneNumber() {
        val organizations = viewModel.patientHealthFacility.value?.data
        val linkedPHU = organizations
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.name }
            ?: getString(R.string.hyphen_symbol)
        bindSummaryView(getString(R.string.linked_phu), linkedPHU, forCbs = true)

        val phoneCode =
            SecuredPreference.getPhoneNumberCode()?.let { if (it.startsWith("+")) it else "+$it" }
        val phoneNumber = viewModel.patientHealthFacility.value
            ?.data
            ?.firstOrNull()
            ?.phoneNumber
        val resultPhoneNumber = if (!phoneNumber.isNullOrBlank()) phoneNumber else "-"
        bindSummaryView(
            getString(R.string.emergency_contact_at_PHU),
            resultPhoneNumber,
            isCallShown = true,
            forCbs = true,
            countryCode = phoneCode,
        )
    }

    private fun createSummaryViewCbs(listSummaryData: MutableList<AssessmentSummaryModel>?) {
        listSummaryData?.let { summaryData ->
            binding.cbsTvTitle.text = getString(R.string.cbs)
            binding.cbsEmptyErrorMessage.visibility = View.GONE
            binding.cbsParentLayout.visibility = View.VISIBLE
            binding.cbsParentLayout.removeAllViews()
            composeSummaryView(
                summaryData
                    .filter {
                        listOf(
                            ICCM_DIARRHEA_NOTIFIABLE_CONDITION,
                            ICCM_FEVER_NOTIFIABLE_CONDITION,
                            OtherNotifiableConditionsForFever,
                            OtherNotifiableConditionsForDiarrhoea,
                        ).contains(it.id)
                    }.toCollection(
                        mutableListOf(),
                    ),
            )
        } ?: kotlin.run {
            showErrorInSummaryCbs()
        }
    }

    private fun composeSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>) {
        listSummaryData
            .filter {
                it.value != null && !listOf(OtherNotifiableConditionsForDiarrhoea, OtherNotifiableConditionsForFever).contains(it.id)
            }.forEach { item ->
                if (item.id.equals(ICCM_DIARRHEA_NOTIFIABLE_CONDITION, true)) {
                    val otherValue =
                        listSummaryData
                            .find {
                                it.id.equals(
                                    OtherNotifiableConditionsForDiarrhoea,
                                    true,
                                )
                            }?.value
                    val value =
                        if (otherValue != null) "${item.value} - $otherValue" else item.value
                    bindSummaryView(item.title + " " + getString(R.string.hyphen_symbol) + " " + getString(R.string.diarrhoea), value, forCbs = true)
                } else if (item.id.equals(ICCM_FEVER_NOTIFIABLE_CONDITION, true)) {
                    val otherValue =
                        listSummaryData
                            .find {
                                it.id.equals(
                                    OtherNotifiableConditionsForFever,
                                    true,
                                )
                            }?.value
                    val value =
                        if (otherValue != null) "${item.value} - $otherValue" else item.value
                    bindSummaryView(item.title + " " + getString(R.string.hyphen_symbol) + " " + getString(R.string.fever), value, forCbs = true)
                } else {
                    bindSummaryView(item.title, item.value, forCbs = true)
                }
            }
        val supervisor = viewModel.userProfileLiveData.value
            ?.data
            ?.supervisor
        val supervisorNumber = supervisor?.phoneNumber.takeIf { !it.isNullOrBlank() }
            ?: getString(R.string.separator_double_hyphen)
        val supervisorName = supervisor?.let {
            if (!supervisor.firstName.isNullOrBlank() && !supervisor.lastName.isNullOrBlank()) {
                requireContext().getString(
                    R.string.firstname_lastname,
                    supervisor.firstName,
                    supervisor.lastName,
                )
            } else {
                getString(R.string.separator_double_hyphen)
            }
        } ?: getString(R.string.separator_double_hyphen)

        // Handle the case where phoneNumber might be null or empty

        bindSummaryView(getString(R.string.peer_supervisor_name), supervisorName, forCbs = true)

        val phoneCode = SecuredPreference.getPhoneNumberCode()?.let { if (it.startsWith("+")) it else "+$it" }
        bindSummaryView(
            getString(R.string.peer_supervisor_number),
            supervisorNumber,
            isCallShown = true,
            forCbs = true,
            countryCode = phoneCode,
        )
    }

    private fun showErrorInSummaryCbs() {
        binding.cbsEmptyErrorMessage.visibility = View.VISIBLE
        binding.cbsParentLayout.visibility = View.GONE
    }

    private fun initViews() {
        getClinicTakenData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = IsClinicTaken
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.otherAssessmentDetails,
                Pair(IsClinicTaken, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback,
            )
            binding.clinicTakenGroup.addView(view)
        }
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.otherAssessmentDetails[IsClinicTaken] = selectedID as String
        }

    private fun getClinicTakenData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun loadPhuSitesList(healthFacilityList: ArrayList<Map<String, Any>>) {
        binding.etPhuChange.background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_background)
        val background = binding.etPhuChange.background as? GradientDrawable
        background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), ContextCompat.getColor(requireContext(), R.color.edittext_stroke))
        val adapter = CustomSpinnerAdapterCustomLayout(requireContext())
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
                        viewModel.otherAssessmentDetails[ReferredPHUSiteID] =
                            selectedId?.toLong() ?: -1L
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
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
        binding.etNextFollowUpDate.background = null
        binding.etNextFollowUpDate.background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_background)
        val background = binding.etNextFollowUpDate.background as? GradientDrawable
        background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), ContextCompat.getColor(requireContext(), R.color.edittext_stroke))
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let { result ->
            var isCbs = false
            createListSummaryData(result)?.let { summaryList ->
                val filteredList = summaryList.filter { it.title?.equals(General_Danger_Signs, ignoreCase = true) == false }

                val hasCbsCondition = filteredList.any { item ->
                    (item.id.equals(hasDiarrhoea, true) || item.id.equals(hasFever, true)) &&
                        summaryList.any {
                            (it.id == ICCM_DIARRHEA_NOTIFIABLE_CONDITION && it.value != null) ||
                                it.id == ICCM_FEVER_NOTIFIABLE_CONDITION &&
                                it.value != null
                        } &&
                        item.value == YES
                }
                if (hasCbsCondition) {
                    isCbs = true
                    viewModel.getUserProfile()
                    binding.cbsResultCardView.visible()
                }
                updateStatusBar(isCbs)
                createSummaryView(summaryList)
            }
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

    private fun updateStatusBar(isCbs: Boolean = false) {
        when (viewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                viewModel.nearestFacilityLiveData.value?.data?.let { siteList ->
                    loadPhuSitesList(siteList)
                }
                binding.phuReferredGroup.visibility = View.VISIBLE
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
                if (viewModel.isDangerSignFlow) {
                    binding.riskResultLayout.text =
                        getString(R.string.urgent_referral)
                } else {
                    binding.riskResultLayout.text =
                        getString(R.string.referred_for_further_assessment)
                }
            }

            ReferralStatus.OnTreatment.name -> {
                binding.coughMalariaGroup.visibility = View.VISIBLE
                val date = getDateAfterDays(
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
        if (isCbs) {
            binding.riskResultLayout.text =
                getString(R.string.urgent_referral)
            binding.riskResultLayout.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
        }
    }

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }

    private fun createSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>?) {
        listSummaryData?.let { summaryData ->
            binding.emptyErrorMessage.visibility = View.GONE
            binding.parentLayout.visibility = View.VISIBLE
            binding.parentLayout.removeAllViews()
            val cbsList = listOf(
                ICCM_DIARRHEA_NOTIFIABLE_CONDITION,
                ICCM_FEVER_NOTIFIABLE_CONDITION,
                OtherNotifiableConditionsForFever,
                OtherNotifiableConditionsForDiarrhoea,
            )
            composeIccmSummaryView(
                summaryData.filter { !cbsList.contains(it.id) }.toCollection(
                    mutableListOf(),
                ),
            )
        } ?: kotlin.run {
            showErrorInSummary()
        }
    }

    companion object {
        const val TAG = "AssessmentICCMSummaryFragment"

        fun newInstance(): AssessmentICCMSummaryFragment = AssessmentICCMSummaryFragment()
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
                    value = getValueOfKeyFromMap(
                        StringConverter.stringToMap(data),
                        formLayout.id,
                        ICCM,
                    ),
                    noOfDays = formLayout.noOfDays,
                )
            }?.toMutableList()

    private fun composeIccmSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>) {
        // Na to Not applicable Ui level change
        listSummaryData.forEach { model ->
            if (model.value == AssessmentDefinedParams.NA) {
                model.value = AssessmentDefinedParams.NotApplicable
            }
        }
        bindICCMSummaryView(
            getString(R.string.patient_status),
            getStatus(viewModel.referralStatus) ?: getString(R.string.seperator_hyphen),
        )
        val isSignContain = composeGeneralDangerSignsResult(listSummaryData)
        listSummaryData
            .filter { it.title?.lowercase() != General_Danger_Signs.lowercase() }
            .forEach { item ->
                when (item.id) {
                    muacCode -> {
                        if (item.value != null) {
                            bindICCMSummaryView(
                                item.title,
                                requireContext().getString(
                                    R.string.nutrition_summary,
                                    item.value,
                                    getNutritionStatus(item.value, requireContext()),
                                ),
                            )
                        }
                        showHyphenSymbol(item)
                    }

                    hasDiarrhoea -> {
                        if (item.value == YES) {
                            val dehydrationStatus = getDehydrationStatus(isSignContain)
                            bindICCMSummaryView(
                                item.title,
                                dehydrationStatus?.let { result ->
                                    requireContext().getString(
                                        R.string.nutrition_summary,
                                        item.value,
                                        result,
                                    )
                                } ?: kotlin.run {
                                    requireContext().getString(
                                        R.string.nutrition_summary_without_signs,
                                        item.value,
                                    )
                                },
                            )
                        } else {
                            bindICCMSummaryView(item.title, item.value)
                        }
                        showHyphenSymbol(item)
                    }

                    hasCough -> {
                        if (item.value == YES) {
                            val status = getPneumoniaStatus()
                            if (status) {
                                bindICCMSummaryView(
                                    item.title,
                                    requireContext().getString(
                                        R.string.nutrition_summary,
                                        item.value,
                                        getString(R.string.pneumonia),
                                    ),
                                )
                            } else {
                                bindICCMSummaryView(item.title, item.value)
                            }
                        } else {
                            bindICCMSummaryView(item.title, item.value)
                        }
                        showHyphenSymbol(item)
                    }

                    BreathPerMinute -> {
                        item.value?.let { result ->
                            bindICCMSummaryView(
                                item.title,
                                requireContext().getString(
                                    R.string.firstname_lastname,
                                    convertStringToIntString(result),
                                    getString(R.string.bpm),
                                ),
                            )
                        }
                    }

                    DiarrhoeaSigns -> {
                        item.value?.let { result ->
                            bindICCMSummaryView(
                                item.title,
                                getSelectedSigns(result),
                            )
                        }
                    }

                    hasFever -> {
                        val rdtResult = viewModel.assessmentStringLiveData.value?.let {
                            val jsonObject = JSONObject(it)
                            val feverObject =
                                jsonObject.optJSONObject(MenuConstants.ICCM_MENU_ID)?.optJSONObject(
                                    AssessmentDefinedParams.Fever,
                                )
                            feverObject?.optString(ReferralDefinedParams.RdtTest)
                        }
                        if (item.value == YES && rdtResult == RdtPositive) {
                            bindICCMSummaryView(
                                item.title,
                                requireContext().getString(
                                    R.string.nutrition_summary,
                                    item.value,
                                    getString(R.string.malaria),
                                ),
                            )
                        } else {
                            bindICCMSummaryView(item.title, item.value)
                        }
                        showHyphenSymbol(item)
                    }

                    NoOfDaysOfCough, NoOfDaysDiarrhoea, NoOfDaysOfFever -> {
                        item.noOfDays?.let { maxDays ->
                            item.value?.let { enteredDays ->
                                bindICCMSummaryView(
                                    item.title,
                                    CommonUtils.getDaysValue(enteredDays, maxDays, requireContext()),
                                )
                            }
                        } ?: kotlin.run {
                            bindICCMSummaryView(item.title, item.value)
                        }
                    }

                    Amoxicillin.lowercase(), ACT.lowercase() -> {
                        if (item.value == Dispensed) {
                            bindICCMSummaryView(Dispensed, item.title)
                        }
                    }

                    else -> {
                        if ((item.id != OrsDispensedStatus) &&
                            (item.id != ZincDispensedStatus) &&
                            (item.id != JellyWaterDispensedStatus) &&
                            (item.id != SssDispensedStatus) &&
                            (item.id != otherSigns)
                        ) {
                            bindICCMSummaryView(item.title, item.value)
                        }
                    }
                }
                if (item.id == hasOedemaOfBothFeet) {
                    showHyphenSymbol(item)
                }
            }

        val zincDispensedStatus = listSummaryData.filter { it.id == ZincDispensedStatus }[0].value
        val orsDispensedStatus = listSummaryData.filter { it.id == OrsDispensedStatus }[0].value
        val jellyWaterDispensedStatus = listSummaryData.filter { it.id == JellyWaterDispensedStatus }[0].value
        val sssDispensedStatus = listSummaryData.filter { it.id == SssDispensedStatus }[0].value

        val dispensedStatus = mutableListOf<String>()
        zincDispensedStatus?.let {
            dispensedStatus.add(
                requireContext().getString(
                    R.string.zinc_or_ors_status,
                    requireContext().getString(R.string.zinc),
                    it,
                ),
            )
        }

        orsDispensedStatus?.let {
            dispensedStatus.add(
                requireContext().getString(
                    R.string.zinc_or_ors_status,
                    requireContext().getString(R.string.ors),
                    it,
                ),
            )
        }

        jellyWaterDispensedStatus?.let {
            dispensedStatus.add(
                requireContext().getString(
                    R.string.zinc_or_ors_status,
                    requireContext().getString(R.string.jelly_water),
                    it,
                ),
            )
        }

        sssDispensedStatus?.let {
            dispensedStatus.add(
                requireContext().getString(
                    R.string.zinc_or_ors_status,
                    requireContext().getString(R.string.sss),
                    it,
                ),
            )
        }

        if (dispensedStatus.isNotEmpty()) {
            bindICCMSummaryView(
                Dispensed,
                dispensedStatus.joinToString(", "),
            )
        }
    }

    private fun getSelectedSigns(listItems: String): String {
        val otherDiarrhoeaSigns = viewModel.assessmentStringLiveData.value?.let {
            val jsonObject = JSONObject(it)
            val otherObject = jsonObject.optJSONObject(MenuConstants.ICCM_MENU_ID)?.optJSONObject(
                Diarrhoea,
            )
            otherObject?.optString(otherSigns)
        }
        val result = if (!otherDiarrhoeaSigns.isNullOrBlank()) {
            requireContext().getString(R.string.other_value, listItems, otherDiarrhoeaSigns)
        } else {
            listItems
        }

        return result
    }

    private fun getPneumoniaStatus(): Boolean {
        var status = false
        viewModel.assessmentStringLiveData.value?.let {
            val jsonObject = JSONObject(it)
            val coughObject = jsonObject.optJSONObject(MenuConstants.ICCM_MENU_ID)?.optJSONObject(
                AssessmentDefinedParams.Cough.lowercase(),
            )
            coughObject?.optString(ReferralDefinedParams.BreathPerMinute)?.let { bpmValue ->
                viewModel.memberDetailsLiveData.value?.data?.let { details ->
                    DateUtils.dateToMonths(details.dateOfBirth).let { month ->
                        month?.let {
                            if ((month in FB_MIN_MONTH..11) && bpmValue.toInt() >= FB_MAX_BREATHING) {
                                status = true
                            } else if (month in FB_MAX_MONTH..60 && bpmValue.toInt() >= FB_MIN_BREATHING) {
                                status = true
                            }
                        }
                    }
                }
            }
        }
        return status
    }

    private fun getDehydrationStatus(signContain: Boolean): String? {
        var result: String? = null
        viewModel.assessmentStringLiveData.value?.let {
            val jsonObject = JSONObject(it)
            val feverObject =
                jsonObject.optJSONObject(MenuConstants.ICCM_MENU_ID)?.optJSONObject(
                    Diarrhoea,
                )
            val diarrhoeaDays =
                feverObject?.optString(ReferralDefinedParams.NoOfDaysOfDiarrhoea)
            val bloodyDiarrhoea =
                feverObject?.optString(ReferralDefinedParams.IsBloodyDiarrhoea)
            diarrhoeaDays?.let {
                bloodyDiarrhoea?.let {
                    if (diarrhoeaDays.isNotEmpty()) {
                        result = if (diarrhoeaDays.toInt() >= 14 || bloodyDiarrhoea == True) {
                            requireContext().getString(R.string.severe_dehydration)
                        } else {
                            requireContext().getString(R.string.moderate_dehydration)
                        }
                    } else {
                        requireContext().getString(R.string.hyphen_symbol)
                    }
                }
            }
        }
        return result
    }

    private fun composeGeneralDangerSignsResult(listSummaryData: MutableList<AssessmentSummaryModel>): Boolean {
        var isSignContain = false
        val targetIds = setOf(
            isUnusualSleepy,
            isConvulsionPastFewDays,
            isVomiting,
            isBreastfeed,
        )
        val signsList = listOf(
            AssessmentDefinedParams.SunkenEyes.lowercase(),
            AssessmentDefinedParams.NoTearsWhenCrying.lowercase(),
            AssessmentDefinedParams.LittleOrNoUrine.lowercase(),
            AssessmentDefinedParams.SkinPinch.lowercase(),
            AssessmentDefinedParams.VeryThirsty.lowercase(),
            AssessmentDefinedParams.DryMouthOrTongue.lowercase(),
            AssessmentDefinedParams.SunkenFontanella.lowercase(),
        )

        var result = DefinedParams.NO
        for (assessment in listSummaryData) {
            if (assessment.value == YES) {
                if (viewModel.isDangerSignFlow) {
                    result = when (viewModel.dangerSingsKey.toString()) {
                        isUnusualSleepy -> getString(R.string.unconscious_unusually_sleepy)
                        isVomiting -> getString(R.string.vomits_everything)
                        isConvulsionPastFewDays -> getString(R.string.convulsions)
                        isBreastfeed -> getString(R.string.unable_to_drink_or_breastfeed)
                        else -> {
                            getString(R.string.separator_double_hyphen)
                        }
                    }
                }
            }
            if (assessment.title.equals(
                    AssessmentDefinedParams.Signs,
                    true,
                ) &&
                signsList.any { sign ->
                    assessment.value?.lowercase()?.contains(sign) == true
                }
            ) {
                isSignContain = true
            }
        }
        bindICCMSummaryView(
            getString(R.string.general_danger_sign),
            result,
        )
        return isSignContain
    }

    private fun bindSummaryView(
        title: String?,
        value: String?,
        valueTextColor: Int? = null,
        isCallShown: Boolean = false,
        forCbs: Boolean = false,
        countryCode: String? = null,
    ) {
        value?.let { result ->
            binding.cbsParentLayout.addView(
                addViewSummaryLayout(
                    title,
                    result,
                    valueTextColor,
                    requireContext(),
                    isCallShown = isCallShown,
                    callBtnTag = title,
                    callback = { tag, value ->
                        tag?.let {
                            handleCallBtnClick(tag, value)
                        }
                    },
                    forCbs = forCbs,
                    countryCode = countryCode,
                ),
            )
        }
    }

    private fun bindICCMSummaryView(
        title: String?,
        value: String?,
        valueTextColor: Int? = null,
    ) {
        value?.let { result ->
            binding.parentLayout.addView(
                addViewSummaryLayout(
                    title,
                    result,
                    valueTextColor,
                    requireContext(),
                ),
            )
        }
    }

    var type: String = ""

    private fun handleCallBtnClick(
        tag: String,
        value: String?,
    ) {
        type = if (tag == getString(R.string.peer_supervisor_number)) {
            DefinedParams.ps
        } else {
            DefinedParams.phu
        }
        value?.let {
            navToDial(it)
        }
    }

    private fun navToDial(phoneNumber: String?) {
        if (hasTelephonyFeature(requireContext())) {
            phoneNumber?.let {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:$it")
                dialerLauncher.launch(dialIntent)
            }
        } else {
            showCallDialError(false)
        }
    }

    private val dialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
                CbsCallResultFragment
                    .newInstance(type)
                    .show(childFragmentManager, CbsCallResultFragment.TAG)
            }
        }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnDone.id -> {
                withLocationCheck(::assessmentICCMDone)
            }

            binding.etNextFollowUpDate.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun assessmentICCMDone() {
        viewModel.fetchCurrentLocation(requireContext())
        if (binding.etNextFollowUpDate.text.isNotEmpty()) {
            updateFollowUpDate(
                binding.etNextFollowUpDate.text
                    .trim()
                    .toString(),
            )
        }
        if (viewModel.otherAssessmentDetails.isEmpty()) {
            val currentActivity = requireActivity()
            requireActivity().startBackgroundOfflineSync()
            if (currentActivity is AssessmentActivity) {
                currentActivity.finishSuccessFlow()
            }
        } else {
            viewModel.updateOtherAssessmentDetails()
        }
        viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
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
            viewModel.otherAssessmentDetails[NextFollowupDate] =
                DateUtils.convertDateTimeToDate(
                    date,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true,
                )
        }
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

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()

    fun showHyphenSymbol(item: AssessmentSummaryModel) {
        if (viewModel.isDangerSignFlow) {
            bindICCMSummaryView(
                item.title,
                getString(R.string.separator_double_hyphen),
            )
        }
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(requireActivity())
        locationManager.getCurrentLocation {
            viewModel.setCurrentLocation(it)
        }
    }
}
