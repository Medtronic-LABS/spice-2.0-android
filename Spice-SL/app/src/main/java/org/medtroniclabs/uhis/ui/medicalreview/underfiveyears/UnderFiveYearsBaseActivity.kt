package org.medtroniclabs.uhis.ui.medicalreview.underfiveyears

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.NestedScrollView
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.databinding.ActivityUnderFiveYearsBaseBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.model.medicalreview.CreateUnderTwoMonthsResponse
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.common.FloatingDetectorFrameLayout
import org.medtroniclabs.uhis.ui.dialog.MedicalReviewSuccessDialogFragment
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.medicalreview.ClinicalNotesFragment
import org.medtroniclabs.uhis.ui.medicalreview.PresentingComplaintsFragment
import org.medtroniclabs.uhis.ui.medicalreview.SystemicExaminationsFragment
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.PresentingComplaintsViewModel
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.SystemicExaminationViewModel
import org.medtroniclabs.uhis.ui.medicalreview.examinations.ExaminationCardFragment
import org.medtroniclabs.uhis.ui.medicalreview.examinations.ExaminationCardViewModel
import org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.medicalreview.prescription.PrescriptionActivity
import org.medtroniclabs.uhis.ui.medicalreview.undertwomonths.fragment.UnderTwoMonthsTreatmentSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.ReferPatientFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

@AndroidEntryPoint
class UnderFiveYearsBaseActivity :
    BaseActivity(),
    View.OnClickListener,
    OnDialogDismissListener,
    AncVisitCallBack {
    private lateinit var binding: ActivityUnderFiveYearsBaseBinding
    private val viewModel: UnderFiveYearsViewModel by viewModels()
    private val summaryViewModel: UnderFiveYearTreatmentSummaryViewModel by viewModels()
    private val systemicExaminationViewModel: SystemicExaminationViewModel by viewModels()
    private val clinicalNotesViewModel: ClinicalNotesViewModel by viewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by viewModels()
    private val clinicalSummaryViewModel: UnderFiveYearsClinicalSummaryViewModel by viewModels()
    private val examinationCardViewModel: ExaminationCardViewModel by viewModels()
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityUnderFiveYearsBaseBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callbackHome = {
                backNavigationToHome()
            },
            callback = {
                backNavigation()
            },
        )
        withNetworkCheck(connectivityManager, ::initializeViews, ::onBackPressPopStack)
        attachObserver()
        initializeListeners()
        setAnalytics()
    }

    private fun setAnalytics() {
        UserDetail.eventName = AnalyticsDefinedParams.MedicalReviewCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.UnderFiveYears)
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                viewModel.setUserJourney(AnalyticsDefinedParams.ONHOMEBUTTONTRIGGERED)
                supportFragmentManager
                    .findFragmentById(R.id.clinicalSummaryContainer)
                    .let { currentFragment ->
                        if (currentFragment !is UnderFiveYearsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.UnderFiveYears,
                                eventName = UserDetail.eventName,
                                exitReason = AnalyticsDefinedParams.HomeButtonClicked,
                                isCompleted = false,
                            )
                        }
                    }
                startActivityWithoutSplashScreen()
            }
        }
    }

    private fun initializeListeners() {
        binding.btnSubmit.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
        binding.underFiveSummaryBottomView.btnDone.safeClickListener(this)
        binding.underFiveSummaryBottomView.btnRefer.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.refreshLayout.setOnRefreshListener {
            withNetworkCheck(
                connectivityManager,
                ::swipeRefresh,
                onNetworkNotAvailable = { setRefresh(false) },
            )
        }
    }

    private fun swipeRefresh() {
        showLoading()
        viewModel.isSwipeRefresh = true
        supportFragmentManager
            .findFragmentById(R.id.clinicalSummaryContainer)
            .let { currentFragment ->
                if (currentFragment is UnderFiveYearsTreatmentSummaryFragment) {
                    getUnderFiveYearsSummaryDetails()
                } else {
                    patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        details.patientId?.let { id ->
                            patientDetailViewModel.getPatients(id)
                            setRefresh(false)
                        }
                    }
                }
            }
    }

    private fun setRefresh(isRefresh: Boolean) = with(binding.refreshLayout) { isRefreshing = isRefresh }

    private fun getUnderFiveYearsSummaryDetails() {
        summaryViewModel.getUnderFiveYearsSummaryDetails(
            CreateUnderTwoMonthsResponse(
                encounterId = viewModel.encounterId ?: "",
                patientReference = viewModel.patientReference ?: "",
            ),
        )
    }

    private fun diagnosisAndExaminationFragment() {
        replaceFragmentInId<MedicalReviewPatientDiagnosisFragment>(
            binding.patientDiagnosisContainer.id,
            bundle = initializeBundle(),
            tag = MedicalReviewPatientDiagnosisFragment.TAG,
        )
    }

    private fun systemicExaminationFragment() {
        replaceFragmentInId<SystemicExaminationsFragment>(
            binding.systemicExaminationsContainer.id,
            bundle = initializeBundle(),
            tag = SystemicExaminationsFragment.TAG,
        )
    }

    private fun attachObserver() {
        viewModel.underFiveYearsMetaLiveData.observe(this) { resourceState ->
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
                    initializePatientDetailFragment()
                }
            }
        }
        patientDetailViewModel.patientDetailsLiveData.observe(this) { resource ->
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

        viewModel.createUnderFiveYearMedicalReview.observe(this) { resourceState ->
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
                    binding.nestedScrollViewID.fullScroll(ScrollView.FOCUS_UP)
                    hideLoading()
                    resourceState.data?.let {
                        viewModel.encounterId = it.encounterId.toString()
                        viewModel.patientReference = it.patientReference.toString()
                        showReviewSummary(it.encounterId, it.patientReference)
                    }
                    viewModel.setAnalyticsData(
                        UserDetail.startDateTime,
                        eventType = AnalyticsDefinedParams.UnderFiveYears,
                        eventName = UserDetail.eventName,
                    )
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
                        if (it) {
                            onBackPressPopStack()
                        }
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

        summaryViewModel.checkSubmitBtn.observe(this) {
            summaryValidation()
        }
    }

    private fun summaryValidation() {
        binding.underFiveSummaryBottomView.btnDone.isEnabled =
            summaryViewModel.nextVisitDate?.isNotBlank() == true ||
            summaryViewModel.selectedPatientStatus?.isNotBlank() == true
    }

    private fun showReviewSummary(
        encounterId: String?,
        patientReference: String?,
    ) {
        val bundle = Bundle().apply {
            putString(DefinedParams.ENCOUNTER_ID, encounterId)
            putString(DefinedParams.PatientReference, patientReference)
        }
        removeFragment(R.id.clinicalSummaryContainer)
        removeFragment(R.id.examinationsContainer)
        removeFragment(R.id.presentingComplaintsContainer)
        removeFragment(R.id.clinicalNotesContainer)
        removeFragment(R.id.patientDiagnosisContainer)
        removeFragment(R.id.birthHistoryContainer)
        removeFragment(R.id.systemicExaminationsContainer)
        replaceFragmentInId<UnderFiveYearsTreatmentSummaryFragment>(
            binding.clinicalSummaryContainer.id,
            bundle,
            tag = UnderFiveYearsTreatmentSummaryFragment.TAG,
        )
        binding.bottomNavigationView.gone()
        binding.underFiveSummaryBottomView.root.visibility = View.VISIBLE
    }

    private fun initializeViews() {
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        examinationCardViewModel.workFlowType = MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
        if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_UNDER_FIVE_YEARS_LOADED.name))) {
            viewModel.getStaticMetaData()
        } else {
            initializePatientDetailFragment()
        }
    }

    private fun initializePatientDetailFragment() {
        val patientInfoFragment =
            PatientInfoFragment.newInstance(intent.getStringExtra(DefinedParams.PatientId))
        supportFragmentManager
            .beginTransaction()
            .add(
                R.id.patientDetailFragment,
                patientInfoFragment,
            ).commit()
        patientInfoFragment.setDataCallback(this)
    }

    private fun initializeBundle(): Bundle =
        Bundle().apply {
            putString(
                MedicalReviewTypeEnums.PresentingComplaints.name,
                MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
            )
            putString(
                MedicalReviewTypeEnums.DiagnosisType.name,
                MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
            )
            putString(
                DefinedParams.ID,
                intent.getStringExtra(DefinedParams.ID),
            )
            putString(
                DefinedParams.MEMBER_ID,
                viewModel.memberId,
            )
        }

    private fun initializeFragments(details: PatientListRespModel) {
        binding.patientDiagnosisContainer.visibility = View.VISIBLE
        diagnosisAndExaminationFragment()
        showLoading()
        systemicExaminationFragment()
        val ageInMonth = details.birthDate?.let { DateUtils.calculateAgeInMonths(it) }
        val bundle = Bundle().apply {
            putString(DefinedParams.GENDER, details.gender)
            ageInMonth?.first?.let { putInt(DefinedParams.AGE, it) }
        }
        replaceFragmentInId<ClinicalSummaryUnderFiveYearsFragment>(
            binding.clinicalSummaryContainer.id,
            tag = ClinicalSummaryUnderFiveYearsFragment.TAG,
            bundle = bundle,
        )
        replaceFragmentInId<ExaminationCardFragment>(
            binding.examinationsContainer.id,
            bundle = initializeBundle(),
            tag = ExaminationCardFragment.TAG,
        )
        replaceFragmentInId<PresentingComplaintsFragment>(
            binding.presentingComplaintsContainer.id,
            tag = PresentingComplaintsFragment.TAG,
        )
        replaceFragmentInId<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id,
            tag = ClinicalNotesFragment.TAG,
        )

        supportFragmentManager.setFragmentResultListener(
            MedicalReviewDefinedParams.SUMMARY_ITEM,
            this,
        ) { _, _ ->
            enableReferralDoneBtn()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnSubmit -> {
                if (validateInputs()) {
                    withLocationCheck({ withNetworkCheck(connectivityManager, ::summaryCreate) })
                }
            }

            R.id.btnDone -> {
                withLocationCheck({ withNetworkCheck(connectivityManager, ::summaryDone) })
            }

            binding.ivPrescription.id -> {
                handlePrescriptionClick()
            }
            binding.ivInvestigation.id -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, InvestigationActivity::class.java)
                    intent.putExtra(DefinedParams.PatientId, data.patientId)
                    intent.putExtra(DefinedParams.ENCOUNTER_ID, patientDetailViewModel.encounterId)
                    getResult.launch(intent)
                }
            }

            R.id.btnRefer -> {
                viewModel.createUnderFiveMedicalReviewLiveData.value?.data?.let {
                    ReferPatientFragment
                        .newInstance(
                            MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
                            it.patientReference,
                            it.encounterId,
                        ).show(
                            supportFragmentManager,
                            ReferPatientFragment.TAG,
                        )
                }
            }
        }
    }

    private fun handlePrescriptionClick() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
            Intent(this, PrescriptionActivity::class.java).apply {
                putExtra(DefinedParams.PatientId, data.patientId)
                putExtra(DefinedParams.ENCOUNTER_ID, patientDetailViewModel.encounterId)
                getResult.launch(this)
            }
        }
    }

    private fun summaryCreate() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { patientDetail ->
            viewModel.createMedicalReviewForUnderFiveYears(
                patientDetail,
                clinicalSummaryAndSigns = clinicalSummaryViewModel.clinicalSummaryAndSigns,
                examinationResultHashMap = examinationCardViewModel.examinationResultHashMap,
                clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes,
                presentingComplaints = presentingComplaintsViewModel.enteredComplaintNotes,
                systemicExaminations = systemicExaminationViewModel.selectedSystemicExaminations.map { it.value },
                systemicExaminationsNotes = systemicExaminationViewModel.enteredExaminationNotes,
                patientDetailViewModel.encounterId,
            )
        }
    }

    private val getResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(DefinedParams.ENCOUNTER_ID)?.let { value ->
                    patientDetailViewModel.encounterId = value
                }
            }
        }

    private fun summaryDone() {
        binding.underFiveSummaryBottomView.btnDone.isEnabled = true
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
            viewModel.createUnderFiveYearMedicalReview.value?.data?.encounterId?.let { submitEncounterId ->
                viewModel.createUnderFiveYearMedicalReview.value?.data?.patientReference?.let { patientReferenceId ->
                    viewModel.underFiveYearsSummaryCreate(
                        details,
                        submitEncounterId,
                        summaryViewModel.nextVisitDate,
                        summaryViewModel.selectedPatientStatus,
                        patientReferenceId,
                    )
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val clinicalSummaryUnderFiveYearsFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer) as? ClinicalSummaryUnderFiveYearsFragment
        val presentingComplaintsFragment =
            supportFragmentManager.findFragmentById(R.id.presentingComplaintsContainer) as? PresentingComplaintsFragment
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? ClinicalNotesFragment
        val isPresentingComplaintsValid = presentingComplaintsFragment?.validate()
        val isClinicalSummaryValid = clinicalSummaryUnderFiveYearsFragment?.validateEditFields()
        val isClinicalNotesValid = clinicalNotesFragment?.validateInput()
        autoScrollError(isClinicalSummaryValid, isClinicalNotesValid)
        return (isClinicalSummaryValid == true && isClinicalNotesValid == true && isPresentingComplaintsValid == true)
    }

    private fun autoScrollError(
        isClinicalSummaryValid: Boolean?,
        isClinicalNotesValid: Boolean?,
    ) {
        if (isClinicalSummaryValid != true && isClinicalNotesValid == true) {
            val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollViewID)
            val clinicalSummaryContainer = findViewById<FloatingDetectorFrameLayout>(R.id.clinicalSummaryContainer)
            val y = clinicalSummaryContainer.top
            nestedScrollView.smoothScrollTo(0, y)
        }
    }

    private fun removeFragment(clinicalSummaryContainer: Int) {
        supportFragmentManager.findFragmentById(clinicalSummaryContainer)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    private fun enableReferralDoneBtn() {
        binding.underFiveSummaryBottomView.btnDone.isEnabled = getSummaryStatus()
    }

    private fun getSummaryStatus(): Boolean =
        (
            (summaryViewModel.selectedPatientStatus == DefinedParams.Recovered && summaryViewModel.nextVisitDate == null) ||
                (summaryViewModel.selectedPatientStatus != DefinedParams.Recovered && summaryViewModel.nextVisitDate != null)
        )

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                supportFragmentManager
                    .findFragmentById(R.id.clinicalSummaryContainer)
                    .let { currentFragment ->
                        if (currentFragment !is UnderTwoMonthsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.UnderFiveYears,
                                eventName = UserDetail.eventName,
                                exitReason = AnalyticsDefinedParams.BackButtonClicked,
                                isCompleted = false,
                            )
                        }
                    }
                onBackPressPopStack()
            }
        }
    }

    private fun onBackPressPopStack() {
        this@UnderFiveYearsBaseActivity.finish()
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        if (viewModel.isSwipeRefresh) {
            systemicExaminationFragment()
        } else {
            initializeFragments(details)
        }
        viewModel.memberId = details.memberId
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
            }
        }

    private fun getCurrentLocation() {
        SpiceLocationManager(this).getCurrentLocation {
            viewModel.lastLocation = it
        }
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }
}
