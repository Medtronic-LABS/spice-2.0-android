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
        Index(value = ["serviceProvided"], name = "idx_member_assessment_history_service_provided"),
        Index(value = [COLUMN_PRACTITIONER_ID], name = INDEX_PRACTITIONER_ID),
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
    val referralReason: String?,
    var nextFollowUpDate: String? = null,
    @ColumnInfo(COLUMN_PRACTITIONER_ID)
    var practitionerId: String? = null,
)

const val COLUMN_PRACTITIONER_ID = "practitionerId"
const val INDEX_PRACTITIONER_ID = "idx_member_assessment_history_practitioner_id"
