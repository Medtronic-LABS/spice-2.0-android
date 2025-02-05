package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.databinding.FragmentNcdMedicalReviewSummaryBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.data.MRSummaryResponse
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENU_ID
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewActivity
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDMRAlertDialog
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewSummaryViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class NCDMedicalReviewSummaryFragment : BaseFragment(),View.OnClickListener,
    NCDMRAlertDialog.DialogCallback {

    private lateinit var binding: FragmentNcdMedicalReviewSummaryBinding
    private val viewModel: NCDMedicalReviewSummaryViewModel by activityViewModels()
    private val medicalReviewViewModel: NCDMedicalReviewViewModel by activityViewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()
    private var datePickerDialog: DatePickerDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdMedicalReviewSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDMedicalReviewSummaryFragment"
        fun newInstance(visitId: String?,menu:String?, memberId: String?): NCDMedicalReviewSummaryFragment {
            return NCDMedicalReviewSummaryFragment().apply {
                arguments = Bundle().apply {
                    putString(NCDMRUtil.MEMBER_REFERENCE, memberId)
                    putString(NCDMRUtil.EncounterReference, visitId)
                    putString(NCDMRUtil.MENU_ID, menu)
                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }
    private fun getVisitId(): String? {
        return arguments?.getString(NCDMRUtil.EncounterReference)
    }

    private fun getMemberReference(): String? {
        return arguments?.getString(NCDMRUtil.MEMBER_REFERENCE)
    }

    private fun getConfirmDiagnosisType(): List<String> {
        return NCDMRUtil.getConfirmDiagnoses(getType())
    }

    fun getType(): String? {
        return arguments?.getString(MENU_ID)
    }

    private fun attachObservers() {
        viewModel.summaryResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    // navigate to summary
                    resourceState.data?.let {
                        populateData(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    showErrorDialog(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                    )
                }
            }
        }
    }

    private fun initView() {
        SecuredPreference.getUserDetails()?.let {
            binding.tvClinicalName.text = requireContext().getString(
                R.string.firstname_lastname,
                it.firstName?.capitalizeFirstChar()
                    ?: getString(R.string.empty),
                it.lastName ?: getString(R.string.empty)
            )
        }
        binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
            DateUtils.getTodayDateDDMMYYYY(),
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy
        )
        medicalReviewViewModel.createMedicalReview.value?.data?.let {
            withNetworkAvailability(online = {
                viewModel.fetchSummaryResponse(it.copy(memberReference = getMemberReference(), patientVisitId = getVisitId(), diagnosisType = getConfirmDiagnosisType()))
            })
        }
        binding.tvNextMedicalReviewLabel.markMandatory()
        binding.tvNextMedicalReviewLabelText.safeClickListener(this)
        binding.btnConfirmDiagnosis.safeClickListener(this)
        binding.btnMedicationPrescribed.safeClickListener(this)
        getType()?.let { type ->
            binding.tvObstetricsExaminationLabel.text = CommonUtils.getPhysicalExaminationTitle(
                requireContext(),
                type,
                isFemalePregnant()
            )
        }
    }

    private fun isFemalePregnant(): Boolean {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let {
            return it.gender.equals(Screening.Female, true) &&
                    it.isPregnant == true
        }
        return false
    }

    private fun populateData(data: MRSummaryResponse) {
        binding.apply {
            tvObstetricsExaminationText.text = CommonUtils.combineText(
                data.physicalExams,
                data.physicalExamComments,
                getString(R.string.hyphen_symbol)
            )
            tvChiefComplaintsText.text = CommonUtils.combineText(
                data.complaints,
                data.compliantComments,
                getString(R.string.hyphen_symbol)
            )
            tvClinicalNotesText.text =
                data.clinicalNote?.takeIf { it.isNotBlank() } ?: getString(R.string.hyphen_symbol)
            if (!viewModel.nextFollowupDate.isNullOrBlank()) {
                binding.tvNextMedicalReviewLabelText.text = viewModel.nextFollowupDate
            }
            tvPrescrptionText.text = NCDMRUtil.printNumberedListString(
                NCDMRUtil.createPrescription(
                    data.prescriptions,
                    requireContext()
                ), requireContext()
            )
            tvInvestigationText.text = CommonUtils.formatListToString(
                data.investigations,
                getString(R.string.hyphen_symbol)
            )
            tvDiagnosisText.text = CommonUtils.combineText(
                data.confirmDiagnosis?.diagnosis?.mapNotNull { it.name },
                data.confirmDiagnosis?.diagnosisNotes.takeIf { it?.isNotBlank() == true },
                getString(R.string.hyphen_symbol)
            )
            val layoutParams = binding.tvDiagnosisText.layoutParams as ConstraintLayout.LayoutParams
            if (!data.confirmDiagnosis?.diagnosis.isNullOrEmpty()) {
                layoutParams.width = 0 // Set width to 0dp (match_constraint)
                binding.btnConfirmDiagnosis.gone()
            } else {
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.btnConfirmDiagnosis.visible()
            }
            binding.tvDiagnosisText.layoutParams = layoutParams
            binding.tvDiagnosisText.requestLayout()
            if (!data.prescriptions.isNullOrEmpty()) {
                binding.btnMedicationPrescribed.gone()
            } else {
                binding.btnMedicationPrescribed.visible()
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
                datePickerDialog = null
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvNextMedicalReviewLabelText.id -> {
                showDatePickerDialog()
            }
            binding.btnConfirmDiagnosis.id -> {
                withNetworkAvailability(online = {
                    (requireActivity() as? NCDMedicalReviewActivity)?.showConfirmDiagnoses()
                })
            }
            binding.btnMedicationPrescribed.id -> {
                (requireActivity() as? NCDMedicalReviewActivity)?.showPrescription()
            }
        }
    }

    fun validateInput(): Boolean {
        val value = binding.tvNextMedicalReviewLabelText.text?.trim().toString()
        if (value.isBlank()) {
            binding.tvNextMedicalReviewError.visible()
            binding.tvNextMedicalReviewLabelText.requestFocus()
            return false
        }
        binding.tvNextMedicalReviewError.invisible()
        return true
    }

    fun handleConfirmDiagnoses(): Boolean {
        return viewModel.summaryResponse.value?.data?.let { data ->
            return when {
                data.confirmDiagnosis?.diagnosis.isNullOrEmpty() && data.prescriptions.isNullOrEmpty() -> {
                    showErrorDialog(
                        message = getString(R.string.no_confirm_diagnosis_prescribed_medication_warning),
                        false,
                        Pair(true, true)
                    )
                    false
                }

                data.confirmDiagnosis?.diagnosis.isNullOrEmpty() -> {
                    showErrorDialog(
                        message = getString(R.string.no_confirm_diagnosis_warning),
                        false,
                        Pair(true, true)
                    )
                    false
                }

                data.prescriptions.isNullOrEmpty() -> {
                    showErrorDialog(
                        message = getString(R.string.no_new_medicines_prescribed_warning),
                        false,
                        Pair(true, true)
                    )
                    false
                }

                else -> true
            }
        } ?: false
    }

    fun showErrorDialog(
        message: String,
        showConfirm: Boolean = false,
        showYesNo: Pair<Boolean, Boolean> = Pair(true, true)
    ) {
        val existingDialog = childFragmentManager.findFragmentByTag(NCDMRAlertDialog.TAG)
        if (existingDialog == null) {
            NCDMRAlertDialog.newInstance(
                getString(R.string.alert),
                message = message,
                showYesNoClose = Triple(showYesNo.first, showYesNo.second, true),
                showConfirm = showConfirm,
                callback = this
            ).show(childFragmentManager, NCDMRAlertDialog.TAG)
        }
    }

    override fun onYesClicked() {
        (requireActivity() as? NCDMedicalReviewActivity)?.hitSummary()
    }

    override fun onConfirmDiagnosisClicked(isBp:Boolean) {
        if (isBp) {
            (requireActivity() as? NCDMedicalReviewActivity)?.addNewReading(true)
        } else {
            withNetworkAvailability(online = {
                (requireActivity() as? NCDMedicalReviewActivity)?.showConfirmDiagnoses(isDiagnosisMismatch = true)
            })
        }
    }
}