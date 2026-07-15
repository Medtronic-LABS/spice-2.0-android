package org.medtroniclabs.uhis.repo

import android.content.Context
import androidx.lifecycle.LiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.ConsentFormType
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.dao.HouseholdSortOrder
import org.medtroniclabs.uhis.db.entity.ConsentForm
import org.medtroniclabs.uhis.db.entity.HouseholdEntity
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration
import org.medtroniclabs.uhis.model.household.HouseHoldFilterUiData
import org.medtroniclabs.uhis.model.medicalreview.AddMemberRegRequest
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import javax.inject.Inject

class HouseHoldRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper,
) {
    suspend fun getHouseHoldDetailsById(houseHoldId: Long) = roomHelper.getHouseHoldDetailsById(houseHoldId)

    suspend fun getAllHouseHoldMemberList(houseHoldId: Long): ArrayList<HouseholdMemberEntity> = roomHelper.getAllHouseHoldMemberList(houseHoldId)

    fun getFilteredHouseholdsLiveData(
        searchTerm: String,
        ssIds: List<Long>,
        subVillageIds: List<Long> = emptyList(),
        sortOrder: HouseholdSortOrder = HouseholdSortOrder.DEFAULT,
    ): LiveData<List<HouseHoldEntityWithLastActivity>> =
        roomHelper.getFilteredHouseholdsLiveData(
            searchInput = searchTerm,
            filterBySs = ssIds,
            filterBySubVillages = subVillageIds,
            sortOrder = sortOrder,
        )

    suspend fun getFormData(formType: String): Resource<String> =
        try {
            val response = when (formType) {
                DefinedParams.HOUSEHOLD_MEMBER_REGISTRATION -> {
                    CommonUtils.getStringFromAssets(
                        DefinedParams.HOUSEHOLD_MEMBER_REGISTRATION_FORM + ".json",
                        context.assets,
                    )
                }
                DefinedParams.EXTERNAL_MEMBER_REGISTRATION -> {
                    CommonUtils.getStringFromAssets(
                        DefinedParams.EXTERNAL_MEMBER_REGISTRATION + ".json",
                        context.assets,
                    )
                }
                DefinedParams.HOUSEHOLD_REGISTRATION -> {
                    CommonUtils.getStringFromAssets(
                        DefinedParams.HOUSEHOLD_REGISTRATION + ".json",
                        context.assets,
                    )
                }
                else -> {
                    roomHelper.getFormData(formType)
                }
            }
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    /** FO/PO Services filter: Shasthya Kormi list for the SK spinner (SS loads after a Kormi is chosen). */
    suspend fun getHouseHoldFilterUiDataForServiceRecipient(): Resource<HouseHoldFilterUiData> =
        try {
            Resource(
                state = ResourceState.SUCCESS,
                HouseHoldFilterUiData(skList = roomHelper.getAllShasthyaKormis()),
            )
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    /** FO/PO Services filter: SK + SS for the selected Kormi (sub-villages load on SS selection in the UI). */
    suspend fun getHouseHoldFilterUiDataForShasthyaKormi(kormiId: Long): Resource<HouseHoldFilterUiData> =
        try {
            Resource(
                state = ResourceState.SUCCESS,
                HouseHoldFilterUiData(
                    ssList = roomHelper.getShasthyaShebikaByShasthyaKormiId(kormiId),
                    skList = roomHelper.getAllShasthyaKormis(),
                ),
            )
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun createOrUpdateHouseHoldEntity(
        map: HashMap<String, Any>,
        entity: HouseholdEntity? = null,
    ): HouseholdEntity {
        val householdEntity = entity ?: HouseholdEntity()

        val householdName = map[HouseHoldRegistration.HOUSEHOLD_NAME]
        var nameFromMap = CommonUtils.getStringOrEmptyString(householdName)
        // If updating and household name is not provided or empty, get it from household head member
        if (entity != null && nameFromMap.isEmpty()) {
            val householdHeadMember = getAllHouseHoldMemberList(entity.id)
                .firstOrNull { it.isHouseholdHead && it.isActive }
            householdHeadMember?.let {
                nameFromMap = it.name
            }
        }
        householdEntity.name = nameFromMap

        val villageID = map[HouseHoldRegistration.VILLAGE_ID]
        val villageLongID = CommonUtils.getLongOrNull(villageID) ?: 0
        householdEntity.villageId = villageLongID

        val shasthyaShebikaId = map[HouseHoldRegistration.SHASTHYA_SHEBIKA_ID]
        householdEntity.shasthyaShebikaId = CommonUtils.getLongOrNull(shasthyaShebikaId)

        val subVillageId = map[HouseHoldRegistration.SUB_VILLAGE_ID]
        householdEntity.subVillageId = CommonUtils.getLongOrNull(subVillageId)

        val householdType = map[HouseHoldRegistration.HOUSEHOLD_TYPE]
        householdEntity.householdType = CommonUtils.getStringOrEmptyString(householdType).takeIf { it.isNotEmpty() }

        val monthlyIncomeRange = map[HouseHoldRegistration.MONTHLY_INCOME_RANGE]
        householdEntity.monthlyIncomeRange = CommonUtils.getStringOrEmptyString(monthlyIncomeRange).takeIf { it.isNotBlank() }

        val occupation = map[HouseHoldRegistration.HOUSEHOLD_HEAD_OCCUPATION]
        householdEntity.householdHeadOccupation = CommonUtils.getStringOrEmptyString(occupation).takeIf { it.isNotEmpty() }

        val otherOccupation = map[HouseHoldRegistration.OTHER_OCCUPATION]
        householdEntity.otherOccupation = CommonUtils.getStringOrEmptyString(otherOccupation).takeIf { it.isNotEmpty() }

        val currentTime = System.currentTimeMillis()

        if (entity != null) {
            householdEntity.sync_status = OfflineSyncStatus.NotSynced

            val noOfPeople = map[HouseHoldRegistration.NO_OF_PEOPLE] ?: map[HouseHoldRegistration.TOTAL_MEMBERS]
            householdEntity.noOfPeople = checkHeadCountOfHouseHold(CommonUtils.getIntegerOrNull(noOfPeople) ?: 0, getMemberCountPerHouseHold(entity.id))

            val disabilityPersonsCount = map[HouseHoldRegistration.ID_DISABILITY_PERSONS_COUNT]
            householdEntity.disabilityPersonsCount =
                checkHeadCountOfHouseHold(CommonUtils.getIntegerOrNull(disabilityPersonsCount) ?: 0, getDisabilityMembersCountPerHousehold(entity.id))
        } else {
            householdEntity.createdAt = currentTime
            // Use household number from form if provided, otherwise generate new one
            val householdNumberFromForm = map[HouseHoldRegistration.HOUSEHOLD_NUMBER]
            householdEntity.householdNo = householdNumberFromForm as? String
                ?: // Fallback: generate if not provided (shouldn't happen if form is populated correctly)
                "HH${System.currentTimeMillis()}"
            val noOfPeople = map[HouseHoldRegistration.NO_OF_PEOPLE] ?: map[HouseHoldRegistration.TOTAL_MEMBERS]
            householdEntity.noOfPeople = CommonUtils.getIntegerOrNull(noOfPeople) ?: 0

            val disabilityPersonsCount = map[HouseHoldRegistration.ID_DISABILITY_PERSONS_COUNT]
            householdEntity.disabilityPersonsCount = CommonUtils.getIntegerOrNull(disabilityPersonsCount) ?: 0
        }
        householdEntity.updatedAt = currentTime
        return householdEntity
    }

    suspend fun insertHouseHoldEntity(householdEntity: HouseholdEntity): Long = roomHelper.saveHouseHoldEntry(householdEntity)

    suspend fun getShasthyaShebikasByKormiId(shasthyaKormiId: Long): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getShasthyaShebikaByShasthyaKormiId(shasthyaKormiId)
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse("shasthya_shebika_id", response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getAllShasthyaKormisSpinner(): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getAllShasthyaKormis()
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse("shasthya_kormi_id", response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getSubVillagesByShasthyaShebikaId(shasthyaShebikaId: Long): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getSubVillagesByShasthyaShebikaId(shasthyaShebikaId)
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse("sub_village_id", response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    /**
     * CHCP registration drives the Union -> Sub-village cascade directly (no Shasthya
     * Kormi/Shebika), so sub-villages are resolved by their parent Union [villageId].
     */
    suspend fun getSubVillagesByVillageId(villageId: Long): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getSubVillage(villageId)
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse("sub_village_id", response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun updateHouseHoldEntity(householdEntity: HouseholdEntity) {
        roomHelper.updateHousehold(householdEntity)
    }

    private fun checkHeadCountOfHouseHold(
        givenHeadCount: Int,
        memberCount: Int,
    ): Int =
        if (memberCount > givenHeadCount) {
            memberCount
        } else {
            givenHeadCount
        }

    suspend fun getUserVillages(tag: String): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getAllVillageEntity()
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getUserLinkedVillages(tag: String): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getAllLinkedVillageEntity()
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getGuardianMembers(
        tag: String,
        hhId: Long,
        hhmId: Long,
    ): Resource<LocalSpinnerResponse> =
        try {
            val response = roomHelper.getOtherHouseholdExcludeTBPatient(hhId, hhmId)
            Resource(state = ResourceState.SUCCESS, LocalSpinnerResponse(tag, response))
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getVillageByID(villageId: Long): Resource<VillageEntity> {
        val response = roomHelper.getVillageByID(villageId)
        return Resource(state = ResourceState.SUCCESS, data = response)
    }

    suspend fun getMemberCountPerHouseHold(householdId: Long): Int = roomHelper.getMemberCountPerHouseHold(householdId)

    suspend fun getDisabilityMembersCountPerHousehold(householdId: Long): Int = roomHelper.getDisabilityMembersCountForHousehold(householdId)

    suspend fun getUnSyncedHouseholdCount(): Int = roomHelper.getUnSyncedHouseholdCount()

    suspend fun getUnSyncedHouseholdMemberCount(): Int = roomHelper.getUnSyncedHouseholdMemberCount()

    fun getHouseholdCardDetailLiveData(id: Long): LiveData<List<HouseHoldEntityWithLastActivity>> =
        roomHelper.getFilteredHouseholdsLiveData(
            searchInput = "",
            filterByHhIds = listOf(id),
        )

    fun getAllHouseHoldMembersLiveData(hhId: Long): LiveData<List<HouseholdMemberWithTb>> = roomHelper.getAllHouseHoldMembersLiveData(hhId)

    fun getAliveHouseHoldMembersLiveData(hhId: Long): List<HouseholdMemberEntity> = roomHelper.getAliveHouseHoldMembersLiveData(hhId)

    suspend fun addNewMember(request: AddMemberRegRequest): Resource<String> =
        try {
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

    suspend fun getConsentForm(): ConsentForm? =
        if (CommonUtils.parseUserLocale() == DefinedParams.EN) {
            roomHelper.getConsentFormByType(ConsentFormType.Household)
        } else {
            roomHelper.getConsentFormByType(ConsentFormType.HouseHoldCulture)
        }

    suspend fun updateHouseholdMemberTbContactTraceStatus(
        hhmId: Long,
        tbContactTracingStatus: Int,
    ) = roomHelper.updateTBContactTraceStatus(hhmId, tbContactTracingStatus)

    suspend fun getHouseholdsCountBasedSubVillage(subVillageId: Long) = roomHelper.getHouseholdsCountBasedSubVillage(subVillageId)

    /**
     * Returns true when any compared business field differs.
     */
    fun hasMeaningfulHouseholdChanges(
        before: HouseholdEntity,
        after: HouseholdEntity,
    ): Boolean {
        if (before.name != after.name) return true
        if (before.villageId != after.villageId) return true
        if (before.shasthyaShebikaId != after.shasthyaShebikaId) return true
        if (before.subVillageId != after.subVillageId) return true
        if (before.householdType != after.householdType) return true
        if (before.monthlyIncomeRange != after.monthlyIncomeRange) return true
        if (before.householdHeadOccupation != after.householdHeadOccupation) return true
        if (before.otherOccupation != after.otherOccupation) return true
        if (before.noOfPeople != after.noOfPeople) return true
        if (before.disabilityPersonsCount != after.disabilityPersonsCount) return true
        return false
    }
}
