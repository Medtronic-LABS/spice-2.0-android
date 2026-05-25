package org.medtroniclabs.uhis.ui.followup

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
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FollowupFilterBottomSheetDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel
import java.time.LocalDate
import java.time.ZoneOffset

class FollowUpFilterBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var binding: FollowupFilterBottomSheetDialogBinding
    private lateinit var ssListTagView: TagListCustomView
    private lateinit var villageListTagView: TagListCustomView
    private lateinit var dataRangesListTagView: TagListCustomView
    private lateinit var referralReasonTagView: TagListCustomView
    private lateinit var ncdReasonTagView: TagListCustomView
    private lateinit var ncdReferralToTagView: TagListCustomView
    private var datePickerDialog: DatePickerDialog? = null
    private val viewModel: FollowUpViewModel by activityViewModels()

    companion object {
        const val TAG = "FollowUpFilterBottomSheetDialogFragment"

        fun newInstance(): FollowUpFilterBottomSheetDialogFragment = FollowUpFilterBottomSheetDialogFragment()
    }

    override fun getTheme(): Int = R.style.DialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FollowupFilterBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initializeListeners()
        attachObservers()
        viewModel.getShashtyaShebikas()
    }

    private fun enableConfirm() {
        val isCustomizedOptionSelected =
            dataRangesListTagView
                .getSelectedTags()
                .any { it.name == FollowUpDefinedParams.FILTER_CUSTOMIZE }

        val isDateRangeValid = if (dataRangesListTagView.getSelectedTags().isNotEmpty()) {
            if (isCustomizedOptionSelected) {
                !(binding.etFromDate.text.isEmpty() || binding.etToDate.text.isEmpty())
            } else {
                true
            }
        } else {
            false
        }

        val isSSValid = if (isCustomizedOptionSelected) {
            isDateRangeValid && ssListTagView.getSelectedTags().isNotEmpty()
        } else {
            ssListTagView.getSelectedTags().isNotEmpty()
        }

        val isVillageValid = if (isCustomizedOptionSelected) {
            isDateRangeValid && villageListTagView.getSelectedTags().isNotEmpty()
        } else {
            villageListTagView.getSelectedTags().isNotEmpty()
        }

        val isValidReferralReasons = if (isCustomizedOptionSelected) {
            isDateRangeValid && referralReasonTagView.getSelectedTags().isNotEmpty()
        } else {
            referralReasonTagView.getSelectedTags().isNotEmpty()
        }

        val isValidReason = if (isCustomizedOptionSelected) {
            isDateRangeValid && ncdReasonTagView.getSelectedTags().isNotEmpty()
        } else {
            ncdReasonTagView.getSelectedTags().isNotEmpty()
        }

        val isValidReferralTo = if (isCustomizedOptionSelected) {
            isDateRangeValid && ncdReferralToTagView.getSelectedTags().isNotEmpty()
        } else {
            ncdReferralToTagView.getSelectedTags().isNotEmpty()
        }

        binding.btnApply.isEnabled = isSSValid || isVillageValid || isDateRangeValid || isValidReferralReasons || isValidReason || isValidReferralTo
    }

    private fun initializeListeners() {
        binding.btnApply.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun attachObservers() {
        viewModel.shashthyaShebikasLiveData.observe(viewLifecycleOwner) {
            ssListTagView.addChipItemList(
                it,
                viewModel.getFilterData()?.selectedShashtyaShebikas,
            )
        }
        viewModel.subVillagesLiveData.observe(viewLifecycleOwner) {
            binding.villageChipGroup.clearCheck()
            if (it.isEmpty()) {
                hideVillage()
            } else {
                binding.tvVillageTitle.visible()
                binding.villageChipGroup.visible()
            }
            villageListTagView.addChipItemList(
                it,
                viewModel.getFilterData()?.selectedVillages,
            )
        }
    }

    private fun initView() {
        viewModel.setUserJourney(AnalyticsDefinedParams.FollowUPFilter)
        // Hide village(sub-village) initially
        hideVillage()
        // Hide NCD reason initially
        hideReason()
        // Hide NCD referral to initially
        hideReferredTo()

        ssListTagView = TagListCustomView(binding.root.context, binding.ssChipGroup, true) { _, _, _ ->
            val selectedTags = ssListTagView.getSelectedTags()
            if (selectedTags.isEmpty()) {
                hideVillage()
            } else {
                viewModel.onShashtyaShebikaSelected(selectedTags)
            }
            enableConfirm()
        }

        villageListTagView =
            TagListCustomView(binding.root.context, binding.villageChipGroup) { _, _, _ ->
                enableConfirm()
            }

        referralReasonTagView =
            TagListCustomView(binding.root.context, binding.cgReferralReason) { _, _, _ ->
                val selectedTags = referralReasonTagView.getSelectedTags()
                val hasNCD = selectedTags.any { it.type == FollowUpDefinedParams.FILTER_NCD }
                if (hasNCD) {
                    binding.tvNcdReason.visible()
                    binding.cgNcdReason.visible()

                    binding.tvNcdReferral.visible()
                    binding.cgNcdReferral.visible()
                } else {
                    hideReason()
                    hideReferredTo()
                }
                enableConfirm()
            }

        ncdReasonTagView = TagListCustomView(binding.root.context, binding.cgNcdReason, true) { _, _, _ ->
            enableConfirm()
        }

        ncdReferralToTagView = TagListCustomView(binding.root.context, binding.cgNcdReferral, true) { _, _, _ ->
            enableConfirm()
        }

        dataRangesListTagView =
            TagListCustomView(
                binding.root.context,
                binding.dateRangeChipGroup,
            ) { _, _, _ ->
                if (dataRangesListTagView.getSelectedTags().isEmpty()) {
                    goneDatePicker()
                } else {
                    val isCustomized =
                        dataRangesListTagView
                            .getSelectedTags()
                            .any { it.name == FollowUpDefinedParams.FILTER_CUSTOMIZE }
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
        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
    }

    /**
     * Hide village group and clear selection
     */
    private fun hideVillage() {
        binding.tvVillageTitle.gone()
        binding.villageChipGroup.gone()
        binding.villageChipGroup.clearCheck()
    }

    /**
     * Hide ncd reason and clear selection
     */
    private fun hideReason() {
        binding.tvNcdReason.gone()
        binding.cgNcdReason.gone()
        binding.cgNcdReason.clearCheck()
    }

    /**
     * Hide ncd referral to and clear selection
     */
    private fun hideReferredTo() {
        binding.tvNcdReferral.gone()
        binding.cgNcdReferral.gone()
        binding.cgNcdReferral.clearCheck()
    }

    /**
     * Hide date picker and clear content
     */
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
                ChipViewItemModel(name = it),
            )
        }

        dataRangesListTagView.addChipItemList(
            statusList,
            viewModel.getFilterData()?.selectedDateRange,
        )

        val referralReasonList = viewModel.getReferralReasons().map {
            ChipViewItemModel(
                name = AssessmentUtil.mapServiceToServiceName(it, requireContext()),
                type = it,
            )
        }
        referralReasonTagView.addChipItemList(
            referralReasonList,
            viewModel.getFilterData()?.selectedReferralReasons,
        )

        ncdReasonTagView.addChipItemList(
            viewModel.getNCDReason(),
            viewModel.getFilterData()?.ncdSelectedReasons,
        )

        ncdReferralToTagView.addChipItemList(
            viewModel.getNcdReferralFacility(),
            viewModel.getFilterData()?.ncdSelectedReferralTo,
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
                if (binding.etFromDate.text
                        .toString()
                        .isNotEmpty()
                ) {
                    showDatePickerDialog(false, binding.etToDate.text.toString())
                } else {
                    binding.etFromDateError.visibility = View.VISIBLE
                    binding.etFromDateError.text = getString(R.string.Select_Date)
                }
                enableConfirm()
            }

            R.id.btnCancel -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.CANCELBUTTONTRIGGERED)
                viewModel.updateFollowUpFilter(
                    selectedShashthyaShebikas = listOf(),
                    selectedVillages = listOf(),
                    selectedDateRange = listOf(),
                    selectedReferralReasons = listOf(),
                    ncdSelectedReason = listOf(),
                    ncdSelectedReferralTo = listOf(),
                    fromDate = "",
                    toDate = "",
                )
                ssListTagView.clearSelection()
                villageListTagView.clearSelection()
                dataRangesListTagView.clearSelection()
                referralReasonTagView.clearSelection()
                ncdReasonTagView.clearSelection()
                ncdReferralToTagView.clearSelection()
                dismiss()
            }
        }
    }

    private fun applyFilter() {
        viewModel.updateFollowUpFilter(
            selectedShashthyaShebikas = ssListTagView.getSelectedTags(),
            selectedVillages = villageListTagView.getSelectedTags(),
            selectedDateRange = dataRangesListTagView.getSelectedTags(),
            selectedReferralReasons = referralReasonTagView.getSelectedTags(),
            ncdSelectedReason = ncdReasonTagView.getSelectedTags(),
            ncdSelectedReferralTo = ncdReferralToTagView.getSelectedTags(),
            fromDate = binding.etFromDate.text.toString(),
            toDate = binding.etToDate.text.toString(),
        )
        viewModel.setUserJourney(AnalyticsDefinedParams.APPLYBUTTONTRIGGERED)
        dismiss()
    }

    private fun showDatePickerDialog(
        isFromDate: Boolean,
        text: String?,
    ) {
        var date: Triple<Int?, Int?, Int?>? = null
        if (!text.isNullOrBlank()) {
            date = DateUtils.convertedMMMToddMM(text)
        }
        val minMaxDate = getMinDate(isFromDate)
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                disableFutureDate = false,
                date = date,
                minDate = minMaxDate.first,
                maxDate = getMaxDate(),
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                DateUtils
                    .convertDateTimeToDate(
                        "$dayOfMonth-$month-$year",
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy,
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
            if (!toDate.isNullOrBlank()) {
                Pair(null, DateUtils.convertDateToLong(toDate, DateUtils.DATE_ddMMyyyy))
            } else {
                Pair(null, System.currentTimeMillis())
            }
        } else {
            if (!fromDate.isNullOrBlank()) {
                Pair(
                    DateUtils.convertDateToLong(fromDate, DateUtils.DATE_ddMMyyyy),
                    System.currentTimeMillis(),
                )
            } else {
                Pair(null, System.currentTimeMillis())
            }
        }
    }

    private fun getMaxDate(): Long {
        val localDate = LocalDate.now().atStartOfDay()
        return localDate.plusDays(6).toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
