package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.convertListToString
import org.medtroniclabs.uhis.common.CommonUtils.createInvestigation
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.None
import org.medtroniclabs.uhis.common.DefinedParams.Tear
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.model.MotherDTO
import org.medtroniclabs.uhis.databinding.FragmentMotherSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliverySummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MotherSummaryFragment : BaseFragment() {
    private lateinit var binding: FragmentMotherSummaryBinding
    private var datePickerDialog: DatePickerDialog? = null
    private val viewModel: LabourDeliverySummaryViewModel by activityViewModels()
    private val viewModelLabourDeliver: LabourDeliveryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMotherSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initializeDatePicker()
        attachObserver()
    }

    private fun initView() {
        binding.tvNextVisitDateLabel.markMandatory()
        viewModelLabourDeliver.motherPatientStatus = binding.tvPatientStatus.text.toString()
    }

    private fun attachObserver() {
        viewModel.summaryDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { motherState ->
                        setDetails(motherState.motherDTO)
                    }
                    resourceState.data.let {
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun setDetails(motherDTO: MotherDTO?) {
        binding.tvDateOfDelivery.text = calculateDateTime(
            motherDTO?.labourDTO?.dateAndTimeOfDelivery ?: getString(R.string.hyphen_symbol),
            true,
        )
//        binding.tvNextVisitDate.text=calculateLabourDeliveryNextVisitDate(
//            motherDTO?.labourDTO?.dateAndTimeOfDelivery ?: getString(R.string.hyphen_symbol),
//            true
//        )
        motherDTO?.labourDTO?.dateAndTimeOfDelivery?.let {
            DateUtils
                .convertStringToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                )?.let { deliveryDate ->
                    RMNCH.calculateNextPNCVisitDate(deliveryDate)?.let { visitDate ->
                        binding.tvNextVisitDate.text = DateUtils.getDateStringFromDate(
                            visitDate,
                            DateUtils.DATE_ddMMyyyy,
                        )
                        viewModelLabourDeliver.nextFollowupDate = binding.tvNextVisitDate.text.toString()
                        summaryListener()
                    }
                }
        }

        binding.tvDateOfLabourOnset.text =
            calculateDateTime(
                motherDTO?.labourDTO?.dateAndTimeOfLabourOnset ?: getString(R.string.hyphen_symbol),
                true,
            )
        binding.tvDateType.text =
            motherDTO?.labourDTO?.deliveryType ?: getString(R.string.hyphen_symbol)
        binding.tvGeneralConditionOfMother.text =
            motherDTO?.generalConditions ?: getString(R.string.hyphen_symbol)
        binding.tvStateOfThePerineum.text = when (motherDTO?.stateOfPerineum) {
            DefinedParams.EPISIOTOMY -> getString(R.string.episotomy)
            Tear -> getString(R.string.hyphen_symbol)
            None -> getString(R.string.none)
            null -> getString(R.string.hyphen_symbol)
            else -> "${getString(R.string.tear_hypen)} ${motherDTO.stateOfPerineum}"
        }
        binding.tvPatientStatus.text = getString(R.string.post_natal)
        binding.tvStatus.text = motherDTO
            ?.status
            ?.map { it }
            ?.let { ArrayList(it) }
            ?.let { convertListToString(it) }
            ?: getString(R.string.hyphen_symbol)
        binding.tvTimeOfDelivery.text =
            calculateDateTime(
                motherDTO?.labourDTO?.dateAndTimeOfDelivery ?: getString(R.string.hyphen_symbol),
                false,
            )
        binding.tvTimeOfLabourOnset.text =
            calculateDateTime(
                motherDTO?.labourDTO?.dateAndTimeOfLabourOnset ?: getString(R.string.hyphen_symbol),
                false,
            )
        binding.tvPrescriptionText.text = motherDTO?.prescriptions.let { CommonUtils.createPrescription(it, requireContext()) }?.takeIf { it.isNotEmpty() }
            ?: requireContext().getString(R.string.hyphen_symbol)

        binding.tvInvestigationText.text = motherDTO?.investigations?.let { createInvestigation(it, requireContext()) }?.takeIf { it.isNotEmpty() }
            ?: requireContext().getString(R.string.hyphen_symbol)
    }

    private fun calculateDateTime(
        dateTime: String,
        isDate: Boolean,
    ): String? {
        val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime1 = ZonedDateTime.parse(dateTime, inputFormatter)
        val timeFormatter: DateTimeFormatter = if (isDate) {
            DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy)
        } else {
            DateTimeFormatter.ofPattern(DateUtils.TIME_FORMAT_hhmma)
        }
        return dateTime1.format(timeFormatter)
    }

    private fun calculateLabourDeliveryNextVisitDate(
        dateTime: String,
        isDate: Boolean,
    ): String? {
        val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime1 = ZonedDateTime.parse(dateTime, inputFormatter)
        val adjustedDate = dateTime1.toLocalDate().plusDays(3)
        if (adjustedDate == LocalDate.now()) {
            return "" // Return empty string if the adjusted date is today
        }
        val timeFormatter: DateTimeFormatter = if (isDate) {
            DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy)
        } else {
            DateTimeFormatter.ofPattern(DateUtils.TIME_FORMAT_hhmma)
        }
        return adjustedDate.format(timeFormatter)
    }

    private fun initializeDatePicker() {
        binding.tvNextVisitDate.setOnClickListener {
            initializeNextVisitDate()
        }
    }

    private fun initializeNextVisitDate() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (binding.tvNextVisitDate.text
                .toString()
                .isNotEmpty()
        ) {
            yearMonthDate = DateUtils.convertedMMMToddMM(binding.tvNextVisitDate.text.toString())
        }
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.tvNextVisitDate.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy,
                    )
                viewModel.nextFollowupDate = binding.tvNextVisitDate.text.toString()
                viewModelLabourDeliver.nextFollowupDate = binding.tvNextVisitDate.text.toString()
                datePickerDialog = null
                summaryListener()
            }
        }
    }

    companion object {
        const val TAG = "MotherSummaryFragment"

        fun newInstance(): MotherSummaryFragment = MotherSummaryFragment()
    }

    private fun summaryListener() {
        setFragmentResult(
            MedicalReviewDefinedParams.SUMMARY_ITEM,
            bundleOf(
                MedicalReviewDefinedParams.SUMMARY_ITEM to true,
            ),
        )
    }
}
