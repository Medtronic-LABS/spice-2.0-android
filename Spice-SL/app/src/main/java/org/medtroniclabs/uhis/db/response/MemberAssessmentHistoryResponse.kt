package org.medtroniclabs.uhis.db.response

import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail

/**
 * Data class representing a member along with their assessment history.
 *
 * @property member The member entity details.
 * @property history A list of assessment history records associated with the member.
 * @property recentPregnancy The pregnancy episode with the latest [PregnancyDetail.endAt] (tie-break: highest id).
 * @property householdHeadName Name of the active household head when [member] belongs to a household; null otherwise.
 */
data class MemberAssessmentHistoryResponse(
    val member: HouseholdMemberEntity,
    val history: List<MemberAssessmentHistoryEntity>,
    val recentPregnancy: PregnancyDetail? = null,
    val householdHeadName: String? = null,
)
