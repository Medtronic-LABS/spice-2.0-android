package org.medtroniclabs.uhis.repo

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.appextensions.IMG_FILE_NAME_EXTENSION
import org.medtroniclabs.uhis.appextensions.SIGNATURE_FOLDER
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.CBS
import org.medtroniclabs.uhis.common.DefinedParams.COMMUNITY_REGISTERED_DATE
import org.medtroniclabs.uhis.common.DefinedParams.DESCRIPTION
import org.medtroniclabs.uhis.common.DefinedParams.FollowUp
import org.medtroniclabs.uhis.common.DefinedParams.Provenance
import org.medtroniclabs.uhis.common.DefinedParams.ReferenceId
import org.medtroniclabs.uhis.common.DefinedParams.UnAssigned
import org.medtroniclabs.uhis.common.DefinedParams.VILLAGE_ID
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.RequestMemberDetails
import org.medtroniclabs.uhis.data.offlinesync.model.Assessment
import org.medtroniclabs.uhis.data.offlinesync.model.AssessmentEncounter
import org.medtroniclabs.uhis.data.offlinesync.model.CallRegisterDetail
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCriteria
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHold
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHoldMember
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberLinkCallDetails
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.offlinesync.model.RequestGetSyncStatus
import org.medtroniclabs.uhis.data.offlinesync.model.ResponseInitialDownload
import org.medtroniclabs.uhis.data.offlinesync.model.ResponseMemberDetails
import org.medtroniclabs.uhis.data.offlinesync.model.ResponseRxBuddy
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddy
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddyFollowUp
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddyMember
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddyRegister
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddyRegisterDetail
import org.medtroniclabs.uhis.data.offlinesync.model.SavedMemberDetails
import org.medtroniclabs.uhis.data.offlinesync.model.SyncResponse
import org.medtroniclabs.uhis.data.offlinesync.model.TreatmentDetails
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineUtils
import org.medtroniclabs.uhis.data.resource.RequestAllEntities
import org.medtroniclabs.uhis.db.entity.CommunityProfile
import org.medtroniclabs.uhis.db.entity.EntitiesName
import org.medtroniclabs.uhis.db.entity.EntitiesName.COMMUNITY_PROFILE
import org.medtroniclabs.uhis.db.entity.FollowUpCall
import org.medtroniclabs.uhis.db.entity.LinkHouseholdMember
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.RxBuddyDetails
import org.medtroniclabs.uhis.db.entity.TreatmentDetailsEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.mappingkey.RxBuddy.RX_BUDDY_TYPE_HOUSEHOLD_MEMBER
import org.medtroniclabs.uhis.mappingkey.RxBuddy.hadReactionToYourMedications
import org.medtroniclabs.uhis.mappingkey.RxBuddy.isSymptomsGettingWorse
import org.medtroniclabs.uhis.mappingkey.RxBuddy.rxBuddyMonitoringDates
import org.medtroniclabs.uhis.model.assessment.AssessmentDetails
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.MenuConstants.ICCM_MENU_ID
import org.medtroniclabs.uhis.ui.MenuConstants.PREGNANCY_OUTCOME
import org.medtroniclabs.uhis.ui.MenuConstants.PREGNANT_WOMEN_PROFILE
import org.medtroniclabs.uhis.ui.MenuConstants.TB_MENU_ID
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ANC
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.CHILD_MENU
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ChildHoodVisit
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PNC
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PNCNeonatal
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PNC_MOTHER_MENU
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PNC_NEONATE_KEY
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.visitNo
import org.medtroniclabs.uhis.ui.boarding.ResourceLoadingSyncProgress
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.util.Locale
import javax.inject.Inject

class OfflineSyncRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper,
) {
    private val mutex = Mutex()

    private suspend fun getUnSyncedAssessmentByPatientId(hhmId: Long): List<Assessment> = convertEntityToRequest(roomHelper.getUnSyncedAssessmentByHHMId(hhmId))

    private suspend fun getOtherUnSyncedAssessments(addedAssessmentIds: List<String>): List<Assessment> =
        convertEntityToRequest(roomHelper.getOtherUnSyncedAssessments(addedAssessmentIds))

    /**
     * Gets pregnancyEpisodeId from pregnancy details for pregnancy-related assessment types
     */
    private suspend fun getPregnancyEpisodeId(entity: AssessmentDetails): String? {
        val assessmentType = entity.assessmentType
        val assessmentTypes = listOf(PREGNANT_WOMEN_PROFILE, PREGNANCY_OUTCOME, ANC, PNC_MOTHER_MENU, CHILD_MENU)
        val isPregnancyRelated = assessmentTypes.any { it.equals(assessmentType, true) }

        return if (isPregnancyRelated) {
            roomHelper.getPregnancyDetailByPatientId(entity.householdMemberLocalId)?.pregnancyEpisodeId
        } else {
            null
        }
    }

    private suspend fun convertEntityToRequest(list: List<AssessmentDetails>): List<Assessment> {
        val peerSupervisorId = SecuredPreference.getLong(SecuredPreference.EnvironmentKey.PEER_SUPERVISOR_ID.name)
        return list.map { entity ->
            val assessmentDetail = getAssessmentDetails(entity)
            Assessment(
                referenceId = entity.id,
                villageId = entity.villageId,
                assessmentType = entity.assessmentType,
                assessmentDetails = assessmentDetail,
                patientStatus = entity.referralStatus,
                referredReasons = entity.referredReason?.filter { it.isNotBlank() }?.joinToString(", "),
                summary = entity.otherDetails?.let { JsonParser.parseString(it) },
                peerSupervisorId = peerSupervisorId,
                encounter = AssessmentEncounter(
                    householdId = entity.householdId,
                    memberId = entity.memberId,
                    referred = entity.isReferred,
                    patientId = entity.patientId,
                    provenance = ProvanceDto(modifiedDate = entity.createdAt.convertToUtcDateTime()),
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    customStatus = entity.status?.filter { it.isNotBlank() },
                    visitNumber = getVisitNumber(entity.assessmentType, assessmentDetail),
                    pregnancyEpisodeId = getPregnancyEpisodeId(entity),
                ),
                followUpId = entity.followUpId,
                updatedAt = entity.createdAt,
            )
        }
    }

    private fun getAssessmentDetails(assessment: AssessmentDetails): JsonElement {
        val assessmentDetails = JsonParser.parseString(assessment.assessmentDetails)
        val followUpDetails = assessment.callResult?.let { JsonParser.parseString(it) }

        val assessmentType = assessment.assessmentType.lowercase()

        // CBS Registration changes
        if (assessmentType == CBS.lowercase()) {
            val assessmentObject =
                assessmentDetails.asJsonObject.get(CBS.lowercase()).asJsonObject
            followUpDetails?.let {
                assessmentObject.add(FollowUp, it)
            }
        }

        // CBS changes for ICCM
        if (assessmentType == ICCM_MENU_ID.lowercase()) {
            val assessmentObject =
                assessmentDetails.asJsonObject.get(ICCM_MENU_ID.lowercase()).asJsonObject
            followUpDetails?.let {
                assessmentObject.add(FollowUp, it)
            }
        }

        // CBS changes for ANC
//        if (assessmentType == RMNCH.ANC_MENU.lowercase()) {
//            updateCbsForRMNCH(assessmentDetails, followUpDetails, ANC)
//        }

        // CBS changes for PNC Neonate
        if (assessmentType == PNC_NEONATE_KEY.lowercase()) {
            updateCbsForRMNCH(assessmentDetails, followUpDetails, PNCNeonatal)
        }

        // CBS changes for Childhood Visit
        if (assessmentType == CHILD_MENU.lowercase()) {
            updateCbsForRMNCH(assessmentDetails, followUpDetails, ChildHoodVisit)
        }

        return assessmentDetails
    }

    private fun updateCbsForRMNCH(
        assessmentDetails: JsonElement,
        followUpDetails: JsonElement?,
        key: String,
    ) {
        val mainObject = assessmentDetails.asJsonObject
        val asst = mainObject.get(key).asJsonObject
        mainObject.get(CBS.lowercase())?.let {
            val cbs = it.asJsonObject
            followUpDetails?.let {
                cbs.add(FollowUp, followUpDetails)
            }
            asst.add(CBS.lowercase(), cbs)

            mainObject.remove(CBS.lowercase())
        }
    }

    private fun getVisitNumber(
        assessmentType: String,
        assessmentDetails: JsonElement,
    ): Long? =
        when (assessmentType.lowercase()) {
            ANC.lowercase() -> getRMNCHVisitNumber(ANC, assessmentDetails)
            PNC_MOTHER_MENU.lowercase() -> getRMNCHVisitNumber(PNC, assessmentDetails)
            PNC_NEONATE_KEY.lowercase() -> getRMNCHVisitNumber(PNCNeonatal, assessmentDetails)
            CHILD_MENU.lowercase() -> getRMNCHVisitNumber(ChildHoodVisit, assessmentDetails)
            else -> null
        }

    private fun getRMNCHVisitNumber(
        key: String,
        assessmentDetails: JsonElement,
    ): Long =
        assessmentDetails.asJsonObject
            .get(key)
            .asJsonObject
            .get(visitNo)
            .asLong

    suspend fun getSyncStatus(request: RequestGetSyncStatus): Response<SyncResponse> = apiHelper.getOfflineSyncStatus(request)

    private suspend fun updateFhirId(
        tableName: String,
        id: String,
        fhirId: String?,
        status: String,
    ) {
        roomHelper.updateFhirId(tableName, id, fhirId, status)
    }

    suspend fun getInsertOrUpdateLocalData(
        liveData: MutableLiveData<Resource<Boolean>>,
        onProgress: ((Int) -> Unit)? = null,
    ) {
        // Check and Delete local data
        val lastSyncedAt = SecuredPreference.getString(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
        if (lastSyncedAt == null) {
            onProgress?.invoke(ResourceLoadingSyncProgress.INITIAL_DOWNLOAD_START)
            roomHelper.deleteAllHouseholds()
            roomHelper.deleteAllHouseholdMembers()
            roomHelper.deleteAllPregnancyDetails()
            roomHelper.deleteAllAssessments()
            roomHelper.deleteAllFollowUpCalls()
            roomHelper.deleteAllFollowUps()
            roomHelper.deleteAllUnAssignedMember()
            roomHelper.deleteAllCallHistory()
            roomHelper.deleteAllCommunityProfiles()
            roomHelper.deleteAllRxBuddyDetails()
            roomHelper.deleteAllRxBuddyFollowUp()
            roomHelper.deleteAllMemberAssessmentHistory()

            val villageIds = roomHelper.getAllSubVillageIds()
            // Fetch Synced Data
            val isInitialDataSuccess = fetchSyncedData(villageIds, null, onProgress)

            // Need to check this to be added for downloading error and in progress data
            // if (!fetchUnSyncedData()) {
            //  liveData.postError("Something went wrong")
            // }

            if (isInitialDataSuccess) {
                liveData.postSuccess(true)
            } else {
                liveData.postError("Something went wrong")
            }
        } else {
            onProgress?.invoke(ResourceLoadingSyncProgress.LOCAL_PERSIST_COMPLETE)
            liveData.postSuccess(true)
        }
    }

    suspend fun fetchSyncedData(
        villageIds: List<Long> = listOf(),
        serverLastSyncedAt: String? = null,
        onProgress: ((Int) -> Unit)? = null,
    ): Boolean {
        val syncedResponse = getSyncedEntities(villageIds, serverLastSyncedAt)
        if (!syncedResponse.isSuccessful) {
            return false
        }
        onProgress?.invoke(ResourceLoadingSyncProgress.SYNCED_PAYLOAD_RECEIVED)

        val responseInitialDownload = parseSyncedDataResponse(syncedResponse.body()?.string()) ?: return false

        val assessmentHistory = if (CommonUtils.isFoOrPo()) {
            val memberIds = extractSyncedMemberIds(responseInitialDownload.members)
            val history = mutableListOf<MemberAssessmentHistoryEntity>()
            // First fetch based on member ids
            if (memberIds.isNotEmpty()) {
                val assessmentHistoryResponse = fetchMemberAssessmentHistory(
                    lastSyncedAt = serverLastSyncedAt,
                    memberIds = memberIds,
                )
                if (!assessmentHistoryResponse.isSuccessful) {
                    return false
                }
                history.addAll(assessmentHistoryResponse.body() ?: emptyList())
            }

            // Now fetch based on practitioner id
            val assessmentHistoryResponse = fetchMemberAssessmentHistory(
                lastSyncedAt = serverLastSyncedAt,
                practitionerId = SecuredPreference.getUserFhirId(),
            )
            if (!assessmentHistoryResponse.isSuccessful) {
                return false
            }
            history.addAll(assessmentHistoryResponse.body() ?: emptyList())

            history
        } else {
            val assessmentHistoryResponse = fetchMemberAssessmentHistory(
                villageList = villageIds,
                lastSyncedAt = serverLastSyncedAt,
            )
            if (!assessmentHistoryResponse.isSuccessful) {
                return false
            }
            assessmentHistoryResponse.body() ?: emptyList()
        }
        onProgress?.invoke(ResourceLoadingSyncProgress.ASSESSMENT_HISTORY_RECEIVED)

        saveRequestInitialDownload(responseInitialDownload, assessmentHistory)
        onProgress?.invoke(ResourceLoadingSyncProgress.LOCAL_PERSIST_COMPLETE)
        return true
    }

    private fun parseSyncedDataResponse(response: String?): ResponseInitialDownload? {
        if (response == null) return null
        return try {
            val gson = Gson()
            val type: Type = object : TypeToken<ResponseInitialDownload>() {}.type
            gson.fromJson(response, type)
        } catch (e: Exception) {
            Timber.d("Exception ${e.localizedMessage}")
            null
        }
    }

    private fun extractSyncedMemberIds(members: List<HouseHoldMember>?): List<Long> = members.orEmpty().mapNotNull { it.id?.toLongOrNull() }

    suspend fun fetchAndSaveMemberDetails(memberId: String): SavedMemberDetails? {
        val response = apiHelper.getMemberDetails(RequestMemberDetails(memberId))
        if (!response.isSuccessful) {
            return null
        }
        val body = response.body()?.string() ?: return null
        return try {
            val gson = Gson()
            val type: Type = object : TypeToken<ResponseMemberDetails>() {}.type
            val memberDetails: ResponseMemberDetails? = gson.fromJson(body, type)
            memberDetails?.let { saveMemberDetailsResponse(it, memberId) }
        } catch (e: Exception) {
            Timber.d("Exception ${e.localizedMessage}")
            null
        }
    }

    private suspend fun saveMemberDetailsResponse(
        response: ResponseMemberDetails,
        requestedMemberId: String,
    ): SavedMemberDetails? {
        val localMemberId = roomHelper
            .insertOrUpdateHHMFromBE(
                response.member.toHouseholdMemberEntity(null, OfflineSyncStatus.Success),
            ).takeIf { it > 0 } ?: roomHelper.getHouseholdMemberIdByFhirId(requestedMemberId)
            ?: return null

        response.followUps?.forEach { followUp ->
            followUp.patientStatus = followUp.patientStatus ?: ""
            followUp.syncStatus = OfflineSyncStatus.Success
            roomHelper.insertOrUpdateFollowUp(followUp)
        }

        response.pregnancyInfos?.forEach {
            roomHelper.insertUpdatePregnancyDetailFromBE(it)
        }

        saveAssessmentHistory(response.assessmentHistory ?: emptyList())

        return SavedMemberDetails(
            localMemberId = localMemberId,
            dateOfBirth = response.member.dateOfBirth,
        )
    }

    private suspend fun saveAssessmentHistory(assessmentHistory: List<MemberAssessmentHistoryEntity>) {
        if (assessmentHistory.isEmpty()) return
        assessmentHistory.forEach { history ->
            val memberId = roomHelper.getHouseholdMemberIdByFhirId(history.memberFhirId)
            val existingHistory = roomHelper.getMemberAssessmentHistory(
                history.memberFhirId,
                memberId,
                history.visitDate,
                history.serviceProvided?.uppercase(Locale.ENGLISH),
            )
            val updatedHistory = if (existingHistory != null) {
                history.copy(id = existingHistory.id, memberId = memberId)
            } else {
                history.copy(memberId = memberId)
            }
            roomHelper.insertMemberAssessmentHistory(updatedHistory)
        }
    }

    private suspend fun saveRequestInitialDownload(
        requestInitialDownload: ResponseInitialDownload,
        assessmentHistory: List<MemberAssessmentHistoryEntity>,
    ) {
        val hhMapping = insertHouseholds(requestInitialDownload.households)
        insertHouseholdMembers(requestInitialDownload.members, hhMapping)

        // Update households where count exceeds no of members
        roomHelper.updateUndercountedHouseholds()

        // Update guardian id if exists with respective to fhir_id
        roomHelper.updateGuardianHhIds()

        // Update households where count of disabilities exceeds actual no of disability members
        roomHelper.updateUndercountedDisabilityHouseholds()

        // List of changes for Followup incremental
        // 1. Delete All Followup where Followup id is null and SyncStatus is InProgress -> Because it is created from Mobile
        // 2. Update record when sync status is Not NotSynced
        // 3. Delete All FollowUp where isCompleted true and syncStatus is Success
        // Insert follow up
        requestInitialDownload.followUps?.forEach { followUp ->
            followUp.patientStatus = followUp.patientStatus ?: ""
            followUp.syncStatus = OfflineSyncStatus.Success
            roomHelper.insertOrUpdateFollowUp(followUp)
            saveFollowUpCallDetails(followUp.followUpDetails)
        }
        roomHelper.deleteCompletedFollowUp()

        // Insert Link household member details
        requestInitialDownload.householdMemberLinks?.let {
            val insertList = mutableListOf<LinkHouseholdMember>()
            val deleteListIds = mutableListOf<String>()
            it.forEach { linkHouseholdMember ->
                if (linkHouseholdMember.status == UnAssigned) { // Insert
                    linkHouseholdMember.syncStatus = OfflineSyncStatus.Success
                    insertList.add(linkHouseholdMember)
                } else { // Delete
                    deleteListIds.add(linkHouseholdMember.memberId)
                }
            }
            roomHelper.insertLinkHouseholdMembers(insertList)
            roomHelper.deleteLinkHouseholdMembersById(deleteListIds)
        }

        // Insert Pregnancy Information
        requestInitialDownload.pregnancyInfos?.forEach {
            roomHelper.insertUpdatePregnancyDetailFromBE(it)
        }

        // Save Community profiles
        requestInitialDownload.communityProfiles?.forEach { item ->
            val id = item.get(DefinedParams.ID).asString
            val villageId = item.get(VILLAGE_ID).asLong
            val description = item.get(DESCRIPTION).asString
            val date = item.get(COMMUNITY_REGISTERED_DATE).asString
            item.remove(DefinedParams.ID)
            item.remove(VILLAGE_ID)
            item.remove(DESCRIPTION)
            item.remove(COMMUNITY_REGISTERED_DATE)
            val communityProfileEntity = CommunityProfile(id = 0, villageId, description, date, payload = item.toString())
            communityProfileEntity.fhirId = id
            communityProfileEntity.sync_status = OfflineSyncStatus.Success
            roomHelper.insertOrUpdateFromBE(communityProfileEntity)
        }

        // Save Treatment details
        requestInitialDownload.treatmentDetails.let {
            insertOrUpdateTreatmentDetails(it)
        }

        // Save Rx Buddy Register Details
        requestInitialDownload.rxBuddies?.let {
            insertOrUpdateRxBuddyData(it)
        }

        // Set FollowUpCriteria in Preference
        requestInitialDownload.followUpCriteria?.let {
            SecuredPreference.putFollowUpCriteria(it)
        } ?: kotlin.run {
            val followUpCriteria = FollowUpCriteria(3, 5, 3, 7, 7, 5)
            SecuredPreference.putFollowUpCriteria(followUpCriteria)
        }
        saveAssessmentHistory(assessmentHistory)

        SecuredPreference.putString(
            SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name,
            requestInitialDownload.lastSyncTime,
        )
    }

    private suspend fun saveFollowUpCallDetails(callDetails: List<FollowUpCall>?) {
        if (callDetails.isNullOrEmpty()) return
        callDetails.forEach { callDetail ->
            val existingCall = roomHelper.getFollowupCall(callDetail.followUpId, callDetail.callDate, callDetail.calledByUserId)
            val updatedCall = if (existingCall != null) {
                callDetail.copy(id = existingCall.id, followUpId = existingCall.followUpId)
            } else {
                callDetail.copy(followUpId = callDetail.callRegisterId)
            }
            roomHelper.insertFollowUpCall(updatedCall)
        }
    }

    private suspend fun insertOrUpdateTreatmentDetails(treatmentDetails: List<TreatmentDetails>?) {
        val gson = Gson()
        roomHelper.deleteAllTreatmentDetails()
        treatmentDetails?.forEach { treatmentDetail ->
            if (treatmentDetail.isTbConfirmed == true) {
                val prescriptionList = gson.toJson(treatmentDetail.prescriptions)
                val entity = TreatmentDetailsEntity(
                    memberId = treatmentDetail.memberId,
                    type = TB_MENU_ID.lowercase(),
                    treatmentStartDate = treatmentDetail.treatmentStartDate,
                    diagnoses = treatmentDetail.diagnosis,
                    diagnosedDate = treatmentDetail.dateDiagnosed ?: "",
                    prescriptions = prescriptionList,
                    tbConfirmationDate = treatmentDetail.tbConfirmationDate,
                )

                roomHelper.insertTreatmentDetails(entity)
            }
        }
    }

    private suspend fun insertOrUpdateRxBuddyData(rxBuddies: List<ResponseRxBuddy>) {
        val disableIds = rxBuddies.mapNotNull { item ->
            if (!item.isActive) return@mapNotNull item.id

            val registry = item.registry
            val rxBuddy = RxBuddyDetails(
                id = 0,
                rxBuddyId = item.id,
                patientMemberId = item.patientMemberId,
                relationship = registry.relationShip,
                isMonitorSheetProvider = registry.isMonitorSheetProvided,
                nextVisitDate = "",
                otherRelationship = registry.otherRelationship,
            ).apply {
                if (item.type == RX_BUDDY_TYPE_HOUSEHOLD_MEMBER && registry.householdMemberId != null) {
                    val hhmWithStatus = roomHelper.getHouseholdMemberIdAndStatusByFhirId(registry.householdMemberId!!)
                        ?: return@mapNotNull item.id
                    if (hhmWithStatus.isActive) {
                        householdMemberId = hhmWithStatus.id
                    } else {
                        return@mapNotNull item.id
                    }
                } else {
                    name = registry.rxBuddyDetails?.name
                    phoneNumber = registry.rxBuddyDetails?.phoneNumber
                }
            }

            roomHelper.insertOrUpdateRxBuddyFromBE(rxBuddy)
            null // No need to disable
        }

        roomHelper.deleteDisableRxBuddies(disableIds)
    }

    private suspend fun insertHouseholds(households: List<HouseHold>?): Map<String, Long> {
        // fhir id, local id
        val hhMap = mutableMapOf<String, Long>()

        households?.forEach { entity ->
            hhMap[entity.id!!] = roomHelper.insertOrUpdateHHFromBE(
                entity.toHouseholdEntity(
                    OfflineSyncStatus.Success,
                ),
            )
        }

        return hhMap
    }

    private suspend fun insertHouseholdMembers(
        householdMembers: List<HouseHoldMember>?,
        hhIdMap: Map<String, Long>,
    ) {
        householdMembers?.forEach { member ->
            if (hhIdMap.containsKey(member.householdId)) {
                roomHelper.insertOrUpdateHHMFromBE(
                    member.toHouseholdMemberEntity(
                        hhIdMap[member.householdId]!!,
                        OfflineSyncStatus.Success,
                    ),
                )
            } else {
                if (member.householdId != null) {
                    roomHelper.getHouseholdIdByFhirId(member.householdId)?.let {
                        roomHelper.insertOrUpdateHHMFromBE(
                            member.toHouseholdMemberEntity(
                                it,
                                OfflineSyncStatus.Success,
                            )
                        )
                    }
                } else {
                    roomHelper.insertOrUpdateHHMFromBE(
                        member.toHouseholdMemberEntity(
                            null,
                            OfflineSyncStatus.Success,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun getSyncedEntities(
        villageList: List<Long>,
        lastSyncedAt: String? = null,
    ): Response<ResponseBody> {
        // Getting village name only. For mapping, I have used following code
        val request = RequestAllEntities(villageList, lastSyncedAt)
        return apiHelper.fetchSyncedData(request)
    }

    private suspend fun fetchMemberAssessmentHistory(
        villageList: List<Long> = emptyList(),
        lastSyncedAt: String? = null,
        memberIds: List<Long> = emptyList(),
        practitionerId: String? = null,
    ): Response<List<MemberAssessmentHistoryEntity>> {
        val request = RequestAllEntities(
            villageIds = villageList,
            lastSyncTime = lastSyncedAt,
            memberIds = memberIds,
            practitionerId = practitionerId,
        )
        return apiHelper.fetchMemberAssessmentHistory(request)
    }

    private suspend fun getUnSyncedEntities(): Response<SyncResponse> {
        val req = RequestGetSyncStatus(
            userId = SecuredPreference.getUserId(),
            dataRequired = true,
            statuses = listOf(OfflineSyncStatus.InProgress.name, OfflineSyncStatus.Failed.name),
            types = listOf(EntitiesName.HOUSEHOLD, EntitiesName.HOUSEHOLD_MEMBER),
        )

        return apiHelper.getOfflineSyncStatus(req)
    }

    suspend fun uploadAllSignatures(): Boolean {
        val hhSignatureDetails = roomHelper.getHHSignatureDetails()

        if (hhSignatureDetails.isEmpty()) {
            return true
        }

        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)

        hhSignatureDetails.forEach { hhSignatureDetail ->
            getRenamedFile(hhSignatureDetail.signatureName, hhSignatureDetail.fhirId)?.let { file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                builder.addFormDataPart("signatureFile", file.name, requestFile)
            }
        }

        val dataRequest = Gson().toJson(ProvanceDto())
        builder.addFormDataPart("provenance", dataRequest)

        return try {
            val response = apiHelper.uploadAllConsentSignatures(builder.build())
            if (response.isSuccessful) {
                deleteAllSyncedImages()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getRenamedFile(
        oldFileName: String,
        newFileName: String,
    ): File? {
        val signatureDirPath = "/data/data/${BuildConfig.APPLICATION_ID}/files/$SIGNATURE_FOLDER"
        val signatureDir = File(signatureDirPath)

        if (signatureDir.exists()) {
            val oldFileNameWithExtension = "$oldFileName.$IMG_FILE_NAME_EXTENSION"
            val newFileNameWithExtension = "$newFileName.$IMG_FILE_NAME_EXTENSION"
            val oldFile = File(signatureDir, oldFileNameWithExtension)
            val newFile = File(signatureDir, newFileNameWithExtension)
            if (oldFile.exists()) {
                oldFile.renameTo(newFile)
                return newFile
            }
        }

        return null
    }

    private fun deleteAllSyncedImages(): Boolean {
        val imagesDirPath = "/data/data/${BuildConfig.APPLICATION_ID}/files/$SIGNATURE_FOLDER"
        val imagesDir = File(imagesDirPath)
        return deleteDirectory(imagesDir)
    }

    private fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }

        return directory.delete()
    }

    private suspend fun getCallRegisterEntityAsRequest(householdLinkCallsMemberIds: MutableList<String>): List<HouseholdMemberLinkCallDetails> {
        val list = roomHelper.getUnSyncedCallHistoryForHHMLink()
        val callDetails = mutableListOf<HouseholdMemberLinkCallDetails>()
        list.groupBy { it.memberId }.forEach { (key, value) ->
            householdLinkCallsMemberIds.add(key)
            val callRegisters = value.map { CallRegisterDetail(it.callDate.convertToUtcDateTime()) }
            callDetails.add(
                HouseholdMemberLinkCallDetails(
                    memberId = key,
                    patientId = value.first().patientId,
                    villageId = value.first().villageId,
                    callRegisterDetails = callRegisters,
                ),
            )
        }
        return callDetails
    }

    private suspend fun formatMemberForPnc(
        input: List<HouseHoldMember>,
        memberIds: MutableList<String>,
        assessmentIds: MutableList<String>,
        rxBuddyRegisterIds: MutableList<Long>,
        rxBuddyFollowUpIds: MutableList<Long>,
    ): List<HouseHoldMember> {
        val motherIds = mutableSetOf<String>()

        input.forEach { hhm ->
            memberIds.add(hhm.referenceId!!)
            hhm.motherReferenceId?.let {
                hhm.isChild = true
                motherIds.add(it)
            }
            hhm.assessments = getUnSyncedAssessmentByPatientId(hhm.referenceId.toLong())
            // Need to Add Rx Buddy Register
            hhm.rxBuddies = getRxBuddiesForHHM(hhm.referenceId.toLong(), rxBuddyRegisterIds, rxBuddyFollowUpIds)
            assessmentIds.addAll(hhm.assessments.map { it.referenceId.toString() })
        }

        val childIds = mutableListOf<String>()
        motherIds.forEach { motherId ->
            val children = input.filter { it.motherReferenceId == motherId && it.id == null }
            val mother = input.find { it.referenceId == motherId }
            if (children.isNotEmpty()) {
                if (mother != null) {
                    mother.children = children
                    // Remove only children that are explicitly nested under mother.
                    childIds.addAll(children.map { it.referenceId!! })
                } else {
                    // Child can be created in a later stage than mother.
                    // Keep these as top-level members so they sync independently.
                }
            }
        }

        return input.filter { !childIds.contains(it.referenceId) }
    }

    private suspend fun getRxBuddiesForHHM(
        hhmId: Long,
        rxBuddyRegisterIds: MutableList<Long>,
        rxBuddyFollowUpIds: MutableList<Long>,
    ): List<RxBuddy> {
        val rxBuddies = mutableListOf<RxBuddy>()
        val rxBuddyRegisters = roomHelper.getAllUnSyncedRxBuddyDetailWithHHM(hhmId, rxBuddyRegisterIds)

        rxBuddyRegisters.forEach { rx ->
            rxBuddies.add(getRxBuddy(rxBuddyRegisterIds, rxBuddyFollowUpIds, rx, true, null))
        }

        return rxBuddies
    }

    private suspend fun getRxBuddy(
        rxBuddyRegisterIds: MutableList<Long>,
        rxBuddyFollowUpIds: MutableList<Long>,
        rx: RxBuddyRegisterDetail,
        isForHouseholdMember: Boolean,
        rxBuddyFhirId: String? = null,
    ): RxBuddy {
        rxBuddyRegisterIds.add(rx.id)

        val register = RxBuddyRegister(
            referenceId = rx.id,
            relationShip = rx.relationship,
            otherRelationship = rx.otherRelationship,
            isMonitorSheetProvided = rx.isMonitorSheetProvider,
            nextVisitDate = rx.nextVisitDate,
            followUpId = rx.followUpId,
            latitude = rx.latitude,
            longitude = rx.longitude,
            provenance = ProvanceDto(modifiedDate = rx.updatedAt.convertToUtcDateTime()),
        )

        if (!isForHouseholdMember) {
            rxBuddyFhirId?.let {
                register.householdMemberId = it
            } ?: run {
                register.rxBuddyDetails = RxBuddyMember(rx.name, rx.phoneNumber)
            }
        }

        // Follow up along with register
        val followUpList = mutableListOf<RxBuddyFollowUp>()
        val followUps = roomHelper.getUnSyncedRxBuddyFollowUpWithoutRxBuddyId(rx.id)
        followUps.forEach { item ->
            rxBuddyFollowUpIds.add(item.rxBuddyLocalId)
            item.followUp?.let { strFollowUp ->
                followUpList.add(
                    getRxBuddyFollowUp(
                        item.id,
                        item.rxBuddyId,
                        strFollowUp,
                        item.nextVisitDate,
                        item.updatedAt,
                        item.followUpId,
                        item.latitude,
                        item.longitude,
                    ),
                )
            }
        }

        return RxBuddy(
            rx.patientMemberId,
            rx.patientId,
            rx.villageId,
            rx.householdId,
            null,
            rx.id,
            register,
            followUpList,
        )
    }

    private suspend fun getRxBuddiesRequest(
        hhmIds: MutableList<String>,
        rxBuddyRegisterIds: MutableList<Long>,
        rxBuddyFollowUpIds: MutableList<Long>,
    ): List<RxBuddy> {
        // Construct Rx Buddy details
        // 1. Get Un Synced Rx Buddy Register
        // 2. Construct Register object with member id / new rx buddy details
        // 3. Get Un Synced Rx Buddy Followup with Rx Buddy ID
        val rxBuddies = mutableListOf<RxBuddy>()
        val rxBuddyRegisters = roomHelper.getAllUnSyncedRxBuddyRegister()
        for (rx in rxBuddyRegisters) {
            // If RxBuddy member doesn't have fhir id skip the item
            var rxBuddyFhirId: String? = null
            if (rx.householdMemberId != null) {
                rxBuddyFhirId = roomHelper.getMemberFhirIdByLocalId(rx.householdMemberId!!)
                if (rxBuddyFhirId == null) {
                    continue
                }
            }

            rxBuddies.add(getRxBuddy(rxBuddyRegisterIds, rxBuddyFollowUpIds, rx, false, rxBuddyFhirId))
        }

        val rxBuddyFollowUps = roomHelper.getUnSyncedRxBuddyFollowUpWithRxBuddyId().groupBy { it.patientMemberId }
        val followUpList = mutableListOf<RxBuddyFollowUp>()
        rxBuddyFollowUps.forEach { (patientMemberId, followUps) ->
            followUps.forEach { item ->
                rxBuddyFollowUpIds.add(item.rxBuddyLocalId)
                item.followUp?.let { strFollowUp ->
                    followUpList.add(
                        getRxBuddyFollowUp(
                            item.id,
                            item.rxBuddyId,
                            strFollowUp,
                            item.nextVisitDate,
                            item.updatedAt,
                            item.followUpId,
                            item.latitude,
                            item.longitude,
                        ),
                    )
                }
            }
            val rx = followUps.first()
            rxBuddies.add(RxBuddy(patientMemberId, rx.patientId, rx.villageId, rx.householdId, followUps[0].rxBuddyId, rx.rxBuddyLocalId, null, followUpList))
        }

        return rxBuddies
    }

    private fun getRxBuddyFollowUp(
        id: Long,
        rxBuddyId: Long?,
        strFollowUp: String,
        nextVisitDate: String,
        updatedAt: Long,
        followUpId: Long? = null,
        latitude: Double,
        longitude: Double,
    ): RxBuddyFollowUp {
        val map = StringConverter.stringToMap(strFollowUp)
        return RxBuddyFollowUp(
            id = id,
            rxBuddyId = rxBuddyId,
            monitoringSheet = map[rxBuddyMonitoringDates] as List<String>,
            isSymptomsGettingWorse = map[isSymptomsGettingWorse] as Boolean,
            hadReactionToYourMedications = map[hadReactionToYourMedications] as Boolean,
            nextVisitDate = nextVisitDate,
            followUpId = followUpId,
            latitude = latitude,
            longitude = longitude,
            provenance = ProvanceDto(modifiedDate = updatedAt.convertToUtcDateTime()),
        )
    }

    /*
     * It will post all un-synced changes from local database and returns List<String>?
     * 1. list size > 0 -> Posted un-synced local changes and API is success
     * 2. List size == 0 -> There are no local changes to post
     * 3. List is null -> Post un-synced local changes and API is failed
     * */
    private suspend fun postOfflineUnSyncedChanges(syncMode: String): List<String> {
        val householdIds = mutableListOf<String>()
        val householdMemberIds = mutableListOf<String>()
        val assessmentIds = mutableListOf<String>()
        val followUpIds = mutableListOf<Long>()
        val followUpCallIds = mutableListOf<Long>()
        val householdLinkCallsMemberIds = mutableListOf<String>()
        val communityProfileIds = mutableListOf<Long>()
        val rxBuddyRegisterIds = mutableListOf<Long>()
        val rxBuddyFollowUpIds = mutableListOf<Long>()

        // uploadAllSignatures()
        val rxBuddies = getRxBuddiesRequest(householdMemberIds, rxBuddyRegisterIds, rxBuddyFollowUpIds)

        val houseHoldList = roomHelper.getAllUnSyncedHouseHolds(householdIds) // Hot Fix change - Done
        householdIds.addAll(houseHoldList.map { it.referenceId!! })
        houseHoldList.forEach { householdEntity ->
            val memberList =
                roomHelper.getAllUnSyncedHouseHoldMembers((householdEntity.referenceId!!.toLong())) // Hot Fix Change - Need to check

            householdEntity.householdMembers.addAll(formatMemberForPnc(memberList, householdMemberIds, assessmentIds, rxBuddyRegisterIds, rxBuddyFollowUpIds))
        }

        val members = roomHelper.getOtherHouseholdMembers(householdMemberIds) // Hot Fix Change - Need to check
        val otherMembers = formatMemberForPnc(members, householdMemberIds, assessmentIds, rxBuddyRegisterIds, rxBuddyFollowUpIds)

        val assignedMemberIds = otherMembers.filter { (it.assignHousehold == 1) && it.id != null }.map { it.id!! }

        // Assessment
        val otherAssessments = getOtherUnSyncedAssessments(assessmentIds) // Hot Fix change - Done
        assessmentIds.addAll(otherAssessments.map { it.referenceId.toString() })

        // Followup
        val allFollowUps = roomHelper.getAllFollowUpRequests()
        allFollowUps.forEach { followUp ->
            followUpIds.add(followUp.referenceId)
            followUp.id?.let {
                val followUpDetails = roomHelper.getAllFollowUpCalls(it)
                followUp.followUpDetails = followUpDetails
                followUpCallIds.addAll(followUpDetails.map { call -> call.id })
            }
        }

        val householdMemberLink = getCallRegisterEntityAsRequest(householdLinkCallsMemberIds)

        // Community profiles
        val communityProfiles = roomHelper.getUnSyncedCommunityDetails()
        val communityProfilesRequests = mutableListOf<JsonObject>()
        communityProfiles.forEach { community ->
            val provenance = ProvanceDto(modifiedDate = community.updatedAt.convertToUtcDateTime())
            val json = JsonParser.parseString(community.payload).asJsonObject
            community.fhirId?.let {
                json.addProperty(DefinedParams.ID, it)
            }
            json.addProperty(DESCRIPTION, community.communityDescription)
            json.addProperty(COMMUNITY_REGISTERED_DATE, community.registeredDate)
            json.addProperty(VILLAGE_ID, community.villageId)
            json.addProperty(ReferenceId, community.id.toString())
            json.add(Provenance, Gson().toJsonTree(provenance))
            communityProfilesRequests.add(json)
            communityProfileIds.add(community.villageId)
        }

        // Nothing to Post anything
        if (houseHoldList.isEmpty() &&
            otherMembers.isEmpty() &&
            otherAssessments.isEmpty() &&
            allFollowUps.isEmpty() &&
            householdMemberLink.isEmpty() &&
            communityProfilesRequests.isEmpty() &&
            rxBuddies.isEmpty()
        ) {
            return listOf()
        }

        val request = OfflineUtils.getRequestObject()
        request[OfflineConstant.SYNC_MODE] = syncMode
        request[OfflineConstant.HOUSE_HOLDS] = houseHoldList
        request[OfflineConstant.HOUSE_HOLD_MEMBERS] = otherMembers
        request[OfflineConstant.ASSESSMENTS] = otherAssessments
        request[OfflineConstant.FOLLOWUPS] = allFollowUps
        request[OfflineConstant.HOUSEHOLD_MEMBER_LINK] = householdMemberLink
        request[OfflineConstant.COMMUNITY_PROFILES] = communityProfilesRequests
        request[OfflineConstant.RX_BUDDIES] = rxBuddies

        val data = Gson().toJson(request)
        Log.d(" post data here", data)

        try {
            val apiResponse = apiHelper.postOfflineSync(request)
            if (apiResponse.isSuccessful) {
                roomHelper.changeHouseholdStatus(householdIds, OfflineSyncStatus.InProgress.name) // Change Status to InProgress
                roomHelper.changeHouseholdMemberStatus(householdMemberIds, OfflineSyncStatus.InProgress.name) // Change Status to InProgress
                roomHelper.changeAssessmentStatus(assessmentIds, OfflineSyncStatus.InProgress.name) // Change status to InProgress
                roomHelper.changeFollowUpStatus(followUpIds, OfflineSyncStatus.InProgress.name) // Change status to InProgress
                roomHelper.changeFollowUpCallStatus(followUpCallIds) // Change isSynced Status to True
                roomHelper.changeAssignHHMStatus(assignedMemberIds, OfflineSyncStatus.InProgress.name)
                roomHelper.changeHHMLinkCallStatus(householdLinkCallsMemberIds, OfflineSyncStatus.InProgress.name)
                roomHelper.changeCommunityProfileStatus(communityProfileIds, OfflineSyncStatus.InProgress.name)
                roomHelper.updateRxBuddyRegisterSyncStatus(rxBuddyRegisterIds, OfflineSyncStatus.InProgress.name)
                roomHelper.updateRxBuddyFollowUpSyncStatus(rxBuddyFollowUpIds, OfflineSyncStatus.InProgress.name)
                return listOf(request[OfflineConstant.REQUEST_ID] as String)
            }
        } catch (e: Exception) {
            roomHelper.changeHouseholdStatus(householdIds, OfflineSyncStatus.NetworkError.name) // Change Status to InProgress
            roomHelper.changeHouseholdMemberStatus(householdMemberIds, OfflineSyncStatus.NetworkError.name) // Change Status to InProgress
            roomHelper.changeAssessmentStatus(assessmentIds, OfflineSyncStatus.NetworkError.name) // Change status to InProgress
            roomHelper.changeFollowUpStatus(followUpIds, OfflineSyncStatus.NetworkError.name) // Change status to InProgress
            roomHelper.changeAssignHHMStatus(assignedMemberIds, OfflineSyncStatus.NetworkError.name)
            roomHelper.changeHHMLinkCallStatus(householdLinkCallsMemberIds, OfflineSyncStatus.NetworkError.name)
            roomHelper.changeCommunityProfileStatus(communityProfileIds, OfflineSyncStatus.NetworkError.name)
            roomHelper.updateRxBuddyRegisterSyncStatus(rxBuddyRegisterIds, OfflineSyncStatus.NetworkError.name)
            roomHelper.updateRxBuddyFollowUpSyncStatus(rxBuddyFollowUpIds, OfflineSyncStatus.NetworkError.name)
            return listOf(request[OfflineConstant.REQUEST_ID] as String)
        }

        return listOf(request[OfflineConstant.REQUEST_ID] as String)
    }

    suspend fun getSyncStatusForOffline(id: String): Boolean {
        val req = RequestGetSyncStatus(requestId = id)
        try {
            // Get Sync Status
            val response = getSyncStatus(req)
            if (response.isSuccessful) {
                var isAllEntitiesSynced = true
                response.body()?.entityList?.forEach { entity ->
                    when (entity.status) {
                        OfflineSyncStatus.Success.name -> {
                            if (entity.referenceId != null && (entity.type == COMMUNITY_PROFILE || entity.type != null && entity.fhirId != null)) {
                                updateFhirId(
                                    entity.type,
                                    entity.referenceId,
                                    entity.fhirId,
                                    OfflineSyncStatus.Success.name,
                                )
                            }
                        }

                        OfflineSyncStatus.Failed.name -> {
                            if (entity.type != null && entity.referenceId != null) {
                                updateFhirId(
                                    entity.type,
                                    entity.referenceId,
                                    entity.fhirId,
                                    OfflineSyncStatus.Failed.name,
                                )
                            }
                        }

                        OfflineSyncStatus.InProgress.name -> {
                            isAllEntitiesSynced = false
                        }
                    }
                }

                return isAllEntitiesSynced
            } else {
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun postOfflineUnSyncedChangesWithMutex(syncMode: String): List<String> {
        mutex.withLock {
            return postOfflineUnSyncedChanges(syncMode)
        }
    }
}
