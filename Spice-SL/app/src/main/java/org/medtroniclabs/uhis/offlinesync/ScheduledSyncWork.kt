package org.medtroniclabs.uhis.offlinesync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import org.medtroniclabs.uhis.appextensions.hideNotification
import org.medtroniclabs.uhis.appextensions.showNotification
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.ncd.followup.repo.NCDFollowUpRepo
import org.medtroniclabs.uhis.repo.OfflineSyncRepository
import org.medtroniclabs.uhis.repo.RxBuddyRepository

@HiltWorker
class ScheduledSyncWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted userParameter: WorkerParameters,
    val roomHelper: RoomHelper,
    val offlineSyncRepository: OfflineSyncRepository,
    val rxBuddyRepository: RxBuddyRepository,
    private val followUpRepo: NCDFollowUpRepo,
) : CoroutineWorker(context, userParameter) {
    // For automatic
    private val syncDelay = 10 * 1000L // 10 Sec

    private val fetchDelay = 30 * 1000L // 30 Sec

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (CommonUtils.isCommunity()) {
            context.showNotification()

            // 1. Update status for old request Id
            if (!getSyncStatus()) {
                context.hideNotification()
                return Result.failure()
            }

            // 2. Post Local changes and Get Status
            if (!postLocalChanges()) {
                context.hideNotification()
                return Result.failure()
            }

            delay(fetchDelay)

            // 3. Fetch Sync data with last synced at
            context.hideNotification()
            return if (fetchSyncedData()) {
                Result.success()
            } else {
                Result.failure()
            }
        } else {
            context.showNotification()
            // 1. Update status for old request Id
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

        // Save request id in Preference
        if (value &&
            !SecuredPreference
                .getStringArray(SecuredPreference.EnvironmentKey.OFFLINE_FOLLOW_UP_SYNC_REQUEST_ID.name)
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
        val requestIds = offlineSyncRepository.postOfflineUnSyncedChangesWithMutex(OfflineConstant.SYNC_MODE_AUTOMATIC) ?: return false

        // Save request id in Preference
        if (requestIds.isNotEmpty()) {
            SecuredPreference.saveStringArray(
                SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name,
                requestIds.toTypedArray(),
            )

            // Get Status for new request id
            delay(syncDelay)
            if (!getSyncStatus()) {
                return false
            }

            /*
             * Rx Buddy Sync for new household
             * */
            /*val unSyncedRxBuddyRegister = rxBuddyRepository.getUnSyncedRxBuddyRegisterCount()
            if (unSyncedRxBuddyRegister > 0) {
                val rxBuddyRequestIds = offlineSyncRepository.postOfflineUnSyncedChangesWithMutex(OfflineConstant.SYNC_MODE_AUTOMATIC)
                if (!rxBuddyRequestIds.isNullOrEmpty()) {
                    SecuredPreference.saveStringArray(
                        SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name,
                        rxBuddyRequestIds.toTypedArray()
                    )

                    // Get Status for new request id
                    delay(syncDelay)
                    if (!getSyncStatus()) {
                        return false
                    }
                }
            }*/

            // Upload images here
//            offlineSyncRepository.uploadAllSignatures()
        }

        return true
    }

    private suspend fun fetchSyncedData(): Boolean {
        SecuredPreference.remove(SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name)
        val villageIds = roomHelper.getAllSubVillageIds()
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
