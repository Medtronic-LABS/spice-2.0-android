package com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryRequest
import com.medtroniclabs.opensource.databinding.ActivityAboveFiveYearsBaseBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.dialog.MedicalReviewSuccessDialogFragment
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.ClinicalNotesFragment
import com.medtroniclabs.opensource.ui.medicalreview.PresentingComplaintsFragment
import com.medtroniclabs.opensource.ui.medicalreview.SystemicExaminationsFragment
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.medicalreview.prescription.PrescriptionActivity
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.CLINICAL_NOTES
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.PC_ITEM
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.SE_ITEM
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.SUMMARY_ITEM
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.ReferPatientFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.ReferPatientViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboveFiveYearsBaseActivity : BaseActivity(), View.OnClickListener, OnDialogDismissListener,
    AncVisitCallBack {

    private lateinit var binding: ActivityAboveFiveYearsBaseBinding
    private val viewModel: AboveFiveYearsViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val chipItemViewModel: ClinicalNotesViewModel by viewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by viewModels()
    private val systemicExaminationViewModel: SystemicExaminationViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityAboveFiveYearsBaseBinding.inflate(layoutInflater)
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
            }
        )
        initializeViews()
        initializeListeners()
        attachObserver()
        UserDetail.eventName=AnalyticsDefinedParams.MedicalReviewCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.AboveFiveYears)
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                supportFragmentManager.findFragmentById(R.id.presentingComplaintsContainer)
                    .let { currentFragment ->
                        if (currentFragment !is AboveFiveYearsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.AboveFiveYears,
                                exitReason = AnalyticsDefinedParams.BackButtonClicked,
                                eventName = UserDetail.eventName,
                                isCompleted = false
                            )
                        }
                    }
                onBackPressPopStack()

            }
        }
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                supportFragmentManager.findFragmentById(R.id.presentingComplaintsContainer)
                .let { currentFragment ->
                    if (currentFragment !is AboveFiveYearsTreatmentSummaryFragment) {
                        viewModel.setAnalyticsData(
                            UserDetail.startDateTime,
                            eventType = AnalyticsDefinedParams.AboveFiveYears,
                            exitReason = AnalyticsDefinedParams.HomeButtonClicked,
                            eventName = UserDetail.eventName,
                            isCompleted = false
                        )
                    }
                }
                startActivityWithoutSplashScreen()
            }
        }
    }

    private fun onBackPressPopStack() {
        this@AboveFiveYearsBaseActivity.finish()
    }

    private fun initializeListeners() {
        binding.btnSubmit.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
        binding.btnDone.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
    }

    private fun swipeRefresh() {
        presentingComplaintsViewModel.isMotherPnc=true
        supportFragmentManager.findFragmentById(R.id.presentingComplaintsContainer)
            .let { currentFragment ->
                if (currentFragment is AboveFiveYearsTreatmentSummaryFragment) {
                    viewModel.aboveFiveYearsCreateResponse.value?.data?.let {
                        if (connectivityManager.isNetworkAvailable()) {
                            viewModel.getAboveFiveYearsSummaryDetails(
                                AboveFiveYearsSummaryRequest(
                                    id = it.encounterId,
                                    patientReference = it.patientReference,
                                    type = MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name
                                )
                            )
                        } else {
                            showErrorDialogue(
                                getString(R.string.error), getString(R.string.no_internet_error),
                                isNegativeButtonNeed = false,
                            ) {}
                        }
                        viewModel.getAboveFiveYearsSummaryDetails(
                            AboveFiveYearsSummaryRequest(
                                id = it.id,
                                type = MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name
                            )
                        )
                    }
                } else {
                    patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        details.patientId?.let { id ->
                            patientViewModel.getPatients(id, origin = null)
                        }
                    }
                }
            }
    }

    private fun initializeViews() {
        if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_ABOVE_FIVE_YEARS_LOADED.name))) {
            if (connectivityManager.isNetworkAvailable()) {
                viewModel.getStaticMetaData(MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name)
                addPatientDetails()
            } else {
                showErrorDialogue(
                    getString(R.string.error), getString(R.string.no_internet_error),
                    isNegativeButtonNeed = false,
                ) {}
            }
        } else {
            addPatientDetails()
        }
        supportFragmentManager
            .setFragmentResultListener(PC_ITEM, this) { _, _ ->
                enableSubmitBtn()
            }
        supportFragmentManager
            .setFragmentResultListener(SE_ITEM, this) { _, _ ->
                enableSubmitBtn()
            }
        supportFragmentManager
            .setFragmentResultListener(CLINICAL_NOTES, this) { _, _ ->
                enableSubmitBtn()
            }
        supportFragmentManager
            .setFragmentResultListener(SUMMARY_ITEM, this) { _, _ ->
                enableReferralDoneBtn()
            }
    }

    private fun addPatientDetails() {
        val patientInfoFragment =
            PatientInfoFragment.newInstance(intent.getStringExtra(DefinedParams.PatientId))
        supportFragmentManager.beginTransaction()
            .add(
                R.id.patientDetailFragment,
                patientInfoFragment
            ).commit()
        patientInfoFragment.setDataCallback(this)
    }

    private fun attachObserver() {
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
                        MedicalReviewSuccessDialogFragment.TAG
                    )
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        viewModel.aboveFiveYearsMetaLiveData.observe(this) { resourceState ->
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
                        if (it) {
                            onBackPressPopStack()
                        }
                    }
                }

                ResourceState.SUCCESS -> {
                }
            }
        }
        viewModel.aboveFiveYearsCreateResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        if (connectivityManager.isNetworkAvailable()) {
                            patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                                details.patientId?.let { id ->
                                    patientViewModel.getPatients(id, origin = null)
                                }
                            }
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.AboveFiveYears,
                                eventName = UserDetail.eventName,
                            )
                        } else {
                            showErrorDialogue(
                                getString(R.string.error), getString(R.string.no_internet_error),
                                isNegativeButtonNeed = false,
                            ) {}
                        }
                    }
                }
            }
        }
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
                        MedicalReviewSuccessDialogFragment.TAG
                    )
                }
            }
        }
    }

    private fun initializeSummaryFragments() {
        binding.apply {
            patientDiagnosisContainer.visibility = View.GONE
            bottomNavContainer.visibility = View.INVISIBLE
            referalBottomView.visibility = View.VISIBLE
        }
        replaceFragmentInId<AboveFiveYearsTreatmentSummaryFragment>(
            binding.presentingComplaintsContainer.id,
            tag = AboveFiveYearsTreatmentSummaryFragment::class.simpleName
        )
        val complaintsFragment =
            supportFragmentManager.findFragmentById(R.id.systemicExaminationsContainer)
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer)
        complaintsFragment?.let {
            supportFragmentManager.beginTransaction().remove(complaintsFragment).commit()
        }
        clinicalNotesFragment?.let {
            supportFragmentManager.beginTransaction().remove(clinicalNotesFragment).commit()
        }
    }

    private fun initializeFragments() {
        binding.patientDiagnosisContainer.visibility = View.VISIBLE
        val bundle = Bundle().apply {
            putString(
                MedicalReviewTypeEnums.PresentingComplaints.name,
                MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name
            )
            putString(
                MedicalReviewTypeEnums.DiagnosisType.name,
                MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name
            )
            putString(
                DefinedParams.ID,
                intent.getStringExtra(DefinedParams.ID)
            )
        }
        replaceFragmentInId<MedicalReviewPatientDiagnosisFragment>(
            binding.patientDiagnosisContainer.id,
            bundle = bundle,
            tag = MedicalReviewPatientDiagnosisFragment::class.simpleName
        )
        replaceFragmentInId<PresentingComplaintsFragment>(
            binding.presentingComplaintsContainer.id,
            bundle = bundle,
            tag = PresentingComplaintsFragment::class.simpleName
        )
        replaceFragmentInId<SystemicExaminationsFragment>(
            binding.systemicExaminationsContainer.id,
            bundle = bundle,
            tag = SystemicExaminationsFragment::class.simpleName
        )
        replaceFragmentInId<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id,
            bundle = bundle,
            tag = ClinicalNotesFragment::class.simpleName
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                postResultInput()
            }

            binding.ivPrescription.id -> {
                patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, PrescriptionActivity::class.java)
                    intent.putExtra(DefinedParams.PatientId, data.patientId)
                    intent.putExtra(DefinedParams.EncounterId,patientViewModel.encounterId)
                    getResult.launch(intent)
                }
            }
            binding.ivInvestigation.id  -> {
                patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, InvestigationActivity::class.java)
                    intent.putExtra(DefinedParams.PatientId, data.patientId)
                    intent.putExtra(DefinedParams.EncounterId,patientViewModel.encounterId)
                    getResult.launch(intent)
                }
            }
            binding.btnRefer.id ->
                viewModel.aboveFiveYearsCreateResponse.value?.data?.let {
                    ReferPatientFragment.newInstance(
                        MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name,
                        it.patientReference,
                        it.encounterId
                    )
                        .show(
                            supportFragmentManager,
                            ReferPatientFragment.TAG
                        )
                }

            binding.btnDone.id -> {
                patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                    viewModel.aboveFiveYearsCreateResponse.value?.data
                        ?.let { response ->
                            if (connectivityManager.isNetworkAvailable()) {
                                response.encounterId?.let { submitEncounterId ->
                                    response.patientReference?.let {submitPatientReferenceId ->
                                        viewModel.aboveFiveYearsSummaryCreate(details, submitEncounterId, submitPatientReferenceId)
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
            }
        }
    }

    private fun postResultInput() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
            details.patientId?.let { id ->
                if (connectivityManager.isNetworkAvailable()) {
                    viewModel.createAboveFiveYearsResult(
                        details,
                        Pair(presentingComplaintsViewModel.selectedPresentingComplaints.map { it.value },
                            systemicExaminationViewModel.selectedSystemicExaminations.map { it.value }),
                        Triple(
                            presentingComplaintsViewModel.enteredComplaintNotes,
                            systemicExaminationViewModel.enteredExaminationNotes,
                            chipItemViewModel.enteredClinicalNotes
                        ),
                        patientViewModel.encounterId
                    )
                } else {
                    showErrorDialogue(
                        getString(R.string.error), getString(R.string.no_internet_error),
                        isNegativeButtonNeed = false,
                    ) {}
                }
            }
        }
    }

    private fun enableSubmitBtn() {
        binding.btnSubmit.isEnabled = chipItemViewModel.enteredClinicalNotes.isNotBlank()
    }

    private fun enableReferralDoneBtn() {
        binding.btnDone.isEnabled = getSummaryStatus()
    }

    private fun getSummaryStatus(): Boolean {
        return ((viewModel.selectedPatientStatus == ReferralStatus.Recovered.name && viewModel.nextFollowupDate == null)
                || (viewModel.selectedPatientStatus != ReferralStatus.Recovered.name && viewModel.nextFollowupDate != null))
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            viewModel.lastLocation = it
        }
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        viewModel.aboveFiveYearsCreateResponse.value?.data?.let {
            viewModel.getAboveFiveYearsSummaryDetails(
                AboveFiveYearsSummaryRequest(
                    id = it.encounterId,
                    patientReference = it.patientReference,
                    type = MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name
                )
            )
            initializeSummaryFragments()
            binding.nestedScrollViewID.fullScroll(ScrollView.FOCUS_UP)
        } ?: kotlin.run {
            initializeFragments()
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
            }
        }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val value = it.data?.getStringExtra(DefinedParams.EncounterId)
                value?.let { valueString ->
                    patientViewModel.encounterId = valueString
                }
            }
        }
}