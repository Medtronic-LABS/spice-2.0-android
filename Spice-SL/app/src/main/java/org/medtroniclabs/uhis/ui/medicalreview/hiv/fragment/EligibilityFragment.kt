package org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.getOptionMap
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_ddMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.convertToRequiredFormat
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.model.MultiSelectDropDownModel
import org.medtroniclabs.uhis.databinding.FragmentEligibilityBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.formgeneration.utility.MultiSelectSpinnerAdapter
import org.medtroniclabs.uhis.formgeneration.utility.MultiSelectionNoneSpinner
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PREGNANCY_MAX_AGE
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PREGNANCY_MIN_AGE
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.HaveYouTakenHivTestBefore
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EligibilityFragment : BaseFragment() {
    private lateinit var binding: FragmentEligibilityBinding
    private val hivViewModel: HivViewModel by activityViewModels()
    private var datePickerDialog: DatePickerDialog? = null
    private val patientViewModel: PatientDetailViewModel by activityViewModels()

    private var adapter: MultiSelectSpinnerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentEligibilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        addObservers()
        lmpValidation()
        valueObserver()
    }

    companion object {
        const val TAG = "EligibilityFragment"
    }

    private fun addObservers() {
        hivViewModel.hivMetaListItems.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { list ->
                        initializeHivHistoryOptions(
                            list
                                .filter { it.category == MedicalReviewTypeEnums.hiv_history.name }
                                .distinctBy { it.name }
                                .sortedBy { it.displayOrder },
                        )
                        initializePopulationType(
                            list
                                .filter { it.category == MedicalReviewTypeEnums.population_type.name }
                                .distinctBy { it.name }
                                .sortedBy { it.displayOrder },
                        )
                        initializeHivTestDuration(
                            list
                                .filter { it.category == MedicalReviewTypeEnums.hiv_test_durations.name }
                                .distinctBy { it.name }
                                .sortedBy { it.displayOrder },
                        )
                    }
                }
            }
        }
    }

    private fun initViews() {
        hivViewModel.getHistoryListMetaItems()
        addCustomView(
            getData(),
            HaveYouTakenHivTestBefore,
            hivViewModel.resultHashMap,
            alreadyHIVTestedCallBack,
            binding.haveTestedHIVBeforeRoot,
        )
        val isEmtct = arguments?.getBoolean(DefinedParams.isPregnant, false)
        if (isEmtct == true) {
            binding.hivEMTCTViewGroup.visible()
        }
        binding.tvLastMenstrualPeriodDateLabelText.safeClickListener {
            showDatePickerDialog(
                binding.tvLastMenstrualPeriodDateLabelText,
                disableFuture = true,
                isLmp = true,
                lmp = binding.tvLastMenstrualPeriodDateLabelText.text
                    .toString()
                    .isNullOrEmpty(),
            )
        }
    }

    private fun initializeHivTestDuration(costList: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
                DefinedParams.Value to DefinedParams.DEFAULT_ID_LABEL,
            ),
        )
        for (item in costList) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to item.name,
                    DefinedParams.ID to item.id.toString(),
                    DefinedParams.Value to (item.value ?: item.name),
                ),
            )
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(dropDownList)
        binding.tvTestingDurationSpinner.adapter = adapter
        binding.tvTestingDurationSpinner.setSelection(0, false)
        binding.tvTestingDurationSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        handleHivTestDuration(it[DefinedParams.NAME] as String)
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    fun handleHivTestDuration(testDuration: String) {
        val isHaveYouTakenHivTestBefore = hivViewModel.resultHashMap[HaveYouTakenHivTestBefore]
        if (testDuration != DefinedParams.DEFAULT_ID_LABEL) {
            when (isHaveYouTakenHivTestBefore) {
                getString(R.string.yes) -> {
                    hivViewModel.selectedLastTestForHIV = testDuration
                    binding.tvTestingDurationSpinner.visible()
                }

                getString(R.string.no) -> {
                    hivViewModel.selectedLastTestForHIV = null
                    binding.tvTestingDurationSpinner.gone()
                }

                else -> {
                    hivViewModel.selectedLastTestForHIV = null
                    binding.tvTestingDurationSpinner.gone()
                }
            }
        } else {
            hivViewModel.selectedLastTestForHIV = null
        }
    }

    private fun initializeHivHistoryOptions(supplyList: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<MultiSelectDropDownModel>()
        for (item in supplyList) {
            dropDownList.add(
                MultiSelectDropDownModel(
                    id = item.id,
                    name = item.name,
                    value = item.value,
                ),
            )
        }
        val adapter = MultiSelectionNoneSpinner(
            requireContext(),
            dropDownList,
            hivViewModel.selectedHistoryListItem,
        )
        binding.tvHistorySpinner.adapter = adapter
        adapter.setOnItemSelectedListener(object :
            MultiSelectionNoneSpinner.OnItemSelectedListener {
            override fun onItemSelected(
                selectedItems: List<MultiSelectDropDownModel>,
                pos: Int,
            ) {
                if (selectedItems.isNotEmpty()) {
                    val containsNone =
                        selectedItems.any { it.name.equals(getString(R.string.none), true) }
                    hivViewModel.selectedHistoryListItem =
                        if (containsNone) {
                            arrayListOf(
                                selectedItems.first {
                                    it.name.equals(
                                        getString(R.string.none),
                                        true,
                                    )
                                },
                            )
                        } else {
                            ArrayList(selectedItems)
                        }
                }
            }
        })
    }

    private fun initializePopulationType(supplyList: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<MultiSelectDropDownModel>()
        var defaultSelectedItems = ArrayList<MultiSelectDropDownModel>()
        var pregnantItemIndex = -1 // Initialize to -1 (not found)
        val isFemale = patientViewModel.getGenderIsFemale()
        val dob = patientViewModel.getDob()
        val isDobValid = !dob.isNullOrBlank()

        val isEligibleAge = if (isFemale && isDobValid && !dob.isNullOrBlank()) {
            val ageAndWeek = DateUtils.getV2YearMonthAndWeek(dob)
            val ageYears = ageAndWeek.years
            val ageMonths = ageAndWeek.months
            val ageWeeks = ageAndWeek.weeks
            val ageDays = ageAndWeek.days

            ageYears in PREGNANCY_MIN_AGE..PREGNANCY_MAX_AGE &&
                !(ageYears == PREGNANCY_MAX_AGE && (ageMonths + ageWeeks + ageDays) != 0)
        } else {
            false
        }

        for ((index, item) in supplyList.withIndex()) {
            val dropDownItem = MultiSelectDropDownModel(
                id = item.id,
                name = item.name,
                value = item.value,
            )
            val isMale = arguments?.getBoolean(DefinedParams.GENDER) != true
            val isExcludedForMale = item.name.equals(getString(R.string.pregnant_), ignoreCase = true) ||
                item.name.equals(getString(R.string.female_sex_worker_fsw), ignoreCase = true)
            val isExcludedForAge = item.name.equals(getString(R.string.pregnant_), ignoreCase = true)

            val isFemale = arguments?.getBoolean(DefinedParams.GENDER) == true

            val isExcludedForFemale = item.name.equals(getString(R.string.men_having_sex_with_men_msm), ignoreCase = true)
            if (!(isMale && isExcludedForMale) && !(isFemale && isExcludedForFemale)) {
                if (!(isExcludedForAge && !isEligibleAge)) {
                    dropDownList.add(dropDownItem)
                }
            }
            // Check if the name is "Pregnant" (case-insensitive)
            if (item.name.equals(getString(R.string.pregnant_), ignoreCase = true)) {
                defaultSelectedItems.add(dropDownItem)
                pregnantItemIndex = index // Capture the index of "Pregnant"
            }
        }
        if (arguments?.getBoolean(DefinedParams.isPregnant, false) == true) {
            // Assign the default selected items to ViewModel (or create an empty list if not found)
            hivViewModel.selectedPopulationType = defaultSelectedItems
            // Create the adapter with the dropDownList and default selection
            adapter = MultiSelectSpinnerAdapter(
                requireContext(),
                dropDownList,
                hivViewModel.selectedPopulationType,
                true,
            )
        } else {
            // Create the adapter with the dropDownList and default selection
            adapter = MultiSelectSpinnerAdapter(
                requireContext(),
                dropDownList,
                hivViewModel.selectedPopulationType,
                false,
            )
        }

        binding.tvPopulationTypeSpinner.adapter = adapter
        adapter?.setOnItemSelectedListener(object : MultiSelectSpinnerAdapter.OnItemSelectedListener {
            override fun onItemSelected(
                selectedItems: List<MultiSelectDropDownModel>,
                pos: Int,
            ) {
                val isPregnancyEnforced = arguments?.getBoolean(DefinedParams.isPregnant, false) == true
                val pregnantName = getString(R.string.pregnant_)
                val pregnantItem = dropDownList.find {
                    it.name.equals(pregnantName, ignoreCase = true)
                }

                val isPregnantSelected = selectedItems.any {
                    it.name.equals(pregnantName, ignoreCase = true)
                }

                val updatedSelection = ArrayList(selectedItems)
                if (isPregnancyEnforced && pregnantItem != null && !isPregnantSelected) {
                    updatedSelection.add(pregnantItem)

                    // Re-set the adapter with updated selection to refresh UI
                    hivViewModel.selectedPopulationType = updatedSelection
                    adapter?.setSelectedItems(updatedSelection, true) // <- You need to expose this method
                    return
                }

                hivViewModel.selectedPopulationType = updatedSelection

                // Show/hide "Other" and "Pregnant" UI parts
                val containsOther = updatedSelection.any {
                    it.name.equals(getString(R.string.other), true)
                }
                binding.viewOtherType.visibleIf(containsOther)
                if (!containsOther) binding.etOtherPopulated.setText("")

                val containsPregnant = updatedSelection.any {
                    it.name.equals(pregnantName, true)
                }
                binding.hivEMTCTViewGroup.visibleIf(containsPregnant)
                if (!containsPregnant) binding.hivEMTCTViewGroup.gone()
            }
        })
    }

    fun View.visibleIf(condition: Boolean) {
        if (condition) this.visibility = View.VISIBLE else this.visibility = View.GONE
    }

    private fun addCustomView(
        data: ArrayList<Map<String, Any>>,
        tag: String,
        hashMap: HashMap<String, Any>,
        callback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)?,
        container: ViewGroup?,
    ) {
        SingleSelectionCustomView(binding.root.context).apply {
            this.tag = tag
            addViewElements(
                data,
                false,
                hashMap,
                Pair(tag, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callback,
            )
            container?.addView(this)
        }
    }

    private fun getData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private var alreadyHIVTestedCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            hivViewModel.resultHashMap[HaveYouTakenHivTestBefore] = selectedID as String
            hivViewModel.resultHashMap[HaveYouTakenHivTestBefore]?.let {
                if (it == getString(R.string.yes)) {
                    binding.tvHaveTestedHIVBeforeError.gone()
                    binding.viewGroupHivTestDuration.visible()
                } else {
                    binding.tvTestingDurationSpinner.setSelection(0, false)
                    hivViewModel.selectedLastTestForHIV = null
                    binding.tvTestingDurationError.gone()
                    binding.viewGroupHivTestDuration.gone()
                }
                resultMapChanged()
            }
        }

    private fun resultMapChanged() {
        setFragmentResult(
            MedicalReviewDefinedParams.HIV_ELIGIBILITY_ITEM,
            bundleOf(
                MedicalReviewDefinedParams.HIV_ELIGIBILITY_VALUES to true,
            ),
        )
    }

    fun validation(): Boolean {
        var isValid = true
        if (binding.etOtherPopulated.isVisible()) {
            if (binding.etOtherPopulated.text.isNullOrEmpty()) {
                isValid = false
                binding.tvOtherError.visible()
            } else {
                binding.tvOtherError.gone()
            }
        }
        return isValid
    }

    private fun showDatePickerDialog(
        textView: AppCompatTextView,
        disableFuture: Boolean = false,
        isLmp: Boolean = false,
        lmp: Boolean,
    ) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!textView.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertedMMMToddMM(textView.text.toString())
        }
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = null,
                date = yearMonthDate,
                isMenstrualPeriod = true,
                disableFutureDate = disableFuture,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                textView.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DATE_FORMAT_ddMMyyyy,
                        DATE_ddMMyyyy,
                    )
                lmpValidation()
                datePickerDialog = null
            }
        }
    }

    private fun lmpValidation() {
        patientViewModel
            .getPregnantDetails()
            ?.lastMenstrualPeriod
            ?.takeIf { it.isNotBlank() }
            ?.let { lmp ->
                binding.apply {
                    tvLastMenstrualPeriodDateLabelText.text = DateUtils.convertDateFormat(
                        lmp,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                }
                calculateGestationalAgeAndEstimationDeliveryDate()
            }

        val isNotEmpty = !binding.tvLastMenstrualPeriodDateLabelText.text.isNullOrEmpty()
        val alphaEnabled = 1.0f
        val alphaDisabled = 0.5f // Faded look
        binding.etGestationalInWeek.isEnabled = isNotEmpty
        binding.etExpectedDateOfDelivery.isEnabled = isNotEmpty
        binding.etGestationalInWeek.alpha = if (isNotEmpty) alphaEnabled else alphaDisabled
        binding.etExpectedDateOfDelivery.alpha = if (isNotEmpty) alphaEnabled else alphaDisabled
        binding.tvGestationalInWeekLabel.alpha = if (isNotEmpty) alphaEnabled else alphaDisabled
        binding.tvExpectedDateOfDeliveryLabel.alpha =
            if (isNotEmpty) alphaEnabled else alphaDisabled

        if (!isNotEmpty) {
            listOf(binding.etGestationalInWeek, binding.etExpectedDateOfDelivery).forEach {
                it.text?.clear()
            }
        } else {
            binding.tvLastMenstrualPeriodDateLabelText.isClickable = false
            binding.etExpectedDateOfDelivery.isClickable = false
            binding.etGestationalInWeek.isClickable = false
            binding.etGestationalInWeek.isEnabled = false
        }
    }

    private fun valueObserver() {
        binding.etOtherPopulated.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                hivViewModel.populationOther = text.toString()
            }
        }
        binding.etGestationalInWeek.apply {
            doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrBlank()) {
//                    expectedDateOfdDelivery()
                    hivViewModel.gestationalWeeks = text.toString()
                }
            }
            filters = arrayOf(
                InputFilter { source, start, end, dest, dstart, dend ->
                    val newValue = dest.substring(0, dstart) + source.subSequence(start, end) + dest.substring(dend)
                    if (newValue.isEmpty()) {
                        return@InputFilter null
                    }

                    try {
                        val value = newValue.toFloat()
                        if (value in 0.0..40.0) null else ""
                    } catch (e: NumberFormatException) {
                        ""
                    }
                },
            )
        }
        binding.tvLastMenstrualPeriodDateLabelText.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                calculateGestationalAgeAndEstimationDeliveryDate()
                hivViewModel.lastMenstrualPeriod = convertToRequiredFormat(text.toString())
            }
        }
    }

    private fun calculateGestationalAgeAndEstimationDeliveryDate() {
        val lmpText = binding.tvLastMenstrualPeriodDateLabelText.text
            .toString()
            .trim()
        if (lmpText.isNotEmpty()) {
            val lmpDate =
                LocalDate.parse(lmpText, DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy))
            val estimatedDeliveryDate = lmpDate.plusDays(MotherNeonateUtil.EstimatedDeliveryDate)
            val formattedEstimatedDeliveryDate =
                estimatedDeliveryDate.format(DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy))
            binding.etExpectedDateOfDelivery.setText(formattedEstimatedDeliveryDate.toString())
            hivViewModel.expectedDateOfDelivery = estimatedDeliveryDate.toString()

            val gestationalAgeInWeeks = DateUtils.calculateGestationalAge(lmpDate)
            binding.etGestationalInWeek.setText(gestationalAgeInWeeks.toString())

            hivViewModel.gestationalWeeks = binding.etGestationalInWeek.text.toString()
        }
    }
}
