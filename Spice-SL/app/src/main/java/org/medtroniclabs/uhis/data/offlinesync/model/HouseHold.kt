package org.medtroniclabs.uhis.data.offlinesync.model

import androidx.room.ColumnInfo
import androidx.room.Ignore
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.HH_COLUMN_MONTHLY_INCOME_RANGE
import org.medtroniclabs.uhis.db.entity.HouseholdEntity

data class HouseHold(
    @ColumnInfo(name = "id")
    var referenceId: String?,
    @ColumnInfo(name = "fhir_id")
    var id: String? = null,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "villageName")
    val village: String,
    @ColumnInfo(name = "village_id")
    val villageId: Long,
    @ColumnInfo(name = "no_of_people")
    val noOfPeople: Int,
    @ColumnInfo(name = "household_no")
    val householdNo: String? = null,
    @ColumnInfo(name = "shasthya_shebika_id")
    val shasthyaShebikaId: Long? = null,
    @ColumnInfo(name = "sub_village_id")
    val subVillageId: Long? = null,
    @ColumnInfo(name = "household_type")
    val householdType: String? = null,
    @ColumnInfo(name = "monthly_income")
    val monthlyIncome: Double? = null,
    @ColumnInfo(name = HH_COLUMN_MONTHLY_INCOME_RANGE)
    var monthlyIncomeRange: String? = null,
    @ColumnInfo(name = "latitude")
    val latitude: Double = 0.0,
    @ColumnInfo(name = "longitude")
    val longitude: Double = 0.0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    val version: String? = null,
    val lastUpdated: String? = null,
    @ColumnInfo("disability_persons_count")
    var disabilityPersonsCount: Int? = null,
    @ColumnInfo("household_head_occupation")
    val householdHeadOccupation: String? = null,
    @ColumnInfo("other_occupation")
    val otherOccupation: String? = null,
) {
    @Ignore
    var provenance: ProvanceDto = ProvanceDto(modifiedDate = updatedAt.convertToUtcDateTime())

    @Ignore
    var householdMembers: MutableList<HouseHoldMember> = mutableListOf()

    fun toHouseholdEntity(
        status: OfflineSyncStatus = OfflineSyncStatus.Success,
        id: Long = 0,
    ): HouseholdEntity =
        HouseholdEntity(
            id = id,
            householdNo = this.householdNo,
            name = this.name,
            villageId = this.villageId,
            noOfPeople = this.noOfPeople,
            shasthyaShebikaId = this.shasthyaShebikaId,
            subVillageId = this.subVillageId,
            householdType = this.householdType,
            monthlyIncome = this.monthlyIncome,
            monthlyIncomeRange = this.monthlyIncomeRange,
            latitude = this.latitude,
            longitude = this.longitude,
            version = this.version,
            lastUpdated = this.lastUpdated,
            disabilityPersonsCount = this.disabilityPersonsCount,
            householdHeadOccupation = this.householdHeadOccupation,
            otherOccupation = this.otherOccupation,
        ).apply {
            val newCreateAt = if (this@HouseHold.createdAt == null || this@HouseHold.createdAt == 0L) System.currentTimeMillis() else this@HouseHold.createdAt
            val newUpdatedAt = if (this@HouseHold.updatedAt == null || this@HouseHold.updatedAt < newCreateAt) newCreateAt else this@HouseHold.updatedAt
            fhirId = this@HouseHold.id.toString()
            sync_status = status
            createdAt = newCreateAt
            updatedAt = newUpdatedAt
        }
}
