package com.medtroniclabs.opensource.ncd.medicalreview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setError
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.ORIGIN
import com.medtroniclabs.opensource.common.GeneralErrorDialog
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.ActivityNcdmedicalReviewCmractivityBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.extension.safePopupMenuClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.EncounterReference
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDTreatmentPlanDialog
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDAssessmentHistoryFragment
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.home.AssessmentToolsActivity
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDCounselingActivity
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDLifestyleActivity
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDLifestyleDialog
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDMedicalReviewHistoryFragment
import com.medtroniclabs.opensource.ncd.data.BadgeNotificationModel
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDInvestigationHistoryFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDLifeStyleStatusFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDPrescriptionHistoryFragment
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.activity.NCDPrescriptionActivity
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewCMRViewModel
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.patientDelete.NCDDeleteConfirmationDialog
import com.medtroniclabs.opensource.ui.patientDelete.viewModel.NCDPatientDeleteViewModel
import com.medtroniclabs.opensource.ui.patientEdit.NCDPatientEditActivity
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferValidate
import com.medtroniclabs.opensource.ui.patientTransfer.dialog.NCDTransferArchiveDialog
import com.medtroniclabs.opensource.ui.patientTransfer.viewModel.NCDPatientTransferViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDMedicalReviewCMRActivity : BaseActivity(), View.OnClickListener, AncVisitCallBack {
    private lateinit var binding: ActivityNcdmedicalReviewCmractivityBinding
    private val viewModel: NCDMedicalReviewViewModel by viewModels()
    private val cmrViewModel: NCDMedicalReviewCMRViewModel by viewModels()
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()
    private val patientDeleteViewModel: NCDPatientDeleteViewModel by viewModels()
    private val patientTransferViewModel: NCDPatientTransferViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNcdmedicalReviewCmractivityBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
        )
    }

    private fun showHideVerticalIcon(visibility: Boolean) {
        showVerticalMoreIcon(visibility) {
            onMoreIconClicked(it)
        }
    }

    private fun onMoreIconClicked(view: View) {
        val popupMenu = PopupMenu(this@NCDMedicalReviewCMRActivity, view)
        popupMenu.menuInflater.inflate(R.menu.ncd_menu_patient_edit, popupMenu.menu)
        popupMenu.menu.findItem(R.id.patient_delete).isVisible =
            CommonUtils.isNonCommunity() && !CommonUtils.isChp()
        popupMenu.menu.findItem(R.id.schedule).isVisible =
            CommonUtils.canShowScheduleMenu()
        popupMenu.menu.findItem(R.id.transfer_patient).isVisible =
            CommonUtils.isNonCommunity() && !CommonUtils.isNURSE() && !CommonUtils.isChp()
        popupMenu.safePopupMenuClickListener(object :
            android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                onPatientEditMenuItemClick(menuItem.itemId)
                return true
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    private fun onPatientEditMenuItemClick(itemId: Int) {
        when (itemId) {
            R.id.patient_delete -> {
                patientDeleteCreate()
            }

            R.id.patient_edit -> {
                val intent =
                    Intent(this@NCDMedicalReviewCMRActivity, NCDPatientEditActivity::class.java)
                intent.putExtra(NCDMRUtil.PATIENT_REFERENCE, patientDetailViewModel.getPatientId())
                intent.putExtra(
                    NCDMRUtil.MEMBER_REFERENCE,
                    patientDetailViewModel.getPatientFHIRId()
                )
                intent.putExtra(DefinedParams.ORIGIN, patientDetailViewModel.origin)
                startActivity(intent)
            }

            R.id.transfer_patient -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    data.patientId?.let {
                        val request = NCDPatientTransferValidate(
                            patientReference = it
                        )
                        patientTransferViewModel.validatePatientTransfer(request)
                    }
                }
            }
        }
    }

    private fun patientDeleteCreate() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { model ->
            val deleteConfirmationDialog = NCDDeleteConfirmationDialog.newInstance(
                getString(R.string.alert),
                getString(
                    R.string.patient_delete_confirmation,
                    model.firstName,
                    model.lastName
                ),
                { _, reason, otherReason ->
                    val request = NCDPatientRemoveRequest(
                        patientId = model.patientId.toString(),
                        reason = reason,
                        otherReason = otherReason,
                        memberId = model.id
                    )
                    patientDeleteViewModel.ncdPatientRemove(request)
                },
                this,
                true,
                okayButton = getString(R.string.yes),
                cancelButton = getString(R.string.no)
            )
            deleteConfirmationDialog.show(
                supportFragmentManager,
                NCDDeleteConfirmationDialog.TAG
            )
        }
    }

    private fun initializeStaticDataSave() {
        if (NCDMRUtil.isNCDMRMetaLoaded())
            initView()
        else {
            withNetworkAvailability(online = {
                viewModel.getStaticMetaData()
            }, offline = {
                onBackPressPopStack()
            })
        }
    }

    fun attachObservers() {
        viewModel.ncdMedicalReviewStaticLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    initView()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialog()
                }
            }
        }
        patientDetailViewModel.patientDetailsLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    if (binding.refreshLayout.isRefreshing) {
                        binding.refreshLayout.isRefreshing = false
                    }
                    badgeNotifications()
                    showHideVerticalIcon(CommonUtils.isNonCommunity() && !resourceState.data?.programId.isNullOrBlank())
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    if (binding.refreshLayout.isRefreshing) {
                        binding.refreshLayout.isRefreshing = false
                    }
                    showError()
                }
            }
        }
        viewModel.getBadgeNotificationLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        updateCounts(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        patientDeleteViewModel.patientRemoveResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    GeneralSuccessDialog.newInstance(
                        title = getString(R.string.delete),
                        message = getString(R.string.patient_delete_message),
                        okayButton = getString(R.string.done)
                    ) {  redirectToHome() }.show(supportFragmentManager, GeneralSuccessDialog.TAG)
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(getString(R.string.error), it, false) {}
                    }
                }
            }
        }

        patientTransferViewModel.validateTransferResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        val title = getString(R.string.patient_transfer)
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                title,
                                callback = {
                                    patientTransferViewModel.validateTransferResponse.setError(message = null)
                                    val dialog = supportFragmentManager.findFragmentByTag(GeneralErrorDialog.TAG) as? GeneralErrorDialog
                                    dialog?.dismiss()
                                },
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(message, true)
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(GeneralErrorDialog.TAG)
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    NCDTransferArchiveDialog.newInstance().show(
                        supportFragmentManager,
                        NCDTransferArchiveDialog.TAG
                    )
                }
            }
        }
    }

    private fun updateCounts(it: BadgeNotificationModel) {
        binding.btnLayout.apply {
            ivLSBadgeCount.text = it.nutritionLifestyleReviewedCount.toString()
            ivIBatchCount.text = it.nonReviewedTestCount.toString()
            ivPBatchCount.text = it.prescriptionDaysCompletedCount.toString()
            ivPsycBadgeCount.text = it.psychologicalCount.toString()

            ivLSBadgeCount.setVisible(it.nutritionLifestyleReviewedCount > 0)
            ivIBatchCount.setVisible(it.nonReviewedTestCount > 0)
            ivPBatchCount.setVisible(it.prescriptionDaysCompletedCount > 0)
            ivPsycBadgeCount.setVisible(it.psychologicalCount > 0)
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
        hideLoading()
    }

    private fun showError(isActivityClosed: Boolean = false) {
        showErrorDialogue(
            title = getString(R.string.alert),
            message = getString(R.string.something_went_wrong_try_later),
            positiveButtonName = getString(R.string.ok),
        ) { isPositiveResult ->
            if (isPositiveResult && isActivityClosed) {
                onBackPressPopStack()
            }
        }
    }

    private fun initView() {
        // FOR Nurse ,Visit id(is always null) for other role always have value
        if (CommonUtils.isNutritionist())
            binding.btnMedicalReview.setImageDrawable(getDrawable(R.drawable.start_session))
        else
            binding.btnLayout.root.setVisible(!getEncounterReference().isNullOrBlank())
        binding.btnMedicalReview.setVisible(!getEncounterReference().isNullOrBlank())
        val isPsycho = CommonUtils.isPsychologicalFlowEnabled()
        binding.btnLayout.apply {
            clPsycMenu.setVisible(isPsycho)
            viewLine.setVisible(isPsycho)
        }
        binding.btnLayout.clBtn.gone()
        binding.btnMedicalReview.safeClickListener(this)
        binding.btnLayout.ivTreatmentPlan.safeClickListener(this)
        binding.btnLayout.ivInvestigation.safeClickListener(this)
        binding.btnLayout.ivLifestyle.safeClickListener(this)
        binding.btnLayout.clPsycMenu.safeClickListener(this)
        binding.btnLayout.ivPrescriptionImgView.safeClickListener(this)
        withNetworkAvailability(online = {
            initializePatientDetails()
        })
        binding.refreshLayout.setOnRefreshListener {
            withNetworkAvailability(online = {
                swipeRefresh()
            }, offline = {
                if (binding.refreshLayout.isRefreshing) {
                    binding.refreshLayout.isRefreshing = false
                }
            })
        }
    }

    private fun badgeNotifications() {
        viewModel.getBadgeNotifications(BadgeNotificationModel(patientReference = patientDetailViewModel.getPatientId()))
    }

     fun swipeRefresh() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
            details.id?.let { id ->
                patientDetailViewModel.getPatients(id, origin = patientDetailViewModel.origin)
            }
        }
    }

    private fun initializePatientDetails() {
        patientDetailViewModel.origin = intent.extras?.getString(DefinedParams.ORIGIN)
        val fragment = PatientInfoFragment.newInstanceForNCD(getFhirId(), getOrigin() ?: "")
        patientDetailViewModel.isCmr = true
        fragment.setDataCallback(this)
        replaceFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            fragment
        )
        hideLoading()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressPopStack()
            }
        }

    private fun onBackPressPopStack() {
        this@NCDMedicalReviewCMRActivity.finish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnMedicalReview.id -> {
                if (CommonUtils.isNutritionist()) {
                    patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
                        val ncdLifestyleDialog = NCDLifestyleDialog.newInstance(
                            patientDetailViewModel.getPatientId(),
                            patientDetailViewModel.getPatientFHIRId(),
                            getEncounterReference()
                        ) {}
                        ncdLifestyleDialog.show(supportFragmentManager, NCDLifestyleDialog.TAG)
                    }
                } else {
                    viewModel.setAnalyticsData(
                        UserDetail.startDateTime,
                        eventName = AnalyticsDefinedParams.NCDCMRMedicalReviewCreation,
                        isCompleted = true
                    )
                    val intent = Intent(this, AssessmentToolsActivity::class.java)
                    intent.putExtra(DefinedParams.FhirId, getFhirId())
                    intent.putExtra(DefinedParams.PatientId, getPatientId())
                    intent.putExtra(DefinedParams.ORIGIN, getOrigin())
                    intent.putExtra(DefinedParams.Gender, getGender())
                    intent.putExtra(EncounterReference, getEncounterReference())
                    startActivity(intent)
                }
            }

            binding.btnLayout.ivTreatmentPlan.id -> {
                val dialog =
                    NCDTreatmentPlanDialog.newInstance(
                        getPatientId(),
                        getFhirId(),
                        showCHO = showCHO()
                    ) { isPositiveResult, message ->
                        if (isPositiveResult)
                            showSuccessDialogue(
                                title = getString(R.string.treatment_plan),
                                message = message
                            ) {}
                        else
                            showErrorDialogue(
                                title = getString(R.string.error),
                                message = message,
                                positiveButtonName = getString(R.string.ok),
                            ) {}
                    }
                dialog.show(supportFragmentManager, NCDTreatmentPlanDialog.TAG)
            }

            binding.btnLayout.ivInvestigation.id -> {
                patientDetailViewModel.patientDetailsLiveData.value?.data?.let { data ->
                    val intent = Intent(this, InvestigationActivity::class.java)
                    intent.putExtra(DefinedParams.PatientId, data.id)
                    intent.putExtra(EncounterReference, getEncounterReference())
                    intent.putExtra(DefinedParams.PatientReference, data.patientId)
                    intent.putExtra(DefinedParams.ORIGIN, getMenuOrigin())
                    getResult.launch(intent)
                }
            }
            binding.btnLayout.ivLifestyle.id -> navigateUser(
                Intent(
                    this,
                    NCDLifestyleActivity::class.java
                )
            )

            binding.btnLayout.clPsycMenu.id -> navigateUser(
                Intent(
                    this,
                    NCDCounselingActivity::class.java
                )
            )

            binding.btnLayout.ivPrescriptionImgView.id -> {
                withNetworkAvailability(online = {
                    val intent = Intent(this, NCDPrescriptionActivity::class.java)
                    intent.putExtra(ORIGIN, DefinedParams.MedicalReview)
                    intent.putExtra(DefinedParams.EnrollmentType, patientDetailViewModel.getEnrollmentType())
                    intent.putExtra(Screening.identityValue, patientDetailViewModel.getIdentityValue())
                    intent.putExtra(DefinedParams.PatientId, patientDetailViewModel.getPatientId())
                    intent.putExtra(DefinedParams.id, patientDetailViewModel.getPatientFHIRId())
                    intent.putExtra(DefinedParams.PatientVisitId, getEncounterReference())
                    getResult.launch(intent)
                })
            }
        }
    }

    private fun navigateUser(intent: Intent) {
        val bundle = Bundle()
        bundle.putString(NCDMRUtil.PATIENT_REFERENCE, patientDetailViewModel.getPatientId())
        bundle.putString(NCDMRUtil.MEMBER_REFERENCE, patientDetailViewModel.getPatientFHIRId())
        bundle.putString(NCDMRUtil.VISIT_ID, getEncounterReference())
        intent.putExtras(bundle)
        getResult.launch(intent)
    }

    private fun getMenuOrigin(): String? {
        return intent.getStringExtra(DefinedParams.ORIGIN)
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                swipeRefresh()
            }
        }

    private fun getFhirId(): String? {
        return intent.getStringExtra(DefinedParams.FhirId)
    }

    private fun getPatientId(): String? {
        return intent.getStringExtra(DefinedParams.PatientId)
    }

    private fun getOrigin(): String? {
        return intent.getStringExtra(DefinedParams.ORIGIN)
    }

    private fun getGender(): String? {
        return intent.getStringExtra(DefinedParams.Gender)
    }

    private fun getEncounterReference(): String? {
        return intent.getStringExtra(EncounterReference)
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        val bpBundle = Bundle().apply {
            putString(NCDMRUtil.TAG, NCDMRUtil.BP_TAG)
        }

        val bgBundle = Bundle().apply {
            putString(NCDMRUtil.TAG, NCDMRUtil.BG_TAG)
        }

        // After patient details loaded
        binding.apply {
            patientBPHistory.visible()
            patientBGHistory.visible()
        }

        replaceFragmentInId<NCDAssessmentHistoryFragment>(binding.patientBPHistory.id, bpBundle)
        replaceFragmentInId<NCDAssessmentHistoryFragment>(binding.patientBGHistory.id, bgBundle)
        val medicalReview = NCDMedicalReviewHistoryFragment.newInstance(details.patientId)
        replaceFragment(
            R.id.medicalReviewHistory,
            NCDMedicalReviewHistoryFragment.TAG,
            medicalReview
        )
        val prescription = NCDPrescriptionHistoryFragment.newInstance(details.patientId)
        replaceFragment(
            R.id.prescriptionHistory,
            NCDPrescriptionHistoryFragment.TAG,
            prescription
        )
        val investigation = NCDInvestigationHistoryFragment.newInstance(details.patientId)
        replaceFragment(
            R.id.investigationHistory,
            NCDInvestigationHistoryFragment.TAG,
            investigation
        )
        val lifeStyle = NCDLifeStyleStatusFragment.newInstance()
        replaceFragment(
            R.id.lifestyleStatusHistory,
            NCDLifeStyleStatusFragment.TAG,
            lifeStyle
        )
    }
    override fun onResume() {
        super.onResume()
        initializeStaticDataSave()
        attachObservers()
    }

    private fun showCHO(): Boolean {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let {
            return SecuredPreference.isAncEnabled() &&
                    it.gender.equals(Screening.Female, true) &&
                    it.isPregnant == true &&
                    it.pregnancyDetails?.isDangerSymptoms == true &&
                    CommonUtils.gestationalWeekLimitCheck(it.pregnancyDetails.lastMenstrualPeriod)
        }
        return false
    }
}