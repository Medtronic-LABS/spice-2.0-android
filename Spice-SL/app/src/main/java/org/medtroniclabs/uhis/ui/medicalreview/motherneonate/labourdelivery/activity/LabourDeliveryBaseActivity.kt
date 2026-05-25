package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.core.view.isVisible
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.data.MedicalReviewSummarySubmitRequest
import org.medtroniclabs.uhis.data.model.CreateLabourDeliveryRequest
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.ActivityMedicalReviewLabourDeliveryactivityBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dialog.MedicalReviewSuccessDialogFragment
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment.LabourOrDeliveryFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment.MotherFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment.MotherSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment.NeonateFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.fragment.NeonateSummaryFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliverySummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.pnc.activity.MotherNeonatePncActivity
import org.medtroniclabs.uhis.ui.medicalreview.prescription.PrescriptionActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.fragment.ReferPatientFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

@AndroidEntryPoint
class LabourDeliveryBaseActivity :
    BaseActivity(),
    View.OnClickListener,
    AncVisitCallBack,
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
            },
        )
        initializeView()
        attachObserver()
        initializeListener()
        viewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        viewModel.isDirectPnc = intent.getBooleanExtra(DefinedParams.DIRECT_PNC_FLOW, false)
        if (viewModel.isDirectPnc) {
            viewModel.setUserJourney(AnalyticsDefinedParams.MOTHERDIRECTPNC)
        } else {
            viewModel.setUserJourney(AnalyticsDefinedParams.MotherNeonateLabourDelivery)
        }
        viewModel.set(this)
        setAnalytics()
    }

    private fun setAnalytics() {
        UserDetail.eventName = AnalyticsDefinedParams.MedicalReviewCreation
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
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
                        isCompleted = false,
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
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventType = AnalyticsDefinedParams.MotherNeonateLabourDelivery,
                    eventName = UserDetail.eventName,
                    exitReason = AnalyticsDefinedParams.HomeButtonClicked,
                    isCompleted = false,
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
                        MedicalReviewSuccessDialogFragment.TAG,
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
                    if (it[0].value == MedicalReviewDefinedParams.StillBirth || it[0].value == MedicalReviewDefinedParams.Miscarriage) {
                        gone()
                    } else {
                        visible()
                        replaceFragmentInId<NeonateFragment>(
                            id,
                            tag = NeonateFragment.TAG,
                        )
                    }
                } else {
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
                        positiveButtonName = getString(R.string.ok),
                    ) {}
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
                        positiveButtonName = getString(R.string.ok),
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
                            it.neonateId,
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
            putExtra(DefinedParams.ENCOUNTER_ID, viewModel.encounterID)
            putExtra(DefinedParams.PatientId, viewModel.patientId)
            putExtra(DefinedParams.ID, getStringExtra(DefinedParams.ID))
            putExtra(DefinedParams.LabourDeliveryData, createLabourDeliveryRequestJson)
            putExtra(DefinedParams.DIRECT_PNC_FLOW, true)
        }
        startActivity(intent)
    }

    private fun initializeView() {
        if (!(SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_LABOUR_DELIVERY_LOADED.name))) {
            if (connectivityManager.isNetworkAvailable()) {
                viewModel.getStaticMetaData()
            } else {
                showErrorDialogue(
                    getString(R.string.error),
                    getString(R.string.no_internet_error),
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
                }
            }
        }
    }

    private fun initializeFragment() {
        binding.btnSubmit.isEnabled = true
        replaceFragmentInId<LabourOrDeliveryFragment>(
            binding.labourDeliveryContainer.id,
            tag = LabourOrDeliveryFragment.TAG,
        )
        if (!viewModel.isDirectPnc) {
            replaceFragmentInId<MotherFragment>(
                binding.motherContainer.id,
                tag = MotherFragment.TAG,
            )
            replaceFragmentInId<NeonateFragment>(
                binding.neonateContainer.id,
                tag = NeonateFragment.TAG,
            )
        } else {
            binding.btnSubmit.text = getString(R.string.next)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnSubmit -> withLocationCheck(::handleSubmitClick)
            binding.btnRefer.id -> handleReferClick()
            binding.btnDone.id -> withLocationCheck(::handleDoneClick)
            binding.ivPrescription.id -> handlePrescriptionClick()
            binding.ivInvestigation.id -> handleInvestigationClick()
        }
    }

    private fun handleInvestigationClick() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
            val intent = Intent(this, InvestigationActivity::class.java)
            intent.putExtra(DefinedParams.PatientId, data.patientId)
            intent.putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
            getResult.launch(intent)
        }
    }

    private fun handleSubmitClick() {
        if (viewModel.isDirectPnc) {
            if (viewModel.neonateOutComeStateLiveData.value
                    ?.get(0)
                    ?.value == MedicalReviewDefinedParams.Miscarriage
            ) {
                handleDirectPncNavigation(viewModel.setLabourDeliveryRequest(patientViewModel.encounterId))
            } else if (viewModel.neonateOutComeStateLiveData.value
                    ?.get(0)
                    ?.value == MedicalReviewDefinedParams.StillBirth &&
                validateLabourOrDelivery()
            ) {
                withNetworkCheck(connectivityManager, ::submitDetails)
            } else {
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
            ReferPatientFragment
                .newInstance(
                    MedicalReviewTypeEnums.MOTHER_DELIVERY_REVIEW.name,
                    it.patientReference,
                    it.motherId,
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
                        inUTC = true,
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
                putExtra(DefinedParams.ENCOUNTER_ID, patientViewModel.encounterId)
                getResult.launch(this)
            }
        }
    }

    private val getResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(DefinedParams.ENCOUNTER_ID)?.let { value ->
                    patientViewModel.encounterId = value
                }
            }
        }

    private fun showLabourDeliverySummary() {
        viewModel.isRefresh = true
        binding.bottomNavigationView.visibility = View.INVISIBLE
        binding.referalBottomView.visibility = View.VISIBLE
        patientViewModel.setUserJourney(AnalyticsDefinedParams.MOTHERPNCSUMMARY)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.labourDeliveryContainer, MotherSummaryFragment())
            .commit()
        if (viewModel.neonateOutcome == MedicalReviewDefinedParams.MaceratedStillBirth ||
            viewModel.neonateOutcome == MedicalReviewDefinedParams.FreshStillBirth
        ) {
            binding.clinicalNameGroup.visible()
            binding.tvClinicalName.text = getString(
                R.string.firstname_lastname,
                SecuredPreference.getUserDetails()?.firstName,
                SecuredPreference.getUserDetails()?.lastName,
            )
            binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
                DateUtils.getTodayDateDDMMYYYY(),
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_ddMMyyyy,
            )
            binding.motherContainer.gone()
        } else {
            binding.motherContainer.visible()
            binding.clinicalNameGroup.gone()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.motherContainer, NeonateSummaryFragment())
                .commit()
        }
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
        supportFragmentManager
            .findFragmentById(R.id.labourDeliveryContainer)
            ?.let { sessionFragment ->
                if (sessionFragment is LabourOrDeliveryFragment) {
                    isValid = sessionFragment.validate()
                    return isValid
                }
            }
        return isValid
    }

    private fun autoScrollError(
        isLabourDelivery: Boolean?,
        isNeonateGender: Boolean?,
    ) {
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
        supportFragmentManager
            .findFragmentById(R.id.neonateContainer)
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
        supportFragmentManager
            .beginTransaction()
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

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }
}
