package org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.setExpandableText
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.toFormattedList
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.model.HivSummaryResponse
import org.medtroniclabs.uhis.databinding.FragmentTbSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.medicalreview.hiv.activity.HivImrAndCmrActivity
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivImrCmrSummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.emtct.activity.MotherNeonateEMTCTActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

@AndroidEntryPoint
class HivImrCmrSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentTbSummaryBinding
    val adapter: CustomSpinnerAdapter by lazy { CustomSpinnerAdapter(requireContext()) }
    val adapterEmtctStatus: CustomSpinnerAdapter by lazy { CustomSpinnerAdapter(requireContext()) }
    val adapterMaternalOutcome: CustomSpinnerAdapter by lazy { CustomSpinnerAdapter(requireContext()) }
    val patientViewModel: PatientDetailViewModel by activityViewModels()
    private var encounterId: String? = null
    private var fhirId: String? = null
    private var isEMTCTMR: Boolean = false
    private var datePickerDialog: DatePickerDialog? = null
    val viewModel: HivImrCmrSummaryViewModel by activityViewModels()

    fun setIds(
        encounterId: String?,
        fhirId: String?,
        isEMTCTMR: Boolean,
    ) {
        this.encounterId = encounterId
        this.fhirId = fhirId
        this.isEMTCTMR = isEMTCTMR
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentTbSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "HivImrCmrSummaryFragment"

        fun newInstance() = HivImrCmrSummaryFragment()

        fun newInstance(
            encounterId: String?,
            fhirId: String?,
            isEMTCTMR: Boolean = false,
        ): HivImrCmrSummaryFragment {
            val fragment = HivImrCmrSummaryFragment()
            fragment.setIds(encounterId = encounterId, fhirId = fhirId, isEMTCTMR = isEMTCTMR)
            return fragment
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    private fun initView() {
        binding.tvNextMedicalReviewLabel.markMandatory()
        binding.tvPatientStatus.markMandatory()
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
        binding.tvNextMedicalReviewLabelText.safeClickListener(this)
        viewModel.fetchHivSummaryDetails(
            encounterId,
            fhirId,
        )
        val views = listOf(
            binding.tvSiteLabel,
            binding.tvSiteSeparator,
            binding.tvSiteText,
            binding.tvTreatmentText,
            binding.tvTreatmentLabel,
            binding.tvTreatmentSeparator,
        )
        binding.tvDiagnosesLabel.text = getText(R.string.diagnosis_tb)
        binding.tvComborbiditiesLabel.text = if (isEMTCTMR)getString(R.string.obstetric_examination)else getString(R.string.comorbidities_coinfections)
        views.forEach { it.setVisible(false) }
        viewModel.getEmtctStatusByCategory(DefinedParams.EMTCT_VISIT_STATUS)
        viewModel.getMaternalStatusByCategory(MedicalReviewTypeEnums.maternal_outcome.name)
    }

    private fun attachObserver() {
        viewModel.hivSummary.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        populate(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.hivEmtctStatusLiveData.observe(viewLifecycleOwner) {
            it
                ?.map { item ->
                    hashMapOf(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to (item.value ?: item.name),
                    )
                }?.distinctBy { it[DefinedParams.Value] } // Ensure uniqueness by Value
                ?.let { uniqueStatusList ->
                    viewModel.statusSpinner = ArrayList(uniqueStatusList)
                    setEmtctStatusSpinner()
                }
        }

        viewModel.hivMaternalStatusLiveData.observe(viewLifecycleOwner) {
            it
                ?.map { item ->
                    hashMapOf(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to (item.value ?: item.name),
                    )
                }?.distinctBy { it[DefinedParams.Value] } // Ensure uniqueness by Value
                ?.let { statusList ->
                    viewModel.maternalOutcomeMap = ArrayList(statusList)
                    setMaternalOutcomeSpinner()
                }
        }
    }

    private fun populate(data: HivSummaryResponse) {
        with(binding) {
            // Diagnosis Text
            val diagnosisList = data.diagnosis ?: emptyList()
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

            if (isEMTCTMR) {
                viewModel.emtctStatusDefault = data.emtctVisitStatus
                tvSiteLabel.visible()
                tvSiteLabel.text = getString(R.string.anc)
                tvSiteSeparator.visible()
                tvSiteText.visible()
                tvSiteText.text = data.ancVisitNumber
                    ?.toString()
                    ?.toIntOrNull()
                    ?.toString()
                    ?.takeIf { it.isNotEmpty() } // or use !it.isNullOrEmpty() for extra safety
                    ?: getString(R.string.hyphen_symbol)
                eMTCTSummaryGroup.visible()
            }
            // Presenting Complaints
            tvPresentingText.setExpandableText(
                fullText = CommonUtils.combineText(
                    data.presentingComplaints,
                    data.presentingComplaintsNotes,
                    getString(R.string.hyphen_symbol),
                ),
                moreColorResId = R.color.purple_700,
                title = tvPresentingLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Comorbidities
            tvComborbiditiesText.setExpandableText(
                fullText = if (!isEMTCTMR) {
                    CommonUtils.combineText(
                        data.comorbiditiesCoinfections,
                        data.comorbiditiesCoinfectionsNotes,
                        getString(R.string.hyphen_symbol),
                    )
                } else {
                    CommonUtils.combineText(
                        data.obstetricExaminations,
                        data.obstetricExaminationNotes,
                        getString(R.string.hyphen_symbol),
                    )
                },
                moreColorResId = R.color.purple_700,
                title = tvComborbiditiesLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Clinical Notes
            tvClinicalNotesText.setExpandableText(
                fullText = data.clinicalNotes?.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.hyphen_symbol),
                moreColorResId = R.color.purple_700,
                title = tvClinicalNotesLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Systematic Examination
            tvGeneralText.setExpandableText(
                fullText = CommonUtils.combineText(
                    data.systemicExaminations?.toFormattedList().takeIf { !it.isNullOrEmpty() }
                        ?: emptyList(),
                    "",
                    getString(R.string.hyphen_symbol),
                ),
                moreColorResId = R.color.purple_700,
                title = tvGeneralLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Prescription
            tvPrescrptionText.text = data.prescriptions
                ?.let { CommonUtils.createPrescription(it, requireContext()) }
                ?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.hyphen_symbol)

            // Investigation
            tvInvestigationText.text = data.investigations
                ?.let { CommonUtils.createInvestigation(it, requireContext()) }
                ?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.hyphen_symbol)
        }

        val dropDownList = ArrayList<Map<String, Any>>()
        if (!data.summaryStatus.isNullOrEmpty()) {
            for (item in data.summaryStatus) {
                dropDownList.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to item.value,
                    ),
                )
            }
            setSpinner(dropDownList)
        }
    }

    private fun setSpinner(statusList: ArrayList<Map<String, Any>>) {
        adapter.setData(statusList)
        var defaultPosition = 0
        for ((index, patientStatus) in statusList.withIndex()) {
            if ((patientStatus[DefinedParams.Value] as? String).equals(
                    ReferralStatus.OnTreatment.name,
                    true,
                )
            ) {
                defaultPosition = index
            }
        }
        binding.tvPatientStatusSpinner.post {
            binding.tvPatientStatusSpinner.setSelection(defaultPosition, false)
        }
        binding.tvPatientStatusSpinner.adapter = adapter
        binding.tvPatientStatusSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedPatientStatus = it[DefinedParams.Value] as String?
                        selectedPatientStatus?.let {
                            viewModel.patientStatus = selectedPatientStatus
                        } ?: kotlin.run {
                            viewModel.patientStatus = null
                        }
                        showHideNextVisit()
                        setMaternalOutcomeSpinner()
                        setEmtctStatusSpinner()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun setEmtctStatusSpinner() {
        var statusList = viewModel.statusSpinner
        adapterEmtctStatus.setData(statusList)
        binding.tvEmtctVisitStatusText.adapter = adapterEmtctStatus
        val isPatientDied = viewModel.patientStatus.equals(getString(R.string.died), ignoreCase = true)
        val defaultPosition = if (isPatientDied) {
            statusList
                .indexOfFirst {
                    val value = it[DefinedParams.Value] as? String
                    value.equals(getString(R.string.d_dead), ignoreCase = true)
                }.takeIf { it != -1 } ?: 0
        } else {
            // Optional: If not died, try to retain previously selected eMTCTStatus
            statusList
                .indexOfFirst {
                    val value = it[DefinedParams.Value] as? String
                    value.equals(viewModel.emtctStatusDefault, ignoreCase = true)
                }.takeIf { it != -1 } ?: 0
        }
        // Set selection AFTER adapter is attached
        binding.tvEmtctVisitStatusText.post {
            binding.tvEmtctVisitStatusText.setSelection(defaultPosition, false)
        }
        // Disable the spinner if "Died"
        binding.tvEmtctVisitStatusText.isEnabled = !isPatientDied
        binding.tvEmtctVisitStatusText.isClickable = !isPatientDied
        binding.tvEmtctVisitStatusText.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapterEmtctStatus.getData(position = pos)
                    selectedItem?.let {
                        val selectedPatientStatus = it[DefinedParams.Value] as String?
                        selectedPatientStatus?.let {
                            viewModel.eMTCTStatus = selectedPatientStatus
                        } ?: kotlin.run {
                            viewModel.eMTCTStatus = null
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun setMaternalOutcomeSpinner() {
        var statusList = viewModel.maternalOutcomeMap
        adapterMaternalOutcome.setData(statusList)
        val isPatientDied = viewModel.patientStatus.equals(getString(R.string.died), ignoreCase = true)
        val defaultPosition = if (isPatientDied) {
            statusList
                .indexOfFirst {
                    val value = it[DefinedParams.Value] as? String
                    value.equals("D-Dead", ignoreCase = true)
                }.takeIf { it != -1 } ?: 0
        } else {
            0
        }
        binding.tvMaternalOutcomeText.post {
            binding.tvMaternalOutcomeText.setSelection(defaultPosition, false)
        }
        binding.tvMaternalOutcomeText.adapter = adapterMaternalOutcome
        // Disable the spinner if "Died"
        binding.tvMaternalOutcomeText.isEnabled = !isPatientDied
        binding.tvMaternalOutcomeText.isClickable = !isPatientDied
        binding.tvMaternalOutcomeText.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedPatientStatus = it[DefinedParams.Value] as String?
                        selectedPatientStatus?.let {
                            viewModel.maternalOutcome = selectedPatientStatus
                        } ?: kotlin.run {
                            viewModel.maternalOutcome = null
                        }
                        showHideNextVisit()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun showHideNextVisit() {
        if (viewModel.patientStatus?.equals(ReferralStatus.Recovered.name, true) == true ||
            viewModel.patientStatus?.equals(ReferralStatus.Died.name, true) == true
        ) {
            viewModel.nextFollowupDate = null
            binding.tvNextMedicalReviewLabelText.text = ""
            binding.tvNextMedicalReviewLabelText.isEnabled = false
        } else {
            binding.tvNextMedicalReviewLabelText.isEnabled = true
            binding.tvNextMedicalReviewLabelText.text = DateUtils.getFormattedDateAfterMonths(1)
            viewModel.nextFollowupDate = DateUtils.getFormattedDateAfterMonths(1)
        }
        (requireActivity() as? HivImrAndCmrActivity)?.enableRefer(
            !viewModel.patientStatus.equals(ReferralStatus.Died.name, true),
        )
        if (isEMTCTMR) {
            (requireActivity() as? MotherNeonateEMTCTActivity)?.enableRefer(
                !viewModel.patientStatus.equals(ReferralStatus.Died.name, true),
            )
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.tvNextMedicalReviewLabelText.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.tvNextMedicalReviewLabelText.text.toString())
        }

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
                viewModel.nextFollowupDate = binding.tvNextMedicalReviewLabelText.text.toString()
                datePickerDialog = null
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvNextMedicalReviewLabelText.id -> {
                showDatePickerDialog()
            }
        }
    }

    fun validateInput(): Boolean {
        val value = binding.tvNextMedicalReviewLabelText.text
            ?.trim()
            .toString()
        if (value.isBlank()) {
            if (viewModel.patientStatus?.equals(ReferralStatus.Recovered.name, true) == true ||
                viewModel.patientStatus?.equals(ReferralStatus.Died.name, true) == true
            ) {
                binding.tvNextMedicalReviewError.invisible()
                return true
            }
            binding.tvNextMedicalReviewError.visible()
            binding.tvNextMedicalReviewLabelText.requestFocus()
            return false
        }
        binding.tvNextMedicalReviewError.invisible()
        return true
    }
}
