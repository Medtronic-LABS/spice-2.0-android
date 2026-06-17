package org.medtroniclabs.uhis.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.medtroniclabs.uhis.db.entity.EntitiesName.MEMBER_ASSESSMENT_HISTORY_ENTITY

/**
 * Member assessment history entity to store history of assessments
 */
@Entity(
    tableName = MEMBER_ASSESSMENT_HISTORY_ENTITY,
    indices = [
        Index(value = ["visitDate"], name = "idx_member_assessment_history_visit_date"),
        Index(value = ["memberId"], name = "idx_member_assessment_history_member_id"),
        Index(value = ["memberId", "visitDate"], name = INDEX_MAH_MEMBER_VISIT),
        Index(value = ["memberFhirId"], name = "idx_member_assessment_history_member_fhir_id"),
        Index(value = ["serviceProvided"], name = "idx_member_assessment_history_service_provided"),
        Index(value = [MAH_COLUMN_PRACTITIONER_ID], name = INDEX_MAH_PRACTITIONER_ID),
        Index(value = ["memberId", "serviceProvided", "visitDate"], name = INDEX_MAH_MEMBER_SERVICE_VISIT),
    ],
)
data class MemberAssessmentHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @SerializedName("householdMemberId")
    val memberFhirId: String? = null,
    val memberId: Long? = null,
    val visitDate: String?,
    val serviceProvided: String?,
    val encounterId: String? = null,
    val customStatus: ArrayList<String>? = null,
    val latestVisit: Boolean,
    val referralStatus: String?,
    @ColumnInfo(name = MAH_COLUMN_REFERRAL_FACILITY_TYPE)
    val referralFacilityType: String? = null,
    val referralReason: String?,
    var nextFollowUpDate: String? = null,
    val serviceProvidedByName: String? = null,
    val serviceProvidedByRole: String? = null,
    @ColumnInfo(MAH_COLUMN_PRACTITIONER_ID)
    var practitionerId: String? = null,
    val observations: MemberAssessmentObservations? = null,
)

const val MAH_COLUMN_PRACTITIONER_ID = "practitionerId"
const val MAH_COLUMN_REFERRAL_FACILITY_TYPE = "referralFacilityType"
const val INDEX_MAH_PRACTITIONER_ID = "idx_mah_practitioner_id"
const val INDEX_MAH_MEMBER_VISIT = "idx_mah_member_visit"
const val INDEX_MAH_MEMBER_SERVICE_VISIT = "idx_mah_member_service_visit"
