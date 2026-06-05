package org.medtroniclabs.uhis.data.registration

data class ResponsePatientDetail(
    val id: String,
    val name: String? = null,
    val birthDate: String? = null,
    val patientId: String? = null,
    val gender: String? = null,
    val subVillage: String? = null,
    val phoneNumber: String? = null,
    val age: Int? = null,
    val memberReference: String? = null,
    val villageId: String? = null,
    val subVillageId: String? = null,
    val phoneNumberCategory: String? = null,
    val isActive: Boolean? = null,
    val identityType: String? = null,
    val identityValue: String? = null,
)
