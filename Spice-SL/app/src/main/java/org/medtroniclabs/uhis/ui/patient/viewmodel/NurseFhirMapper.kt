package org.medtroniclabs.uhis.ui.patient.viewmodel

import com.google.gson.Gson
import org.medtroniclabs.uhis.data.history.HistoryEntity
import org.medtroniclabs.uhis.data.registration.LabTestHistory
import org.medtroniclabs.uhis.data.registration.LabTestListResponse
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.data.registration.PatientLabTestHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientPrescription
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.VisitDateModel
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.model.LabTestResultObject
import org.medtroniclabs.uhis.data.Prescription as FhirPrescription
import org.medtroniclabs.uhis.model.LabTestListResponse as FhirLabTestListResponse

/**
 * Maps the FHIR backend responses (prescription-request / investigation endpoints) onto the
 * relational nurse-flow UI models. This is the display-first layer of the nurse port: it lets the
 * existing nurse screens render data from the existing endpoints.
 *
 * Note: the FHIR responses identify records with String references, while the nurse models use
 * Long ids. Those id fields are intentionally left null here; remove/edit/date-navigation that
 * round-trip those ids are handled in a follow-up.
 */
object NurseFhirMapper {
    fun mapPrescriptionList(source: List<FhirPrescription>?): ArrayList<PrescriptionModel> {
        val result = ArrayList<PrescriptionModel>()
        source?.forEach { item ->
            result.add(
                PrescriptionModel(
                    medicationName = item.medicationName,
                    prescribedDays = item.prescribedDays?.toInt(),
                    prescribedSince = item.prescribedSince,
                    endDate = item.endDate,
                    classificationName = item.classificationName,
                    brandName = item.brandName,
                    dosageUnitValue = item.dosageUnitValue,
                    dosageUnitName = item.dosageUnitName,
                    dosageFrequencyName = item.dosageFrequencyName ?: item.frequencyName,
                    instructionNote = item.instructionNote ?: item.instruction,
                    dosageFormName = item.dosageFormName,
                    discontinuedOn = item.discontinuedOn ?: item.discontinuedDate,
                    prescriptionRemainingDays = item.prescriptionRemainingDays,
                ),
            )
        }
        return result
    }

    fun mapPrescriptionHistory(entity: HistoryEntity?): PatientPrescriptionHistoryResponse {
        val prescriptions = ArrayList<PatientPrescription>()
        entity?.prescriptions?.forEach { item ->
            prescriptions.add(
                PatientPrescription(
                    id = null,
                    medicationName = item.medicationName,
                    dosageUnitValue = item.dosageUnitValue,
                    dosageUnitName = item.dosageUnitName,
                    dosageFrequencyName = item.dosageFrequencyName ?: item.frequencyName,
                    patientVisitId = null,
                    createdAt = item.prescribedSince ?: entity.dateOfReview ?: "",
                    prescribedSince = item.prescribedSince,
                    dosageFormName = item.dosageFormName,
                    prescribedDays = item.prescribedDays,
                    instructionNote = item.instructionNote,
                    classificationName = item.classificationName,
                    brandName = item.brandName,
                ),
            )
        }
        return PatientPrescriptionHistoryResponse(
            patientPrescription = prescriptions,
            prescriptionHistoryDates = mapVisitDates(entity),
        )
    }

    fun mapLabTestList(
        source: List<FhirLabTestListResponse>?,
        resultUpdated: Boolean,
    ): LabTestListResponse {
        val list = ArrayList<LabTestModel>()
        source?.filter { (it.testedOn != null) == resultUpdated }?.forEach { item ->
            list.add(
                LabTestModel(
                    _id = item.id.toLongOrNull(),
                    fhirId = item.id,
                    labTestId = item.labTestCustomization.id,
                    recommendedById = item.recommendedBy,
                    formInput = item.labTestCustomization.formInput,
                    testName = item.testName,
                    labTestName = item.testName,
                    referredBy = item.recommendedName,
                    referredByDisplay = item.recommendedName,
                    referredDate = item.recommendedOn,
                    resultDate = item.testedOn,
                    isReviewed = item.isReview ?: false,
                    comment = item.comments,
                    resultComments = item.comments,
                    labResultDetails = mapResultDetails(
                        item.labTestResults,
                        item.labTestCustomization.formInput,
                    ),
                ),
            )
        }
        return LabTestListResponse(patientLabTest = list, patientLabtestDates = ArrayList())
    }

    /**
     * Converts the inline FHIR result objects into the map shape the result-detail grid
     * ([org.medtroniclabs.uhis.ui.patient.adapter.ResultsAdapter]) reads. This replaces the
     * removed relational patient-labtest/result/details endpoint.
     */
    private fun mapResultDetails(
        results: List<LabTestResultObject>?,
        formInput: String?,
    ): ArrayList<Map<String, Any>> {
        val list = ArrayList<Map<String, Any>>()
        // The saved result's name is the form field id; resolve its human-readable title (and a
        // unit fallback) from the lab test's inline form definition.
        val fieldsById = parseFormFields(formInput)
        results?.forEach { result ->
            val map = HashMap<String, Any>()
            val field = fieldsById[result.name]
            val displayName = field?.title?.takeIf { it.isNotBlank() } ?: result.name
            map[DefinedParams.RESULT_NAME] = displayName
            result.value?.let { map[DefinedParams.RESULT_VALUE] = it.toString() }
            val unit = result.unit?.takeIf { it.isNotBlank() }
                ?: (field?.unitList?.firstOrNull()?.get(DefinedParams.NAME) as? String)
            unit?.let { map[DefinedParams.UNIT] = it }
            list.add(map)
        }
        return list
    }

    private fun parseFormFields(formInput: String?): Map<String, FormLayout> {
        if (formInput.isNullOrBlank()) return emptyMap()
        return try {
            Gson()
                .fromJson(formInput, FormResponse::class.java)
                ?.formLayout
                ?.associateBy { it.id }
                ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun mapLabTestHistory(entity: HistoryEntity?): PatientLabTestHistoryResponse {
        val list = ArrayList<LabTestHistory>()
        entity?.investigations?.forEach { item ->
            list.add(
                LabTestHistory(
                    labtestName = item.testName,
                    referredDate = entity.dateOfReview ?: "",
                    patientVisitId = 0L,
                ),
            )
        }
        return PatientLabTestHistoryResponse(
            total = list.size,
            patientLabTest = list,
            patientLabtestDates = mapVisitDates(entity),
        )
    }

    private fun mapVisitDates(entity: HistoryEntity?): ArrayList<VisitDateModel> {
        val dates = ArrayList<VisitDateModel>()
        entity?.history?.forEachIndexed { index, referredDate ->
            dates.add(
                VisitDateModel(
                    visitDate = referredDate.date ?: "",
                    _id = index.toLong(),
                ),
            )
        }
        return dates
    }
}
