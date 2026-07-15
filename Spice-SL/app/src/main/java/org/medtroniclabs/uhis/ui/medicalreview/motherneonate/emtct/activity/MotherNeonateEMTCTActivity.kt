package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.emtct.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.takeIfNotNull
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.data.model.HivRequestData
import org.medtroniclabs.uhis.data.model.MedicalReviewEncounter
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.ActivityMotherNeonateEmtctctivityBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dialog.MedicalReviewSuccessDialogFragment
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.medicalreview.ClinicalNotesFragment
import org.medtroniclabs.uhis.ui.medicalreview.PresentingComplaintsFragment
import org.medtroniclabs.uhis.ui.medicalreview.SystemicExaminationsFragment
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.PresentingComplaintsViewModel
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.SystemicExaminationViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.ARTRegimenFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HIVStatusFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HivGeneralAndSystemicExaminationFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HivImrCmrSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HivMedicalReviewDiagnosesFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.RecommendedInvestigationsDialog
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.ViralLoadFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivGeneralAndSystemicExaminationViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivImrAndCmrViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivImrCmrSummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.WhoClinicalStageViewModel
import org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment.PregnancyDetailsFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment.PregnancySummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.prescription.PrescriptionActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CLINICAL_NOTES
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.isViralLoadTestRecommended
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.ReferPatientFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.MotherNeonateBpWeightViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PregnancyDetailsViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

@AndroidEntryPoint
class MotherNeonateEMTCTActivity :
    BaseActivity(),
    AncVisitCallBack,
    View.OnClickListener,
    OnDialogDismissListener {
    companion object {
        private const val TAG = "MotherNeonateEMTCTActivity"
    }

    private val binding: ActivityMotherNeonateEmtctctivityBinding by lazy {
        ActivityMotherNeonateEmtctctivityBinding.inflate(layoutInflater)
    }

    // ViewModels
    private val hivViewModel: HivViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by viewModels()
    private val weightViewModel: MotherNeonateBpWeightViewModel by viewModels()
    private val hivGeneralAndSystemicExaminationViewModel: HivGeneralAndSystemicExaminationViewModel by viewModels()
    private val clinicalNotesViewModel: ClinicalNotesViewModel by viewModels()
    private val viewModel: HivImrAndCmrViewModel by viewModels()
    private val summaryViewModel: HivImrCmrSummaryViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()
    private val systemicExaminationViewModel: SystemicExaminationViewModel by viewModels()
    private val whoViewModel: WhoClinicalStageViewModel by viewModels()
    private val pregnancyDetailsViewModel: PregnancyDetailsViewModel by viewModels()

    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleActivityResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupView()
        initStaticDataCall()
        attachObserver()
        setupSwipeRefresh()
        setButtonClickListener()
        scrollUp()
    }

    private fun handleActivityResult(data: Intent?) {
        data?.getStringExtra(DefinedParams.ENCOUNTER_ID)?.let { valueString ->
            patientViewModel.encounterId = valueString
        }
        when {
            data?.getBooleanExtra(DefinedParams.Investigation, false) == true -> {
                refreshViralLoadFragment()
            }
            data?.getBooleanExtra(DefinedParams.PRESCRIPTION, false) == true -> {
                refreshARTRegimenFragment()
            }
        }
    }

    private fun refreshViralLoadFragment() {
        (supportFragmentManager.findFragmentById(R.id.viralLoadResultContainer) as? ViralLoadFragment)?.apply {
            refreshFragment(
                intent.getStringExtra(DefinedParams.ID),
                intent.getStringExtra(DefinedParams.MEMBER_ID),
            )
        }
    }

    private fun refreshARTRegimenFragment() {
        (supportFragmentManager.findFragmentById(R.id.aRTRegimenResultContainer) as? ARTRegimenFragment)?.apply {
            refreshFragment(
                intent.getStringExtra(DefinedParams.PatientId),
                intent.getStringExtra(DefinedParams.ID),
            )
        }
    }

    private fun setupView() {
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callback = { backNavigation() },
            callbackHome = { backNavigationToHome() },
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backNavigation()
                }
            },
        )

        whoViewModel.setWhoStage(MedicalReviewTypeEnums.whoClinicalStage.name)
        hivViewModel.getHivEmtctVistStatusByCategory(MedicalReviewTypeEnums.emtct_visit_status.name)
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                hivViewModel.isHivSummary = false
                finishWithResultOk()
            }
        }
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                hivViewModel.isHivSummary = false
                startActivityWithoutSplashScreen()
            }
        }
    }

    private fun finishWithResultOk() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun initStaticDataCall() {
        if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_HIV_DATA_LOADED.name))) {
            if (connectivityManager.isNetworkAvailable()) {
                hivViewModel.getHivMetaData()
            } else {
                showErrorDialogue(
                    getString(R.string.error),
                    getString(R.string.no_internet_error),
                    isNegativeButtonNeed = false,
                ) {}
            }
        } else {
            initializePatientDetailFragment()
        }
        hivViewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
    }

    private fun initializePatientDetailFragment() {
        replaceFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            PatientInfoFragment
                .newInstanceForEMTCT(
                    intent.getStringExtra(DefinedParams.PatientId),
                    isEMTCTMR = true,
                    isEMTCTSummary = hivViewModel.isHivSummary,
                ).apply {
                    setDataCallback(this@MotherNeonateEMTCTActivity)
                },
        )
    }

    private fun attachObserver() {
        viewModel.checkRecommendationRInvestigations.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let {
                        if (it[isViralLoadTestRecommended] == true) {
                            showDialogIfNotPresent(
                                RecommendedInvestigationsDialog.TAG,
                            ) {
                                RecommendedInvestigationsDialog.newInstance().apply {
                                    onOkayClickListener = {
                                        openInvestigationActivity()
                                    }
                                    onCancelClickListener = {
                                        submitWithPregnancyDetails()
                                    }
                                }
                            }
                        } else {
                            submitWithPregnancyDetails()
                        }
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
                    showDialogIfNotPresent(
                        MedicalReviewSuccessDialogFragment.TAG,
                    ) {
                        MedicalReviewSuccessDialogFragment.newInstance()
                    }
                }
            }
        }
        hivViewModel.hivMetaResponseLiveData.observe(this) { resourceState ->
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
                            // onBackPressPopStack()
                        }
                    }
                }

                ResourceState.SUCCESS -> {
                    initializePatientDetailFragment()
                    hideLoading()
                }
            }
        }
        viewModel.hivCreateResponse.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        showSummary {
                            setupSwipeRefresh()
                        }
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
                    showDialogIfNotPresent(
                        MedicalReviewSuccessDialogFragment.TAG,
                    ) {
                        MedicalReviewSuccessDialogFragment.newInstance()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
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
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        hivViewModel.memberId = details.memberId

        if (hivViewModel.isHivSummary) {
        } else {
            hivViewModel.ancVisit =
                details.pregnancyDetails
                    ?.ancVisitMedicalReview
                    ?.takeIf { true }
                    ?.plus(1) ?: 1
            initializeReviewFragments()
        }
    }

    private fun initializeReviewFragments() {
        setupResultListeners()
        binding.patientDetailFragment.visible()
        binding.patientBMIContainer.visible()

        replaceFragment(
            R.id.patientBMIContainer,
            HivMedicalReviewDiagnosesFragment.TAG,
            HivMedicalReviewDiagnosesFragment.newInstance(true, true),
        )

        initEmtctFragments()
    }

    private fun setupSwipeRefresh() {
        binding.refreshLayout.setOnRefreshListener {
            getPatientDetails()
        }
    }

    private fun getPatientDetails() {
        withNetworkAvailability(online = {
            supportFragmentManager
                .findFragmentById(R.id.patientDetailFragment)
                .let {
                    patientViewModel.getPatientId()?.let { id ->
                        patientViewModel.getPatients(id)
                    }
                }
        }, offline = {
            if (binding.refreshLayout.isRefreshing) {
                binding.refreshLayout.isRefreshing = false
            }
        })
    }

    private fun enableSubmitButton() {
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? ClinicalNotesFragment
        val isClinicalNotesValid = clinicalNotesFragment?.validateInput()
        val pregnancyDetailsFragment =
            supportFragmentManager.findFragmentById(R.id.pregnancySummaryContainer) as? Any

        if (pregnancyDetailsFragment is PregnancyDetailsFragment) {
            binding.btnSubmit.isEnabled = isClinicalNotesValid == true
        } else {
            binding.btnSubmit.isEnabled = isClinicalNotesValid == true
        }
    }

    private fun setupResultListeners() {
        val listener: (String, Bundle) -> Unit = { _, _ -> enableSubmitButton() }
        supportFragmentManager.setFragmentResultListener(CLINICAL_NOTES, this, listener)
    }

    private fun initEmtctFragments() {
        if (patientViewModel.getPatientLmb() != null) {
            addOrReuseFragment(
                R.id.pregnancySummaryContainer,
                PregnancySummaryFragment.TAG,
                PregnancySummaryFragment.newInstanceEmtct(isEmtct = true, patientReference = intent.getStringExtra(DefinedParams.ID)),
            )
        } else {
            addOrReuseFragment(
                R.id.pregnancySummaryContainer,
                PregnancyDetailsFragment.TAG,
                PregnancyDetailsFragment.newInstance(patientViewModel.getPatientLmb()),
            )
        }

        val hivBundle = Bundle().apply {
            putString(
                MedicalReviewTypeEnums.PresentingComplaints.name,
                MedicalReviewTypeEnums.HIV.name,
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewTypeEnums.HIV.name,
            )
        }

        replaceFragmentOrCreateNewFragment<PresentingComplaintsFragment>(
            R.id.presentingComplaintsContainer,
            bundle = hivBundle,
            tag = PresentingComplaintsFragment.TAG,
        )

        replaceFragmentOrCreateNewFragment<SystemicExaminationsFragment>(
            R.id.obstetricExaminationContainer,
            bundle = hivBundle,
            tag = SystemicExaminationsFragment.TAG,
        )

        replaceFragmentOrCreateNewFragment<HivGeneralAndSystemicExaminationFragment>(
            R.id.systemicExaminationsContainer,
            bundle = hivBundle,
            tag = HivGeneralAndSystemicExaminationFragment.TAG,
        )

        var bundle = Bundle().apply {
            putBoolean(DefinedParams.EMTCTMR, true)
        }
        addOrReuseFragment(
            R.id.emtctStatusContainer,
            HIVStatusFragment.TAG,
            HIVStatusFragment.newInstance(),
            bundle,
        )

        createNewFragmentOnly<ARTRegimenFragment>(
            R.id.aRTRegimenResultContainer,
            bundle = Bundle().apply {
                putString(DefinedParams.PatientId, intent.getStringExtra(DefinedParams.PatientId))
                putString(DefinedParams.ID, intent.getStringExtra(DefinedParams.ID))
                putString(DefinedParams.MEMBER_ID, patientViewModel.getPatientMemberId())
            },
            tag = ARTRegimenFragment.TAG,
        )
        replaceFragmentOrCreateNewFragment<ViralLoadFragment>(
            R.id.viralLoadResultContainer,
            bundle = Bundle().apply {
                putBoolean(DefinedParams.VIRAL_LOAD, true)
                putString(DefinedParams.PatientReference, intent.getStringExtra(DefinedParams.ID))
                putString(
                    DefinedParams.MemberReference,
                    intent.getStringExtra(DefinedParams.MEMBER_ID),
                )
            },
            tag = ViralLoadFragment.TAG,
        )
        replaceFragmentOrCreateNewFragment<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id,
            bundle = null,
            tag = ClinicalNotesFragment.TAG,
        )
    }

    private fun setButtonClickListener() {
        binding.btnSubmit.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivPrescription.id -> withNetworkAvailability(online = {
                openPrescriptionActivity()
            })

            binding.ivInvestigation.id -> withNetworkAvailability(online = {
                openInvestigationActivity()
            })

            binding.loadingProgress.id -> {}
            binding.btnSubmit.id -> withLocationCheck(::clickSubmit)
            binding.btnDone.id -> withLocationCheck(::createSummary)
            binding.btnRefer.id -> showReferPatientDialog()
        }
    }

    private fun showReferPatientDialog() {
        withNetworkAvailability(online = {
            viewModel.hivCreateResponse.value?.data?.let {
                showDialogIfNotPresent(
                    ReferPatientFragment.TAG,
                ) {
                    ReferPatientFragment.newInstance(
                        MedicalReviewTypeEnums.HIV.name,
                        it.patientReference,
                        it.encounterId,
                    )
                }
            }
        })
    }

    private fun createSummary() {
        val fragment = supportFragmentManager.findFragmentById(R.id.hivSummary)
        if (fragment is HivImrCmrSummaryFragment) {
            val isValid = (fragment as? HivImrCmrSummaryFragment)?.validateInput()
            if (isValid == true) {
                val submitCreateId = viewModel.getSubmitCreateId()
                val nextVisitDate = DateUtils.convertDateTimeToDate(
                    summaryViewModel.nextFollowupDate,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true,
                )
                if (connectivityManager.isNetworkAvailable()) {
                    viewModel.createHivSummary(
                        MedicalReviewTypeEnums.HIV.name,
                        patientViewModel.getPatientMemberId(),
                        submitCreateId,
                        patientViewModel.getPatientHouseholdId(),
                        viewModel.getPatientReference(),
                        nextVisitDate,
                        summaryViewModel.patientStatus,
                        patientViewModel.getVillageId(),
                        patientViewModel.getPatientId(),
                        DefinedParams.EMTCT_HIV_MEDICAL_REVIEW,
                        summaryViewModel.eMTCTStatus,
                        summaryViewModel.maternalOutcome,
                    )
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

    private fun clickSubmit() {
        val pregnancyDetailsFragment =
            supportFragmentManager.findFragmentById(R.id.pregnancySummaryContainer) as? PregnancyDetailsFragment

        val systemicExaminationFragment =
            supportFragmentManager.findFragmentById(R.id.obstetricExaminationContainer) as? SystemicExaminationsFragment

        if (!patientViewModel.getPatientLmb().isNullOrEmpty() && systemicExaminationFragment?.validateInput() == true) {
            callViralLoadTestRecommendation()
        } else if (pregnancyDetailsFragment?.validateInput() == true && systemicExaminationFragment?.validateInput() == true) {
            callViralLoadTestRecommendation()
        }
    }

    private fun submitWithPregnancyDetails() {
        val pregnancyDetailsFragment =
            supportFragmentManager.findFragmentById(R.id.pregnancySummaryContainer) as? PregnancyDetailsFragment
        val systemicExaminationFragment =
            supportFragmentManager.findFragmentById(R.id.obstetricExaminationContainer) as? SystemicExaminationsFragment

        if (!patientViewModel.getPatientLmb().isNullOrEmpty() && systemicExaminationFragment?.validateInput() == true) {
            submitEmtctRequest()
        } else if (pregnancyDetailsFragment?.validateInput() == true && systemicExaminationFragment?.validateInput() == true) {
            submitEmtctRequest()
        }
    }

    private fun callViralLoadTestRecommendation() {
        viewModel.checkRecommendationRInvestigations(patientReference = patientViewModel.getPatientFHIRId(), memberId = patientViewModel.getPatientMemberId())
    }

    private fun submitEmtctRequest() {
        val request = createMedicalReviewRequest()
        withNetworkAvailability(online = {
            viewModel.hivCreate(request = request)
        })
    }

    private fun createMedicalReviewRequest(): HivRequestData {
        val pregnancyDetailsFragment =
            supportFragmentManager.findFragmentById(R.id.pregnancySummaryContainer) as? Any
        var height: Double? = null
        var weight: Double? = null
        if (pregnancyDetailsFragment is PregnancyDetailsFragment) {
            height = pregnancyDetailsViewModel.pregnancyDetailsModel.height
            weight = pregnancyDetailsViewModel.pregnancyDetailsModel.weight
        } else {
            height = weightViewModel.getHeights()
            weight = weightViewModel.getWeight()
        }
        return HivRequestData(
            clinicalStage = hivViewModel.whovalue,
            cd4 = hivViewModel.cd4Value,
            artCode = patientViewModel.artCode,
            weight = weight,
            height = height,
            hivStatus = (supportFragmentManager.findFragmentByTag(HIVStatusFragment.TAG) as? HIVStatusFragment)?.getRequest(),
            presentingComplaints = presentingComplaintsViewModel.selectedPresentingComplaints.map { it.value },
            presentingComplaintsNotes = presentingComplaintsViewModel.enteredComplaintNotes.takeIf { it.isNotBlank() },
            systemicExaminations = hivGeneralAndSystemicExaminationViewModel.resultHashMap,
            encounter = createMedicalReviewEncounter(
                encounterId = patientViewModel.encounterId,
                patientHouseholdId = patientViewModel.getPatientHouseholdId(),
                memberId = patientViewModel.getPatientMemberId(),
            ),
            medicalReviewType = DefinedParams.EMTCT_HIV_MEDICAL_REVIEW,
            clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes,
            id = patientViewModel.encounterId,
            emtctVisitStatus = hivViewModel.emtctVisitStatus,
            obstetricExaminations = systemicExaminationViewModel.selectedSystemicExaminations.map { it.value },
            obstetricExaminationNotes = systemicExaminationViewModel.enteredExaminationNotes,
            fundalHeight = systemicExaminationViewModel.fundalHeight.takeIfNotNull(),
            fetalHeartRate = systemicExaminationViewModel.fetalHeartRate.takeIfNotNull(),
            pregnancyDetails = if (pregnancyDetailsFragment is PregnancyDetailsFragment) pregnancyDetailsViewModel.pregnancyDetailsModel else null,
        )
    }

    private fun createMedicalReviewEncounter(
        encounterId: String?,
        patientHouseholdId: String?,
        memberId: String?,
    ): MedicalReviewEncounter =
        MedicalReviewEncounter(
            id = encounterId,
            patientId = hivViewModel.patientId,
            provenance = ProvanceDto(),
            memberId = memberId,
            latitude = hivViewModel.lastLocation?.latitude ?: 0.0,
            longitude = hivViewModel.lastLocation?.longitude ?: 0.0,
            startTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ),
            endTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ),
            householdId = patientHouseholdId,
            visitNumber = hivViewModel.ancVisit,
            referred = true,
        )

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

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
        getPatientDetails()
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            hivViewModel.lastLocation = it
        }
    }

    private fun showSummary(callBack: () -> Unit) {
        scrollUp()
        with(binding) {
            patientBMIContainer.gone()
            pregnancySummaryContainer.gone()
            presentingComplaintsContainer.gone()
            obstetricExaminationContainer.gone()
            systemicExaminationsContainer.gone()
            aRTRegimenResultContainer.gone()
            viralLoadResultContainer.gone()
            clinicalNotesContainer.gone()
            emtctStatusContainer.gone()
            binding.bottomNavigationView.gone()
            binding.referralBottomView.visible()
            binding.btnDone.isEnabled = true
            binding.btnRefer.isEnabled = true
            patientViewModel.isSummary = true
            hivViewModel.isHivSummary = true

            initializePatientDetailFragment()
            replaceFragment(
                R.id.hivSummary,
                HivImrCmrSummaryFragment.TAG,
                HivImrCmrSummaryFragment.newInstance(
                    encounterId = viewModel.hivCreateResponse.value
                        ?.data
                        ?.encounterId,
                    fhirId = patientViewModel.getPatientFHIRId(),
                    isEMTCTMR = true,
                ),
            )
        }
        callBack.invoke()
    }

    fun enableRefer(isEnable: Boolean) {
//        binding.btnRefer.isEnabled = isEnable
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    private fun onBackPressPopStack() {
        this.finish()
    }

    private fun scrollUp() {
        binding.nestedScrollViewID.post {
            binding.nestedScrollViewID.fullScroll(View.FOCUS_UP)
        }
    }

    private fun isPregnancyDetailsAndHistoryValidation(): Boolean {
        val model = pregnancyDetailsViewModel.pregnancyDetailsModel
        return model.height != null ||
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
}
