package org.medtroniclabs.uhis.data.registration

data class NurseLabTest(
    val id: Long?,
    val patientTrackId: Int?,
    val countryId: Int,
    val roleNames: String?,
    val searchTerm: String?,
    val patientVisitId: Int?,
    val tenantId: Int,
    val menuName: String?,
    val reason: String?,
    val otherReason: String?,
    val isSessionDropOut: Boolean?,
    val prescribedSiteId: Int?,
    val name: String,
    val displayOrder: Int,
    val updatedAt: String,
    val labTestResults: List<LabTestResult>,
    val createdBy: Int,
    val updatedBy: Int,
    val createdAt: String,
    val active: Boolean,
    val deleted: Boolean,
    val resultTemplate: Boolean,
)

data class LabTestResult(
    val id: Int,
    val createdBy: Int,
    val updatedBy: Int,
    val createdAt: String,
    val updatedAt: String,
    val tenantId: Int,
    val name: String,
    val labTestId: Int,
    val displayOrder: Int,
    val active: Boolean,
    val deleted: Boolean,
)

data class InvestigationReq(var countryId: Long)

data class InvestigationModel(
    var name: String? = null,
    var result: String? = null,
    var testedon: String? = null,
    var orderDate: String? = null,
)
