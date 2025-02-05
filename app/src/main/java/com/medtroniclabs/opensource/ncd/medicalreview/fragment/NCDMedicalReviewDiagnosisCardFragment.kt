package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.common.CommonUtils.convertListToString
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentNcdMedicalReviewDiagnosisCardBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.assessment.dialog.BPLogListDialog
import com.medtroniclabs.opensource.ncd.assessment.dialog.GlucoseLogListDialog
import com.medtroniclabs.opensource.ncd.assessment.ui.AssessmentReadingActivity
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.BloodPressureViewModel
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.GlucoseViewModel
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetRequest
import com.medtroniclabs.opensource.ncd.medicalreview.NCDDialogDismissListener
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.EncounterReference
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.IS_FEMALE
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.IS_INITIAL_MR
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENU_ID
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENU_Name
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewActivity
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDDiagnosisDialogFragment
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDMentalHealthQuestionDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPatientHistoryDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPregnancyDialog
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewDiagnosisCardViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDMedicalReviewDiagnosisCardFragment : BaseFragment(), View.OnClickListener,
    NCDDialogDismissListener {
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()
    private val medicalReviewDiagnosisCardViewModel: NCDMedicalReviewDiagnosisCardViewModel by activityViewModels()
    private lateinit var binding: FragmentNcdMedicalReviewDiagnosisCardBinding
    private val bpViewModel: BloodPressureViewModel by activityViewModels()
    private val glucoseViewModel: GlucoseViewModel by activityViewModels()
    private val viewModel: NCDMedicalReviewViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdMedicalReviewDiagnosisCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDMedicalReviewDiagnosisCardFragment"
        fun newInstance(visitId: String?, isInitial: Boolean, isFemale: Boolean, menu: String?,menuName:String?) =
            NCDMedicalReviewDiagnosisCardFragment().apply {
                arguments = Bundle().apply {
                    putString(EncounterReference, visitId)
                    putBoolean(IS_INITIAL_MR, isInitial)
                    putBoolean(IS_FEMALE, isFemale)
                    putString(MENU_ID, menu)
                    putString(MENU_Name, menuName)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        medicalReviewDiagnosisCardViewModel.getConfirmDiagonsis.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {

                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        val diag = data.diagnosis?.mapNotNull { it.name }
                            ?.let { convertListToString(ArrayList(it)) }
                        binding.diagnosisCard.tvDiagnosis.text = diag ?: getProvisionDiagnosis() ?: getString(R.string.hyphen_symbol)
                        binding.diagnosisCard.tvDiagnosisConfirm.text =
                            if (diag.isNullOrBlank())
                                getString(R.string.confirm_diagnoses)
                            else
                                getString(R.string.edit_diagnosis)
                    }
                }

                ResourceState.ERROR -> {

                }
            }
        }
        bpViewModel.bpLogListResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as? BaseActivity?)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as? BaseActivity?)?.hideLoading()
                    val bpLogListDialog = BPLogListDialog.newInstance { addNewReading(true) }
                    bpLogListDialog.show(childFragmentManager, BPLogListDialog.TAG)
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity?)?.hideLoading()
                    resourceState.message?.let { message ->
                        (activity as? BaseActivity?)?.showErrorDialogue(message = message) {}
                    }
                }
            }
        }
        glucoseViewModel.glucoseLogListResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as? BaseActivity?)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as? BaseActivity?)?.hideLoading()
                    val glucoseLogDialog = GlucoseLogListDialog.newInstance { addNewReading(false) }
                    glucoseLogDialog.show(childFragmentManager, GlucoseLogListDialog.TAG)
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity?)?.hideLoading()
                    resourceState.message?.let { message ->
                        (activity as? BaseActivity?)?.showErrorDialogue(message = message) {}
                    }
                }
            }
        }
    }

    private fun getProvisionDiagnosis(): String? {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.provisionalDiagnosis?.let {
            if (it.isNotEmpty())
                return "${it.joinToString()} ${getString(R.string.provisional_text)}"
        }
        return null
    }

    private fun getInitialMr(): Boolean {
        return arguments?.getBoolean(IS_INITIAL_MR) ?: false
    }

    private fun getIsFemale(): Boolean {
        return arguments?.getBoolean(IS_FEMALE) ?: false
    }

    private fun getMenu(): String? {
        return arguments?.getString(MENU_ID)?.lowercase()
    }

    private fun geEncounterReference(): String? {
        return arguments?.getString(EncounterReference)
    }

    private fun initView() {
        getDiagonsis()
        val hyphen = getString(R.string.hyphen_symbol)
        binding.apply {
            val isContinuous = getInitialMr()
            val isFemalePregnancy = isFemalePregnancy()
            val isMaternal = !getInitialMr() && isFemalePregnancy

            // Hide all cards initially
            pregnancyCard.root.setVisible(false)
            weightCard.root.setVisible(false)
            eddCard.root.setVisible(false)
            patientStatusCard.root.setVisible(false)
            when {
                isContinuous -> {
                    pregnancyCard.root.setVisible(isFemalePregnancy)
                    weightCard.root.setVisible(isFemalePregnancy)
                    eddCard.root.setVisible(isFemalePregnancy)
                }

                isMaternal -> {
                    pregnancyCard.root.setVisible(true)
                    weightCard.root.setVisible(true)
                    eddCard.root.setVisible(true)
                }
            }

            patientDetailViewModel.patientDetailsLiveData.value?.data?.let {
                if (isMentalHealth()) {
                    mentalHealthCard.apply {
                        val hasPHQ9: Boolean =
                            it.mentalHealthLevels?.contains(Screening.PHQ9) == true
                        val hasGAD7: Boolean =
                            it.mentalHealthLevels?.contains(Screening.GAD7) == true
                        root.setVisible(hasPHQ9 || hasGAD7)
                        if (root.isVisible()) {
                            clPHQ9.setVisible(hasPHQ9)
                            clGAD7.setVisible(hasGAD7)
                            if (it.phq9Score.isNullOrBlank()) {
                                phq9Value.text = getString(R.string.hyphen_symbol)
                                tvAssessmentPHQ9.text = getString(R.string.start_assessment)
                            } else {
                                phq9Value.text = it.phq9Score.textOrHyphen()
                                tvAssessmentPHQ9.text = getString(R.string.edit_assessment)
                            }
                            if (it.gad7Score.isNullOrBlank()) {
                                gad7Value.text = getString(R.string.hyphen_symbol)
                                tvAssessmentGAD7.text = getString(R.string.start_assessment)
                            } else {
                                gad7Value.text = it.gad7Score.textOrHyphen()
                                tvAssessmentGAD7.text = getString(R.string.edit_assessment)
                            }
                        }
                    }
                }
            }

            diagnosisCard.tvDiagnosisLbl.text = getString(R.string.diagnosis)
            diagnosisCard.tvDiagnosis.text = hyphen
            diagnosisCard.tvDiagnosisConfirm.text = getString(R.string.confirm_diagnoses)

            val latestBP = patientDetailViewModel.recentBP()
            bpCard.tvDiagnosisLbl.text = getString(R.string.blood_pressure)
            bpCard.tvDiagnosis.text = latestBP.ifBlank { hyphen }
            bpCard.tvDiagnosisConfirm.text =
                if (latestBP.isBlank()) getString(R.string.add_new_reading) else getString(R.string.view_details)

            val latestGlucose = patientDetailViewModel.recentGlucose()
            bgCard.tvDiagnosisLbl.text = getString(R.string.blood_glucose)
            bgCard.tvDiagnosis.text = latestGlucose.ifBlank { hyphen }
            bgCard.tvDiagnosisConfirm.text =
                if (latestGlucose.isBlank()) getString(R.string.add_new_reading) else getString(R.string.view_details)

            pregnancyCard.tvDiagnosisLbl.text = getString(R.string.pregnancy_details)
            pregnancyCard.tvDiagnosis.apply {
                text = getString(R.string.edit_details)
                setTextColor(getColor(requireContext(), R.color.medium_blue))
                typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            }
            pregnancyCard.tvDiagnosisConfirm.invisible()

            weightCard.tvDiagnosisLbl.text = getString(R.string.weight)
            weightCard.tvDiagnosis.text = MotherNeonateUtil.convertWeight(
                patientDetailViewModel.getWeightInKG(),
                requireContext()
            )
            weightCard.tvDiagnosisConfirm.invisible()

            val pregnantDetails = patientDetailViewModel.getPregnantDetails()
            eddCard.tvDiagnosisLbl.text =
                getString(R.string.estimated_delivery_date)
            eddCard.tvDiagnosis.text =
                pregnantDetails?.estimatedDeliveryDate?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy
                    ).takeIf { it.isNotBlank() } ?: hyphen
                } ?: hyphen
            eddCard.tvDiagnosisConfirm.text =
                pregnantDetails?.gestationalAge?.toLongOrNull()?.let { gestationalAge ->
                    DateUtils.formatGestationalAge(gestationalAge, requireContext())
                } ?: hyphen
            eddCard.tvDiagnosisConfirm.apply {
                setTextColor(getColor(requireContext(), R.color.text_label_color))
                typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            }
            patientStatusCard.tvDiagnosisLbl.text = getString(R.string.patient_status)
            patientStatusCard.tvDiagnosis.apply {
                text = getString(R.string.edit_details)
                setTextColor(getColor(requireContext(), R.color.medium_blue))
                typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            }
            patientStatusCard.tvDiagnosisConfirm.invisible()
            bpCard.tvDiagnosisConfirm.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
            bgCard.tvDiagnosisConfirm.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
            pregnancyCard.tvDiagnosis.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
            diagnosisCard.tvDiagnosisConfirm.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
            patientStatusCard.tvDiagnosis.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
            mentalHealthCard.tvAssessmentPHQ9.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
            mentalHealthCard.tvAssessmentGAD7.safeClickListener(this@NCDMedicalReviewDiagnosisCardFragment)
        }
    }

    private fun isFemalePregnancy(): Boolean {
        return getMenu().equals(DefinedParams.PregnancyANC, true) && getIsFemale()
    }

    private fun isMentalHealth(): Boolean {
        return getMenu().equals(NCDMRUtil.mentalHealth, true)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.diagnosisCard.tvDiagnosisConfirm -> {
                withNetworkAvailability(online = {
                    showDiagnosisDialog()
                })
            }

            binding.patientStatusCard.tvDiagnosis -> {
                withNetworkAvailability(online = {
                    showPatientHistoryDialog()
                })
            }

            binding.bpCard.tvDiagnosisConfirm -> {
                if (patientDetailViewModel.recentBP().isBlank())
                    addNewReading(true)
                else
                    patientDetailViewModel.getPatientFHIRId()?.let { id ->
                        bpViewModel.bpLogList(patientId = id)
                    }
            }

            binding.pregnancyCard.tvDiagnosis -> {
                withNetworkAvailability(online = {
                    val hasNCD =
                        !(viewModel.ncdPatientDiagnosisStatus.value?.data?.get(NCDMRUtil.NCDPatientStatus) as? Map<*, *>).isNullOrEmpty()
                    val initialReviewed = patientDetailViewModel.getNCDInitialMedicalReview()
                    patientDetailViewModel.getPatientFHIRId()?.let { id ->
                        val dialog = childFragmentManager.findFragmentByTag(NCDPregnancyDialog.TAG)
                        if (dialog == null) {
                            NCDPregnancyDialog.newInstance(
                                visitId = geEncounterReference(),
                                patientDetailViewModel.getPatientId(),
                                patientId = id,
                                patientDetailViewModel.getGenderIsFemale(),
                                patientDetailViewModel.isPregnant(),
                                showNCD = !initialReviewed || !hasNCD
                            ) { isPositiveResult, message ->
                                if (isPositiveResult) {
                                    showSuccessDialogue(
                                        title = getString(R.string.pregnancy_details),
                                        message = message,
                                    )
                                    (requireActivity() as? NCDMedicalReviewActivity)?.forceRefresh()
                                } else showErrorDialog(
                                    title = getString(R.string.error),
                                    message = message
                                )
                            }.apply {
                                listener = this@NCDMedicalReviewDiagnosisCardFragment
                            }.show(childFragmentManager, NCDPregnancyDialog.TAG)
                        }
                    }
                })
            }

            binding.bgCard.tvDiagnosisConfirm -> {
                withNetworkAvailability(online = {
                    if (patientDetailViewModel.recentGlucose().isBlank())
                        addNewReading(false)
                    else
                        patientDetailViewModel.getPatientFHIRId()?.let { id ->
                            glucoseViewModel.glucoseLogList(patientId = id)
                        }
                })
            }

            binding.mentalHealthCard.tvAssessmentPHQ9 -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let {
                    withNetworkAvailability(online = {
                        showMentalHealthDialog(AssessmentDefinedParams.phq9, it.phq9Score != null)
                    })
                }
            }

            binding.mentalHealthCard.tvAssessmentGAD7 -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let {
                    withNetworkAvailability(online = {
                        showMentalHealthDialog(AssessmentDefinedParams.gad7, it.gad7Score != null)
                    })
                }
            }
        }
    }

    private fun showDiagnosisDialog() {
        val dialog = childFragmentManager.findFragmentByTag(NCDDiagnosisDialogFragment.TAG)
        if (dialog == null) {
            NCDDiagnosisDialogFragment.newInstance(
                patientDetailViewModel.getPatientId(),
                patientDetailViewModel.getPatientFHIRId(),
                NCDMRUtil.getTypeForDiagnoses(getMenu()),
                patientDetailViewModel.getGenderIsFemale(),
                NCDMRUtil.getConfirmDiagnoses(getMenu()),
                patientDetailViewModel.isPregnant(),
                isDiagnosisMismatch = false,
                getMenu(),
                arguments?.getString(MENU_Name)
            ).apply {
                listener = this@NCDMedicalReviewDiagnosisCardFragment
            }.show(childFragmentManager, NCDDiagnosisDialogFragment.TAG)
        }
    }

    private fun showPatientHistoryDialog() {
        val dialog = childFragmentManager.findFragmentByTag(NCDPatientHistoryDialog.TAG)
        if (dialog == null) {
            patientDetailViewModel.getPatientId()?.let {
                NCDPatientHistoryDialog.newInstance(
                    visitId = geEncounterReference(),
                    patientReference = it,
                    memberReference = patientDetailViewModel.getPatientFHIRId(),
                    isFemale = patientDetailViewModel.getGenderIsFemale(),
                    isPregnant = patientDetailViewModel.isPregnant()
                ).apply {
                    listener = this@NCDMedicalReviewDiagnosisCardFragment
                }.show(childFragmentManager, NCDPatientHistoryDialog.TAG)
            }
        }
    }

    private fun showMentalHealthDialog(type: String, isEditAssessment: Boolean) {
        val dialog = childFragmentManager.findFragmentByTag(NCDMentalHealthQuestionDialog.TAG)
        if (dialog == null) {
            NCDMentalHealthQuestionDialog.newInstance(
                type,
                patientDetailViewModel.getPatientId(),
                patientDetailViewModel.getPatientFHIRId(),
                isEditAssessment
            ) { response, errorResponse ->
                if (errorResponse.isNullOrBlank() && response != null) {
                    val fragment = childFragmentManager.findFragmentByTag(GeneralSuccessDialog.TAG)
                    if (fragment == null) {
                        GeneralSuccessDialog.newInstance(
                            title = response.first,
                            message = response.second,
                            okayButton = getString(R.string.done)
                        ) {
                            (requireActivity() as? NCDMedicalReviewActivity)?.swipeRefresh()
                        }.show(childFragmentManager, GeneralSuccessDialog.TAG)
                    }
                } else {
                    (activity as BaseActivity?)?.showErrorDialogue(
                        title = getString(R.string.error),
                        message = errorResponse
                            ?: getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok)
                    ) {}
                }
            }.show(childFragmentManager, NCDPatientHistoryDialog.TAG)
        }
    }

    override fun onDialogDismissed(isConfirmed: Boolean) {
        // call the get method
        if (isConfirmed) {
            withNetworkAvailability(online = {
                (requireActivity() as? NCDMedicalReviewActivity)?.swipeRefresh()
            })
        } else {
            //patient status for NCD
            // now it is hide so it is not used now
            val dialogFragment =
                childFragmentManager.findFragmentByTag(NCDPatientHistoryDialog.TAG) as? NCDPatientHistoryDialog
            dialogFragment?.dismiss()
            (requireActivity() as? NCDMedicalReviewActivity)?.showCurrentMedication()
        }
    }

    override fun closePage() {
        (childFragmentManager.findFragmentByTag(NCDPregnancyDialog.TAG) as? NCDPregnancyDialog)?.dismiss()
    }

    private fun getDiagonsis() {
        patientDetailViewModel.getPatientId()?.let { patientId ->
            medicalReviewDiagnosisCardViewModel.getConfirmDiagonsis(
                NCDDiagnosisGetRequest(
                    patientReference = patientId,
                    diagnosisType = NCDMRUtil.getConfirmDiagnoses(getMenu())
                )
            )
        }
    }

    private fun addNewReading(isBP: Boolean) {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { detail ->
            val intent = Intent(requireActivity(), AssessmentReadingActivity::class.java)
            intent.putExtra(
                DefinedParams.FORM_TYPE_ID,
                if (isBP) DefinedParams.BP_LOG else DefinedParams.GLUCOSE_LOG
            )
            intent.putExtra(MENU_Name, arguments?.getString(MENU_Name))
            intent.putExtra(DefinedParams.IntentPatientDetails, detail)
            getResult.launch(intent)
        }
    }
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                (requireActivity() as? NCDMedicalReviewActivity)?.swipeRefresh()
            }
        }
}