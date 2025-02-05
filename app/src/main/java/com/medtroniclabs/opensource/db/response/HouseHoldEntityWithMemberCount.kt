package com.medtroniclabs.opensource.db.response

import androidx.room.ColumnInfo

data class HouseHoldEntityWithMemberCount(
    val id: Long,
    @ColumnInfo("household_no")
    val householdNo: Long? = null,
    val name: String,
    @ColumnInfo("village_id")
    val villageId: Long,
    val landmark: String? = null,
    @ColumnInfo("head_phone_number")
    val headPhoneNumber: String? = null,
    @ColumnInfo("no_of_people")
    val noOfPeople: Int,
    @ColumnInfo("is_owned_an_improved_latrine")
    val isOwnedAnImprovedLatrine: Boolean,
    @ColumnInfo("is_owned_hand_washing_facility_with_soap")
    val isOwnedHandWashingFacilityWithSoap: Boolean,
    @ColumnInfo("is_owned_a_treated_bed_net")
    val isOwnedATreatedBedNet: Boolean,
    @ColumnInfo("bed_net_count")
    val bedNetCount: Int? = null,
    @ColumnInfo("member_count")
    val registerMemberCount: Int = 0,
    @ColumnInfo("village_name")
    val villageName: String
)