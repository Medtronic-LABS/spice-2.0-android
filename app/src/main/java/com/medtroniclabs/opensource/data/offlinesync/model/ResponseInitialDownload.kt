package com.medtroniclabs.opensource.data.offlinesync.model

import com.medtroniclabs.opensource.db.entity.FollowUp
import com.medtroniclabs.opensource.db.entity.LinkHouseholdMember
import com.medtroniclabs.opensource.db.entity.PregnancyDetail

data class ResponseInitialDownload(
    val households: List<HouseHold>?,
    val members: List<HouseHoldMember>?,
    val pregnancyInfos: List<PregnancyDetail>?,
    val followUps: List<FollowUp>?,
    val followUpCriteria: FollowUpCriteria?,
    val householdMemberLinks: List<LinkHouseholdMember>?,
    val lastSyncTime: String,
)
