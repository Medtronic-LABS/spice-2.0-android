package org.medtroniclabs.uhis.ui.medicalreview.tb.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.data.model.ChipResponse
import org.medtroniclabs.uhis.data.model.MedicalReviewEncounter
import org.medtroniclabs.uhis.data.model.TbMedicalReviewCreateRequest
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.ActivityTbMedicalReviewBinding
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
import org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.medicalreview.prescription.PrescriptionActivity
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.ComorbiditiesFragment
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.PresumptiveTreatmentAndHistoryFragment
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.TbSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.ComorbiditiesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.TbSummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.TbViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.respiratory
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.ReferPatientFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

@AndroidEntryPoint
class TBMedicalReviewActivity :
    BaseActivity(),
    View.OnClickListener,
    AncVisitCallBack,
    OnDialogDismissListener {
    private lateinit var binding: ActivityTbMedicalReviewBinding
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val viewModel: TbViewModel by viewModels()
    private val clinicalNotesViewModel: ClinicalNotesViewModel by viewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by viewModels()
    private val systemicExaminationViewModel: SystemicExaminationViewModel by viewModels()
    private val comorbiditiesViewModel: ComorbiditiesViewModel by viewModels()
    private val summaryViewModel: TbSummaryViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityTbMedicalReviewBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                showErrorDialog()
            },
            callbackHome = {
                showErrorDialog(true)
            },
        )
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        initView()
        setButtonClickListener()
        attachObserver()
        patientViewModel.setUserJourney(AnalyticsDefinedParams.TBMEDICALREVIEW)
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            viewModel.lastLocation = it
        }
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
        viewModel.tbMetaResponse.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    initView()
                }

                ResourceState.ERROR -> {
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
        viewModel.tbCreateResponse.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        showSummary {
                            swipeRefresh()
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    private fun showSummary(callBack: () -> Unit) {
        with(binding) {
            patientViewModel.setUserJourney(AnalyticsDefinedParams.TBMEDICALREVIEWSUMMARY)
            patientSummaryContainer.gone()
            comorbiditiesContainer.gone()
            presentingComplaintsContainer.gone()
            comorbiditiesContainer.gone()
            systemicExaminationsContainer.gone()
            clinicalNotesContainer.gone()
            patientMedicalReviewDiagnosis.gone()
            clinicalNotesContainer.visible()
            binding.bottomNavigationView.gone()
            binding.referralBottomView.visible()
            binding.btnDone.isEnabled = true
            patientViewModel.isSummary = true
            replaceFragment(
                R.id.clinicalNotesContainer,
                TbSummaryFragment.TAG,
                TbSummaryFragment.newInstance(
                    encounterId = viewModel.tbCreateResponse.value
                        ?.data
                        ?.encounterId,
                    fhirId = patientViewModel.getPatientFHIRId(),
                ),
            )
        }
        callBack.invoke()
    }

    private fun onBackPressPopStack() {
        this@TBMedicalReviewActivity.finish()
    }

    private fun initStaticDataCall() {
        if (!SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_TB_LOADED.name)) {
            viewModel.getTbStaticData()
        } else {
            initView()
        }
    }

    private fun initView() {
        initializePatientDetailFragment()
    }

    private fun initializePatientDetailFragment() {
        replaceFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            PatientInfoFragment
                .newInstance(
                    intent.getStringExtra(DefinedParams.PatientId),
                    isTb = true,
                ).apply {
                    setDataCallback(this@TBMedicalReviewActivity)
                },
        )
    }

    private fun swipeRefresh() {
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

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showErrorDialog()
            }
        }

    private fun showErrorDialog(isHome: Boolean = false) {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                if (isHome) {
                    startActivityWithoutSplashScreen()
                } else {
                    onBackPressPopStack()
                }
            }
        }
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

    private fun createSummary() {
        patientViewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
        val fragment = supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer)
        if (fragment is TbSummaryFragment) {
            val isValid = (fragment as? TbSummaryFragment)?.validateInput()
            if (isValid == true) {
                val submitCreateId = viewModel.getSubmitCreateId()
                val nextVisitDate = DateUtils.convertDateTimeToDate(
                    summaryViewModel.nextFollowupDate,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true,
                )
                if (connectivityManager.isNetworkAvailable()) {
                    viewModel.createTbSummary(
                        MedicalReviewTypeEnums.TB.name,
                        patientViewModel.getPatientMemberId(),
                        submitCreateId,
                        patientViewModel.getPatientHouseholdId(),
                        viewModel.getPatientReference(),
                        nextVisitDate,
                        summaryViewModel.patientStatus,
                        patientViewModel.getVillageId(),
                        patientViewModel.getPatientId(),
                        MedicalReviewTypeEnums.TB.name,
                        summaryViewModel.treatmentOutCome,
                        patientViewModel.getTbMedicalReviewStatus(),
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
        val request = createMedicalReviewRequest()
        withNetworkAvailability(online = {
            patientViewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
            viewModel.tbCreate(request)
        })
    }

    private fun showReferPatientDialog() {
        patientViewModel.setUserJourney(AnalyticsDefinedParams.REFERBUTTONTRIGGERED)
        withNetworkAvailability(online = {
            viewModel.tbCreateResponse.value?.data?.let {
                showDialogIfNotPresent(
                    ReferPatientFragment.TAG,
                ) {
                    ReferPatientFragment.newInstance(
                        MedicalReviewTypeEnums.TB.name,
                        it.patientReference,
                        it.encounterId,
                    )
                }
            }
        })
    }

    /**
     * Creates a `TbMedicalReviewCreateRequest` with safe handling.
     */
    private fun createMedicalReviewRequest(): TbMedicalReviewCreateRequest {
        // TODO value should null in selectedSystemicExaminations
        return TbMedicalReviewCreateRequest(
            presentingComplaints = presentingComplaintsViewModel.selectedPresentingComplaints.map { it.value },
            presentingComplaintsNotes = presentingComplaintsViewModel.enteredComplaintNotes,
            systemicExaminations = systemicExaminationViewModel.selectedSystemicExaminations.map {
                ChipResponse(
                    name = it.value,
                    value = if (it.value == respiratory) systemicExaminationViewModel.respiratoryNotes else "",
                )
            },
            clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes,
            comorbidities = comorbiditiesViewModel.chips.map { it.value },
            comorbiditiesNotes = comorbiditiesViewModel.comments.ifBlank { null },
            encounter = createMedicalReviewEncounter(
                encounterId = patientViewModel.encounterId,
                patientHouseholdId = patientViewModel.getPatientHouseholdId(),
                memberId = patientViewModel.getPatientMemberId(),
            ),
            presumptiveTbNo = patientViewModel.presumptiveTbNo,
            id = patientViewModel.encounterId,
        )
    }

    private fun createMedicalReviewEncounter(
        encounterId: String?,
        patientHouseholdId: String?,
        memberId: String?,
    ): MedicalReviewEncounter {
        val currentTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        return MedicalReviewEncounter(
            id = encounterId,
            patientId = viewModel.patientId,
            provenance = ProvanceDto(),
            memberId = memberId,
            latitude = viewModel.lastLocation?.latitude ?: 0.0,
            longitude = viewModel.lastLocation?.longitude ?: 0.0,
            startTime = currentTime,
            endTime = currentTime,
            householdId = patientHouseholdId,
            referred = true,
        )
    }

    private fun openPrescriptionActivity() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
            val intent = Intent(this, PrescriptionActivity::class.java)
            intent.putExtra(DefinedParams.PatientId, data.patientId)
            intent.putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
            getResult.launch(intent)
        }
    }

    fun openInvestigationActivity() {
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

    override fun onDataLoaded(details: PatientListRespModel) {
        viewModel.memberId = details.memberId
        (supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? TbSummaryFragment)
            ?.let {
                showSummary {
                    // Do nothing
                }
            } ?: replaceWithDiagnosisFragment()
    }

    private fun setButtonClickListener() {
        binding.btnSubmit.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
    }

    private fun replaceWithDiagnosisFragment() {
        replaceFragment(
            R.id.patientSummaryContainer,
            MedicalReviewPatientDiagnosisFragment.TAG,
            MedicalReviewPatientDiagnosisFragment.newInstance(
                isAnc = false,
                isPnc = false,
                isTB = true,
                patientId = intent.getStringExtra(DefinedParams.PatientId),
                memberID = viewModel.memberId,
                id = intent.getStringExtra(DefinedParams.ID),
            ),
        )
        addOrReuseFragment(
            R.id.comorbiditiesContainer,
            ComorbiditiesFragment.TAG,
            ComorbiditiesFragment.newInstance(DefinedParams.TB),
        )
        replaceFragment(
            R.id.patientMedicalReviewDiagnosis,
            PresumptiveTreatmentAndHistoryFragment.TAG,
            PresumptiveTreatmentAndHistoryFragment.newInstance(),
        )
        initializeFragments()
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
                MedicalReviewTypeEnums.TB.name,
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewTypeEnums.TB.name,
            )
            putString(
                MedicalReviewTypeEnums.ClinicalNotes.name,
                MedicalReviewTypeEnums.TB.name,
            )
        }
        replaceFragmentOrCreateNewFragment<PresentingComplaintsFragment>(
            binding.presentingComplaintsContainer.id,
            bundle = bundle,
            tag = PresentingComplaintsFragment.TAG,
        )
        replaceFragmentOrCreateNewFragment<SystemicExaminationsFragment>(
            binding.systemicExaminationsContainer.id,
            bundle = bundle,
            tag = SystemicExaminationsFragment.TAG,
        )
        replaceFragmentOrCreateNewFragment<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id,
            bundle = bundle,
            tag = ClinicalNotesFragment.TAG,
        )
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
        supportFragmentManager
            .setFragmentResultListener(MedicalReviewDefinedParams.CF_ITEM, this) { _, _ ->
                enableSubmitBtn()
            }
    }

    private fun enableSubmitBtn() {
        val comorbiditiesFragment =
            supportFragmentManager.findFragmentByTag(ComorbiditiesFragment.TAG) as? ComorbiditiesFragment
        val isValidFragment = comorbiditiesFragment?.validateInput(true)?.first ?: false

        val hasClinicalNotes = clinicalNotesViewModel.enteredClinicalNotes.isNotBlank()
        val hasSelectedPresentingComplaints =
            presentingComplaintsViewModel.selectedPresentingComplaints.isNotEmpty()
        val hasEnteredComplaintNotes =
            presentingComplaintsViewModel.enteredComplaintNotes.isNotBlank()
        val hasEnteredExaminationNotes =
            systemicExaminationViewModel.enteredExaminationNotes.isNotBlank()

        val hasSelectedSystemicExaminations =
            systemicExaminationViewModel.selectedSystemicExaminations.isNotEmpty()
        val isRespiratorySelected =
            systemicExaminationViewModel.selectedSystemicExaminations.any { it.value == respiratory }
        val hasValidRespiratoryNotes =
            !systemicExaminationViewModel.respiratoryNotes.isNullOrBlank()
        val hasComorbidities = comorbiditiesViewModel.chips.isNotEmpty()
        // If "Ear" is selected, respiratoryNotes must not be blank
        val isSystemicExaminationValid = !isRespiratorySelected || hasValidRespiratoryNotes

        binding.btnSubmit.isEnabled = (
            hasSelectedPresentingComplaints ||
                hasEnteredComplaintNotes ||
                hasEnteredExaminationNotes ||
                hasSelectedPresentingComplaints ||
                isValidFragment
        )

        if (hasSelectedSystemicExaminations) {
            binding.btnSubmit.isEnabled = isSystemicExaminationValid
            if (!isSystemicExaminationValid) {
                return
            }
        }
        if (hasComorbidities) {
            binding.btnSubmit.isEnabled = isValidFragment
            if (!isValidFragment) {
                return
            }
        }

        binding.btnSubmit.isEnabled = hasClinicalNotes
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
        swipeRefresh()
    }

    fun enableRefer(isEnable: Boolean) {
        binding.btnRefer.isEnabled = isEnable
    }
}
