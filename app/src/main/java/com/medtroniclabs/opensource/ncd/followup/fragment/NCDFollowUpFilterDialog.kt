package com.medtroniclabs.opensource.ncd.followup.fragment

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentNcdFollowUpFilterDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.CustomDate
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.TagListCustomView
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class NCDFollowUpFilterDialog : DialogFragment(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: FragmentNcdFollowUpFilterDialogBinding
    private lateinit var tagListCustomView: TagListCustomView
    private val viewModel: NCDFollowUpViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNcdFollowUpFilterDialogBinding.inflate(inflater, container, false)
        isCancelable = false
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    companion object {
        const val TAG = "NCDFollowUpFilterDialog"
        fun newInstance() =
            NCDFollowUpFilterDialog()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setListeners()
    }


    override fun onStart() {
        super.onStart()
        handleOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientation()
    }

    private fun handleOrientation() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = when {
            isTablet && isLandscape -> 70
            else -> 100
        }
        val height = when {
            isTablet && isLandscape -> 90
            else -> 100
        }
        setDialogPercent(width, height)
    }

    private fun changeUIFromToDateVisibility(checked: Boolean) {
        binding.clFromToDate.visibility = if (checked) {
            View.VISIBLE
        } else {
            resetFromTODate()
            View.GONE
        }
    }

    private fun setListeners() {
        binding.btnReset.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.tvFromDate.safeClickListener(this)
        binding.tvToDate.safeClickListener(this)
        binding.labelHeader.ivClose.safeClickListener(this)

        //Data range
        binding.dataRangeDailyChip.setOnCheckedChangeListener(this)
        binding.dataRangeWeeklyChip.setOnCheckedChangeListener(this)
        binding.dataRangeCustomizeChip.setOnCheckedChangeListener(this)
        binding.dataRangeMonthlyChip.setOnCheckedChangeListener(this)
    }

    private fun initializeViews() {

        binding.tvTo.markMandatory()
        binding.tvFrom.markMandatory()

        initializeChipView(binding.dataRangeDailyChip)
        initializeChipView(binding.dataRangeWeeklyChip)
        initializeChipView(binding.dataRangeMonthlyChip)
        initializeChipView(binding.dataRangeCustomizeChip)
        prefillData()

        if (binding.tvFromDate.text.isNullOrEmpty()) {
            binding.tvToDate.isEnabled = false
        }

        binding.tvFromDate.doAfterTextChanged {
            if (viewModel.customDate == null) {
                viewModel.customDate = CustomDate()
            }
            if (it.isNullOrEmpty()) {
                viewModel.customDate?.startDate = null
            } else {
                viewModel.customDate?.startDate =
                    DateUtils.convertDateTimeToDate(
                        it.toString(),
                        DateUtils.DATE_FORMAT_ddMMMyyyy,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        inUTC = true
                    )
            }
            enableButton()
        }
        binding.tvToDate.doAfterTextChanged {
            if (viewModel.customDate == null) {
                viewModel.customDate = CustomDate()
            }
            if (it.isNullOrEmpty()) {
                viewModel.customDate = null
            } else {
                val endDate = DateUtils.changeFormat(it.toString())
                viewModel.customDate?.endDate = DateUtils.getEndDate(
                    endDate,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                )
            }
            enableButton()
        }

        tagListCustomView =
            TagListCustomView(requireContext(), binding.cgRetryAttempts) { _, _, _ ->
                enableButton()
            }
        val chipItems = (1..5).map { id ->
            ChipViewItemModel(
                id = id.toLong(),
                name = "$id",
                cultureValue = "$id",
                value = "$id"
            )
        }.toCollection(ArrayList())
        tagListCustomView.addChipItemList(chipItems, viewModel.remainingAttempts)
        enableButton()
    }

    private fun prefillData() {
        viewModel.dateRange?.let {
            binding.dataRangeCustomizeChip.isChecked = NCDFollowUpFilterEnum.CUSTOMISE.title == it
            if (NCDFollowUpFilterEnum.CUSTOMISE.title == it) {
                viewModel.customDate?.startDate?.let {
                    binding.tvFromDate.text = DateUtils.convertDateTimeToDate(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                        DateUtils.DATE_FORMAT_ddMMMyyyy
                    )
                }
                viewModel.customDate?.endDate?.let {
                    binding.tvToDate.isEnabled = true
                    binding.tvToDate.text = DateUtils.convertDateTimeToDate(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                        DateUtils.DATE_FORMAT_ddMMMyyyy
                    )
                }
                changeUIFromToDateVisibility(NCDFollowUpFilterEnum.CUSTOMISE.title == it)
            }
            binding.dataRangeDailyChip.isChecked = NCDFollowUpFilterEnum.DAILY.title == it
            binding.dataRangeWeeklyChip.isChecked = NCDFollowUpFilterEnum.WEEKLY.title == it
            binding.dataRangeMonthlyChip.isChecked = NCDFollowUpFilterEnum.MONTHLY.title == it
        }

    }


    private fun initializeChipView(chip: Chip) {
        requireContext().let { context ->
            chip.chipBackgroundColor =
                getColorStateList(
                    context.getColor(R.color.medium_blue),
                    context.getColor(R.color.white)
                )
            chip.setChipBackgroundColorResource(R.color.diagnosis_confirmation_selector)
            chip.chipStrokeWidth = 3f
            chip.setTextColor(
                getColorStateList(
                    context.getColor(R.color.white),
                    context.getColor(R.color.navy_blue)
                )
            )
            chip.chipStrokeColor = getColorStateList(
                context.getColor(R.color.medium_blue),
                context.getColor(R.color.mild_gray)
            )
        }
    }

    private fun getColorStateList(
        selectedColor: Int,
        unSelectedColor: Int
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
            unSelectedColor
        )
        return ColorStateList(states, colors)
    }

    private fun resetFromTODate() {
        binding.tvFromDate.text = ""
        binding.tvToDate.text = ""
        binding.tvToDate.isEnabled = false
    }

    private fun changeView(chipView: Chip, isChecked: Boolean) {
        if (isChecked) {
            chipView.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
            chipView.chipStrokeWidth = 0f
        } else {
            chipView.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            chipView.chipStrokeWidth = 3f
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnDone.id -> {
                viewModel.remainingAttempts = tagListCustomView.getSelectedTags()
                if (connectivityManager.isNetworkAvailable()) {
                    viewModel.filterLiveData()
                } else {
                    (activity as BaseActivity).showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                        false
                    ) {}
                }
                dismiss()
            }

            binding.btnReset.id -> {
                resetFilterOption()
            }

            binding.tvFromDate.id -> {
                showDatePickerDialog(true, binding.tvFromDate.text?.toString())
            }

            binding.tvToDate.id -> {
                showDatePickerDialog(false, binding.tvToDate.text?.toString())
            }

            binding.labelHeader.ivClose.id -> {
                dismiss()
            }
        }
    }

    private fun resetFilterOption() {
        binding.cgRetryAttempts.clearCheck()
        binding.dataRangeChipGroup.clearCheck()
        viewModel.customDate = null
        viewModel.dateRange = null
        viewModel.remainingAttempts = listOf()
        viewModel.filterLiveData()
        dismiss()
    }

    private fun showDatePickerDialog(isFromDate: Boolean, text: String?) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!text.isNullOrBlank()) {
            yearMonthDate = DateUtils.getYearMonthAndDate(
                text,
                SimpleDateFormat(DateUtils.DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH)
            )
        }
        val minMaxDate = getMinDate(isFromDate)
        ViewUtils.showDatePicker(
            context = requireContext(),
            maxDate = minMaxDate.second,
            minDate = minMaxDate.first,
            date = yearMonthDate,
            cancelCallBack = { }
        ) { _, year, month, dayOfMonth ->
            DateUtils.convertDateFormat(
                "$dayOfMonth/$month/$year",
                DateUtils.DATE_ddMMyyyy,
                DateUtils.DATE_FORMAT_ddMMMyyyy
            )?.let { stringDate ->
                if (isFromDate) {
                    binding.tvFromDate.text = stringDate
                    binding.tvToDate.isEnabled = true
                } else {
                    binding.tvToDate.text = stringDate
                }
            }
        }
    }

    private fun getMinDate(isFromDate: Boolean): Pair<Long?, Long?> {
        val fromDate = binding.tvFromDate.text?.toString()
        val toDate = binding.tvToDate.text?.toString()
        val minCalenderInstance = Calendar.getInstance()
        minCalenderInstance.set(2000, Calendar.JANUARY, 1)
        return if (isFromDate) {
            if (!toDate.isNullOrBlank())
                Pair(
                    minCalenderInstance.timeInMillis,
                    DateUtils.convertDateToLong(toDate, DateUtils.DATE_FORMAT_ddMMMyyyy)
                )
            else {
                Pair(minCalenderInstance.timeInMillis, System.currentTimeMillis())
            }
        } else {
            if (!fromDate.isNullOrBlank())
                Pair(
                    DateUtils.convertDateToLong(fromDate, DateUtils.DATE_FORMAT_ddMMMyyyy),
                    System.currentTimeMillis()
                )
            else Pair(minCalenderInstance.timeInMillis, System.currentTimeMillis())
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView is Chip) {
            changeView(buttonView, isChecked)
        }

        when (buttonView?.id) {
            binding.dataRangeCustomizeChip.id -> {
                changeUIFromToDateVisibility(isChecked)
                if (isChecked) {
                    viewModel.dateRange = NCDFollowUpFilterEnum.CUSTOMISE.title
                } else {
                    viewModel.dateRange = null
                }
            }

            else -> {
                viewModel.dateRange =
                    getDateRangeOption(binding.dataRangeChipGroup.checkedChipId)
            }
        }
        enableButton()
    }

    private fun getDateRangeOption(checkedChipId: Int): String? {
        var dataRange: String? = null
        when (checkedChipId) {
            binding.dataRangeDailyChip.id -> {
                dataRange = NCDFollowUpFilterEnum.DAILY.title
                viewModel.customDate = null
            }

            binding.dataRangeWeeklyChip.id -> {
                dataRange = NCDFollowUpFilterEnum.WEEKLY.title
                viewModel.customDate = null
            }

            binding.dataRangeMonthlyChip.id -> {
                dataRange = NCDFollowUpFilterEnum.MONTHLY.title
                viewModel.customDate = null
            }
        }
        return dataRange
    }

    private fun enableButton() {
        val hasRemainingAttempts = tagListCustomView.getSelectedTags().isNotEmpty()
        val isDateRangeValid = !viewModel.dateRange.isNullOrBlank()
        val isCustomiseSelected = viewModel.dateRange == NCDFollowUpFilterEnum.CUSTOMISE.title
        val isCustomDateValid = viewModel.customDate?.startDate != null && viewModel.customDate?.endDate != null

        val shouldEnable = when {
            !hasRemainingAttempts && !isDateRangeValid -> false
            hasRemainingAttempts && !isDateRangeValid -> true
            hasRemainingAttempts && isDateRangeValid && !isCustomiseSelected -> true
            !hasRemainingAttempts && isDateRangeValid && !isCustomiseSelected -> true
            hasRemainingAttempts && isDateRangeValid && isCustomiseSelected && isCustomDateValid -> true
            !hasRemainingAttempts && isDateRangeValid && isCustomiseSelected && isCustomDateValid -> true
            else -> false
        }

        binding.btnReset.isEnabled = shouldEnable
        binding.btnDone.isEnabled = shouldEnable
    }
}

enum class NCDFollowUpFilterEnum(val title: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    CUSTOMISE("customise")
}