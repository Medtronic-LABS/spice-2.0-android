package org.medtroniclabs.uhis.ui.assessment.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateMarginsRelative
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.DONEBUTTONTRIGGERED
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ASSESSMENT_ID
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.FragmentRmnchSummaryBinding
import org.medtroniclabs.uhis.databinding.InstructionLayoutBinding
import org.medtroniclabs.uhis.formgeneration.FormSupport.translateTitle
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.extension.px
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapterCustomLayout
import org.medtroniclabs.uhis.formgeneration.utility.InformationLayoutFragment
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentActivity
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.addViewSummaryLayout
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ChildHoodVisit
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.DEATH_OF_MOTHER
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.getValueFromMap
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.cbs.activity.CbsActivity
import java.util.Calendar
import kotlin.collections.get

class AssessmentRMNCHSummaryFragment : BaseFragment(), View.OnClickListener {
    lateinit var binding: FragmentRmnchSummaryBinding

    private val viewModel: AssessmentViewModel by activityViewModels()

    private var datePickerDialog: DatePickerDialog? = null

    /**
     * Max date in case of PNC
     */
    private var maxDate: Calendar? = null

    // Variables to collect summary data for saving to PregnancyDetail

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRmnchSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListener()
        initSummaryViewByWorkFlowName()
        viewModel.setUserJourney(getUserJourneyName())
        binding.etNextFollowUpDate.background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_background)
        val background = binding.etNextFollowUpDate.background as? GradientDrawable
        background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), ContextCompat.getColor(requireContext(), R.color.edittext_stroke))
    }

    private fun getUserJourneyName(): String =
        when (viewModel.workflowName) {
            ChildHoodVisit -> {
                "${viewModel.workflowName}${AnalyticsDefinedParams.RMNCHCHILDASSESSMENTSUMMARY}"
            }

            else -> {
                "${viewModel.workflowName}${AnalyticsDefinedParams.RMNCHSummaryAssessment}"
            }
        }

    private fun setListener() {
        binding.btnDone.safeClickListener(this)
        binding.callSupervisor.safeClickListener(this)
        binding.etNextFollowUpDate.safeClickListener(this)
        binding.etNextFollowUpDate.addTextChangedListener {
            binding.btnDone.isEnabled = !it.isNullOrEmpty()
        }
    }

    private fun updateStatusBar() {
        when (viewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
                binding.riskResultLayout.text = getString(R.string.referred_for_further_assessment)
                binding.etPhuChange.visible()
                binding.labelPhuReferred.visible()
            }

            ReferralStatus.OnTreatment.name -> {
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.red_risk_moderate)
                binding.riskResultLayout.text = getString(R.string.patient_on_treatment)
                binding.etPhuChange.gone()
                binding.labelPhuReferred.gone()
            }

            else -> {
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.green_attention_color)
                // Change no referral status display in case of childhood visit
                if (viewModel.workflowName == ChildHoodVisit) {
                    binding.riskResultLayout.text = getString(R.string.no_referral_required)
                } else {
                    binding.riskResultLayout.text = getString(R.string.no_refferral_treatment_required)
                }
                binding.etPhuChange.gone()
                binding.labelPhuReferred.gone()
            }
        }
    }

    private fun initSummaryViewByWorkFlowName() {
        viewModel.assessmentStringLiveData.value?.let { mapString ->
            val map = StringConverter.stringToMap(mapString)
            binding.parentLayout.removeAllViews()
            if (viewModel.workflowName == RMNCH.ChildHoodVisit) {
                bindRmnchSummaryView(
                    getString(R.string.child_status),
                    getStatus(viewModel.referralStatus) ?: getString(R.string.seperator_hyphen),
                )
            }

            when (viewModel.workflowName) {
                RMNCH.PNC -> {
                    bindPNCSummary(map)
                }

                RMNCH.ANC -> {
                    bindAncSummary(map)
                }

                else -> {
                    bindChildHoodSummary(map)
                }
            }
        }
    }

    /**
     * Binds summary for ANC
     */
    private fun bindAncSummary(map: HashMap<String, Any>) {
        // Find the index right after resultCardView to insert gaps card & counselling card
        val resultCardIndex = binding.scrollViewLL.indexOfChild(binding.resultCardView)
        var insertIndex = if (resultCardIndex >= 0) resultCardIndex + 1 else -1

        updateStatusBar()
        binding.tvTitle.setText(R.string.assessment_result)
        binding.labelPhuReferred.setText(R.string.referral_health_facility)

        // Set follow-up date to 4 weeks (28 days) from current date
        val fourWeeksFromNow = DateUtils.getDateAfterDays(28)
        binding.etNextFollowUpDate.text = fourWeeksFromNow
        updateFollowUpDate(fourWeeksFromNow)

        // Load Referral Facility spinner options from JSON
        loadReferralFacilityOptions()

        // For ANC workflow, render Result section with only highRiskPregnantWoman and gapsInAnc
        // (Counselling fields will be rendered separately after this block)
        // First, read values to check if referral facility should be shown
        val ancMap = map[RMNCH.ANC] as? Map<*, *>
        val summaryGroup = ancMap?.get(AssessmentDefinedParams.GROUP_SUMMARY) as? Map<*, *>
        val highRiskList = (summaryGroup?.get(AssessmentDefinedParams.HIGH_RISK_PREGNANT_WOMAN) as? Map<*, *>)
        var riskText = ""
        if (!highRiskList.isNullOrEmpty()) {
            val urgentReferralList = highRiskList[RMNCH.AncPncReferralType.URGENT.name] as? List<String>
            val nonUrgentReferralList = highRiskList[RMNCH.AncPncReferralType.NON_URGENT.name] as? List<String>
            if (!urgentReferralList.isNullOrEmpty()) {
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    root.setPadding(0)
                    with(tvTitle) {
                        setText(R.string.emergency_referral)
                        setTypeface(null, Typeface.BOLD)
                    }
                    binding.parentLayout.addView(root)
                }
                riskText = getString(R.string.rmnch_emergency_referral_message)
                urgentReferralList.forEach { condition ->
                    with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                        root.setPadding(0)
                        with(tvTitle) {
                            setPadding(8.px)
                            text = "  • ${RMNCH.getDisplayCondition(condition, isTranslationEnabled)}" // Use bigger bullet (•)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.red_risk))
                            setBackgroundResource(R.drawable.bg_high_risk)
                        }
                        binding.parentLayout.addView(root)
                    }
                }
            }
            if (!nonUrgentReferralList.isNullOrEmpty()) {
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    root.setPadding(0)
                    with(tvTitle) {
                        setText(R.string.non_emergency_referral)
                        setTypeface(null, Typeface.BOLD)
                    }
                    binding.parentLayout.addView(root)
                }
                if (riskText.isBlank()) {
                    riskText = getString(R.string.rmnch_non_emergency_referral_message)
                }
                nonUrgentReferralList.forEach { condition ->
                    with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                        root.setPadding(0)
                        with(tvTitle) {
                            setPadding(8.px)
                            text = "  • ${RMNCH.getDisplayCondition(condition, isTranslationEnabled)}" // Use bigger bullet (•)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.low_risk_color))
                            setBackgroundResource(R.drawable.bg_low_risk)
                        }
                        binding.parentLayout.addView(root)
                    }
                }
            }
        } else {
            binding.resultCardView.gone()
        }
        val gapsList = (summaryGroup?.get(AssessmentDefinedParams.GAPS_IN_ANC) as? List<String>)
        if (!gapsList.isNullOrEmpty()) {
            val gapsCardBinding = getCounsellingCardBinding()
            gapsCardBinding.cardTitle.setText(R.string.gaps_in_anc)
            gapsList.forEach { condition ->
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    root.setPadding(0)
                    with(tvTitle) {
                        setPadding(8.px)
                        text = "  • ${RMNCH.getDisplayCondition(condition, isTranslationEnabled)}" // Use bigger bullet (•)
                        setBackgroundResource(R.drawable.bg_gaps)
                    }
                    gapsCardBinding.llFamilyRoot.addView(root)
                }
            }
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.updateMarginsRelative(start = 6.px, end = 6.px)
            // Insert gaps card
            if (insertIndex >= 0) {
                binding.scrollViewLL.addView(gapsCardBinding.root, insertIndex, layoutParams)
            } else {
                // Fallback: add to end
                binding.scrollViewLL.addView(gapsCardBinding.root, layoutParams)
            }
            insertIndex += 1
        }
        if (riskText.isNotBlank()) {
            binding.riskResultLayout.text = riskText
        }
        bindAncInstructions(ancMap, insertIndex)
    }

    /**
     * Binds instructions card for PNC
     */
    private fun bindAncInstructions(
        ancMap: Map<*, *>?,
        insertIndex: Int,
    ) {
        // Get counselling card layout info for title
        val counsellingCardLayout = viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.firstOrNull { it.id == AssessmentDefinedParams.GROUP_COUNSELLING && it.viewType == ViewType.VIEW_TYPE_FORM_CARD_FAMILY }

        // Get counselling items
        val counsellingItems = viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.filter {
                it.id == AssessmentDefinedParams.NUTRITION_COUNSELLING ||
                    it.id == AssessmentDefinedParams.CARE_DURING_ANTENATAL_PERIOD ||
                    it.id == AssessmentDefinedParams.BIRTH_PREPAREDNESS ||
                    it.id == AssessmentDefinedParams.NEW_BORN_CARE_EDUCATION ||
                    (it.viewType == ViewType.VIEW_TYPE_INSTRUCTION && it.isSummary == true && it.family == AssessmentDefinedParams.GROUP_COUNSELLING)
            }?.sortedBy { it.orderId ?: Int.MAX_VALUE }
            ?.toMutableList()

        viewModel.pregnancyDetailLiveData.value?.lastMenstrualPeriod?.let { lmp ->
            val lmpCalendar = DateUtils.getLastMenstrualDate(lmp)
            val gestationalAge = DateUtils.calculateGestationalAge(lmpCalendar)
            // Newborn care : Shown only during 3rd trimester
            if (gestationalAge.first < AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_27) {
                counsellingItems?.removeIf { it.id == AssessmentDefinedParams.NEW_BORN_CARE_EDUCATION }
            }
        }

        counsellingItems?.firstOrNull { it.id == AssessmentDefinedParams.NUTRITION_COUNSELLING }?.let { nutritionCounselling ->
            val options = nutritionCounselling.optionsList
            val instructions = arrayListOf<String>()
            val instructionsCulture = arrayListOf<String>()
            if (!options.isNullOrEmpty()) {
                val medicalHistoryPhysicalExaminationMap = ancMap?.get(AssessmentDefinedParams.GROUP_MEDICAL_HISTORY_PHYSICAL_EXAMINATION) as? Map<*, *>
                val bmi = CommonUtils.getDouble(medicalHistoryPhysicalExaminationMap?.get(AssessmentDefinedParams.BMI)).takeIf { it > 0 }
                bmi?.let {
                    if (bmi < AssessmentDefinedParams.BMI_NORMAL_WEIGHT_THRESHOLD) {
                        val option = options.firstOrNull { it[DefinedParams.id] == "underWeight" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    } else if (bmi < AssessmentDefinedParams.BMI_OVER_WEIGHT_THRESHOLD) {
                        val option = options.firstOrNull { it[DefinedParams.id] == "normalWeight" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    } else if (bmi < AssessmentDefinedParams.BMI_OBSESS_WEIGHT_THRESHOLD) {
                        val option = options.firstOrNull { it[DefinedParams.id] == "overWeight" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    } else {
                        val option = options.firstOrNull { it[DefinedParams.id] == "obsess" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    }
                }
                val pointOfCareInvestigationMap = ancMap?.get(AssessmentDefinedParams.GROUP_POINT_OF_CARE_INVESTIGATIONS) as? Map<*, *>
                val hb = CommonUtils.getDouble(pointOfCareInvestigationMap?.get(AssessmentDefinedParams.HEMOGLOBIN)).takeIf { it > 0 }
                hb?.let {
                    if (hb < AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD) {
                        val option = options.firstOrNull { it[DefinedParams.id] == "severeAnemia" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    } else if (hb < AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD) {
                        val option = options.firstOrNull { it[DefinedParams.id] == "moderateAnemia" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    } else if (hb < AssessmentDefinedParams.HEMOGLOBIN_MILD_ANEMIA_THRESHOLD) {
                        val option = options.firstOrNull { it[DefinedParams.id] == "mildAnemia" }
                        option?.let {
                            instructions.add(option[DefinedParams.NAME] as? String ?: "")
                            instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                        }
                    } else {
                        // Do nothing
                    }
                }
                val fastingSugar = CommonUtils.getDouble(pointOfCareInvestigationMap?.get(AssessmentDefinedParams.BLOOD_SUGAR_FASTING)).takeIf { it > 0 }
                val randomSugar = CommonUtils.getDouble(pointOfCareInvestigationMap?.get(AssessmentDefinedParams.BLOOD_SUGAR_RANDOM)).takeIf { it > 0 }
                if ((fastingSugar != null && fastingSugar < AssessmentDefinedParams.LOW_SUGAR_THRESHOLD) ||
                    (randomSugar != null && randomSugar < AssessmentDefinedParams.LOW_SUGAR_THRESHOLD)
                ) {
                    val option = options.firstOrNull { it[DefinedParams.id] == "lowSugar" }
                    option?.let {
                        instructions.add(option[DefinedParams.NAME] as? String ?: "")
                        instructionsCulture.add(option[DefinedParams.CULTURE_VALUE] as? String ?: "")
                    }
                }
            }
            if (instructions.isEmpty() || instructionsCulture.isEmpty()) {
                counsellingItems.removeIf { it.id == AssessmentDefinedParams.NUTRITION_COUNSELLING }
            } else {
                nutritionCounselling.instructions = instructions
                nutritionCounselling.instructionsCulture = instructionsCulture
            }
        }

        // Only create counselling card if there are items to display
        if (!counsellingItems.isNullOrEmpty()) {
            // Create Counselling CardView using CardLayoutBinding
            val counsellingCardBinding = getCounsellingCardBinding()

            // Set card title
            counsellingCardLayout?.let { cardLayout ->
                val isTranslationEnabled = SecuredPreference
                    .getIsTranslationEnabled()
                val cardTitle = if (isTranslationEnabled && !cardLayout.titleCulture.isNullOrBlank()) {
                    cardLayout.titleCulture ?: cardLayout.title
                } else {
                    cardLayout.title
                }
                counsellingCardBinding.cardTitle.text = cardTitle
            } ?: run {
                // Fallback title if cardLayout not found
                counsellingCardBinding.cardTitle.text = "Counselling"
            }

            // Add counselling items to the card's content layout
            counsellingItems.forEach { data ->
                addInstructionsCard(data, counsellingCardBinding.llFamilyRoot)
            }

            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.updateMarginsRelative(start = 6.px, end = 6.px)
            // Insert counselling card
            if (insertIndex >= 0) {
                binding.scrollViewLL.addView(counsellingCardBinding.root, insertIndex, layoutParams)
            } else {
                // Fallback: add to end
                binding.scrollViewLL.addView(counsellingCardBinding.root, layoutParams)
            }
        }
    }

    /**
     * Creates instruction layout and adds to the parent [llLayout]]
     */
    private fun addInstructionsCard(
        data: FormLayout,
        llLayout: LinearLayout,
    ) {
        with(data) {
            val instructionBinding = InstructionLayoutBinding.inflate(LayoutInflater.from(requireContext()))
            instructionBinding.root.setBackgroundResource(R.drawable.bg_red_risk_orange)
            instructionBinding.tvTitle.text = translateTitle(titleCulture, title, isTranslationEnabled)
            instructionBinding.tvTitle.visibility = View.VISIBLE
            instructionBinding.tvTitle.setTextColor(Color.BLACK)

            instructionBinding.clInstructionRoot.safeClickListener {
                // Use instructionsCulture if translation is enabled and available, otherwise use instructions
                val instructionsList = if (isTranslationEnabled && !instructionsCulture.isNullOrEmpty()) {
                    instructionsCulture
                } else {
                    instructions
                }
                if (!instructionsList.isNullOrEmpty()) {
                    InformationLayoutFragment
                        .newInstance(id, translateTitle(titleCulture, title, isTranslationEnabled), customInformationList = instructionsList)
                        .show(childFragmentManager, InformationLayoutFragment.TAG)
                    viewModel.setUserJourney("$id ${AnalyticsDefinedParams.INFORMATIONDIALOUGE}")
                }
            }
            // Add to counselling card's content layout
            llLayout.addView(instructionBinding.root)
        }
    }

    /**
     * Binds summary for PNC
     */
    private fun bindPNCSummary(map: HashMap<String, Any>) {
        // Find the index right after resultCardView to insert gaps card & counselling card
        val resultCardIndex = binding.scrollViewLL.indexOfChild(binding.resultCardView)
        var insertIndex = if (resultCardIndex >= 0) resultCardIndex + 1 else -1

        updateStatusBar()
        binding.tvTitle.setText(R.string.assessment_result)
        binding.labelPhuReferred.setText(R.string.referral_health_facility)

        val pncMap = map[RMNCH.PNC] as? Map<String, Any>
        val maternalHealth = pncMap?.get(RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT) as? Map<*, *>
        val daysSinceDelivery = CommonUtils.getInteger(maternalHealth?.get(RMNCH.ID_DAYS_SINCE_DELIVERY))
        // Load Referral Facility spinner options from JSON
        loadReferralFacilityOptions()
        // Calculate follow-up visit based on days since delivery
        val nextVisitDateDiff = when (daysSinceDelivery) {
            in 0..2 -> {
                3
            }

            in 3..6 -> {
                7
            }

            in 7..13 -> {
                14
            }

            else -> {
                42
            }
        }
        val nextVisitDate = Calendar.getInstance()
        nextVisitDate.add(Calendar.DAY_OF_MONTH, nextVisitDateDiff)
        maxDate = nextVisitDate
        val followUpDate = DateUtils.formatDateToDisplayFormat(nextVisitDate.timeInMillis)
        binding.etNextFollowUpDate.text = followUpDate
        updateFollowUpDate(followUpDate ?: "")
        val highRiskList = (pncMap?.get(RMNCH.ID_MOTHER_RISKS) as? Map<*, *>)
        var riskText = ""
        if (!highRiskList.isNullOrEmpty()) {
            val urgentReferralList = highRiskList[RMNCH.AncPncReferralType.URGENT.name] as? List<String>
            val nonUrgentReferralList = highRiskList[RMNCH.AncPncReferralType.NON_URGENT.name] as? List<String>
            if (!urgentReferralList.isNullOrEmpty()) {
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    root.setPadding(0)
                    with(tvTitle) {
                        setText(R.string.emergency_referral)
                        setTypeface(null, Typeface.BOLD)
                    }
                    binding.parentLayout.addView(root)
                }
                riskText = getString(R.string.rmnch_emergency_referral_message)
                urgentReferralList.forEach { condition ->
                    with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                        root.setPadding(0)
                        with(tvTitle) {
                            setPadding(8.px)
                            text = "  • ${RMNCH.getDisplayCondition(condition, isTranslationEnabled)}" // Use bigger bullet (•)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.red_risk))
                            setBackgroundResource(R.drawable.bg_high_risk)
                        }
                        binding.parentLayout.addView(root)
                    }
                }
            }
            if (!nonUrgentReferralList.isNullOrEmpty()) {
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    root.setPadding(0)
                    with(tvTitle) {
                        setText(R.string.non_emergency_referral)
                        setTypeface(null, Typeface.BOLD)
                    }
                    binding.parentLayout.addView(root)
                }
                if (riskText.isBlank()) {
                    riskText = getString(R.string.rmnch_non_emergency_referral_message)
                }
                nonUrgentReferralList.forEach { condition ->
                    with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                        root.setPadding(0)
                        with(tvTitle) {
                            setPadding(8.px)
                            text = "  • ${RMNCH.getDisplayCondition(condition, isTranslationEnabled)}" // Use bigger bullet (•)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.low_risk_color))
                            setBackgroundResource(R.drawable.bg_low_risk)
                        }
                        binding.parentLayout.addView(root)
                    }
                }
            }
        } else {
            binding.resultCardView.gone()
        }
        val gapsList = (pncMap?.get(RMNCH.ID_PNC_GAPS) as? List<String>)
        if (!gapsList.isNullOrEmpty()) {
            val gapsCardBinding = getCounsellingCardBinding()
            gapsCardBinding.cardTitle.setText(R.string.gaps_in_pnc)
            gapsList.forEach { condition ->
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    root.setPadding(0)
                    with(tvTitle) {
                        setPadding(8.px)
                        text = "  • ${RMNCH.getDisplayCondition(condition, isTranslationEnabled)}" // Use bigger bullet (•)
                        setBackgroundResource(R.drawable.bg_gaps)
                    }
                    gapsCardBinding.llFamilyRoot.addView(root)
                }
            }
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.updateMarginsRelative(start = 6.px, end = 6.px)
            // Insert gaps card
            if (insertIndex >= 0) {
                binding.scrollViewLL.addView(gapsCardBinding.root, insertIndex, layoutParams)
            } else {
                // Fallback: add to end
                binding.scrollViewLL.addView(gapsCardBinding.root, layoutParams)
            }
            insertIndex += 1
        }
        if (riskText.isNotBlank()) {
            binding.riskResultLayout.text = riskText
        }
        bindPNCInstructions(insertIndex)
    }

    /**
     * Binds instructions card for PNC
     */
    private fun bindPNCInstructions(insertIndex: Int) {
        val formLayouts = viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout ?: return
        val isTranslationEnabled = SecuredPreference
            .getIsTranslationEnabled()

        // Get counseling card layout info for title
        val counselingCardLayout =
            formLayouts.firstOrNull { it.id == AssessmentDefinedParams.GROUP_COUNSELLING && it.viewType == ViewType.VIEW_TYPE_FORM_CARD_FAMILY }

        // Get counseling title
        val instructionsTitle = formLayouts.firstOrNull { it.id == RMNCH.ID_COUNSELLING_MOTHER_CARE && it.viewType == ViewType.VIEW_TYPE_FORM_TEXTLABEL }

        // Get counseling items
        val counselingItems = formLayouts.filter {
            it.family == AssessmentDefinedParams.GROUP_COUNSELLING &&
                it.viewType == ViewType.VIEW_TYPE_INSTRUCTION &&
                it.isSummary == true
        }

        // Only create counseling card if there are items to display
        if (counselingItems.isNotEmpty()) {
            // Create Counselling CardView using CardLayoutBinding
            val counsellingCardBinding = getCounsellingCardBinding()

            // Set card title
            counselingCardLayout?.let { cardLayout ->
                counsellingCardBinding.cardTitle.text = translateTitle(cardLayout.titleCulture, cardLayout.title, isTranslationEnabled)
            } ?: run {
                // Fallback title if cardLayout not found
                counsellingCardBinding.cardTitle.text = getString(R.string.counseling_education)
            }

            // Add instructions title
            instructionsTitle?.let {
                with(AssessmentCommonUtils.getTextSummaryLabelLayoutBinding(context)) {
                    tvTitle.text = translateTitle(instructionsTitle.titleCulture, instructionsTitle.title, isTranslationEnabled)
                    counsellingCardBinding.llFamilyRoot.addView(root)
                }
            }

            // Add counseling items to the card's content layout
            counselingItems.forEach { data ->
                addInstructionsCard(data, counsellingCardBinding.llFamilyRoot)
            }

            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.updateMarginsRelative(start = 6.px, end = 6.px)
            // Insert counseling card
            if (insertIndex >= 0) {
                binding.scrollViewLL.addView(counsellingCardBinding.root, insertIndex, layoutParams)
            } else {
                // Fallback: add to end
                binding.scrollViewLL.addView(counsellingCardBinding.root, layoutParams)
            }
        }
    }

    /**
     * Gets referral facilities from form layout response
     */
    private fun loadReferralFacilityOptions() {
        val referralFacilityField = viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.find { it.id == AssessmentDefinedParams.REFERRAL_FACILITY }

        val optionsList = referralFacilityField?.optionsList ?: return

        // Convert optionsList to the format expected by the spinner
        val siteList = ArrayList<Map<String, Any>>()
        optionsList.forEach { option ->
            val optionMap = HashMap<String, Any>()
            optionMap[DefinedParams.id] = option[DefinedParams.id]?.toString() ?: ""
            optionMap[DefinedParams.NAME] = option[DefinedParams.NAME]?.toString() ?: ""
            optionMap[DefinedParams.CULTURE_VALUE] = option[DefinedParams.CULTURE_VALUE]?.toString() ?: ""
            siteList.add(optionMap)
        }

        binding.etPhuChange.background = ContextCompat.getDrawable(requireContext(), R.drawable.edittext_background)
        val background = binding.etPhuChange.background as? GradientDrawable
        background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), ContextCompat.getColor(requireContext(), R.color.edittext_stroke))
        val adapter = CustomSpinnerAdapterCustomLayout(requireContext(), isTranslationEnabled)
        adapter.setData(siteList)
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
                        // Store the selected referral facility ID
                        viewModel.otherAssessmentDetails[AssessmentDefinedParams.ReferredPHUSiteID] =
                            selectedId ?: ""
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    /**
     * @return Returns counselling card layout binding
     */
    fun getCounsellingCardBinding(): CardLayoutBinding {
        val counsellingCardBinding = CardLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        counsellingCardBinding.llFamilyRoot.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        counsellingCardBinding.llFamilyRoot.setPadding(12.px)
        counsellingCardBinding.llFamilyRoot.dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider_transparent_12)
        return counsellingCardBinding
    }

    /**
     * Binds summary for child-hood visit
     */
    private fun bindChildHoodSummary(map: HashMap<String, Any>) {
        val pncChildMap = map[ChildHoodVisit] as Map<*, *>
        updateStatusBar()
        // Hide referral
        binding.etPhuChange.gone()
        binding.labelPhuReferred.gone()
        // Hide next follow-up date
        binding.tvNextFollowupDateTitle.gone()
        binding.etNextFollowUpDate.gone()

        bindRmnchSummaryView(
            getString(R.string.child_hood_visit),
            getValueFromMap(
                map,
                RMNCH.visitNo,
                ViewType.VIEW_TYPE_FORM_EDITTEXT,
                viewModel.workflowName,
                false,
                Triple(
                    getString(R.string.yes),
                    getString(R.string.no),
                    getString(R.string.hyphen_symbol),
                ),
                requireContext(),
                isTranslationEnabled,
            ),
        )
        if (pncChildMap.containsKey(AssessmentDefinedParams.ID_CHILD_ILLNESS_TYPE)) {
            bindRmnchSummaryView(
                getString(R.string.referral_reason),
                getValueFromMap(
                    map,
                    AssessmentDefinedParams.ID_CHILD_ILLNESS_TYPE,
                    ViewType.VIEW_TYPE_DIALOG_CHECKBOX,
                    viewModel.workflowName,
                    false,
                    Triple(
                        getString(R.string.yes),
                        getString(R.string.no),
                        getString(R.string.hyphen_symbol),
                    ),
                    requireContext(),
                    isTranslationEnabled,
                ),
            )
        }
        if (pncChildMap.containsKey(AssessmentDefinedParams.ID_CHILD_REFERRAL_FACILITY_TYPE)) {
            val referralFacilityId = pncChildMap[AssessmentDefinedParams.ID_CHILD_REFERRAL_FACILITY_TYPE]
            // Place of referral
            val referralFacilityForm =
                viewModel.formLayoutsLiveData.value
                    ?.data
                    ?.formLayout
                    ?.firstOrNull { it.id.equals(AssessmentDefinedParams.ID_CHILD_REFERRAL_FACILITY_TYPE, true) }
            referralFacilityForm?.let {
                val option = referralFacilityForm.optionsList?.firstOrNull { it[DefinedParams.ID] == referralFacilityId }
                bindRmnchSummaryView(
                    getString(R.string.place_of_referral),
                    (if (isTranslationEnabled) option?.get(DefinedParams.CULTURE_VALUE) else option?.get(DefinedParams.NAME)) as? String,
                )
            }
        }
        if (DefinedParams.yes.equals(pncChildMap[AssessmentDefinedParams.ID_CONGENITAL_DEFECT] as String?, true)) {
            val congenitalChoiceId = pncChildMap[AssessmentDefinedParams.ID_CONGENITAL_DEFECT]?.toString()
            val congenitalDefectForm =
                viewModel.formLayoutsLiveData.value
                    ?.data
                    ?.formLayout
                    ?.firstOrNull { it.id.equals(AssessmentDefinedParams.ID_CONGENITAL_DEFECT, true) }
            congenitalDefectForm?.let { form ->
                val option =
                    form.optionsList?.firstOrNull {
                        it[DefinedParams.ID]?.toString().equals(congenitalChoiceId, ignoreCase = true)
                    }
                val displayValue =
                    (
                        (
                            if (isTranslationEnabled) {
                                option?.get(DefinedParams.CULTURE_VALUE)
                            } else {
                                option?.get(DefinedParams.NAME)
                            }
                        ) as? String
                    )?.takeIf { it.isNotBlank() }
                        ?: option?.get(DefinedParams.NAME)?.toString()
                        ?: congenitalChoiceId
                bindRmnchSummaryView(
                    form.getSummaryTitle(isTranslationEnabled),
                    displayValue,
                )
            }
        }
        if (pncChildMap.containsKey(AssessmentDefinedParams.ID_RECEIVED_VACCINE)) {
            val vaccinationStatus = pncChildMap[AssessmentDefinedParams.ID_RECEIVED_VACCINE]
            val vaccinationStatusDisplay = if (DefinedParams.yes.equals(vaccinationStatus as String?, true)) {
                getString(R.string.taken)
            } else {
                getString(R.string.not_taken)
            }
            bindRmnchSummaryView(
                getString(R.string.vaccination),
                vaccinationStatusDisplay,
            )
        }
        binding.btnDone.isEnabled = true
    }

    companion object {
        const val TAG: String = "AssessmentRMNCHSummaryFragment"
    }

    private fun handleDoneButtonClick() {
        viewModel.fetchCurrentLocation(requireContext())
        if (binding.etNextFollowUpDate.isVisible && binding.etNextFollowUpDate.text.isNotEmpty()) {
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
        viewModel.setUserJourney(DONEBUTTONTRIGGERED)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnDone -> {
                withLocationCheck(::handleDoneButtonClick)
            }

            R.id.callSupervisor -> {
                viewModel.workflowName?.let { startCbsActivity(it) }
            }

            binding.etNextFollowUpDate.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun startCbsActivity(workFlowName: String) {
        val intent = Intent(requireContext(), CbsActivity::class.java)
        intent.putExtra(DefinedParams.MEMBER_ID, viewModel.selectedHouseholdMemberId)
        intent.putExtra(DefinedParams.DOB, viewModel.selectedMemberDob)
        intent.putExtra(MenuConstants.WORKFLOW_NAME, workFlowName)
        viewModel.assessmentSaveLiveData.value?.data?.second?.id?.let {
            intent.putExtra(ASSESSMENT_ID, it)
        }
        if (workFlowName.equals(ChildHoodVisit, true)) {
            intent.putExtra(RMNCH.DEATH_OF_NEWBORN, true)
        } else {
            intent.putExtra(DEATH_OF_MOTHER, true)
        }
        intent.putExtra(DefinedParams.MENU_ID, DefinedParams.CBS.lowercase())
        startActivity(intent)
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
                maxDate = maxDate?.timeInMillis,
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
                updateFollowUpDate(binding.etNextFollowUpDate.text.toString())
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

    private fun bindRmnchSummaryView(
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

    fun getStatus(referralStatus: String?): String? =
        when (referralStatus) {
            ReferralStatus.Referred.name -> getString(R.string.referred)
            ReferralStatus.OnTreatment.name -> getString(R.string.on_treatment)
            ReferralStatus.Recovered.name -> getString(R.string.recovered)
            else -> {
                null
            }
        }
}
