package com.medtroniclabs.opensource.ncd.followup.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
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
import com.medtroniclabs.opensource.ncd.data.CustomDate
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.CommonEnums
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class NCDFollowUpOfflineBottomDialogFilter : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentFilterBottomSheetDialogBinding
    private lateinit var villageListTagView: TagListCustomView
    private val viewModel: NCDFollowUpViewModel by activityViewModels()
    private lateinit var dateRangeTagView: TagListCustomView

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

    companion object {
        const val TAG = "NCDFollowUpOfflineBottomDialogFilter"
        fun newInstance() =
            NCDFollowUpOfflineBottomDialogFilter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun composeStatusListChipView() {
        val statusList = ArrayList<ChipViewItemModel>()
        statusList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.TODAY.title,
                cultureValue = getString(CommonEnums.TODAY.cultureValue),
                value = CommonEnums.TODAY.value
            )
        )
        statusList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.YESTERDAY.title,
                cultureValue = getString(CommonEnums.YESTERDAY.cultureValue),
                value = CommonEnums.YESTERDAY.value
            )
        )
        statusList.add(
            ChipViewItemModel(
                id = 3,
                name = CommonEnums.CUSTOMISE.title,
                cultureValue = getString(CommonEnums.CUSTOMISE.cultureValue),
                value = CommonEnums.CUSTOMISE.value
            )
        )
        dateRangeTagView.addChipItemList(
            statusList,
            viewModel.filterByDateRange
        )
    }

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
    }

    private fun attachObservers() {
        viewModel.villageListResponse.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let { listItems ->
                        val chipItemList = ArrayList<ChipViewItemModel>()
                        listItems.forEach {
                            chipItemList.add(
                                ChipViewItemModel(
                                    id = it.id,
                                    name = it.name
                                )
                            )
                        }
                        villageListTagView.addChipItemList(
                            chipItemList,
                            viewModel.filterByVillage
                        )
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun initView() {
        binding.tvRegistrationStatus.text = getString(R.string.date_range)
        villageListTagView =
            TagListCustomView(binding.root.context, binding.villageChipGroup) { _, _, _ ->
                viewModel.filterByVillage = villageListTagView.getSelectedTags()
                enableConfirm()
            }
        dateRangeTagView =
            TagListCustomView(
                binding.root.context,
                binding.registrationStatusChipGroup
            ) { _, _, _ ->
                viewModel.filterByDateRange = dateRangeTagView.getSelectedTags()
                changeUIFromToDateVisibility(viewModel.filterByDateRange.any { it.name.uppercase() ==  FollowUpDefinedParams.FilterCustomize.uppercase() })
                enableConfirm()
            }
        composeStatusListChipView()
        viewModel.getAllVillagesName()

        enableConfirm()

        if (binding.etFromDate.text.isNullOrEmpty()) {
            binding.etToDate.isEnabled = false
        }

        binding.etFromDate.doAfterTextChanged {
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
            enableConfirm()
        }
        binding.etToDate.doAfterTextChanged {
            if (viewModel.customDate == null) {
                viewModel.customDate = CustomDate()
            }
            if (it.isNullOrEmpty()) {
                viewModel.customDate = null
            } else {
                viewModel.customDate?.endDate = DateUtils.convertDateTimeToDate(
                    it.toString(),
                    DateUtils.DATE_FORMAT_ddMMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                )
            }
            enableConfirm()
        }
        binding.btnCancel.text = getString(R.string.reset)
        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnApply.safeClickListener(this)
        prefillData()
    }

    private fun prefillData() {
        viewModel.filterByDateRange.let {
            if (it.any { it.name.uppercase() ==  FollowUpDefinedParams.FilterCustomize.uppercase() }) {
                viewModel.customDate?.startDate?.let {
                    binding.etFromDate.text = DateUtils.convertDateTimeToDate(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                        DateUtils.DATE_FORMAT_ddMMMyyyy
                    )
                }
                viewModel.customDate?.endDate?.let {
                    binding.etToDate.isEnabled = true
                    binding.etToDate.text = DateUtils.convertDateTimeToDate(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                        DateUtils.DATE_FORMAT_ddMMMyyyy
                    )
                }
            }
        }
    }


    private fun changeUIFromToDateVisibility(checked: Boolean) {
        binding.clDateRange.visibility = if (checked) {
            View.VISIBLE
        } else {
            resetFromTODate()
            View.GONE
        }
    }

    private fun resetFromTODate() {
        binding.etToDate.text = ""
        binding.etFromDate.text = ""
        binding.etToDate.isEnabled = false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.etFromDate.id -> {
                showDatePickerDialog(true, binding.etFromDate.text?.toString())
            }

            binding.etToDate.id -> {
                showDatePickerDialog(false, binding.etToDate.text?.toString())
            }

            binding.btnCancel.id -> {
                resetFilterOption()
            }

            binding.btnApply.id -> {
                viewModel.filterFollowUpOfflineLiveData()
                dismiss()
            }
        }
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
                    binding.etFromDate.text = stringDate
                    binding.etToDate.isEnabled = true
                } else {
                    binding.etToDate.text = stringDate
                }
            }
        }
    }

    private fun getMinDate(isFromDate: Boolean): Pair<Long?, Long?> {
        val fromDate = binding.etFromDate.text?.toString()
        val toDate = binding.etToDate.text?.toString()
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

    private fun enableConfirm() {
        var isVillageValid = villageListTagView.getSelectedTags().isNotEmpty()
        var isStatusListValid = dateRangeTagView.getSelectedTags().isNotEmpty()

        if (isStatusListValid && dateRangeTagView.getSelectedTags()
                .any { it.name.uppercase() ==  FollowUpDefinedParams.FilterCustomize.uppercase() }
        ) {
            isStatusListValid =
                (viewModel.customDate?.startDate != null && viewModel.customDate?.endDate != null)
            isVillageValid =
                (viewModel.customDate?.startDate != null && viewModel.customDate?.endDate != null)
        }
        binding.btnApply.isEnabled = isVillageValid || isStatusListValid
    }

    private fun resetFilterOption() {
        binding.villageChipGroup.clearCheck()
        binding.registrationStatusChipGroup.clearCheck()
        viewModel.customDate = null
        viewModel.filterByDateRange = listOf()
        viewModel.filterByVillage = listOf()
        viewModel.filterFollowUpOfflineLiveData()
        dismiss()
    }
}