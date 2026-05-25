package org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.model.HivStatus
import org.medtroniclabs.uhis.databinding.FragmentHivStatusBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PREGNANCY_MAX_AGE
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PREGNANCY_MIN_AGE
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HIVStatusViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.notEstablished
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PregnancyDetailsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class HIVStatusFragment : BaseFragment() {
    val adapter: CustomSpinnerAdapter by lazy { CustomSpinnerAdapter(requireContext()) }
    private val viewModel: HIVStatusViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var binding: FragmentHivStatusBinding
    private var datePickerDialog: DatePickerDialog? = null
    private val pregnancyDetailsViewModel: PregnancyDetailsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHivStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "HIVStatusFragment"

        fun newInstance() = HIVStatusFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    fun attachObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pregnancyDetailsViewModel.sharedValueLmp.collect { value ->
                    if (!value.isNullOrEmpty()) {
                        binding.tvLastMenstrualPeriodDate.alpha = 0.5f
                        binding.tvLastMenstrualPeriodLabel.alpha = 0.5f
                        binding.tvGestationalAgeLabel.alpha = 0.5f
                        binding.etGestationalAge.alpha = 0.5f
                        binding.tvExpectedDate.alpha = 0.5f
                        binding.etExpectedDate.alpha = 0.5f
                        binding.tvLastMenstrualPeriodDate.isEnabled = false
                        binding.tvLastMenstrualPeriodDate.isClickable = false
                        binding.tvLastMenstrualPeriodDate.text = value
                        calculateGestationalAgeAndEstimationDeliveryDate()
                    }
                }
            }
        }

        viewModel.getHivStatusMetaList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        fun filterAndMap(category: String): ArrayList<Map<String, Any>> =
                            listItems
                                .filter { it.category == category }
                                .map { item ->
                                    CommonUtils.getOptionMap(
                                        value = item.value.orEmpty(),
                                        name = item.name,
                                    )
                                }.toCollection(ArrayList())

                        if (viewModel.isEMTCT) {
                            binding.tbGroup.visible()
                            initTBStatus(listItems)
//                            binding.lmbGroup.gone()
//                            binding.gestationalAgeGroup.gone()
//                            binding.expectedDateGroup.gone()
                        } else {
                            binding.tbGroup.gone()
                        }
                        initPregnancyStatus(filterAndMap(MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name))
                        initAHD(filterAndMap(MedicalReviewTypeEnums.ahdStatus.name))
                        initDSD(filterAndMap(MedicalReviewTypeEnums.dsdStatus.name))
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun getSelectModelMeta(isEstablished: Boolean) {
        val listItems = viewModel.getHivStatusMetaList.value
            ?.data
            .orEmpty()
        val categoryKey = if (isEstablished) {
            MedicalReviewTypeEnums.establishedModels.name
        } else {
            MedicalReviewTypeEnums.nonEstablishedModels.name
        }
        val dropDownList = buildList {
            add(
                mapOf(
                    DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                    DefinedParams.Value to DefinedParams.DEFAULT_ID,
                ),
            )
            addAll(
                listItems
                    .filter { it.category == categoryKey }
                    .map { item ->
                        mapOf(
                            DefinedParams.NAME to item.name,
                            DefinedParams.Value to (item.value ?: item.name),
                        )
                    },
            )
        }
        setSpinner(ArrayList(dropDownList))
    }

    private fun initAHD(data: ArrayList<Map<String, Any>>?) {
        if (data.isNullOrEmpty()) return // Exit if null or empty

        val view = SingleSelectionCustomView(requireContext()).apply {
            tag = MedicalReviewTypeEnums.ahdStatus.name
            addViewElements(
                data,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultAHD,
                Pair(MedicalReviewTypeEnums.ahdStatus.name, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionAHDCallback,
            )
        }

        binding.llAHD.addView(view)
    }

    private fun initDSD(data: ArrayList<Map<String, Any>>?) {
        if (data.isNullOrEmpty()) return // Exit if null or empty

        val view = SingleSelectionCustomView(requireContext()).apply {
            tag = MedicalReviewTypeEnums.dsdStatus.name
            addViewElements(
                data,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultDSD,
                Pair(MedicalReviewTypeEnums.dsdStatus.name, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionDSDCallback,
            )
        }

        binding.llDSD.addView(view)
    }

    private fun initView() {
        viewModel.isEMTCT = arguments?.getBoolean(DefinedParams.EMTCTMR, false) == true
        viewModel.getHivStatusMeta(MedicalReviewTypeEnums.HIV.name)
        binding.etSelectModel.setVisible(false)
        binding.tvSelectModelLabel.setVisible(false)
        binding.tvSelectModelError.setVisible(false)
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

        // Show pregnancy group only if gender is female AND eligible age
        binding.pregnancyStatusGroup.setVisible(isEligibleAge)
        binding.tvLastMenstrualPeriodDate.isEnabled = true
        binding.tvLastMenstrualPeriodDate.safeClickListener {
            showDatePickerDialog(binding.tvLastMenstrualPeriodDate)
        }
        autoPopulatePregnantDetails()
    }

    private fun autoPopulatePregnantDetails() {
        if (patientViewModel
                .getPregnancyBreastFeedStatus()
                ?.equals(DefinedParams.yes, true) == true
        ) {
            viewModel.resultPregnantStatus[MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name] =
                DefinedParams.yes
            binding.lmbGroup.setVisible(true)
            binding.gestationalAgeGroup.setVisible(true)
            binding.expectedDateGroup.setVisible(true)
        } else if (patientViewModel
                .getPregnancyBreastFeedStatus()
                ?.equals(DefinedParams.NO, true) == true
        ) {
            viewModel.resultPregnantStatus[MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name] =
                DefinedParams.NO
        } else if (patientViewModel
                .getPregnancyBreastFeedStatus()
                ?.equals(DefinedParams.NOT_APPLICABLE, true) == true
        ) {
            viewModel.resultPregnantStatus[MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name] =
                DefinedParams.NOT_APPLICABLE
        }

        patientViewModel
            .getPregnantDetails()
            ?.lastMenstrualPeriod
            ?.takeIf { it.isNotBlank() }
            ?.let { lmp ->
                binding.apply {
                    tvLastMenstrualPeriodDate.text = DateUtils.convertDateFormat(
                        lmp,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                    lmpVisibility()
//                    tvLastMenstrualPeriodDate.isEnabled = false
                }
                calculateGestationalAgeAndEstimationDeliveryDate()
            }
    }

    private fun initPregnancyStatus(data: ArrayList<Map<String, Any>>?) {
        if (data.isNullOrEmpty()) return // Exit if null or empty

        val view = SingleSelectionCustomView(requireContext()).apply {
            tag = MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name
            addViewElements(
                data,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultPregnantStatus,
                Pair(MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback,
            )
        }

        binding.llPregnancyAndBreastFeedingStatus.addView(view)
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultPregnantStatus[MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name] =
                selectedID as? String ?: ""
            val isValid = (selectedID as? String).equals(DefinedParams.yes, ignoreCase = true)
            showPregnantRelatedViews(isValid)
            setFragmentResult(
                MedicalReviewDefinedParams.HIV_STATUS,
                bundleOf(
                    MedicalReviewDefinedParams.CHIP_ITEMS to true,
                ),
            )
        }

    private fun showPregnantRelatedViews(isValid: Boolean) {
        binding.lmbGroup.setVisible(isValid)
        binding.gestationalAgeGroup.setVisible(isValid)
        binding.expectedDateGroup.setVisible(isValid)
        lmpVisibility()
        calculateGestationalAgeAndEstimationDeliveryDate()
    }

    private fun lmpVisibility() {
        val isNotEmpty = !binding.tvLastMenstrualPeriodDate.text.isNullOrEmpty()

        if (isNotEmpty) {
            binding.tvLastMenstrualPeriodDate.isClickable = false
            binding.etExpectedDate.isClickable = false
            binding.etGestationalAge.isClickable = false
            binding.etGestationalAge.isEnabled = false
        }
    }

    private fun showDatePickerDialog(textView: AppCompatTextView) {
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
                disableFutureDate = true,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                textView.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy,
                    )
                calculateGestationalAgeAndEstimationDeliveryDate()
                setFragmentResult(
                    MedicalReviewDefinedParams.HIV_STATUS,
                    bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true,
                    ),
                )
                datePickerDialog = null
            }
        }
    }

    private fun calculateGestationalAgeAndEstimationDeliveryDate() {
        val lmpText = binding.tvLastMenstrualPeriodDate.text
            .toString()
            .trim()
        if (lmpText.isNotEmpty()) {
            val lmpDate =
                LocalDate.parse(lmpText, DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy))
            val estimatedDeliveryDate = lmpDate.plusDays(MotherNeonateUtil.EstimatedDeliveryDate)
            val formattedEstimatedDeliveryDate =
                estimatedDeliveryDate.format(DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy))
            binding.etExpectedDate.text = formattedEstimatedDeliveryDate
            val gestationalAgeInWeeks = DateUtils.calculateGestationalAge(lmpDate)
            binding.etGestationalAge.text =
                DateUtils.formatGestationalAge(gestationalAgeInWeeks, requireContext())
        }
    }

    private var singleSelectionAHDCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultAHD[MedicalReviewTypeEnums.ahdStatus.name] =
                selectedID as? String ?: ""
            setFragmentResult(
                MedicalReviewDefinedParams.HIV_STATUS,
                bundleOf(
                    MedicalReviewDefinedParams.CHIP_ITEMS to true,
                ),
            )
        }

    private var singleSelectionDSDCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            val selectedValue = selectedID as? String ?: ""
            val currentValue = viewModel.resultDSD[MedicalReviewTypeEnums.dsdStatus.name] as? String ?: ""
            viewModel.resultDSD[MedicalReviewTypeEnums.dsdStatus.name] = selectedValue
            val isValid = selectedValue.equals(notEstablished, ignoreCase = true)
            if (!selectedValue.equals(currentValue, ignoreCase = true)) {
                showModel(isValid)
            }
            setFragmentResult(
                MedicalReviewDefinedParams.HIV_STATUS,
                bundleOf(
                    MedicalReviewDefinedParams.CHIP_ITEMS to true,
                ),
            )
        }

    private fun showModel(isValid: Boolean) {
        binding.tvSelectModelLabel.setVisible(true)
        binding.etSelectModel.setVisible(true)
        binding.tvSelectModelError.setVisible(false)
        viewModel.selectModel = null
        getSelectModelMeta(!isValid)
    }

    private fun setSpinner(statusList: ArrayList<Map<String, Any>>) {
        adapter.setData(statusList)
        binding.etSelectModel.adapter = adapter
        val defaultPosition = 0
        binding.etSelectModel.post {
            binding.etSelectModel.setSelection(defaultPosition, false)
        }
        binding.etSelectModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(pos)
                    val selectedModel = selectedItem?.get(DefinedParams.Value) as? String
                    viewModel.selectModel =
                        if (!selectedModel.isNullOrEmpty() &&
                            !selectedModel.equals(
                                DefinedParams.DEFAULT_ID,
                                ignoreCase = true,
                            )
                        ) {
                            selectedModel
                        } else {
                            null
                        }
                    setFragmentResult(
                        MedicalReviewDefinedParams.HIV_STATUS,
                        bundleOf(MedicalReviewDefinedParams.CHIP_ITEMS to true),
                    )
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    fun validateInput(): Boolean {
        val isValidPregnantStatus = viewModel.resultPregnantStatus.isNotEmpty()
        val isValidAHD = viewModel.resultAHD.isNotEmpty()
        val isValidDSD = viewModel.resultDSD.isNotEmpty()
        val isLMB = binding.tvLastMenstrualPeriodDate.text
            .toString()
            .trim()
            .isNotBlank()
        val isSelectModel = viewModel.selectModel != DefinedParams.DEFAULT_ID && viewModel.selectModel != null
        val isTBStatus = viewModel.tbStatus != DefinedParams.DEFAULT_ID && viewModel.tbStatus != null
        return isValidPregnantStatus || isValidAHD || isValidDSD || isLMB || isSelectModel || isTBStatus
    }

    fun getRequest(): HivStatus {
        val lmpText = binding.tvLastMenstrualPeriodDate.text
            .toString()
            .trim()
        val expectedDateText = binding.etExpectedDate.text
            .toString()
            .trim()

        val lmpDate = lmpText.takeIf { it.isNotBlank() }?.let {
            LocalDate.parse(it, DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy))
        }

        val lastMenstrualPeriodFormatted = DateUtils
            .convertDateTimeToDate(
                lmpText.takeIf { it.isNotBlank() },
                DateUtils.DATE_ddMMyyyy,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            ).takeIf { it.isNotBlank() }

        val expectedDeliveryDateFormatted = DateUtils
            .convertDateTimeToDate(
                expectedDateText.takeIf { it.isNotBlank() },
                DateUtils.DATE_ddMMyyyy,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            ).takeIf { it.isNotBlank() }

        return HivStatus(
            pregnancyBreastfeedStatus = viewModel.resultPregnantStatus[MedicalReviewTypeEnums.hivPreganancyBreastFeedingStatus.name] as? String,
            ahdStatus = viewModel.resultAHD[MedicalReviewTypeEnums.ahdStatus.name] as? String,
            dsdStatus = viewModel.resultDSD[MedicalReviewTypeEnums.dsdStatus.name] as? String,
            model = viewModel.selectModel,
            lastMenstrualPeriod = lastMenstrualPeriodFormatted,
            gestationalInWeeks = lmpDate?.let { DateUtils.calculateGestationalAge(it) },
            expectedDateOfDelivery = expectedDeliveryDateFormatted,
            tbStatus = viewModel.tbStatus,
        )
    }

    private fun initTBStatus(data: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            mapOf(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.Value to DefinedParams.DEFAULT_ID,
            ),
        )
        dropDownList.addAll(
            data
                .filter { it.category == MedicalReviewTypeEnums.tbStatus.name }
                .map { item ->
                    mapOf(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to (item.value ?: item.name),
                    )
                },
        )
        if (dropDownList.isNotEmpty()) {
            adapter.setData(dropDownList)
            binding.etTBStatus.adapter = adapter
            val defaultPosition = 0
            binding.etTBStatus.post {
                binding.etTBStatus.setSelection(defaultPosition, false)
            }
            binding.etTBStatus.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        pos: Int,
                        itemId: Long,
                    ) {
                        val selectedItem = adapter.getData(position = pos)
                        selectedItem?.let {
                            var selectedModel = it[DefinedParams.Value] as String?
                            if (selectedModel == DefinedParams.DEFAULT_ID) {
                                selectedModel = null
                            }
                            selectedModel?.let {
                                viewModel.tbStatus = selectedModel
                            } ?: kotlin.run {
                                viewModel.tbStatus = null
                            }
                            setFragmentResult(
                                MedicalReviewDefinedParams.HIV_STATUS,
                                bundleOf(
                                    MedicalReviewDefinedParams.CHIP_ITEMS to true,
                                ),
                            )
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        /**
                         * this method is not used
                         */
                    }
                }
        }
    }
}
