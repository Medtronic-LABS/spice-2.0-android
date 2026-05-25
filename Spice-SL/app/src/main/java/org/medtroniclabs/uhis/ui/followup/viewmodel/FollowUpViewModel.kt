package org.medtroniclabs.uhis.ui.followup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.FollowUpPatientModel
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.followup.FollowUpFilter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.repo.FollowUpRepository
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
import javax.inject.Inject

@HiltViewModel
class FollowUpViewModel @Inject constructor(
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val followUpRepository: FollowUpRepository,
) : BaseViewModel(dispatcherIO) {
    val callResultHashMap = HashMap<String, Any>()
    val patientStatusHashMap = HashMap<String, Any>()
    val unSuccessfulHashMap = HashMap<String, Any>()
    var selectedFollowUpDetail: FollowUpPatientModel? = null

    private val villages = mutableListOf<SubVillageEntity>()
    private val filterLiveData = MutableLiveData<FollowUpFilter>()
    val followUpPatientListLiveData: LiveData<List<FollowUpPatientModel>> =
        filterLiveData.switchMap {
            val referralLimit = referralDayLimitLiveData.value ?: 2
            followUpRepository.getFollowUpListLiveData(it, referralLimit)
        }

    val referralDayLimitLiveData = MutableLiveData<Int>()
    var maxSuccessfulCallLimit: Int = 5
    private var maxUnSuccessfulCallLimit: Int = 5
    val addCallHistoryLiveData = MutableLiveData<Resource<Boolean>>()
    var informedCallAttempts: Int = 5

    init {
        SecuredPreference.getFollowUpCriteria()?.let { followUpCriteria ->
            referralDayLimitLiveData.postValue(followUpCriteria.referral)
            maxSuccessfulCallLimit = followUpCriteria.successfulAttempts
            maxUnSuccessfulCallLimit = followUpCriteria.unsuccessfulAttempts
            informedCallAttempts = followUpCriteria.informedCallAttempts
        }

        viewModelScope.launch {
            villages.addAll(followUpRepository.getSubVillages())
            createNewFollowUpFilter(0)
        }
    }

    fun createNewFollowUpFilter(pageType: Int) {
        val filter =
            FollowUpFilter(type = getFollowUpType(pageType), villages = villages.map { it.id })
        filterLiveData.postValue(filter)
    }

    fun updateFollowUpFilter(
        pageType: Int? = null,
        search: String? = null,
        selectedVillages: List<ChipViewItemModel>? = null,
        selectedDateRange: List<ChipViewItemModel>? = null,
        selectedReasons: List<ChipViewItemModel>? = null,
        fromDate: String? = null,
        toDate: String? = null,
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

            // Update Village Ids
            selectedVillages?.let {
                this.selectedVillages = it
            }

            selectedDateRange?.let {
                this.selectedDateRange = it
                this.fromDate = ""
                this.toDate = ""
            }

            selectedReasons?.let {
                this.selectedReasons = it
            }

            // Update Date Filter
            fromDate?.let {
                this.fromDate = it
            }

            toDate?.let {
                this.toDate = it
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

    fun getVillages(): List<SubVillageEntity> = villages

    fun getFilterData(): FollowUpFilter? = filterLiveData.value

    fun getFilterDataLiveData(): LiveData<FollowUpFilter> = filterLiveData

    fun getDateRange(): List<String> =
        listOf(
            FollowUpDefinedParams.FilterToday,
            FollowUpDefinedParams.FilterTomorrow,
            FollowUpDefinedParams.FilterCustomize,
        )

    fun getReferralReasons(): List<String> =
        listOf(
            FollowUpDefinedParams.FilterMalaria,
            FollowUpDefinedParams.FilterFever,
            FollowUpDefinedParams.FilterDiarrhoea,
            FollowUpDefinedParams.FilterANC,
            FollowUpDefinedParams.FilterPNC,
            FollowUpDefinedParams.FilterPneumonia,
            FollowUpDefinedParams.FilterCough,
            FollowUpDefinedParams.FilterGeneralDangerSigns,
            FollowUpDefinedParams.FilterMUAC,
            FollowUpDefinedParams.FilterTBSymptoms,
            FollowUpDefinedParams.FilterNCD,
            FollowUpDefinedParams.FilterFPConsult,
        )

    fun addCallHistory() {
        viewModelScope.launch(dispatcherIO) {
            selectedFollowUpDetail?.let {
                addCallHistoryLiveData.postLoading()
                val callStatus =
                    getCallStatus(callResultHashMap[DefinedParams.CALL_RESULT] as String)
                val patientStatus = getPatientStatus(callStatus)
                val unSuccessfulReason = getUnSuccessfulReason(callStatus)
                followUpRepository.addCallHistory(
                    maxSuccessfulCallLimit,
                    maxUnSuccessfulCallLimit,
                    informedCallAttempts,
                    it.id,
                    callStatus,
                    patientStatus,
                    unSuccessfulReason,
                )
                setAnalyticsFollowUpData(
                    it.id,
                    it.patientId,
                    callStatus,
                    patientStatus,
                    unSuccessfulReason,
                    SecuredPreference.getString(DefinedParams.FollowUpStartTiming),
                )
                addCallHistoryLiveData.postSuccess(true)
            }
        }
    }

    private fun getCallStatus(status: String): FollowUpCallStatus {
        if (status == FollowUpCallStatus.SUCCESSFUL.name) {
            return FollowUpCallStatus.SUCCESSFUL
        }
        return FollowUpCallStatus.UNSUCCESSFUL
    }

    private fun getPatientStatus(status: FollowUpCallStatus): String? {
        if (status == FollowUpCallStatus.SUCCESSFUL && patientStatusHashMap.isNotEmpty()) {
            return (patientStatusHashMap[DefinedParams.PATIENT_STATUS] as String)
        }

        return null
    }

    private fun getUnSuccessfulReason(status: FollowUpCallStatus): String? {
        if (status == FollowUpCallStatus.UNSUCCESSFUL) {
            return (unSuccessfulHashMap[DefinedParams.UN_SUCCESSFUL] as String)
        }
        return null
    }
}
