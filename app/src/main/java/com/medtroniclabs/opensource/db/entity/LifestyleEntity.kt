package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "LifestyleEntity")
data class LifestyleEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val displayValue: String? = null,
    val value: String? = null,
    val answers: ArrayList<LifeStyleAnswer>,
    val type: String,
    val displayOrder: Int,
)

data class LifeStyleAnswer(
    val name: String,
    @SerializedName("is_answer_dependent")
    val isAnswerDependent: Boolean,
    val displayValue: String? = null,
    val value: String? = null
)

data class LifeStyleUIModel(
    val _id: Long,
    @SerializedName("display_order")
    val displayOrder: Int,
    val lifestyle: String,
    @SerializedName("lifestyle_answer")
    val lifestyleAnswer: ArrayList<LifeStyleAnswerUIModel>,
    @SerializedName("lifestyle_type")
    val lifestyleType: String,
    @SerializedName("culture_question_value")
    val cultureQuestionValue: String? = null,
    val value: String? = null
)


data class LifeStyleAnswerUIModel(
    val question: String? = null,
    val name: String,
    var isSelected: Boolean = false,
    @SerializedName("is_answer_dependent")
    val isAnswerDependent: Boolean,
    var comments: String? = null,
    @SerializedName("culture_answer_value")
    val cultureAnswerValue:String?= null,
    val value: String? = null
)