package org.medtroniclabs.uhis.repo

import android.location.Location
import androidx.lifecycle.LiveData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.getStringOrEmptyString
import org.medtroniclabs.uhis.common.DefinedParams.CHIEF_DOM_CODE_LENGTH
import org.medtroniclabs.uhis.common.DefinedParams.PATIENT_NUMBER_LENGTH
import org.medtroniclabs.uhis.common.DefinedParams.VILLAGE_CODE_LENGTH
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberFhirId
import org.medtroniclabs.uhis.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant.CALL_TYPE_LINK_HHM
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.CallHistory
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.MemberClinicalEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration
import org.medtroniclabs.uhis.mappingkey.MemberRegistration
import org.medtroniclabs.uhis.mappingkey.MemberRegistration.ID_MARITAL_STATUS
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.deceasedReason
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.isDeceased
import javax.inject.Inject

class HouseholdMemberRepository @Inject constructor(
    private var roomHelper: RoomHelper,
) {
    suspend fun registerMember(
        map: HashMap<String, Any>,
        householdId: Long?,
        entity: HouseholdMemberEntity? = null,
        parentReferenceId: Long? = null,
        isPhuWalkInFlow: Boolean? = null,
        location: Location?,
    ): Long {
        val beforeMember = if (entity != null) roomHelper.getMemberDetailsByID(entity.id) else null

        val memberEntity = createOrUpdateHouseHoldMemberEntity(map, householdId, entity, parentReferenceId, location)
//         if (memberEntity.patientId == null) {
//             return  null
//         }

        // If updating a member and isHouseholdHead is not explicitly set in the map, preserve the old value
        if (entity != null && beforeMember != null && !map.containsKey(MemberRegistration.IS_HOUSEHOLD_HEAD)) {
            memberEntity.isHouseholdHead = beforeMember.isHouseholdHead
        }

        if (entity != null && beforeMember != null && !hasMeaningfulMemberChanges(beforeMember, memberEntity)) {
            return entity.id
        }

        val memberId = roomHelper.registerMember(memberEntity)

        // Only perform household-related operations if householdId is not null
        if (householdId != null) {
            // If updating a member who is household head, update household name with member's name
            if (entity != null && memberEntity.isHouseholdHead && memberEntity.name.isNotEmpty()) {
                val householdEntity = roomHelper.getHouseHoldDetailsById(householdId)
                householdEntity.name = memberEntity.name
                householdEntity.updatedAt = System.currentTimeMillis()
                householdEntity.sync_status = OfflineSyncStatus.NotSynced
                roomHelper.updateHousehold(householdEntity)
            }

            // Assign same household for Parent or Child
//            if (isPhuWalkInFlow == true) {
//                val fhirIds = if (memberEntity.parentId != null) {
//                    roomHelper.getUnAssignedParentFhirId(memberEntity.parentId!!)
//                } else {
//                    roomHelper.getUnAssignedChildFhirIds(memberEntity.patientId!!)
//                }
//
//                if (fhirIds.isNotEmpty()) {
//                    memberEntity.fhirId?.let {
//                        updateMemberAsAssigned(fhirIds, householdId, it)
//                    }
//                }
//            }

            // Update Member count in household only in insert case
            if (entity == null || isPhuWalkInFlow == true) {
                roomHelper.updateHeadCountIfUnderCounted(householdId)
            }

            roomHelper.updateDisabilityPersonsCountIfUnderCounted(householdId)
        }

        return memberId
    }

    suspend fun updateHeadPhoneNumber(
        householdId: Long,
        map: HashMap<String, Any>,
    ) {
        val isHouseholdHead = map[MemberRegistration.IS_HOUSEHOLD_HEAD]
        if (CommonUtils.getIsBooleanFromString(isHouseholdHead)) {
            // Updating in HouseHoldMEMBER
            roomHelper.updatePhoneNumberForHouseholdHead(
                householdId,
                map[MemberRegistration.PHONE_NUMBER]?.toString(),
                null, // phoneNumberCategory parameter kept for interface compatibility
            )
        }
    }

    /**
     * Returns true when any compared business field differs.
     */
    private fun hasMeaningfulMemberChanges(
        before: HouseholdMemberEntity,
        after: HouseholdMemberEntity,
    ): Boolean {
        if (before.name != after.name) return true
        if (before.phoneNumber != after.phoneNumber) return true
        if (before.phoneNumberCategory != after.phoneNumberCategory) return true
        if (before.dateOfBirth != after.dateOfBirth) return true
        if (before.gender != after.gender) return true
        if (before.householdId != after.householdId) return true
        if (before.villageId != after.villageId) return true
        if (before.shasthyaShebikaId != after.shasthyaShebikaId) return true
        if (before.subVillageId != after.subVillageId) return true
        if (before.motherReferenceId != after.motherReferenceId) return true
        if (before.idType != after.idType) return true
        if (before.nationalId != after.nationalId) return true
        if (before.isHouseholdHead != after.isHouseholdHead) return true
        if (before.guardianId != after.guardianId) return true
        if (before.maritalStatus != after.maritalStatus) return true
        if (before.disability != after.disability) return true
        return false
    }

    private suspend fun createOrUpdateHouseHoldMemberEntity(
        map: HashMap<String, Any>,
        householdId: Long?,
        entity: HouseholdMemberEntity? = null,
        parentReferenceId: Long?,
        location: Location?,
    ): HouseholdMemberEntity {
        val householdMemberEntity = entity ?: HouseholdMemberEntity()

        val name = map[MemberRegistration.NAME]
        householdMemberEntity.name = getStringOrEmptyString(name)

        parentReferenceId?.let {
            householdMemberEntity.motherReferenceId = it
        }

        val phoneNumber = map[MemberRegistration.PHONE_NUMBER]
        householdMemberEntity.phoneNumber = getStringOrEmptyString(phoneNumber)

        val phoneNumberCategory = map[MemberRegistration.PHONE_NUMBER_CATEGORY]
        householdMemberEntity.phoneNumberCategory = getStringOrEmptyString(phoneNumberCategory)

        val dateOfBirth = map[MemberRegistration.DATE_OF_BIRTH]
        householdMemberEntity.dateOfBirth = getStringOrEmptyString(dateOfBirth)

        val gender = map[MemberRegistration.GENDER]
        householdMemberEntity.gender = getStringOrEmptyString(gender)

        val idType = map[MemberRegistration.ID_TYPE]
        householdMemberEntity.idType = getStringOrEmptyString(idType)

        val nationalId = map[MemberRegistration.NATIONAL_ID]
        householdMemberEntity.nationalId = nationalId?.toString()?.takeIf { it.isNotEmpty() }

        val isHouseholdHead = map[MemberRegistration.IS_HOUSEHOLD_HEAD]
        householdMemberEntity.isHouseholdHead = isHouseholdHead == true

        val householdFhirId = map[MemberRegistration.HOUSEHOLD_FHIR_ID]
        householdMemberEntity.householdFhirId = householdFhirId?.toString()?.takeIf { it.isNotEmpty() }

        val isDeceased = map[isDeceased]
        if (isDeceased != null && isDeceased is Boolean && isDeceased) {
            householdMemberEntity.isActive = false
        }

        val deceasedReason = map[deceasedReason]
        if (deceasedReason != null && deceasedReason is String) {
            householdMemberEntity.deceasedReason = deceasedReason
        }

        householdMemberEntity.householdId = householdId

        val maritalStatus = map[ID_MARITAL_STATUS]
        if (maritalStatus != null && maritalStatus is String) {
            householdMemberEntity.maritalStatus = maritalStatus
        }

        val disability = map[MemberRegistration.ID_DISABILITY]
        if (disability != null && disability is String) {
            householdMemberEntity.disability = disability
        }

        val guardianId = CommonUtils.getLongOrNull(map[MemberRegistration.ID_GUARDIAN])
        if (guardianId != null) {
            householdMemberEntity.guardianId = guardianId
            // Fetch member details for the guardian to store FhirId
            householdMemberEntity.guardianFhirId = getMemberDetailsByID(guardianId).data?.fhirId
        }

        val currentTime = System.currentTimeMillis()

        if (entity == null) {
            // If householdId is null, get location fields from form map
            if (householdId == null) {
                val villageIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.VILLAGE_ID])
                if (villageIdFromMap != null) {
                    householdMemberEntity.villageId = villageIdFromMap
                }

                val shasthyaShebikaIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.SHASTHYA_SHEBIKA_ID])
                if (shasthyaShebikaIdFromMap != null) {
                    householdMemberEntity.shasthyaShebikaId = shasthyaShebikaIdFromMap
                }

                val subVillageIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.SUB_VILLAGE_ID])
                if (subVillageIdFromMap != null) {
                    householdMemberEntity.subVillageId = subVillageIdFromMap
                }
                applyLocationFromMap(householdMemberEntity, map)
            } else {
                // For regular members, derive full location hierarchy from household.
                val householdDetails =
                    try {
                        roomHelper.getHouseHoldDetailsById(householdId)
                    } catch (_: Exception) {
                        null
                    }
                if (householdDetails != null) {
                    householdMemberEntity.villageId = householdDetails.villageId
                    householdMemberEntity.shasthyaShebikaId = householdDetails.shasthyaShebikaId
                    householdMemberEntity.subVillageId = householdDetails.subVillageId
                } else {
                    // Defensive fallback for stale/invalid household references.
                    applyLocationFromMap(householdMemberEntity, map)
                }
                householdMemberEntity.createdAt = currentTime
            }
        } else {
            householdMemberEntity.sync_status = OfflineSyncStatus.NotSynced
            if (householdId == null) {
                applyLocationFromMap(householdMemberEntity, map)
            }
        }
        householdMemberEntity.updatedAt = currentTime
        location?.let {
            householdMemberEntity.latitude = it.latitude
            householdMemberEntity.longitude = it.longitude
        }

        return householdMemberEntity
    }

    private fun applyLocationFromMap(
        householdMemberEntity: HouseholdMemberEntity,
        map: HashMap<String, Any>,
    ) {
        val villageIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.VILLAGE_ID])
        if (villageIdFromMap != null) {
            householdMemberEntity.villageId = villageIdFromMap
        }

        val shasthyaShebikaIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.SHASTHYA_SHEBIKA_ID])
        if (shasthyaShebikaIdFromMap != null) {
            householdMemberEntity.shasthyaShebikaId = shasthyaShebikaIdFromMap
        }

        val subVillageIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.SUB_VILLAGE_ID])
        if (subVillageIdFromMap != null) {
            householdMemberEntity.subVillageId = subVillageIdFromMap
        }

        val shasthyaKormiIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.SHASTHYA_KORMI_ID])
        if (shasthyaKormiIdFromMap != null) {
            householdMemberEntity.shasthyaKormiId = shasthyaKormiIdFromMap
        }

        val chiefdomIdFromMap = CommonUtils.getLongOrNull(map[HouseHoldRegistration.CHIEFDOM_ID])
        if (chiefdomIdFromMap != null) {
            householdMemberEntity.chiefdomId = chiefdomIdFromMap
        }
    }

    private suspend fun getNextPatientId(villageId: Long): String? {
        val villageDetail = roomHelper.getVillageByID(villageId)
        if (villageDetail.chiefdomCode.isNullOrBlank()) {
            return null
        }
        if (villageDetail.villagecode.isNullOrBlank()) {
            return null
        }
        val chiefDomCode = villageDetail.chiefdomCode.padStart(CHIEF_DOM_CODE_LENGTH, '0')
        val villageCode = villageDetail.villagecode.padStart(VILLAGE_CODE_LENGTH, '0')
        val chwUserId = SecuredPreference.getUserId().toString()
        val startIndex = chiefDomCode.length + villageCode.length + chwUserId.length

        val lastPatientId =
            roomHelper
                .getLastPatientId("$chiefDomCode$villageCode$chwUserId%")
                ?.let { it.substring(startIndex, it.length) }
                ?.toInt() ?: 0
        val nextPatientId = (lastPatientId + 1).toString().padStart(PATIENT_NUMBER_LENGTH, '0')
        return "$chiefDomCode$villageCode$chwUserId$nextPatientId"
    }

    suspend fun getMemberDetailsByID(memberId: Long): Resource<HouseholdMemberEntity> =
        try {
            val memberEntity = roomHelper.getMemberDetailsByID(memberId)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.SUCCESS)
        }

    suspend fun getMemberDetails(memberId: Long): HouseholdMemberEntity = roomHelper.getMemberDetailsByID(memberId)

    suspend fun getMemberDetailsByParentId(memberId: String): Resource<List<HouseholdMemberEntity>> =
        try {
            val memberEntity = roomHelper.getMemberDetailsByParentId(memberId)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getMemberDetailsByPatientId(patientId: String): Resource<HouseholdMemberEntity> =
        try {
            val memberEntity = roomHelper.getMemberDetailsByPatientId(patientId)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getAssessmentMemberDetails(id: Long): Resource<AssessmentMemberDetails> =
        try {
            val memberEntity = roomHelper.getAssessmentMemberDetails(id)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getPatientVisitCountByType(
        type: String,
        hhmLocalId: Long,
    ): MemberClinicalEntity? = roomHelper.getPatientVisitCountByType(type, hhmLocalId)

    suspend fun savePatientVisitCountByType(memberClinicalEntity: MemberClinicalEntity) {
        roomHelper.savePatientVisitCountByType(memberClinicalEntity)
    }

    suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail? = roomHelper.getPregnancyDetailByPatientId(hhmLocalId)

    suspend fun savePregnancyDetail(pregnancyDetail: PregnancyDetail): Long = roomHelper.savePregnancyDetail(pregnancyDetail)

    suspend fun updateMemberDeceasedStatus(
        id: Long,
        status: Boolean,
    ) = roomHelper.updateMemberDeceasedStatus(id, status)

    suspend fun getPatientIdById(id: Long): String = roomHelper.getPatientIdById(id)

    suspend fun changeMemberDetailsToNotSynced(id: Long) {
        roomHelper.changeMemberDetailsToNotSynced(id)
    }

    fun getUnAssignedHouseholdMember(): LiveData<List<UnAssignedHouseholdMemberDetail>> = roomHelper.getUnAssignedHouseholdMembersLiveData()

    suspend fun addLinkMemberCall(
        memberId: String,
        callStartTime: Long,
        callEndTime: Long,
    ): Long {
        val callHistory = CallHistory(
            type = CALL_TYPE_LINK_HHM,
            referenceId = memberId,
            callStartTime = callStartTime,
            callEndTime = callEndTime,
        )
        return roomHelper.addLinkMemberCall(callHistory)
    }

    suspend fun updateMemberAsAssigned(memberId: String) {
        roomHelper.updateMemberAsAssigned(memberId)
    }

    suspend fun updateMemberDeceasedReason(
        id: Long,
        status: Boolean,
        deceasedReason: String?,
    ) = roomHelper.updateMemberDeceasedReason(id, status, deceasedReason)

    suspend fun getHouseholdHeadDob(householdId: Long): String = roomHelper.getHouseholdHeadDob(householdId)

    suspend fun updatePregnantStatus(
        memberId: Long,
        isPregnant: Boolean,
    ) = roomHelper.updatePregnantStatus(memberId, isPregnant)

    suspend fun getTbPatientLocalIdByHouseholdId(householdId: Long): MutableList<Long> = roomHelper.getTbPatientLocalIdByHouseholdId(householdId)

    suspend fun isTbPatient(memberId: String): Boolean = roomHelper.getTreatmentDetails(memberId) != null

    suspend fun updateContactTracingStatus(
        memberId: Long,
        status: Int?,
    ) {
        roomHelper.updateContactTracingStatus(memberId, status)
    }

    suspend fun updateContactTracingForLinkTbPatient(
        tbHHMId: Long,
        householdId: Long,
    ) {
        roomHelper.updateContactTracingForLinkTbPatient(tbHHMId, householdId)
    }

    private suspend fun updateMemberAsAssigned(
        hhmFhirIds: List<HouseholdMemberFhirId>,
        hhId: Long,
        motherOrChildFhirId: String,
    ) {
        val isMotherOrChildTbPatient = isTbPatient(motherOrChildFhirId)
        hhmFhirIds.forEach { hhmFhirId ->
            val hhTbPatientIds = getTbPatientLocalIdByHouseholdId(hhId)
            val isTbPatient = isTbPatient(hhmFhirId.hhmFhirId)

            if (isTbPatient) {
                if (hhTbPatientIds.isEmpty()) {
                    updateContactTracingForLinkTbPatient(hhmFhirId.hhmId, hhId)
                }
            } else {
                if (hhTbPatientIds.isNotEmpty() || isMotherOrChildTbPatient) {
                    updateContactTracingStatus(
                        hhmFhirId.hhmId,
                        OfflineConstant.CONTACT_TRACING_YET_TO_TAKE,
                    )
                } else {
                    updateContactTracingStatus(hhmFhirId.hhmId, null)
                }
            }
            roomHelper.updateHouseholdHeadAndRelationShip(listOf(hhmFhirId.hhmFhirId), hhId)
            updateMemberAsAssigned(hhmFhirId.hhmFhirId)
        }
    }

    fun getServiceMembers(
        searchInput: String,
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        staticFilter: ServiceStaticFilter,
        allowNullHousehold: Boolean = false,
    ) = roomHelper.getServiceMembers(searchInput, filterBySs, filterBySubVillages, staticFilter, allowNullHousehold)

    /**
     * Fetches counts for the given static filters using capped parallel COUNT queries.
     * Dynamic filters match [getServiceMembers].
     */
    suspend fun getServiceMemberCounts(
        filters: List<ServiceStaticFilter>,
        searchInput: String = "",
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        allowNullHousehold: Boolean = false,
    ): Map<ServiceStaticFilter, Int> {
        if (filters.isEmpty()) return emptyMap()
        val semaphore = Semaphore(SERVICE_MEMBER_COUNT_PARALLELISM)
        return coroutineScope {
            filters
                .map { filter ->
                    async {
                        semaphore.withPermit {
                            filter to
                                roomHelper.getServiceMemberCountForFilter(
                                    staticFilter = filter,
                                    searchInput = searchInput,
                                    filterBySs = filterBySs,
                                    filterBySubVillages = filterBySubVillages,
                                    allowNullHousehold = allowNullHousehold,
                                )
                        }
                    }
                }.awaitAll()
                .toMap()
        }
    }

    /**
     * Retrieves all National IDs for the specified ID type from the Room database.
     *
     * @param idType The type of ID to filter by.
     * @return A list of National IDs.
     */
    suspend fun getAllNationalIds(idType: String): List<String> = roomHelper.getAllNationalIds(idType)

    suspend fun getPregnancyDetails(id: Long) = roomHelper.getPregnancyDetailByPatientId(id)

    companion object {
        /** Max concurrent COUNT queries (tuned for low-end devices). */
        private const val SERVICE_MEMBER_COUNT_PARALLELISM = 4
    }
}
