package com.medtroniclabs.opensource.model

import androidx.room.ColumnInfo

data class MemberDobGenderModel(
    val gender: String,
    @ColumnInfo("date_of_birth")
    val dateOfBirth: String
)