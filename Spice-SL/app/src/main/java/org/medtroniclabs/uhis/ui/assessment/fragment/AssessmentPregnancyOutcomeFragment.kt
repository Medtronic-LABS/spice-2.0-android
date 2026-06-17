package org.medtroniclabs.uhis.ui.assessment.fragment

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.CheckboxDialogSpinnerLayoutBinding
import org.medtroniclabs.uhis.databinding.EdittextLayoutBinding
import org.medtroniclabs.uhis.databinding.FragmentAssessmentBinding
import org.medtroniclabs.uhis.databinding.LayoutSingleSelectionBinding
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

/**
 * Fragment responsible for pregnancy outcome assessment
 */
@AndroidEntryPoint
class AssessmentPregnancyOutcomeFragment :
    BaseFragment(),
    FormEventListener,
    View.OnClickListener {
    private lateinit var binding: FragmentAssessmentBinding
    private val viewModel: AssessmentViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    private var currentBabyCount = 0

    // Store baby data: babyIndex (1-based) -> fieldId -> value
    private val babyDataMap = HashMap<Int, HashMap<String, Any>>()

    // Store original field titles (without status) for fields that have status conditions
    private val originalTitles = HashMap<String, String>()

    private val sexOptions = AssessmentDefinedParams.pregnancyOutcomeNewbornSexOptions

    private val babyAliveOptions = AssessmentDefinedParams.pregnancyOutcomeBabyAliveOptions

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
        attachObservers()
        setListener()
        UserDetail.startDateTime = AnalyticsUtils.getCurrentDateTimeInLocalTime()
        viewModel.setUserJourney(AnalyticsDefinedParams.PREGNANCY_OUTCOME)
    }

    private fun attachObservers() {
        // Load pregnancy details to get EDD for preterm calculation
        viewModel.getPregnancyDetailInformation()
        viewModel.pregnancyDetailLiveData.observe(viewLifecycleOwner) {
            // When pregnancy details are loaded, check and update preterm status if date exists
            checkAndUpdatePretermStatus()
        }

        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        // Filter out fields marked as isSummary to hide them in form view
                        val filteredFormLayout = data.formLayout.filter { it.isSummary != true }
                        formGenerator.populateViews(filteredFormLayout)
                        // Store original title for date of delivery
                        storeOriginalDateOfDeliveryTitle()
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.LOADING -> {
                    showProgress()
                }
            }
        }
    }

    private fun setListener() {
        binding.btnSubmit.safeClickListener(this)
    }

    fun initView() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )
        childFragmentManager.executePendingTransactions()
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = isTranslationEnabled,
            callback = { resultMap, changedFieldId ->
                when (changedFieldId) {
                    AssessmentDefinedParams.ID_LIVE_BIRTH_NUMBERS -> {
                        val count = (resultMap[AssessmentDefinedParams.ID_LIVE_BIRTH_NUMBERS] as? Number)?.toInt() ?: 0
                        updateBabySections(count)
                    }

                    AssessmentDefinedParams.DATE_OF_DELIVERY -> {
                        checkAndUpdatePretermStatus()
                    }

                    AssessmentDefinedParams.ID_PREGNANCY_OUTCOME_TYPE -> {
                        val outcomeType = resultMap[changedFieldId].toString()
                        if (outcomeType == AssessmentDefinedParams.PregnancyOutcomeType.LIVE_BIRTH.value) {
                            // If the user selected maternal death and then selects live birth,
                            // then there is some conflict happens(because delivery outcome has dependency with maternal death) due to which delivery outcome is not getting visible.
                            // Hence, added post callback to make the view visible
                            binding.llForm.post {
                                formGenerator.getViewByTag(AssessmentDefinedParams.ID_DELIVERY_OUTCOMES + formGenerator.rootSuffix)?.visible()
                            }
                        }
                    }

                    AssessmentDefinedParams.PLACE_OF_DELIVERY,
                    AssessmentDefinedParams.ID_MODE_OF_DELIVERY,
                    -> {
                        val placeOfDelivery = resultMap[AssessmentDefinedParams.PLACE_OF_DELIVERY].toString()
                        val modeOfDelivery = resultMap[AssessmentDefinedParams.ID_MODE_OF_DELIVERY].toString()
                        binding.llForm.post {
                            formGenerator.getViewByTag(AssessmentDefinedParams.ID_BIRTH_ATTENDANT + formGenerator.rootSuffix)?.let {
                                if (placeOfDelivery == AssessmentDefinedParams.PlaceOfDelivery.HOME.value) {
                                    if (modeOfDelivery == AssessmentDefinedParams.ModeOfDelivery.CESAREAN_SECTION.value) {
                                        formGenerator.resetChildViews(it)
                                        it.gone()
                                    } else {
                                        it.visible()
                                    }
                                } else {
                                    formGenerator.resetChildViews(it)
                                    it.gone()
                                }
                            }
                        }
                    }
                }
            },
        )
        viewModel.getFormData(MenuConstants.PREGNANCY_OUTCOME)
    }

    /**
     * Creates or updates the dynamic baby sections based on liveBirthNumbers value
     */
    private fun updateBabySections(count: Int) {
        val effectiveCount = count.coerceIn(0, MAX_BABIES)

        if (count > MAX_BABIES) {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.max_babies_allowed, MAX_BABIES),
                    Toast.LENGTH_SHORT,
                ).show()
        }

        // Remove existing dynamic container
        val existingContainer = binding.llForm.findViewWithTag<LinearLayout>(NEWBORN_CONTAINER_TAG)
        existingContainer?.let { binding.llForm.removeView(it) }

        // Clear baby data for removed babies
        val keysToRemove = babyDataMap.keys.filter { it > effectiveCount }
        keysToRemove.forEach { babyDataMap.remove(it) }

        currentBabyCount = effectiveCount

        if (effectiveCount <= 0) {
            return
        }

        // Create a container for all baby sections
        val container = LinearLayout(requireContext()).apply {
            tag = NEWBORN_CONTAINER_TAG
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        for (babyIndex in 1..effectiveCount) {
            // Ensure baby data map exists for this baby
            if (!babyDataMap.containsKey(babyIndex)) {
                babyDataMap[babyIndex] = HashMap()
            }

            val babyCard = createBabyCard(babyIndex, isTranslationEnabled)
            container.addView(babyCard)
        }

        // Find the position of the counselling section and insert before it
        // or just append at the end of the form
        val counsellingCardTag = AssessmentDefinedParams.COUNSELLING_ADVERSE_EVENT + AssessmentDefinedParams.rootSuffix
        val counsellingView = binding.llForm.findViewWithTag<View>(counsellingCardTag)
        if (counsellingView != null) {
            val counsellingIndex = binding.llForm.indexOfChild(counsellingView)
            binding.llForm.addView(container, counsellingIndex)
        } else {
            binding.llForm.addView(container)
        }
    }

    /**
     * Creates a single baby card with all fields
     */
    private fun createBabyCard(
        babyIndex: Int,
        translate: Boolean,
    ): View {
        val cardBinding = CardLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        cardBinding.cardTitle.text = getString(R.string.baby_label, babyIndex)
        cardBinding.root.tag = "babyCard_$babyIndex"

        val familyRoot = cardBinding.llFamilyRoot

        // 1. Is the baby alive - SingleSelectionView
        addIsBabyAliveField(familyRoot, babyIndex, translate)

        // 2. Sex - SingleSelectionView
        addSexField(familyRoot, babyIndex, translate)

        // 3. Birth Weight - Removed (no longer needed)

        // 4. Cause of neonatal death - DialogCheckbox (initially hidden)
        addCauseOfDeathField(familyRoot, babyIndex, translate)

        return cardBinding.root
    }

    private fun addIsBabyAliveField(
        parent: LinearLayout,
        babyIndex: Int,
        translate: Boolean,
    ) {
        val selectionBinding = LayoutSingleSelectionBinding.inflate(
            LayoutInflater.from(requireContext()),
        )
        selectionBinding.root.tag = "isBabyAlive_${babyIndex}_root"
        selectionBinding.tvTitle.text = if (translate) {
            getString(R.string.is_baby_alive)
        } else {
            getString(R.string.is_baby_alive)
        }
        selectionBinding.tvTitle.markMandatory()

        val singleSelectionView = SingleSelectionCustomView(requireContext())
        singleSelectionView.tag = "isBabyAlive_$babyIndex"

        val resultMap = babyDataMap[babyIndex] ?: HashMap()
        val formLayout = FormLayout(
            viewType = "SingleSelectionView",
            id = "isBabyAlive_$babyIndex",
            title = getString(R.string.is_baby_alive),
            optionsList = babyAliveOptions,
            visibility = "visible",
        )

        singleSelectionView.addViewElements(
            babyAliveOptions,
            translate,
            resultMap,
            Pair(AssessmentDefinedParams.IS_BABY_ALIVE, null),
            formLayout,
        ) { selectedId, _, _, name ->
            babyDataMap[babyIndex]?.set(AssessmentDefinedParams.IS_BABY_ALIVE, selectedId ?: "")

            // Clear error message when value is selected
            selectionBinding.tvErrorMessage.visibility = View.GONE

            // Toggle cause of neonatal death visibility
            val causeRoot = parent.findViewWithTag<View>("${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_${babyIndex}_root")
            if (name == AssessmentDefinedParams.NO) {
                causeRoot?.visibility = View.VISIBLE
            } else {
                causeRoot?.visibility = View.GONE
                // Clear cause of death data when baby is alive
                babyDataMap[babyIndex]?.remove(AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH)
                // Reset the checkbox text
                val checkboxView = parent.findViewWithTag<View>("${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_$babyIndex")
                if (checkboxView is androidx.appcompat.widget.AppCompatTextView) {
                    checkboxView.text = getString(R.string.cause_of_death_hint)
                }
                // Clear error message for cause of death when baby is alive
                val checkboxBinding = causeRoot?.let {
                    CheckboxDialogSpinnerLayoutBinding.bind(it)
                }
                checkboxBinding?.tvErrorMessage?.visibility = View.GONE
            }
        }

        selectionBinding.selectionGroup.addView(singleSelectionView)
        parent.addView(selectionBinding.root)
    }

    private fun addSexField(
        parent: LinearLayout,
        babyIndex: Int,
        translate: Boolean,
    ) {
        val selectionBinding = LayoutSingleSelectionBinding.inflate(
            LayoutInflater.from(requireContext()),
        )
        selectionBinding.root.tag = "sex_${babyIndex}_root"
        selectionBinding.tvTitle.text = if (translate) {
            getString(R.string.sex_label)
        } else {
            getString(R.string.sex_label)
        }
        selectionBinding.tvTitle.markMandatory()

        val singleSelectionView = SingleSelectionCustomView(requireContext())
        singleSelectionView.tag = "sex_$babyIndex"

        val resultMap = babyDataMap[babyIndex] ?: HashMap()
        val formLayout = FormLayout(
            viewType = "SingleSelectionView",
            id = "sex_$babyIndex",
            title = getString(R.string.sex_label),
            optionsList = sexOptions,
            visibility = "visible",
        )

        singleSelectionView.addViewElements(
            sexOptions,
            translate,
            resultMap,
            Pair(AssessmentDefinedParams.SEX, null),
            formLayout,
        ) { selectedId, _, _, _ ->
            babyDataMap[babyIndex]?.set(AssessmentDefinedParams.SEX, selectedId ?: "")
            // Clear error message when value is selected
            selectionBinding.tvErrorMessage.visibility = View.GONE
            Unit
        }

        selectionBinding.selectionGroup.addView(singleSelectionView)
        parent.addView(selectionBinding.root)
    }

    private fun addBirthWeightField(
        parent: LinearLayout,
        babyIndex: Int,
    ) {
        val editBinding = EdittextLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        editBinding.root.tag = "birthWeight_${babyIndex}_root"
        editBinding.tvTitle.text = getString(R.string.birth_weight_in_kg)
        editBinding.etUserInput.tag = "birthWeight_$babyIndex"
        editBinding.etUserInput.hint = getString(R.string.enter_weight_in_kg)
        editBinding.etUserInput.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editBinding.etUserInput.filters = arrayOf(android.text.InputFilter.LengthFilter(6))

        (editBinding.etUserInput.background as? GradientDrawable)?.apply {
            setStroke(
                resources.getDimensionPixelSize(R.dimen._1sdp),
                requireContext().getColor(R.color.edittext_stroke),
            )
        }

        editBinding.etUserInput.addTextChangedListener { editable ->
            val value = editable?.toString()?.trim()?.toDoubleOrNull()
            if (value != null) {
                babyDataMap[babyIndex]?.set(AssessmentDefinedParams.BIRTH_WEIGHT, value)
            } else {
                babyDataMap[babyIndex]?.remove(AssessmentDefinedParams.BIRTH_WEIGHT)
            }
        }

        parent.addView(editBinding.root)
    }

    private fun addCauseOfDeathField(
        parent: LinearLayout,
        babyIndex: Int,
        translate: Boolean,
    ) {
        val checkboxBinding = CheckboxDialogSpinnerLayoutBinding.inflate(
            LayoutInflater.from(requireContext()),
        )
        checkboxBinding.root.tag = "${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_${babyIndex}_root"
        checkboxBinding.tvTitle.text = getString(R.string.cause_of_neonatal_death)
        checkboxBinding.tvTitle.markMandatory()
        checkboxBinding.etUserInput.tag = "${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_$babyIndex"
        checkboxBinding.etUserInput.text = getString(R.string.cause_of_death_hint)

        // Initially hidden
        checkboxBinding.root.visibility = View.GONE

        checkboxBinding.etUserInput.safeClickListener {
            openCauseOfDeathDialog(babyIndex, checkboxBinding.etUserInput)
        }

        parent.addView(checkboxBinding.root)
    }

    private fun openCauseOfDeathDialog(
        babyIndex: Int,
        textView: androidx.appcompat.widget.AppCompatTextView,
    ) {
        val dialogKey = "${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_$babyIndex"
        val inputData = arrayListOf<SignsAndSymptomsEntity>()
        RMNCH.neonatalDeathCauseOptions.forEachIndexed { index, it ->
            val name = it[DefinedParams.NAME] as String
            val value = it[DefinedParams.ID] as String
            inputData.add(
                SignsAndSymptomsEntity(
                    _id = index.toLong(),
                    symptom = name,
                    type = dialogKey,
                    value = value,
                    displayValue = it[DefinedParams.CULTURE_VALUE]?.toString(),
                ),
            )
        }

        val existingData = babyDataMap[babyIndex]?.get(AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH)

        CheckBoxDialog
            .newInstance(dialogKey, existingData, title = getString(R.string.cause_of_death_hint), inputData = inputData) { map ->
                // Get the root view to access error message
                val rootView = binding.llForm.findViewWithTag<View>("${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_${babyIndex}_root")
                val checkboxBinding = rootView?.let { CheckboxDialogSpinnerLayoutBinding.bind(it) }

                if (map.isEmpty()) {
                    babyDataMap[babyIndex]?.remove(AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH)
                    textView.text = getString(R.string.cause_of_death_hint)
                    // Show error if empty and baby is not alive
                    val isBabyAlive = babyDataMap[babyIndex]?.get(AssessmentDefinedParams.IS_BABY_ALIVE)
                    if (isBabyAlive == AssessmentDefinedParams.NO) {
                        checkboxBinding?.tvErrorMessage?.visibility = View.VISIBLE
                        checkboxBinding?.tvErrorMessage?.text = getString(R.string.default_user_input_error)
                    }
                } else {
                    // Transform ArrayList<HashMap> to ArrayList<String> (extract 'value' field)
                    val valueList = ArrayList<String>()
                    map.forEach { item ->
                        val value = item[DefinedParams.Value]?.toString()
                            ?: item[DefinedParams.ID]?.toString()
                        value?.let { valueList.add(it) }
                    }
                    babyDataMap[babyIndex]?.set(AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH, valueList)
                    val count = valueList.size
                    textView.text = "$count ${getString(R.string.selected)}"
                    // Clear error message when value is selected
                    checkboxBinding?.tvErrorMessage?.visibility = View.GONE
                }
            }.show(childFragmentManager, CheckBoxDialog.TAG)
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
        // Use localDataCache if available, otherwise use id (for database loading)
        val dialogKey = formLayout.localDataCache ?: id

        val title = if (isTranslationEnabled && !formLayout.hintCulture.isNullOrBlank()) {
            formLayout.hintCulture
        } else {
            formLayout.hint
        }

        if (formLayout.localDataCache != null) {
            // If localDataCache exists, load from database
            CheckBoxDialog
                .newInstance(dialogKey, resultMap, title = title) { map ->
                    formGenerator.validateCheckboxDialogue(id, formLayout, map)
                }.show(childFragmentManager, CheckBoxDialog.TAG)
        } else {
            // Handle causeOfDeath field with conditional filtering based on timeOfDeath
            if (id == AssessmentDefinedParams.CAUSE_OF_DEATH) {
                val filteredInputData = getFilteredCauseOfDeathOptions(formLayout, dialogKey)
                val validatedResultMap = validateCauseOfDeathSelections(resultMap, formLayout)
                CheckBoxDialog
                    .newInstance(dialogKey, validatedResultMap, title = title, inputData = filteredInputData) { map ->
                        formGenerator.validateCheckboxDialogue(id, formLayout, map)
                    }.show(childFragmentManager, CheckBoxDialog.TAG)
            } else {
                // Create inputData from optionsList for other fields
                val inputData = arrayListOf<SignsAndSymptomsEntity>()
                formLayout.optionsList?.forEachIndexed { index, it ->
                    val name = it[DefinedParams.NAME] as String
                    val value = it[DefinedParams.ID] as String
                    inputData.add(
                        SignsAndSymptomsEntity(
                            _id = index.toLong(),
                            symptom = name,
                            type = dialogKey,
                            value = value,
                            displayValue = it[DefinedParams.CULTURE_VALUE]?.toString(),
                        ),
                    )
                }
                CheckBoxDialog
                    .newInstance(dialogKey, resultMap, title = title, inputData = inputData) { map ->
                        formGenerator.validateCheckboxDialogue(id, formLayout, map)
                    }.show(childFragmentManager, CheckBoxDialog.TAG)
            }
        }
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
    }

    /**
     * Validates all baby fields before form submission
     * @return true if all required fields are filled, false otherwise
     */
    private fun validateBabyFields(): Boolean {
        if (currentBabyCount == 0) {
            return true // No babies to validate
        }

        var isValid = true

        for (babyIndex in 1..currentBabyCount) {
            // Validate isBabyAlive
            val isBabyAliveValue = babyDataMap[babyIndex]?.get(AssessmentDefinedParams.IS_BABY_ALIVE)?.toString()
            val isBabyAliveRoot = binding.llForm.findViewWithTag<View>("isBabyAlive_${babyIndex}_root")
            val isBabyAliveBinding = isBabyAliveRoot?.let { LayoutSingleSelectionBinding.bind(it) }

            if (isBabyAliveValue.isNullOrBlank()) {
                isValid = false
                isBabyAliveBinding?.tvErrorMessage?.visibility = View.VISIBLE
                isBabyAliveBinding?.tvErrorMessage?.text = getString(R.string.default_user_input_error)
            } else {
                isBabyAliveBinding?.tvErrorMessage?.visibility = View.GONE
            }

            // Validate sex
            val sexValue = babyDataMap[babyIndex]?.get(AssessmentDefinedParams.SEX)?.toString()
            val sexRoot = binding.llForm.findViewWithTag<View>("sex_${babyIndex}_root")
            val sexBinding = sexRoot?.let { LayoutSingleSelectionBinding.bind(it) }

            if (sexValue.isNullOrBlank()) {
                isValid = false
                sexBinding?.tvErrorMessage?.visibility = View.VISIBLE
                sexBinding?.tvErrorMessage?.text = getString(R.string.default_user_input_error)
            } else {
                sexBinding?.tvErrorMessage?.visibility = View.GONE
            }

            // Validate causeOfNeonatalDeath (only if baby is not alive)
            if (isBabyAliveValue == AssessmentDefinedParams.NO) {
                val causeOfDeathValue = babyDataMap[babyIndex]?.get(AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH)
                val causeOfDeathRoot = binding.llForm.findViewWithTag<View>("${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_${babyIndex}_root")
                val causeOfDeathBinding = causeOfDeathRoot?.let { CheckboxDialogSpinnerLayoutBinding.bind(it) }

                val isEmpty = when {
                    causeOfDeathValue == null -> true
                    causeOfDeathValue is ArrayList<*> -> causeOfDeathValue.isEmpty()
                    causeOfDeathValue is List<*> -> causeOfDeathValue.isEmpty()
                    causeOfDeathValue.toString().isBlank() -> true
                    else -> false
                }

                if (isEmpty) {
                    isValid = false
                    causeOfDeathBinding?.tvErrorMessage?.visibility = View.VISIBLE
                    causeOfDeathBinding?.tvErrorMessage?.text = getString(R.string.default_user_input_error)
                } else {
                    causeOfDeathBinding?.tvErrorMessage?.visibility = View.GONE
                }
            } else {
                // Hide error message if baby is alive
                val causeOfDeathRoot = binding.llForm.findViewWithTag<View>("${AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH}_${babyIndex}_root")
                val causeOfDeathBinding = causeOfDeathRoot?.let { CheckboxDialogSpinnerLayoutBinding.bind(it) }
                causeOfDeathBinding?.tvErrorMessage?.visibility = View.GONE
            }
        }

        return isValid
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        // Validate baby fields before processing
        if (!validateBabyFields()) {
            // Scroll to first error if needed
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, 0)
            }
            return
        }

        resultMap?.let { details ->
            // Add baby data as an array under "newbornDetails" key
            if (currentBabyCount > 0) {
                val newbornDetailsList = ArrayList<HashMap<String, Any>>()
                for (babyIndex in 1..currentBabyCount) {
                    babyDataMap[babyIndex]?.let { babyData ->
                        if (babyData.isNotEmpty()) {
                            val cleanedBabyData = HashMap<String, Any>()
                            babyData.forEach { (key, value) ->
                                // Transform causeOfNeonatalDeath if it's ArrayList<HashMap>
                                if (key == AssessmentDefinedParams.CAUSE_OF_NEONATAL_DEATH && value is ArrayList<*>) {
                                    val transformedList = ArrayList<String>()
                                    value.forEach { item ->
                                        if (item is HashMap<*, *>) {
                                            val stringValue = item[DefinedParams.Value]?.toString()
                                                ?: item[DefinedParams.ID]?.toString()
                                            stringValue?.let { transformedList.add(it) }
                                        } else if (item is String) {
                                            transformedList.add(item)
                                        }
                                    }
                                    if (transformedList.isNotEmpty()) {
                                        cleanedBabyData[key] = transformedList
                                    }
                                } else {
                                    cleanedBabyData[key] = value
                                }
                            }
                            if (cleanedBabyData.isNotEmpty()) {
                                newbornDetailsList.add(cleanedBabyData)
                            }
                        }
                    }
                }
                if (newbornDetailsList.isNotEmpty()) {
                    details[AssessmentDefinedParams.NEWBORN_DETAILS] = newbornDetailsList
                }
            }

            // Remove empty objects from the result map
            val referralResult = ReferralResultGenerator().calculatePregnancyOutcomeStatus(details)
            val result = serverData?.let {
                FormResultComposer().groupValues(
                    serverData = it,
                    details,
                    MenuConstants.PREGNANCY_OUTCOME,
                )
            }
            result?.second?.let {
                val cleanedDetails = removeEmptyObjects(it)
                viewModel.saveAssessment(serverData, cleanedDetails, referralResult, viewModel.menuId)
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.PREGNANCY_OUTCOME,
            eventName = AnalyticsDefinedParams.AssessmentCreation,
        )
    }

    override fun onRenderingComplete() {
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        // When timeOfDeath changes, validate and clean causeOfDeath selections
        if (id == AssessmentDefinedParams.TIME_OF_DEATH) {
            validateCauseOfDeathOnTimeOfDeathChange()
        }
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
    }

    override fun onQRScanRequested() {
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

    /**
     * Returns whether user has filled any details already or not.
     *
     * Based on this check, when user pressing back, we show alert for confirmation
     */
    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty() || babyDataMap.any { it.value.isNotEmpty() }

    /**
     * Recursively removes empty HashMap objects from the map
     */
    private fun removeEmptyObjects(map: HashMap<String, Any>): HashMap<String, Any> {
        val result = HashMap<String, Any>()
        map.forEach { (key, value) ->
            when (value) {
                is HashMap<*, *> -> {
                    val cleaned = removeEmptyObjects(value as HashMap<String, Any>)
                    if (cleaned.isNotEmpty()) {
                        result[key] = cleaned
                    }
                }

                is Map<*, *> -> {
                    val cleaned = removeEmptyObjects(value as HashMap<String, Any>)
                    if (cleaned.isNotEmpty()) {
                        result[key] = cleaned
                    }
                }

                else -> {
                    if (value != null) {
                        result[key] = value
                    }
                }
            }
        }
        return result
    }

    /**
     * Filters cause of death options based on time of death selection
     * Option 6 (unsafeAbortion): Hide when death is during/after delivery
     * Option 4 (obstructedLabor): Hide when death is before/after delivery
     */
    private fun getFilteredCauseOfDeathOptions(
        formLayout: FormLayout,
        dialogKey: String,
    ): ArrayList<SignsAndSymptomsEntity> {
        val timeOfDeathValue = getTimeOfDeathValue()
        val inputData = arrayListOf<SignsAndSymptomsEntity>()

        formLayout.optionsList?.forEachIndexed { index, it ->
            val optionId = it[DefinedParams.ID] as? String ?: ""

            // Option 4: "obstructedLabor" - Hide when death is before/after delivery
            if (optionId == AssessmentDefinedParams.CAUSE_OF_DEATH_OBSTRUCTED_LABOR) {
                if (timeOfDeathValue == AssessmentDefinedParams.TIME_OF_DEATH_DURING_CHILDBIRTH) {
                    addOptionToInputData(it, index, dialogKey, inputData)
                }
            } else if (optionId == AssessmentDefinedParams.CAUSE_OF_DEATH_UNSAFE_ABORTION) {
                if (timeOfDeathValue == AssessmentDefinedParams.TIME_OF_DEATH_BEFORE_DELIVERY) {
                    addOptionToInputData(it, index, dialogKey, inputData)
                }
            } else {
                addOptionToInputData(it, index, dialogKey, inputData)
            }
        }

        return inputData
    }

    /**
     * Helper method to add option to inputData
     */
    private fun addOptionToInputData(
        option: Map<String, Any>,
        index: Int,
        dialogKey: String,
        inputData: ArrayList<SignsAndSymptomsEntity>,
    ) {
        val name = option[DefinedParams.NAME] as? String ?: ""
        val value = option[DefinedParams.ID] as? String ?: ""
        inputData.add(
            SignsAndSymptomsEntity(
                _id = index.toLong(),
                symptom = name,
                type = dialogKey,
                value = value,
                displayValue = option[DefinedParams.CULTURE_VALUE]?.toString(),
            ),
        )
    }

    /**
     * Gets the current timeOfDeath value from form result
     */
    private fun getTimeOfDeathValue(): String? {
        val timeOfDeathResult = formGenerator.getResult(AssessmentDefinedParams.TIME_OF_DEATH)
        return when (timeOfDeathResult) {
            is Map<*, *> -> {
                // If it's a Map, extract the "id" value
                timeOfDeathResult[DefinedParams.ID]?.toString()
                    ?: timeOfDeathResult[DefinedParams.ID]?.toString()
            }

            is String -> timeOfDeathResult
            else -> null
        }
    }

    /**
     * Validates and filters existing causeOfDeath selections based on current timeOfDeath
     * Removes invalid selections that should be hidden
     */
    private fun validateCauseOfDeathSelections(
        resultMap: Any?,
        formLayout: FormLayout,
    ): Any? {
        val timeOfDeathValue = getTimeOfDeathValue()
        if (timeOfDeathValue == null || resultMap == null) {
            return resultMap
        }

        val resultList = resultMap as? ArrayList<HashMap<String, Any>> ?: return resultMap
        val validatedList = ArrayList<HashMap<String, Any>>()

        resultList.forEach { selectedItem ->
            val optionId = selectedItem[DefinedParams.ID]?.toString()
                ?: selectedItem[DefinedParams.ID]?.toString()
                ?: selectedItem[DefinedParams.Value]?.toString()

            if (optionId == AssessmentDefinedParams.CAUSE_OF_DEATH_OBSTRUCTED_LABOR) {
                if (timeOfDeathValue == AssessmentDefinedParams.TIME_OF_DEATH_DURING_CHILDBIRTH) {
                    validatedList.add(selectedItem)
                }
            } else if (optionId == AssessmentDefinedParams.CAUSE_OF_DEATH_UNSAFE_ABORTION) {
                if (timeOfDeathValue == AssessmentDefinedParams.TIME_OF_DEATH_BEFORE_DELIVERY) {
                    validatedList.add(selectedItem)
                }
            } else {
                validatedList.add(selectedItem)
            }
        }

        return if (validatedList.isEmpty()) null else validatedList
    }

    /**
     * Validates causeOfDeath selections when timeOfDeath changes
     * Updates the form result and UI if invalid selections are found
     */
    private fun validateCauseOfDeathOnTimeOfDeathChange() {
        val causeOfDeathResult = formGenerator.getResult(AssessmentDefinedParams.CAUSE_OF_DEATH) ?: return

        // Get the formLayout for causeOfDeath to validate selections
        val serverData = formGenerator.getServerData()
        val causeOfDeathLayout = serverData?.find { it.id == AssessmentDefinedParams.CAUSE_OF_DEATH }

        if (causeOfDeathLayout != null) {
            val validatedResult = validateCauseOfDeathSelections(causeOfDeathResult, causeOfDeathLayout)

            // Update the result map if selections were removed
            if (validatedResult != causeOfDeathResult) {
                if (validatedResult == null) {
                    formGenerator.getResultMap().remove(AssessmentDefinedParams.CAUSE_OF_DEATH)
                } else {
                    formGenerator.getResultMap()[AssessmentDefinedParams.CAUSE_OF_DEATH] = validatedResult
                }

                // Update the UI display
                formGenerator.getViewByTag(AssessmentDefinedParams.CAUSE_OF_DEATH)?.let { view ->
                    if (view is androidx.appcompat.widget.AppCompatTextView) {
                        val validatedList = validatedResult as? ArrayList<HashMap<String, Any>> ?: arrayListOf()
                        if (validatedList.isEmpty()) {
                            view.text = causeOfDeathLayout.hint ?: getString(R.string.cause_of_death_hint)
                        } else {
                            view.text = "${validatedList.size} ${getString(R.string.selected)}"
                        }
                    }
                }
            }
        }
    }

    /**
     * Stores the original title for Date of Delivery field (without status)
     */
    private fun storeOriginalDateOfDeliveryTitle() {
        val fieldId = AssessmentDefinedParams.DATE_OF_DELIVERY
        val tag = fieldId + formGenerator.titleSuffix
        val titleView = formGenerator.getViewByTag(tag) as? TextView
        titleView?.let {
            val currentText = it.text.toString()
            // Remove any existing status pattern: " (Status Text)"
            val statusPattern = "\\s+\\([^)]+\\)\\s*$".toRegex()
            val original = currentText.replace(statusPattern, "")
            originalTitles[fieldId] = original
        }
    }

    /**
     * Checks if delivery is preterm and updates the field title accordingly
     */
    private fun checkAndUpdatePretermStatus() {
        val dateOfDelivery = formGenerator.getResult(AssessmentDefinedParams.DATE_OF_DELIVERY) as? String
        if (dateOfDelivery.isNullOrBlank()) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                AssessmentDefinedParams.DATE_OF_DELIVERY,
            )
            return
        }

        val pregnancyDetail = viewModel.pregnancyDetailLiveData.value
        val edd = pregnancyDetail?.estimatedDeliveryDate

        if (edd.isNullOrBlank()) {
            // No EDD available, can't determine preterm
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                AssessmentDefinedParams.DATE_OF_DELIVERY,
            )
            return
        }

        // Check if preterm
        val isPreterm = isPretermDelivery(dateOfDelivery, edd)
        AssessmentUtil.updateFieldTitleWithStatus(
            formGenerator,
            originalTitles,
            isTranslationEnabled,
            AssessmentDefinedParams.DATE_OF_DELIVERY,
            if (isPreterm) getString(R.string.preterm_birth) else null,
        )
    }

    /**
     * Checks if delivery is preterm (<37 weeks from EDD)
     */
    private fun isPretermDelivery(
        dateOfDelivery: String,
        edd: String,
    ): Boolean {
        try {
            val gestationalWeeks = calculateGestationalAgeAtDelivery(dateOfDelivery, edd)
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
            val deliveryDateStr = org.medtroniclabs.uhis.common.DateUtils.convertDateFormat(
                dateOfDelivery,
                org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMdd,
            )
            val eddDateStr = org.medtroniclabs.uhis.common.DateUtils.convertDateFormat(
                edd,
                org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMdd,
            )

            if (deliveryDateStr.isNullOrBlank() || eddDateStr.isNullOrBlank()) {
                return null
            }

            // Get time in milliseconds
            val deliveryMillis = org.medtroniclabs.uhis.common.DateUtils.getCalendarFromString(
                deliveryDateStr,
                org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMdd,
            )
            val eddMillis = org.medtroniclabs.uhis.common.DateUtils.getCalendarFromString(
                eddDateStr,
                org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMdd,
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
            val weeksFromEDD = (diffInDays / 7.0).toInt()
            val gestationalWeeks = 40 - weeksFromEDD

            return gestationalWeeks
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        const val TAG = "AssessmentPregnancyOutcomeFragment"
        private const val MAX_BABIES = 5
        private const val NEWBORN_CONTAINER_TAG = "newbornDynamicContainer"
    }
}
