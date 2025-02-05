package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isNotTabletAndPortrait
import com.medtroniclabs.opensource.appextensions.setPercentWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.data.model.BpAndWeightRequestModel
import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.ActivityMedicalReviewAncactivityBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.dialog.MedicalReviewSuccessDialogFragment
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.ClinicalNotesFragment
import com.medtroniclabs.opensource.ui.medicalreview.PresentingComplaintsFragment
import com.medtroniclabs.opensource.ui.medicalreview.SystemicExaminationsFragment
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.PresentingComplaintsViewModel
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.SystemicExaminationViewModel
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.MotherNeonateAncHistoryFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.MotherNeonateAncSummary
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.PregnancyDetailsFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.PregnancyPastObstetricHistoryFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.PregnancySummaryFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.AddBpViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.AddWeightViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.MotherNeonateANCViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.MotherNeonateSummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.prescription.PrescriptionActivity
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.ReferPatientFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.MotherNeonateBpWeightViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PregnancyDetailsViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PregnancyPastObstetricHistoryViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.ReferPatientViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MotherNeonateANCActivity : BaseActivity(), View.OnClickListener, AncVisitCallBack,
    OnDialogDismissListener {

    private lateinit var binding: ActivityMedicalReviewAncactivityBinding
    private val viewModel: MotherNeonateANCViewModel by viewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by viewModels()
    private val systemicExaminationViewModel: SystemicExaminationViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val clinicalNotesViewModel: ClinicalNotesViewModel by viewModels()
    private val pregnancyPastObstetricHistoryViewModel: PregnancyPastObstetricHistoryViewModel by viewModels()
    private val pregnancyDetailsViewModel: PregnancyDetailsViewModel by viewModels()
    private val motherNeonateSummaryViewModel: MotherNeonateSummaryViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()
    private val bpViewModel: AddBpViewModel by viewModels()
    private val weightViewModel: AddWeightViewModel by viewModels()
    private val motherNeonateBpWeightViewModel: MotherNeonateBpWeightViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityMedicalReviewAncactivityBinding.inflate(layoutInflater)
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
        attachObservers()
        getCurrentLocation()
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        initStaticDataCall()
        setButtonClickListener()
        binding.loadingProgress.safeClickListener(this)
        UserDetail.eventName= AnalyticsDefinedParams.MedicalReviewCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.MotherNeonateAnc)
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            viewModel.lastLocation = it
        }
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                val patientDetails = supportFragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner)
                if (patientDetails !is MotherNeonateAncSummary ) {
                    viewModel.setAnalyticsData(
                        UserDetail.startDateTime,
                        eventType = AnalyticsDefinedParams.MotherNeonateAnc,
                        exitReason = AnalyticsDefinedParams.HomeButtonClicked,
                        eventName = UserDetail.eventName,
                        isCompleted = false
                    )
                }
                startActivityWithoutSplashScreen()
            }
        }
    }

    private fun initStaticDataCall() {
        if (!SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_MOTHER_NEONATE_LOADEDANC.name)) {
            viewModel.getMotherNeoNateAncStaticData()
        } else {
            initView()
        }
    }

    private fun attachObservers() {
        bpViewModel.saveBloodPressure.observe(this) { resourcesState ->
            when (resourcesState.state) {
                ResourceState.LOADING -> {
                }

                ResourceState.SUCCESS -> {
                    if (viewModel.ancVisit > 1) {
                        handleSubmit(false)
                    } else {
                        handleSubmit()
                    }
                }

                ResourceState.ERROR -> {
                    resourcesState.optionalData?.let {

                    } ?: showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {

                    }
                }
            }
        }
        weightViewModel.saveWeight.observe(this) { resourcesState ->
            when (resourcesState.state) {
                ResourceState.LOADING -> {
                }

                ResourceState.SUCCESS -> {
                    if (viewModel.ancVisit > 1) {
                        handleSubmit(false)
                    } else {
                        handleSubmit()
                    }
                    saveBloodPressureIfNeeded(pregnancyDetailsViewModel, bpViewModel, viewModel)
                }

                ResourceState.ERROR -> {
                    resourcesState.optionalData?.let {

                    } ?: showErrorDialogue(
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
                        MedicalReviewSuccessDialogFragment.TAG
                    )
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
                    if (binding.refreshLayout.isRefreshing) {
                        binding.refreshLayout.isRefreshing = false
                    }
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
        }

        viewModel.motherNeonateMetaResponse.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
//                    hideLoading()
                    initView()
                }

                ResourceState.ERROR -> {
//                    hideLoading()
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
        }


        viewModel.motherNeonateCreateResponse.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let {
                        binding.loadingProgress.visible()
                        handleSummary()
                        viewModel.setAnalyticsData(
                            UserDetail.startDateTime,
                            eventType = AnalyticsDefinedParams.MotherNeonateAnc,
                            eventName = UserDetail.eventName,
                        )
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
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
                        MedicalReviewSuccessDialogFragment.TAG
                    )
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        pregnancyPastObstetricHistoryViewModel.checkSubmitBtn.observe(this) {
            isPregnancyDetailsAndHistoryValidation()
        }
        pregnancyDetailsViewModel.checkSubmitBtn.observe(this) {
            isPregnancyDetailsAndHistoryValidation()
        }
        motherNeonateSummaryViewModel.checkSubmitBtn.observe(this) {
            isMotherNeonateSummaryValidation()
        }
    }
    private fun isMotherNeonateSummaryValidation() {
        binding.btnDone.isEnabled =
            motherNeonateSummaryViewModel.nextFollowupDate?.isNotBlank() == true || motherNeonateSummaryViewModel.patientStatus?.isNotBlank() == true
    }
    private fun isPregnancyDetailsAndHistoryValidation() {
        val model = pregnancyDetailsViewModel.pregnancyDetailsModel
        binding.btnLayout.btnNext.isEnabled =
            pregnancyPastObstetricHistoryViewModel.pregnancyHistoryNotes?.isNotBlank() == true || pregnancyPastObstetricHistoryViewModel.pregnancyHistoryChip.isNotEmpty() || pregnancyPastObstetricHistoryViewModel.resultFlowHashMap[PregnancyPastObstetricHistoryFragment.TAG] != null || model.height != null ||
                    model.weight != null ||
                    model.pulse != null ||
                    model.lastMenstrualPeriod != null ||
                    model.estimatedDeliveryDate != null ||
                    model.noOfFetus != null ||
                    model.gravida != null ||
                    model.parity != null ||
                    model.patientBloodGroup != null ||
                    model.systolic != null ||
                    model.diastolic != null
    }

    private fun swipeRefresh() {
        if (connectivityManager.isNetworkAvailable()) {
            supportFragmentManager.findFragmentById(R.id.patientDetailFragment)
                .let {
                    patientViewModel.getPatientId()?.let { id ->
                        patientViewModel.getPatients(id, origin = null)
                    }
                }
        } else {
            showErrorDialogue(
                getString(R.string.error), getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
                if (binding.refreshLayout.isRefreshing) {
                    binding.refreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun initView() {
        initializePatientDetailFragment()
    }

    private fun initializePatientDetailFragment() {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentById(R.id.patientDetailFragment)
        if (existingFragment == null) {
            val fragment =
                PatientInfoFragment.newInstance(
                    intent.getStringExtra(DefinedParams.PatientId),
                    isAnc = true
                )
            fragment.setDataCallback(this)
            fragmentManager.beginTransaction()
                .add(R.id.patientDetailFragment, fragment)
                .commit()
        } else if (existingFragment !is PatientInfoFragment) {
            val fragment =
                PatientInfoFragment.newInstance(
                    intent.getStringExtra(DefinedParams.PatientId),
                    isAnc = true
                )
            fragment.setDataCallback(this)
            fragmentManager.beginTransaction()
                .replace(R.id.patientDetailFragment, fragment)
                .commit()
        }
    }

    private fun initializePregnancyDetailsFragment() {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner)

        if (existingFragment == null) {
            fragmentManager.beginTransaction()
                .add(R.id.pregnancyDetailsConatiner, PregnancyDetailsFragment.newInstance(patientViewModel.getPatientLmb()))
                .commit()
        } else if (existingFragment !is PregnancyDetailsFragment) {
            fragmentManager.beginTransaction()
                .replace(R.id.pregnancyDetailsConatiner, PregnancyDetailsFragment.newInstance(patientViewModel.getPatientLmb()))
                .commit()
        }
    }

    private fun initializePregnancyHistoryFragment() {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentById(R.id.pregnancyHistoryConatiner)

        if (existingFragment == null) {
            fragmentManager.beginTransaction()
                .add(
                    R.id.pregnancyHistoryConatiner,
                    PregnancyPastObstetricHistoryFragment.newInstance()
                )
                .commit()
        } else if (existingFragment !is PregnancyPastObstetricHistoryFragment) {
            fragmentManager.beginTransaction()
                .replace(
                    R.id.pregnancyHistoryConatiner,
                    PregnancyPastObstetricHistoryFragment.newInstance()
                )
                .commit()
        }
    }


    private fun hideContainers() {
        with(binding) {
            presentingComplaintsContainer.gone()
            systemicExaminationsContainer.gone()
            clinicalNotesContainer.gone()
            bottomNavigationView.gone()
            btnLayout.btnNext.visible()
        }
    }

    private fun setButtonClickListener() {
        binding.btnLayout.btnNext.safeClickListener(this)
        binding.btnSubmit.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
    }

    private fun setButtonWidth() {
        val width = if (isNotTabletAndPortrait()) 0.4f else 0.2f
        binding.btnLayout.root.setPercentWidth(binding.btnLayout.btnNext.id, width)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnLayout.btnNext.id -> validatePregnantDetails()
            binding.ivPrescription.id -> openPrescriptionActivity()
            binding.ivInvestigation.id -> openInvestigationActivity()
            binding.btnSubmit.id -> validateAndSubmitRequest()
            binding.btnDone.id -> submitSummary()
            binding.btnRefer.id -> showReferPatientDialog()
            binding.loadingProgress.id -> {}
        }
    }

    private fun openInvestigationActivity() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
            val intent = Intent(this, InvestigationActivity::class.java)
            intent.putExtra(DefinedParams.PatientId, data.patientId)
            intent.putExtra(DefinedParams.EncounterId,patientViewModel.encounterId)
            getResult.launch(intent)
        }
    }

    private fun validateAndSubmitRequest() {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.systemicExaminationsContainer) as? SystemicExaminationsFragment
        val clFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? ClinicalNotesFragment
        if (fragment != null && clFragment != null) {
            val isFragmentValid = fragment.validateInput()
            val isClFragmentValid = clFragment.validateInput()

            if (isFragmentValid && isClFragmentValid) {
                submitRequest(patientViewModel.encounterId)
            }
        }
    }

    private fun showReferPatientDialog() {
        viewModel.motherNeonateCreateResponse.value?.data?.let {
            ReferPatientFragment.newInstance(
                MedicalReviewTypeEnums.ANC_REVIEW.name,
                it.patientReference,
                it.encounterId
            ).show(supportFragmentManager, ReferPatientFragment.TAG)
        }
    }

    private fun openPrescriptionActivity() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
            val intent = Intent(this, PrescriptionActivity::class.java)
            intent.putExtra(DefinedParams.PatientId, data.patientId)
            intent.putExtra(DefinedParams.EncounterId, patientViewModel.encounterId)
            getResult.launch(intent)
        }
    }

    private fun submitSummary() {
        val motherNeonateSummary =
            supportFragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner) as? MotherNeonateAncSummary
        val isValidSummary = motherNeonateSummary?.validateInput() ?: false

        if (isValidSummary) {
            val submitCreateId = viewModel.getSubmitCreateId()
            val nextVisitDate = DateUtils.convertDateTimeToDate(
                motherNeonateSummaryViewModel.nextFollowupDate,
                DateUtils.DATE_ddMMyyyy,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                inUTC = true
            )
            if (connectivityManager.isNetworkAvailable()) {
                viewModel.motherNeonateSummaryCreate(
                    MedicalReviewTypeEnums.RMNCH.name,
                    patientViewModel.getPatientMemberId(),
                    submitCreateId,
                    patientViewModel.getPatientHouseholdId(),
                    viewModel.getPatientReference(),
                    nextVisitDate,
                    motherNeonateSummaryViewModel.patientStatus,
                    patientViewModel.getVillageId(),
                    patientViewModel.getPatientId(),
                    MedicalReviewTypeEnums.ANC_REVIEW.name
                )
            } else {
                showErrorDialogue(
                    getString(R.string.error), getString(R.string.no_internet_error),
                    isNegativeButtonNeed = false,
                ) {}
            }
        }
    }


    private fun submitRequest(prescriptionEncounterId: String?) {
        createMotherNeonateRequest(prescriptionEncounterId)
        if (connectivityManager.isNetworkAvailable()) {
            viewModel.createMotherNeonate(
                patientViewModel.encounterId,
                patientViewModel.getPatientHouseholdId(),
                patientViewModel.getPatientMemberId()
            )
        } else {
            showErrorDialogue(
                getString(R.string.error), getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {}
        }
    }

    private fun createMotherNeonateRequest(prescriptionEncounterId: String?) {
        viewModel.motherNeonateAncRequest.apply {
            id = prescriptionEncounterId
            assessmentType =  MedicalReviewTypeEnums.ANC_REVIEW.name
            presentingComplaints =
                presentingComplaintsViewModel.selectedPresentingComplaints.map { it.value }
            presentingComplaintsNotes = presentingComplaintsViewModel.enteredComplaintNotes
            obstetricExaminations =
                systemicExaminationViewModel.selectedSystemicExaminations.map { it.value }
            obstetricExaminationNotes = systemicExaminationViewModel.enteredExaminationNotes
            fundalHeight = systemicExaminationViewModel.fundalHeight
            fetalHeartRate = systemicExaminationViewModel.fetalHeartRate
            clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes
            pregnancyDetails = pregnancyDetailsViewModel.pregnancyDetailsModel.apply {
                if (viewModel.ancVisit > 1) {
                    weight = motherNeonateBpWeightViewModel.getWeight()
                    diastolic = motherNeonateBpWeightViewModel.getBp()?.diastolic
                    systolic = motherNeonateBpWeightViewModel.getBp()?.systolic
                    pulse = motherNeonateBpWeightViewModel.getBp()?.pulse
                }
            }
            deliveryKit = pregnancyPastObstetricHistoryViewModel.deliveryKit
            pregnancyHistory = pregnancyPastObstetricHistoryViewModel.pregnancyHistoryChip
                .map { it.value }
            pregnancyHistoryNotes = pregnancyPastObstetricHistoryViewModel.pregnancyHistoryNotes
            patientReference = patientViewModel.getPatientFHIRId()
        }
    }

    private fun validatePregnantDetails() {
        val pregnancyDetailsFragment =
            supportFragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner) as? PregnancyDetailsFragment
        if (pregnancyDetailsFragment?.validateInput() == true) {
            binding.loadingProgress.visible()
            if ((pregnancyDetailsViewModel.pregnancyDetailsModel.systolic == null &&
                        pregnancyDetailsViewModel.pregnancyDetailsModel.diastolic == null) && pregnancyDetailsViewModel.pregnancyDetailsModel.weight == null
            ) {
                handleSubmit()
            } else {
                handlePregnancyDetails(
                    pregnancyDetailsViewModel,
                    weightViewModel,
                    bpViewModel,
                    viewModel
                )
            }
        }
    }
    private fun createEncounter(viewModel: MotherNeonateANCViewModel): MedicalReviewEncounter {
        return MedicalReviewEncounter(
            provenance = ProvanceDto(),
            latitude = viewModel.lastLocation?.latitude,
            longitude = viewModel.lastLocation?.longitude,
            patientId = viewModel.patientId,
            startTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ),
            endTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        )
    }

    private fun saveWeightIfNeeded(
        pregnancyDetailsViewModel: PregnancyDetailsViewModel,
        weightViewModel: AddWeightViewModel,
        viewModel: MotherNeonateANCViewModel
    ) {
        pregnancyDetailsViewModel.pregnancyDetailsModel.weight?.let {
            weightViewModel.saveWeight(
                BpAndWeightRequestModel(
                    weight = it,
                    encounter = createEncounter(viewModel)
                )
            )
        }
    }

    private fun saveBloodPressureIfNeeded(
        pregnancyDetailsViewModel: PregnancyDetailsViewModel,
        bpViewModel: AddBpViewModel,
        viewModel: MotherNeonateANCViewModel
    ) {
        if ((pregnancyDetailsViewModel.pregnancyDetailsModel.systolic != null &&
            pregnancyDetailsViewModel.pregnancyDetailsModel.diastolic != null) ||
            pregnancyDetailsViewModel.pregnancyDetailsModel.pulse != null
        ) {
            bpViewModel.saveBloodPressure(
                BpAndWeightRequestModel(
                    diastolic = pregnancyDetailsViewModel.pregnancyDetailsModel.diastolic,
                    systolic = pregnancyDetailsViewModel.pregnancyDetailsModel.systolic,
                    pulse = pregnancyDetailsViewModel.pregnancyDetailsModel.pulse,
                    encounter = createEncounter(viewModel)
                )
            )
        }
    }

    private fun handlePregnancyDetails(
        pregnancyDetailsViewModel: PregnancyDetailsViewModel,
        weightViewModel: AddWeightViewModel,
        bpViewModel: AddBpViewModel,
        viewModel: MotherNeonateANCViewModel
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            saveWeightIfNeeded(pregnancyDetailsViewModel, weightViewModel, viewModel)
        } else {
            showErrorDialogue(
                getString(R.string.error), getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
                binding.loadingProgress.gone()
            }
        }
    }


    private fun backNavigation() {
        val fragmentManager = supportFragmentManager
        val pregnancyDetailsFragment =
            fragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner)
        if (viewModel.ancVisit == 1 && pregnancyDetailsFragment is PregnancyDetailsFragment) {
            // Show the dialog here
            showErrorDialog()
            viewModel.setAnalyticsData(
                UserDetail.startDateTime,
                eventType = AnalyticsDefinedParams.MotherNeonateAnc,
                exitReason = AnalyticsDefinedParams.BackButtonClicked,
                eventName = UserDetail.eventName,
                isCompleted = false
            )
        } else if (viewModel.ancVisit == 1 && pregnancyDetailsFragment is MedicalReviewPatientDiagnosisFragment) {
            binding.loadingProgress.visible()
            initializePregnancyDetailsFragment()
            initializePregnancyHistoryFragment()
            hideContainers()
            setButtonWidth()
            binding.loadingProgress.gone()
        } else if (pregnancyDetailsFragment is MotherNeonateAncSummary) {
            showErrorDialog()
        } else if (viewModel.ancVisit > 1 && pregnancyDetailsFragment is MedicalReviewPatientDiagnosisFragment) {
            showErrorDialog()
            viewModel.setAnalyticsData(
                UserDetail.startDateTime,
                eventType = AnalyticsDefinedParams.MotherNeonateAnc,
                exitReason = AnalyticsDefinedParams.BackButtonClicked,
                eventName = UserDetail.eventName,
                isCompleted = false
            )
        }
    }


    private fun showErrorDialog() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                onBackPressPopStack()
            }
        }
    }

    private fun onBackPressPopStack() {
        this@MotherNeonateANCActivity.finish()
    }

    private fun initializeFragments() {
        with(binding) {
            presentingComplaintsContainer.visible()
            systemicExaminationsContainer.visible()
            clinicalNotesContainer.visible()
        }
        val bundle = Bundle().apply {
            putString(
                MedicalReviewTypeEnums.PresentingComplaints.name,
                MedicalReviewTypeEnums.ANC_REVIEW.name
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewTypeEnums.ANC_REVIEW.name
            )
        }
        replaceFragmentOrCreateNewFragment<PresentingComplaintsFragment>(
            binding.presentingComplaintsContainer.id,
            bundle = bundle,
            tag = PresentingComplaintsFragment.TAG
        )
        replaceFragmentOrCreateNewFragment<SystemicExaminationsFragment>(
            binding.systemicExaminationsContainer.id,
            bundle = bundle,
            tag = SystemicExaminationsFragment.TAG
        )
        replaceFragmentOrCreateNewFragment<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id,
            bundle = bundle,
            tag = ClinicalNotesFragment.TAG
        )
    }


    private fun handleSubmit(isAncVisitOne: Boolean = true) {
        replaceWithDiagnosisFragment()
        if (isAncVisitOne) {
            replaceWithPregnancySummaryFragment()
        } else {
            replaceWithMotherNeonateAncHistoryFragment()
        }
        scrollToTop()
        hideNextButton()
        showBottomNavigation()
        initializeFragments()
        attachObserversListenerForChip()
        binding.loadingProgress.gone()
    }

    private fun attachObserversListenerForChip() {
        supportFragmentManager
            .setFragmentResultListener(MedicalReviewDefinedParams.PC_ITEM, this) { _, _ ->
                enableSubmitBtn()
            }
        supportFragmentManager
            .setFragmentResultListener(MedicalReviewDefinedParams.SE_ITEM, this) { _, _ ->
                enableSubmitBtn()
            }
        supportFragmentManager
            .setFragmentResultListener(MedicalReviewDefinedParams.CLINICAL_NOTES, this) { _, _ ->
                enableSubmitBtn()
            }
    }

    private fun enableSubmitBtn() {
        binding.btnSubmit.isEnabled = clinicalNotesViewModel.enteredClinicalNotes.isNotBlank() ||
                (presentingComplaintsViewModel.selectedPresentingComplaints.isNotEmpty() || systemicExaminationViewModel.selectedSystemicExaminations.isNotEmpty() || presentingComplaintsViewModel.enteredComplaintNotes.isNotBlank() || systemicExaminationViewModel.enteredExaminationNotes.isNotBlank() || systemicExaminationViewModel.fundalHeight != null || systemicExaminationViewModel.fetalHeartRate != null)
    }

    private fun replaceWithDiagnosisFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.pregnancyDetailsConatiner,
                MedicalReviewPatientDiagnosisFragment.newInstance(
                    true,
                    false,
                    intent.getStringExtra(DefinedParams.PatientId),
                    viewModel.memberId,
                    intent.getStringExtra(DefinedParams.ID)
                )
            )
            .commit()
    }

    private fun replaceWithPregnancySummaryFragment() {
        val fragment = PregnancySummaryFragment.newInstance()
        fragment.setData(
            pregnancyPastObstetricHistoryViewModel.pregnancyHistoryChip,
            pregnancyPastObstetricHistoryViewModel.pregnancyHistoryNotes,
            pregnancyDetailsViewModel.pregnancyDetailsModel
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.pregnancyHistoryConatiner, fragment)
            .commit()
    }

    private fun scrollToTop() {
        binding.nestedScrollViewID.smoothScrollTo(0, 0)
    }

    private fun hideNextButton() {
        binding.btnLayout.btnNext.gone()
    }

    private fun showBottomNavigation() {
        binding.bottomNavigationView.visible()
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        binding.loadingProgress.visible()
        viewModel.ancVisit =
            details.pregnancyDetails?.ancVisitMedicalReview?.takeIf { true }?.plus(1) ?: 1
        viewModel.memberId = details.memberId

        val patientDetails = supportFragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner)

        if (patientDetails is MotherNeonateAncSummary || viewModel.ancVisit == 1) {
            when (patientDetails) {
                is PregnancyDetailsFragment -> binding.loadingProgress.gone()
                is MotherNeonateAncSummary -> {
                    patientViewModel.isSummary = true
                    val motherNeonateSummary =
                        supportFragmentManager.findFragmentById(R.id.pregnancyDetailsConatiner) as? MotherNeonateAncSummary
                    motherNeonateSummary?.setIds(viewModel.getSubmitCreateId(), details.id)
                    binding.loadingProgress.gone()
                }
                is MedicalReviewPatientDiagnosisFragment -> handleSubmit()
                else -> {
                    initializePregnancyDetailsFragment()
                    initializePregnancyHistoryFragment()
                    hideContainers()
                    setButtonWidth()
                    binding.loadingProgress.gone()
                }
            }
        } else if (viewModel.ancVisit > 1) {
            handleSubmit(false)
        }
    }

    private fun handleSummary() {
        with(binding) {
            bottomNavigationView.gone()
            referalBottomView.visible()
            btnLayout.btnNext.gone()
        }
        replaceMotherNeonateSummary()
        scrollToTop()
        binding.loadingProgress.gone()
    }

    private fun replaceMotherNeonateSummary() {
        patientViewModel.isSummary = true
        swipeRefresh()
        supportFragmentManager.beginTransaction()
            .replace(R.id.pregnancyDetailsConatiner, MotherNeonateAncSummary.newInstance())
            .commit()
        with(supportFragmentManager) {
            findFragmentById(R.id.pregnancyHistoryConatiner)?.let {
                beginTransaction().remove(it).commit()
            }
            findFragmentById(R.id.presentingComplaintsContainer)?.let {
                beginTransaction().remove(it).commit()
            }
            findFragmentById(R.id.systemicExaminationsContainer)?.let {
                beginTransaction().remove(it).commit()
            }
            findFragmentById(R.id.clinicalNotesContainer)?.let {
                beginTransaction().remove(it).commit()
            }
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    private fun replaceWithMotherNeonateAncHistoryFragment() {
        patientViewModel.getPatientId()?.let {
            val fragment = MotherNeonateAncHistoryFragment.newInstance(it,patientViewModel.getPatientFHIRId())
            supportFragmentManager.beginTransaction()
                .replace(R.id.pregnancyHistoryConatiner, fragment)
                .commit()
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