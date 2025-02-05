package com.medtroniclabs.opensource.offlinesync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medtroniclabs.opensource.appextensions.hideNotification
import com.medtroniclabs.opensource.appextensions.showNotification
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.followup.repo.NCDFollowUpRepo
import com.medtroniclabs.opensource.repo.OfflineSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class ScheduledSyncWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted userParameter: WorkerParameters,
    val roomHelper: RoomHelper,
    val offlineSyncRepository: OfflineSyncRepository,
    private val followUpRepo: NCDFollowUpRepo,
) : CoroutineWorker(context, userParameter) {

    //For schedule
    //private val syncDelay = 40 * 1000L // 40 Sec

    //For automatic
    private val syncDelay = 10 * 1000L // 40 Sec

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (CommonUtils.isCommunity()) {
            context.showNotification()

            //1. Update status for old request Id
            if (!getSyncStatus()) {
                context.hideNotification()
                return Result.failure()
            }

            //2. Post Local changes and Get Status
            if (!postLocalChanges()) {
                context.hideNotification()
                return Result.failure()
            }

            //3. Fetch Sync data with last synced at
            context.hideNotification()
            return if (fetchSyncedData())
                Result.success()
            else
                Result.failure()
        } else {
            context.showNotification()
            //1. Update status for old request Id
            if (!getSyncStatusForNCD()) {
                context.hideNotification()
                return Result.failure()
            }
            if (!postLocalChangesNcd()) {
                context.hideNotification()
                return Result.failure()
            }
            return if (fetchNCDFollowUpData()) {
                Result.success()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun getSyncStatusForNCD(): Boolean {
        val requestIds =
            SecuredPreference.getStringArray(SecuredPreference.EnvironmentKey.OFFLINE_FOLLOW_UP_SYNC_REQUEST_ID.name)
        if (requestIds.isNullOrEmpty()) {
            return true
        }

        val uuid = requestIds[0]

        repeat(4) {
            if (followUpRepo.getSyncStatusForOffline(uuid)) {
                SecuredPreference.remove(SecuredPreference.EnvironmentKey.OFFLINE_FOLLOW_UP_SYNC_REQUEST_ID.name)
                return true
            }
            delay(syncDelay)
        }

        return false
    }
    private suspend fun postLocalChangesNcd(): Boolean {
        val value = followUpRepo.createCallDetails()

        //Save request id in Preference
        if (value && !SecuredPreference.getStringArray(SecuredPreference.EnvironmentKey.OFFLINE_FOLLOW_UP_SYNC_REQUEST_ID.name)
                .isNullOrEmpty()
        ) {
            // Get Status for new request id
            delay(syncDelay)
            if (!getSyncStatusForNCD()) {
                return false
            }
        }
        return true
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

    private suspend fun postLocalChanges(): Boolean {
        val requestIds = offlineSyncRepository.postOfflineUnSyncedChangesWithMutex() ?: return false

        //Save request id in Preference
        if (requestIds.isNotEmpty()) {
            SecuredPreference.saveStringArray(
                SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name,
                requestIds.toTypedArray()
            )

            // Get Status for new request id
            delay(syncDelay)
            if (!getSyncStatus()) {
                return false
            }

            /*Upload images here*/
            offlineSyncRepository.uploadAllSignatures()
        }

        return true
    }

    private suspend fun fetchSyncedData(): Boolean {
        SecuredPreference.remove(SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name)
        val villageIds = roomHelper.getAllVillageIds()
        val lastSyncedAt = SecuredPreference.getString(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
        return offlineSyncRepository.fetchSyncedData(villageIds, lastSyncedAt)
    }

    private suspend fun fetchNCDFollowUpData(): Boolean {
        val prefKey = SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS.name
        val villageIds = SecuredPreference.getLongList(prefKey)
        // 2. Check Village check
        if (villageIds.isEmpty()) {
            return false
        }
        val lastSyncedAt =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LAST_SYNCED.name)
        return followUpRepo.fetchSyncNcdFollowUpData(villageIds, lastSyncedAt)
    }
}