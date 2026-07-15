package org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.OTHER
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.model.Eligibilities
import org.medtroniclabs.uhis.data.model.HivCreateScreeningSummaryResponse
import org.medtroniclabs.uhis.data.model.HivScreeningResponse
import org.medtroniclabs.uhis.databinding.FragmentHivSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.medicalreview.hiv.activity.HivMedicalReviewBaseActivity
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums

class HivSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentHivSummaryBinding
    private val hivViewModel: HivViewModel by activityViewModels()
    private var datePickerDialog: DatePickerDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHivSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "HivSummaryFragment"

        fun newInstance(): HivSummaryFragment {
            val args = Bundle()
            val fragment = HivSummaryFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListeners()
        attachObserver()
    }

    fun initView() {
        binding.tvNextMedicalReviewLabel.markMandatory()
        binding.tvPatientStatus.markMandatory()
        if (arguments?.getBoolean(
                DefinedParams.EMTCT,
                false,
            ) == false
        ) {
            binding.groupHivHbsag?.gone()
        }
        hivViewModel.getHivScreeningDetails(
            HivScreeningResponse(
                encounterId = arguments?.getString(DefinedParams.ENCOUNTER_ID) ?: "",
                patientReference = arguments?.getString(DefinedParams.PatientReference) ?: "",
            ),
        )
    }

    private fun attachObserver() {
        hivViewModel.hivScreeningDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        renderSummaryDetails(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {}
                }
            }
        }

        val swipeRefresh =
            (activity as HivMedicalReviewBaseActivity).findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
        if (swipeRefresh.isRefreshing) {
            swipeRefresh.isRefreshing = false
        }
    }

    private fun renderSummaryDetails(response: HivCreateScreeningSummaryResponse) {
        with(binding) {
            groupA3.visibility = if (hivViewModel.isEMTCT) View.GONE else View.VISIBLE
//             Diagnosis Text
            val diagnosisList = response.diagnosis ?: emptyList()
            if (diagnosisList.isNotEmpty()) {
                tvDiagnosesText.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.a_red_error,
                    ),
                )
            }
            tvDiagnosesText.text = diagnosisList
                .filter {
                    it.diseaseCategory.lowercase() != DefinedParams.OTHER_NOTES.lowercase()
                }.map { it.diseaseCategory }
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { CommonUtils.convertListToString(ArrayList(it)) }
                ?: getString(R.string.hyphen_symbol)
        }
        binding.tvClinicalName.text = requireContext().getString(
            R.string.firstname_lastname,
            SecuredPreference.getUserDetails()?.firstName,
            SecuredPreference.getUserDetails()?.lastName,
        )
        binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
            DateUtils.getTodayDateDDMMYYYY(),
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy,
        )
        binding.tvA1TestText.text = response.a1TestResult?.takeIf { it.isNotEmpty() } ?: "-"
        binding.tvA2TestText.text = response.a2TestResult?.takeIf { it.isNotEmpty() } ?: "-"
        binding.tvA3TestText.text = response.a3TestResult?.takeIf { it.isNotEmpty() } ?: "-"
        binding.tvEntryPointText.text = generateEntryPointText(response) ?: "-"
        binding.tvHivSyphilisText.text =
            response.hivSyphilisDuoTest?.takeIf { it.isNotEmpty() } ?: "-"
        binding.tvHbsgText.text =
            response.hbsAGTest?.takeIf { it.isNotEmpty() } ?: "-"
        binding.tvClinicalNotesText.text = response.clinicalNotes?.takeIf { it.isNotEmpty() } ?: "-"

        // Prescription
        binding.tvPrescrptionText.text = response.prescriptions
            ?.let { CommonUtils.createPrescription(it, requireContext()) }
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.hyphen_symbol)

        // Investigation
        binding.tvInvestigationText.text = response.investigations
            ?.let { CommonUtils.createInvestigation(it, requireContext()) }
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.hyphen_symbol)
        val dropDownList = ArrayList<Map<String, Any>>()
        val (test1, test2, test3) = if (response.hbsAGTest.isNullOrEmpty()) {
            Triple(response.a1TestResult, response.a2TestResult, response.a3TestResult)
        } else {
            Triple(response.hbsAGTest, response.a2TestResult, response.a3TestResult)
        }

        val nextVisitType = getTestResultStatus(test1, test2, test3)

        binding.tvNextMedicalReviewLabelText.text = when (nextVisitType) {
            1 -> DateUtils.getFormattedDateAfterDays(3)
            2, 4 -> DateUtils.getFormattedDateAfterDays(14)
            else -> DateUtils.getFormattedDateAfterMonths(1)
        }
        hivViewModel.nextVisitDate = binding.tvNextMedicalReviewLabelText.text
            .toString()
            .trim()

        if (!response.summaryStatus.isNullOrEmpty()) {
            for (item in response.summaryStatus) {
                if (nextVisitType == 1 && item.name == "Retest (HTS)") continue
                dropDownList.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to item.value,
                    ),
                )
            }
            setListenerToDeliveryStatus(dropDownList, nextVisitType)
        }
        renderSymptoms(response)
    }

    private fun getTestResultStatus(
        a1: String?,
        a2: String?,
        a3: String?,
    ): Int {
        val testResults = listOfNotNull(a1, a2, a3)

        return when {
            testResults.all { it.equals(getString(R.string.reactive), ignoreCase = true) } -> 1
            testResults.any { it.equals(getString(R.string.inconclusive), ignoreCase = true) } -> 2
            testResults.any { it.equals(getString(R.string.non_reactive), ignoreCase = true) } -> 4
            else -> 3
        }
    }

    private fun generateEntryPointText(response: HivCreateScreeningSummaryResponse): String? {
        return response.entryPoint?.let {
            if (it.isNotEmpty()) {
                if (it.equals(OTHER, true)) {
                    return "${it.capitalizeFirstChar()} - ${hivViewModel.otherEntryPoint}"
                }
                return it
            } else {
                return getString(R.string.seperator_hyphen)
            }
        }
    }

    private fun renderSymptoms(response: HivCreateScreeningSummaryResponse) {
        hivViewModel.getHivPatientStatusByCategory(MedicalReviewTypeEnums.patient_status.name)
        binding.flExaminationContainer.text = (
            response.eligibilities
                ?.let {
                    formatEligibility(it, otherPopulationType = response.otherPopulationType)
                }.takeIf { !it.isNullOrBlank() }
                ?: getString(R.string.hyphen_symbol)
        )
    }

    private fun formatEligibility(
        eligibility: Eligibilities,
        otherPopulationType: String? = null,
    ): String =
        buildString {
            // Handle Symptoms
            eligibility.Symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                val label = "${getString(R.string.history)} - "
                append(label)
                val padding = " ".repeat(8)
                symptoms.forEach { symptom ->
                    append("\n$padding$symptom")
                }
                append("\n") // Add a line break between sections
            }

            eligibility.hivPopulationType?.takeIf { it.isNotEmpty() }?.let { types ->
                val label = "${getString(R.string.population_type)} - "
                append(label)
                val padding = " ".repeat(8)
                types.forEach { type ->
                    val line = if (type.equals(OTHER, ignoreCase = true) &&
                        !otherPopulationType.isNullOrBlank()
                    ) {
                        "$type - ${otherPopulationType.trim()}"
                    } else {
                        type
                    }
                    append("\n$padding$line")
                }
            }
        }.trim()

    private fun setListeners() {
        binding.tvNextMedicalReviewLabelText.safeClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.tvNextMedicalReviewLabelText.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        val yearMonthDate = binding.tvNextMedicalReviewLabelText.text
            ?.takeIf { it.isNotBlank() }
            ?.let { DateUtils.convertedMMMToddMM(it.toString()) }
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.tvNextMedicalReviewLabelText.text = DateUtils.convertDateTimeToDate(
                    stringDate,
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DateUtils.DATE_ddMMyyyy,
                )
                hivViewModel.nextVisitDate =
                    binding.tvNextMedicalReviewLabelText.text
                        .toString()
                        .trim()
                        .takeIf { it.isNotBlank() }
                datePickerDialog = null
                binding.tvNextMedicalReviewError.invisible()
                summaryListener()
            }
        }
    }

    private fun setListenerToDeliveryStatus(
        list: ArrayList<Map<String, Any>>,
        nextVisitType: Int,
    ) {
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        var defaultPosition = 0
        if (nextVisitType == 1) {
            defaultPosition = list
                .indexOfFirst {
                    (it[DefinedParams.Value] as? String).equals(
                        getString(R.string.referred),
                        ignoreCase = true,
                    )
                }.takeIf { it != -1 } ?: 0
            binding.tvPatientStatusSpinner.isEnabled = false
        } else if (nextVisitType == 2 || nextVisitType == 4) {
            // Auto-populate "Retest (HTS)"
            defaultPosition = list
                .indexOfFirst {
                    (it[DefinedParams.Value] as? String)?.equals(
                        "retest",
                        ignoreCase = true,
                    ) ?: false
                }.takeIf { it != -1 } ?: 0
            binding.tvPatientStatusSpinner.isEnabled = false
        } else {
            for ((index, patientStatus) in list.withIndex()) {
                if ((patientStatus[DefinedParams.Value] as? String).equals(
                        ReferralStatus.OnTreatment.name,
                        true,
                    )
                ) {
                    defaultPosition = index
                }
            }
        }

        binding.tvPatientStatusSpinner.post {
            binding.tvPatientStatusSpinner.setSelection(defaultPosition, false)
        }

        binding.tvPatientStatusSpinner.adapter = adapter
        binding.tvPatientStatusSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val selectedItem = adapter.getData(position = position)
                    selectedItem?.let {
                        val selectedName = it[DefinedParams.Value] as String?
                        selectedName?.let { name ->
                            hivViewModel.selectedPatientStatus = name
                        }
                        updateNextFollowUpDate()
                    }
                    summaryListener()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun updateNextFollowUpDate() {
        if (hivViewModel.selectedPatientStatus?.equals(
                ReferralStatus.Recovered.name,
                true,
            ) == true ||
            hivViewModel.selectedPatientStatus?.equals(
                ReferralStatus.Died.name,
                true,
            ) == true
        ) {
            if (hivViewModel.nextVisitDate != null) {
                hivViewModel.nextVisitDate = null
                binding.tvNextMedicalReviewLabelText.text = ""
            }
            binding.tvNextMedicalReviewLabelText.isEnabled = false
        } else {
            if (!binding.tvNextMedicalReviewLabelText.isEnabled) {
                binding.tvNextMedicalReviewLabelText.isEnabled = true
            }
        }
    }

    private fun summaryListener() {
        setFragmentResult(
            MedicalReviewDefinedParams.SUMMARY_ITEM,
            bundleOf(
                MedicalReviewDefinedParams.SUMMARY_ITEM to true,
            ),
        )
    }

    fun validation(): Boolean {
        var isValid = true
        if (hivViewModel.nextVisitDate != null) {
            binding.tvNextMedicalReviewError.invisible()
        } else {
            binding.tvNextMedicalReviewError.visible()
            isValid = false
        }

        if (hivViewModel.selectedPatientStatus.isNullOrEmpty()) {
            binding.tvPatientError.visible()
            isValid = false
        } else {
            binding.tvPatientError.gone()
        }

        return isValid
    }
}
