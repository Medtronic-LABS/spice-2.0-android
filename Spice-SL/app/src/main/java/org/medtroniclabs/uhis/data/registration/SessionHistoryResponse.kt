package org.medtroniclabs.uhis.data.registration

data class SessionHistoryResponse(
    var sessionHistory: ArrayList<SessionModel>? = null,
    val paraCounsellingDetails: SessionDetailResponse? = null,
    var canUpdateDate: Boolean = false,
)

data class SessionDetailResponse(
    val session: Int? = null,
    val srq24Score: Int? = null,
    val moodScore: Int? = null,
    val createdAt: String? = null,
    val srq24: ArrayList<Map<String, Any>>? = null,
    val subjectiveQuestionaries: ArrayList<InitialSubjectives>? = null,
    val patientParaCounsellingId: Long? = null,
    val patientTrackId: Long? = null,
    val patientVisitId: Long? = null,
    val piScore: Int? = null,
)

data class SessionModel(
    val session: Int,
    val visitId: Long,
)
