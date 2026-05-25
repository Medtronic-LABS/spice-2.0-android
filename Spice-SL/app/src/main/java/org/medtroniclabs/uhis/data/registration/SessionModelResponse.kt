package org.medtroniclabs.uhis.data.registration

data class SessionModelResponse(
    val id: Long,
    val questions: String,
    val displayOrder: Int,
    val type: String,
    val cultureValue: String?,
    var mandatory: Boolean,
    val modelAnswers: ArrayList<AnswerModel>?,
    val select: Boolean,
)

data class AnswerModel(
    val id: Long,
    val answer: String,
    var isSelected: Boolean = false,
    var comments: String? = null,
    val displayOrder: Int?,
    val cultureValue: String?,
    val value: Int?,
)
