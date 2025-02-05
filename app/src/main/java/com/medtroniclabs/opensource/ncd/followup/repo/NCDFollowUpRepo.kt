package com.medtroniclabs.opensource.ncd.followup.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DateUtils.convertToTimestamp
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.RequestGetSyncStatus
import com.medtroniclabs.opensource.data.offlinesync.model.SyncResponse
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineConstant
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineUtils
import com.medtroniclabs.opensource.data.resource.RequestAllEntities
import com.medtroniclabs.opensource.db.entity.ResponseNCDFollowUp
import com.medtroniclabs.opensource.db.entity.NCDCallDetails
import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.db.entity.NCDFollowUpDownload
import com.medtroniclabs.opensource.db.entity.NCDFollowUpRequestCreate
import com.medtroniclabs.opensource.db.entity.NCDPatientDetailsEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.data.FollowUpUpdateRequest
import com.medtroniclabs.opensource.ncd.data.RegisterCallResponse
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import java.lang.reflect.Type
import javax.inject.Inject

class NCDFollowUpRepo @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {
    suspend fun getPatientCallRegister(): Resource<RegisterCallResponse> {
        return try {
            val response = apiHelper.getPatientCallRegister()
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun updatePatientCallRegister(request: FollowUpUpdateRequest): Resource<HashMap<String, Any>> {
        return try {
            val response = apiHelper.updatePatientCallRegister(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, response.body()?.entity)
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun fetchSyncNcdFollowUpData(
        villageIds: List<Long> = emptyList(),
        serverLastSyncedAt: String? = null
    ): Boolean {
        return try {
            val syncedResponse = getSyncedNcdFollowUpEntities(villageIds, serverLastSyncedAt)
            if (syncedResponse.isSuccessful) {
                syncedResponse.body()?.string()?.let { response ->
                    // Deserialize the JSON response into the NCDFollowUpDownload model
                    val type: Type = object : TypeToken<NCDFollowUpDownload>() {}.type
                    val responseInitialDownload: NCDFollowUpDownload? = Gson().fromJson(response, type)
                    // Save the deserialized follow-up data
                    responseInitialDownload?.let { saveNcdFollowUpData(it) } ?: false
                } ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.d("Exception: ${e.localizedMessage}")
            false
        }
    }
    suspend fun getNcdFollowUpData(liveData: MutableLiveData<Resource<Boolean>>) {
        // Retrieve all village IDs from the local database
        val prefKey = SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS.name
        val villageIds = SecuredPreference.getLongList(prefKey)

        // Fetch synced data from the server
        val lastSyncedAt =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LAST_SYNCED.name)
        val isInitialDataSuccess = if (lastSyncedAt == null) {
            fetchSyncNcdFollowUpData(villageIds, null)
        } else {
            fetchSyncNcdFollowUpData(villageIds, lastSyncedAt)
        }

        // Placeholder: Add logic to handle unsynced data download
        /* if (!fetchUnSyncedData()) {
             liveData.postError("Something went wrong")
         }*/

        // Update live data based on the success of data fetching
        if (isInitialDataSuccess) {
            liveData.postSuccess(true)
        } else {
            liveData.postError("Something went wrong")
        }
    }


    private suspend fun saveNcdFollowUpData(responseInitialDownload: NCDFollowUpDownload): Boolean {
        return try {
            if (SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS_ALTER.name)) {
                roomHelper.deleteAllNCDFollowUp()
                roomHelper.deleteAllNCDPatientDetails()
                SecuredPreference.putBoolean(SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS_ALTER.name,false)
            }
            // Save follow-up data if it is not empty
            responseInitialDownload.patientDetails?.let {
                it.forEach { data ->
                    val request = data.id?.let { id ->
                        NCDPatientDetailsEntity(
                            id = id,
                            patientDetails = Gson().toJson(data)
                        )
                    }
                    if (request != null) {
                        roomHelper.insertNCDPatientDetails(request)
                    }
                }
            }
            responseInitialDownload.followUps?.let { followUps ->
                val mappedFollowUps = followUps.map { it.toNCDFollowUp() }
                mappedFollowUps.forEach { roomHelper.insertNCDFollowUp(it) }
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LAST_SYNCED.name,
                    responseInitialDownload.lastSyncTime
                )
                responseInitialDownload.followUpCriteria?.let {
                    SecuredPreference.putInt(
                        SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_ATTEMPTS.name,
                        it.followupAttempts ?: 5
                    )
                    SecuredPreference.putInt(
                        SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_SCREENING_REMAINING_DAYS.name,
                        it.screeningFollowupRemainingDays ?: 5
                    )
                    SecuredPreference.putInt(
                        SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_ASSESSMENT_REMAINING_DAYS.name,
                        it.assessmentFollowupRemainingDays ?: 7
                    )
                    SecuredPreference.putInt(
                        SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_MEDICAL_REVIEW_REMAINING_DAYS.name,
                        it.medicalReviewFollowupRemainingDays ?: 30
                    )
                    SecuredPreference.putInt(
                        SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LOST_REMAINING_DAYS.name,
                        it.lostToFollowupRemainingDays ?: 90
                    )
                }
                true
            } ?: false
        } catch (e: Exception) {
            Timber.d("Exception while saving data: ${e.localizedMessage}")
            false
        }
    }

    private fun ResponseNCDFollowUp.toNCDFollowUp(): NCDFollowUp {
        return NCDFollowUp(
            id = this.id,
            deleted = this.deleted,
            isCompleted = this.isCompleted,
            isWrongNumber = this.isWrongNumber,
            isInitiated = this.isInitiated,
            patientId = this.patientId,
            memberId = this.memberId,
            type = this.type,
            referredSiteId = this.referredSiteId,
            identityType = this.identityType,
            identityValue = this.identityValue,
            name = this.name,
            gender = this.gender,
            dateOfBirth = this.dateOfBirth,
            phoneNumber = this.phoneNumber,
            countyName = this.countyName,
            subCountyName = this.subCountyName,
            communityHealthUnitName = this.communityHealthUnitName,
            villageId = this.villageId,
            villageName = this.villageName,
            landmark = this.landmark,
            retryAttempts = this.retryAttempts,
            overDueCategories = this.overDueCategories,
            dueDate = convertToTimestamp(this.dueDate),
            referredReasons = this.referredReasons,
            createdBy = this.createdBy,
            updatedBy = this.updatedBy,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private suspend fun getSyncedNcdFollowUpEntities(
        villageList: List<Long>,
        lastSyncedAt: String? = null
    ): Response<ResponseBody> {
        // Build the request for fetching synced entities
        val request = RequestAllEntities(villageList, lastSyncedAt)
        return apiHelper.fetchSyncedData(request)
    }

    suspend fun createCallDetails(): Boolean {
        val request = OfflineUtils.getRequestObject()
        return try {
            val requestData = roomHelper.getAllNCDCallDetails()
            if (requestData.isNotEmpty()) {
                val requestCreate = convertToNCDFollowUpRequestCreate(requestData)
                request["followUps"] = requestCreate
                val apiResponse = apiHelper.postOfflineSync(request)
                if (apiResponse.isSuccessful) {
                    SecuredPreference.saveStringArray(
                        SecuredPreference.EnvironmentKey.OFFLINE_FOLLOW_UP_SYNC_REQUEST_ID.name,
                        arrayOf(request[OfflineConstant.REQUEST_ID] as String)
                    )
                }
                true
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun convertToNCDFollowUpRequestCreate(callDetailsList: List<NCDCallDetails>): List<NCDFollowUpRequestCreate> {
        return callDetailsList
            .groupBy { it.id } // Group by the `id` field
            .map { (id, details) ->
                val firstDetail = details.firstOrNull() // Take the first element as a base
                NCDFollowUpRequestCreate(
                    id = id,
                    patientId = firstDetail?.patientId,
                    memberId = firstDetail?.memberId, // Assuming memberId is not available in NCDCallDetails
                    type = firstDetail?.type,
                    createdAt = firstDetail?.createdAt,
                    updatedAt = firstDetail?.updatedAt,
                    createdBy = firstDetail?.createdBy,
                    updatedBy = firstDetail?.updatedBy,
                    isInitiated = false, // Default value as per the class definition
                    followUpDetails = details // Use all grouped elements as `followUpDetails`
                )
            }
    }

    fun getNCDFollowUpData(
        type: String,
        searchText: String,
        dateBasedOnChip: Pair<Long?, Long?>?,
        isScreened: Boolean?,
        reason:String?
    ): LiveData<List<NCDFollowUp>> =
        roomHelper.getNCDFollowUpData(
            type, searchText, dateBasedOnChip, isScreened,
            reason
        )

    suspend fun updatedCallInitiatedCall(id: NCDFollowUp) = roomHelper.updatedCallInitiatedCall(id)

    suspend fun getNCDInitiatedCallFollowUp() =
        roomHelper.getNCDInitiatedCallFollowUp()

    suspend fun insertNCDCallDetails(followUp: NCDCallDetails):  NCDCallDetails? {
        return roomHelper.insertNCDCallDetails(followUp)
    }

    suspend fun updateRetryAttempts(id: Long, retryAttempts: Long) {
        return roomHelper.updateRetryAttempts(id, retryAttempts)
    }

    suspend fun getAttemptsById(id: Long): Long? {
        return roomHelper.getAttemptsById(id)
    }
    suspend fun getNCDFollowUpById(id: Long): NCDFollowUp {
        return roomHelper.getNCDFollowUpById(id)
    }
    suspend fun getAllVillagesName(): Resource<List<VillageEntity>> {
        val response = roomHelper.getUserVillages()
        return Resource(state = ResourceState.SUCCESS, data = response)
    }

    suspend fun getSyncStatusForOffline(id: String): Boolean {
        val req = RequestGetSyncStatus(requestId = id)
        try {
            // Get Sync Status
            val response = getSyncStatus(req)
            if (response.isSuccessful) {
                response.body()?.entityList?.forEach { entity ->
                    when (entity.status) {
                        OfflineSyncStatus.Success.name -> {
                            entity.referenceId?.toLongOrNull()?.let {
                                roomHelper.deleteCallDetails(it)
                            }
                        }

                        OfflineSyncStatus.Failed.name -> {
                        }
                    }
                }

                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    private suspend fun getSyncStatus(request: RequestGetSyncStatus): Response<SyncResponse> {
        return apiHelper.getOfflineSyncStatus(request)
    }
}