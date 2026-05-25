package org.medtroniclabs.uhis.ui.medicalreview.familyplan.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.data.AboveFiveYearsSummaryRequest
import org.medtroniclabs.uhis.databinding.ActivityFamilyPlanMedicalReviewBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dialog.MedicalReviewSuccessDialogFragment
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.medicalreview.ClinicalNotesFragment
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.familyplan.fragment.ContraceptivesFragment
import org.medtroniclabs.uhis.ui.medicalreview.familyplan.fragment.FamilyPlanningMRSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.familyplan.viewmodel.ContraceptivesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.familyplan.viewmodel.FamilyPlanViewModel
import org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.medicalreview.prescription.PrescriptionActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CLINICAL_NOTES
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CONTRACEPTIVES_ITEMS
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.ReferPatientFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

@AndroidEntryPoint
class FamilyPlanMedicalReviewActivity :
    BaseActivity(),
    AncVisitCallBack,
    View.OnClickListener,
    OnDialogDismissListener {
    private lateinit var binding: ActivityFamilyPlanMedicalReviewBinding
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val chipItemViewModel: ClinicalNotesViewModel by viewModels()
    private val contraceptivesViewModel: ContraceptivesViewModel by viewModels()
    private val viewModel: FamilyPlanViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityFamilyPlanMedicalReviewBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation()
            },
            callbackHome = {
                backNavigationToHome()
            },
        )

        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        initStaticDataCall()
        attachObserver()
        setButtonClickListener()
        patientViewModel.setUserJourney(AnalyticsDefinedParams.FamilyPlanningMedicalReview)
    }

    private fun attachObserver() {
        patientViewModel.patientDetailsLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {
                        if (it) {
                            onBackPressPopStack()
                        }
                    }
                }
            }
            if (binding.refreshLayout.isRefreshing) {
                binding.refreshLayout.isRefreshing = false
            }
        }

        viewModel.familyPlanningCreateLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    if (connectivityManager.isNetworkAvailable()) {
                        patientViewModel.isFamilyPlanning = true
                        patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                            details.patientId?.let { id ->
                                patientViewModel.getPatients(id)
                            }
                        }
                    } else {
                        showErrorDialogue(
                            getString(R.string.error),
                            getString(R.string.no_internet_error),
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {
                    }
                }
            }
        }

        viewModel.summaryCreateResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    MedicalReviewSuccessDialogFragment.newInstance().show(
                        supportFragmentManager,
                        MedicalReviewSuccessDialogFragment.TAG,
                    )
                }
            }
        }

        referPatientViewModel.referPatientResultLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    val fragment =
                        supportFragmentManager.findFragmentByTag(ReferPatientFragment.TAG) as? ReferPatientFragment
                    fragment?.dismiss()
                    MedicalReviewSuccessDialogFragment.newInstance().show(
                        supportFragmentManager,
                        MedicalReviewSuccessDialogFragment.TAG,
                    )
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun setButtonClickListener() {
        binding.btnSubmit.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
    }

    private fun initStaticDataCall() {
        if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_FAMILY_PLANNING_LOADED.name))) {
            if (connectivityManager.isNetworkAvailable()) {
                viewModel.getFamilyPlanStaticData(MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name)
            } else {
                showErrorDialogue(
                    getString(R.string.error),
                    getString(R.string.no_internet_error),
                    isNegativeButtonNeed = false,
                ) {}
            }
        }
        initView()
    }

    private fun initView() {
        initializePatientDetailFragment()
        supportFragmentManager
            .setFragmentResultListener(CLINICAL_NOTES, this) { _, _ ->
                enableSubmitBtn()
            }

        supportFragmentManager
            .setFragmentResultListener(CONTRACEPTIVES_ITEMS, this) { _, _ ->
                enableSubmitBtn()
            }
        supportFragmentManager
            .setFragmentResultListener(MedicalReviewDefinedParams.SUMMARY_ITEM, this) { _, _ ->
                enableReferralDoneBtn()
            }
    }

    private fun initializePatientDetailFragment() {
        replaceFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            PatientInfoFragment
                .newInstance(
                    intent.getStringExtra(DefinedParams.PatientId),
                    isFamilyPlan = true,
                    isFPSummary = viewModel.isFamilyPlanSummary,
                ).apply {
                    setDataCallback(this@FamilyPlanMedicalReviewActivity)
                },
        )
    }

    private fun swipeRefresh() {
        if (connectivityManager.isNetworkAvailable()) {
            supportFragmentManager
                .findFragmentById(R.id.patientDetailFragment)
                .let {
                    patientViewModel.getPatientId()?.let { id ->
                        patientViewModel.getPatients(id)
                    }
                }
        } else {
            showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
                if (binding.refreshLayout.isRefreshing) {
                    binding.refreshLayout.isRefreshing = false
                    startActivityWithoutSplashScreen()
                }
            }
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
            }
        }

    override fun onDataLoaded(details: PatientListRespModel) {
        viewModel.memberId = details.memberId
        viewModel.familyPlanningCreateLiveData.value?.data?.let {
            viewModel.getFamilyPlanningSummaryDetails(
                AboveFiveYearsSummaryRequest(
                    id = it.encounterId,
                    patientReference = it.patientReference,
                    type = MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name,
                ),
            )
            initializeSummaryFragment()
            binding.nestedScrollViewID.fullScroll(ScrollView.FOCUS_UP)
        } ?: kotlin.run {
            initializeFragments()
            replaceFragment(
                R.id.patientBMIContainer,
                MedicalReviewPatientDiagnosisFragment.TAG,
                MedicalReviewPatientDiagnosisFragment.newInstance(
                    isAnc = false,
                    isPnc = false,
                    isTB = false,
                    patientId = intent.getStringExtra(DefinedParams.PatientId),
                    memberID = viewModel.memberId,
                    id = intent.getStringExtra(DefinedParams.ID),
                    isFp = true,
                ),
            )
        }
    }

    private fun initializeFragments() {
        with(binding) {
            patientDetailFragment.visible()
            patientBMIContainer.visible()
            contraceptivesContainer.visible()
            familyPlanClinicalNotesContainer.visible()
        }

        addOrReuseFragment(
            R.id.patientBMIContainer,
            MedicalReviewPatientDiagnosisFragment.TAG,
            MedicalReviewPatientDiagnosisFragment.newInstance(
                isAnc = false,
                isPnc = false,
                isTB = false,
                patientId = intent.getStringExtra(DefinedParams.PatientId),
                memberID = viewModel.memberId,
                id = intent.getStringExtra(DefinedParams.ID),
                isFp = true,
            ),
        )

        replaceFragmentOrCreateNewFragment<ContraceptivesFragment>(
            binding.contraceptivesContainer.id,
            bundle = null,
            tag = ContraceptivesFragment.TAG,
        )

        replaceFragmentOrCreateNewFragment<ClinicalNotesFragment>(
            binding.familyPlanClinicalNotesContainer.id,
            bundle = null,
            tag = ClinicalNotesFragment.TAG,
        )
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.ivPrescription.id -> openPrescriptionActivity()
            binding.ivInvestigation.id -> openInvestigationActivity()
            binding.btnSubmit.id -> {
                validateInputRequest()
            }

            binding.btnRefer.id -> viewModel.familyPlanningCreateLiveData.value?.data?.let {
                ReferPatientFragment
                    .newInstance(
                        MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name,
                        it.patientReference,
                        it.encounterId,
                    ).show(
                        supportFragmentManager,
                        ReferPatientFragment.TAG,
                    )
            }

            binding.btnDone.id -> {
                withLocationCheck({
                    patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        viewModel.familyPlanningCreateLiveData.value
                            ?.data
                            ?.let { response ->
                                if (connectivityManager.isNetworkAvailable()) {
                                    response.encounterId?.let { submitEncounterId ->
                                        response.patientReference?.let { submitPatientReferenceId ->
                                            viewModel.familyPlanningSummaryCreate(
                                                details,
                                                submitEncounterId,
                                                submitPatientReferenceId,
                                            )
                                        }
                                    }
                                } else {
                                    showErrorDialogue(
                                        getString(R.string.error),
                                        getString(R.string.no_internet_error),
                                        isNegativeButtonNeed = false,
                                    ) {}
                                }
                            }
                    }
                })
            }
        }
    }

    private fun validateInputRequest() {
        val conFragment = getFragmentById(
            supportFragmentManager,
            (R.id.contraceptivesContainer),
        ) as? ContraceptivesFragment

        val clinicalNotesFragment = getFragmentById(
            supportFragmentManager,
            (R.id.familyPlanClinicalNotesContainer),
        ) as? ClinicalNotesFragment

        conFragment?.let {
            if (conFragment.validInputs()) {
                if (clinicalNotesFragment?.validateInput() == true) {
                    patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        patientViewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                        details.patientId?.let { id ->
                            viewModel.createFamilyPlanningMR(
                                details,
                                contraceptivesViewModel.getContraceptivesResult(),
                                patientViewModel.occupation,
                                patientViewModel.maritalStatus,
                                patientViewModel.encounterId,
                                chipItemViewModel.enteredClinicalNotes,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initializeSummaryFragment() {
        binding.apply {
            patientViewModel.setUserJourney(AnalyticsDefinedParams.FamilyPlanningSummary)
            binding.patientBMIContainer.gone()
            binding.contraceptivesContainer.gone()
            familyPlanClinicalNotesContainer.gone()
            binding.bottomNavigationView.gone()
            binding.referralBottomView.visible()
            binding.familyPlanSummaryContainer.visible()
        }
        replaceFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            PatientInfoFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId),
                isFamilyPlan = true,
                isFPSummary = viewModel.isFamilyPlanSummary,
            ),
        )

        replaceFragmentInId<FamilyPlanningMRSummaryFragment>(
            binding.familyPlanSummaryContainer.id,
            tag = FamilyPlanningMRSummaryFragment::class.simpleName,
        )
        val bmiFragment = supportFragmentManager.findFragmentById(R.id.patientBMIContainer)
        val contraceptivesFragment =
            supportFragmentManager.findFragmentById(R.id.contraceptivesContainer)
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.familyPlanClinicalNotesContainer)
        bmiFragment?.let {
            supportFragmentManager.beginTransaction().remove(bmiFragment).commit()
        }
        contraceptivesFragment?.let {
            supportFragmentManager.beginTransaction().remove(contraceptivesFragment).commit()
        }
        clinicalNotesFragment?.let {
            supportFragmentManager.beginTransaction().remove(clinicalNotesFragment).commit()
        }
    }

    private fun openPrescriptionActivity() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
            val intent = Intent(this, PrescriptionActivity::class.java)
            intent.putExtra(DefinedParams.PatientId, data.patientId)
            intent.putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
            getResult.launch(intent)
        }
    }

    private fun openInvestigationActivity() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
            val intent = Intent(this, InvestigationActivity::class.java)
            intent.putExtra(DefinedParams.PatientId, data.patientId)
            intent.putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
            getResult.launch(intent)
        }
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val value = it.data?.getStringExtra(DefinedParams.ENCOUNTER_ID)
                value?.let { valueString ->
                    patientViewModel.encounterId = valueString
                }
            }
        }

    private fun enableSubmitBtn() {
        binding.btnSubmit.isEnabled = (
            chipItemViewModel.enteredClinicalNotes.isNotBlank() ||
                contraceptivesViewModel.resultHashMap.size > 0
        )
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                viewModel.isFamilyPlanSummary = false
                startActivityWithoutSplashScreen()
            }
        }
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                viewModel.isFamilyPlanSummary = false
                onBackPressPopStack()
            }
        }
    }

    private fun onBackPressPopStack() {
        this@FamilyPlanMedicalReviewActivity.finish()
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            viewModel.lastLocation = it
        }
    }

    private fun enableReferralDoneBtn() {
        binding.btnDone.isEnabled = getSummaryStatus()
    }

    private fun getSummaryStatus(): Boolean = viewModel.nextFollowupDate != null

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }
}
