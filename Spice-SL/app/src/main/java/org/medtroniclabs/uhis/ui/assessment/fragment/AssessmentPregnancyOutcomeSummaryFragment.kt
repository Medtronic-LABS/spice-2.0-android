package org.medtroniclabs.uhis.ui.assessment.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.FragmentAssessmentPregnancyOutcomeSummaryBinding
import org.medtroniclabs.uhis.databinding.InstructionLayoutBinding
import org.medtroniclabs.uhis.formgeneration.FormSupport.translateTitle
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.extension.px
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.InformationLayoutFragment
import org.medtroniclabs.uhis.model.AssessmentSummaryModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

/**
 * Fragment responsible for showing pregnancy outcome assessment summary details
 */
@AndroidEntryPoint
class AssessmentPregnancyOutcomeSummaryFragment : BaseFragment(), View.OnClickListener {
    lateinit var binding: FragmentAssessmentPregnancyOutcomeSummaryBinding
    private val viewModel: AssessmentViewModel by activityViewModels()
    private var currentMapData: Map<*, *>? = null

    /**
     * Fields that need to appear in summary even though isSummary is false
     * (they are also shown in the form page, so isSummary can't be true).
     * Hidden automatically when they have no value.
     */
    private val additionalSummaryFieldIds = setOf(
        AssessmentDefinedParams.TIME_OF_DEATH,
        AssessmentDefinedParams.GESTATION_MONTH_AT_DEATH,
        AssessmentDefinedParams.CAUSE_OF_DEATH,
        AssessmentDefinedParams.GESTATION_MONTH_AT_ABORTION,
        AssessmentDefinedParams.TYPE_OF_ABORTION,
        AssessmentDefinedParams.DELIVERY_OUTCOME,
        AssessmentDefinedParams.PLACE_OF_DELIVERY,
        AssessmentDefinedParams.DATE_OF_DELIVERY,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentPregnancyOutcomeSummaryBinding.inflate(
            inflater,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.PREGNANCY_OUTCOME_SUMMARY)
    }

    private fun setListeners() {
        binding.btnDone.safeClickListener(this)
    }

    private fun attachObservers() {
        // Load pregnancy details to get EDD for preterm calculation
        viewModel.getPregnancyDetailInformation()
        viewModel.pregnancyDetailLiveData.observe(viewLifecycleOwner) {
            // Re-render summary when pregnancy details are loaded to show preterm status
            viewModel.assessmentStringLiveData.value?.let { data ->
                renderSummaryData(data)
            }
        }
        viewModel.assessmentStringLiveData.value?.let {
            renderSummaryData(it)
        }
    }

    /**
     * Renders summary data
     */
    private fun renderSummaryData(data: String) {
        val convertedMap = StringConverter.stringToMap(data)
        if (convertedMap.isEmpty()) {
            showErrorInSummary()
            return
        }
        binding.emptyErrorMessage.visibility = View.GONE
        binding.parentLayout.visibility = View.VISIBLE
        binding.parentLayout.removeAllViews()

        val mapData = convertedMap[MenuConstants.PREGNANCY_OUTCOME] as? Map<*, *>
        if (mapData == null) {
            showErrorInSummary()
            return
        }

        // Store mapData for use in counselling card
        currentMapData = mapData

        // Create summary data list from form layout
        val listSummaryData = createListSummaryData(data)
        createSummaryView(listSummaryData, mapData)

        // Add counselling card
        addCounsellingCard()
    }

    private fun createListSummaryData(data: String): MutableList<AssessmentSummaryModel>? =
        viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout
            ?.filter {
                it.isSummary == true || it.id in additionalSummaryFieldIds
            }?.mapNotNull { formLayout ->
                val dataMap = data.replaceFirst(
                    MenuConstants.PREGNANCY_OUTCOME,
                    MenuConstants.PREGNANCY_OUTCOME.lowercase(),
                )

                val value = AssessmentCommonUtils.getValueOfKeyFromMap(
                    StringConverter.stringToMap(dataMap),
                    formLayout.id,
                    menuType = MenuConstants.PREGNANCY_OUTCOME,
                )

                if (!value.isNullOrBlank()) {
                    AssessmentSummaryModel(
                        title = formLayout.titleSummary ?: formLayout.title,
                        id = formLayout.id,
                        cultureValue = formLayout.titleCulture,
                        value = value,
                    )
                } else {
                    null
                }
            }?.toMutableList()

    private fun createSummaryView(
        listSummaryData: MutableList<AssessmentSummaryModel>?,
        mapData: Map<*, *>,
    ) {
        listSummaryData?.let { summaryData ->
            binding.emptyErrorMessage.visibility = View.GONE
            binding.parentLayout.visibility = View.VISIBLE
            binding.parentLayout.removeAllViews()
            composeSummaryView(summaryData)

            // Add newborn details sections
            addNewbornDetailsSummary(mapData)
        } ?: kotlin.run {
            showErrorInSummary()
        }
    }

    private fun composeSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>) {
        listSummaryData.forEach { item ->
            val isOldCounselling = item.id == AssessmentDefinedParams.COUNSELLING_ABORTION ||
                item.id == AssessmentDefinedParams.COUNSELLING_STILL_BIRTH ||
                item.id == AssessmentDefinedParams.COUNSELLING_NEONATAL_DEATH
            val isNewCounselling = item.id == AssessmentDefinedParams.COUNSELLING_EMOTIONAL_SUPPORT ||
                item.id == AssessmentDefinedParams.COUNSELLING_FUTURE_PREGNANCY_PLANNING
            val isAdditionalField = item.id in additionalSummaryFieldIds
            val displayTitle = if (isTranslationEnabled) item.cultureValue ?: item.title else item.title

            when {
                isOldCounselling || isNewCounselling -> {
                    // Skip counselling fields - they will be shown in separate card
                }

                isAdditionalField -> {
                    // Additional fields: hide if no value
                    val value = item.value
                    if (!value.isNullOrBlank()) {
                        val displayValue = if (item.id == AssessmentDefinedParams.DATE_OF_DELIVERY) {
                            val formattedDate = DateUtils.convertDateFormat(
                                value,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy,
                            )
                            // Check if preterm birth and add highlighting
                            checkAndDisplayPretermBirth(formattedDate, value)
                        } else {
                            // Convert ID to display name for SingleSelectionView/Spinner fields
                            getSpinnerDisplayValue(item.id.toString(), value, isTranslationEnabled) ?: value
                        }
                        if (item.id != AssessmentDefinedParams.DATE_OF_DELIVERY) {
                            bindSummaryView(displayTitle, displayValue.toString())
                        }
                    }
                }

                else -> {
                    // Other isSummary fields: show normally
                    // Convert ID to display name for SingleSelectionView/Spinner fields
                    val displayValue = getSpinnerDisplayValue(item.id.toString(), item.value, isTranslationEnabled) ?: item.value
                    bindSummaryView(displayTitle, displayValue)
                }
            }
        }
    }

    /**
     * Converts spinner/SingleSelectionView field ID to display name using optionsList from formLayout
     */
    private fun getSpinnerDisplayValue(
        fieldId: String,
        valueId: String?,
        isTranslationEnabled: Boolean,
    ): String? {
        if (valueId.isNullOrBlank()) return null

        val formLayout = viewModel.formLayoutsLiveData.value?.data?.formLayout?.find {
            it.id == fieldId
        } ?: return null

        val optionsList = formLayout.optionsList ?: return null
        optionsList.forEach { option ->
            val optionId = option[DefinedParams.ID] as? String
                ?: option[DefinedParams.id] as? String
            if (optionId == valueId) {
                // Return cultureValue if translation is enabled, otherwise return name
                return if (isTranslationEnabled) {
                    option[DefinedParams.CULTURE_VALUE] as? String
                        ?: option[DefinedParams.NAME] as? String
                } else {
                    option[DefinedParams.NAME] as? String
                }
            }
        }

        return null
    }

    /**
     * Adds newborn details summary for each baby
     */
    private fun addNewbornDetailsSummary(mapData: Map<*, *>) {
        // newbornDetails could be at the top level of the pregnancy outcome map
        // or nested inside a sub-map
        val newbornDetailsList = findNewbornDetails(mapData) ?: return

        if (newbornDetailsList.isEmpty()) return

        newbornDetailsList.forEachIndexed { index, babyData ->
            if (babyData is Map<*, *>) {
                // Add baby heading
                addBabyHeading(index + 1)

                // Is baby alive
                val isBabyAlive = babyData[AssessmentDefinedParams.IS_BABY_ALIVE]
                if (isBabyAlive != null) {
                    val idForLabel = when (isBabyAlive) {
                        is Boolean -> if (isBabyAlive) AssessmentDefinedParams.YES else AssessmentDefinedParams.NO
                        else -> isBabyAlive.toString().trim()
                    }
                    val displayValue = AssessmentDefinedParams.pregnancyOutcomeOptionDisplayLabel(
                        AssessmentDefinedParams.pregnancyOutcomeBabyAliveOptions,
                        idForLabel,
                        isTranslationEnabled,
                    ) ?: idForLabel
                    bindSummaryView(getString(R.string.is_baby_alive), displayValue)
                }

                // Sex
                val sex = babyData[AssessmentDefinedParams.SEX]
                if (sex != null) {
                    val raw = sex.toString().trim()
                    val displaySex = AssessmentDefinedParams.pregnancyOutcomeOptionDisplayLabel(
                        AssessmentDefinedParams.pregnancyOutcomeNewbornSexOptions,
                        raw,
                        isTranslationEnabled,
                    ) ?: raw
                    bindSummaryView(getString(R.string.sex_label), displaySex)
                }

                // Birth weight - Removed (no longer displayed)

                // Cause of neonatal death
                val causeOfDeath = babyData[AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH]
                if (causeOfDeath != null) {
                    val displayValue = formatCauseOfDeath(causeOfDeath)
                    if (displayValue.isNotBlank()) {
                        bindSummaryView(getString(R.string.cause_of_neonatal_death), displayValue)
                    }
                }
            }
        }
    }

    private fun findNewbornDetails(mapData: Map<*, *>): List<*>? {
        // Check direct key
        val directList = mapData[AssessmentDefinedParams.NEWBORN_DETAILS]
        if (directList is List<*>) return directList

        // Check in nested maps (family groups)
        for (entry in mapData.entries) {
            if (entry.value is Map<*, *>) {
                val nestedMap = entry.value as Map<*, *>
                val nestedList = nestedMap[AssessmentDefinedParams.NEWBORN_DETAILS]
                if (nestedList is List<*>) return nestedList
            }
        }
        return null
    }

    private fun formatCauseOfDeath(value: Any?): String {
        if (value is List<*>) {
            val names = value.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        // Use cultureValue if translation is enabled, otherwise use name
                        if (isTranslationEnabled) {
                            item[DefinedParams.CULTURE_VALUE]?.toString()
                                ?: item[DefinedParams.NAME]?.toString()
                                ?: item["symptom"]?.toString()
                        } else {
                            item[DefinedParams.NAME]?.toString()
                                ?: item["symptom"]?.toString()
                        }
                    }

                    is String -> neonatalDeathCauseDisplayLabel(item)
                    else -> null
                }
            }
            return names.joinToString(", ")
        }
        if (value is String && value.isNotBlank()) {
            return neonatalDeathCauseDisplayLabel(value)
        }
        return value?.toString() ?: ""
    }

    private fun neonatalDeathCauseDisplayLabel(id: String): String =
        AssessmentDefinedParams.pregnancyOutcomeOptionDisplayLabel(
            RMNCH.neonatalDeathCauseOptions,
            id.trim(),
            isTranslationEnabled,
        ) ?: id.trim()

    /**
     * Checks if delivery is preterm (<37 weeks from EDD) and displays with highlighting
     */
    private fun checkAndDisplayPretermBirth(
        formattedDate: String,
        dateOfDelivery: String,
    ) {
        val pregnancyDetail = viewModel.pregnancyDetailLiveData.value
        val edd = pregnancyDetail?.estimatedDeliveryDate

        // Build value with preterm status if applicable
        val displayValue = if (!edd.isNullOrBlank() && isPretermDelivery(dateOfDelivery, edd)) {
            // Format: "[date]  (Preterm Birth)" where "(Preterm Birth)" is in RED
            buildSpannedString {
                append(formattedDate)
                append("  ")
                color(Color.RED) {
                    append("(${getString(R.string.preterm_birth)})")
                }
            }
        } else {
            formattedDate
        }

        // Display date of delivery with preterm status in value
        bindSummaryView(getString(R.string.date_of_delivery), displayValue)
    }

    /**
     * Checks if delivery is preterm (<37 weeks from EDD)
     */
    private fun isPretermDelivery(
        dateOfDelivery: String,
        edd: String?,
    ): Boolean {
        if (edd.isNullOrBlank()) {
            return false // Can't determine preterm without EDD
        }

        try {
            // Calculate gestational age at delivery
            val gestationalWeeks = calculateGestationalAgeAtDelivery(dateOfDelivery, edd)

            // Preterm is < 37 weeks
            return gestationalWeeks != null && gestationalWeeks < 37
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Calculates gestational age in weeks at delivery based on EDD
     * Returns weeks or null if calculation fails
     */
    private fun calculateGestationalAgeAtDelivery(
        dateOfDelivery: String,
        edd: String,
    ): Int? {
        try {
            // Parse dates - both are in yyyy-MM-dd'T'HH:mm:ssZZZZZ format
            // Convert to yyyyMMdd format for calculation
            val deliveryDateStr = DateUtils.convertDateFormat(
                dateOfDelivery,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_yyyyMMdd,
            )
            val eddDateStr = DateUtils.convertDateFormat(
                edd,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_yyyyMMdd,
            )

            if (deliveryDateStr.isBlank() || eddDateStr.isBlank()) {
                return null
            }

            // Get time in milliseconds
            val deliveryMillis = DateUtils.getCalendarFromString(
                deliveryDateStr,
                DateUtils.DATE_FORMAT_yyyyMMdd,
            )
            val eddMillis = DateUtils.getCalendarFromString(
                eddDateStr,
                DateUtils.DATE_FORMAT_yyyyMMdd,
            )

            if (deliveryMillis == null || eddMillis == null) {
                return null
            }

            // Calculate difference in days
            val diffInMillis = eddMillis - deliveryMillis
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

            // Calculate gestational age at delivery
            // EDD is typically at 40 weeks from LMP
            // If delivery is before EDD, gestational age = 40 - (days before EDD / 7)
            // If delivery is after EDD, gestational age = 40 + (days after EDD / 7)
            val weeksFromEDD = (diffInDays / 7.0).toInt()
            val gestationalWeeks = 40 - weeksFromEDD

            return gestationalWeeks
        } catch (e: Exception) {
            return null
        }
    }

    private fun addBabyHeading(babyNumber: Int) {
        val headingView = TextView(requireContext()).apply {
            text = getString(R.string.baby_label, babyNumber)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(requireContext().getColor(R.color.navy_blue))
            setPadding(
                resources.getDimensionPixelSize(R.dimen._16sdp),
                resources.getDimensionPixelSize(R.dimen._12sdp),
                resources.getDimensionPixelSize(R.dimen._16sdp),
                resources.getDimensionPixelSize(R.dimen._4sdp),
            )
        }
        binding.parentLayout.addView(headingView)
    }

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }

    private fun bindSummaryView(
        title: String?,
        value: CharSequence?,
        valueTextColor: Int? = null,
    ) {
        value?.let { result ->
            // If value is a SpannableString/Spanned, don't pass valueTextColor to avoid overriding colors
            val summaryLayout = if (result is android.text.SpannableString || result is android.text.Spanned) {
                AssessmentCommonUtils
                    .addViewSummaryLayout(
                        title,
                        result.toString(),
                        null, // Don't set valueTextColor for SpannableString
                        requireContext(),
                    ).apply {
                        // Set the SpannableString directly to preserve formatting
                        val summaryBinding = org.medtroniclabs.uhis.databinding.AssessmentSummaryLayoutBinding
                            .bind(this)
                        summaryBinding.tvValue.text = result
                    }
            } else {
                AssessmentCommonUtils.addViewSummaryLayout(
                    title,
                    result.toString(),
                    valueTextColor,
                    requireContext(),
                )
            }

            binding.parentLayout.addView(summaryLayout)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                withLocationCheck({
                    viewModel.fetchCurrentLocation(requireContext())
                    viewModel.updatePregnantWomanAssessmentDetails()
                    viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
                })
            }
        }
    }

    companion object {
        const val TAG = "AssessmentPregnancyOutcomeSummaryFragment"
    }

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()

    /**
     * Checks if counselling should be shown based on pregnancy outcome conditions
     * Returns true if abortion, stillbirth, or baby death occurred
     */
    private fun shouldShowCounselling(mapData: Map<*, *>?): Boolean {
        if (mapData == null) return false

        // Check for abortion
        val abortionMap = mapData[AssessmentDefinedParams.ID_ABORTION] as? Map<*, *>
        abortionMap?.let { abortion ->
            val gestationMonth = abortion[AssessmentDefinedParams.GESTATION_MONTH_AT_ABORTION]
            val typeOfAbortion = abortion[AssessmentDefinedParams.TYPE_OF_ABORTION]
            if ((gestationMonth != null && gestationMonth.toString().isNotBlank()) ||
                (typeOfAbortion != null && typeOfAbortion.toString().isNotBlank())
            ) {
                return true
            }
        }

        // Check for stillbirth
        val deliveryOutcomes = mapData[AssessmentDefinedParams.ID_DELIVERY_OUTCOMES] as? Map<*, *>
        deliveryOutcomes?.let { delivery ->
            val stillbirthCount = when (val stillBirthNumbers = delivery[AssessmentDefinedParams.STILLBIRTH_NUMBERS]) {
                is Number -> stillBirthNumbers.toInt()
                is String -> stillBirthNumbers.toIntOrNull() ?: 0
                else -> 0
            }
            if (stillbirthCount > 0) {
                return true
            }
        }

        // Check for baby death
        val newbornDetailsList = findNewbornDetails(mapData)
        newbornDetailsList?.forEach { babyData ->
            if (babyData is Map<*, *>) {
                val isDead = when (val isBabyAlive = babyData[AssessmentDefinedParams.IS_BABY_ALIVE]) {
                    is String -> isBabyAlive.equals(AssessmentDefinedParams.NO, ignoreCase = true)
                    is Boolean -> !isBabyAlive
                    else -> isBabyAlive?.toString()?.equals(AssessmentDefinedParams.NO, ignoreCase = true) == true
                }
                if (isDead) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Adds counselling card below the result card
     */
    private fun addCounsellingCard() {
        // Check if counselling should be shown
        if (!shouldShowCounselling(currentMapData)) {
            return
        }

        val formLayouts = viewModel.formLayoutsLiveData.value
            ?.data
            ?.formLayout ?: return

        // Get counselling card layout for title
        val counsellingCardLayout = formLayouts.firstOrNull {
            it.id == AssessmentDefinedParams.COUNSELLING_ADVERSE_EVENT
        }

        // Check if timeOfDeath has any value and update member status
        val maternalDeath = currentMapData?.get(AssessmentDefinedParams.MATERNAL_DEATH) as? Map<String, Any?>
        val timeOfDeath = maternalDeath?.get(AssessmentDefinedParams.TIME_OF_DEATH)
        val hasTimeOfDeath = when (timeOfDeath) {
            is String -> timeOfDeath.isNotBlank() && timeOfDeath != DefinedParams.DefaultID
            is Map<*, *> -> {
                val timeOfDeathId = timeOfDeath[DefinedParams.ID]?.toString()
                !timeOfDeathId.isNullOrBlank() && timeOfDeathId != DefinedParams.DefaultID
            }

            else -> false
        }

        // Get only the new counselling items (filter out old ones)
        val counsellingItems = formLayouts
            .filter {
                it.family == AssessmentDefinedParams.COUNSELLING_ADVERSE_EVENT &&
                    it.viewType == ViewType.VIEW_TYPE_INSTRUCTION &&
                    it.isSummary == true &&
                    (
                        it.id == AssessmentDefinedParams.COUNSELLING_EMOTIONAL_SUPPORT ||
                            // If mother not died then add future pregnancy planning to counselling
                            (!hasTimeOfDeath && it.id == AssessmentDefinedParams.COUNSELLING_FUTURE_PREGNANCY_PLANNING)
                    )
            }.sortedBy { it.orderId ?: Int.MAX_VALUE }

        // If no counselling items found, don't show card
        if (counsellingItems.isEmpty()) {
            return
        }

        // Create Counselling CardView using CardLayoutBinding
        val counsellingCardBinding = CardLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        counsellingCardBinding.llFamilyRoot.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        counsellingCardBinding.llFamilyRoot.setPadding(12.px)
        counsellingCardBinding.llFamilyRoot.dividerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.divider_transparent_12)

        // Set card title
        counsellingCardLayout?.let { cardLayout ->
            counsellingCardBinding.cardTitle.text = translateTitle(
                cardLayout.titleCulture,
                cardLayout.title,
                isTranslationEnabled,
            )
        } ?: run {
            // Fallback title if cardLayout not found
            counsellingCardBinding.cardTitle.text = getString(R.string.counseling_education)
        }

        // Add counselling items to the card's content layout
        counsellingItems.forEach { formLayout ->
            addCounsellingInstructions(formLayout, counsellingCardBinding.llFamilyRoot, isTranslationEnabled)
        }

        // Find the parent ConstraintLayout inside scrollView to add the card
        val scrollViewContent = binding.scrollView.getChildAt(0) as? androidx.constraintlayout.widget.ConstraintLayout
        scrollViewContent?.let { content ->
            // Set layout parameters with constraints
            val layoutParams = androidx.constraintlayout.widget.ConstraintLayout
                .LayoutParams(
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    // Set margins to match resultCardView
                    marginStart = resources.getDimensionPixelSize(R.dimen._6sdp)
                    marginEnd = resources.getDimensionPixelSize(R.dimen._6sdp)
                    topMargin = resources.getDimensionPixelSize(R.dimen._6sdp)
                    bottomMargin = resources.getDimensionPixelSize(R.dimen._6sdp)

                    // Set constraints: below resultCardView, start/end to parent
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    topToBottom = binding.resultCardView.id
                }

            counsellingCardBinding.root.layoutParams = layoutParams

            // Add counselling card to the ConstraintLayout
            content.addView(counsellingCardBinding.root)
        }
    }

    /**
     * Adds counselling instruction row with clickable title that opens dialog
     */
    private fun addCounsellingInstructions(
        formLayout: org.medtroniclabs.uhis.formgeneration.model.FormLayout,
        parentLayout: LinearLayout,
        isTranslationEnabled: Boolean,
    ) {
        with(formLayout) {
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
            parentLayout.addView(instructionBinding.root)
        }
    }
}
