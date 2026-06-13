package org.medtroniclabs.uhis.data.offlinesync.model

import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity

data class HouseholdMemberWithTb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo("name")
    var name: String = "",
    @ColumnInfo("phone_number")
    var phoneNumber: String? = "",
    @ColumnInfo("date_of_birth")
    var dateOfBirth: String = "",
    @ColumnInfo("gender")
    var gender: String = "",
    @ColumnInfo("household_id")
    var householdId: Long? = null,
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
    @ColumnInfo("fhir_id")
    val fhirId: String? = null,
    val diagnoses: String? = null,
    @ColumnInfo("recent_service_date")
    val recentServiceDate: Long? = null,
    @ColumnInfo("shasthya_shebika_name")
    val shasthyaShebikaName: String? = null,
    @ColumnInfo("shasthya_shebika_ssId")
    val shasthyaShebikaNameSsId: String? = null,
    @ColumnInfo("sub_village_name")
    val subVillageName: String? = null,
    @ColumnInfo("created_by_role_name")
    val createdByRoleName: String? = null,
) {
    @Ignore
    var assessmentHistory: List<MemberAssessmentHistoryEntity> = emptyList()
}
