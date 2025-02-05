package com.medtroniclabs.opensource.ui.followup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.FollowUpPatientModel
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.data.offlinesync.model.FollowUpCallStatus
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.followup.FollowUpFilter
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.FollowUpRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowUpViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val followUpRepository: FollowUpRepository
) : BaseViewModel(dispatcherIO) {

    val callResultHashMap = HashMap<String, Any>()
    val patientStatusHashMap = HashMap<String, Any>()
    val unSuccessfulHashMap = HashMap<String, Any>()
    var selectedFollowUpDetail: FollowUpPatientModel? = null

    private val villages = mutableListOf<VillageEntity>()
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

    init {
        SecuredPreference.getFollowUpCriteria()?.let { followUpCriteria ->
            referralDayLimitLiveData.postValue(followUpCriteria.referral)
            maxSuccessfulCallLimit = followUpCriteria.successfulAttempts
            maxUnSuccessfulCallLimit = followUpCriteria.unsuccessfulAttempts
        }

        viewModelScope.launch {
            villages.addAll(followUpRepository.getVillageIds())
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
        fromDate: String? = null,
        toDate: String? = null
    ) {
        val filter = filterLiveData.value ?: FollowUpFilter()
        filter.apply {

            //Update Page
            pageType?.let {
                this.type = getFollowUpType(it)
            }

            //Update search
            search?.let {
                this.search = it
            }

            //Update Village Ids
            selectedVillages?.let {
                this.selectedVillages = it
            }

            selectedDateRange?.let {
                this.selectedDateRange = it
                this.fromDate = ""
                this.toDate = ""
            }

            //Update Date Filter
            fromDate?.let {
                this.fromDate = it
            }

            toDate?.let {
                this.toDate = it
            }

            filterLiveData.value = this
        }
    }

    private fun getFollowUpType(type: Int): String {
        return when (type) {
            1 -> FU_TYPE_REFERRED
            2 -> FU_TYPE_MEDICAL_REVIEW
            else -> FU_TYPE_HH_VISIT
        }
    }

    fun getVillages(): List<VillageEntity> {
        return villages
    }

    fun getFilterData(): FollowUpFilter? {
        return filterLiveData.value
    }

    fun getFilterDataLiveData(): LiveData<FollowUpFilter> {
        return filterLiveData
    }

    fun getDateRange(): List<String> {
        return listOf(
            FollowUpDefinedParams.FilterToday,
            FollowUpDefinedParams.FilterTomorrow,
            FollowUpDefinedParams.FilterCustomize
        )
    }

    fun addCallHistory() {
        viewModelScope.launch(dispatcherIO) {
            selectedFollowUpDetail?.let {
                addCallHistoryLiveData.postLoading()
                val callStatus =
                    getCallStatus(callResultHashMap[DefinedParams.CallResult] as String)
                val patientStatus=getPatientStatus(callStatus)
                val unSuccessfulReason= getUnSuccessfulReason(callStatus)
                followUpRepository.addCallHistory(
                    maxSuccessfulCallLimit,
                    maxUnSuccessfulCallLimit,
                    it.id,
                    callStatus,
                    patientStatus,
                    unSuccessfulReason
                )
                setAnalyticsFollowUpData(it.id,it.patientId,callStatus,patientStatus,
                    unSuccessfulReason,SecuredPreference.getString(DefinedParams.FollowUpStartTiming)
                )
                addCallHistoryLiveData.postSuccess(true)
            }
        }
    }

    private fun getCallStatus(status: String) : FollowUpCallStatus {
        if (status == FollowUpCallStatus.SUCCESSFUL.name)
            return FollowUpCallStatus.SUCCESSFUL
        return FollowUpCallStatus.UNSUCCESSFUL
    }

    private fun getPatientStatus(status: FollowUpCallStatus): String? {
        if (status == FollowUpCallStatus.SUCCESSFUL && patientStatusHashMap.isNotEmpty()) {
            return (patientStatusHashMap[DefinedParams.PatientStatus] as String)
        }

        return null
    }

    private fun getUnSuccessfulReason(status: FollowUpCallStatus): String? {
        if (status == FollowUpCallStatus.UNSUCCESSFUL) {
            return (unSuccessfulHashMap[DefinedParams.UnSuccessful] as String)
        }
        return null
    }
}