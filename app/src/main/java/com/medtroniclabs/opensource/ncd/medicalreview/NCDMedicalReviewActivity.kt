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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.MenuId
import com.medtroniclabs.opensource.common.DefinedParams.ORIGIN
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.ActivityNcdMrBaseBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.extension.safePopupMenuClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.assessment.ui.AssessmentReadingActivity
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDCounselingActivity
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDLifestyleActivity
import com.medtroniclabs.opensource.ncd.data.Answer
import com.medtroniclabs.opensource.ncd.data.BadgeNotificationModel
import com.medtroniclabs.opensource.ncd.data.Chip
import com.medtroniclabs.opensource.ncd.data.ContinuousMedicalReview
import com.medtroniclabs.opensource.ncd.data.CurrentMedications
import com.medtroniclabs.opensource.ncd.data.InitialMedicalReview
import com.medtroniclabs.opensource.ncd.data.MedicalReviewRequestResponse
import com.medtroniclabs.opensource.ncd.data.NCDMRSummaryRequestResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferValidate
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.EncounterReference
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MATERNAL_HEALTH
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENTAL_HEALTH
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENU_ID
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENU_Name
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.NCD
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.getConfirmDiagnoses
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.getTypeForDiagnoses
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDDiagnosisDialogFragment
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDMRAlertDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDMentalHealthFragment
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPatientHistoryDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPregnancyDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDTreatmentPlanDialog
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDChiefComplaintsFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDClinicalNotesFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDComorbiditiesFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDComplicationsFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDCurrentMedicationFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDLifestyleAssessmentFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDMedicalReviewDiagnosisCardFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDMedicalReviewSummaryFragment
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDObstetricExaminationFragment
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.activity.NCDPrescriptionActivity
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDChiefComplaintsViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDClinicalNotesViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDComorbiditiesViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDComplicationsViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDCurrentMedicationViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDLifestyleAssessmentViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewDiagnosisCardViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewSummaryViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMentalHealthViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDObstetricExaminationViewModel
import com.medtroniclabs.opensource.ncd.registration.ui.RegistrationActivity
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.dialog.MedicalReviewSuccessDialogFragment
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.investigation.InvestigationActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.patientDelete.NCDDeleteConfirmationDialog
import com.medtroniclabs.opensource.ui.patientDelete.viewModel.NCDPatientDeleteViewModel
import com.medtroniclabs.opensource.ui.patientEdit.NCDPatientEditActivity
import com.medtroniclabs.opensource.ui.patientTransfer.dialog.NCDTransferArchiveDialog
import com.medtroniclabs.opensource.ui.patientTransfer.viewModel.NCDPatientTransferViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDMedicalReviewActivity : BaseActivity(), View.OnClickListener, AncVisitCallBack,
    NCDDialogDismissListener, NCDMRAlertDialog.DialogCallback, OnDialogDismissListener {

    private val viewModel: NCDMedicalReviewViewModel by viewModels()
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()
    private val comorbiditiesViewModel: NCDComorbiditiesViewModel by viewModels()
    private val currentMedicationViewModel: NCDCurrentMedicationViewModel by viewModels()
    private val complicationsViewModel: NCDComplicationsViewModel by viewModels()
    private val lifestyleAssessmentViewModel: NCDLifestyleAssessmentViewModel by viewModels()
    private val obstetricExaminationViewModel: NCDObstetricExaminationViewModel by viewModels()
    private val clinicalNotesViewModel: NCDClinicalNotesViewModel by viewModels()
    private val chiefComplaintsViewModel: NCDChiefComplaintsViewModel by viewModels()
    private val summaryViewModel: NCDMedicalReviewSummaryViewModel by viewModels()
    private val medicalReviewDiagnosisCardViewModel: NCDMedicalReviewDiagnosisCardViewModel by viewModels()
    private val patientDeleteViewModel: NCDPatientDeleteViewModel by viewModels()
    private val patientTransferViewModel: NCDPatientTransferViewModel by viewModels()
    private val mentalHealthViewModel: NCDMentalHealthViewModel by viewModels()
    private lateinit var binding: ActivityNcdMrBaseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNcdMrBaseBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation()
            },
            callbackHome = {
                showErrorDialogHome()
            }
        )
        initializeStaticDataSave()
        setListeners()
        attachObservers()
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
    }

    private fun showHideVerticalIcon(visibility: Boolean) {
        showVerticalMoreIcon(visibility) {
            onMoreIconClicked(it)
        }
    }

    private fun onMoreIconClicked(view: View) {
        val popupMenu = PopupMenu(this@NCDMedicalReviewActivity, view)
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
                    Intent(this@NCDMedicalReviewActivity, NCDPatientEditActivity::class.java)
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

    fun attachObservers() {
        viewModel.ncdPatientDiagnosisStatus.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    showInitialDialogs()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
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

        viewModel.createMedicalReview.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    // navigate to summary
                    resourceState.data?.let {
                        loadSummary()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showError()
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
                    checkPatientDetails(resourceState.data?.patientId)
                    badgeNotifications()
                    showHideVerticalIcon(CommonUtils.isNonCommunity() && !resourceState.data?.programId.isNullOrBlank())
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    if (binding.refreshLayout.isRefreshing) {
                        binding.refreshLayout.isRefreshing = false
                    }
                    showError(true)
                }
            }
        }
        summaryViewModel.createNCDMRSummaryCreate.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    summarySuccessDialog()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    if (binding.refreshLayout.isRefreshing) {
                        binding.refreshLayout.isRefreshing = false
                    }
                    showError(true)
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
                    ) { redirectToHome() }.show(supportFragmentManager, GeneralSuccessDialog.TAG)
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
                        showErrorDialogue(title, message, isNegativeButtonNeed = false) {}
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

    private fun showInitialDialogs() {
        when (getMenuId()) {
            NCD.lowercase() -> showNcdPatientHistoryDialog()
            MENTAL_HEALTH.lowercase() -> showMentalHealthPatientHistoryDialog()
            DefinedParams.PregnancyANC.lowercase() -> showPregnancyDialog()
        }
    }

    private fun showNcdPatientHistoryDialog() {
        if (!patientDetailViewModel.getNCDInitialMedicalReview())
            patientDetailViewModel.getPatientFHIRId()?.let { fhirId ->
                val fragment = supportFragmentManager.findFragmentByTag(NCDPatientHistoryDialog.TAG)
                if (fragment == null) {
                    NCDPatientHistoryDialog.newInstance(
                        getEncounterReference(),
                        patientDetailViewModel.getPatientId(),
                        fhirId,
                        isFemale = patientDetailViewModel.getGenderIsFemale(),
                        patientDetailViewModel.isPregnant()
                    ).apply {
                        listener = this@NCDMedicalReviewActivity
                    }.show(supportFragmentManager, NCDPatientHistoryDialog.TAG)
                }
            }
    }

    private fun checkPatientDetails(patientReference: String?) {
        if (viewModel.isPatientStatusCompleted)
            return
        else {
            viewModel.isPatientStatusCompleted = true

            if (patientReference.isNullOrBlank())
                showInitialDialogs()
            else
                viewModel.ncdPatientDiagnosisStatus(HashMap<String, Any>().apply {
                    put(
                        DefinedParams.PatientReference,
                        patientReference
                    )
                })
        }
    }

    private fun showSuccessDialog(map: HashMap<String, Any>) {
        if (map.containsKey(NCDMRUtil.message)) {
            val message = map[NCDMRUtil.message]
            if (message is String) {
                GeneralSuccessDialog.newInstance(
                    title = "Medical Review",
                    message = message,
                    okayButton = getString(R.string.done)
                ) { }.show(supportFragmentManager, GeneralSuccessDialog.TAG)
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

    private fun badgeNotifications() {
        viewModel.getBadgeNotifications(BadgeNotificationModel(patientReference = patientDetailViewModel.getPatientId()))
    }

    private fun summarySuccessDialog() {
        val dialog =
            supportFragmentManager.findFragmentByTag(MedicalReviewSuccessDialogFragment.TAG)
        if (dialog == null) {
            MedicalReviewSuccessDialogFragment.newInstance(patientDetailViewModel.isPatientEnrolled())
                .show(
                    supportFragmentManager,
                    MedicalReviewSuccessDialogFragment.TAG
                )
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        if (isFinish) {
            // enrolled click in success dialog
            navigateToEnrolled()
        } else {
            startActivityWithoutSplashScreen()
        }
    }

    private fun navigateToEnrolled() {
        val intent = Intent(this, RegistrationActivity::class.java).apply {
            putExtra(DefinedParams.FhirId, patientDetailViewModel.getPatientFHIRId())
            putExtra(DefinedParams.PatientId, patientDetailViewModel.getPatientId())
            putExtra(ORIGIN, MenuConstants.REGISTRATION.lowercase())
            putExtra(DefinedParams.Gender, patientDetailViewModel.getGender())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
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

    private fun loadSummary() {
        binding.apply {
            comorbiditiesContainer.gone()
            medicalDiagnosisContainer.gone()
            complicationsContainer.gone()
            lifestyleAssessmentContainer.gone()
            chiefComplaintsContainer.gone()
            clinicalNotesContainer.gone()
            obstetricExaminationContainer.visible()
            btnLayout.btnNext.text = getString(R.string.submit)
            btnLayout.btnNext.isAllCaps = true
        }
        replaceFragment(
            R.id.obstetricExaminationContainer,
            NCDMedicalReviewSummaryFragment.TAG,
            NCDMedicalReviewSummaryFragment.newInstance(getEncounterReference(), getMenuId(), getPatientId())
        )
    }

    private fun onBackPressPopStack() {
        this@NCDMedicalReviewActivity.finish()
    }

    private fun initializeStaticDataSave() {
        if (NCDMRUtil.isNCDMRMetaLoaded())
            initView() else {
            withNetworkAvailability(online = {
                viewModel.getStaticMetaData()
            }, offline = {
                onBackPressPopStack()
            })
        }
    }

    fun initView() {
        val isPsycho = CommonUtils.isPsychologicalFlowEnabled()
        binding.btnLayout.apply {
            clPsycMenu.setVisible(isPsycho)
            viewLine.setVisible(isPsycho)
        }
        initializePatientDetails()
    }

    private fun setListeners() {
        binding.btnLayout.btnNext.safeClickListener(this)
        binding.btnLayout.ivPrescriptionImgView.safeClickListener(this)
        binding.btnLayout.ivTreatmentPlan.safeClickListener(this)
        binding.btnLayout.ivInvestigation.safeClickListener(this)
        binding.btnLayout.ivLifestyle.safeClickListener(this)
        binding.btnLayout.clPsycMenu.safeClickListener(this)
    }

    private fun getPatientId(): String? {
        return intent.getStringExtra(DefinedParams.FhirId)
    }

    private fun getEncounterReference(): String? {
        return intent.getStringExtra(EncounterReference)
    }

    private fun getMenuOrigin(): String? {
        return intent.getStringExtra(ORIGIN)
    }

    private fun getMenuId(): String? {
        return when (intent.getStringExtra(MENU_ID)?.lowercase()) {
            NCD.lowercase() -> {
                return NCD.lowercase()
            }

            MATERNAL_HEALTH.lowercase() -> {
                return DefinedParams.PregnancyANC.lowercase()
            }

            MENTAL_HEALTH.lowercase() -> {
                return MENTAL_HEALTH.lowercase()
            }

            else -> {
                null
            }
        }
    }


    private fun initializePatientDetails() {
        patientDetailViewModel.origin = intent.extras?.getString(ORIGIN)
        patientDetailViewModel.mrMenuId = getMenuId()
        val fragment = PatientInfoFragment.newInstanceForNCD(getPatientId(), getMenuOrigin() ?: "")
        fragment.setDataCallback(this)
        addOrReuseFragment(
            R.id.patientDetailFragment,
            PatientInfoFragment.TAG,
            fragment
        )
        hideLoading()
    }

    private fun initializeFragments() {
        replaceFragment(
            R.id.medicalDiagnosisContainer,
            NCDMedicalReviewDiagnosisCardFragment.TAG,
            NCDMedicalReviewDiagnosisCardFragment.newInstance(
                getEncounterReference(),
                patientDetailViewModel.getNCDInitialMedicalReview(),
                patientDetailViewModel.getGenderIsFemale(),
                getMenuId(),
                intent.getStringExtra(MENU_ID)
            )
        )
        hideLoading()
    }

    private fun reloadFragment(container: Int, tag: String, fragment: Fragment) {
        if (patientDetailViewModel.forceRefresh)
            replaceFragment(container, tag, fragment)
        else
            addOrReuseFragment(container, tag, fragment)
    }

    private fun loadFragment(isInitialMR: Boolean) {
        showLoading()
        if (isInitialMR) {
            binding.apply {
                comorbiditiesContainer.visible()
                complicationsContainer.visible()
                lifestyleAssessmentContainer.visible()
                chiefComplaintsContainer.gone()
                clinicalNotesContainer.gone()
                obstetricExaminationContainer.gone()
                btnLayout.btnNext.text = getString(R.string.next)
                btnLayout.btnNext.isAllCaps = true
            }
            showCurrentMedication()
            reloadFragment(
                R.id.comorbiditiesContainer,
                NCDComorbiditiesFragment.TAG,
                NCDComorbiditiesFragment.newInstance(getMenuId(), isFemalePregnant())
            )
            reloadFragment(
                R.id.complicationsContainer,
                NCDComplicationsFragment.TAG,
                NCDComplicationsFragment.newInstance()
            )
            reloadFragment(
                R.id.lifestyleAssessmentContainer,
                NCDLifestyleAssessmentFragment.TAG,
                NCDLifestyleAssessmentFragment.newInstance()
            )
        } else {
            binding.apply {
                comorbiditiesContainer.gone()
                complicationsContainer.gone()
                currentMedicationContainer.gone()
                lifestyleAssessmentContainer.gone()
                chiefComplaintsContainer.visible()
                clinicalNotesContainer.visible()
                obstetricExaminationContainer.visible()
                btnLayout.btnNext.text = getString(R.string.submit)
                btnLayout.btnNext.isAllCaps = true
            }
            reloadFragment(
                R.id.chiefComplaintsContainer,
                NCDChiefComplaintsFragment.TAG,
                NCDChiefComplaintsFragment.newInstance(getMenuId(), isFemalePregnant())
            )
            reloadFragment(
                R.id.clinicalNotesContainer,
                NCDClinicalNotesFragment.TAG,
                NCDClinicalNotesFragment.newInstance()
            )
            reloadFragment(
                R.id.obstetricExaminationContainer,
                NCDObstetricExaminationFragment.TAG,
                NCDObstetricExaminationFragment.newInstance(getMenuId(), isFemalePregnant())
            )
        }
        hideLoading()
        patientDetailViewModel.forceRefresh = false
    }

    private fun isFemalePregnant(): Boolean {
        var isFemalePregnant = false
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let {
            isFemalePregnant =
                (it.gender?.equals(DefinedParams.female, true) == true) && (it.isPregnant == true)
        }
        return isFemalePregnant
    }

    fun showCurrentMedication() {
        val dialogFragment =
            supportFragmentManager.findFragmentByTag(NCDPatientHistoryDialog.TAG) as? NCDPatientHistoryDialog
        dialogFragment?.dismiss()
        val iscomorbiditiesContainer =
            supportFragmentManager.findFragmentById(R.id.comorbiditiesContainer) is NCDComorbiditiesFragment
        binding.apply {
            currentMedicationContainer.gone()
            // current only visible on ncd and after patient history submit diabetes known patient(also visible state of NCDComorbiditiesFragment(it show only on initial medical review))
            if ((getMenuId().equals(NCD, true)
                        && (viewModel.statusDiabetesValue != null
                        && !patientDetailViewModel.getNCDInitialMedicalReview()))
                && (iscomorbiditiesContainer && binding.comorbiditiesContainer.isVisible())
            ) {
                currentMedicationContainer.visible()
                addOrReuseFragment(
                    R.id.currentMedicationContainer,
                    NCDCurrentMedicationFragment.TAG,
                    NCDCurrentMedicationFragment.newInstance()
                )
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnLayout.btnNext.id -> {
                val isComorbiditiesFragmentVisible = binding.comorbiditiesContainer.isVisible
                val isChiefComplaintsFragmentVisible = binding.chiefComplaintsContainer.isVisible
                val isInitialReview = patientDetailViewModel.getNCDInitialMedicalReview()
                val isSummary =
                    supportFragmentManager.findFragmentByTag(NCDMedicalReviewSummaryFragment.TAG) is NCDMedicalReviewSummaryFragment
                when {
                    isSummary -> {
                        submitNextVisitCreate()
                    }

                    !isInitialReview && isComorbiditiesFragmentVisible -> {
                        if (validateInput()) {
                            loadFragment(false)
                        }
                    }

                    !isInitialReview && isChiefComplaintsFragmentVisible -> {
                        if (validateInputCMR() && handleValidation()) {
                            getInitialMedicalReviewData()
                        }
                    }

                    isInitialReview && isChiefComplaintsFragmentVisible -> {
                        // Add your logic here for when chief complaints fragment is visible and initial review
                        if (validateInputCMR()) {
                            getInitialMedicalReviewData()
                        }
                    }
                }
            }

            binding.btnLayout.ivPrescriptionImgView.id -> {
                showPrescription()
            }

            binding.btnLayout.ivTreatmentPlan.id -> {
                val dialog = NCDTreatmentPlanDialog.newInstance(
                    patientDetailViewModel.getPatientId(),
                    patientDetailViewModel.getPatientFHIRId(),
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
                    intent.putExtra(ORIGIN, getMenuOrigin())
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
        }
    }

    fun showPrescription() {
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

    private fun navigateUser(intent: Intent) {
        val bundle = Bundle()
        bundle.putString(NCDMRUtil.PATIENT_REFERENCE, patientDetailViewModel.getPatientId())
        bundle.putString(NCDMRUtil.MEMBER_REFERENCE, patientDetailViewModel.getPatientFHIRId())
        bundle.putString(NCDMRUtil.VISIT_ID, getEncounterReference())
        intent.putExtras(bundle)
        getResult.launch(intent)
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                swipeRefresh()
            }
        }

    private fun submitNextVisitCreate() {
        val fragment =
            supportFragmentManager.findFragmentByTag(NCDMedicalReviewSummaryFragment.TAG) as? NCDMedicalReviewSummaryFragment
        if (fragment?.validateInput() == true && fragment.handleConfirmDiagnoses()) {
            hitSummary()
        }
    }

    fun hitSummary() {
        withNetworkAvailability(online = {
            val request = NCDMRSummaryRequestResponse(
                enrollmentType = patientDetailViewModel.getEnrollmentType(),
                identityValue = patientDetailViewModel.getIdentityValue(),
                memberReference = patientDetailViewModel.getPatientFHIRId(),
                patientReference = patientDetailViewModel.getPatientId(),
                encounterReference = medicalReviewReference(),
                patientVisitId = getEncounterReference(),
                diagnosisType = getConfirmDiagnoses(getMenuId()),
                nextMedicalReviewDate = DateUtils.convertDateTimeToDate(
                    summaryViewModel.nextFollowupDate,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                ),
                villageId = patientDetailViewModel.getPatientVillageId(),
                provenance = ProvanceDto()
            )
            summaryViewModel.createNCDMRSummaryCreate(request,intent.getStringExtra(MENU_ID))
        })
    }

    private fun medicalReviewReference(): String? {
        return viewModel.createMedicalReview.value?.data?.encounterReference
    }

    private fun handleValidation(): Boolean {
        // Function to show the error dialog with customizable options
        fun showErrorDialog(
            message: String,
            showConfirm: Boolean = false,
            showYesNo: Pair<Boolean, Boolean> = Pair(true, true),
            cfText: String = ""
        ) {
            val existingDialog = supportFragmentManager.findFragmentByTag(NCDMRAlertDialog.TAG)
            if (existingDialog == null) {
                NCDMRAlertDialog.newInstance(
                    getString(R.string.alert),
                    message = message,
                    showYesNoClose = Triple(showYesNo.first, showYesNo.second, true),
                    showConfirm = showConfirm,
                    cfTest = cfText,
                    callback = this
                ).show(supportFragmentManager, NCDMRAlertDialog.TAG)
            }
        }
        // To check the bp is mandatory
        patientDetailViewModel.recentBP().let {
            if (it.isBlank()) {
                showErrorDialog(
                    getString(R.string.bp_mandatory_warning), cfText = getString(R.string.add_new_reading), showConfirm = true,
                    showYesNo = Pair(false, false)
                )
                return false
            }
        }

        val diagnosisList =
            medicalReviewDiagnosisCardViewModel.getConfirmDiagonsis.value?.data?.diagnosis
        val isDiagnosisListEmpty = diagnosisList.isNullOrEmpty()
        val isKnownDiabetes = viewModel.statusDiabetesValue != null

        // Show error if diagnosis list is empty and patient has no known diabetes status
        if (isDiagnosisListEmpty && !isKnownDiabetes) {
            showErrorDialog(getString(R.string.no_confirm_diagnosis_warning))
            return false
        }

        // If the menu ID is NCD and the patient has a known diabetes status
        if (getMenuId().equals(NCD, ignoreCase = true) && isKnownDiabetes) {
            // Show error if diagnosis list is empty for a known diabetes patient
            if (isDiagnosisListEmpty) {
                showErrorDialog(
                    getString(R.string.no_confirm_diagnosis_mandatory_warning),
                    showConfirm = true,
                    showYesNo = Pair(false, false)
                )
                return false
            }

            /*
             * 1. Check for matching diagnosis in the validation list.
             * 2. After submitting the patient history for NCD, retrieve the data from the ViewModel.
             * 3. Extract all diabetes types from the confirm diagnoses.
             * 4. Compare these types with list (e.g., gestational diabetes, diabetes type 1, type 2, pre-diabetes).
             * 5. For each comparison, obtain the confirmation value for patient history ncd.
             * 6. For example, if compare get confirm diagnoses against the list(contain only diabetes),then  it filter diabetes only then match the diabetes of patient history NCD.
             */
            val mismatchedItems = diagnosisList?.any { diagnosisItem ->
                viewModel.validationForStatus?.any { validationItem ->
                    validationItem.value == diagnosisItem.value && viewModel.statusDiabetesValue == validationItem.value
                } == true
            }

            // Show error if there are any mismatched diagnoses
            if (mismatchedItems == false) {
                showErrorDialog(
                    getString(R.string.edit_confirm_diagnosis_mandatory_warning),
                    showConfirm = true,
                    showYesNo = Pair(false, false)
                )
                return false
            }
        }
        return true
    }

    private fun getInitialMedicalReviewData() {
        val request = MedicalReviewRequestResponse(
            enrollmentType = patientDetailViewModel.getEnrollmentType(),
            identityValue = patientDetailViewModel.getIdentityValue(),
            patientReference = patientDetailViewModel.getPatientId(),
            memberReference = patientDetailViewModel.getPatientFHIRId(),
            provenance = ProvanceDto(),
            encounterReference = getEncounterReference()
        ).apply {
            initialMedicalReview = InitialMedicalReview().apply {
                //TODO request to make it generically in backend
                if (getMenuId().equals(NCD, true)
                    && viewModel.statusDiabetesValue != null
                    && !patientDetailViewModel.getNCDInitialMedicalReview()
                ) {
                    currentMedications = CurrentMedications(
                        medications = currentMedicationViewModel.chips.mapNotNull { chip ->
                            Chip(
                                id = chip.id,
                                name = chip.name,
                                value = if (chip.name.equals(
                                        DefinedParams.Other,
                                        true
                                    )
                                ) currentMedicationViewModel.comments.trim()
                                    .takeIf { it.isNotBlank() } else chip.value,
                                other = chip.name.equals(DefinedParams.Other, true)
                            )
                        },
                        drugAllergies = currentMedicationViewModel.drugAllergies,
                        adheringCurrentMed = currentMedicationViewModel.adheringCurrentMed,
                        adheringMedComment = currentMedicationViewModel.adheringMedComment,
                        allergiesComment = currentMedicationViewModel.allergiesComment
                    )
                }
                comorbidities = comorbiditiesViewModel.chips.map { chip ->
                    Chip(
                        id = chip.id,
                        name = chip.name,
                        value = if (chip.name.equals(
                                DefinedParams.Other,
                                true
                            )
                        ) comorbiditiesViewModel.comments.trim()
                            .takeIf { it.isNotBlank() } else chip.value,
                        other = chip.name.equals(DefinedParams.Other, true)
                    )
                }

                complications = complicationsViewModel.chips.map { chip ->
                    Chip(
                        id = chip.id,
                        name = chip.name,
                        value = if (chip.name.equals(
                                DefinedParams.Other,
                                true
                            )
                        ) complicationsViewModel.comments.trim()
                            .takeIf { it.isNotBlank() } else chip.value,
                        other = chip.name.equals(DefinedParams.Other, true)
                    )
                }

                lifestyle = lifestyleAssessmentViewModel.lifestyle?.map { lifeStyle ->
                    Chip(
                        id = lifeStyle.id,
                        name = lifeStyle.lifestyleQuestion,
                        value = lifeStyle.questionValue,
                        answer = Answer(
                            name = lifeStyle.lifestyleAnswer,
                            value = lifeStyle.answerValue
                        ),
                        comments = lifeStyle.comments?.trim().takeIf { it?.isNotBlank() == true }
                    )
                }
            }
            continuousMedicalReview = ContinuousMedicalReview(
                physicalExams = obstetricExaminationViewModel.chips.map { chip ->
                    Chip(
                        id = chip.id,
                        name = chip.name,
                        value = chip.value,
                        other = chip.name.equals(DefinedParams.Other, true)
                    )
                },
                clinicalNote = clinicalNotesViewModel.comments.trim().takeIf { it.isNotBlank() },
                complaints = chiefComplaintsViewModel.chips.map { chip ->
                    Chip(
                        id = chip.id,
                        name = chip.name,
                        value = chip.value,
                        other = chip.name.equals(DefinedParams.Other, true)
                    )
                },
                physicalExamComments = obstetricExaminationViewModel.comments.trim()
                    .takeIf { it.isNotBlank() },
                complaintComments = chiefComplaintsViewModel.comments.trim()
                    .takeIf { it.isNotBlank() }
            )
        }
        withNetworkCheck(connectivityManager, onNetworkAvailable = {
            val initialMr = if (patientDetailViewModel.getNCDInitialMedicalReview()) { AnalyticsDefinedParams.NCDContinuousMedicalReviewCreation } else { AnalyticsDefinedParams.NCDInitialMedicalReviewCreation}
            viewModel.createNCDMedicalReview(request,intent.getStringExtra(MENU_ID),initialMr)
        })
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        val comorbiditiesFragment =
            supportFragmentManager.findFragmentById(R.id.comorbiditiesContainer) as? NCDComorbiditiesFragment
        val chiefComplaintsFragment =
            supportFragmentManager.findFragmentById(R.id.chiefComplaintsContainer) as? NCDChiefComplaintsFragment
        val summaryFragment =
            supportFragmentManager.findFragmentById(R.id.obstetricExaminationContainer) as? NCDMedicalReviewSummaryFragment
        when {
            details.initialReviewed == false -> {
                when {
                    summaryFragment != null && (!binding.chiefComplaintsContainer.isVisible && !binding.comorbiditiesContainer.isVisible) -> loadSummary()
                    comorbiditiesFragment != null && !binding.chiefComplaintsContainer.isVisible -> loadFragment(
                        true
                    )

                    chiefComplaintsFragment != null && !binding.comorbiditiesContainer.isVisible -> loadFragment(
                        false
                    )

                    else -> {
                        loadFragment(true)
                    }
                }
            }

            details.initialReviewed == true -> {
                if (summaryFragment != null && (!binding.chiefComplaintsContainer.isVisible && !binding.comorbiditiesContainer.isVisible)) {
                    // Summary logic can go here if needed
                    loadSummary()
                } else {
                    loadFragment(false)
                }
            }
        }
        if (summaryFragment == null) {
            initializeFragments()
        }

    }

    private fun showPregnancyDialog() {
        withNetworkAvailability(online = {
            val hasNCD =
                !(viewModel.ncdPatientDiagnosisStatus.value?.data?.get(NCDMRUtil.NCDPatientStatus) as? Map<*, *>).isNullOrEmpty()
            val initialReviewed = patientDetailViewModel.getNCDInitialMedicalReview()
            patientDetailViewModel.getPatientFHIRId()?.let { id ->
                val dialog = supportFragmentManager.findFragmentByTag(NCDPregnancyDialog.TAG)
                if (dialog == null) {
                    NCDPregnancyDialog.newInstance(
                        getEncounterReference(),
                        patientDetailViewModel.getPatientId(),
                        patientId = id,
                        patientDetailViewModel.getGenderIsFemale(),
                        patientDetailViewModel.isPregnant(),
                        showNCD = !initialReviewed || !hasNCD
                    ) { isPositiveResult, message ->
                        if (isPositiveResult) showSuccessDialogue(
                            title = getString(R.string.pregnancy_details),
                            message = message
                        ) {
                            forceRefresh()
                        }
                        else showErrorDialogue(
                            title = getString(R.string.error),
                            message = message,
                            isNegativeButtonNeed = false
                        ) {

                        }
                    }.apply {
                        listener = this@NCDMedicalReviewActivity
                    }.show(supportFragmentManager, NCDPregnancyDialog.TAG)
                }
            }
        })
    }

    private fun showMentalHealthPatientHistoryDialog() {
        withNetworkAvailability(online = {
            val hasMentalHealth =
                !(viewModel.ncdPatientDiagnosisStatus.value?.data?.get(NCDMRUtil.MentalHealthStatus) as? Map<*, *>).isNullOrEmpty()
            val hasNCD =
                !(viewModel.ncdPatientDiagnosisStatus.value?.data?.get(NCDMRUtil.NCDPatientStatus) as? Map<*, *>).isNullOrEmpty()
            val initialReviewed = patientDetailViewModel.getNCDInitialMedicalReview()
            if (!initialReviewed || !hasMentalHealth)
                patientDetailViewModel.getPatientFHIRId()?.let { id ->
                    val dialog =
                        supportFragmentManager.findFragmentByTag(NCDMentalHealthFragment.TAG)
                    if (dialog == null) {
                        NCDMentalHealthFragment.newInstance(
                            getEncounterReference(),
                            patientDetailViewModel.getPatientId(),
                            id,
                            isFemale = patientDetailViewModel.getGenderIsFemale(),
                            patientDetailViewModel.isPregnant(),
                            showNCD = !initialReviewed || !hasNCD
                        ).apply {
                            listener = this@NCDMedicalReviewActivity
                        }.show(supportFragmentManager, NCDMentalHealthFragment.TAG)
                    }
                }
        })
    }

    fun validateInput(): Boolean {
        val fragmentOne =
            supportFragmentManager.findFragmentById(R.id.comorbiditiesContainer) as? NCDComorbiditiesFragment
        val fragmentTwo =
            supportFragmentManager.findFragmentById(R.id.complicationsContainer) as? NCDComplicationsFragment
        val fragmentThree =
            supportFragmentManager.findFragmentById(R.id.lifestyleAssessmentContainer) as? NCDLifestyleAssessmentFragment
        val fragmentFour =
            supportFragmentManager.findFragmentById(R.id.currentMedicationContainer) as? NCDCurrentMedicationFragment
        // Execute all validations

        val valueCurrentMedication = fragmentFour?.validateInput(true)
        val valueComorbidities = fragmentOne?.validateInput()
        val valueComplications = fragmentTwo?.validateInput()
        val valueLifestyle = fragmentThree?.validateInput()

        val isCurrentMedication =
            if (getMenuId().equals(NCD, true) && viewModel.statusDiabetesValue != null) {
                valueCurrentMedication?.first == true
            } else {
                true
            }
        val isValidComorbidities = valueComorbidities?.first == true
        val isValidComplications = valueComplications?.first == true
        val isValidLifestyle = valueLifestyle?.first == true

        val firstInvalidView = when {
            !isCurrentMedication -> valueCurrentMedication?.second
            !isValidComorbidities -> valueComorbidities?.second
            !isValidComplications -> valueComplications?.second
            !isValidLifestyle -> valueLifestyle?.second
            else -> null
        }
        // Request focus on the first invalid view, if it exists, and return validation result
        firstInvalidView?.requestFocus()
        // Return true only if all validations are true
        return isValidComorbidities && isValidComplications && isValidLifestyle && isCurrentMedication
    }

    private fun validateInputCMR(): Boolean {
        val fragmentOne =
            supportFragmentManager.findFragmentById(R.id.chiefComplaintsContainer) as? NCDChiefComplaintsFragment
        val fragmentTwo =
            supportFragmentManager.findFragmentById(R.id.clinicalNotesContainer) as? NCDClinicalNotesFragment
        val fragmentThree =
            supportFragmentManager.findFragmentById(R.id.obstetricExaminationContainer) as? NCDObstetricExaminationFragment
        val isValidChiefComplaints = fragmentOne?.validateInput()?.first == true
        val isValidClinicalNotes = fragmentTwo?.validateInput()?.first == true
        val isValidObstetricExamination = fragmentThree?.validateInput()?.first == true

        val firstInvalidView = when {
            !isValidChiefComplaints -> fragmentOne?.validateInput()?.second
            !isValidClinicalNotes -> fragmentTwo?.validateInput()?.second
            !isValidObstetricExamination -> fragmentThree?.validateInput()?.second
            else -> null
        }
        // Request focus on the first invalid view, if it exists, and return validation result
        firstInvalidView?.requestFocus()
        return isValidChiefComplaints && isValidClinicalNotes && isValidObstetricExamination
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
            }
        }

    fun backNavigation() {
        showLoading()
        val isComorbiditiesFragmentVisible = binding.comorbiditiesContainer.isVisible
        val isChiefComplaintsFragmentVisible = binding.chiefComplaintsContainer.isVisible
        val isSummaryVisible = binding.obstetricExaminationContainer.isVisible
        val isInitialReview = patientDetailViewModel.getNCDInitialMedicalReview()
        when {
            !isInitialReview && isComorbiditiesFragmentVisible -> showErrorDialog()
            !isInitialReview && isChiefComplaintsFragmentVisible -> {
                loadFragment(true)
                hideLoading()
            }

            isInitialReview && isChiefComplaintsFragmentVisible -> showErrorDialog()
            isInitialReview && isSummaryVisible -> showErrorDialog()
            !isInitialReview && (isSummaryVisible && (!isComorbiditiesFragmentVisible && !isChiefComplaintsFragmentVisible)) -> showErrorDialog()
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

    private fun showErrorDialogHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                startActivityWithoutSplashScreen()
            }
        }
        hideLoading()
    }

    fun forceRefresh() {
        patientDetailViewModel.forceRefresh = true
        swipeRefresh()
    }

    fun swipeRefresh() {
        withNetworkCheck(connectivityManager, onNetworkAvailable = {
            supportFragmentManager.findFragmentById(R.id.patientDetailFragment)
                .let {
                    getPatientId()?.let { id ->
                        patientDetailViewModel.getPatients(
                            id,
                            origin = getMenuOrigin()
                        )
                    }
                }
        }) {
            if (binding.refreshLayout.isRefreshing) {
                binding.refreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDialogDismissed(isConfirmed: Boolean) {
        if (isConfirmed) {
            // to refresh the card
            initializeFragments()
            showCurrentMedication()
            swipeRefresh()
        }
    }

    override fun closePage() {
        finish()
    }

    override fun onYesClicked() {
        if (validateInputCMR()) {
            getInitialMedicalReviewData()
        }
    }

    override fun onConfirmDiagnosisClicked(isBp: Boolean) {
        if (isBp) {
            addNewReading(true)
        } else {
            withNetworkAvailability(online = {
                showConfirmDiagnoses(isDiagnosisMismatch = true)
            })
        }
    }

    fun addNewReading(isBP: Boolean) {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { detail ->
            val intent = Intent(this, AssessmentReadingActivity::class.java)
            intent.putExtra(
                DefinedParams.FORM_TYPE_ID,
                if (isBP) DefinedParams.BP_LOG else DefinedParams.GLUCOSE_LOG
            )
            intent.putExtra(DefinedParams.IntentPatientDetails, detail)
            intent.putExtra(MENU_Name, intent.getStringExtra(MENU_ID))
            getResult.launch(intent)
        }
    }

    fun showConfirmDiagnoses(isDiagnosisMismatch: Boolean = false) {
        val dialog = supportFragmentManager.findFragmentByTag(NCDDiagnosisDialogFragment.TAG)
        if (dialog == null) {
            NCDDiagnosisDialogFragment.newInstance(
                patientDetailViewModel.getPatientId(),
                patientDetailViewModel.getPatientFHIRId(),
                getTypeForDiagnoses(getMenuId()),
                patientDetailViewModel.getGenderIsFemale(),
                getConfirmDiagnoses(getMenuId()),
                patientDetailViewModel.isPregnant(),
                isDiagnosisMismatch = isDiagnosisMismatch,
                getMenuId(),
                intent.getStringExtra(MenuId)
            ).apply {
                listener = this@NCDMedicalReviewActivity
            }.show(supportFragmentManager, NCDDiagnosisDialogFragment.TAG)
        }
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