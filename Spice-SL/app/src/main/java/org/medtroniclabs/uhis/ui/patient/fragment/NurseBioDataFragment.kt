package org.medtroniclabs.uhis.ui.patient.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.PatientStatusEvaluator
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PatientHistoryModel
import org.medtroniclabs.uhis.databinding.FragmentNurseBioDataBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.patient.util.CommonDialogInterface
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseBioDataViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import org.medtroniclabs.uhis.ui.patientEdit.NCDPatientEditActivity
import java.util.ArrayList
import kotlin.getValue
import org.medtroniclabs.uhis.common.DefinedParams as CommonDefinedParams

class NurseBioDataFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentNurseBioDataBinding

    private val nurseBioDataViewModel: NurseBioDataViewModel by viewModels()
    private val medicalReviewBaseViewModel: MedicalReviewBaseViewModel by activityViewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "BioDataFragment"

        fun newInstance(isSummary: Boolean = false): NurseBioDataFragment {
            val args = Bundle()
            args.putBoolean(DefinedParams.IS_SUMMARY, isSummary)
            val fragment = NurseBioDataFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNurseBioDataBinding.inflate(inflater, container, false)
        return binding.root
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
        nurseBioDataViewModel.isSummary = arguments?.getBoolean(DefinedParams.IS_SUMMARY) == true
        if (nurseBioDataViewModel.isSummary) {
            binding.editGroup.visibility = View.GONE
        }

        binding.ivEdit.safeClickListener(this)
        binding.tvEdit.safeClickListener(this)

        if (!medicalReviewBaseViewModel.initialReview) {
            val parentLayout = binding.tvDiagnosesLabel.parent as ConstraintLayout
            val parentLayoutSep = binding.tvDiagnosesSeparator.parent as ConstraintLayout
            val constraintSet = ConstraintSet()
            constraintSet.clone(parentLayout)
            constraintSet.clone(parentLayoutSep)

// Set bottom constraint for another TextView (e.g., tvAnother) with a margin of 20px
            constraintSet.connect(
                binding.tvDiagnosesLabel.id,
                ConstraintSet.BOTTOM,
                ConstraintLayout.LayoutParams.PARENT_ID,
                ConstraintSet.BOTTOM,
                20,
            )

// Set bottom constraint for another TextView (e.g., tvLast) with a margin of 30px
            constraintSet.connect(
                binding.tvDiagnosesSeparator.id,
                ConstraintSet.BOTTOM,
                ConstraintLayout.LayoutParams.PARENT_ID,
                ConstraintSet.BOTTOM,
                20,
            )

// Apply the new constraints
            constraintSet.applyTo(parentLayout)
            constraintSet.applyTo(parentLayoutSep)

            binding.bioGroup.visibility = View.GONE
        } else {
            binding.bioGroup.visibility = View.VISIBLE
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.tvEdit, R.id.ivEdit -> {
                openPatientEdit()
            }
        }
    }

    /**
     * Right-side bio-data edit navigates to the patient enrollment/edit page.
     * (The "Edit diagnosis" link continues to open the confirm-diagnosis dialog.)
     */
    private fun openPatientEdit() {
        // NCDPatientEditActivity looks up the patient via the MEMBER_REFERENCE extra
        // (passed to /patient/patientDetails). For the nurse flow the value that resolves
        // is patientIdString (the same id used by getPatientDetails), not memberReference.
        val patientIdString = nurseViewModel.patientIdString
        if (!patientIdString.isNullOrEmpty()) {
            val intent = Intent(requireContext(), NCDPatientEditActivity::class.java)
            intent.putExtra(NCDMRUtil.PATIENT_REFERENCE, nurseViewModel.nurseMrRequestModel.patientReference)
            intent.putExtra(NCDMRUtil.MEMBER_REFERENCE, patientIdString)
            intent.putExtra(CommonDefinedParams.ORIGIN, MenuConstants.MY_PATIENTS_MENU_ID)
            patientEditLauncher.launch(intent)
        }
    }

    private fun attachObserver() {
        nurseViewModel.patientDetailsValue?.let { data ->
            // nurseViewModel.newPatientId = data.patientId
            showBioData(data)
        }
        nurseViewModel.latestConfirmDiagnosesList.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { data ->
                        // patient/patientDetails returns confirmed diagnoses under
                        // patientConfirmDiagnosis; confirmDiagnosis can be null in that payload.
                        val diagnosesToShow = data.patientConfirmDiagnosis ?: data.confirmDiagnosis
                        medicalReviewBaseViewModel.confirmDiagnosis = diagnosesToShow
                        confirmedDiagnosis(diagnosesToShow)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        nurseBioDataViewModel.cvdRiskResult.observe(viewLifecycleOwner) { result ->
            binding.tvCvdRisk.text = result.display
            binding.tvCvdRisk.setTextColor(
                CommonUtils.cvdRiskColorCode(result.score.toDouble(), requireContext()),
            )
        }

        nurseBioDataViewModel.patientStatusResult.observe(viewLifecycleOwner) { status ->
            val (textRes, colorRes) =
                when (status) {
                    PatientStatusEvaluator.ControlStatus.UNCONTROLLED ->
                        R.string.un_controlled to R.color.attention_color

                    PatientStatusEvaluator.ControlStatus.CONTROLLED ->
                        R.string.controlled to R.color.secondary_green_color
                }
            binding.tvPatientStatus.text = getString(textRes)
            binding.tvPatientStatus.setTextColor(requireContext().getColor(colorRes))
        }

        nurseBioDataViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        it.firstName?.let { firstName ->
                            val text =
                                StringConverter.appendTexts(firstText = firstName, it.lastName)
                            setTitle(
                                StringConverter.appendTexts(
                                    firstText = CommonUtils.capitalize(text),
                                    it.age?.toInt().toString(),
                                    it.gender,
                                    separator = "-",
                                ),
                            )
                        }
                        showBioData(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun openConfirmDiagnosis() {
        ConfirmDiagnosisDialog
            .newInstance(true, commonDialogInterface)
            .show(childFragmentManager, ConfirmDiagnosisDialog.TAG)
    }

    private val commonDialogInterface = object : CommonDialogInterface {
        override fun onSuccess(
            confirmedDiagnoses: ArrayList<String>?,
            diagnosisNotes: String?,
        ) {
            if (!confirmedDiagnoses.isNullOrEmpty()) {
                nurseViewModel.applyLocalConfirmDiagnosis(confirmedDiagnoses, diagnosisNotes)
            }
        }
    }

    private fun getIdentityLabel(identityType: String?): String? =
        when {
            identityType.isNullOrEmpty() || identityType == DefinedParams.NA -> null
            identityType == DefinedParams.IDENTITY_TYPE_BRN -> requireContext().getString(R.string.brn)
            else -> requireContext().getString(R.string.national_id)
        }

    private fun showBioData(data: PatientDetailsModel) {
        with(binding) {
            // patient/patientDetails returns the confirmed diagnoses under
            // patientConfirmDiagnosis; confirmDiagnosis can be null in that payload.
            val diagnosesToShow = data.patientConfirmDiagnosis ?: data.confirmDiagnosis
            medicalReviewBaseViewModel.confirmDiagnosis = diagnosesToShow

            clNationalId.gone()
            getIdentityLabel(data.identityType)?.let {
                clNationalId.visible()
                tvNationalIdLabel.text = it
                tvNationalId.text = data.identityValue ?: requireContext().getString(R.string.hyphen_symbol)
            }

            tvPatientId.text = data.patientId ?: getString(
                R.string.hyphen_symbol,
            )
            tvMobileNumber.text = data.phoneNumber ?: getString(
                R.string.hyphen_symbol,
            )
            tvDateOfRegistration.text = data.enrollmentAt?.let {
                DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                    DateUtils.DATE_DD_MMM_YYYY,
                )
            } ?: getString(R.string.hyphen_symbol)

            tvProgramId.text = data.programId.toString().takeIf { data.programId != null }
                ?: getString(R.string.hyphen_symbol)
            tvPatientStatus.text =
                data.ncdStatus?.let { getPatientType(it) } ?: getString(R.string.hyphen_symbol)

            confirmedDiagnosis(diagnosesToShow)

            nextFollowupDate.text = data.nextMedicalReviewDate?.let {
                DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_DD_MMM_YYYY,
                )
            } ?: getString(R.string.hyphen_symbol)

            tvCvdRisk.text = data.cvdRiskScore?.let {
                StringConverter.appendTexts(
                    "$it%",
                    data.cvdRiskLevel,
                    separator = "-",
                )
            } ?: getString(R.string.hyphen_symbol)

            // Backend may not return a pre-computed CVD score; derive it on-device from the
            // averaged systolic BP and the patient's bio data when missing.
            nurseBioDataViewModel.computeCvdRiskIfNeeded(data)

            // Derive the Controlled / Uncontrolled status from the latest BP, last two glucose
            // readings, comorbidities/complications and the medication-adherence answer captured
            // during the review. Falls back to the backend ncdStatus shown above when undetermined.
            nurseBioDataViewModel.computePatientStatusIfNeeded(
                memberId = data.memberId,
                hasComorbidities = !data.comorbidities.isNullOrEmpty(),
                hasComplications = !nurseViewModel.nurseMrRequestModel.complications.isNullOrEmpty(),
                takingMedication = parseMedicationAdherence(
                    nurseViewModel.nurseMrRequestModel.symptomsLog?.compliance,
                ),
            )

            tvDateOfLastVisit.text = data.lastReviewDate?.let {
                DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_DD_MMM_YYYY,
                )
            } ?: getString(R.string.hyphen_symbol)

            val textColor =
                data.cvdRiskScore?.let { CommonUtils.cvdRiskColorCode(it, requireContext()) }
            if (textColor != null) {
                binding.tvCvdRisk.setTextColor(textColor)
            }

            val bmi = CommonUtils.getBMIInformation(requireContext(), data.bmi)
            bmi?.second?.let { tvBmi.setTextColor(requireContext().getColor(it)) }
            tvBmi.text = data.bmi?.let { bmiValue ->
                val formattedBmi = CommonUtils.getDecimalFormatted(bmiValue)
                bmi?.first?.let { category -> "$formattedBmi ($category)" } ?: formattedBmi
            } ?: getString(R.string.hyphen_symbol)

            tvHealthHistory.text = data.patientHealthHistory?.let { generateConditionString(it) }
                ?: getString(R.string.hyphen_symbol)
        }
    }

    private fun confirmedDiagnosis(confirmDiagnosis: ArrayList<String>?) {
        var savedDiagnosis: String
        confirmDiagnosis.let {
            savedDiagnosis = it?.joinToString(", ") ?: getString(R.string.hyphen_symbol)
        }
        if (nurseBioDataViewModel.isSummary) {
            binding.tvDiagnosesText.text = savedDiagnosis
        } else {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(mView: View) {
                    openConfirmDiagnosis()
                }
            }
            val subText = " ${getString(R.string.edit_diagnosis)}"
            val text = "$savedDiagnosis$subText"
            var index = text.length - subText.length
            index = if (index >= 0) index + 1 else 0
            binding.tvDiagnosesText.text = CommonUtils.getSpannableString(
                clickableSpan,
                text,
                index,
            )
            binding.tvDiagnosesText.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    // The medication spinner stores the answer as "Yes" / "No"; null means the question wasn't
    // answered yet (e.g. opening bio-data before completing the review).
    private fun parseMedicationAdherence(compliance: String?): Boolean? =
        when {
            compliance.equals(DefinedParams.YES, ignoreCase = true) -> true
            compliance.equals(DefinedParams.NO, ignoreCase = true) -> false
            else -> null
        }

    private fun getPatientType(it: String?): String =
        when (it) {
            DefinedParams.CONTROLLED -> getString(R.string.controlled)
            DefinedParams.UN_CONTROLLED -> getString(R.string.un_controlled)
            else -> getString(R.string.hyphen_symbol)
        }

    private fun generateConditionString(patientHistory: PatientHistoryModel?): String {
        val conditions = mutableListOf<String>()

        DefinedParams.HEART_ATTACK
        if (patientHistory?.heartAttack.equals("yes", ignoreCase = true)) {
            conditions.add(DefinedParams.HEART_ATTACK)
        }
        if (patientHistory?.stroke.equals("yes", ignoreCase = true)) {
            conditions.add(DefinedParams.STROKE)
        }
        if (patientHistory?.kidneyDisease.equals("yes", ignoreCase = true)) {
            conditions.add(DefinedParams.KIDNEY_DISEASE)
        }
        if (patientHistory?.copd.equals("yes", ignoreCase = true)) {
            conditions.add(DefinedParams.COPD)
        }

        return if (conditions.isEmpty()) {
            getString(R.string.hyphen_symbol)
        } else {
            conditions.joinToString(
                ", ",
            )
        }
    }

    private val patientEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                nurseViewModel.patientIdString?.let {
                    nurseBioDataViewModel.getPatientDetails(requireContext(), it)
                }
            }
        }
}
