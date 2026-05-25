package org.medtroniclabs.uhis.data.registration

data class MedicationSearchReqModel(
    val searchTerm: String? = null,
    val isQualipharmEnabledSite: Boolean? = null,
    val countryId: Long? = null,
)
