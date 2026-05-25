package org.medtroniclabs.uhis.ui.patient

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.databinding.ActivityNurseMedicalReviewBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.patient.IntentConstants.NURSE_RESPONE
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment
import org.medtroniclabs.uhis.ui.patient.fragment.BloodPressureFragment
import org.medtroniclabs.uhis.ui.patient.fragment.InvestigationNurseFragment
import org.medtroniclabs.uhis.ui.patient.fragment.NurseBioDataFragment
import org.medtroniclabs.uhis.ui.patient.fragment.NurseDiagnosesFragment
import org.medtroniclabs.uhis.ui.patient.fragment.NurseFollowUpFragment
import org.medtroniclabs.uhis.ui.patient.fragment.NurseMedicalReviewPrescriptionFragment
import org.medtroniclabs.uhis.ui.patient.fragment.SymptomsAdherenceFragment
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import kotlin.getValue

@AndroidEntryPoint
class NurseMedicalReviewActivity : BaseActivity(), View.OnClickListener {
    lateinit var binding: ActivityNurseMedicalReviewBinding
    private val viewModel: MedicalReviewBaseViewModel by viewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNurseMedicalReviewBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            homeAndBackVisibility = Pair(true, null),
            callback = {
                showAlertOnBackPress()
            },
            callbackHome = {
                onHomeIconClicked()
            },
        )
        initView()
        attachObserver()
        swipeRefresh()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showAlertOnBackPress()
            }
        }

    fun onHomeIconClicked() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) {
            if (it) {
                startAsNewActivity(Intent(this, LandingActivity::class.java))
            }
        }
    }

    private fun showAlertOnBackPress() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                startAsNewActivity(Intent(this, LandingActivity::class.java))
            }
        }
    }

    private fun attachObserver() {
        nurseViewModel.patientDetailsResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    showLoading()
                    resourceState.data?.let { data ->
                        viewModel.initialReview = data.initialReview
                        nurseViewModel.initialReview = data.initialReview

                        nurseViewModel.unselectedDiagnosis = data.unselectedDiagnosis

                        nurseViewModel.patientDetailsValue = data
                        loadFragment()
                        nurseViewModel.patientTrackId = data._id
                        nurseViewModel.nurseMrRequestModel.patientTrackId = data._id
                        nurseViewModel.nurseMrRequestModel.patientVisitId =
                            intent.getLongExtra(IntentConstants.INTENT_VISIT_ID, -1L)
                        nurseViewModel.nurseMrRequestModel.tenantId = data.tenantId
                        data.firstName?.let { firstName ->
                            val text =
                                StringConverter.appendTexts(firstText = firstName, data.lastName)
                            setTitle(
                                StringConverter.appendTexts(
                                    firstText = CommonUtils.capitalize(text),
                                    data.age?.toInt().toString(),
                                    data.gender,
                                    separator = "-",
                                ),
                            )
                        }
                    }
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(
                            getString(R.string.error),
                            it,
                            false,
                        ) {}
                    }
                }
            }
        }
        nurseViewModel.nurseCreateResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { data ->
                        finish()
                        val intent = Intent(this, NurseMedicalReviewSummaryActivity::class.java)
                        val bundle = Bundle()
                        nurseViewModel.patientId?.let { patientId ->
                            bundle.putLong(IntentConstants.INTENT_PATIENT_ID, patientId)
                        }
                        nurseViewModel.patientVisitId?.let { patientVisitId ->
                            bundle.putLong(IntentConstants.INTENT_VISIT_ID, patientVisitId)
                        }
                        bundle.putSerializable(NURSE_RESPONE, data)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(
                            getString(R.string.error),
                            it,
                            false,
                        ) {}
                    }
                }
            }
        }
    }

    private fun initView() {
        binding.btnSubmit.safeClickListener(this)
        viewModel.showContinuousMedicalReview =
            intent.getBooleanExtra(IntentConstants.SHOW_CONTINUOUS_MEDICAL_REVIEW, false)
        viewModel.origin = intent.getStringExtra(DefinedParams.ORIGIN)
        val patientId = intent.getLongExtra(IntentConstants.INTENT_PATIENT_ID, -1L)
        nurseViewModel.patientId = patientId
        val visitId = intent.getLongExtra(IntentConstants.INTENT_VISIT_ID, -1L)
        nurseViewModel.patientVisitId = visitId
    }

    private fun loadFragment() {
        if (!viewModel.initialReview) {
            showFragments()
        } else {
            replaceFragment(
                binding.symptomsAdherenceFragment.id,
                SymptomsAdherenceFragment.TAG,
                SymptomsAdherenceFragment.newInstance(),
            )
            showFragments()
        }
    }

    private fun showFragments() {
        replaceFragment(
            binding.bioDataFragment.id,
            NurseBioDataFragment.TAG,
            NurseBioDataFragment.newInstance(),
        )

        replaceFragment(
            binding.BgFragment.id,
            BloodGlucoseFragment.TAG,
            BloodGlucoseFragment.newInstance(),
        )

        replaceFragment(
            binding.BpFragment.id,
            BloodPressureFragment.TAG,
            BloodPressureFragment.newInstance(),
        )

        replaceFragment(
            binding.prescriptionFragment.id,
            NurseMedicalReviewPrescriptionFragment.TAG,
            NurseMedicalReviewPrescriptionFragment.newInstance(),
        )
        replaceFragment(
            binding.labTestFragment.id,
            InvestigationNurseFragment.TAG,
            InvestigationNurseFragment.newInstance(),
        )

        replaceFragment(
            binding.diagnosisFragment.id,
            NurseDiagnosesFragment.TAG,
            NurseDiagnosesFragment.newInstance(),
        )

        replaceFragment(
            binding.followUpFragment.id,
            NurseFollowUpFragment.TAG,
            NurseFollowUpFragment.newInstance(),
        )
        hideLoading()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(view: View?) {
        var isValid = true
        when (view?.id) {
            R.id.btnSubmit -> {
                val diagnosesFragment =
                    supportFragmentManager.findFragmentById(R.id.diagnosisFragment) as? NurseDiagnosesFragment
                if (diagnosesFragment?.validation() != true) {
                    isValid = false
                    binding.nestedScrollViewID.post {
                        binding.nestedScrollViewID.smoothScrollTo(
                            0,
                            binding.diagnosisFragment.top,
                        )
                    }
                }
                val bloodGlucoseFragment =
                    supportFragmentManager.findFragmentById(R.id.BgFragment) as? BloodGlucoseFragment
                if (bloodGlucoseFragment?.validation(true) != true) {
                    isValid = false
                }
                val symptomsAdherenceFragment =
                    supportFragmentManager.findFragmentById(R.id.symptomsAdherenceFragment) as? SymptomsAdherenceFragment
                val bloodPressureFragment =
                    supportFragmentManager.findFragmentById(R.id.BpFragment) as? BloodPressureFragment

                if (bloodPressureFragment?.validation() != true) {
                    binding.nestedScrollViewID.post {
                        binding.nestedScrollViewID.smoothScrollTo(0, binding.BpFragment.top)
                    }
                    isValid = false
                }
                if (nurseViewModel.initialReview) {
                    if (symptomsAdherenceFragment?.validation() != true) {
                        isValid = false
                        binding.nestedScrollViewID.post {
                            binding.nestedScrollViewID.smoothScrollTo(
                                0,
                                binding.symptomsAdherenceFragment.top,
                            )
                        }
                    }
                }

                val prescriptionFragment =
                    supportFragmentManager.findFragmentById(R.id.prescriptionFragment) as? NurseMedicalReviewPrescriptionFragment
                if (prescriptionFragment?.createOrUpdatePrescription() != true) {
                    isValid = false
                    binding.nestedScrollViewID.post {
                        binding.nestedScrollViewID.smoothScrollTo(
                            0,
                            binding.prescriptionFragment.top,
                        )
                    }
                }

                val investigationNurseFragment =
                    supportFragmentManager.findFragmentById(R.id.labTestFragment) as? InvestigationNurseFragment

                val followUpFragment =
                    supportFragmentManager.findFragmentById(R.id.followUpFragment) as? NurseFollowUpFragment
                followUpFragment?.validation()

                if (isValid) {
                    investigationNurseFragment?.validation(isValid)
                    nurseViewModel.createNurseMedicalReview(
                        this,
                        nurseViewModel.nurseMrRequestModel,
                    )
                }
            }
        }
    }

    private fun swipeRefresh() {
        val request = nurseViewModel.patientId?.let {
            PatientDetailsModel(
                it,
                isAssessmentDataRequired = false,
            )
        }
        if (request != null) {
            nurseViewModel.getPatientDetails(this, request)
        }
        val symptomsAdherenceFragment =
            supportFragmentManager.findFragmentById(R.id.symptomsAdherenceFragment) as? SymptomsAdherenceFragment
        symptomsAdherenceFragment?.resetSelection()
        val diagnosesFragment =
            supportFragmentManager.findFragmentById(R.id.diagnosisFragment) as? NurseDiagnosesFragment
        diagnosesFragment?.resetSelection()
    }

    fun goToPrescriptionList() {
        val prescriptionFragment =
            supportFragmentManager.findFragmentById(R.id.prescriptionFragment) as? NurseMedicalReviewPrescriptionFragment
        prescriptionFragment?.repeatedPrescriptionView()?.let { repeatedPrescriptionView ->
            if (repeatedPrescriptionView.isVisible) {
                binding.nestedScrollViewID.post {
                    binding.nestedScrollViewID.post {
                        val scrollViewHeight = binding.nestedScrollViewID.height
                        val targetY =
                            binding.prescriptionFragment.top - (scrollViewHeight - binding.prescriptionFragment.height)
                        binding.nestedScrollViewID.smoothScrollTo(0, targetY)
                    }
                }
            }
        }
    }
}
