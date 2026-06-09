package org.medtroniclabs.uhis.ui.assessment

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.databinding.ActivityAssessmentBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.OtherSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Summary
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentFamilyPlanningFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentFamilyPlanningSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentICCMFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentICCMSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentNCDFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentNCDSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentOtherSymptomSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentOtherSymptomsFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentPregnancyOutcomeFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentPregnancyOutcomeSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentPregnantWomenRegistrationFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentPregnantWomenRegistrationSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentRMNCHFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentRMNCHSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentSLNCDFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentSLNCDSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentTBFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.AssessmentTBSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.BDCataractAssessmentFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.BDCataractAssessmentSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.BDEyeCareAssessmentFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.BDEyeCareAssessmentSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.BDNCDAssessmentFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.BDNCDAssessmentSummaryFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.RxBuddySummaryFragment
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ANC
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ChildHoodVisit
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.DEATH_OF_MOTHER
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.DEATH_OF_NEWBORN
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PNC
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.cbs.activity.CbsActivity
import org.medtroniclabs.uhis.ui.followup.FollowUpMyPatientActivity
import org.medtroniclabs.uhis.ui.home.AssessmentToolsActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams
import org.medtroniclabs.uhis.ui.household.summary.HouseholdSummaryActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.membersearch.MemberSearchActivity
import org.medtroniclabs.uhis.ui.services.ServicesActivity

@AndroidEntryPoint
class AssessmentActivity : BaseActivity() {
    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityAssessmentBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.assessment),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation(false)
            },
            callbackHome = {
                viewModel.setUserJourney(AnalyticsDefinedParams.ONHOMEBUTTONTRIGGERED)
                backNavigation(true)
            },
        )
        getIntentValue()
        if (isCataractAccessDenied()) {
            finish()
            return
        }
        loadFragment()
        attachObservers()
    }

    private fun isCataractAccessDenied(): Boolean =
        CommonUtils.isCataractMenuId(viewModel.menuId) &&
            !CommonUtils.isCataractWorkflowEnabledForUser()

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            viewModel.setCurrentLocation(it)
        }
    }

    private fun backNavigation(isHome: Boolean) {
        val backButtonStatus = getBackButtonStatus()
        if (backButtonStatus.first) {
            showErrorDialogue(
                getString(R.string.alert),
                getString(R.string.exit_reason),
                isNegativeButtonNeed = true,
            ) { isPositive ->
                if (isPositive) {
                    viewModel.isAssessmentCancelLiveData.value = true
                    navigationHandling(isHome, backButtonStatus.second)
                }
            }
        } else {
            navigationHandling(isHome, backButtonStatus.second)
        }
    }

    /**
     * First boolean - Changes in page
     * Second boolean - Summary page or not
     */
    private fun getBackButtonStatus(): Pair<Boolean, Boolean> {
        when (val fragment = supportFragmentManager.findFragmentById(R.id.formsFragmentContainer)) {
            is AssessmentRMNCHFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentICCMFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentOtherSymptomsFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentICCMSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentOtherSymptomSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentRMNCHSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentNCDFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentTBFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentTBSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentNCDSummaryFragment -> {
                return Pair(false, false)
            }

            is AssessmentSLNCDFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentSLNCDSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentFamilyPlanningFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentFamilyPlanningSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is AssessmentPregnantWomenRegistrationFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentPregnantWomenRegistrationSummaryFragment -> {
                return Pair(false, true)
            }

            is AssessmentPregnancyOutcomeFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is AssessmentPregnancyOutcomeSummaryFragment -> {
                return Pair(false, true)
            }

            is BDNCDAssessmentFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is BDEyeCareAssessmentFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is BDCataractAssessmentFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), false)
            }

            is BDNCDAssessmentSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is BDEyeCareAssessmentSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            is BDCataractAssessmentSummaryFragment -> {
                return Pair(fragment.getCurrentAnsweredStatus(), true)
            }

            else -> return Pair(false, false)
        }
    }

    private fun navigationHandling(
        isHome: Boolean,
        isFromSummary: Boolean,
    ) {
        if (isFromSummary && !CommonUtils.isNonCommunity()) {
            startBackgroundOfflineSync()
        }

        if (isHome) {
            setupAnalytic(AnalyticsDefinedParams.HomeButtonClicked)
            val intent = Intent(this, LandingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        } else {
            setupAnalytic(AnalyticsDefinedParams.BackButtonClicked)
            when (supportFragmentManager.findFragmentById(R.id.fragmentContainer)) {
                is AssessmentICCMSummaryFragment,
                is AssessmentRMNCHSummaryFragment,
                is AssessmentOtherSymptomSummaryFragment,
                -> {
                    finishSuccessFlow()
                }

                is AssessmentNCDSummaryFragment -> {
                    val intent = Intent(this, LandingActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    finish()
                }

                else -> {
                    this@AssessmentActivity.finish()
                }
            }
        }
    }

    private fun setupAnalytic(btnClickType: String) {
        val type = when (supportFragmentManager.findFragmentById(R.id.formsFragmentContainer)) {
            is AssessmentICCMFragment -> {
                AnalyticsDefinedParams.ICCMAssessment
            }

            is AssessmentRMNCHFragment -> {
                viewModel.workflowName.plus(AnalyticsDefinedParams.RMNCHAssessment)
            }

            is AssessmentOtherSymptomsFragment -> {
                AnalyticsDefinedParams.OtherSymptoms
            }

            is AssessmentPregnantWomenRegistrationFragment -> {
                AnalyticsDefinedParams.PREGNANT_WOMEN_PROFILE
            }

            is AssessmentPregnancyOutcomeFragment -> {
                AnalyticsDefinedParams.PREGNANCY_OUTCOME
            }

            else -> {
                ""
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = type,
            exitReason = btnClickType,
            eventName = AnalyticsDefinedParams.AssessmentCreation,
            isCompleted = false,
        )
    }

    private fun loadSummaryFragment() {
        val bundle = Bundle()
        bundle.putString(MenuConstants.WORKFLOW_NAME, viewModel.workflowName)
        when (viewModel.menuId) {
            MenuConstants.ICCM_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentICCMSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentICCMSummaryFragment.TAG,
                )
            }

            MenuConstants.TB_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentTBSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentTBSummaryFragment.TAG,
                )
            }

            MenuConstants.OTHER_SYMPTOMS -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentOtherSymptomSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentOtherSymptomSummaryFragment::class.simpleName,
                )
            }

            MenuConstants.RMNCH_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentRMNCHSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentRMNCHSummaryFragment.TAG,
                )
            }

            MenuConstants.NCD_MENU_ID -> {
                if (CommonUtils.isNonCommunity()) {
                    setTitle(getString(R.string.assessment_summary))
                    showBackButton()
                    replaceFragmentInId<AssessmentNCDSummaryFragment>(
                        binding.formsFragmentContainer.id,
                        tag = AssessmentNCDSummaryFragment.TAG,
                    )
                } else {
                    setTitle(Summary.capitalizeFirstChar())
                    hideBackButton()
                    replaceFragmentInId<BDNCDAssessmentSummaryFragment>(
                        binding.formsFragmentContainer.id,
                        tag = BDNCDAssessmentSummaryFragment.TAG,
                    )
                }
            }

            MenuConstants.MATERNAL_HEALTH -> {
                setTitle(getString(R.string.assessment_summary))
                showBackButton()
                replaceFragmentInId<AssessmentNCDSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentNCDSummaryFragment.TAG,
                )
            }

            MenuConstants.MENTAL_HEALTH -> {
                setTitle(getString(R.string.assessment_summary))
                showBackButton()
                replaceFragmentInId<AssessmentNCDSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentNCDSummaryFragment.TAG,
                )
            }

            MenuConstants.FP_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentFamilyPlanningSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentFamilyPlanningSummaryFragment::class.simpleName,
                )
            }

            MenuConstants.PREGNANT_WOMEN_PROFILE -> {
                setTitle(getString(R.string.summary))
                hideBackButton()
                replaceFragmentInId<AssessmentPregnantWomenRegistrationSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentPregnantWomenRegistrationSummaryFragment.TAG,
                )
            }

            MenuConstants.PREGNANCY_OUTCOME -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentPregnancyOutcomeSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentPregnancyOutcomeSummaryFragment.TAG,
                )
            }

            MenuConstants.EYE_CARE_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<BDEyeCareAssessmentSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = BDEyeCareAssessmentSummaryFragment.TAG,
                )
            }

            MenuConstants.CATARACT_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<BDCataractAssessmentSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = BDCataractAssessmentSummaryFragment.TAG,
                )
            }
        }
    }

    private fun loadFragment() {
        val bundle = Bundle()
        bundle.putString(DefinedParams.FhirId, intent.getStringExtra(DefinedParams.FhirId))
        bundle.putString(DefinedParams.ORIGIN, intent.getStringExtra(DefinedParams.ORIGIN))
        bundle.putBoolean(MenuConstants.FOLLOW_UP, intent.getBooleanExtra(MenuConstants.FOLLOW_UP, false))
        when (viewModel.menuId) {
            MenuConstants.ICCM_MENU_ID -> {
                setTitle(MenuConstants.ICCM_MENU_ID.uppercase())
                replaceFragmentInId<AssessmentICCMFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentICCMFragment.TAG,
                )
            }

            MenuConstants.TB_MENU_ID -> {
                bundle.putBoolean(DefinedParams.CONTACT_TRACING, intent.getBooleanExtra(DefinedParams.CONTACT_TRACING, false))
                bundle.putLong(DefinedParams.HOUSEHOLD_ID, viewModel.selectedHouseholdId)
                bundle.putBoolean(DefinedParams.isTbPatient, true)
                setTitle(MenuConstants.TB_MENU_ID.uppercase())
                replaceFragmentInId<AssessmentTBFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentTBFragment.TAG,
                )
            }

            MenuConstants.RMNCH_MENU_ID -> {
                setTitle(getRMNCHScreenTitle())
                replaceFragmentInId<AssessmentRMNCHFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentRMNCHFragment.TAG,
                )
            }

            MenuConstants.OTHER_SYMPTOMS -> {
                setTitle(OtherSymptoms)
                replaceFragmentInId<AssessmentOtherSymptomsFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentOtherSymptomsFragment::class.simpleName,
                )
            }

            MenuConstants.NCD_MENU_ID -> {
                if (CommonUtils.isNonCommunity()) {
                    setTitle(getString(R.string.ncd))
                    bundle.putString(Screening.type, MenuConstants.NCD_MENU_ID)
                    showLoading()
                    replaceFragmentInId<AssessmentNCDFragment>(
                        binding.formsFragmentContainer.id,
                        bundle = bundle,
                        tag = AssessmentNCDFragment.TAG,
                    )
                } else {
                    setTitle(getString(R.string.ncd))
                    bundle.putString(Screening.type, MenuConstants.NCD_MENU_ID)
                    showLoading()
                    replaceFragmentInId<BDNCDAssessmentFragment>(
                        binding.formsFragmentContainer.id,
                        bundle = bundle,
                        tag = BDNCDAssessmentFragment.TAG,
                    )
                }
            }

            MenuConstants.EYE_CARE_MENU_ID -> {
                setTitle(getString(R.string.eye_care))
                bundle.putString(Screening.type, MenuConstants.EYE_CARE_MENU_ID)
                showLoading()
                replaceFragmentInId<BDEyeCareAssessmentFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = BDEyeCareAssessmentFragment.TAG,
                )
            }

            MenuConstants.CATARACT_MENU_ID -> {
                setTitle(getString(R.string.cataract))
                bundle.putString(Screening.type, MenuConstants.CATARACT_MENU_ID)
                showLoading()
                replaceFragmentInId<BDCataractAssessmentFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = BDCataractAssessmentFragment.TAG,
                )
            }

            MenuConstants.MATERNAL_HEALTH -> {
                setTitle(AssessmentDefinedParams.MaternalHealth)
                bundle.putString(Screening.type, MenuConstants.MATERNAL_HEALTH)
                showLoading()
                replaceFragmentInId<AssessmentNCDFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentNCDFragment.TAG,
                )
            }

            MenuConstants.MENTAL_HEALTH -> {
                setTitle(this.getString(R.string.mental_health))
                bundle.putString(Screening.type, MenuConstants.MENTAL_HEALTH)
                showLoading()
                replaceFragmentInId<AssessmentNCDFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentNCDFragment.TAG,
                )
            }

            MenuConstants.FP_MENU_ID -> {
                setTitle(getString(R.string.family_planning).uppercase())
                replaceFragmentInId<AssessmentFamilyPlanningFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentFamilyPlanningFragment.TAG,
                )
            }

            MenuConstants.PREGNANT_WOMEN_PROFILE -> {
                setTitle(getString(R.string.pregnant_women_profile))
                replaceFragmentInId<AssessmentPregnantWomenRegistrationFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentPregnantWomenRegistrationFragment.TAG,
                )
            }

            MenuConstants.PREGNANCY_OUTCOME -> {
                setTitle(getString(R.string.pregnancy_outcome))
                replaceFragmentInId<AssessmentPregnancyOutcomeFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentPregnancyOutcomeFragment.TAG,
                )
            }
        }
    }

    private fun getRMNCHScreenTitle(): String =
        when (viewModel.workflowName) {
            ANC -> getString(R.string.anc)
            PNC -> getString(R.string.pnc)
            ChildHoodVisit -> getString(R.string.child_health)
            else -> MenuConstants.RMNCH_MENU_ID.uppercase()
        }

    private fun attachObservers() {
        viewModel.assessmentSaveLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let {
                        loadSummaryFragment()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        viewModel.assessmentUpdateLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    finishSuccessFlow()
                    if (!CommonUtils.isNonCommunity()) {
                        startBackgroundOfflineSync()
                    }
                }

                else -> {}
            }
        }

        viewModel.assessmentSaveResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(
                            getString(R.string.error),
                            it,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    loadSummaryFragment()
                }
            }
        }

        viewModel.saveRxBuddyDetails.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(
                            getString(R.string.error),
                            it,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    setTitle(Summary.capitalizeFirstChar())
                    hideBackButton()
                    replaceFragmentInId<RxBuddySummaryFragment>(
                        binding.formsFragmentContainer.id,
                        tag = RxBuddySummaryFragment.TAG,
                    )
                }
            }
        }

        viewModel.saveRxBuddyFollowUpLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(
                            getString(R.string.error),
                            it,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    val bundle = Bundle().apply {
                        putBoolean(DefinedParams.isRxBuddyFollowUp, true)
                        putBoolean(MenuConstants.FOLLOW_UP, intent.getBooleanExtra(MenuConstants.FOLLOW_UP, false))
                    }
                    setTitle(Summary.capitalizeFirstChar())
                    hideBackButton()
                    replaceFragmentInId<RxBuddySummaryFragment>(
                        binding.formsFragmentContainer.id,
                        bundle = bundle,
                        tag = RxBuddySummaryFragment.TAG,
                    )
                }
            }
        }
    }

    private fun startCbsActivity(
        workFlowName: String,
        memberId: Long?,
        assessmentId: Long?,
        deathOfMother: Boolean = false,
        deathOfNewborn: Boolean = false,
        motherId: Long? = null,
    ) {
        val intent = Intent(this, CbsActivity::class.java).apply {
            putExtra(DefinedParams.MEMBER_ID, memberId)
            putExtra(DefinedParams.DOB, viewModel.selectedMemberDob)
            putExtra(MenuConstants.WORKFLOW_NAME, workFlowName)
            putExtra(DefinedParams.MENU_ID, DefinedParams.CBS.lowercase())
            putExtra(DefinedParams.MOTHER_ID, motherId)
            if (assessmentId != null) putExtra(DefinedParams.ASSESSMENT_ID, assessmentId)
            if (deathOfMother) putExtra(DEATH_OF_MOTHER, true)
            if (deathOfNewborn) putExtra(DEATH_OF_NEWBORN, true)
        }
        startActivity(intent)
    }

    fun finishSuccessFlow() {
        val intent = if (viewModel.followUpId != null) {
            Intent(this, FollowUpMyPatientActivity::class.java)
        } else {
            // For pregnant women after click done navigate user to tools screen
            if (viewModel.menuId == MenuConstants.PREGNANT_WOMEN_PROFILE) {
                Intent(this, AssessmentToolsActivity::class.java).apply {
                    putExtra(DefinedParams.HOUSEHOLD_ID, viewModel.selectedHouseholdId)
                    putExtra(DefinedParams.MEMBER_ID, viewModel.selectedHouseholdMemberId)
                    putExtra(DefinedParams.FOLLOW_UP_ID, viewModel.followUpId)
                    putExtra(DefinedParams.DOB, viewModel.selectedMemberDob)
                    putExtra(DefinedParams.FhirId, viewModel.memberFhirId)
                    putExtra(DefinedParams.ORIGIN, this@AssessmentActivity.intent.getStringExtra(DefinedParams.ORIGIN))
                    putExtra(MenuConstants.FOLLOW_UP, this@AssessmentActivity.intent.getBooleanExtra(MenuConstants.FOLLOW_UP, false))
                }
            } else {
                // Check if member is external (householdLocalId is null or householdLocalId is 0)
                val householdLocalId = viewModel.memberDetailsLiveData.value
                    ?.data
                    ?.householdLocalId
                val isExternalMember = householdLocalId == null || householdLocalId == 0L
                if (viewModel.entryPoint == MemberSearchActivity.ENTRY_POINT_MEMBER_SEARCH) {
                    Intent(this, MemberSearchActivity::class.java)
                } else if (viewModel.entryPoint == ServicesActivity.ENTRY_POINT_SERVICES) {
                    Intent(this, ServicesActivity::class.java)
                } else if (isExternalMember) {
                    Intent(this, ServicesActivity::class.java).apply {
                        putExtra(ServicesActivity.IS_EXTERNAL_MEMBER, true)
                    }
                } else {
                    Intent(this, HouseholdSummaryActivity::class.java).apply {
                        putExtra(DefinedParams.householdId, viewModel.selectedHouseholdId)
                        putExtra(HouseholdDefinedParams.IS_FROM_HOUSEHOLD_REGISTRATION, false)
                    }
                }
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun getIntentValue() {
        viewModel.selectedHouseholdMemberId = intent.getLongExtra(DefinedParams.MEMBER_ID, -1L)
        viewModel.menuId = intent.getStringExtra(DefinedParams.MENU_ID)
        viewModel.workflowName = intent.getStringExtra(MenuConstants.WORKFLOW_NAME)
        viewModel.memberFhirId = intent.getStringExtra(DefinedParams.FhirId)
        viewModel.selectedMemberDob = intent.getStringExtra(DefinedParams.DOB)
        viewModel.selectedHouseholdId = intent.getLongExtra(DefinedParams.HOUSEHOLD_ID, -1L)
        viewModel.entryPoint = intent.getStringExtra(DefinedParams.ENTRY_POINT)
        val followUpId = intent.getLongExtra(DefinedParams.FOLLOW_UP_ID, -1L)
        if (followUpId != -1L) {
            viewModel.followUpId = followUpId
        } else {
            viewModel.followUpId = null
        }

        viewModel.workflowName?.let {
            viewModel.setUserJourney(getUserJourneyName(it))
        }
    }

    private fun getUserJourneyName(it: String): String =
        if (it == PNC) {
            AnalyticsDefinedParams.PNCMOTHERASSESSMENT
        } else if (it == ChildHoodVisit) {
            AnalyticsDefinedParams.RMNCHCHILDASSESSMENT
        } else {
            it.plus(getString(R.string.assessment))
        }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation(false)
            }
        }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }

    override fun consumeImeInsets() = true
}
