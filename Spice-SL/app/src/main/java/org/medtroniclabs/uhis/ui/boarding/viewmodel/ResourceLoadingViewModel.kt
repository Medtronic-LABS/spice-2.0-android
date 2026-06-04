package org.medtroniclabs.uhis.ui.boarding.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.ncd.data.DeviceDetails
import org.medtroniclabs.uhis.ncd.followup.repo.NCDFollowUpRepo
import org.medtroniclabs.uhis.network.DeviceInformation
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.OfflineSyncRepository
import org.medtroniclabs.uhis.ui.boarding.ResourceLoadingSyncProgress
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import javax.inject.Inject

@HiltViewModel
class ResourceLoadingViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val connectivityManager: ConnectivityManager,
    private val followUpRepo: NCDFollowUpRepo,
    @param:IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    val oldUserDataSync = MutableLiveData<Resource<List<String>?>>()
    val metaDataCompleteLiveData = MutableLiveData<Resource<Boolean>>()
    val deviceDetailsLiveData = MutableLiveData<Resource<DeviceDetails>>()
    val householdsLiveData = MutableLiveData<Resource<Boolean>>()
    val ncdFollowUpLiveData = MutableLiveData<Resource<Boolean>>()

    /** 0–100 cumulative sync progress for the resource loading screen. */
    val syncProgressPercent = MutableLiveData(0)

    private val syncDelay = 20 * 1000L // 20 Sec
    var changeFacility = false

    /** Single callback for repositories; avoids repeating `{ …(it) }` at every call site. */
    private val onSyncProgress: (Int) -> Unit = { percent ->
        syncProgressPercent.postValue(percent.coerceIn(0, 100))
    }

    fun syncOldUserData() {
        viewModelScope.launch(dispatcherIO) {
            oldUserDataSync.postLoading()
            try {
                val requestIds = offlineSyncRepository.postOfflineUnSyncedChangesWithMutex(OfflineConstant.SYNC_MODE_MANUAL)
                oldUserDataSync.postSuccess(requestIds)
            } catch (e: Exception) {
                oldUserDataSync.postSuccess(null)
            }
        }
    }

    fun getMetaDataInformation() {
        viewModelScope.launch(dispatcherIO) {
            metaDataCompleteLiveData.postLoading()
            onSyncProgress(ResourceLoadingSyncProgress.USER_DATA_REQUEST_PENDING)
            if (!connectivityManager.isNetworkAvailable()) {
                onSyncProgress(0)
                metaDataCompleteLiveData.postError()
                return@launch
            }
            metaDataCompleteLiveData.postValue(
                metaRepository.getMetaDataInformation(
                    changeFacility,
                    onSyncProgress,
                ),
            )
        }
    }

    fun updateDeviceDetails(context: Context) {
        viewModelScope.launch(dispatcherIO) {
            deviceDetailsLiveData.postLoading()
            onSyncProgress(ResourceLoadingSyncProgress.USER_DATA_REQUEST_PENDING)
            if (!connectivityManager.isNetworkAvailable()) {
                onSyncProgress(0)
                deviceDetailsLiveData.postError()
                return@launch
            }
            deviceDetailsLiveData.postValue(
                metaRepository.updateDeviceDetails(DeviceInformation.getDeviceDetails(context)),
            )
        }
    }

    fun downloadInitialDetails() {
        viewModelScope.launch(dispatcherIO) {
            householdsLiveData.postLoading()

            if (!getSyncStatus()) {
                householdsLiveData.postError()
                return@launch
            }
            onSyncProgress(ResourceLoadingSyncProgress.SYNC_STATUS_COMPLETE)

            if (!checkAndProceedVillageChange()) {
                return@launch
            }
            onSyncProgress(ResourceLoadingSyncProgress.VILLAGE_CHECK_COMPLETE)

            offlineSyncRepository.getInsertOrUpdateLocalData(householdsLiveData, onSyncProgress)
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
        val newVillageIds = metaRepository.getAllSubVillageIds()
        if (oldVillageIds.isNotEmpty()) {
            val changedVillage = oldVillageIds.subtract(newVillageIds.toSet())
            val newlyAddedVillage = newVillageIds.subtract(oldVillageIds.toSet())
            // Existing village changed remove all data and fresh download for all villages
            if (changedVillage.isNotEmpty()) {
                // Village changed post local changes
                val requestIds = offlineSyncRepository.postOfflineUnSyncedChangesWithMutex(OfflineConstant.SYNC_MODE_INITIAL)
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
                onSyncProgress(ResourceLoadingSyncProgress.PARTIAL_VILLAGE_SYNC_DONE)
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
            followUpRepo.getNcdFollowUpData(ncdFollowUpLiveData, onSyncProgress)
        }
    }

    fun syncCallResultDetails() {
        viewModelScope.launch(dispatcherIO) {
            followUpRepo.createCallDetails()
        }
    }

    fun markSyncProgressComplete() {
        onSyncProgress(ResourceLoadingSyncProgress.LOCAL_PERSIST_COMPLETE)
    }
}
