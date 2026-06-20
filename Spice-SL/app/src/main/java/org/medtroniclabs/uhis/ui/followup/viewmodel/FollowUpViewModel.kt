package org.medtroniclabs.uhis.ui.followup.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.FollowUpPatientModel
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.followup.FollowUpFilter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.repo.FollowUpRepository
import org.medtroniclabs.uhis.ui.BaseFilterViewModel
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_COMMUNITY_CLINIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_UPAZILA
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class FollowUpViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val followUpRepository: FollowUpRepository,
    override val metaRepository: MetaRepository,
) : BaseFilterViewModel(dispatcherIO, metaRepository) {
    var selectedFollowUpDetail: FollowUpPatientModel? = null

    private val filterLiveData = MutableLiveData<FollowUpFilter>()
    val followUpPatientListLiveData: LiveData<List<FollowUpPatientModel>> =
        filterLiveData.switchMap {
            val referralLimit = referralDayLimitLiveData.value ?: 2
            followUpRepository.getFollowUpListLiveData(it, referralLimit, screeningRetryAttempts)
        }

    val referralDayLimitLiveData = MutableLiveData<Int>()
    var screeningRetryAttempts: Int = 5
    val addCallHistoryLiveData = MutableLiveData<Resource<Boolean>>()

    var callType: String? = null
    var isSuccessful: Boolean? = null
    var callResultStatus: FollowUpCallStatus = FollowUpCallStatus.SUCCESSFUL
    var visitRejectReason: String? = null
    var otherVisitRejectReason: String? = null
    var unSuccessfulCallReason: String? = null
    var isWillingToVisitUHC: Boolean? = null
    var callStartTime: Long? = null
    var callEndTime: Long? = null

    init {
        SecuredPreference.getFollowUpCriteria()?.let { followUpCriteria ->
            referralDayLimitLiveData.postValue(followUpCriteria.referral)
            screeningRetryAttempts = followUpCriteria.screeningRetryAttempts
        }

        viewModelScope.launch {
            createNewFollowUpFilter(0)
        }
        observeSearch {
            updateFollowUpFilter(search = it)
        }
    }

    fun createNewFollowUpFilter(pageType: Int) {
        val filter = FollowUpFilter(type = getFollowUpType(pageType))
        filterLiveData.postValue(filter)
    }

    fun updateFollowUpFilter(
        pageType: Int? = null,
        search: String? = null,
        selectedVillages: List<ChipViewItemModel>? = null,
        selectedDateRange: List<ChipViewItemModel>? = null,
        selectedReferralReasons: List<ChipViewItemModel>? = null,
        ncdSelectedReason: List<ChipViewItemModel>? = null,
        ncdSelectedReferralTo: List<ChipViewItemModel>? = null,
        fromDate: String? = null,
        toDate: String? = null,
        selectedShashthyaShebikas: List<ChipViewItemModel>? = null,
        remainingAttempt: Int? = null,
        callStatus: String? = null,
        updateRemainingAttempt: Boolean = false,
        updateCallStatus: Boolean = false,
    ) {
        val filter = filterLiveData.value ?: FollowUpFilter()
        filter.apply {
            // Update Page
            pageType?.let {
                this.type = getFollowUpType(it)
            }

            // Update search
            search?.let {
                this.search = it
            }

            selectedShashthyaShebikas?.let {
                this.selectedShashtyaShebikas = it
            }

            // Update Village Ids
            selectedVillages?.let {
                this.selectedVillages = it
            }

            selectedDateRange?.let {
                this.selectedDateRange = it
                this.fromDate = ""
                this.toDate = ""
            }

            selectedReferralReasons?.let {
                this.selectedReferralReasons = it
            }

            ncdSelectedReason?.let {
                this.ncdSelectedReasons = it
            }

            ncdSelectedReferralTo?.let {
                this.ncdSelectedReferralTo = it
            }

            // Update Date Filter
            fromDate?.let {
                this.fromDate = it
            }

            toDate?.let {
                this.toDate = it
            }

            if (updateRemainingAttempt) {
                this.remainingAttempt = remainingAttempt
            }

            if (updateCallStatus) {
                this.callStatus = callStatus
            }

            filterLiveData.value = this
        }
    }

    private fun getFollowUpType(type: Int): String =
        when (type) {
            1 -> FU_TYPE_REFERRED
            2 -> FU_TYPE_MEDICAL_REVIEW
            else -> FU_TYPE_HH_VISIT
        }

    fun getFilterData(): FollowUpFilter? = filterLiveData.value

    fun getFilterDataLiveData(): LiveData<FollowUpFilter> = filterLiveData

    fun getDateRange(): List<String> =
        listOf(
            FollowUpDefinedParams.FILTER_TODAY,
            FollowUpDefinedParams.FILTER_TOMORROW,
            FollowUpDefinedParams.FILTER_CUSTOMIZE,
        )

    fun getReferralReasons(): List<String> =
        listOf(
            FollowUpDefinedParams.FILTER_ANC,
            FollowUpDefinedParams.FILTER_PNC,
            FollowUpDefinedParams.FILTER_CHILD_HEALTH,
            FollowUpDefinedParams.FILTER_NCD,
        )

    fun getNCDReason() =
        listOf(
            ChipViewItemModel(
                name = context.getString(R.string.high_bp),
                type = FollowUpDefinedParams.HIGH_BP,
            ),
            ChipViewItemModel(
                name = context.getString(R.string.high_bg),
                type = FollowUpDefinedParams.HIGH_BG,
            ),
            ChipViewItemModel(
                name = context.getString(R.string.both),
                type = FollowUpDefinedParams.BOTH,
            ),
        )

    fun getNcdReferralFacility() =
        listOf(
            ChipViewItemModel(
                name = context.getString(R.string.community_clinic),
                type = FACILITY_TYPE_COMMUNITY_CLINIC,
            ),
            ChipViewItemModel(
                name = context.getString(R.string.upazilla_health_complex),
                type = FACILITY_TYPE_UPAZILA,
            ),
        )

    fun addCallHistory() {
        viewModelScope.launch(dispatcherIO) {
            selectedFollowUpDetail?.let {
                addCallHistoryLiveData.postLoading()
                val wrongNumber = if (!CommonUtils.isHealthScreener()) {
                    unSuccessfulCallReason?.equals(
                        DefinedParams.WRONG_NUMBER,
                        ignoreCase = true,
                    ) == true
                } else {
                    callResultStatus == FollowUpCallStatus.WRONG_NUMBER
                }
                followUpRepository.addCallHistory(
                    it.id,
                    callType,
                    callResultStatus,
                    isWillingToVisitUHC,
                    visitRejectReason,
                    otherVisitRejectReason,
                    unSuccessfulCallReason,
                    wrongNumber,
                    calculateTotalTimeTaken(),
                    screeningRetryAttempts,
                )
                setAnalyticsFollowUpData(
                    it.id,
                    it.patientId,
                    callResultStatus,
                    it.patientStatus,
                    visitRejectReason ?: unSuccessfulCallReason,
                    SecuredPreference.getString(DefinedParams.FollowUpStartTiming),
                )
                addCallHistoryLiveData.postSuccess(true)
            }
        }
    }

    /**
     * Calculate total time taken during the call in miutes
     */
    private fun calculateTotalTimeTaken(): Double? =
        callStartTime?.let { startTime ->
            val endTime = callEndTime ?: System.currentTimeMillis()
            val durationInMillis = endTime - startTime
            durationInMillis / TimeUnit.MINUTES.toMillis(1).toDouble()
        }
}
