package com.medtroniclabs.opensource.repo

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.ConsentFormType
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.data.model.HouseholdCardDetail
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHoldMember
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.ConsentForm
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.db.response.HouseHoldEntityWithMemberCount
import com.medtroniclabs.opensource.db.response.HouseholdMemberCount
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration
import com.medtroniclabs.opensource.model.medicalreview.AddMemberRegRequest
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import javax.inject.Inject

class HouseHoldRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {

    suspend fun getLastHouseholdNo(villageId: Long): Long? =
        roomHelper.getLastHouseholdNo(villageId)

    suspend fun getHouseHoldDetailsById(houseHoldId: Long) =
        roomHelper.getHouseHoldDetailsById(houseHoldId)

    suspend fun getAllHouseHoldMemberList(houseHoldId: Long): ArrayList<HouseholdMemberEntity> =
        roomHelper.getAllHouseHoldMemberList(houseHoldId)

    fun getMemberCountInHouseholdLiveData(houseHoldId: Long): LiveData<HouseholdMemberCount> {
        return roomHelper.getMemberCountInHouseholdLiveData(houseHoldId)
    }

    fun getFilteredHouseholdsLiveData(searchTerm: String, villageIds: List<Long>, status: String): LiveData<List<HouseHoldEntityWithMemberCount>> {
        return roomHelper.getFilteredHouseholdsLiveData(searchTerm, villageIds, status)
    }

    suspend fun getFormData(
        formType: String,
    ): Resource<String> {
        return try {
            val response = roomHelper.getFormData(formType)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }


    suspend fun getAllVillagesName(): Resource<List<VillageEntity>> {
        val response = roomHelper.getAllVillageEntity()
        return Resource(state = ResourceState.SUCCESS, data = response)
    }

    suspend fun createOrUpdateHouseHoldEntity(map: HashMap<String, Any>, entity: HouseholdEntity? = null): HouseholdEntity {
        val householdEntity = entity ?: HouseholdEntity()

        val householdName = map[HouseHoldRegistration.householdName]
        householdEntity.name = CommonUtils.getStringOrEmptyString(householdName)

        val headPhoneNumber = map[HouseHoldRegistration.headPhoneNumber]
        householdEntity.headPhoneNumber = CommonUtils.getStringOrEmptyString(headPhoneNumber)

        val headPhoneNumberCategory = map[HouseHoldRegistration.headPhoneNumberCategory]
        householdEntity.headPhoneNumberCategory = CommonUtils.getStringOrEmptyString(headPhoneNumberCategory)

        val landmark = map[HouseHoldRegistration.landmark]
        householdEntity.landmark = CommonUtils.getStringOrEmptyString(landmark)

        val villageID = map[HouseHoldRegistration.villageId]
        val villageLongID = CommonUtils.getLongOrNull(villageID) ?: 0
        householdEntity.villageId = villageLongID

        val isOwnedAnImprovedLatrine = map[HouseHoldRegistration.isOwnedAnImprovedLatrine]
        householdEntity.isOwnedAnImprovedLatrine = CommonUtils.getIsBooleanFromString(isOwnedAnImprovedLatrine)

        val isOwnedHandWashingFacilityWithSoap =
            map[HouseHoldRegistration.isOwnedHandWashingFacilityWithSoap]
        householdEntity.isOwnedHandWashingFacilityWithSoap = CommonUtils.getIsBooleanFromString(
            isOwnedHandWashingFacilityWithSoap
        )

        val isOwnedATreatedBedNet = map[HouseHoldRegistration.isOwnedATreatedBedNet]
        householdEntity.isOwnedATreatedBedNet = CommonUtils.getIsBooleanFromString(isOwnedATreatedBedNet)

        val bedNetCount = map[HouseHoldRegistration.bedNetCount]
        householdEntity.bedNetCount = CommonUtils.getIntegerOrNull(bedNetCount)

        val lastHouseHoldNo = getLastHouseholdNo(villageLongID) ?: 0

        if (entity != null) {
            householdEntity.updatedAt = System.currentTimeMillis()
            householdEntity.sync_status = OfflineSyncStatus.NotSynced

            val noOfPeople = map[HouseHoldRegistration.noOfPeople]
            householdEntity.noOfPeople = checkHeadCountOfHouseHold(CommonUtils.getIntegerOrNull(noOfPeople) ?: 0, getMemberCountPerHouseHold(entity.id))
        } else {
            //householdEntity.householdNo = lastHouseHoldNo + 1
            val noOfPeople = map[HouseHoldRegistration.noOfPeople]
            householdEntity.noOfPeople = CommonUtils.getIntegerOrNull(noOfPeople) ?: 0
        }
        return householdEntity
    }

    suspend fun insertHouseHoldEntity(householdEntity: HouseholdEntity): Long {
        return roomHelper.saveHouseHoldEntry(householdEntity)
    }

    suspend fun updateHouseholdHeadPhoneNumber(id: Long, phoneNumber: String?, phoneNumberCategory: String?) {
        roomHelper.updatePhoneNumberForHouseholdHead(id, phoneNumber, phoneNumberCategory)
    }

    suspend fun updateHouseHoldEntity(householdEntity: HouseholdEntity) {
        roomHelper.updateHousehold(householdEntity)
    }

    private fun checkHeadCountOfHouseHold(
        givenHeadCount: Int,
        memberCount: Int
    ): Int {
        return if (memberCount > givenHeadCount) {
            memberCount
        } else {
            givenHeadCount
        }
    }

    suspend fun getUserVillages(
        tag: String
    ): Resource<LocalSpinnerResponse> {
        return try {
            val response = roomHelper.getAllVillageEntity()
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getVillageByID(villageId: Long): Resource<VillageEntity> {
        val response = roomHelper.getVillageByID(villageId)
        return Resource(state = ResourceState.SUCCESS, data = response)
    }

    suspend fun getMemberCountPerHouseHold(householdId: Long): Int {
        return roomHelper.getMemberCountPerHouseHold(householdId)
    }

    private suspend fun insertHouseholdMembers(householdMembers: List<HouseHoldMember>?, hhIdMap: Map<String, Long>) {
        householdMembers?.forEach { member ->
            hhIdMap[member.householdId]?.let {
                roomHelper.registerMember(
                    member.toHouseholdMemberEntity(
                        it,
                        OfflineSyncStatus.Success
                    )
                )
            }
        }
    }

    suspend fun getUnSyncedHouseholdCount(): Int {
        return roomHelper.getUnSyncedHouseholdCount()
    }

    suspend fun getUnSyncedHouseholdMemberCount(): Int {
        return roomHelper.getUnSyncedHouseholdMemberCount()
    }

    fun getHouseholdCardDetailLiveData(id: Long): LiveData<HouseholdCardDetail> {
        return roomHelper.getHouseholdCardDetailLiveData(id)
    }

    fun getAllHouseHoldMembersLiveData(hhId: Long) : LiveData<List<HouseholdMemberEntity>> {
        return roomHelper.getAllHouseHoldMembersLiveData(hhId)
    }

    fun getAliveHouseHoldMembersLiveData(hhId: Long) : List<HouseholdMemberEntity> {
        return roomHelper.getAliveHouseHoldMembersLiveData(hhId)
    }

    suspend fun addNewMember(request: AddMemberRegRequest): Resource<String> {
        return try{
            val response = apiHelper.addNewMember(request)
            if (response.isSuccessful) {
                Resource(ResourceState.SUCCESS, response.body()?.entity)
            } else {
                val errorMessage = StringConverter.getErrorMessage(response.errorBody())
                Resource(state = ResourceState.ERROR, message = errorMessage)
            }
        } catch (e: Exception) {
            Resource(ResourceState.ERROR, message = e.localizedMessage)
        }
    }

    suspend fun getConsentForm() : ConsentForm? {
        return roomHelper.getConsentFormByType(ConsentFormType.Household)
    }
}