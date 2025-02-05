package com.medtroniclabs.opensource.data.offlinesync.model

import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import java.util.Locale

data class HouseHoldMember(

    @ColumnInfo("id")
    val referenceId: String?,

    @ColumnInfo(name = "fhir_id")
    var id: String? = null,

    @ColumnInfo("name")
    val name: String,

    @ColumnInfo("household_id")
    var householdReferenceId: String? = null,

    @ColumnInfo("household_fhir_id")
    var householdId: String? = null,

    @ColumnInfo("date_of_birth")
    val dateOfBirth: String,

    @ColumnInfo("phone_number")
    val phoneNumber: String,

    @ColumnInfo("phone_number_category")
    val phoneNumberCategory: String,

    @ColumnInfo("patient_id")
    val patientId: String? = null,

    @ColumnInfo("gender")
    val gender: String,

    @ColumnInfo("household_head_relationship")
    val householdHeadRelationship: String?,

    @ColumnInfo("created_at")
    val createdAt: Long,

    @ColumnInfo("updated_at")
    val updatedAt: Long,

    @ColumnInfo(name= "village_name")
    val village: String,

    @ColumnInfo(name = "village_id")
    val villageId: Long,

    @ColumnInfo(name = "parentId")
    val motherPatientId: String? = null,

    @ColumnInfo(name = "isPregnant")
    val isPregnant: Boolean? = null,

    val motherReferenceId: String? = null,

    val isActive: Boolean = true,

    val signature: String? = null,

    val initial: String? = null,

    val version: String? = null,

    val lastUpdated: String? = null,

    val assignHousehold: Boolean? = null,
) {

    @Ignore
    var isChild: Boolean? = false

    @Ignore
    var provenance: ProvanceDto = ProvanceDto(modifiedDate = updatedAt.convertToUtcDateTime())

    @Ignore
    var assessments = listOf<Assessment>()

    @Ignore
    var children: List<HouseHoldMember>? = null

    fun toHouseholdMemberEntity(hhId: Long?, status: OfflineSyncStatus, id: Long = 0): HouseholdMemberEntity {
        return HouseholdMemberEntity(
            id = id,
            name = this.name,
            phoneNumber = this.phoneNumber,
            phoneNumberCategory = toRegularCase(this.phoneNumberCategory),
            dateOfBirth = this.dateOfBirth,
            gender = this.gender,
            householdHeadRelationship = this.householdHeadRelationship ?: "",
            householdId = hhId,
            patientId = this.patientId,
            parentId = this.motherPatientId,
            isPregnant = this.isPregnant,
            villageId = this.villageId,
            isActive = this.isActive,
            signature = this.signature,
            initial = this.initial,
            version = this.version,
            lastUpdated = this.lastUpdated
        ).apply {
            fhirId = this@HouseHoldMember.id.toString()
            sync_status = status
        }
    }

    private fun toRegularCase(sentence: String): String {
        return sentence.split(" ")
            .joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
    }
}
