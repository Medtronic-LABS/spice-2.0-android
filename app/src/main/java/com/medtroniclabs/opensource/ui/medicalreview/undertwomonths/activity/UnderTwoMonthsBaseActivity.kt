package com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.ActivityUnderTwoMonthsBaseBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.common.FloatingDetectorFrameLayout
import com.medtroniclabs.opensource.ui.dialog.MedicalReviewSuccessDialogFragment
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.ClinicalNotesFragment
import com.medtroniclabs.opensource.ui.medicalreview.PresentingComplaintsFragment
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.PresentingComplaintsViewModel
import com.medtroniclabs.opensource.ui.medicalreview.examinations.ExaminationCardFragment
import com.medtroniclabs.opensource.ui.medicalreview.examinations.ExaminationCardViewModel
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.medicalreview.prescription.PrescriptionActivity
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.fragment.BirthHistoryFragment
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.fragment.ClinicalSummaryFragment
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.fragment.UnderTwoMonthsTreatmentSummaryFragment
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel.ClinicalSummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel.UnderTwoMonthViewModel
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel.UnderTwoMonthsTreatmentSummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.fragment.MedicalReviewPatientDiagnosisFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.ReferPatientFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.ReferPatientViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnderTwoMonthsBaseActivity : BaseActivity(), View.OnClickListener, OnDialogDismissListener,
    AncVisitCallBack {

    private lateinit var binding: ActivityUnderTwoMonthsBaseBinding
    private val viewModel: UnderTwoMonthViewModel by viewModels()
    private val summaryViewModel: UnderTwoMonthsTreatmentSummaryViewModel by viewModels()
    private val clinicalNotesViewModel: ClinicalNotesViewModel by viewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by viewModels()
    private val clinicalSummaryViewModel: ClinicalSummaryViewModel by viewModels()
    private val examinationCardViewModel: ExaminationCardViewModel by viewModels()
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityUnderTwoMonthsBaseBinding.inflate(layoutInflater)
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
        withNetworkCheck(connectivityManager,::initializeViews,::onBackPressPopStack)
        attachObserver()
        initializeListeners()
        setAnalytics()
    }
    private fun setAnalytics(){
        UserDetail.eventName= AnalyticsDefinedParams.MedicalReviewCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.UnderTwoMonths)
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
            }
        }
    private fun swipeRefresh() {
            supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer)
                .let { currentFragment ->
                    viewModel.isRefresh = true
                    if (currentFragment is ClinicalSummaryFragment) {
                        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
                            details.patientId?.let { id ->
                                patientDetailViewModel.getPatients(id, origin = null)
                            }
                        }
                    } else {
                        val createUnderTwoMonthsResponse = CreateUnderTwoMonthsResponse(
                            encounterId = viewModel.encounterId,
                            patientReference = viewModel.patientReference
                        )
                        summaryViewModel.getUnderTwoMonthsSummaryDetails(
                            createUnderTwoMonthsResponse
                        )
                        setRefresh(false)
                    }
                }

    }

    private fun attachObserver() {
        clinicalNotesViewModel.submitButtonStateLiveData.observe(this) {
            val clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes
            binding.btnSubmit.isEnabled = clinicalNotes.isNotEmpty() && clinicalNotes.isNotBlank()
        }
        referPatientViewModel.referPatientResultLiveData.observe(this) { resourceState ->
            handleResourceState(
                resourceState = resourceState,
                onSuccess = {
                    val fragment =
                        supportFragmentManager.findFragmentByTag(ReferPatientFragment.TAG) as? ReferPatientFragment
                    fragment?.dismiss()
                    MedicalReviewSuccessDialogFragment.newInstance().show(
                        supportFragmentManager,
                        MedicalReviewSuccessDialogFragment.TAG
                    )
                }
            )
        }

        viewModel.createUnderTwoMonthsMedicalReview.observe(this) { resourceState ->
            handleResourceState(
                resourceState = resourceState,
                onSuccessParam = { data ->
                    viewModel.encounterId = data.encounterId
                    viewModel.patientReference = data.patientReference
                    showUnderTwoMonthsReviewSummary()
                }
            )
        }

        viewModel.underTwoMonthsMetaLiveData.observe(this) { resourceState ->
            handleResourceState(
                resourceState = resourceState,
                onSuccess =::initializePatientDetailsFragments

            )
        }

        viewModel.summaryCreateResponse.observe(this) { resourceState ->
            handleResourceState(
                resourceState = resourceState,
                onSuccess = ::successSummaryDialog
            )
        }
        summaryViewModel.checkSubmitBtn.observe(this) {
            summaryValidation()
        }
    }

private fun successSummaryDialog() {
    MedicalReviewSuccessDialogFragment.newInstance().show(
        supportFragmentManager,
        MedicalReviewSuccessDialogFragment.TAG
    )
}
    private fun summaryValidation() {
        binding.underTwoSummaryBottomView.btnDone.isEnabled =
            summaryViewModel.nextVisitDate?.isNotBlank() == true || summaryViewModel.selectedPatientStatus?.isNotBlank() == true
    }

    private fun showUnderTwoMonthsReviewSummary() {
        removeFragment(R.id.clinicalSummaryContainer)
        removeFragment(R.id.examinationsContainer)
        removeFragment(R.id.presentingComplaintsContainer)
        removeFragment(R.id.clinicalNotesContainer)
        removeFragment(R.id.patientDiagnosisContainer)
        removeFragment(R.id.birthHistoryContainer)
        initializeUnderTwoMonthSummaryFragment()
        binding.bottomNavigationView.gone()
        binding.underTwoSummaryBottomView.root.visibility = View.VISIBLE
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.UnderTwoMonths,
            eventName = UserDetail.eventName,
        )
    }

    private fun initializeUnderTwoMonthSummaryFragment() {
        val bundle = Bundle().apply {
            putString(DefinedParams.EncounterId, viewModel.encounterId)
            putString(DefinedParams.PatientReference, viewModel.patientReference)
        }
        replaceFragmentInId<UnderTwoMonthsTreatmentSummaryFragment>(
            binding.clinicalSummaryContainer.id,
            bundle,
            tag = UnderTwoMonthsTreatmentSummaryFragment.TAG
        )
    }

    private fun initializeViews() {
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        examinationCardViewModel.workFlowType = MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name
        if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_UNDER_TWO_MONTHS_LOADED.name))) {
            viewModel.getStaticMetaData()
        } else {
            showLoading()
            initializePatientDetailsFragments()
        }
    }

    private fun initializePatientDetailsFragments() {
        val fragmentManager = supportFragmentManager
        val fragment =
            PatientInfoFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId)
            )
        fragment.setDataCallback(this)
        fragmentManager.beginTransaction()
            .add(R.id.patientDetailFragment, fragment)
            .commit()
    }

    private fun initializeDiagnosisFragments() {
        showLoading()
        replaceFragmentInId<MedicalReviewPatientDiagnosisFragment>(
            binding.patientDiagnosisContainer.id,
            bundle = initializeBundle(),
            tag = MedicalReviewPatientDiagnosisFragment.TAG
        )
        binding.patientDiagnosisContainer.visible()
    }

    private fun initializeBirthHistoryFragments() {
        replaceFragmentInId<BirthHistoryFragment>(
            binding.birthHistoryContainer.id,
            bundle = initializeBundle(),
            tag = BirthHistoryFragment.TAG
        )
    }

    private fun initializeBundle(): Bundle=Bundle().apply {
            putString(
                MedicalReviewTypeEnums.PresentingComplaints.name,
                MedicalReviewDefinedParams.PC_5YEARS
            )
            putString(
                MedicalReviewTypeEnums.SystemicExaminations.name,
                MedicalReviewDefinedParams.PC_5YEARS
            )
            putString(
                MedicalReviewTypeEnums.DiagnosisType.name,
                MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name
            )
            putString(
                DefinedParams.PatientId,
                intent.getStringExtra(DefinedParams.PatientId)
            )
            putString(
                DefinedParams.MemberID,
                viewModel.memberId
            )
            putString(
                DefinedParams.ID,
                intent.getStringExtra(DefinedParams.ID)
            )
    }

    private fun initializeFragments(details: PatientListRespModel) {
        showLoading()
        initializeDiagnosisFragments()
        initializeBirthHistoryFragments()
        val ageInMonth =details.birthDate?.let { DateUtils.calculateAgeInMonths(it) }
        val bundle = Bundle().apply {
            putString(DefinedParams.Gender, details.gender)
            ageInMonth?.first?.let { putInt(DefinedParams.Age, it) }
        }
        replaceFragment<ClinicalSummaryFragment>(binding.clinicalSummaryContainer.id,tag=ClinicalSummaryFragment.TAG, bundle = bundle)
        replaceFragment<ExaminationCardFragment>(binding.examinationsContainer.id,tag=ExaminationCardFragment.TAG)
        replaceFragment<PresentingComplaintsFragment>(binding.presentingComplaintsContainer.id, initializeBundle(),tag=PresentingComplaintsFragment.TAG)
        replaceFragment<ClinicalNotesFragment>(binding.clinicalNotesContainer.id, initializeBundle(), tag = ClinicalNotesFragment.TAG)
        supportFragmentManager.setFragmentResultListener(MedicalReviewDefinedParams.SUMMARY_ITEM, this) { _, _ ->
            enableReferralDoneBtn()
        }
    }

    private inline fun <reified T : Fragment> replaceFragment(containerId: Int, bundle: Bundle? = null,tag: String? = null) {
        replaceFragmentInId<T>(containerId, bundle = bundle, tag = tag)
    }


    private fun initializeListeners() {
        binding.btnSubmit.safeClickListener(this)
        binding.ivPrescription.safeClickListener(this)
        binding.ivInvestigation.safeClickListener(this)
        binding.underTwoSummaryBottomView.btnDone.isEnabled = false
        binding.underTwoSummaryBottomView.btnDone.safeClickListener(this)
        binding.underTwoSummaryBottomView.btnRefer.safeClickListener(this)
        binding.refreshLayout.setOnRefreshListener {
            withNetworkCheck(connectivityManager, ::swipeRefresh,
                onNetworkNotAvailable = { setRefresh(false) })
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnSubmit -> handleButtonSubmit()
            R.id.btnDone -> handleButtonDone()
            binding.ivPrescription.id -> handlePrescriptionClick()
            R.id.btnRefer -> handleButtonRefer()
            binding.ivInvestigation.id  -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, InvestigationActivity::class.java)
                    intent.putExtra(DefinedParams.PatientId, data.patientId)
                    intent.putExtra(DefinedParams.EncounterId,patientDetailViewModel.encounterId)
                    getResult.launch(intent)
                }
            }
        }
    }

    private fun handleButtonSubmit() {
        if (validateInputs()) {
            withNetworkCheck(connectivityManager, ::submitDetails)
        }
    }

    private fun handleButtonDone() = withNetworkCheck(connectivityManager, ::summaryDoneDetails)


    private fun handlePrescriptionClick() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
            Intent(this, PrescriptionActivity::class.java).apply {
                putExtra(DefinedParams.PatientId, data.patientId)
                putExtra(DefinedParams.EncounterId, patientDetailViewModel.encounterId)
                getResult.launch(this)
            }
        }
    }

    private fun handleButtonRefer() {
        viewModel.createUnderTwoMonthsMedicalReview.value?.data?.let {
            ReferPatientFragment.newInstance(
                MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name,
                it.patientReference,
                it.encounterId
            ).show(supportFragmentManager, ReferPatientFragment.TAG)
        }
    }


    private fun submitDetails() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
                viewModel.createMedicalReviewForUnderTwoMonths(
                    details,
                    clinicalSummaryAndSigns = clinicalSummaryViewModel.clinicalSummaryAndSigns,
                    examinationResultHashMap = examinationCardViewModel.examinationResultHashMap,
                    clinicalNotes = clinicalNotesViewModel.enteredClinicalNotes,
                    presentingComplaints = presentingComplaintsViewModel.enteredComplaintNotes,
                    patientDetailViewModel.encounterId
                )
        }
    }

    private fun summaryDoneDetails() {
        binding.underTwoSummaryBottomView.btnDone.isEnabled = true
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
            viewModel.createUnderTwoMonthsMedicalReview.value?.data?.encounterId
                ?.let { submitCreateId ->
                    viewModel.createUnderTwoMonthsMedicalReview.value?.data?.patientReference?.let { submitCreatePatientReference ->
                        viewModel.underTwoMonthsSummaryCreate(
                            details,
                            submitCreateId,
                            summaryViewModel.nextVisitDate,
                            summaryViewModel.selectedPatientStatus,
                            submitCreatePatientReference
                        )
                    }
                }
        }
    }

    private fun removeFragment(clinicalSummaryContainer: Int) {
        supportFragmentManager.findFragmentById(clinicalSummaryContainer)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    private fun validateInputs(): Boolean {
        val clinicalSummaryFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer) as? ClinicalSummaryFragment
        val presentingComplaintsFragment =
            supportFragmentManager.findFragmentById(R.id.presentingComplaintsContainer) as? PresentingComplaintsFragment
        val clinicalNotesFragment =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? ClinicalNotesFragment
        val isPresentingComplaintsValid = presentingComplaintsFragment?.validate()
        val isClinicalSummaryValid = clinicalSummaryFragment?.validateEditFields()
        val isClinicalNotesValid = clinicalNotesFragment?.validateInput()
        autoScrollError(isClinicalSummaryValid,isClinicalNotesValid)
        return (isClinicalSummaryValid==true && isPresentingComplaintsValid == true&& isClinicalNotesValid==true)
    }

    private fun autoScrollError(isClinicalSummaryValid: Boolean?, isClinicalNotesValid: Boolean?) {
        if (isClinicalSummaryValid!=true && isClinicalNotesValid==true){
            val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollViewID)
            val clinicalSummaryContainer = findViewById<FloatingDetectorFrameLayout>(R.id.clinicalSummaryContainer)
            val y = clinicalSummaryContainer.top
            nestedScrollView.smoothScrollTo(0, y)
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        startActivityWithoutSplashScreen()
    }

    private fun enableReferralDoneBtn() {
        binding.underTwoSummaryBottomView.btnDone.isEnabled = getSummaryStatus()
    }

    private fun getSummaryStatus(): Boolean {
        return ((summaryViewModel.selectedPatientStatus == ReferralStatus.Recovered.name && summaryViewModel.nextVisitDate == null)
                || (summaryViewModel.selectedPatientStatus != ReferralStatus.Recovered.name && summaryViewModel.nextVisitDate != null))
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer)
                    .let { currentFragment ->
                        if (currentFragment !is UnderTwoMonthsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.UnderTwoMonths,
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
        this@UnderTwoMonthsBaseActivity.finish()
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        viewModel.memberId = details.memberId
        when (viewModel.isRefresh) {
            true -> {
                showLoading()
                initializeBirthHistoryFragments()
                setRefresh(false)
            }
            false -> initializeFragments(details)
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

    private fun setRefresh(isRefresh: Boolean) =
        with(binding.refreshLayout) { isRefreshing = isRefresh }

    private fun <T> handleResourceState(
        resourceState: Resource<T>,
        onSuccess: () -> Unit={},
        onSuccessParam: (T) -> Unit={},
        onError: () -> Unit = {
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
    ) {
        when (resourceState.state) {
            ResourceState.LOADING -> {
                showLoading()
            }
            ResourceState.ERROR -> {
                hideLoading()
                onError()
            }
            ResourceState.SUCCESS -> {
                hideLoading()
                onSuccess()
                resourceState.data?.let(onSuccessParam)
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
                supportFragmentManager.findFragmentById(R.id.clinicalSummaryContainer)
                    .let { currentFragment ->
                        if (currentFragment !is UnderTwoMonthsTreatmentSummaryFragment) {
                            viewModel.setAnalyticsData(
                                UserDetail.startDateTime,
                                eventType = AnalyticsDefinedParams.UnderTwoMonths,
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

}