package org.medtroniclabs.uhis.ui.cbs.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.microcoaching.MicroCoachingSDK
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ASSESSMENT_ID
import org.medtroniclabs.uhis.common.DefinedParams.CBS
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.ActivityAssessmentBinding
import org.medtroniclabs.uhis.db.dao.MetaDataDAO
import org.medtroniclabs.uhis.db.entity.AssessmentEntity
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.microcoaching.toSdkAssessmentMap
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.DEATH_OF_MOTHER
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.DEATH_OF_NEWBORN
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.cbs.fragment.CbsFragment
import org.medtroniclabs.uhis.ui.cbs.fragment.CbsMemberRegistration
import org.medtroniclabs.uhis.ui.cbs.fragment.CbsSummaryFragment
import org.medtroniclabs.uhis.ui.household.HouseholdSearchActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import javax.inject.Inject

@AndroidEntryPoint
class CbsActivity : BaseActivity(), OnDialogDismissListener {
    private lateinit var binding: ActivityAssessmentBinding
    private val viewModel: AssessmentViewModel by viewModels()

    /**
     * Used by [notifyMicroCoachingSDK] to resolve `villageId → chiefdomId`
     * (SPICE's equivalent of the backend's `upazila_id`). Mirrors the
     * field-injection style in
     * [org.medtroniclabs.uhis.ui.assessment.AssessmentActivity].
     */
    @Inject
    lateinit var metaDataDAO: MetaDataDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityAssessmentBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.cbs_register),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                handleBack()
            },
            callbackHome = {
                viewModel.setUserJourney(AnalyticsDefinedParams.ONHOMEBUTTONTRIGGERED)
                if (intent.getBooleanExtra(DEATH_OF_MOTHER, false) ||
                    intent.getBooleanExtra(DEATH_OF_NEWBORN, false)
                ) {
                    navigateAncCbs()
                } else {
                    navigateHome(true)
                }
            },
        )
        initView()
        attachObservers()
    }

    fun handleBack() {
        if (intent.getBooleanExtra(DEATH_OF_MOTHER, false) ||
            intent.getBooleanExtra(DEATH_OF_NEWBORN, false)
        ) {
            navigateAncCbs()
        } else {
            navigateHome()
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        finishSuccessFlow()
    }

    private fun finishSuccessFlow() {
        val intent = Intent(this, HouseholdSearchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        this.finish()
        this.startBackgroundOfflineSync()
    }

    private fun backNavigation(): Pair<Boolean, Boolean> {
        val fragment = supportFragmentManager.findFragmentById(R.id.formsFragmentContainer)
        return if (fragment is CbsFragment) {
            Pair(fragment.getCurrentAnsweredStatus(), false)
        } else if (fragment is CbsSummaryFragment) {
            Pair(true, true)
        } else if (fragment is CbsMemberRegistration) {
            Pair(fragment.getCurrentAnsweredStatus(), false)
        } else {
            Pair(false, false)
        }
    }

    private fun navigateHome(isHome: Boolean = false) {
        val backButtonStatus = backNavigation()
        if (backButtonStatus.first) {
            showErrorDialogue(
                getString(R.string.alert),
                getString(R.string.exit_reason),
                isNegativeButtonNeed = true,
            ) { isPositive ->
                if (isPositive) {
                    navigationHandling(isHome, backButtonStatus.second)
                }
            }
        } else {
            navigationHandling(isHome, backButtonStatus.second)
        }
    }

    private fun navigateAncCbs() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                navigationHandling(isHome = true, true)
            }
        }
    }

    private fun navigationHandling(
        isHome: Boolean,
        isFromSummary: Boolean,
    ) {
        if (isFromSummary) {
            startBackgroundOfflineSync()
        }

        if (isHome) {
            val intent = Intent(this, LandingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        } else {
            this.finish()
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
                    resource.data?.let { (_, assessmentEntity) ->
                        notifyMicroCoachingSDK(assessmentEntity)
                        // insertOtherAssessmentDetails()
                        if (intent.getBooleanExtra(DEATH_OF_NEWBORN, false) ||
                            intent.getBooleanExtra(DEATH_OF_MOTHER, false)
                        ) {
                            val key = if (intent.getBooleanExtra(DEATH_OF_MOTHER, false)) {
                                DefinedParams.RmnchNotifiableCondition
                            } else if (intent.getBooleanExtra(DEATH_OF_NEWBORN, false)) {
                                DefinedParams.CbsNotifiableCondition
                            } else {
                                DefinedParams.CbsNotifiableCondition
                            }
                            viewModel.updateMemberDeceasedStatus(
                                viewModel.memberDetailsLiveData.value
                                    ?.data
                                    ?.id ?: -1L,
                                false,
                                extractNotifiableConditions(viewModel.assessmentStringLiveData.value, key),
                            )
                        }
                        loadSummaryFragment()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.birthLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let {
                        loadMemberRegistration()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.callResultSaveLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let {
                        // insertOtherAssessmentDetails()
                        viewModel.assessmentSaveLiveData.postValue(Resource(state = ResourceState.SUCCESS, Pair(it.first, it.second)))
                        viewModel.callResultSaveLiveData.postError()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.memberCbsDetailsLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let { data ->
                        val it = data.second
                        // insertOtherAssessmentDetails()
                        val deathReason =
                            (
                                (viewModel.assessmentMap[CBS.lowercase()] as? Map<String, Any>)
                                    ?.get(DefinedParams.surveillanceDetails) as? Map<String, Any>
                            )?.get(DefinedParams.RmnchNotifiableCondition) as? List<Map<String, Any>>

                        val rmnchText =
                            deathReason
                                ?.mapNotNull { it[DefinedParams.NAME] as? String }
                                ?.toMutableList() ?: mutableListOf()

                        val otherText =
                            (
                                (viewModel.assessmentMap[CBS.lowercase()] as? Map<String, Any>)
                                    ?.get(DefinedParams.surveillanceDetails) as? Map<String, Any>
                            )?.get(DefinedParams.OTHER_NOTIFIABLE_CONDITIONS) as? String
                        val cbsMap =
                            (viewModel.assessmentMap[CBS.lowercase()] as? MutableMap<String, Any>)
                                ?: mutableMapOf()
                        val surveillanceMap =
                            (cbsMap[DefinedParams.surveillanceDetails] as? MutableMap<String, Any>)
                                ?: mutableMapOf()
                        surveillanceMap[RMNCH.DateOfDelivery] = it.dateOfBirth
                        cbsMap[DefinedParams.surveillanceDetails] = surveillanceMap
                        viewModel.assessmentMap[CBS.lowercase()] = cbsMap
                        val index = rmnchText.indexOfFirst {
                            it.equals(
                                DefinedParams.OTHER,
                                ignoreCase = true,
                            )
                        }

                        if (index != -1 && !otherText.isNullOrBlank()) {
                            rmnchText[index] = "${DefinedParams.OTHER} ($otherText)"
                        }
                        val finalText = rmnchText.joinToString(", ")

                        /*
                        In direct CBS member death cases, we should handle it separately. However,
                         in RMNCH cases, CBS navigation occurs only in the event of a mother's death.
                         Since the assessment flow already manages death cases,
                         we only need to handle it specifically for CBS.
                         */
                        val isAncOrNormal = viewModel.birthLiveData.value
                            ?.data
                            ?.third ?: false
                        if (isAncOrNormal) {
                            val isDelete = viewModel.birthLiveData.value
                                ?.data
                                ?.second ?: false
                            if (isDelete) {
                                viewModel.updateMemberDeceasedStatus(
                                    viewModel.memberDetailsLiveData.value
                                        ?.data
                                        ?.id ?: -1L,
                                    false,
                                    finalText,
                                )
                            }
                            viewModel.cbsMemberIDAndPregnancyDetail.first?.let {
                                viewModel.savePatientClinicalInformation(
                                    viewModel.getUpdatedPregnancyDetail(
                                        it,
                                        viewModel.cbsMemberIDAndPregnancyDetail.second,
                                        true,
                                    ),
                                )
                            }
                            viewModel.saveAssessment(
                                data.first,
                                viewModel.assessmentMap,
                                viewModel.referralResult,
                                viewModel.menuId,
                            )
                        } else {
                            val dataOfDelivery = it.dateOfBirth
                            viewModel.assessment?.let { assessment ->
                                val type = object : TypeToken<HashMap<String, Any>>() {}.type
                                val assessmentMap: HashMap<String, Any> = Gson().fromJson(assessment.assessmentDetails, type)
                                val cbsKey = CBS.lowercase()
                                val cbsMap = (assessmentMap[cbsKey] as? LinkedTreeMap<String, Any>) ?: hashMapOf()
                                cbsMap[RMNCH.DateOfDelivery] = dataOfDelivery
                                assessmentMap[cbsKey] = cbsMap
                                assessment.assessmentDetails = Gson().toJson(assessmentMap)
                                viewModel.saveCallResult(data.first, assessment, viewModel.resultValue)
                            }
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun extractNotifiableConditions(
        jsonString: String?,
        key: String,
    ): String =
        try {
            jsonString?.let {
                val jsonObject = JSONObject(it)
                val surveillanceDetails = jsonObject
                    .getJSONObject(CBS.lowercase())
                    .getJSONObject(DefinedParams.surveillanceDetails)

                val jsonArray =
                    surveillanceDetails.getJSONArray(key)

                val conditions = (0 until jsonArray.length())
                    .mapNotNull { index ->
                        jsonArray.getJSONObject(index).optString(DefinedParams.NAME, null)
                    }.toMutableList()

                // Check if "Other" exists, then append "Other(value)"
                if (DefinedParams.OTHER in conditions) {
                    val otherValue =
                        surveillanceDetails
                            .optString(DefinedParams.OTHER_NOTIFIABLE_CONDITIONS, "")
                            .takeIf { it.isNotEmpty() }
                    if (otherValue != null) {
                        conditions[conditions.indexOf(DefinedParams.OTHER)] =
                            "${DefinedParams.OTHER}($otherValue)"
                    }
                }
                conditions.joinToString(", ")
            } ?: ""
        } catch (e: Exception) {
            "" // Return an empty string if parsing fails
        }

    private fun loadSummaryFragment() {
        setTitle(AssessmentDefinedParams.Summary.capitalizeFirstChar())
        hideBackButton()
        replaceFragmentInId<CbsSummaryFragment>(
            binding.formsFragmentContainer.id,
            tag = CbsSummaryFragment.TAG,
        )
    }

    private fun loadMemberRegistration() {
        setTitle(getString(R.string.member_registration))
        replaceFragmentInId<CbsMemberRegistration>(
            binding.formsFragmentContainer.id,
            tag = CbsMemberRegistration.TAG,
        )
    }

    private fun initView() {
        loadFragment()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        }

    private fun loadFragment() {
        viewModel.selectedHouseholdMemberId = intent.getLongExtra(DefinedParams.MEMBER_ID, -1L)
        viewModel.menuId = intent.getStringExtra(DefinedParams.MENU_ID)
        viewModel.workflowName = intent.getStringExtra(MenuConstants.WORKFLOW_NAME)
        viewModel.motherID = intent.getLongExtra(DefinedParams.MOTHER_ID, -1L)
        val bundle = Bundle()
        bundle.putString(DefinedParams.FhirId, intent.getStringExtra(DefinedParams.FhirId))
        bundle.putString(DefinedParams.ORIGIN, intent.getStringExtra(DefinedParams.ORIGIN))
        bundle.putString(
            MenuConstants.WORKFLOW_NAME,
            intent.getStringExtra(MenuConstants.WORKFLOW_NAME),
        )
        bundle.putLong(ASSESSMENT_ID, intent.getLongExtra(ASSESSMENT_ID, 0L))
        bundle.putBoolean(DEATH_OF_NEWBORN, intent.getBooleanExtra(DEATH_OF_NEWBORN, false))
        bundle.putBoolean(DEATH_OF_MOTHER, intent.getBooleanExtra(DEATH_OF_MOTHER, false))
        setTitle(getString(R.string.cbs_register))
        replaceFragmentInId<CbsFragment>(
            binding.formsFragmentContainer.id,
            bundle = bundle,
            tag = CbsFragment.TAG,
        )
    }

    /**
     * Hand the just-submitted CBS assessment off to the MicroCoaching SDK
     * so it can emit the `clinical_observed` family events
     * (`spice_action_observed`, conditional `risk_flag_observed`) plus the
     * stub `card_shown` row.
     *
     * Fires *before* the deceased-status update + `loadSummaryFragment()`
     * so the event row exists regardless of what happens next in the
     * (DEATH_OF_MOTHER / DEATH_OF_NEWBORN) handling branch. See
     * [org.medtroniclabs.uhis.ui.assessment.AssessmentActivity.notifyMicroCoachingSDK]
     * for the equivalent NCD-side hook, the rationale for the blank
     * `encounterId`, and the design notes on what `viewModel.referralStatus`
     * / `viewModel.referralReason` / `chiefdomId` map to in the backend
     * three-axis `correctReferral*` payload.
     */
    private fun notifyMicroCoachingSDK(assessmentEntity: AssessmentEntity) {
        if (!MicroCoachingSDK.isInitialized()) return
        val chwId = runCatching { SecuredPreference.getUserId().toString() }
            .getOrDefault("")
        if (chwId.isBlank()) return

        // Snapshot mutable VM fields now (they may be reset by a subsequent
        // submission before the IO coroutine resumes).
        val systemReferralStatus = viewModel.referralStatus
        val systemReferralReasons = viewModel.referralReason

        lifecycleScope.launch(Dispatchers.IO) {
            val upazilaId = runCatching {
                assessmentEntity.villageId
                    .toLongOrNull()
                    ?.let { metaDataDAO.getVillageByID(it).chiefdomId }
                    ?.toString()
            }.getOrNull()

            MicroCoachingSDK.getInstance().onAssessmentSubmitted(
                encounterId = "",
                patientId = assessmentEntity.patientId.orEmpty(),
                assessmentData = assessmentEntity.toSdkAssessmentMap(
                    systemReferralStatus = systemReferralStatus,
                    systemReferralReasons = systemReferralReasons,
                    upazilaId = upazilaId,
                ),
            )
        }
    }
}
