package com.medtroniclabs.opensource.ncd.data

import com.google.gson.annotations.SerializedName

data class InitialLifeStyle(
    var lifestyleQuestion: String? = null,
    @SerializedName("answer")
    var lifestyleAnswer: String? = null,
    var id: Long? = null,
    var comments: String? = null,
    @SerializedName("is_answer_dependent")
    var isAnswerDependent: Boolean = false,
    var questionValue: String? = null,
    var answerValue: String? = null,
)
