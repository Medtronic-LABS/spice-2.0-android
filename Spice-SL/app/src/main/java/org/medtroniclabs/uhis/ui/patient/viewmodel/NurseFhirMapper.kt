package org.medtroniclabs.uhis.ui.patient.viewmodel

import org.medtroniclabs.uhis.data.history.HistoryEntity
import org.medtroniclabs.uhis.data.registration.LabTestHistory
import org.medtroniclabs.uhis.data.registration.LabTestListResponse
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.data.registration.PatientLabTestHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientPrescription
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.VisitDateModel
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
                    testName = item.testName,
                    labTestName = item.testName,
                    referredBy = item.recommendedName,
                    referredByDisplay = item.recommendedName,
                    referredDate = item.recommendedOn,
                    resultDate = item.testedOn,
                    isReviewed = item.isReview ?: false,
                    comment = item.comments,
                ),
            )
        }
        return LabTestListResponse(patientLabTest = list, patientLabtestDates = ArrayList())
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
