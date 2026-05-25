package org.medtroniclabs.uhis.ui.medicalreview.epi.fragment

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.databinding.ImmunisationSummaryFragmentBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.medicalreview.ResponseImmunisationSummaryDetails
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.medicalreview.epi.viewmodel.ImmunisationViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@AndroidEntryPoint
class ImmunisationSummaryFragment : BaseFragment() {
    private lateinit var binding: ImmunisationSummaryFragmentBinding
    private val viewModel: ImmunisationViewModel by activityViewModels()

    companion object {
        const val TAG = "ImmunisationSummaryFragment"

        fun newInstance() = ImmunisationSummaryFragment()

        fun newInstance(
            patientId: String?,
            dateOfBirth: String?,
            encounterId: String?,
            patientReference: String?,
        ): ImmunisationSummaryFragment {
            val fragment = ImmunisationSummaryFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putString(DefinedParams.DOB, dateOfBirth)
            bundle.putString(DefinedParams.ENCOUNTER_ID, encounterId)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = ImmunisationSummaryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        viewModel.setUserJourney(AnalyticsDefinedParams.ImmunisationSummary)
    }

    private fun initView() {
        viewModel.getLastVaccineScheduleDateAndVaccinationDate()
        arguments?.getString(DefinedParams.ENCOUNTER_ID)?.let { encounterId ->
            viewModel.getImmunisationSummaryDetails(encounterId)
        }

        binding.tvNextVaccinationDate.setOnClickListener {
            showDatePicker()
        }

        binding.lblCatchUpPolicy.safeClickListener {
            val missedVaccineCount = viewModel.immunisationSummaryLiveData.value
                ?.data
                ?.missedVaccine
                ?.size ?: 0
            val dialog = EpiCatchUpPolicyDialogFragment(missedVaccineCount)
            dialog.show(childFragmentManager, "EpiCatchPolicy")
        }

        binding.tvClinicalName.text = requireContext().getString(
            R.string.firstname_lastname,
            SecuredPreference.getUserDetails()?.firstName,
            SecuredPreference.getUserDetails()?.lastName,
        )
        binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
            DateUtils.getTodayDateDDMMYYYY(),
            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DATE_ddMMyyyy,
        )
    }

    private fun showDatePicker() {
        val selectedDate = DateUtils.convertedMMMToddMM(binding.tvNextVaccinationDate.text.toString())
        val minDate = LocalDate
            .now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        ViewUtils.showDatePicker(
            context = requireContext(),
            disableFutureDate = false,
            date = selectedDate,
            minDate = minDate,
        ) { _, year, month, dayOfMonth ->
            DateUtils
                .convertDateTimeToDate(
                    "$dayOfMonth-$month-$year",
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DATE_ddMMyyyy,
                ).let { stringDate ->
                    binding.tvNextVaccinationDate.text = stringDate

                    val inputFormatter = DateTimeFormatter.ofPattern(DATE_ddMMyyyy)
                    val outputFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
                    val localDate = LocalDate.parse(stringDate, inputFormatter)
                    val formattedDate = localDate.atStartOfDay(ZoneOffset.UTC).format(outputFormatter)
                    viewModel.nextVisitDate = formattedDate
                }
        }
    }

    private fun attachObserver() {
        viewModel.immunisationSummaryLiveData.observe(viewLifecycleOwner) {
            when (it.state) {
                ResourceState.LOADING -> showProgress()
                ResourceState.SUCCESS -> {
                    hideProgress()
                    it.data?.let { summaryDetails ->
                        updateSummaryDetails(summaryDetails)
                    }
                }
                ResourceState.ERROR -> {
                    showProgress()
                }
            }
        }

        viewModel.updateScheduleDateAndVaccinationDate.observe(viewLifecycleOwner) {
            binding.tvScheduledDate.text = getColoredSpannableString(it)
        }

        viewModel.nextVaccinationDetails?.let {
            binding.tvNextDuration.text = it.nextVaccinationDuration
            binding.tvNextDose.text = it.nextVaccinationDose.joinToString(separator = ", ")
        } ?: run {
            binding.lblNextDuration.gone()
            binding.tvNextDuration.gone()
            binding.tvNextDurationSeparator.gone()

            binding.lblNextDose.gone()
            binding.tvNextDose.gone()
            binding.tvNextDoseSeparator.gone()
        }

        viewModel.nextVisitDate?.let {
            binding.tvNextVaccinationDate.text = DateUtils.convertDateTimeToDate(
                it,
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_ddMMyyyy,
            )
        } ?: kotlin.run {
            binding.lblNextVaccinationDate.gone()
            binding.tvNextVaccinationDate.gone()
            binding.tvNextVaccinationDateSeparator.gone()
        }
    }

    private fun updateSummaryDetails(response: ResponseImmunisationSummaryDetails) {
        val vaccinationTaken = response.vaccinated.joinToString(separator = ", ")
        binding.tvVaccinationTaken.text = vaccinationTaken

        if (response.missedVaccine.isEmpty()) {
            binding.lblVaccinationMissed.gone()
            binding.tvVaccinationMissedSeparator.gone()
            binding.tvVaccinationMissed.gone()

            binding.lblMissedReason.gone()
            binding.tvMissedReasonSeparator.gone()
            binding.tvMissedReason.gone()
        } else {
            val missedVaccine = response.missedVaccine.joinToString(separator = ", ")
            binding.tvVaccinationMissed.text = missedVaccine

            val missedReason = response.missedReason ?: "--"
            binding.tvMissedReason.text = missedReason
        }
    }

    private fun getColoredSpannableString(pair: Pair<LocalDate, LocalDate>): Spannable {
        val dayDiff = ChronoUnit.DAYS.between(pair.first, pair.second)
        val date = pair.first.format(DateTimeFormatter.ofPattern(DATE_ddMMyyyy))

        val status = when {
            dayDiff > 1L -> getString(R.string.days_delay, dayDiff.toInt())
            dayDiff == 1L -> getString(R.string.day_delay, dayDiff.toInt())
            else -> getString(R.string.on_time)
        }

        val fullText = "$date, ($status)"
        val spannable = SpannableString(fullText)

        val color = if (dayDiff > 0) {
            ContextCompat.getColor(requireContext(), R.color.epi_missed_primary)
        } else {
            ContextCompat.getColor(requireContext(), R.color.green_attention_color)
        }

        val startIndex = fullText.indexOf("($status)")
        val endIndex = startIndex + status.length + 2 // +2 for parentheses

        if (startIndex != -1) {
            spannable.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }
}
