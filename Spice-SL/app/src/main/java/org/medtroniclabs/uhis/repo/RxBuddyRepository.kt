package org.medtroniclabs.uhis.repo

import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.db.entity.RxBuddyDetails
import org.medtroniclabs.uhis.db.entity.RxBuddyFollowUpEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RxBuddyRepository @Inject constructor(
    private var roomHelper: RoomHelper,
) {
    suspend fun insertRxBuddyDetails(
        rxBuddyId: Long?,
        patientMemberId: String,
        memberId: Long?,
        name: String?,
        phoneNumber: String?,
        relationship: String,
        otherRelationship: String?,
        isMonitorSheetProvider: Boolean,
        nextVisitDate: LocalDate,
        followUpId: Long? = null,
    ): Resource<Long> {
        val latLng = getLatLng()
        return try {
            val rxBuddy = RxBuddyDetails(
                rxBuddyId = rxBuddyId,
                patientMemberId = patientMemberId,
                householdMemberId = memberId,
                name = name,
                phoneNumber = phoneNumber,
                relationship = relationship,
                isMonitorSheetProvider = isMonitorSheetProvider,
                otherRelationship = otherRelationship,
                followUpId = followUpId,
                nextVisitDate = getNextVisitDate(nextVisitDate),
                latitude = latLng.first,
                longitude = latLng.second,
            )
            val response = roomHelper.insertRxBuddyDetails(rxBuddy)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR, data = null)
        }
    }

    suspend fun getOtherHouseholdMembersExcludeTBPatient(
        householdId: Long,
        patientId: Long,
    ): Resource<ArrayList<Map<String, Any>>> {
        val otherHouseholdMembers = roomHelper.getOtherHouseholdExcludeTBPatient(
            householdId,
            patientId,
        )

        val dropDownList = ArrayList<Map<String, Any>>()
        for ((_, householdMemberEntity) in otherHouseholdMembers.withIndex()) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to householdMemberEntity.name,
                    DefinedParams.ID to householdMemberEntity.id,
                ),
            )
        }

        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.OTHER,
                DefinedParams.ID to 0L,
            ),
        )
        return Resource(
            state = ResourceState.SUCCESS,
            data = dropDownList,
        )
    }

    suspend fun getRxBuddyDetails(patientMemberId: String): RxBuddyDetails? = roomHelper.getRxBuddyDetails(patientMemberId)

    suspend fun insertRxBuddyFollowUp(
        rxBuddyLocalId: Long,
        rxBuddyId: Long? = null,
        patientMemberId: String,
        map: HashMap<String, Any>,
        nextVisitDate: LocalDate,
        followUpId: Long? = null,
    ): Resource<Long> {
        val latLng = getLatLng()
        return try {
            val rxBuddyFollowUp = RxBuddyFollowUpEntity(
                rxBuddyLocalId = rxBuddyLocalId,
                rxBuddyId = rxBuddyId,
                patientMemberId = patientMemberId,
                followUp = StringConverter.convertGivenMapToString(map),
                nextVisitDate = getNextVisitDate(nextVisitDate),
                followUpId = followUpId,
                latitude = latLng.first,
                longitude = latLng.second,
            )
            val response = roomHelper.insertRxBuddyFollowUp(rxBuddyFollowUp)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR, data = null)
        }
    }

    suspend fun updateNextVisitDateRxBuddyRegister(
        nextVisitDate: String,
        id: Long,
    ): Resource<String> {
        roomHelper.updateNextVisitDateRxBuddyRegister(nextVisitDate, id)
        return Resource(state = ResourceState.SUCCESS)
    }

    suspend fun updateNextVisitDateRxBuddyFollowUp(
        nextVisitDate: String,
        id: Long,
    ): Resource<String> {
        roomHelper.updateNextVisitDateRxBuddyFollowUp(nextVisitDate, id)
        return Resource(state = ResourceState.SUCCESS)
    }

    private fun getNextVisitDate(date: LocalDate): String {
        val tomorrowDate = date.format(DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy))
        return DateUtils.convertDateTimeToDate(
            tomorrowDate,
            DateUtils.DATE_ddMMyyyy,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            inUTC = true,
        )
    }

    private fun getLatLng(): Pair<Double, Double> =
        Pair(
            SecuredPreference.getDouble(SecuredPreference.EnvironmentKey.CURRENT_LATITUDE.name),
            SecuredPreference.getDouble(SecuredPreference.EnvironmentKey.CURRENT_LONGITUDE.name),
        )

    suspend fun getUnSyncedRxBuddyRegisterCount(): Int = roomHelper.getUnSyncedRxBuddyRegisterCount()

    suspend fun getUnSyncedRxBuddyFollowUpCount(): Int = roomHelper.getUnSyncedRxBuddyFollowUpCount()
}
