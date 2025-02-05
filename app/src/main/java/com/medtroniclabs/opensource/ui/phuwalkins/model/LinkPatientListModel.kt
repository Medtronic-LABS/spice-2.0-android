package com.medtroniclabs.opensource.ui.phuwalkins.model

data class LinkPatientListModel(
val name: String,
val age: String,  // For example: "48 Months" or "38"
val gender: String, // "M" or "F"
val village: String,
val mobileNumber: String
)
data class Household(
    val name: String,
    val householdNumber: Int,
    val mobileNumber: String
)