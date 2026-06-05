package org.medtroniclabs.uhis.ui.patient.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.data.model.SortModel
import org.medtroniclabs.uhis.data.registration.CustomDateModel
import org.medtroniclabs.uhis.data.registration.UserDashboardRequest
import org.medtroniclabs.uhis.databinding.NurseDashboardFragmentBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.patient.ActivityEnum
import org.medtroniclabs.uhis.ui.patient.FilterSortInterface
import org.medtroniclabs.uhis.ui.patient.util.ViewUtil
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseDashboardViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.toString

class NurseDashboardFragment : Fragment(), View.OnClickListener, FilterSortInterface {
    private lateinit var binding: NurseDashboardFragmentBinding
    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var activityTagListCustomView: TagListCustomView
    private val viewModel: NurseDashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = NurseDashboardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        attachObservers()
        setFilterListener()
    }

    private fun initViews() {
        loadActivityList()
        handleViews()
        binding.etTo.safeClickListener(this)
        binding.etFrom.safeClickListener(this)
        updateFilterButtonText()
    }

    private fun updateFilterButtonText() {
        val filterCount = viewModel.filterCount()
        val filter = getString(R.string.filter)
        if (filterCount > 0) {
            binding.btnFilter.text = "$filter ($filterCount)"
        } else {
            binding.btnFilter.text = filter
        }
    }

    private fun handleViews() {
        val role = SecuredPreference.getRole()
        binding.apply {
            when (role) {
                RoleConstant.NURSE -> {
                    cardEnrolled.visibility = View.VISIBLE
                    cardScreening.visibility = View.VISIBLE
                    cardReferred.visibility = View.VISIBLE
                    cardLinkedToCare.visibility = View.GONE
                    cardAssessed.visibility = View.VISIBLE
                }

                else -> {
                    cardScreening.visibility = View.GONE
                    cardReferred.visibility = View.GONE
                    cardEnrolled.visibility = View.GONE
                    cardAssessed.visibility = View.GONE

                    cardLinkedToCare.visibility = View.GONE
                }
            }
        }
    }

    private fun setFilterListener() {
        binding.btnFilter.safeClickListener {
            viewModel.filter = filterModel()
            FilterDialogFragment
                .newInstance()
                .show(childFragmentManager, FilterDialogFragment.TAG)
        }
    }

    private fun filterModel(): FilterModel = viewModel.filter?.copy(origin = viewModel.origin) ?: FilterModel(origin = viewModel.origin)

    private fun attachObservers() {
        viewModel.userDashboardDetails.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    binding.tvScreening.text = getString(R.string.screened)
                    resourceState.data?.let {
                        binding.apply {
                            tvScreeningValue.text = "${it.screened}"
                            tvReferredValue.text = "${it.referred}"
                            tvEnrolledValue.text = "${it.registered}"
                            tvAssessedValue.text = "${it.assessed}"
                        }
                    }
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    fun showLoading() {
        (requireActivity() as BaseActivity).showLoading()
    }

    fun hideLoading() {
        (requireActivity() as BaseActivity).hideLoading()
    }

    private fun loadActivityList() {
        activityTagListCustomView = TagListCustomView(
            requireContext(),
            binding.cgActivity,
            isSelectionRequired = true,
            otherSingleSelect = true,
        ) { _, _, isChecked ->
            if (isChecked) getDashboardList()
        }

        val chipNewList = mutableListOf(
            ChipViewItemModel(name = getString(R.string.today), value = ActivityEnum.TODAY.title, type = ActivityEnum.TODAY.fieldName),
            ChipViewItemModel(name = getString(R.string.yesterday), value = ActivityEnum.YESTERDAY.title, type = ActivityEnum.YESTERDAY.fieldName),
            ChipViewItemModel(name = getString(R.string.weekly), value = ActivityEnum.WEEK.title, type = ActivityEnum.WEEK.fieldName),
            ChipViewItemModel(name = getString(R.string.month), value = ActivityEnum.MONTH.title, type = ActivityEnum.MONTH.fieldName),
            ChipViewItemModel(name = getString(R.string.customize), value = ActivityEnum.CUSTOMISE.title, type = ActivityEnum.CUSTOMISE.fieldName),
        )

        val chipList = chipNewList

        val selectedList = ArrayList<ChipViewItemModel>()
        selectedList.add(ChipViewItemModel(name = getString(R.string.customize), value = ActivityEnum.CUSTOMISE.title, type = ActivityEnum.CUSTOMISE.fieldName))

        activityTagListCustomView.addChipItemList(chipList, selectedList)
    }

    private fun getDashboardList(fetchDates: Boolean? = false) {
        if (fetchDates == false) {
            val selectedItem = activityTagListCustomView.getSelectedTags()
            if (selectedItem.isNotEmpty()) {
                selectedItem[0].let {
                    if (it.value == ActivityEnum.CUSTOMISE.title) {
                        resetCount()
                        showDatePickers()
                        binding.etFrom.text = getDashboardFromDate()
                        binding.etTo.text = getDashboardToDate()
                        binding.etFrom.text?.takeIf { it.isNotEmpty() }?.let { fromDate ->
                            binding.etTo.text?.takeIf { it.isNotEmpty() }?.let { toDate ->
                                getDashboardList(true)
                            }
                        }
                    } else {
                        hideDatePicker()
                        val request = UserDashboardRequest(
                            sortField = it.type,
                            tenantId = SecuredPreference.getTenantId(),
                            userId = SecuredPreference.getUserFhirId(),
                            shasthyaShebikaId = viewModel.filter?.shasthyaShebikaId,
                        )
                        constructRequest(request)
                    }
                }
            }
        } else {
            val endDate = changeStringToDate(binding.etTo.text.toString(), DATE_FORMAT_ddMMMyyyy)
            val request = UserDashboardRequest(
                sortField = null,
                tenantId = SecuredPreference.getTenantId(),
                customDate = CustomDateModel(
                    startDate = convertDateTimeToDate(
                        binding.etFrom.text.toString(),
                        DATE_FORMAT_ddMMMyyyy,
                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        inUTC = true,
                    ),
                    endDate = getEndDate(
                        endDate,
                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        inUTC = true,
                    ),
                ),
                userId = SecuredPreference.getUserFhirId(),
                shasthyaShebikaId = viewModel.filter?.shasthyaShebikaId,
            )
            constructRequest(request)
        }
    }

    private fun constructRequest(request: UserDashboardRequest) {
        viewModel.getUserDashboardDetails(requireContext(), request)
    }

    private fun resetCount() {
        binding.apply {
            tvScreeningValue.text = "0"
            tvReferredValue.text = "0"
            tvEnrolledValue.text = "0"
            tvAssessedValue.text = "0"

            tvLinkedToCareValue.text = "0"
        }
    }

    private fun showDatePickers() {
        binding.customiseGroup.visibility = View.VISIBLE
    }

    fun changeStringToDate(
        dateString: String,
        pattern: String,
    ): Date? = SimpleDateFormat(pattern, Locale.getDefault()).parse(dateString)

    fun getDashboardFromDate(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1) // Set to the first day of the month
        }
        val dateFormat = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun convertDateTimeToDate(
        inputText: String?,
        inputFormat: String,
        outputFormat: String,
        inUserTimeZone: Boolean? = false,
        inUTC: Boolean? = null,
    ): String {
        try {
            inputText?.let {
                if (it.isNotBlank()) {
                    var userTimeZone: TimeZone? = null
                    val isTimeZoneFormat = inputFormat == DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                    if (isTimeZoneFormat || inUserTimeZone == true) {
                        getTimeZoneInput(inputText, isTimeZoneFormat)?.let { timeZone ->
                            userTimeZone = timeZone
                        }
                    } else if (inUTC == true) {
                        userTimeZone = getUTCFormat()
                    }
                    val sdfInput = SimpleDateFormat(inputFormat, Locale.getDefault())
                    userTimeZone?.let { ust ->
                        sdfInput.timeZone = ust
                    }
                    val date = sdfInput.parse(it)
                    date?.let {
                        val sdfOutput = SimpleDateFormat(outputFormat, Locale.ENGLISH)
                        userTimeZone?.let { ust ->
                            sdfOutput.timeZone = ust
                        }
                        return sdfOutput.format(date)
                    }
                }
            }
        } catch (e: Exception) {
            return ""
        }
        return ""
    }

    fun getEndDate(
        endDate: Date?,
        opFormat: String,
        inUTC: Boolean? = null,
    ): String? {
        endDate?.let { date ->
            val sdf = SimpleDateFormat(opFormat, Locale.ENGLISH)
            val calendar = Calendar.getInstance()
            calendar.time = date
            if (inUTC == true) {
                getUTCFormat()?.let {
                    calendar.timeZone = it
                    sdf.timeZone = it
                }
            }
            calendar.set(Calendar.HOUR, calendar.getActualMaximum(Calendar.HOUR))
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND))
            return sdf.format(calendar.time)
        }
        return null
    }

    private fun getUTCFormat(): TimeZone? = TimeZone.getTimeZone("GMT+00:00")

    private fun getTimeZoneInput(
        inputText: String,
        timeZoneFormat: Boolean,
    ): TimeZone? {
        var timeZoneInput = SecuredPreference.getTimeZoneId()
        if (timeZoneInput.isNullOrBlank() && timeZoneFormat) {
            timeZoneInput = "GMT${
                if (inputText.contains("+")) {
                    inputText.substring(inputText.indexOf("+"))
                } else {
                    inputText.substring(inputText.indexOf("-"))
                }
            }"
        } else if (!timeZoneInput.isNullOrBlank()) {
            timeZoneInput = "GMT$timeZoneInput"
        }

        return TimeZone.getTimeZone(timeZoneInput)
    }

    fun getDashboardToDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun hideDatePicker() {
        binding.customiseGroup.visibility = View.GONE
        binding.etFrom.text = ""
        binding.etTo.text = ""
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.etFrom -> {
                displayDatePicker(true, binding.etFrom.text?.toString())
            }

            binding.etTo -> {
                displayDatePicker(false, binding.etTo.text?.toString())
            }

            else -> {
                // Else block
            }
        }
    }

    private fun displayDatePicker(
        isFromDate: Boolean,
        text: String?,
    ) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!text.isNullOrBlank()) yearMonthDate = DateUtils.getYearMonthAndDate(text, DateUtils.getDateDDMMMYYYY())

        if (datePickerDialog == null) {
            val minMaxDate = getMinDate(isFromDate)
            datePickerDialog = ViewUtil.showDatePicker(
                context = requireContext(),
                maxDate = minMaxDate.second,
                minDate = minMaxDate.first,
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                dateFormatConvertor(
                    "$dayOfMonth/$month/$year",
                    DATE_ddMMyyyy,
                    DATE_FORMAT_ddMMMyyyy,
                )?.let { stringDate ->
                    if (isFromDate) {
                        binding.etFrom.text = stringDate
                    } else {
                        binding.etTo.text = stringDate
                    }
                }
                if (!binding.etFrom.text.isNullOrEmpty() && !binding.etTo.text.isNullOrEmpty()) {
                    getDashboardList(
                        true,
                    )
                }
                datePickerDialog = null
            }
        }
    }

    fun dateFormatConvertor(
        inputDate: String,
        inputDateFormat: String,
        outputDateFormat: String,
    ): String? =
        try {
            val inputFormat = SimpleDateFormat(inputDateFormat, Locale.getDefault())
            val outputFormat = SimpleDateFormat(outputDateFormat, Locale.getDefault())

            inputFormat.parse(inputDate)?.let { parsedDate ->
                outputFormat.format(parsedDate)
            } ?: run {
                null
            }
        } catch (e: Exception) {
            null
        }

    private fun getMinDate(isFromDate: Boolean): Pair<Long?, Long?> {
        val fromDate = binding.etFrom.text?.toString()
        val toDate = binding.etTo.text?.toString()
        return if (isFromDate) {
            if (!toDate.isNullOrBlank()) {
                Pair(
                    null,
                    convertDateToLong(toDate, DATE_FORMAT_ddMMMyyyy),
                )
            } else {
                Pair(null, System.currentTimeMillis())
            }
        } else {
            if (!fromDate.isNullOrBlank()) {
                Pair(
                    convertDateToLong(fromDate, DATE_FORMAT_ddMMMyyyy),
                    System.currentTimeMillis(),
                )
            } else {
                Pair(null, System.currentTimeMillis())
            }
        }
    }

    fun convertDateToLong(
        date: String?,
        format: String? = DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): Long? {
        try {
            val convertedDate = date?.let { dateStr ->
                SimpleDateFormat(
                    format,
                    Locale.getDefault(),
                ).parse(dateStr)?.time
            }
            return convertedDate ?: 0L
        } catch (e: Exception) {
            return null
        }
    }

    override fun filter(filterModel: FilterModel) {
        viewModel.filter = filterModel
        updateFilterButtonText()
        getDashboardList()
    }

    override fun sort(sortModel: SortModel?) {
    }
}
