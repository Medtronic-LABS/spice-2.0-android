package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.setExpandableText
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.combineText
import org.medtroniclabs.uhis.common.CommonUtils.createInvestigation
import org.medtroniclabs.uhis.common.CommonUtils.getBMI
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.convertStringToDate
import org.medtroniclabs.uhis.common.DateUtils.getDateStringFromDate
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.MotherNeonateAncSummaryModel
import org.medtroniclabs.uhis.data.history.PatientStatus
import org.medtroniclabs.uhis.databinding.FragmentMotherNeonateAncSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertWeight
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.viewmodel.MotherNeonateSummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

class MotherNeonateAncSummary : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentMotherNeonateAncSummaryBinding
    var adapter: CustomSpinnerAdapter? = null
    private var datePickerDialog: DatePickerDialog? = null
    val viewModel: MotherNeonateSummaryViewModel by activityViewModels()
    val patientViewModel: PatientDetailViewModel by activityViewModels()
    private var encounterId: String? = null
    private var fhirId: String? = null

    fun setIds(
        encounterId: String?,
        fhirId: String?,
    ) {
        this.encounterId = encounterId
        this.fhirId = fhirId
        calculateNextVisitFOrRMNCHANC()
    }

    private fun calculateNextVisitFOrRMNCHANC() {
        patientViewModel.getPatientLmb()?.let { lmp ->
            convertStringToDate(
                lmp,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            )?.let { lmpDate ->
                RMNCH
                    .calculateNextANCVisitDate(
                        lmpDate,
                        true,
                    )?.let { visitDate ->
                        binding.tvNextMedicalReviewLabelText.text = getDateStringFromDate(
                            visitDate,
                            DateUtils.DATE_ddMMyyyy,
                        )
                        viewModel.nextFollowupDate =
                            binding.tvNextMedicalReviewLabelText.text.toString()
                    }
            }
        }
        viewModel.fetchMotherNeonateSummary(
            encounterId,
            fhirId,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMotherNeonateAncSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "MotherNeonateSummary"

        fun newInstance(): MotherNeonateAncSummary = MotherNeonateAncSummary()

        fun newInstance(
            encounterId: String?,
            fhirId: String?,
        ): MotherNeonateAncSummary {
            val fragment = MotherNeonateAncSummary()
            val bundle = Bundle()
            bundle.putString(DefinedParams.ENCOUNTER_ID, encounterId)
            bundle.putString(DefinedParams.FhirId, fhirId)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.motherNeonateAncSummary.observe(viewLifecycleOwner) { resourceState ->
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
        patientViewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    viewModel.setAncReqToGetMetaForPatientStatus(MedicalReviewTypeEnums.patient_status.name)
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun setPatientStatus(summaryStatus: List<PatientStatus>?) {
        val statusList = ArrayList<Map<String, Any>>()
        if (summaryStatus != null) {
            for (item in summaryStatus) {
                statusList.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to (item.value ?: item.name),
                    ),
                )
            }
        }
        setSpinner(statusList)
    }

    private fun populate(motherNeonateSummaryModel: MotherNeonateAncSummaryModel) {
        setPatientStatus(motherNeonateSummaryModel.summaryStatus)
        with(binding) {
            tvAncVisitText.text = motherNeonateSummaryModel.visitNumber
                ?: requireContext().getString(R.string.hyphen_symbol)
            tvWeightText.text = convertWeight(motherNeonateSummaryModel.weight, requireContext())
            tvBPText.text = if (motherNeonateSummaryModel.systolic == null && motherNeonateSummaryModel.diastolic == null) {
                getString(R.string.hyphen_symbol)
            } else {
                MotherNeonateUtil.calculateBp(motherNeonateSummaryModel.systolic, motherNeonateSummaryModel.diastolic, requireContext())
            }
            val obstetricsExaminationText = combineText(
                motherNeonateSummaryModel.obstetricExaminations,
                motherNeonateSummaryModel.obstetricExaminationNotes,
                getString(R.string.hyphen_symbol),
            )
            tvObstetricsExaminationText.setExpandableText(
                fullText = obstetricsExaminationText,
                moreColorResId = R.color.purple_700,
                title = tvObstetricsExaminationLabel.text.toString(),
                activity = (requireActivity() as BaseActivity),
            )

            val presentingComplaintsText = combineText(
                motherNeonateSummaryModel.presentingComplaints,
                motherNeonateSummaryModel.presentingComplaintsNotes,
                getString(R.string.hyphen_symbol),
            )
            tvPresentingComplaintsText.setExpandableText(
                fullText = presentingComplaintsText,
                moreColorResId = R.color.purple_700,
                title = tvPresentingComplaintsLabel.text.toString(),
                activity = (requireActivity() as BaseActivity),
            )
            if (motherNeonateSummaryModel.height != null && motherNeonateSummaryModel.weight != null) {
                tvBMIText.text = getBMI(
                    motherNeonateSummaryModel.height,
                    motherNeonateSummaryModel.weight,
                    requireContext(),
                )
            }
            val clinicalNotes = motherNeonateSummaryModel.clinicalNotes

            if (clinicalNotes.isNullOrEmpty()) {
                tvClinicalNotesText.text = requireContext().getString(R.string.hyphen_symbol)
            } else {
                tvClinicalNotesText.setExpandableText(
                    fullText = clinicalNotes,
                    moreColorResId = R.color.purple_700,
                    title = tvClinicalNotesLabel.text.toString(),
                    activity = (requireActivity() as BaseActivity),
                )
            }

            tvFundalHeightText.text =
                MotherNeonateUtil.convertCMS(motherNeonateSummaryModel.fundalHeight, requireContext())
            tvFetalHeartRateText.text =
                MotherNeonateUtil.convertBeatsPerMinute(motherNeonateSummaryModel.fetalHeartRate, requireContext())

            tvPrescrptionText.text =
                motherNeonateSummaryModel.prescriptions?.let { CommonUtils.createPrescription(it, requireContext()) }?.takeIf { it.isNotEmpty() }
                    ?: requireContext().getString(R.string.hyphen_symbol)
            binding.tvInvestigationText.text =
                motherNeonateSummaryModel.investigations?.let { createInvestigation(it, requireContext()) }?.takeIf { it.isNotEmpty() }
                    ?: requireContext().getString(R.string.hyphen_symbol)
        }
    }

    private fun initView() {
        patientViewModel.setUserJourney(AnalyticsDefinedParams.MOTHERANCSUMMARY)
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
        adapter = CustomSpinnerAdapter(requireContext())
        binding.tvNextMedicalReviewLabelText.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvNextMedicalReviewLabelText.id -> {
                showDatePickerDialog()
            }
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
                viewModel.checkSubmitBtn()
                datePickerDialog = null
            }
        }
    }

    private fun setSpinner(statusList: ArrayList<Map<String, Any>>) {
        adapter?.setData(statusList)
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
                    val selectedItem = adapter?.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = it[DefinedParams.ID] as String?
                        val selectedPatientStatus = it[DefinedParams.Value] as String?
                        if (selectedId != DefinedParams.DEFAULT_ID) {
                            viewModel.patientStatus = selectedPatientStatus
                            // handleRecoveredState()
                        } else {
                            viewModel.patientStatus = null
                        }
                        viewModel.checkSubmitBtn()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    fun handleRecoveredState() {
        if (viewModel.patientStatus?.contains(ReferralStatus.Recovered.name) == true) {
            binding.tvNextMedicalReviewLabelText.text = ""
            binding.tvNextMedicalReviewLabelText.isEnabled = false
            binding.tvNextMedicalReviewError.invisible()
        } else {
            binding.tvNextMedicalReviewLabelText.isEnabled = true
        }
    }

    fun validateInput(): Boolean {
        val value = binding.tvNextMedicalReviewLabelText.text
            ?.trim()
            .toString()
        if (value.isBlank()) {
            if (viewModel.patientStatus?.equals(ReferralStatus.Recovered.name, true) == true) {
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
