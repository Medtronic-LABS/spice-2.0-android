package org.medtroniclabs.uhis.ui.assessment.fragment

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.isMandateOrNot
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.getYearMonthAndWeek
import org.medtroniclabs.uhis.common.DefinedParams.CbsNotifiableCondition
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_DIARRHEA_NOTIFIABLE_CONDITION
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_FEVER_NOTIFIABLE_CONDITION
import org.medtroniclabs.uhis.common.DefinedParams.NOTIFIABLE_CONDITIONS
import org.medtroniclabs.uhis.common.DefinedParams.RmnchNotifiableCondition
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.INFORMATION
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VISIBLE
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.formgeneration.utility.FastBreathingLayoutFragment
import org.medtroniclabs.uhis.formgeneration.utility.InformationLayoutFragment
import org.medtroniclabs.uhis.formgeneration.utility.RecommendedDosageFragment
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants.ICCM_MENU_ID
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getMuacColorCode
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getNutritionStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ACT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ACTStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Amoxicillin
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AmoxicillinStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BreathPerMinute
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CoughCondition
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DiarrheaCondition
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_BREATHING
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_MONTH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_YEAR
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_BREATHING
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_MONTH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_YEAR
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FeverCondition
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.JellyWaterDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MUAC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MalnutritionCondition
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ModerateDehydration
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysOfCough
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoOfDaysOfFever
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ORSStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.OrsDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SevereDehydration
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SssDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ZincDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ZincStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.chestInDrawing
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.errorSuffix
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasCough
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasOedemaOfBothFeet
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.infoSuffixText
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isBreastfeed
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isVomiting
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.muacCode
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.muacStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.rdtTest
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.rootSuffix
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.summaryKey
import org.medtroniclabs.uhis.ui.assessment.dialog.DangerSignsDialog
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.IsBloodyDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtPositive
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtTest
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralReasons
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

@AndroidEntryPoint
class AssessmentICCMFragment :
    BaseFragment(),
    FormEventListener,
    View.OnClickListener,
    DangerSignsDialog.DangerSignsClickListener {
    private lateinit var binding: FragmentAssessmentBinding
    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        getFormDataForWorkflow()
        setListeners()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.ICCMAssessment)
    }

    private fun setListeners() {
        binding.btnSubmit.safeClickListener(this)
    }

    private fun getFormDataForWorkflow() {
        viewModel.getFormData(ICCM_MENU_ID)
        viewModel.getNearestHealthFacility()
    }

    private fun attachObservers() {
        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.populateViews(data.formLayout)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.isAssessmentCancelLiveData.observe(viewLifecycleOwner) {
            if (it) {
                formGenerator.getViewByTag(muacCode)?.apply {
                    val background = background as? GradientDrawable
                    background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
                }
            }
        }
    }

    private fun renderDosageDetails(dateOfBirth: String) {
        val age = CommonUtils.convertStringDobToMonths(
            dateOfBirth,
        )
        age?.let {
            /**
             * ACT Status Condition Rendering
             */
            if (age >= 6) {
                formGenerator.getViewByTag(ACTStatus)?.let {
                    if (it is TextView) {
                        it.text = requireContext().getString(R.string.act_6)
                    }
                }
                formGenerator.getViewByTag(ACTStatus + infoSuffixText)?.let {
                    if (it is TextView) {
                        it.text =
                            getACTSuffixText(
                                viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                    CommonUtils.convertStringDobToMonths(
                                        dob,
                                    )
                                },
                            )
                        it.visibility = View.VISIBLE
                    }
                }
            } else {
                formGenerator.getViewByTag(ACTStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }
            }

            /**
             * Amoxicillin & Zinc Condition Status rendering
             */
            if (age >= 2) {
                formGenerator.getViewByTag(AmoxicillinStatus)?.let {
                    if (it is TextView) {
                        it.text = requireContext().getString(R.string.amoxicillin_250_mg)
                    }
                }

                formGenerator.getViewByTag(ZincStatus)?.let {
                    if (it is TextView) {
                        it.text = requireContext().getString(R.string.zinc_tablet)
                    }
                }

                formGenerator.getViewByTag(AmoxicillinStatus + infoSuffixText)?.let {
                    if (it is TextView) {
                        it.text =
                            getSuffixText(
                                viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                    CommonUtils.convertStringDobToMonths(
                                        dob,
                                    )
                                },
                            )
                        it.visibility = View.VISIBLE
                    }
                }

                formGenerator.getViewByTag(ZincStatus + infoSuffixText)?.let {
                    if (it is TextView) {
                        it.text =
                            getZincSuffixText(
                                viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                    CommonUtils.convertStringDobToMonths(
                                        dob,
                                    )
                                },
                            )
                        it.visibility = View.VISIBLE
                    }
                }
            } else {
                formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }

                formGenerator.getViewByTag(ZincStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }
            }

            formGenerator.getViewByTag(ORSStatus)?.let {
                if (it is TextView) {
                    it.text = requireContext().getString(R.string.ors_packet)
                }
            }
            formGenerator.getViewByTag(ORSStatus + infoSuffixText)?.let {
                if (it is TextView) {
                    it.text = requireContext().getString(R.string.with_one_litre_of_water)
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initView() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        )
    }

    companion object {
        const val TAG = "AssessmentICCMFragment"
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        val title = if (setOf(
                CbsNotifiableCondition,
                ICCM_FEVER_NOTIFIABLE_CONDITION,
                ICCM_DIARRHEA_NOTIFIABLE_CONDITION,
                NOTIFIABLE_CONDITIONS,
                RmnchNotifiableCondition,
            ).contains(id)
        ) {
            getString(R.string.notifiable_conditions)
        } else {
            null
        }
        CheckBoxDialog
            .newInstance(id, resultMap, title = title) { map ->
                formGenerator.validateCheckboxDialogue(id, formLayout, map)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        viewModel.instructionId = id
        viewModel.dosageListModel = dosageListModel
        showInstructionDialog(id)
    }

    private fun showInstructionDialog(id: String) {
        val titleById = getTitleById(id)
        when (id) {
            isUnusualSleepy, isVomiting, isConvulsionPastFewDays, isBreastfeed, muacCode, hasOedemaOfBothFeet, chestInDrawing, rdtTest -> {
                InformationLayoutFragment
                    .newInstance(id, titleById)
                    .show(childFragmentManager, InformationLayoutFragment.TAG)
                viewModel.setUserJourney("$id ${AnalyticsDefinedParams.INFORMATIONDIALOUGE}")
            }

            Amoxicillin.lowercase(), ZincDispensedStatus, ACT.lowercase(), OrsDispensedStatus, JellyWaterDispensedStatus, SssDispensedStatus -> {
                RecommendedDosageFragment
                    .newInstance(id, titleById)
                    .show(childFragmentManager, RecommendedDosageFragment.TAG)
                viewModel.setUserJourney("$id ${AnalyticsDefinedParams.RecommendedDosage}")
            }

            BreathPerMinute -> {
                FastBreathingLayoutFragment.newInstance().show(childFragmentManager, FastBreathingLayoutFragment.TAG)
                viewModel.setUserJourney(AnalyticsDefinedParams.FastBreathingINFO)
            }
        }
    }

    private fun getTitleById(id: String): String =
        when (id) {
            muacCode -> getString(R.string.measuring_muac)
            hasOedemaOfBothFeet -> getString(R.string.checking_for_oedema)
            chestInDrawing -> getString(R.string.chest_in_drawing)
            isUnusualSleepy -> getString(R.string.job_aid_unconscious_unusually_sleepy)
            isConvulsionPastFewDays -> getString(R.string.job_aid_convulsions)
            isVomiting -> getString(R.string.job_aid_vomiting)
            isBreastfeed -> getString(R.string.job_aid_not_able_to_breastfeed_drink)
            rdtTest -> getString(R.string.job_aid_reading_result)
            else -> {
                id
            }
        }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        val dob = viewModel.memberDetailsLiveData.value
            ?.data
            ?.dateOfBirth

        resultMap?.let { map ->
            var noOfDaysOfFever = if (map.containsKey(NoOfDaysOfFever)) {
                map[NoOfDaysOfFever] as? Int
            } else {
                null
            }

            var numberOfDaysDiarrhoea = if (map.containsKey(NoOfDaysDiarrhoea)) {
                map[NoOfDaysDiarrhoea] as? Int
            } else {
                null
            }

            var noOfDaysOfCough = if (map.containsKey(NoOfDaysOfCough)) {
                map[NoOfDaysOfCough] as? Int
            } else {
                null
            }
            var isFeverDaysValid: Boolean? = null
            var isCoughDaysValid: Boolean? = null
            var isDiarrhoeaDaysValid: Boolean? = null
            if (!dob.isNullOrEmpty()) {
                if (noOfDaysOfFever != null) {
                    isFeverDaysValid = noOfDaysOfFever.let {
                        CommonUtils.isDateHigherThanInput(dob, it)
                    }
                }
                if (noOfDaysOfCough != null) {
                    isCoughDaysValid = noOfDaysOfCough.let {
                        CommonUtils.isDateHigherThanInput(dob, it)
                    }
                }
                if (numberOfDaysDiarrhoea != null) {
                    isDiarrhoeaDaysValid = numberOfDaysDiarrhoea.let {
                        CommonUtils.isDateHigherThanInput(dob, it)
                    }
                }
            }

            if (isFeverDaysValid == null && isDiarrhoeaDaysValid == null && isCoughDaysValid == null) {
                onSubmitICCM(resultMap, serverData)
            } else {
                val feverValid = isFeverDaysValid ?: true
                val coughValid = isCoughDaysValid ?: true
                val diarrhoeaValid = isDiarrhoeaDaysValid ?: true
                if (!feverValid) {
                    formGenerator.getViewByTag(NoOfDaysOfFever + errorSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                }
                if (!diarrhoeaValid) {
                    formGenerator.getViewByTag(NoOfDaysDiarrhoea + errorSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                }
                if (!coughValid) {
                    formGenerator.getViewByTag(NoOfDaysOfCough + errorSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                }
                if (feverValid && coughValid && diarrhoeaValid) {
                    onSubmitICCM(resultMap, serverData)
                }
            }
        }
    }

    private fun onSubmitICCM(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        if (viewModel.isDangerSignFlow) {
            resultMap?.keys?.retainAll(listOf(isUnusualSleepy, isConvulsionPastFewDays, isBreastfeed, isVomiting))
            resultMap?.filterValues { it == true }?.keys?.forEach { key ->
                viewModel.dangerSingsKey = key
            }
        }
        resultMap?.let { details ->
            composeICCMOtherMetrics(details)
            val referralResult = ReferralResultGenerator().calculateIccmReferralResult(details, viewModel.memberDetailsLiveData.value?.data)
            val result = serverData?.let {
                FormResultComposer().groupValues(
                    serverData = it,
                    details,
                    ICCM_MENU_ID,
                )
            }

            result?.second?.let {
                viewModel.saveAssessment(serverData, it, referralResult, viewModel.menuId)
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.ICCMAssessment,
            eventName = AnalyticsDefinedParams.AssessmentCreation,
        )
    }

    private fun composeICCMOtherMetrics(resultMap: HashMap<String, Any>) {
        if (resultMap.containsKey(muacCode) && resultMap[muacCode] is String) {
            getNutritionStatus(resultMap[muacCode] as String, requireContext()).let { value ->
                if (value != "-") {
                    viewModel.otherAssessmentDetails[MalnutritionCondition] = value
                }
            }
        }
        if (resultMap.containsKey(BreathPerMinute) && resultMap[BreathPerMinute] is Int) {
            (resultMap[BreathPerMinute] as Int).let { bpmValue ->
                viewModel.memberDetailsLiveData.value?.data?.let { details ->
                    DateUtils.dateToMonths(details.dateOfBirth).let { month ->
                        month?.let {
                            if ((month in FB_MIN_MONTH..11) && bpmValue >= FB_MAX_BREATHING) {
                                viewModel.otherAssessmentDetails[CoughCondition] =
                                    ReferralReasons.Pneumonia.name
                            } else if (month in FB_MAX_MONTH..60 && bpmValue >= FB_MIN_BREATHING) {
                                viewModel.otherAssessmentDetails[CoughCondition] =
                                    ReferralReasons.Pneumonia.name
                            }
                        }
                    }
                }
            }
        }
        if (resultMap.containsKey(RdtTest) && resultMap[RdtTest] is String) {
            if (resultMap[RdtTest] == RdtPositive) {
                viewModel.otherAssessmentDetails[FeverCondition] =
                    ReferralReasons.Malaria.name
            }
        }
        if (resultMap.containsKey(hasDiarrhoea) && resultMap[hasDiarrhoea] is Boolean && resultMap[hasDiarrhoea] as Boolean) {
            if ((resultMap.containsKey(NoOfDaysDiarrhoea) && resultMap[NoOfDaysDiarrhoea] is Int && ((resultMap[NoOfDaysDiarrhoea] as Int) >= 14)) ||
                (resultMap.containsKey(IsBloodyDiarrhoea) && resultMap[IsBloodyDiarrhoea] is Boolean && resultMap[IsBloodyDiarrhoea] as Boolean)
            ) {
                viewModel.otherAssessmentDetails[DiarrheaCondition] = SevereDehydration
            } else {
                viewModel.otherAssessmentDetails[DiarrheaCondition] = ModerateDehydration
            }
        }
    }

    override fun onRenderingComplete() {
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        // Updating ORS Visibility based on muac
        updateOrsConditionBasedOnMuac(id, selectedId)
        when (id) {
            muacCode -> {
                if (selectedId is String && selectedId != DEFAULT_ID) {
                    formGenerator.getViewByTag(muacStatus + rootSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                    formGenerator.getViewByTag(muacStatus + summaryKey)?.let {
                        if (it is TextView) {
                            it.text = requireContext().getString(
                                R.string.firstname_lastname,
                                MUAC.uppercase(),
                                selectedId,
                            )
                        }
                    }
                    formGenerator.getViewByTag(muacStatus)?.let {
                        if (it is TextView) {
                            it.text = getNutritionStatus(selectedId, requireContext())
                        }
                    }
                    formGenerator.getViewByTag(muacCode)?.apply {
                        val background = background as? GradientDrawable
                        background?.setStroke(
                            resources.getDimensionPixelSize(R.dimen._4sdp),
                            getMuacColorCode(
                                selectedId,
                                requireContext(),
                            ),
                        )
                    }
                } else {
                    formGenerator.getViewByTag(muacStatus + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                    formGenerator.getViewByTag(muacCode)?.apply {
                        val background = background as? GradientDrawable
                        background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), getMuacColorCode(selectedId as String, requireContext()))
                    }
                }
            }
            hasCough -> {
                viewModel.memberDetailsLiveData.value?.data?.let {
                    renderDosageDetails(it.dateOfBirth)
                }
                handleRecommendedDoseage(hasCough)
            }
            hasDiarrhoea, rdtTest -> {
                viewModel.memberDetailsLiveData.value?.data?.let {
                    renderDosageDetails(it.dateOfBirth)
                }
            }
            isUnusualSleepy, isConvulsionPastFewDays, isVomiting, isBreastfeed -> {
                viewModel.nameOfDangerSignClicked = id
                if (selectedId == true) {
                    viewModel.isDangerSignFlow = true
                    val existingFragment =
                        childFragmentManager.findFragmentByTag(
                            DangerSignsDialog.TAG,
                        )
                    if (existingFragment == null) {
                        DangerSignsDialog
                            .newInstance(id)
                            .apply { setDangerSignListener(this@AssessmentICCMFragment) }
                            .show(childFragmentManager, DangerSignsDialog.TAG)
                    }
                } else {
                    viewModel.isDangerSignFlow = false
                }
            }
        }
    }

    private fun handleRecommendedDoseage(id: String) {
        viewModel.memberDetailsLiveData.value?.data?.let { data ->
            getYearMonthAndWeek(data.dateOfBirth, DATE_FORMAT_yyyyMMddHHmmssZZZZZ).let { result ->
                val year = result.second.first ?: 0
                val month = result.second.second ?: 0
                if ((year == 0 && month in 0..2)) {
                    displayDaysInformation(id, View.INVISIBLE)
                    formGenerator.getViewByTag((Amoxicillin.lowercase()) + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                    formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun getZincSuffixText(age: Int?): CharSequence? =
        when (age) {
            in 2..6 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days_string, "1/2", 10)
            }
            in 7..60 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 1, 10)
            }
            else -> {
                null
            }
        }

    private fun getSuffixText(age: Int?): String? =
        when (age) {
            in 2..12 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 2, 5)
            }
            in 12..36 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 4, 5)
            }
            in 36..60 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 6, 5)
            }
            else -> {
                null
            }
        }

    private fun getACTSuffixText(age: Int?): String =
        when (age) {
            in 6..36 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 2, 3)
            }
            in 37..96 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 4, 3)
            }
            in 97..168 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 6, 3)
            }
            else -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 8, 3)
            }
        }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
        when (id) {
            BreathPerMinute -> {
                enteredDays?.let {
                    viewModel.memberDetailsLiveData.value?.data?.let { data ->
                        getYearMonthAndWeek(data.dateOfBirth, DATE_FORMAT_yyyyMMddHHmmssZZZZZ).let { result ->
                            val year = result.second.first ?: 0
                            val month = result.second.second ?: 0
                            if ((year == 0 && month > FB_MIN_MONTH && month < FB_MAX_MONTH) && enteredDays >= FB_MAX_BREATHING) {
                                displayDaysInformation(id, View.VISIBLE)
                                getAmoxicillinStatus()
                            } else if ((month == FB_MAX_MONTH || year in FB_MIN_YEAR..FB_MAX_YEAR) && enteredDays >= FB_MIN_BREATHING) {
                                displayDaysInformation(id, View.VISIBLE)
                                getAmoxicillinStatus()
                            } else {
                                displayDaysInformation(id, View.INVISIBLE)
                                dismissAmoxicillinStatus(resultMap)
                            }
                        }
                    }
                } ?: kotlin.run {
                    displayDaysInformation(id, View.INVISIBLE)
                }
            }

            NoOfDaysOfCough -> {
                onDaysValidation(id)
                showDaysLimitation(enteredDays, noOfDays, id)
            }

            NoOfDaysDiarrhoea -> {
                onDaysValidation(id)
                showDaysLimitation(enteredDays, noOfDays, id)
            }

            NoOfDaysOfFever -> {
                onDaysValidation(id)
                showDaysLimitation(enteredDays, noOfDays, id)
            }

            else -> {
                showDaysLimitation(enteredDays, noOfDays, id)
            }
        }
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
        if (formLayout?.id == muacCode) {
            viewModel.selectedMemberDob?.let { dateOfBirth ->
                val visibility = isMandateOrNot(dateOfBirth)
                formLayout.visibility = visibility
                formLayout.isMandatory = (visibility == VISIBLE)
                formLayout.isSummary = (visibility == VISIBLE)
            }
        }

        if (formLayout?.id == muacStatus) {
            viewModel.selectedMemberDob?.let { dateOfBirth ->
                formLayout.visibility = isMandateOrNot(dateOfBirth)
            }
        }
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
        /*
       Never used
         */
    }

    override fun onQRScanRequested() {
    }

    private fun dismissAmoxicillinStatus(resultMap: HashMap<String, Any>?) {
        formGenerator.getViewByTag((Amoxicillin.lowercase()) + rootSuffix)?.apply {
            visibility = View.GONE
            resultMap?.let { map ->
                if (map.containsKey(Amoxicillin.lowercase())) {
                    map.remove(Amoxicillin.lowercase())
                    formGenerator.resetSingleSelection(Amoxicillin.lowercase())
                }
            }
        }
        formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
            visibility = View.GONE
        }
    }

    private fun getAmoxicillinStatus() {
        formGenerator.getViewByTag((Amoxicillin.lowercase()) + rootSuffix)?.apply {
            visibility = View.VISIBLE
        }
        formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
            visibility = View.VISIBLE
        }
    }

    private fun updateColorCode(
        id: String,
        colorCode: Int,
    ) {
        formGenerator.getViewByTag(id + INFORMATION)?.let { view ->
            if (view is TextView) {
                view.setTextColor(colorCode)
            }
        }
    }

    private fun displayDaysInformation(
        id: String,
        viewVisibility: Int,
    ) {
        formGenerator.getViewByTag(id + INFORMATION)?.apply { visibility = viewVisibility }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                withLocationCheck({
                    viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                    viewModel.fetchCurrentLocation(requireContext())
                    formGenerator.formSubmitAction(view)
                })
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val dosageDialog = childFragmentManager.findFragmentByTag("RecommendedDosageFragment") as? DialogFragment
        val instructionDialog = childFragmentManager.findFragmentByTag("InformationLayoutFragment") as? DialogFragment
        val fastBreathingDialog = childFragmentManager.findFragmentByTag("FastBreathingLayoutFragment") as? DialogFragment
        if (dosageDialog != null && dosageDialog.showsDialog) {
            dosageDialog.dismiss()
            showDialogBasedOnId()
        }

        if (instructionDialog != null && instructionDialog.showsDialog) {
            instructionDialog.dismiss()
            showDialogBasedOnId()
        }

        if (fastBreathingDialog != null && fastBreathingDialog.showsDialog) {
            fastBreathingDialog.dismiss()
            showDialogBasedOnId()
        }
    }

    private fun showDialogBasedOnId() {
        viewModel.instructionId?.let {
            showInstructionDialog(it)
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    override fun onDangerSignsClicked(isClicked: Boolean) {
        if (isClicked) {
            onSubmitICCM(formGenerator.getResultMap(), formGenerator.getServerData())
        } else {
            formGenerator
                .getViewByTag(viewModel.nameOfDangerSignClicked.toString() + rootSuffix)
                ?.let { formGenerator.resetChildViews(it) }
        }
    }

    // Based on age the no of days fever/cough/diarrhoea validation
    private fun onDaysValidation(id: String) {
        formGenerator.getViewByTag(id).takeIf { it is EditText }?.let { editText ->
            val dob = viewModel.memberDetailsLiveData.value
                ?.data
                ?.dateOfBirth
            val inputText = (editText as EditText).text.toString()
            if (!dob.isNullOrEmpty() && inputText.isNotEmpty()) {
                val isValid = CommonUtils.isDateHigherThanInput(dob, inputText.toInt())
                formGenerator
                    .getViewByTag(id + errorSuffix)
                    ?.apply {
                        visibility = if (isValid) View.GONE else View.VISIBLE
                    }.takeIf { it is TextView }
                    ?.let { textView ->
                        (textView as TextView).text =
                            getString(R.string.the_day_s_should_be_less_than_age)
                    }
            }
        }
    }

    private fun showDaysLimitation(
        enteredDays: Int?,
        noOfDays: Int,
        id: String,
    ) {
        if (enteredDays != null && enteredDays > noOfDays) {
            updateColorCode(id, ContextCompat.getColor(requireContext(), R.color.medium_high_risk_color))
            displayDaysInformation(id, View.VISIBLE)
        } else {
            updateColorCode(id, ContextCompat.getColor(requireContext(), R.color.secondary_black))
            displayDaysInformation(id, View.INVISIBLE)
        }
    }

    private fun updateOrsConditionBasedOnMuac(
        id: String,
        selectedId: Any?,
    ) {
        if (id == muacCode) {
            if (selectedId == AssessmentDefinedParams.Red) {
                viewModel.muacColor = AssessmentDefinedParams.Red
                formGenerator
                    .getViewByTag(OrsDispensedStatus + rootSuffix)
                    ?.apply {
                        visibility = View.GONE
                    }.let {
                        if (it != null) {
                            formGenerator.resetChildViews(it)
                        }
                    }
                formGenerator.getViewByTag(ORSStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }
            } else {
                viewModel.muacColor = selectedId.toString()
                if (viewModel.hasDiarrhoea) {
                    formGenerator
                        .getViewByTag(OrsDispensedStatus + rootSuffix)
                        ?.apply {
                            visibility = View.VISIBLE
                        }.let {
                            if (it != null) {
                                formGenerator.resetChildViews(it)
                            }
                        }
                    formGenerator.getViewByTag(ORSStatus + rootSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                }
            }
        }
        if (id == hasDiarrhoea && selectedId == true) {
            viewModel.hasDiarrhoea = true
            if (viewModel.muacColor == AssessmentDefinedParams.Red) {
                formGenerator
                    .getViewByTag(OrsDispensedStatus + rootSuffix)
                    ?.apply {
                        visibility = View.GONE
                    }.let {
                        if (it != null) {
                            formGenerator.resetChildViews(it)
                        }
                    }
                formGenerator.getViewByTag(ORSStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }
            } else {
                formGenerator
                    .getViewByTag(OrsDispensedStatus + rootSuffix)
                    ?.apply {
                        visibility = View.VISIBLE
                    }.let {
                        if (it != null) {
                            formGenerator.resetChildViews(it)
                        }
                    }
                formGenerator.getViewByTag(ORSStatus + rootSuffix)?.apply {
                    visibility = View.VISIBLE
                }
            }
        }
    }
}
