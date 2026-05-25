package org.medtroniclabs.uhis.ui.medicalreview.hiv.activity

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
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.HIV
import org.medtroniclabs.uhis.common.DefinedParams.HIV_MEDICAL_REVIEW
import org.medtroniclabs.uhis.common.DefinedParams.PatientId
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.data.MedicalReviewSummarySubmitRequest
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.ActivityHivMedicalReviewBaseBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dialog.MedicalReviewSuccessDialogFragment
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.medicalreview.ClinicalNotesFragment
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.EligibilityFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HivMedicalReviewDiagnosesFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HivSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.HivTestFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivViewModel
import org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.medicalreview.prescription.PrescriptionActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.A1_TEST_RESULT
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.A2_TEST_RESULT
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.A3_TEST_RESULT
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.CLINICAL_NOTES
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.HBsAg
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.HIV_SYPHILIS_DUO_TEST
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.HaveYouTakenHivTestBefore
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.ReferPatientFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.MotherNeonateBpWeightViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

@AndroidEntryPoint
class HivMedicalReviewBaseActivity :
    BaseActivity(),
    AncVisitCallBack,
    View.OnClickListener,
    OnDialogDismissListener {
    private lateinit var binding: ActivityHivMedicalReviewBaseBinding
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val hivViewModel: HivViewModel by viewModels()
    private val clinicalNotesViewModel: ClinicalNotesViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()
    private val weightViewModel: MotherNeonateBpWeightViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityHivMedicalReviewBaseBinding.inflate(layoutInflater)
        initSetup()
        initStaticDataCall()
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
        setButtonClickListener()
        attachObserver()
    }

    private fun initSetup() {
        setMainContentView(
            binding.root,
            true,
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation()
            },
            callbackHome = {
                backNavigationToHome()
            },
        )
        hivViewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        hivViewModel.isEMTCT = intent.getBooleanExtra(DefinedParams.EMTCT, false)
    }

    private fun attachObserver() {
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

        hivViewModel.createHivScreeningLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data.let { hivScreeningDetails ->
                        resourceState.data?.let {
                            hivViewModel.encounterId = it.encounterId.toString()
                            hivViewModel.patientReference = it.patientReference.toString()
                            hivViewModel.isHivSummary = true
                            showReviewSummary(it.encounterId, it.patientReference)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {}
                }
            }
        }

        hivViewModel.createHivMedicalReviewSummaryLiveData.observe(this) { resources ->

            when (resources.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    MedicalReviewSuccessDialogFragment.newInstance().show(
                        supportFragmentManager,
                        MedicalReviewSuccessDialogFragment.TAG,
                    )
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {}
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
    }

    private fun setButtonClickListener() {
        binding.btnSubmit.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
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
    }

    private fun initializePatientDetailFragment() {
        replaceFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            PatientInfoFragment
                .newInstanceForEMTCT(
                    intent.getStringExtra(DefinedParams.PatientId),
                    isEMTCT = true,
                    isEMTCTSummary = hivViewModel.isHivSummary,
                ).apply {
                    setDataCallback(this@HivMedicalReviewBaseActivity)
                },
        )
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    private fun swipeRefresh() {
        if (!connectivityManager.isNetworkAvailable()) {
            showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
                binding.refreshLayout.isRefreshing = false
                startActivityWithoutSplashScreen()
            }
            return
        }

        if (hivViewModel.isHivSummary) {
            showReviewSummary(hivViewModel.encounterId, hivViewModel.patientReference)
            binding.refreshLayout.isRefreshing = false
        } else {
            supportFragmentManager.findFragmentById(R.id.patientDetailFragment)?.let {
                patientViewModel.getPatientId()?.let { id ->
                    patientViewModel.getPatients(id)
                }
            }
        }
        binding.refreshLayout.isRefreshing = false
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
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

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                hivViewModel.isHivSummary = false
                onBackPressPopStack()
            }
        }
    }

    private fun onBackPressPopStack() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun enableSubmitBtn() {
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? ClinicalNotesFragment
        val isClinicalNotesValid = clinicalNotesFragment?.validateInput()

        binding.btnSubmit.isEnabled = (isClinicalNotesValid == true)
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        hivViewModel.memberId = details.memberId
        initializeFragments()
    }

    private fun initializeFragments() {
        supportFragmentManager.setFragmentResultListener(
            MedicalReviewDefinedParams.SUMMARY_ITEM,
            this,
        ) { _, _ ->
            enableReferralDoneBtn()
        }

        setupResultListeners()

        with(binding) {
            patientDetailFragment.visible()
            patientBMIContainer.visible()
            patientEligibility.visible()
        }
        replaceFragment(
            R.id.patientBMIContainer,
            HivMedicalReviewDiagnosesFragment.TAG,
            HivMedicalReviewDiagnosesFragment.newInstance(true),
        )
        val bundle = if (hivViewModel.isEMTCT) {
            Bundle().apply {
                putBoolean(DefinedParams.EMTCT, intent.getBooleanExtra(DefinedParams.EMTCT, false))
                putBoolean(DefinedParams.isPregnant, patientViewModel.isPregnant())
                putBoolean(DefinedParams.GENDER, patientViewModel.getGenderIsFemale())
            }
        } else {
            Bundle().apply {
                putBoolean(DefinedParams.isPregnant, patientViewModel.isPregnant())
                putBoolean(DefinedParams.GENDER, patientViewModel.getGenderIsFemale())
            }
        }

        replaceFragmentOrCreateNewFragment<EligibilityFragment>(
            binding.patientEligibility.id,
            bundle = bundle,
            tag = EligibilityFragment.TAG,
        )

        replaceFragmentOrCreateNewFragment<HivTestFragment>(
            binding.patientHIVTest.id,
            bundle = bundle,
            tag = HivTestFragment.TAG,
        )
        replaceFragmentOrCreateNewFragment<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id,
            bundle = null,
            tag = ClinicalNotesFragment.TAG,
        )
    }

    private fun enableReferralDoneBtn() {
        binding.btnDone.isEnabled =
            hivViewModel.selectedPatientStatus != null ||
            hivViewModel.nextVisitDate != null
    }

    private fun setupResultListeners() {
        val listener: (String, Bundle) -> Unit = { _, _ -> enableSubmitBtn() }
        supportFragmentManager.setFragmentResultListener(CLINICAL_NOTES, this, listener)
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
        swipeRefresh()
    }

    private fun callPatientDetails() {
        withNetworkAvailability(online = {
            supportFragmentManager
                .findFragmentById(R.id.patientDetailFragment)
                .let {
                    patientViewModel.getPatientId()?.let { id ->
                        patientViewModel.getPatients(id)
                    }
                }
        })
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            hivViewModel.lastLocation = it
        }
    }

    private fun createHivDetails() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
            if (connectivityManager.isNetworkAvailable()) {
                val haveYouTakenTestBefore =
                    hivViewModel.resultHashMap[HaveYouTakenHivTestBefore] as? String
                haveYouTakenTestBefore.let { isHaveYouTakenTestBefore ->
                    hivViewModel.createHivRequestModel(
                        patientListRespModel = details,
                        selectedEligibilityPair = Pair(
                            hivViewModel.selectedHistoryListItem.map { it.value },
                            hivViewModel.selectedPopulationType.map { it.value },
                        ),
                        haveHivTestTestedBeforePair = Pair(
                            isHaveYouTakenTestBefore,
                            hivViewModel.selectedLastTestForHIV,
                        ),
                        hivTestResult = Triple(
                            hivViewModel.resultHashMap[A1_TEST_RESULT] as? String ?: "",
                            hivViewModel.resultHashMap[A2_TEST_RESULT] as? String ?: "",
                            hivViewModel.resultHashMap[A3_TEST_RESULT] as? String ?: "",
                        ),
                        entryPoint = hivViewModel.selectedEntryPoint,
                        hivEmtctResult = Pair(
                            hivViewModel.resultHashMap[HIV_SYPHILIS_DUO_TEST] as? String ?: "",
                            hivViewModel.resultHashMap[HBsAg] as? String ?: "",
                        ),
                        clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes,
                        encounterId = patientViewModel.encounterId,
                        isConsentGiven = if (!hivViewModel.isEMTCT) true else false,
                        weight = weightViewModel.getWeight(),
                        height = weightViewModel.getHeights(),
                    )
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

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnSubmit -> {
                if (validation()) {
                    showLoading()
                    createHivDetails()
                }
            }

            R.id.btnDone -> {
                if (summaryValidation()) {
                    createHivSummaryDetails()
                }
            }

            R.id.btnRefer -> {
                ReferPatientFragment
                    .newInstance(
                        MedicalReviewTypeEnums.HIV.name,
                        hivViewModel.patientReference,
                        hivViewModel.encounterId,
                    ).show(
                        supportFragmentManager,
                        ReferPatientFragment.TAG,
                    )
            }

            binding.ivInvestigation.id -> {
                patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, InvestigationActivity::class.java)
                    intent.putExtra(PatientId, data.patientId)
                    intent.putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
                    getResult.launch(intent)
                }
            }

            R.id.ivPrescription -> {
                patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    Intent(this, PrescriptionActivity::class.java).apply {
                        putExtra(PatientId, data.patientId)
                        putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
                        getResult.launch(this)
                    }
                }
            }
        }
    }

    private fun validation(): Boolean {
        val eligibilityFragment =
            supportFragmentManager.findFragmentById(R.id.patientEligibility) as? EligibilityFragment
        val isValidEligibilityFragment = eligibilityFragment?.validation()
        val hivTestFragment =
            supportFragmentManager.findFragmentById(R.id.patientHIVTest) as? HivTestFragment
        val isHivTestFragment = hivTestFragment?.validation()
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? ClinicalNotesFragment
        val isClinicalNotesValid = clinicalNotesFragment?.validateInput()

        return (isValidEligibilityFragment == true && isHivTestFragment == true && isClinicalNotesValid == true)
    }

    private fun summaryValidation(): Boolean {
        val summaryFragment =
            supportFragmentManager.findFragmentById(R.id.hivSummary) as? HivSummaryFragment
        val isValidSummaryFragment = summaryFragment?.validation()
        return (isValidSummaryFragment == true)
    }

    private fun showReviewSummary(
        encounterId: String?,
        patientReference: String?,
    ) {
        scrollUp()
        removeFragment(R.id.patientBMIContainer)
        removeFragment(R.id.patientHIVTest)
        removeFragment(R.id.patientEligibility)
        val bundle = Bundle().apply {
            putString(DefinedParams.ENCOUNTER_ID, encounterId)
            putString(DefinedParams.PatientReference, patientReference)
            putBoolean(DefinedParams.EMTCT, hivViewModel.isEMTCT)
        }
        replaceFragmentInId<HivSummaryFragment>(
            binding.hivSummary.id,
            bundle,
            tag = HivSummaryFragment.TAG,
        )
        binding.bottomNavigationView.gone()
        binding.referralBottomView.visible()
    }

    private fun removeFragment(hivCreateScreeningSummary: Int) {
        supportFragmentManager.findFragmentById(hivCreateScreeningSummary)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    private fun createHivSummaryDetails() {
        val request = MedicalReviewSummarySubmitRequest(
            category = HIV,
            encounterType = HIV_MEDICAL_REVIEW,
            patientReference = hivViewModel.patientReference,
            id = hivViewModel.getSubmitCreateId(),
            villageId = hivViewModel.villageId,
            memberId = hivViewModel.memberId,
            provenance = ProvanceDto(),
            patientId = hivViewModel.patientId,
            patientStatus = hivViewModel.selectedPatientStatus,
            nextVisitDate = DateUtils
                .convertDateTimeToDate(
                    hivViewModel.nextVisitDate,
                    DATE_ddMMyyyy,
                    DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                ).takeIf { it.isNotEmpty() },
        )
        hivViewModel.createHivSummary(request)
    }

    private val getResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(DefinedParams.ENCOUNTER_ID)?.let { value ->
                    patientViewModel.encounterId = value
                }
            }
        }

    private fun scrollUp() {
        binding.nestedScrollViewID.post {
            binding.nestedScrollViewID.fullScroll(View.FOCUS_UP)
        }
    }
}
