package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.db.entity.EntitiesName.ASSESSMENT
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus

@Entity(tableName = ASSESSMENT)
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val householdMemberLocalId: Long = 0,
    val memberId: String?, // Member -FHIR id
    val householdId: String?, // Household - FHIR id
    val patientId: String?, // member - patient Id
    val villageId: String, // Village Id of household
    val assessmentType: String,
    val assessmentDetails:String,
    var otherDetails: String? = null,
    var isReferred: Boolean = false,
    val referralStatus: ReferralStatus,
    val referredReason: ArrayList<String>?= null,
    val followUpId: Long? = null,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
): BaseEntity()
