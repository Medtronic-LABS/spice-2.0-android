package org.medtroniclabs.uhis.data.offlinesync.model

import org.medtroniclabs.uhis.db.entity.FollowUp
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail

data class ResponseMemberDetails(
    val member: HouseHoldMember,
    val followUps: List<FollowUp>?,
    val pregnancyInfos: List<PregnancyDetail>?,
    val assessmentHistory: List<MemberAssessmentHistoryEntity>?,
)
