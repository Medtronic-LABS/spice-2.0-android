package org.medtroniclabs.uhis.data.registration

import java.io.Serializable

data class SessionGraphModel(
    val srq24Scores: ArrayList<SessionGraphItem>? = null,
    val moodScores: ArrayList<SessionGraphItem>? = null,
    val piScores: ArrayList<SessionGraphItem>? = null,
) : java.io.Serializable

data class SessionGraphItem(
    val score: Int? = null,
    val session: Int? = null,
    var type: String? = null,
) : Serializable
