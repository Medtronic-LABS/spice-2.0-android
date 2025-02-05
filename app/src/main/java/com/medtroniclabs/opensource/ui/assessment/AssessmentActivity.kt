package com.medtroniclabs.opensource.ui.assessment

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.startBackgroundOfflineSync
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.databinding.ActivityAssessmentBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.OtherSymptoms
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.Summary
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentICCMFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentICCMSummaryFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentNCDFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentNCDSummaryFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentOtherSymptomSummaryFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentOtherSymptomsFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentRMNCHFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentRMNCHNeonateFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentRMNCHNeonateSummaryFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentRMNCHSummaryFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentTBFragment
import com.medtroniclabs.opensource.ui.assessment.fragment.AssessmentTBSummaryFragment
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import com.medtroniclabs.opensource.ui.followup.FollowUpMyPatientActivity
import com.medtroniclabs.opensource.ui.household.HouseholdSearchActivity
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import dagger.hilt.android.AndroidEntryPoint

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
                backNavigation(true)
            }

        )
        getIntentValue()
        loadFragment()
        attachObservers()
        getCurrentLocation()
    }

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
                isNegativeButtonNeed = true
            ) { isPositive ->
                if (isPositive) {
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
    private fun getBackButtonStatus(): Pair<Boolean,Boolean> {
        val fragment = supportFragmentManager.findFragmentById(R.id.formsFragmentContainer)
        if (fragment is AssessmentRMNCHFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), false)
        } else if (fragment is AssessmentICCMFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), false)
        }else if (fragment is AssessmentOtherSymptomsFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), false)
        }else if (fragment is AssessmentRMNCHNeonateFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), false)
        }else if (fragment is AssessmentICCMSummaryFragment){
            return Pair(fragment.getCurrentAnsweredStatus(), true)
        }else if (fragment is AssessmentOtherSymptomSummaryFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), true)
        }else if (fragment is AssessmentRMNCHSummaryFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), true)
        }else if (fragment is AssessmentRMNCHNeonateSummaryFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), true)
        } else if(fragment is AssessmentNCDFragment) {
            return Pair(fragment.getCurrentAnsweredStatus(), true)
        } else if(fragment is AssessmentNCDSummaryFragment) {
            return Pair(false, false)
        }
        return Pair(false, false)
    }

    private fun navigationHandling(isHome: Boolean, isFromSummary: Boolean) {
        if (isFromSummary && !com.medtroniclabs.opensource.common.CommonUtils.isNonCommunity() )
            startBackgroundOfflineSync()

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
                is AssessmentRMNCHNeonateSummaryFragment,
                is AssessmentOtherSymptomSummaryFragment -> {
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
        var type= when (supportFragmentManager.findFragmentById(R.id.formsFragmentContainer)) {
            is AssessmentICCMFragment->{AnalyticsDefinedParams.ICCMAssessment}
            is AssessmentRMNCHFragment->{viewModel.workflowName.plus(AnalyticsDefinedParams.RMNCHAssessment)}
            is AssessmentRMNCHNeonateFragment->{AnalyticsDefinedParams.RMNCHNeonateAssessment}
            is AssessmentOtherSymptomsFragment->{AnalyticsDefinedParams.OtherSymptoms}
            else -> {""}
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = type,
            exitReason = btnClickType,
            eventName = AnalyticsDefinedParams.AssessmentCreation,
            isCompleted = false
        )
    }

    private fun loadSummaryFragment() {
        when (viewModel.menuId) {
            MenuConstants.ICCM_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentICCMSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentICCMSummaryFragment.TAG
                )
            }

            MenuConstants.TB_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentTBSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentTBSummaryFragment.TAG
                )
            }

            MenuConstants.OTHER_SYMPTOMS -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentOtherSymptomSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentOtherSymptomSummaryFragment::class.simpleName
                )
            }

            MenuConstants.RMNCH_MENU_ID -> {
                setTitle(Summary.capitalizeFirstChar())
                hideBackButton()
                replaceFragmentInId<AssessmentRMNCHSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentRMNCHSummaryFragment.TAG
                )
            }

            MenuConstants.NCD_MENU_ID -> {
                setTitle(getString(R.string.assessment_summary))
                showBackButton()
                replaceFragmentInId<AssessmentNCDSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentNCDSummaryFragment.TAG
                )
            }

            MenuConstants.MATERNAL_HEALTH -> {
                setTitle(getString(R.string.assessment_summary))
                showBackButton()
                replaceFragmentInId<AssessmentNCDSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentNCDSummaryFragment.TAG
                )
            }

            MenuConstants.MENTAL_HEALTH -> {
                setTitle(getString(R.string.assessment_summary))
                showBackButton()
                replaceFragmentInId<AssessmentNCDSummaryFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentNCDSummaryFragment.TAG
                )
            }
        }
    }

    private fun loadFragment() {
        val bundle = Bundle()
        bundle.putString(DefinedParams.FhirId, intent.getStringExtra(DefinedParams.FhirId))
        bundle.putString(DefinedParams.ORIGIN, intent.getStringExtra(DefinedParams.ORIGIN))
        bundle.putBoolean(MenuConstants.FOLLOW_UP, intent.getBooleanExtra(MenuConstants.FOLLOW_UP,false))
        when (viewModel.menuId) {
            MenuConstants.ICCM_MENU_ID -> {
                setTitle(MenuConstants.ICCM_MENU_ID.uppercase())
                replaceFragmentInId<AssessmentICCMFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentICCMFragment.TAG
                )
            }

            MenuConstants.TB_MENU_ID -> {
                setTitle(MenuConstants.TB_MENU_ID.uppercase())
                replaceFragmentInId<AssessmentTBFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentTBFragment.TAG
                )
            }

            MenuConstants.RMNCH_MENU_ID -> {
                setTitle(MenuConstants.RMNCH_MENU_ID.uppercase())
                replaceFragmentInId<AssessmentRMNCHFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentRMNCHFragment.TAG
                )
            }

            MenuConstants.OTHER_SYMPTOMS -> {
                setTitle(OtherSymptoms)
                replaceFragmentInId<AssessmentOtherSymptomsFragment>(
                    binding.formsFragmentContainer.id,
                    tag = AssessmentOtherSymptomsFragment::class.simpleName
                )
            }

            MenuConstants.NCD_MENU_ID -> {
                setTitle(AssessmentDefinedParams.ncd.uppercase())
                bundle.putString(Screening.type, MenuConstants.NCD_MENU_ID)
                showLoading()
                replaceFragmentInId<AssessmentNCDFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentNCDFragment.TAG
                )
            }

            MenuConstants.MATERNAL_HEALTH -> {
                setTitle(AssessmentDefinedParams.MaternalHealth)
                bundle.putString(Screening.type, MenuConstants.MATERNAL_HEALTH)
                showLoading()
                replaceFragmentInId<AssessmentNCDFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentNCDFragment.TAG
                )
            }

            MenuConstants.MENTAL_HEALTH -> {
                setTitle(this.getString(R.string.mental_health))
                bundle.putString(Screening.type, MenuConstants.MENTAL_HEALTH)
                showLoading()
                replaceFragmentInId<AssessmentNCDFragment>(
                    binding.formsFragmentContainer.id,
                    bundle = bundle,
                    tag = AssessmentNCDFragment.TAG
                )
            }

        }
    }

    private fun attachObservers() {
        viewModel.assessmentSaveLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let { _ ->
                        // insertOtherAssessmentDetails()
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
                    if (!com.medtroniclabs.opensource.common.CommonUtils.isNonCommunity() ) {
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
                            isNegativeButtonNeed = false
                        ) {}
                    }
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    loadSummaryFragment()
                }
            }
        }
    }

    private fun finishSuccessFlow() {
        val intent = if (viewModel.followUpId != null)
            Intent(this, FollowUpMyPatientActivity::class.java)
        else
            Intent(this, HouseholdSearchActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun getIntentValue() {
        viewModel.selectedHouseholdMemberId = intent.getLongExtra(DefinedParams.MemberID, -1L)
        viewModel.menuId = intent.getStringExtra(DefinedParams.MenuId)
        viewModel.workflowName = intent.getStringExtra(MenuConstants.WorkFlowName)
        viewModel.selectedMemberDob = intent.getStringExtra(DefinedParams.DOB)
        val followUpId = intent.getLongExtra(DefinedParams.FollowUpId, -1L)
        if (followUpId != -1L)
            viewModel.followUpId = followUpId
        else
            viewModel.followUpId = null

        viewModel.setUserJourney(viewModel.workflowName?.plus(getString(R.string.assessment)) ?:getString(R.string.assessment))
    }

    fun replaceAssessmentRMNCHNeonateFragment() {
        replaceFragmentInId<AssessmentRMNCHNeonateFragment>(
            binding.formsFragmentContainer.id,
            tag = AssessmentRMNCHNeonateFragment::class.simpleName
        )
    }

    fun replaceAssessmentRMNCHNeonateSummaryFragment() {
        hideBackButton()
        replaceFragmentInId<AssessmentRMNCHNeonateSummaryFragment>(
            binding.formsFragmentContainer.id,
            tag = AssessmentRMNCHNeonateSummaryFragment::class.simpleName
        )
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation(false)
            }
        }


}