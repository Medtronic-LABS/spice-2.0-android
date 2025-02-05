package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.setExpandableText
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.CommonUtils.createInvestigation
import com.medtroniclabs.opensource.common.CommonUtils.getBMI
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.convertStringToDate
import com.medtroniclabs.opensource.common.DateUtils.getDateStringFromDate
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.MotherNeonateAncSummaryModel
import com.medtroniclabs.opensource.data.history.PatientStatus
import com.medtroniclabs.opensource.databinding.FragmentMotherNeonateAncSummaryBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertWeight
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.MotherNeonateSummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class MotherNeonateAncSummary : BaseFragment(),View.OnClickListener {

    private lateinit var binding: FragmentMotherNeonateAncSummaryBinding
    var adapter: CustomSpinnerAdapter? = null
    private var datePickerDialog: DatePickerDialog? = null
    val viewModel: MotherNeonateSummaryViewModel by activityViewModels()
    val patientViewModel: PatientDetailViewModel by activityViewModels()
    private var encounterId: String? = null
    private var fhirId: String? = null

    fun setIds(encounterId: String?, fhirId: String?) {
        this.encounterId = encounterId
        this.fhirId = fhirId
        calculateNextVisitFOrRMNCHANC()
    }

    private fun calculateNextVisitFOrRMNCHANC() {
        patientViewModel.getPatientLmb()?.let { lmp ->
            convertStringToDate(
                lmp,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
            )?.let { lmpDate ->
                RMNCH.calculateNextANCVisitDate(
                    lmpDate, true
                )?.let { visitDate ->
                    binding.tvNextMedicalReviewLabelText.text = getDateStringFromDate(
                        visitDate, DateUtils.DATE_ddMMyyyy
                    )
                    viewModel.nextFollowupDate =
                        binding.tvNextMedicalReviewLabelText.text.toString()
                }
            }

        }
        viewModel.fetchMotherNeonateSummary(
            encounterId, fhirId
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMotherNeonateAncSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "MotherNeonateSummary"
        fun newInstance(): MotherNeonateAncSummary {
            return MotherNeonateAncSummary()
        }

        fun newInstance(encounterId: String?,fhirId: String?): MotherNeonateAncSummary {
            val fragment = MotherNeonateAncSummary()
            val bundle = Bundle()
            bundle.putString(DefinedParams.EncounterId, encounterId)
            bundle.putString(DefinedParams.FhirId, fhirId)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                        DefinedParams.Value to (item.value ?: item.name)
                    )
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
            tvWeightText.text = convertWeight(motherNeonateSummaryModel.weight,requireContext())
            tvBPText.text = if (motherNeonateSummaryModel.systolic == null && motherNeonateSummaryModel.diastolic == null) {
                getString(R.string.hyphen_symbol)
            } else {
                MotherNeonateUtil.calculateBp(motherNeonateSummaryModel.systolic, motherNeonateSummaryModel.diastolic, requireContext())
            }
            val obstetricsExaminationText = combineText(
                motherNeonateSummaryModel.obstetricExaminations,
                motherNeonateSummaryModel.obstetricExaminationNotes,
                getString(R.string.hyphen_symbol)
            )
            tvObstetricsExaminationText.setExpandableText(
                fullText = obstetricsExaminationText,
                moreColorResId = R.color.purple_700,
                title = tvObstetricsExaminationLabel.text.toString(),
                activity = (requireActivity() as BaseActivity)
            )

            val presentingComplaintsText = combineText(
                motherNeonateSummaryModel.presentingComplaints,
                motherNeonateSummaryModel.presentingComplaintsNotes,
                getString(R.string.hyphen_symbol)
            )
            tvPresentingComplaintsText.setExpandableText(
                fullText = presentingComplaintsText,
                moreColorResId = R.color.purple_700,
                title = tvPresentingComplaintsLabel.text.toString(),
                activity = (requireActivity() as BaseActivity)
            )
            if (motherNeonateSummaryModel.height != null && motherNeonateSummaryModel.weight != null) {
                tvBMIText.text = getBMI(
                    motherNeonateSummaryModel.height,
                    motherNeonateSummaryModel.weight,
                    requireContext()
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
                    activity = (requireActivity() as BaseActivity)
                )
            }

            tvFundalHeightText.text =
                MotherNeonateUtil.convertCMS(motherNeonateSummaryModel.fundalHeight, requireContext())
            tvFetalHeartRateText.text =
                MotherNeonateUtil.convertBeatsPerMinute(motherNeonateSummaryModel.fetalHeartRate, requireContext())

            tvPrescrptionText.text = motherNeonateSummaryModel.prescriptions?.let { CommonUtils.createPrescription(it,requireContext()) }?.takeIf { it.isNotEmpty() }
                ?: requireContext().getString(R.string.hyphen_symbol)
            binding.tvInvestigationText.text = motherNeonateSummaryModel.investigations?.let { createInvestigation(it,requireContext()) }?.takeIf { it.isNotEmpty() }
                ?: requireContext().getString(R.string.hyphen_symbol)
        }
    }

    private fun initView() {
        binding.tvNextMedicalReviewLabel.markMandatory()
        binding.tvPatientStatus.markMandatory()
        binding.tvClinicalName.text = requireContext().getString(
            R.string.firstname_lastname,
            SecuredPreference.getUserDetails()?.firstName,
            SecuredPreference.getUserDetails()?.lastName
        )
        binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
            DateUtils.getTodayDateDDMMYYYY(),
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy
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
        if (!binding.tvNextMedicalReviewLabelText.text.isNullOrBlank())
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.tvNextMedicalReviewLabelText.text.toString())

        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null }
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.tvNextMedicalReviewLabelText.text = DateUtils.convertDateTimeToDate(
                    stringDate,
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DateUtils.DATE_ddMMyyyy
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
                    true
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
                    itemId: Long
                ) {
                    val selectedItem = adapter?.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = it[DefinedParams.id] as String?
                        val selectedPatientStatus = it[DefinedParams.Value] as String?
                        if (selectedId != DefinedParams.DefaultID) {
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
        val value = binding.tvNextMedicalReviewLabelText.text?.trim().toString()
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