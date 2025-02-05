package com.medtroniclabs.opensource.repo

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.getStringOrEmptyString
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.CHIEF_DOM_CODE_LENGTH
import com.medtroniclabs.opensource.common.DefinedParams.PATIENT_NUMBER_LENGTH
import com.medtroniclabs.opensource.common.DefinedParams.VILLAGE_CODE_LENGTH
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineConstant.CALL_TYPE_LINK_HHM
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.CallHistory
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.db.entity.MemberClinicalEntity
import com.medtroniclabs.opensource.db.entity.PregnancyDetail
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.mappingkey.MemberRegistration
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.otherFamilyMember
import com.medtroniclabs.opensource.model.assessment.AssessmentMemberDetails
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.isDeceased
import javax.inject.Inject

class HouseholdMemberRepository @Inject constructor(
    private var roomHelper: RoomHelper
) {
    suspend fun registerMember(
        map: HashMap<String, Any>,
        householdId: Long,
        entity: HouseholdMemberEntity? = null,
        parentReferenceId: Long? = null,
        initial: String? = null,
        signature: String? = null,
        isPhuWalkInFlow: Boolean? = null
    ): Long? {
        val memberEntity = createOrUpdateHouseHoldMemberEntity(map, householdId, entity, parentReferenceId, initial, signature)
       /* if (memberEntity.patientId == null) {
            return  null
        }*/
        val memberId = roomHelper.registerMember(memberEntity)

        // Assign same household for Parent or Child
        if (isPhuWalkInFlow == true) {
            val fhirIds = if (memberEntity.parentId != null) {
                roomHelper.getUnAssignedParentFhirId(memberEntity.parentId!!)
            } else {
                roomHelper.getUnAssignedChildFhirIds(memberEntity.patientId!!)
            }

            if (fhirIds.isNotEmpty()) {
                roomHelper.updateHouseholdHeadAndRelationShip(fhirIds, householdId)
                roomHelper.updateMembersAsAssigned(fhirIds)
            }
        }

        //Update Member count in household only in insert case
        if (entity == null || isPhuWalkInFlow == true) {
            val memberAddedForHouseHold = getMemberCountPerHouseHold(householdId)
            val memberMentionedInHouseHold =
                roomHelper.getHouseHoldDetailsById(householdId).noOfPeople
            if (memberAddedForHouseHold > memberMentionedInHouseHold) {
                updateHeadCount(householdId, memberAddedForHouseHold)
            }
        }

        return memberId
    }

    suspend fun updateHeadPhoneNumber(householdId: Long, map: HashMap<String, Any>) {
         if (map[MemberRegistration.householdHeadRelationship]==DefinedParams.HouseholdHead) {
            //Updating in HouseHoldMEMBER
            roomHelper.updateHeadPhoneNumber(householdId,
                map[MemberRegistration.phoneNumber].toString(),
                map[MemberRegistration.phoneNumberCategory].toString()
            )
        }
    }


    private suspend fun createOrUpdateHouseHoldMemberEntity(
        map: HashMap<String, Any>,
        householdId: Long,
        entity: HouseholdMemberEntity? = null,
        parentReferenceId: Long?,
        initial: String? = null,
        signature: String? = null
    ): HouseholdMemberEntity {
        val householdMemberEntity = entity ?: HouseholdMemberEntity()

        val name = map[MemberRegistration.name]
        householdMemberEntity.name = getStringOrEmptyString(name)

        parentReferenceId?.let {
            householdMemberEntity.motherReferenceId = it
        }

        val phoneNumber = map[MemberRegistration.phoneNumber]
        householdMemberEntity.phoneNumber = getStringOrEmptyString(phoneNumber)

        val phoneNumberCategory = map[MemberRegistration.phoneNumberCategory]
        householdMemberEntity.phoneNumberCategory =
            getStringOrEmptyString(phoneNumberCategory)

        val dateOfBirth = map[MemberRegistration.dateOfBirth]
        householdMemberEntity.dateOfBirth = getStringOrEmptyString(dateOfBirth)

        val gender = map[MemberRegistration.gender]
        householdMemberEntity.gender = getStringOrEmptyString(gender)

        val householdHeadRelationship = map[MemberRegistration.householdHeadRelationship]
        val otherHouseholdRelationship =
            if (map.containsKey(otherFamilyMember)) map[otherFamilyMember] else null
        householdMemberEntity.householdHeadRelationship =
            householdRelationshipStatus(householdHeadRelationship, otherHouseholdRelationship)

        val isPregnantOrNot = map[MemberRegistration.isPregnant]
        householdMemberEntity.isPregnant =
            isPregnantOrNot?.let { CommonUtils.getIsBooleanFromString(isPregnantOrNot) }

        val isDeceased = map[isDeceased]
        if (isDeceased != null && isDeceased is Boolean && isDeceased){
             householdMemberEntity.isActive = false
        }

        householdMemberEntity.householdId = householdId

        if (entity == null) {
            val householdDetails = roomHelper.getHouseHoldDetailsById(householdId)
            householdMemberEntity.villageId = householdDetails.villageId
          //  householdMemberEntity.patientId = getNextPatientId(householdDetails.villageId)
            householdMemberEntity.initial = initial
            householdMemberEntity.localSignatureFile = signature
        } else {
            householdMemberEntity.updatedAt = System.currentTimeMillis()
            householdMemberEntity.sync_status = OfflineSyncStatus.NotSynced
        }

        return householdMemberEntity
    }

    private fun householdRelationshipStatus(
        householdHeadRelationship: Any?,
        otherHouseholdRelationship: Any?
    ): String {
        return if (otherHouseholdRelationship != null) {
            "${getStringOrEmptyString(householdHeadRelationship)}-${
                getStringOrEmptyString(
                    otherHouseholdRelationship
                )
            }"
        } else {
            getStringOrEmptyString(householdHeadRelationship)
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
            roomHelper.getLastPatientId("$chiefDomCode$villageCode$chwUserId%")?.let { it.substring(startIndex, it.length) }
                ?.toInt() ?: 0
        val nextPatientId = (lastPatientId + 1).toString().padStart(PATIENT_NUMBER_LENGTH, '0')
        return "$chiefDomCode$villageCode$chwUserId$nextPatientId"
    }

    suspend fun getMemberDetailsByID(
        memberId: Long,
    ): Resource<HouseholdMemberEntity> {
        return try {
            val memberEntity = roomHelper.getMemberDetailsByID(memberId)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.SUCCESS)
        }
    }

    suspend fun getMemberDetailsByParentId(
        memberId: String,
    ): Resource<List<HouseholdMemberEntity>> {
        return try {
            val memberEntity = roomHelper.getMemberDetailsByParentId(memberId)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getMemberDetailsByPatientId(
        patientId: String,
    ): Resource<HouseholdMemberEntity> {
        return try {
            val memberEntity = roomHelper.getMemberDetailsByPatientId(patientId)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAssessmentMemberDetails(
        id: Long
    ): Resource<AssessmentMemberDetails> {
        return try {
            val memberEntity = roomHelper.getAssessmentMemberDetails(id)
            Resource(state = ResourceState.SUCCESS, data = memberEntity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    private suspend fun getMemberCountPerHouseHold(householdId: Long): Int {
        return roomHelper.getMemberCountPerHouseHold(householdId)
    }

    private suspend fun updateHeadCount(householdId: Long, newNoOfPeople: Int) {
        return roomHelper.updateHeadCount(householdId, newNoOfPeople)
    }

    suspend fun getPatientVisitCountByType(
        type: String,
        hhmLocalId: Long
    ): MemberClinicalEntity? {
        return roomHelper.getPatientVisitCountByType(type, hhmLocalId)
    }

    suspend fun savePatientVisitCountByType(memberClinicalEntity: MemberClinicalEntity) {
        roomHelper.savePatientVisitCountByType(memberClinicalEntity)
    }

    suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail? {
        return roomHelper.getPregnancyDetailByPatientId(hhmLocalId)
    }

    suspend fun savePregnancyDetail(pregnancyDetail: PregnancyDetail): Long {
        return roomHelper.savePregnancyDetail(pregnancyDetail)
    }

   suspend fun updateMemberDeceasedStatus(id: Long, status: Boolean) {
        return roomHelper.updateMemberDeceasedStatus(id, status)
    }

    suspend fun getPatientIdById(id: Long): String {
        return roomHelper.getPatientIdById(id)
    }

    suspend fun changeMemberDetailsToNotSynced(id: Long) {
        roomHelper.changeMemberDetailsToNotSynced(id)
    }

    fun getUnAssignedHouseholdMember(): LiveData<List<UnAssignedHouseholdMemberDetail>> {
        return roomHelper.getUnAssignedHouseholdMembersLiveData()
    }

    suspend fun addLinkMemberCall(memberId: String, callStartTime: Long, callEndTime: Long): Long {
        val callHistory = CallHistory(
            type = CALL_TYPE_LINK_HHM,
            referenceId = memberId,
            callStartTime = callStartTime,
            callEndTime = callEndTime
        )
        return roomHelper.addLinkMemberCall(callHistory)
    }

    suspend fun updateMemberAsAssigned(memberId: String) {
        roomHelper.updateMemberAsAssigned(memberId)
    }
}