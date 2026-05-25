package org.medtroniclabs.uhis.ui.patient.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_DD_MMM_YYYY
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_ddMMyyyy
import org.medtroniclabs.uhis.databinding.FragmentNurseFollowUpBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.patient.util.ViewUtil.showDatePicker
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import java.util.Calendar
import kotlin.getValue
import kotlin.toString

class NurseFollowUpFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentNurseFollowUpBinding
    private var defaultDaysCount = 28
    private var datePickerDialog: DatePickerDialog? = null
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "NurseFollowUpFragment"

        fun newInstance(): NurseFollowUpFragment = NurseFollowUpFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNurseFollowUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        updateDayAndFollowUpDate(defaultDaysCount)
    }

    private fun initView() {
        binding.ivPlus.safeClickListener(this)
        binding.ivMinus.safeClickListener(this)
        binding.tvDateManually.safeClickListener(this)
        binding.tvNextFollowUpLabel.markMandatory()
    }

    override fun onClick(view: View?) {
        with(binding) {
            when (view?.id) {
                ivPlus.id -> {
                    if (defaultDaysCount < 730) {
                        defaultDaysCount++
                        tvErrorMessage.visibility = View.GONE
                        ivPlus.isEnabled = true
                    } else {
                        tvErrorMessage.apply {
                            text = getString(R.string.years_error)
                            visibility = View.VISIBLE
                        }
                        ivPlus.isEnabled = false
                    }
                    ivPlus.isEnabled = true
                    updateDayAndFollowUpDate(defaultDaysCount)
                }

                ivMinus.id -> {
                    if (defaultDaysCount > 1) {
                        ivMinus.isEnabled = true
                        defaultDaysCount--
                        tvErrorMessage.visibility = View.GONE
                        updateDayAndFollowUpDate(defaultDaysCount)
                    } else {
                        tvErrorMessage.visibility = View.VISIBLE
                        ivMinus.isEnabled = false
                    }
                    ivMinus.isEnabled = true
                }
                tvDateManually.id -> showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.tvNextFollowUpDate.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertddMMMToddMM(
                    binding.tvNextFollowUpDate.text.toString(),
                    DATE_DD_MMM_YYYY,
                )
        }
        if (datePickerDialog == null) {
            datePickerDialog = showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                maxDate = DateUtils.getTwoYearsLater(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val selectedDay = Calendar.getInstance().apply {
                    set(year, month - 1, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val currentDay = Calendar.getInstance()
                val daysDifference = calculateDaysDifference(currentDay, selectedDay)
                this.defaultDaysCount = daysDifference
                updateDayAndFollowUpDate(daysDifference)
                val stringDate = "$dayOfMonth-$month-$year"
                val (formattedDate, dayOfWeek) = DateUtils.convertDateTimeToDateWithDay(
                    stringDate,
                    DATE_FORMAT_ddMMyyyy,
                    DATE_DD_MMM_YYYY,
                )
                binding.tvNextFollowUpDate.text = "$formattedDate ($dayOfWeek)"
                datePickerDialog = null
            }
        }
    }

    private fun calculateDaysDifference(
        startDate: Calendar,
        endDate: Calendar,
    ): Int {
        startDate.set(Calendar.HOUR_OF_DAY, 0)
        startDate.set(Calendar.MINUTE, 0)
        startDate.set(Calendar.SECOND, 0)
        startDate.set(Calendar.MILLISECOND, 0)

        endDate.set(Calendar.HOUR_OF_DAY, 0)
        endDate.set(Calendar.MINUTE, 0)
        endDate.set(Calendar.SECOND, 0)
        endDate.set(Calendar.MILLISECOND, 0)

        val diffInMillis = endDate.timeInMillis - startDate.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun updateDayAndFollowUpDate(daysCount: Int) {
        this.defaultDaysCount = daysCount
        binding.tvDaysCount.text = if (daysCount < 10) {
            getString(R.string.next_follow_up_day, daysCount)
        } else {
            getString(R.string.next_follow_up_days, daysCount)
        }
        val (formattedDate, dayOfWeek) = DateUtils.calculateDaysToCurrentDateAndDay(daysCount, DATE_DD_MMM_YYYY)
        binding.tvNextFollowUpDate.text = "$formattedDate ($dayOfWeek)"
    }

    fun validation() {
        nurseViewModel.nurseMrRequestModel.nextMedicalReviewDate = DateUtils.convertToIsoFormat(
            binding.tvNextFollowUpDate.text
                .toString()
                .trim(),
        )
    }
}
