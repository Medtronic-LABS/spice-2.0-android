package com.medtroniclabs.opensource.ui.medicalreview.underfiveyears

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.NestedScrollView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.ActivityUnderFiveYearsBaseBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.common.FloatingDetectorFrameLayout
import com.medtroniclabs.opensource.ui.dialog.MedicalReviewSuccessDialogFragment
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.ClinicalNotesFragment
import com.medtroniclabs.opensource.ui.medicalreview.PresentingComplaintsFragment
import com.medtroniclabs.opensource.ui.medicalreview.SystemicExaminationsFragment
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.PresentingComplaintsViewModel
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.SystemicExaminationViewModel
import com.medtroniclabs.opensource.ui.medicalreview.examinations.ExaminationCardFragment
import com.medtroniclabs.opensource.ui.medicalreview.examinations.ExaminationCardViewModel
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.medicalreview.prescription.PrescriptionActivity
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.fragment.UnderTwoMonthsTreatmentSummaryFragment
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.ReferPatientFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.ReferPatientViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnderFiveYearsBaseActivity : BaseActivity(), View.OnClickListener, OnDialogDismissListener,
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
        setMainContentView(binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callbackHome = {
                backNavigationToHome()
            },
            callback = {
                backNavigation()
            })
        withNetworkCheck(connectivityManager,::initializeViews,::onBackPressPopStack)
        attachObserver()
        initializeListeners()
        setAnalytics()
    }
    private fun setAnalytics(){
        UserDetail.eventName= AnalyticsDefinedParams.MedicalReviewCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.UnderFiveYears)
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer)
                    .let { currentFragment ->
                        if (currentFragment !is UnderFiveYearsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.UnderFiveYears,
                                eventName = UserDetail.eventName,
                                exitReason = AnalyticsDefinedParams.HomeButtonClicked,
                                isCompleted = false
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
                onNetworkNotAvailable = { setRefresh(false) })
        }
    }

    private fun swipeRefresh() {
        showLoading()
        viewModel.isSwipeRefresh = true
        supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer)
            .let { currentFragment ->
                if (currentFragment is UnderFiveYearsTreatmentSummaryFragment) {
                    getUnderFiveYearsSummaryDetails()
                } else {
                    patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        details.patientId?.let { id ->
                            patientDetailViewModel.getPatients(id, origin = null)
                            setRefresh(false)
                        }
                    }
                }
            }
    }

    private fun setRefresh(isRefresh: Boolean) =
        with(binding.refreshLayout) { isRefreshing = isRefresh }

    private fun getUnderFiveYearsSummaryDetails(){
        summaryViewModel.getUnderFiveYearsSummaryDetails(
            CreateUnderTwoMonthsResponse(
                encounterId = viewModel.encounterId ?: "",
                patientReference = viewModel.patientReference ?: ""
            )
        )
    }

    private fun diagnosisAndExaminationFragment() {
        replaceFragmentInId<MedicalReviewPatientDiagnosisFragment>(
            binding.patientDiagnosisContainer.id,
            bundle = initializeBundle(),
            tag = MedicalReviewPatientDiagnosisFragment.TAG
        )
    }

    private fun systemicExaminationFragment() {
        replaceFragmentInId<SystemicExaminationsFragment>(
            binding.systemicExaminationsContainer.id,
            bundle = initializeBundle(),
            tag = SystemicExaminationsFragment.TAG
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
                        positiveButtonName = getString(R.string.ok)
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
                        positiveButtonName = getString(R.string.ok)
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
                        eventName = UserDetail.eventName
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
                        positiveButtonName = getString(R.string.ok)
                    ) {
                        if (it) {
                            onBackPressPopStack()
                        }
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    MedicalReviewSuccessDialogFragment.newInstance().show(
                        supportFragmentManager, MedicalReviewSuccessDialogFragment.TAG
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
                        supportFragmentManager, MedicalReviewSuccessDialogFragment.TAG
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
            summaryViewModel.nextVisitDate?.isNotBlank() == true || summaryViewModel.selectedPatientStatus?.isNotBlank() == true
    }

    private fun showReviewSummary(encounterId: String?, patientReference: String?) {
        val bundle = Bundle().apply {
            putString(DefinedParams.EncounterId, encounterId)
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
            tag = UnderFiveYearsTreatmentSummaryFragment.TAG
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
        supportFragmentManager.beginTransaction().add(
                R.id.patientDetailFragment, patientInfoFragment
            ).commit()
        patientInfoFragment.setDataCallback(this)
    }

    private fun initializeBundle(): Bundle {
        return Bundle().apply {
            putString(
                MedicalReviewTypeEnums.PresentingComplaints.name,
                MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
            )
            putString(
                MedicalReviewTypeEnums.DiagnosisType.name,
                MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
            )
            putString(
                DefinedParams.ID, intent.getStringExtra(DefinedParams.ID)
            )
            putString(
                DefinedParams.MemberID, viewModel.memberId
            )
        }
    }

    private fun initializeFragments(details: PatientListRespModel) {
        binding.patientDiagnosisContainer.visibility = View.VISIBLE
        diagnosisAndExaminationFragment()
        showLoading()
        systemicExaminationFragment()
        val ageInMonth =details.birthDate?.let { DateUtils.calculateAgeInMonths(it) }
        val bundle = Bundle().apply {
            putString(DefinedParams.Gender, details.gender)
            ageInMonth?.first?.let { putInt(DefinedParams.Age, it) }
        }
        replaceFragmentInId<ClinicalSummaryUnderFiveYearsFragment>(
            binding.clinicalSummaryContainer.id, tag = ClinicalSummaryUnderFiveYearsFragment.TAG, bundle = bundle
        )
        replaceFragmentInId<ExaminationCardFragment>(
            binding.examinationsContainer.id,
            bundle = initializeBundle(),
            tag = ExaminationCardFragment.TAG
        )
        replaceFragmentInId<PresentingComplaintsFragment>(
            binding.presentingComplaintsContainer.id, tag = PresentingComplaintsFragment.TAG
        )
        replaceFragmentInId<ClinicalNotesFragment>(
            binding.clinicalNotesContainer.id, tag = ClinicalNotesFragment.TAG
        )

        supportFragmentManager.setFragmentResultListener(
                MedicalReviewDefinedParams.SUMMARY_ITEM,
                this
            ) { _, _ ->
                enableReferralDoneBtn()
            }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnSubmit -> {
                if (validateInputs()) {
                   withNetworkCheck(connectivityManager,::summaryCreate)
                }
            }

            R.id.btnDone -> {
                withNetworkCheck(connectivityManager,::summaryDone)
            }

            binding.ivPrescription.id -> {
                handlePrescriptionClick()
            }
            binding.ivInvestigation.id  -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, InvestigationActivity::class.java)
                    intent.putExtra(DefinedParams.PatientId, data.patientId)
                    intent.putExtra(DefinedParams.EncounterId,patientDetailViewModel.encounterId)
                    getResult.launch(intent)
                }
            }

            R.id.btnRefer -> {
                viewModel.createUnderFiveMedicalReviewLiveData.value?.data?.let {
                    ReferPatientFragment.newInstance(
                        MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
                        it.patientReference,
                        it.encounterId
                    ).show(
                        supportFragmentManager, ReferPatientFragment.TAG
                    )
                }
            }
        }
    }

    private fun handlePrescriptionClick() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
            Intent(this, PrescriptionActivity::class.java).apply {
                putExtra(DefinedParams.PatientId, data.patientId)
                putExtra(DefinedParams.EncounterId, patientDetailViewModel.encounterId)
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
                patientDetailViewModel.encounterId
            )
        }
    }

    private val getResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(DefinedParams.EncounterId)?.let { value ->
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
                        patientReferenceId
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
        autoScrollError(isClinicalSummaryValid,isClinicalNotesValid)
        return (isClinicalSummaryValid==true && isClinicalNotesValid==true && isPresentingComplaintsValid == true)
    }

    private fun autoScrollError(isClinicalSummaryValid: Boolean?, isClinicalNotesValid: Boolean?) {
        if (isClinicalSummaryValid!=true && isClinicalNotesValid==true){
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

    private fun getSummaryStatus(): Boolean {
        return ((summaryViewModel.selectedPatientStatus == DefinedParams.Recovered && summaryViewModel.nextVisitDate == null) || (summaryViewModel.selectedPatientStatus != DefinedParams.Recovered && summaryViewModel.nextVisitDate != null))
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert), getString(R.string.exit_reason), isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer)
                    .let { currentFragment ->
                        if (currentFragment !is UnderTwoMonthsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.UnderFiveYears,
                                eventName = UserDetail.eventName,
                                exitReason = AnalyticsDefinedParams.BackButtonClicked,
                                isCompleted = false
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

}