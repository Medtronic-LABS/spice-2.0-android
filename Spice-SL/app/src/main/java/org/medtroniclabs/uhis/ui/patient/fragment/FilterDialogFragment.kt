package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.FilterEnum
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.CustomDate
import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.databinding.FragmentFilterDialogBinding
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.patient.FilterSortInterface
import org.medtroniclabs.uhis.ui.patient.util.ViewUtil
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientListViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class FilterDialogFragment() : DialogFragment() {
    private val viewModel: PatientListViewModel by activityViewModels()
    private var filterModel = FilterModel()

    private var unionsAdapter: CustomSpinnerAdapter? = null
    private var villagesAdapter: CustomSpinnerAdapter? = null

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    var filterSortInterface: FilterSortInterface? = null

    private lateinit var binding: FragmentFilterDialogBinding

    companion object {
        const val TAG = "CommonFilterDialogFragment"

        fun newInstance(): FilterDialogFragment = FilterDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFilterDialogBinding.inflate(inflater, container, false)
        isCancelable = false
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        adjustDialogSize()
    }

    private fun adjustDialogSize() {
        if (CommonUtils.checkIsTablet(requireContext())) {
            setDialogPercent(80, getHeightPercentage())
        } else {
            setDialogPercent(95, getHeightPercentage())
        }
    }

    private fun getHeightPercentage(): Int {
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            75 // Adjusted height for landscape
        } else {
            70
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustDialogSize() // Reapply dialog size on rotation
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setObservers()
    }

    private fun initViews() {
        binding.apply {
            labelHeader.ivClose.safeClickListener(onClickListener)
            btnApply.safeClickListener(onClickListener)
            btnReset.safeClickListener(onClickListener)
            // touchView.setOnTouchListener(touchListener)
        }
        coreFunction(DefinedParams.FILTER_INIT_PREFILL)
    }

    private fun setObservers() {
        viewModel.unionListResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    loadUnionDetails(resourceState.data)
                }

                else -> {
                    // Else block
                }
            }
        }
        viewModel.villageListResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    loadVillageDetails(resourceState.data)
                }

                else -> {
                    // Else block
                }
            }
        }
    }

    private fun loadUnionDetails(data: List<VillageEntity>?) {
        val list = ArrayList<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )
        data?.forEach { site ->
            convertUnionModelToMap(site)?.let {
                list.add(it)
            }
        }
        unionsAdapter?.setData(list)
        binding.lUnionVillage.spinnerUnion.adapter = unionsAdapter

        filterModel.villageId?.let { unionId ->
            list.let { list ->
                val index = list.indexOfFirst { map ->
                    map.containsKey(DefinedParams.ID) &&
                        map[DefinedParams.ID] != null &&
                        CommonUtils.convertType(
                            map[DefinedParams.ID],
                        ) == unionId
                }
                binding.lUnionVillage.spinnerUnion.setSelection(index, true)
            }
        }
    }

    private fun loadVillageDetails(data: List<SubVillageEntity>?) {
        val list = ArrayList<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )
        data?.forEach { village ->
            convertUnionModelToMap(village)?.let {
                list.add(it)
            }
        }
        villagesAdapter?.setData(list)
        binding.lUnionVillage.apply {
            spinnerVillage.adapter = villagesAdapter
            grpVillage.visibility = View.VISIBLE
        }

        filterModel.subVillageId?.let { villageId ->
            list.let { list ->
                val index = list.indexOfFirst { map ->
                    map.containsKey(DefinedParams.ID) &&
                        map[DefinedParams.ID] != null &&
                        CommonUtils.convertType(
                            map[DefinedParams.ID],
                        ) == villageId
                }
                binding.lUnionVillage.spinnerVillage.setSelection(index, true)
                filterModel.villagePreFilled = true
            }
        }
    }

    private fun convertUnionModelToMap(village: Any): Map<String, Any>? {
        val gson = Gson()
        val json = gson.toJson(village)
        return StringConverter.convertStringToMap(json)
    }

    private fun coreFunction(action: String) {
        filterModel = viewModel.filter ?: return

        val role = SecuredPreference
            .getUserDetails()
            ?.roles
            ?.first()
            ?.name ?: ""

        when (filterModel.origin) {
            MenuConstants.MY_PATIENTS_MENU_ID.lowercase() -> {
                filterTypeA(role, action)
            }
        }
    }

    private fun filterTypeA(
        role: String?,
        action: String,
    ) {
        when (role) {
            RoleConstant.NURSE -> {
                when (action) {
                    DefinedParams.FILTER_INIT_PREFILL -> {
                        // Registration Date
                        binding.lRegistrationDate.apply {
                            root.visibility = View.VISIBLE

                            todayRegistrationChip.setOnCheckedChangeListener(chipListener)
                            yesterdayRegistrationChip.setOnCheckedChangeListener(chipListener)
                            weeklyRegistrationChip.setOnCheckedChangeListener(chipListener)
                            monthlyRegistrationChip.setOnCheckedChangeListener(chipListener)
                            customizeRegistrationChip.setOnCheckedChangeListener(chipListener)

                            customizeChipView(todayRegistrationChip)
                            customizeChipView(yesterdayRegistrationChip)
                            customizeChipView(weeklyRegistrationChip)
                            customizeChipView(monthlyRegistrationChip)
                            customizeChipView(customizeRegistrationChip)

                            tvRegistrationFromDate.safeClickListener(onClickListener)
                            tvRegistrationToDate.safeClickListener(onClickListener)

                            tvRegistrationFromDate.addTextChangedListener(fromToTextWatcher)
                            tvRegistrationToDate.addTextChangedListener(fromToTextWatcher)

                            filterModel.let {
                                todayRegistrationChip.isChecked = it.registrationDate == FilterEnum.TODAY.title
                                yesterdayRegistrationChip.isChecked = it.registrationDate == FilterEnum.YESTERDAY.title
                                weeklyRegistrationChip.isChecked = it.registrationDate == FilterEnum.WEEK.title
                                monthlyRegistrationChip.isChecked = it.registrationDate == FilterEnum.MONTH.title
                                customizeRegistrationChip.isChecked =
                                    filterModel.customRegistrationDate?.startDate != null &&
                                    filterModel.customRegistrationDate?.endDate != null
                                it.customRegistrationDate?.let { customRegistrationDate ->
                                    customRegistrationDate.startDate?.let { fromRegistrationDate ->
                                        tvRegistrationFromDate.text = DateUtils.convertDateTimeToDate(
                                            fromRegistrationDate,
                                            DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                                            DateUtils.DATE_FORMAT_ddMMMyyyy,
                                        )
                                        tvRegistrationToDate.isEnabled = true
                                    }
                                    customRegistrationDate.endDate?.let { toRegistrationDate ->
                                        tvRegistrationToDate.text = DateUtils.convertDateTimeToDate(
                                            toRegistrationDate,
                                            DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                                            DateUtils.DATE_FORMAT_ddMMMyyyy,
                                        )
                                    }
                                }
                            }
                        }

                        // Health Condition
//                        traning not yet completed
                        binding.lHealthCondition.apply {
                            root.visibility = View.VISIBLE

                            hypertensionChip.setOnCheckedChangeListener(chipListener)
                            diabetesChip.setOnCheckedChangeListener(chipListener)
                            bothChip.setOnCheckedChangeListener(chipListener)

                            customizeChipView(hypertensionChip)
                            customizeChipView(diabetesChip)
                            customizeChipView(bothChip)

                            filterModel.let {
                                hypertensionChip.isChecked = it.healthCondition == FilterEnum.HYPERTENSION.title
                                diabetesChip.isChecked = it.healthCondition == FilterEnum.DIABETES.title
                                bothChip.isChecked = it.healthCondition == FilterEnum.BOTH.title
                            }
                        }

                        // Union and Village
                        binding.lUnionVillage.apply {
                            root.visibility = View.VISIBLE

                            unionsAdapter = CustomSpinnerAdapter(requireContext())
                            villagesAdapter = CustomSpinnerAdapter(requireContext())

                            spinnerUnion.onItemSelectedListener = itemSelectedListener
                            spinnerVillage.onItemSelectedListener = itemSelectedListener

                            viewModel.getAllUnionList()
                        }
                    }

                    DefinedParams.FILTER_EQUALITY_CHECK, DefinedParams.FILTER_APPLY -> {
                        val customRegistrationDate = getRegistrationCustomDate()
                        val requestModel = FilterModel(
                            villageId = getUnionId(),
                            subVillageId = getUnionVillageId(),
                            dateRange = null,
                            customRegistrationOption = null,
                            customRegistrationDate = customRegistrationDate,
                            selectedParaCounselor = null,
                            sessionDate = null,
                            patientStatus = null,
                            registrationDate = getRegistrationDate(),
                            healthCondition = getHealthCondition(),
                            paraCounsellingStatus = null,
                            isDefaultPcFilter = true,
                        )
                        requestModel.apply {
                            isFromPcFilter = if (paraCounsellingStatus != null) true else null
                        }
                        enableApplyBtn(requestModel, action == DefinedParams.FILTER_APPLY)
                    }

                    DefinedParams.FILTER_RESET -> {
                        binding.apply {
                            lUnionVillage.spinnerUnion.setSelection(0, true)
                            lRegistrationDate.registrationDateChipGroup.clearCheck()
                            lHealthCondition.healthConditionChipGroup.clearCheck()
                        }
                    }
                }
            }
        }
    }

    private fun getRegistrationDate(): String? =
        when (binding.lRegistrationDate.registrationDateChipGroup.checkedChipId) {
            R.id.todayRegistrationChip -> FilterEnum.TODAY.title
            R.id.yesterdayRegistrationChip -> FilterEnum.YESTERDAY.title
            R.id.weeklyRegistrationChip -> FilterEnum.WEEK.title
            R.id.monthlyRegistrationChip -> FilterEnum.MONTH.title
            else -> null
        }

    private fun getHealthCondition(): String? =
        when (binding.lHealthCondition.healthConditionChipGroup.checkedChipId) {
            R.id.hypertensionChip -> FilterEnum.HYPERTENSION.title
            R.id.diabetesChip -> FilterEnum.DIABETES.title
            R.id.bothChip -> FilterEnum.BOTH.title
            else -> null
        }

    private fun getUnionId(): Long? {
        val unionId: Long?
        val pos = binding.lUnionVillage.spinnerUnion.selectedItemPosition
        unionId = if (pos < 0) {
            null
        } else {
            val id = unionsAdapter
                ?.getData(binding.lUnionVillage.spinnerUnion.selectedItemPosition)
                ?.let {
                    getItemId(it)
                }
            if (id == DefinedParams.DEFAULT_SELECT_ID) null else id
        }
        if (unionId == null) {
            viewModel.isFilteredUnion = true
        } else {
            viewModel.isFilteredUnion = false
        }
        return unionId
    }

    private fun getUnionVillageId(): Long? {
        val unionVillageId: Long?
        val pos = binding.lUnionVillage.spinnerVillage.selectedItemPosition
        unionVillageId = if (pos < 0) {
            null
        } else {
            val id =
                villagesAdapter
                    ?.getData(binding.lUnionVillage.spinnerVillage.selectedItemPosition)
                    ?.let {
                        getItemId(it)
                    }
            if (id == DefinedParams.DEFAULT_SELECT_ID) null else id
        }
        return unionVillageId
    }

    private fun isCustomRegistrationOption(): Boolean? = if (binding.lRegistrationDate.customizeRegistrationChip.isChecked) true else null

    private fun getRegistrationCustomDate(): CustomDate? =
        if (isCustomRegistrationOption() == true) {
            val startDate = DateUtils.convertDateTimeToDate(
                binding.lRegistrationDate.tvRegistrationFromDate.text
                    .toString(),
                DateUtils.DATE_FORMAT_ddMMMyyyy,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                inUTC = true,
            )
            var endDate: String? = null
            binding.lRegistrationDate.tvRegistrationToDate.text.let {
                if (!it.isNullOrBlank()) {
                    val formattedEndDate =
                        DateUtils.changeFormat(
                            binding.lRegistrationDate.tvRegistrationToDate.text
                                .toString(),
                        )
                    endDate = DateUtils.getEndDate(
                        formattedEndDate,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        inUTC = true,
                    )
                }
            }

            CustomDate(startDate = startDate, endDate = endDate)
        } else {
            null
        }

    private val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long,
        ) {
            when (parent?.id) {
                R.id.spinnerUnion -> {
                    if (position != 0) {
                        unionsAdapter?.getData(position)?.let {
                            viewModel.fetchVillageList(getItemId(it))
                        }
                    } else {
                        binding.lUnionVillage.apply {
                            spinnerVillage.adapter = null
                            grpVillage.visibility = View.GONE
                        }
                    }
                }

                R.id.spinnerVillage -> {
                    //
                }
            }
            enableApplyBtn()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // Not used
        }
    }

    private fun getItemId(selectedItem: Map<String, Any>): Long =
        if (selectedItem.containsKey(DefinedParams.ID)) {
            CommonUtils.convertType(
                selectedItem[DefinedParams.ID],
            )
        } else {
            DefinedParams.DEFAULT_SELECT_ID
        }

    private val fromToTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) {
        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int,
        ) {}

        override fun afterTextChanged(s: Editable?) {
            enableApplyBtn()
        }
    }
    private val chipListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        binding.apply {
            when (buttonView?.id) {
                lRegistrationDate.todayRegistrationChip.id -> changeChipView(
                    binding.lRegistrationDate.todayRegistrationChip,
                    isChecked,
                )

                lRegistrationDate.yesterdayRegistrationChip.id -> changeChipView(
                    binding.lRegistrationDate.yesterdayRegistrationChip,
                    isChecked,
                )

                lRegistrationDate.weeklyRegistrationChip.id -> changeChipView(
                    binding.lRegistrationDate.weeklyRegistrationChip,
                    isChecked,
                )

                lRegistrationDate.monthlyRegistrationChip.id -> changeChipView(
                    binding.lRegistrationDate.monthlyRegistrationChip,
                    isChecked,
                )

                lRegistrationDate.customizeRegistrationChip.id -> {
                    handleRDCustomizedView(isChecked)
                    changeChipView(binding.lRegistrationDate.customizeRegistrationChip, isChecked)
                }

                lHealthCondition.hypertensionChip.id -> changeChipView(binding.lHealthCondition.hypertensionChip, isChecked)
                lHealthCondition.diabetesChip.id -> changeChipView(binding.lHealthCondition.diabetesChip, isChecked)
                lHealthCondition.bothChip.id -> changeChipView(binding.lHealthCondition.bothChip, isChecked)
            }
        }
        enableApplyBtn()
    }

    private fun handleRDCustomizedView(isChecked: Boolean) {
        binding.lRegistrationDate.apply {
            tvRegistrationFromDate.text = ""
            tvRegistrationToDate.text = ""
            tvRegistrationToDate.isEnabled = false
            clFromToDate.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun changeChipView(
        chip: Chip,
        checked: Boolean,
    ) {
        if (checked) {
            chip.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
            chip.chipStrokeWidth = 0f
        } else {
            chip.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            chip.chipStrokeWidth = 3f
        }
    }

    private fun customizeChipView(chip: Chip) {
        requireContext().let { context ->
            chip.chipBackgroundColor = getColorStateList(
                context.getColor(R.color.medium_blue),
                context.getColor(R.color.white),
            )
            chip.setChipBackgroundColorResource(R.color.diagnosis_confirmation_selector)
            chip.chipStrokeWidth = 3f
            chip.setTextColor(
                getColorStateList(
                    context.getColor(R.color.white),
                    context.getColor(R.color.navy_blue),
                ),
            )
            chip.chipStrokeColor = getColorStateList(
                context.getColor(R.color.medium_blue),
                context.getColor(R.color.mild_gray),
            )
        }
    }

    private fun getColorStateList(
        selectedColor: Int,
        unSelectedColor: Int,
    ): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(-android.R.attr.state_selected),
        )
        val colors = intArrayOf(
            selectedColor,
            unSelectedColor,
            selectedColor,
            unSelectedColor,
        )
        return ColorStateList(states, colors)
    }

    private fun enableApplyBtn() {
        binding.apply {
            val customRegistrationDateRange =
                if (binding.lRegistrationDate.clFromToDate.isVisible) {
                    (
                        !binding.lRegistrationDate.tvRegistrationFromDate.text
                            .isNullOrBlank() &&
                            !binding.lRegistrationDate.tvRegistrationToDate.text
                                .isNullOrBlank()
                    )
                } else {
                    (
                        lRegistrationDate.registrationDateChipGroup.checkedChipId != -1 &&
                            lRegistrationDate.registrationDateChipGroup.checkedChipId != R.id.customizeRegistrationChip
                    )
                }

            val anySpinnerSelected = (
                lUnionVillage.spinnerUnion.selectedItem != null &&
                    lUnionVillage.spinnerUnion.selectedItem.toString() != DefinedParams.DEFAULT_ID_LABEL
            )

            val hasInputs = customRegistrationDateRange || anySpinnerSelected
            coreFunction(DefinedParams.FILTER_EQUALITY_CHECK)

            btnReset.isEnabled = hasInputs
        }
    }

    private fun enableApplyBtn(
        requestModel: FilterModel,
        isSubmit: Boolean,
    ) {
        val preFillModel = getModifiedModel(filterModel)
        val currentModel = getModifiedModel(requestModel)
        binding.btnApply.isEnabled =
            preFillModel != currentModel ||
            validateCustomDate(filterModel, requestModel) ||
            validateRegistrationCustomDate(filterModel, requestModel)

        if (isSubmit) {
            filterSortInterface?.filter(requestModel)
            dismiss()
        }
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        val parent = parentFragment
        if (parent is FilterSortInterface) {
            filterSortInterface = parent
        } else if (context is FilterSortInterface) {
            filterSortInterface = context
        } else {
            throw RuntimeException()
        }
    }

    private fun validateRegistrationCustomDate(
        filterModel: FilterModel,
        requestModel: FilterModel,
    ): Boolean {
        return if (binding.lRegistrationDate.customizeRegistrationChip.isChecked) {
            val hasValidDate =
                !requestModel.customRegistrationDate?.startDate.isNullOrBlank() && !requestModel.customRegistrationDate?.endDate.isNullOrBlank()
            if (hasValidDate) {
                checkDate(
                    filterModel.customRegistrationDate,
                    requestModel.customRegistrationDate,
                )
            } else {
                return false
            }
        } else {
            checkDate(filterModel.customRegistrationDate, requestModel.customRegistrationDate)
        }
    }

    private fun validateCustomDate(
        filterModel: FilterModel,
        requestModel: FilterModel,
    ): Boolean = checkDate(filterModel.customDate, requestModel.customDate)

    private fun checkDate(
        preFillDate: CustomDate?,
        requestDate: CustomDate?,
    ): Boolean {
        val startDate1 = preFillDate?.startDate
        val endDate1 = preFillDate?.endDate

        val startDate2 = requestDate?.startDate
        val endDate2 = requestDate?.endDate

        val isNew =
            startDate1.isNullOrBlank() && endDate1.isNullOrBlank() && !startDate2.isNullOrBlank() && !endDate2.isNullOrBlank()

        val isClear =
            !startDate1.isNullOrBlank() && !endDate1.isNullOrBlank() && startDate2.isNullOrBlank() && endDate2.isNullOrBlank()

        if (isNew || isClear) return true

        val dataOnBoth =
            !startDate1.isNullOrBlank() && !startDate2.isNullOrBlank() && !endDate1.isNullOrBlank() && !endDate2.isNullOrBlank()

        if (dataOnBoth) return startDate1 != startDate2 || endDate1 != endDate2

        return false
    }

    private fun getModifiedModel(requestModel: FilterModel): FilterModel =
        requestModel.copy(
            origin = null,
            selectedTab = null,
            screeningReferral = null,
            villagePreFilled = null,
            customOption = null,
            customDate = null,
        )

    private val onClickListener = View.OnClickListener { v ->
        when (v?.id) {
            R.id.ivClose -> dismiss()
            R.id.btnApply -> coreFunction(DefinedParams.FILTER_APPLY)
            R.id.btnReset -> coreFunction(DefinedParams.FILTER_RESET)
            R.id.tvRegistrationFromDate -> showDatePickerDialog(
                true,
                binding.lRegistrationDate.tvRegistrationFromDate.text
                    ?.toString(),
                binding.lRegistrationDate.tvRegistrationFromDate,
                binding.lRegistrationDate.tvRegistrationToDate,
            )

            R.id.tvRegistrationToDate -> showDatePickerDialog(
                false,
                binding.lRegistrationDate.tvRegistrationToDate.text
                    ?.toString(),
                binding.lRegistrationDate.tvRegistrationFromDate,
                binding.lRegistrationDate.tvRegistrationToDate,
            )
        }
    }

    private fun showDatePickerDialog(
        isFromDate: Boolean,
        text: String?,
        tvFromDate: AppCompatTextView,
        tvToDate: AppCompatTextView,
    ) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!text.isNullOrBlank()) {
            yearMonthDate = DateUtils.getYearMonthAndDate(text, DateUtils.getDateDDMMMYYYY())
        }
        val minMaxDate = getMinDate(isFromDate, tvFromDate, tvToDate)
        ViewUtil.showDatePicker(
            context = requireContext(),
            maxDate = minMaxDate.second,
            minDate = minMaxDate.first,
            date = yearMonthDate,
            cancelCallBack = { },
        ) { _, year, month, dayOfMonth ->
            DateUtils
                .convertDateFormat(
                    "$dayOfMonth/$month/$year",
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_ddMMMyyyy,
                )?.let { stringDate ->
                    if (isFromDate) {
                        tvFromDate.text = stringDate
                        tvToDate.isEnabled = true
                    } else {
                        tvToDate.text = stringDate
                    }
                }
        }
    }

    private fun getMinDate(
        isFromDate: Boolean,
        tvFromDate: AppCompatTextView,
        tvToDate: AppCompatTextView,
    ): Pair<Long?, Long?> {
        val fromDate = tvFromDate.text?.toString()
        val toDate = tvToDate.text?.toString()
        val minCalenderInstance = Calendar.getInstance()
        minCalenderInstance.set(2000, Calendar.JANUARY, 1)
        return if (isFromDate) {
            if (!toDate.isNullOrBlank()) {
                Pair(
                    minCalenderInstance.timeInMillis,
                    DateUtils.convertDateToLong(toDate, DateUtils.DATE_FORMAT_ddMMMyyyy),
                )
            } else {
                Pair(minCalenderInstance.timeInMillis, System.currentTimeMillis())
            }
        } else {
            if (!fromDate.isNullOrBlank()) {
                Pair(
                    DateUtils.convertDateToLong(fromDate, DateUtils.DATE_FORMAT_ddMMMyyyy),
                    System.currentTimeMillis(),
                )
            } else {
                Pair(minCalenderInstance.timeInMillis, System.currentTimeMillis())
            }
        }
    }
}
