package org.medtroniclabs.uhis.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.db.entity.EntitiesName.HOUSEHOLD_MEMBER

@Entity(
    tableName = HOUSEHOLD_MEMBER,
    indices = [
        Index(value = ["household_id"], name = "idx_household_id"),
        Index(value = ["updated_at"], name = "idx_HouseholdMember_updated_at"),
        Index(value = ["fhir_id"], name = "idx_HouseholdMember_fhir_id", unique = true),
        Index(value = ["guardian_hh_member_fhir_id"], name = "idx_guardian_hh_member_fhir_id"),
        Index(value = ["disability"], name = "idx_HouseholdMember_disability"),
    ],
)
data class HouseholdMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo("name")
    var name: String = "",
    @ColumnInfo("phone_number")
    var phoneNumber: String? = "",
    @ColumnInfo("phone_number_category")
    var phoneNumberCategory: String? = "",
    @ColumnInfo("date_of_birth")
    var dateOfBirth: String = "",
    @ColumnInfo("gender")
    var gender: String = "",
    @ColumnInfo("household_id")
    var householdId: Long? = null,
    @ColumnInfo("villageId")
    var villageId: Long? = null,
    @ColumnInfo("shasthya_shebika_id")
    var shasthyaShebikaId: Long? = null,
    @ColumnInfo("shasthya_kormi_id")
    var shasthyaKormiId: Long? = null,
    @ColumnInfo("chiefdom_id")
    var chiefdomId: Long? = null,
    @ColumnInfo("sub_village_id")
    var subVillageId: Long? = null,
    @ColumnInfo("patient_id")
    var patientId: String? = null,
    var isActive: Boolean = true,
    val version: String? = null,
    val lastUpdated: String? = null,
    var motherReferenceId: Long? = null,
    var deceasedReason: String? = null,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    @ColumnInfo("id_type")
    var idType: String = "",
    @ColumnInfo("national_id")
    var nationalId: String? = null,
    @ColumnInfo("is_house_hold_head")
    var isHouseholdHead: Boolean = false,
    @ColumnInfo("household_fhir_id")
    var householdFhirId: String? = null,
    @ColumnInfo("guardian_hh_member_id")
    var guardianId: Long? = null,
    @ColumnInfo("guardian_hh_member_fhir_id")
    var guardianFhirId: String? = null,
    @ColumnInfo("marital_status")
    var maritalStatus: String? = null,
    @ColumnInfo("disability")
    var disability: String? = null,
) : BaseEntity()
