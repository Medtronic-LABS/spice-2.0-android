package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.data.MedicalReviewSummarySubmitRequest
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryRequest
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.ActivityMedicalReviewLabourDeliveryactivityBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.dialog.MedicalReviewSuccessDialogFragment
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment.LabourOrDeliveryFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment.MotherFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment.MotherSummaryFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment.NeonateFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment.NeonateSummaryFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliverySummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.pnc.activity.MotherNeonatePncActivity
import com.medtroniclabs.opensource.ui.medicalreview.prescription.PrescriptionActivity
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.fragment.ReferPatientFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.ReferPatientViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LabourDeliveryBaseActivity : BaseActivity(), View.OnClickListener, AncVisitCallBack,
    OnDialogDismissListener {

    private lateinit var binding: ActivityMedicalReviewLabourDeliveryactivityBinding
    private val viewModel: LabourDeliveryViewModel by viewModels()
    private val viewModelSummary: LabourDeliverySummaryViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val referPatientViewModel: ReferPatientViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityMedicalReviewLabourDeliveryactivityBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.labour_delivery_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation()
            },
            callbackHome = {
                backNavigationToHome()
            }
        )
        initializeView()
        attachObserver()
        initializeListener()
        getCurrentLocation()
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        viewModel.isDirectPnc = intent.getBooleanExtra(DefinedParams.DirectPNCFlow,false)
        viewModel.set(this)
        setAnalytics()
    }

    private fun setAnalytics(){
        UserDetail.eventName= AnalyticsDefinedParams.MedicalReviewCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.MotherNeonateLabourDelivery)
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                val fragmentManager = supportFragmentManager
                val labourDeliveryContainer =
                    fragmentManager.findFragmentById(R.id.labourDeliveryContainer)
                if (labourDeliveryContainer is MotherSummaryFragment) {
                   startActivityWithoutSplashScreen()
                } else {
                    viewModel.setAnalyticsData(
                        UserDetail.startDateTime,
                        eventType = AnalyticsDefinedParams.MotherNeonateLabourDelivery,
                        eventName = UserDetail.eventName,
                        exitReason = AnalyticsDefinedParams.BackButtonClicked,
                        isCompleted = false
                    )
                   onBackPressPopStack()
                }
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
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventType = AnalyticsDefinedParams.MotherNeonateLabourDelivery,
                    eventName = UserDetail.eventName,
                    exitReason = AnalyticsDefinedParams.HomeButtonClicked,
                    isCompleted = false
                )
                startActivityWithoutSplashScreen()
            }
        }
    }



private val onBackPressedCallback: OnBackPressedCallback =
    object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backNavigation()
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
    viewModel.neonateOutComeStateLiveData.observe(this) {
        binding.neonateContainer.apply {
            if (it != null && it.isNotEmpty()) {
                if (it[0].value == MedicalReviewDefinedParams.MaceratedStillBirth || it[0].value == MedicalReviewDefinedParams.FreshStillBirth) {gone() }
                else {
                    visible()
                    replaceFragmentInId<NeonateFragment>(
                        id,
                        tag = NeonateFragment.TAG
                    )
                }
            }else{
                gone()
            }
        }
    }
    viewModel.neonateDataForPncLiveData.observe(this) {
            handleDirectPncNavigation(it)
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
                ) {}
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
                viewModel.lastMensurationDate = resource.data?.pregnancyDetails?.lastMenstrualPeriod
                if (binding.refreshLayout.isRefreshing) {
                    binding.refreshLayout.isRefreshing = false
                }
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
        }
    }


    viewModel.labourDeliveryMetaLiveData.observe(this) { resourceState ->
        when (resourceState.state) {
            ResourceState.LOADING -> {
                showLoading()
            }

            ResourceState.ERROR -> {
                hideLoading()
            }

            ResourceState.SUCCESS -> {
                viewModel.getLabourDeliveryMetaList()
                initializePatientDetailsFragments()
            }
        }
    }

//        viewModel.submitButtonState.observe(this) {
//            binding.btnSubmit.isEnabled = it
//        }

    viewModel.createLabourDeliveryMedicalReviewResponse.observe(this) { resourceState ->
        when (resourceState.state) {
            ResourceState.LOADING -> {
                showLoading()
            }

            ResourceState.ERROR -> {
                hideLoading()

            }

            ResourceState.SUCCESS -> {
                hideLoading()
                resourceState.data?.let {
                    viewModelSummary.getLabourDeliverySummaryDetails(
                        it.motherId,
                        it.patientReference,
                        it.childPatientReference,
                        it.neonateId
                    )
                }
                showLabourDeliverySummary()
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventType = AnalyticsDefinedParams.MotherNeonateLabourDelivery,
                    eventName = UserDetail.eventName,
                )
            }
        }
    }
}
    private fun handleDirectPncNavigation(createLabourDeliveryRequest: CreateLabourDeliveryRequest) {
        val gson = Gson()
        val createLabourDeliveryRequestJson = gson.toJson(createLabourDeliveryRequest)
            val intent = Intent(this, MotherNeonatePncActivity::class.java).apply {
                putExtra(DefinedParams.EncounterId, viewModel.encounterID)
                putExtra(DefinedParams.PatientId, viewModel.patientId)
                putExtra(DefinedParams.ID, getStringExtra(DefinedParams.ID))
                putExtra(DefinedParams.LabourDeliveryData, createLabourDeliveryRequestJson)
                putExtra(DefinedParams.DirectPNCFlow, true)
            }
            startActivity(intent)
    }

private fun initializeView() {
    if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_LABOUR_DELIVERY_LOADED.name))) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModel.getStaticMetaData()
        } else {
            showErrorDialogue(
                getString(R.string.error), getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {}
        }
    } else {
        viewModel.getLabourDeliveryMetaList()
        initializePatientDetailsFragments()
    }
    supportFragmentManager
        .setFragmentResultListener(MedicalReviewDefinedParams.SUMMARY_ITEM, this) { _, _ ->
            enableReferralDoneBtn()
        }
}

private fun enableReferralDoneBtn() {
    binding.btnDone.isEnabled = viewModel.nextFollowupDate != null
}

private fun initializeListener() {
    binding.btnSubmit.safeClickListener(this)
    binding.refreshLayout.setOnRefreshListener {
        swipeRefresh()
    }
    binding.btnRefer.safeClickListener(this)
    binding.btnDone.safeClickListener(this)
    binding.ivPrescription.safeClickListener(this)
    binding.ivInvestigation.safeClickListener(this)
}

private fun swipeRefresh() {
    viewModel.isRefresh = true
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
            isNegativeButtonNeed = false
        ) {
            if (binding.refreshLayout.isRefreshing) {
                binding.refreshLayout.isRefreshing = false
            }
        }
    }
}

private fun initializeFragment() {
    binding.btnSubmit.isEnabled = true
    replaceFragmentInId<LabourOrDeliveryFragment>(
        binding.labourDeliveryContainer.id,
        tag = LabourOrDeliveryFragment.TAG
    )
    if (!viewModel.isDirectPnc) {
        replaceFragmentInId<MotherFragment>(
            binding.motherContainer.id,
            tag = MotherFragment.TAG
        )
        replaceFragmentInId<NeonateFragment>(
            binding.neonateContainer.id,
            tag = NeonateFragment.TAG
        )
    }else{
        binding.btnSubmit.text=getString(R.string.next)
    }

}

override fun onClick(view: View) {
    when (view.id) {
        R.id.btnSubmit -> handleSubmitClick()
        binding.btnRefer.id -> handleReferClick()
        binding.btnDone.id -> handleDoneClick()
        binding.ivPrescription.id -> handlePrescriptionClick()
        binding.ivInvestigation.id -> handleInvestigationClick()
    }
}

private fun handleInvestigationClick() {
    patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
        val intent = Intent(this, InvestigationActivity::class.java)
        intent.putExtra(DefinedParams.PatientId, data.patientId)
        intent.putExtra(DefinedParams.EncounterId, patientViewModel.encounterId)
        getResult.launch(intent)
    }
}

    private fun handleSubmitClick() {
        if (viewModel.isDirectPnc) {
            if (viewModel.neonateOutComeStateLiveData.value?.get(0)?.value == MedicalReviewDefinedParams.FreshStillBirth) {
                handleDirectPncNavigation(viewModel.setLabourDeliveryRequest(patientViewModel.encounterId))
            }else if (viewModel.neonateOutComeStateLiveData.value?.get(0)?.value == MedicalReviewDefinedParams.MaceratedStillBirth &&validateLabourOrDelivery()){
                    withNetworkCheck(connectivityManager, ::submitDetails)
            }
            else {
                if (labourValidation()) {
                    withNetworkCheck(connectivityManager, ::submitDetails)
                }
            }
        } else {
            if (labourValidation()) {
                withNetworkCheck(connectivityManager, ::submitDetails)
            }
        }
    }

private fun submitDetails() {
    viewModel.createLabourDeliveryRequest(patientViewModel.encounterId)
}

private fun handleReferClick() {
    viewModel.createLabourDeliveryMedicalReviewResponse.value?.data?.let {
        ReferPatientFragment.newInstance(
            MedicalReviewTypeEnums.MOTHER_DELIVERY_REVIEW.name,
            it.patientReference,
            it.motherId
        ).show(supportFragmentManager, ReferPatientFragment.TAG)
    }
}

private fun handleDoneClick() {
    patientViewModel.patientDetailsLiveData.value?.data?.let { patientDetails ->
        viewModel.createLabourDeliveryMedicalReviewResponse.value?.data?.let {
            val request = MedicalReviewSummarySubmitRequest(
                id = it.motherId,
                patientId = patientDetails.patientId,
                nextVisitDate = DateUtils.convertDateTimeToDate(
                    viewModel.nextFollowupDate,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                ),
                memberId = patientDetails.memberId,
                patientReference = it.patientReference,
                patientStatus = DefinedParams.Postnatal,
                provenance = ProvanceDto(),
                householdId = patientDetails.houseHoldId,
                villageId = patientDetails.villageId,
                category = MedicalReviewTypeEnums.RMNCH.name,
                encounterType = MedicalReviewTypeEnums.MOTHER_DELIVERY_REVIEW.name,
            )
            viewModel.labourDeliverySummaryCreate(request)
        }
    }
}

private fun handlePrescriptionClick() {
    patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
        Intent(this, PrescriptionActivity::class.java).apply {
            putExtra(DefinedParams.PatientId, data.patientId)
            putExtra(DefinedParams.EncounterId, patientViewModel.encounterId)
            getResult.launch(this)
        }
    }
}

private val getResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(DefinedParams.EncounterId)?.let { value ->
                patientViewModel.encounterId = value
            }
        }
    }

private fun showLabourDeliverySummary() {
    viewModel.isRefresh = true
    binding.bottomNavigationView.visibility = View.INVISIBLE
    binding.referalBottomView.visibility = View.VISIBLE
    supportFragmentManager.beginTransaction()
        .replace(R.id.labourDeliveryContainer, MotherSummaryFragment()).commit()
    supportFragmentManager.beginTransaction()
        .replace(R.id.motherContainer, NeonateSummaryFragment()).commit()
    binding.neonateContainer.isVisible = false
}

private fun labourValidation(): Boolean {
    val isLabourDelivery = validateLabourOrDelivery()
    val isNeonateGender = validateNeonateGender()
    autoScrollError(isLabourDelivery, isNeonateGender)
    return (isLabourDelivery && isNeonateGender)
}

private fun validateLabourOrDelivery(): Boolean {
    var isValid = false
    supportFragmentManager.findFragmentById(R.id.labourDeliveryContainer)
        ?.let { sessionFragment ->
            if (sessionFragment is LabourOrDeliveryFragment) {
                isValid = sessionFragment.validate()
                return isValid
            }
        }
    return isValid
}

private fun autoScrollError(isLabourDelivery: Boolean?, isNeonateGender: Boolean?) {
    val nestedScrollView = binding.nestedScrollView
    val container = when {
        isNeonateGender == false && isLabourDelivery == true -> binding.neonateContainer
        isNeonateGender == true && isLabourDelivery == false -> binding.labourDeliveryContainer
        else -> binding.labourDeliveryContainer
    }
    val y = container.top
    nestedScrollView.smoothScrollTo(0, y)
}

private fun validateNeonateGender(): Boolean {
    var isValid = false
    supportFragmentManager.findFragmentById(R.id.neonateContainer)
        ?.let { sessionFragment ->
            if (sessionFragment is NeonateFragment) {
                isValid = sessionFragment.validate()
                return isValid
            }
        }
    return isValid
}

private fun getCurrentLocation() {
    val locationManager = SpiceLocationManager(this)
    locationManager.getCurrentLocation {
        viewModel.lastLocation = it
    }
}

private fun initializePatientDetailsFragments() {
    val patientId = intent.getStringExtra(DefinedParams.PatientId)
    val fragment = PatientInfoFragment.newInstance(patientId).apply {
        setDataCallback(this@LabourDeliveryBaseActivity)
    }
    supportFragmentManager.beginTransaction()
        .replace(R.id.patientDetailFragment, fragment)
        .commit()
}

override fun onDataLoaded(detailPatient: PatientListRespModel) {
    viewModel.patientDetailModel = detailPatient
    if (!viewModel.isRefresh) {
        initializeFragment()
    }
}

private fun onBackPressPopStack() {
    this@LabourDeliveryBaseActivity.finish()
}

override fun onDialogDismissListener(isFinish: Boolean) {
    startActivityWithoutSplashScreen()
}
}