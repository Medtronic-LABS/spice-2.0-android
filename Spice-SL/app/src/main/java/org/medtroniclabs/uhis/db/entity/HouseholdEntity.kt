package org.medtroniclabs.uhis.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.db.entity.EntitiesName.HOUSEHOLD

@Entity(
    tableName = HOUSEHOLD,
    indices = [
        Index(value = ["sub_village_id"], name = "idx_sub_village_id"),
        Index(value = ["village_id"], name = "idx_village_id"),
        Index(value = ["shasthya_shebika_id"], name = "id_shasthya_shebika_id"),
        Index(value = ["household_no"], name = "idx_household_no"),
        Index(value = ["updated_at"], name = "idx_household_updated_at"),
    ],
)
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "household_no")
    var householdNo: String? = null,
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "village_id")
    var villageId: Long = 0,
    @ColumnInfo(name = "no_of_people")
    var noOfPeople: Int = 0,
    @ColumnInfo(name = "shasthya_shebika_id")
    var shasthyaShebikaId: Long? = null,
    @ColumnInfo(name = "sub_village_id")
    var subVillageId: Long? = null,
    @ColumnInfo(name = "household_type")
    var householdType: String? = null,
    @ColumnInfo(name = "monthly_income")
    var monthlyIncome: Double? = null,
    @ColumnInfo(name = HH_COLUMN_MONTHLY_INCOME_RANGE)
    var monthlyIncomeRange: String? = null,
    @ColumnInfo("latitude")
    var latitude: Double = 0.0,
    @ColumnInfo("longitude")
    var longitude: Double = 0.0,
    val version: String? = null,
    val lastUpdated: String? = null,
    @ColumnInfo("disability_persons_count")
    var disabilityPersonsCount: Int? = null,
    @ColumnInfo("household_head_occupation")
    var householdHeadOccupation: String? = null,
    @ColumnInfo("other_occupation")
    var otherOccupation: String? = null,
) : BaseEntity()

const val HH_COLUMN_MONTHLY_INCOME_RANGE = "monthly_income_range"
