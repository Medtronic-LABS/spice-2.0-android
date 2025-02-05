package com.medtroniclabs.opensource.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.db.entity.EntitiesName.HOUSEHOLD

@Entity(tableName = HOUSEHOLD)
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "household_no")
    var householdNo: Long? = null,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "village_id")
    var villageId: Long = 0,

    @ColumnInfo(name = "landmark")
    var landmark: String? = null,

    @ColumnInfo(name = "head_phone_number")
    var headPhoneNumber: String? = null,

    @ColumnInfo(name = "head_phone_number_category")
    var headPhoneNumberCategory: String? = null,

    @ColumnInfo(name = "no_of_people")
    var noOfPeople: Int = 0,

    @ColumnInfo("is_owned_an_improved_latrine")
    var isOwnedAnImprovedLatrine: Boolean = false,

    @ColumnInfo("is_owned_hand_washing_facility_with_soap")
    var isOwnedHandWashingFacilityWithSoap: Boolean = false,

    @ColumnInfo("is_owned_a_treated_bed_net")
    var isOwnedATreatedBedNet: Boolean = false,

    @ColumnInfo("bed_net_count")
    var bedNetCount: Int? = null,

    @ColumnInfo("latitude")
    var latitude: Double = 0.0,

    @ColumnInfo("longitude")
    var longitude: Double = 0.0,

    val version: String? = null,

    val lastUpdated: String? = null,
): BaseEntity()

