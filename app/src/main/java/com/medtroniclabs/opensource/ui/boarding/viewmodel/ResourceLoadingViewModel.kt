package com.medtroniclabs.opensource.ui.boarding.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.DeviceDetails
import com.medtroniclabs.opensource.network.DeviceInformation
import com.medtroniclabs.opensource.ncd.followup.repo.NCDFollowUpRepo
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.OfflineSyncRepository
import com.medtroniclabs.opensource.ui.boarding.repo.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourceLoadingViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val connectivityManager: ConnectivityManager,
    private val followUpRepo: NCDFollowUpRepo,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {

    val metaDataCompleteLiveData = MutableLiveData<Resource<Boolean>>()
    val deviceDetailsLiveData = MutableLiveData<Resource<DeviceDetails>>()
    val householdsLiveData = MutableLiveData<Resource<Boolean>>()
    val ncdFollowUpLiveData = MutableLiveData<Resource<Boolean>>()

    private val workflowNames = mutableListOf<Long>()
    private val meta = mutableListOf<String>()
    private val syncDelay = 20 * 1000L // 20 Sec
    var changeFacility = false


    fun getMetaDataInformation() {
        viewModelScope.launch(dispatcherIO) {
            metaDataCompleteLiveData.postLoading()
            if (!connectivityManager.isNetworkAvailable()) {
                metaDataCompleteLiveData.postError()
                return@launch
            }
            metaDataCompleteLiveData.postValue(
                metaRepository.getMetaDataInformation(
                    workflowNames,
                    meta,
                    changeFacility
                )
            )
        }
    }

    fun updateDeviceDetails(context: Context) {
        viewModelScope.launch(dispatcherIO) {
            deviceDetailsLiveData.postLoading()
            if (!connectivityManager.isNetworkAvailable()) {
                deviceDetailsLiveData.postError()
                return@launch
            }
            deviceDetailsLiveData.postValue(
                metaRepository.updateDeviceDetails(DeviceInformation.getDeviceDetails(context))
            )
        }
    }

    fun downloadInitialDetails() {
        viewModelScope.launch(dispatcherIO) {
            householdsLiveData.postLoading()

            //1. Update status for old request Id
            if (!getSyncStatus()) {
                householdsLiveData.postError()
                return@launch
            }

            // 2. Check Village check
            if (!checkAndProceedVillageChange()) {
                return@launch
            }

            // 2. Get Fetch sync
            offlineSyncRepository.getInsertOrUpdateLocalData(householdsLiveData)
        }
    }

    private suspend fun getSyncStatus(): Boolean {
        val requestIds =
            SecuredPreference.getStringArray(SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name)
        if (requestIds.isNullOrEmpty()) {
            return true
        }

        val uuid = requestIds[0]

        repeat(4) {
            if (offlineSyncRepository.getSyncStatusForOffline(uuid)) {
                SecuredPreference.remove(SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name)
                return true
            }
            delay(syncDelay)
        }

        return false
    }

    private suspend fun checkAndProceedVillageChange(): Boolean {
        val prefKey = SecuredPreference.EnvironmentKey.VILLAGE_IDS.name
        val oldVillageIds = SecuredPreference.getLongList(prefKey)
        val newVillageIds = metaRepository.getAllVillageIds()
        if (oldVillageIds.isNotEmpty()) {
            val changedVillage = oldVillageIds.subtract(newVillageIds.toSet())
            val newlyAddedVillage = newVillageIds.subtract(oldVillageIds.toSet())
            // Existing village changed remove all data and fresh download for all villages
            if (changedVillage.isNotEmpty()) {
                // Village changed post local changes
                val requestIds = offlineSyncRepository.postOfflineUnSyncedChangesWithMutex()
                if (requestIds == null) {
                    householdsLiveData.postError()
                    return false
                }
                SecuredPreference.remove(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
            } else if (newlyAddedVillage.isNotEmpty()) {
                if (!offlineSyncRepository.fetchSyncedData(newlyAddedVillage.toList())) {
                    householdsLiveData.postError()
                    return false
                }
            }
        }

        SecuredPreference.saveLongList(prefKey, newVillageIds)
        return true
    }


    fun downloadTheFollowUpData() {
        viewModelScope.launch(dispatcherIO) {
            ncdFollowUpLiveData.postLoading()
            val prefKey = SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS.name
            val villageIds = SecuredPreference.getLongList(prefKey)
            // 2. Check Village check
            if (villageIds.isEmpty()) {
                return@launch
            }
            // 2. Get Fetch sync
            followUpRepo.getNcdFollowUpData(ncdFollowUpLiveData)
        }
    }

    fun syncCallResultDetails() {
        viewModelScope.launch(dispatcherIO) {
            followUpRepo.createCallDetails()
        }
    }

}