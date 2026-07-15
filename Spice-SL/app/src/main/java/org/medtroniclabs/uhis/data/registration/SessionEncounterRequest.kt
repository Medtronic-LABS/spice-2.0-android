package org.medtroniclabs.uhis.data.registration

data class SessionEncounterRequest(
    var session: Int? = null,
    var srq24Score: Int? = null,
    var moodScore: Int? = null,
    var srq24: ArrayList<Map<String, Any>>? = null,
    var subjectiveQuestionaries: ArrayList<InitialSubjectives>? = ArrayList(),
    var patientTrackId: Long? = -1,
    var patientVisitId: Long? = null,
    var tenantId: Long? = -1,
    var nextSessionDate: String? = null,
    var pcMentalHealth: ArrayList<Map<String, Any>>? = null,
    var piScore: Int? = null,
)

data class InitialSubjectives(
    val question: String? = null,
    var answer: String? = null,
    val questionId: Long,
    var answerId: Long? = null,
    var comments: String? = null,
    var cultureValue: String? = null,
    var answerCulture: String? = null,
)

data class SessionQaModel(
    val question: String,
    val answer: String? = null,
    val comment: String? = null,
    val cultureValue: String? = null,
    val answerCulture: String? = null,
)
