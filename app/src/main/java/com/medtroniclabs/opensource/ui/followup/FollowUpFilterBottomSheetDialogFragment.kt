package com.medtroniclabs.opensource.ui.followup

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentFilterBottomSheetDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.followup.viewmodel.FollowUpViewModel
import java.time.LocalDate
import java.time.ZoneOffset

class FollowUpFilterBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentFilterBottomSheetDialogBinding
    private lateinit var villageListTagView: TagListCustomView
    private lateinit var dataRangesListTagView: TagListCustomView
    private var datePickerDialog: DatePickerDialog? = null
    private val viewModel: FollowUpViewModel by activityViewModels()

    companion object {
        const val TAG = "FollowUpFilterBottomSheetDialogFragment"
        fun newInstance(): FollowUpFilterBottomSheetDialogFragment {
            return FollowUpFilterBottomSheetDialogFragment()
        }
    }

    override fun getTheme(): Int {
        return R.style.DialogStyle
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilterBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initializeListeners()
        attachObservers()
    }

    private fun enableConfirm() {

        val isCustomizedOptionSelected =
            dataRangesListTagView.getSelectedTags().any { it.name == FollowUpDefinedParams.FilterCustomize }

        val isDateRangeValid = if (dataRangesListTagView.getSelectedTags().isNotEmpty()) {
            if (isCustomizedOptionSelected) {
                !(binding.etFromDate.text.isEmpty() || binding.etToDate.text.isEmpty())
            } else {
                true
            }
        } else {
            false
        }

        val isVillageValid = if (isCustomizedOptionSelected) {
            isDateRangeValid && villageListTagView.getSelectedTags().isNotEmpty()
        } else {
            villageListTagView.getSelectedTags().isNotEmpty()
        }

        binding.btnApply.isEnabled = isVillageValid || isDateRangeValid
    }

    private fun initializeListeners() {
        binding.btnApply.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun attachObservers() {
        val villages = viewModel.getVillages()
        val chipItemList = ArrayList<ChipViewItemModel>()
        villages.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name
                )
            )
        }
        villageListTagView.addChipItemList(
            chipItemList,
            viewModel.getFilterData()?.selectedVillages
        )
    }

    private fun initView() {
        binding.tvVillageTitle.text = getString(R.string.household_location)
        villageListTagView =
            TagListCustomView(binding.root.context, binding.villageChipGroup) { _, _, _ ->
                enableConfirm()
            }

        dataRangesListTagView =
            TagListCustomView(binding.root.context, binding.registrationStatusChipGroup) { _, _, _ ->
                if (dataRangesListTagView.getSelectedTags().isEmpty()) {
                    goneDatePicker()
                } else {
                    val isCustomized =
                        dataRangesListTagView.getSelectedTags().any { it.name == FollowUpDefinedParams.FilterCustomize }
                    if (isCustomized) {
                        binding.clDateRange.visibility = View.VISIBLE
                        binding.tvApplyError.visibility = View.GONE
                    } else {
                        goneDatePicker()
                    }
                }

                enableConfirm()
            }
        composeStatusListChipView()
        binding.tvRegistrationStatus.text = getString(R.string.data_range)
        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
    }

    private fun goneDatePicker() {
        binding.etFromDate.text = ""
        binding.etToDate.text = ""
        binding.tvApplyError.visibility = View.GONE
        binding.etFromDateError.visibility = View.GONE
        binding.clDateRange.visibility = View.GONE
    }


    private fun composeStatusListChipView() {
        binding.etFromDate.text = viewModel.getFilterData()?.fromDate ?: ""
        binding.etToDate.text = viewModel.getFilterData()?.toDate ?: ""

        val itemList = viewModel.getDateRange()
        val statusList = ArrayList<ChipViewItemModel>()
        itemList.forEach {
            statusList.add(
                ChipViewItemModel(name = it)
            )
        }

        dataRangesListTagView.addChipItemList(
            statusList,
            viewModel.getFilterData()?.selectedDateRange
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnApply -> {
                applyFilter()
            }

            binding.etFromDate.id -> {
                binding.etFromDateError.visibility = View.GONE
                binding.tvApplyError.visibility = View.GONE
                showDatePickerDialog(true, binding.etFromDate.text.toString())
            }

            binding.etToDate.id -> {
                if (binding.etFromDate.text.toString().isNotEmpty()) {
                    showDatePickerDialog(false, binding.etToDate.text.toString())
                } else {
                    binding.etFromDateError.visibility = View.VISIBLE
                    binding.etFromDateError.text = getString(R.string.Select_Date)
                }
                enableConfirm()
            }

            R.id.btnCancel -> {
                viewModel.updateFollowUpFilter(
                    selectedVillages = listOf(),
                    selectedDateRange = listOf(),
                    fromDate = "",
                    toDate = ""
                )
                villageListTagView.clearSelection()
                dataRangesListTagView.clearSelection()
                dismiss()
            }
        }
    }

    private fun applyFilter() {
        viewModel.updateFollowUpFilter(
            selectedVillages = villageListTagView.getSelectedTags(),
            selectedDateRange = dataRangesListTagView.getSelectedTags(),
            fromDate = binding.etFromDate.text.toString(),
            toDate = binding.etToDate.text.toString()
        )
        dismiss()
    }

    private fun showDatePickerDialog(isFromDate: Boolean, text: String?) {
        var date: Triple<Int?, Int?, Int?>? = null
        if (!text.isNullOrBlank())
            date = DateUtils.convertedMMMToddMM(text)
        val minMaxDate = getMinDate(isFromDate)
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                disableFutureDate = false,
                date = date,
                minDate = minMaxDate.first,
                maxDate = getMaxDate(),
                cancelCallBack = { datePickerDialog = null }) { _, year, month, dayOfMonth ->
                DateUtils.convertDateTimeToDate(
                    "$dayOfMonth-$month-$year",
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DateUtils.DATE_ddMMyyyy
                ).let { stringDate ->
                    if (isFromDate) {
                        binding.etFromDate.text = stringDate
                        binding.etToDate.text = ""
                    } else {
                        binding.etToDate.text = stringDate
                    }
                }
                enableConfirm()
                datePickerDialog = null
            }
        }
    }

    private fun getMinDate(isFromDate: Boolean): Pair<Long?, Long?> {
        val fromDate = binding.etFromDate.text?.toString()
        val toDate = binding.etToDate.text?.toString()
        return if (isFromDate) {
            if (!toDate.isNullOrBlank())
                Pair(null, DateUtils.convertDateToLong(toDate, DateUtils.DATE_ddMMyyyy))
            else Pair(null, System.currentTimeMillis())
        } else {
            if (!fromDate.isNullOrBlank())
                Pair(
                    DateUtils.convertDateToLong(fromDate, DateUtils.DATE_ddMMyyyy),
                    System.currentTimeMillis()
                )
            else Pair(null, System.currentTimeMillis())
        }
    }

    private fun getMaxDate(): Long {
        val localDate = LocalDate.now().atStartOfDay()
        return localDate.plusDays(6).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

}