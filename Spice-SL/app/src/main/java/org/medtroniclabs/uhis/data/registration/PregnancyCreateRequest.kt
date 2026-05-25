package org.medtroniclabs.uhis.data.registration

data class PregnancyCreateRequest(
    var id: Long?,
    var patientPregnancyId: Long?,
    var patientTrackId: Long?,
    var tenantId: Long?,
    var gravida: Int? = null,
    var parity: Int? = null,
    var temperature: Double? = null,
    var lastMenstrualPeriodDate: String? = null,
    var estimatedDeliveryDate: String? = null,
    var pregnancyFetusesNumber: Int? = null,
    var diagnosis: ArrayList<String>? = null,
    var diagnosisList: ArrayList<HashMap<String, Any>>? = null,
    var isOnTreatment: Boolean? = null,
    var diagnosisTime: String? = null,
    var gestationalAge: Int? = null,
    var unitMeasurement: String? = null,
    var neonatalOutcomes: String? = null,
    var maternalOutcomes: String? = null,
    var actualDeliveryDate: String? = null,
    var isInitialReview: Boolean? = null,
)
