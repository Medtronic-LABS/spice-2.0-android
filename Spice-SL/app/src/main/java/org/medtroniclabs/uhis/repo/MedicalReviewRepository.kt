package org.medtroniclabs.uhis.repo

import okhttp3.RequestBody
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.PatientPrescriptionModel
import org.medtroniclabs.uhis.data.PrescriptionListRequest
import org.medtroniclabs.uhis.data.medicalreview.ReqBPBGLogList
import org.medtroniclabs.uhis.data.medicalreview.ResLabTestRecommendations
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.registration.AssessmentListRequest
import org.medtroniclabs.uhis.data.registration.BadgeModel
import org.medtroniclabs.uhis.data.registration.ConfirmDiagnosesRequest
import org.medtroniclabs.uhis.data.registration.FillPrescriptionRequest
import org.medtroniclabs.uhis.data.registration.FillPrescriptionUpdateRequest
import org.medtroniclabs.uhis.data.registration.InvestigationReq
import org.medtroniclabs.uhis.data.registration.Lifestyle
import org.medtroniclabs.uhis.data.registration.MedicationSearchReqModel
import org.medtroniclabs.uhis.data.registration.NurseMrRequestModel
import org.medtroniclabs.uhis.data.registration.PatientPregnancyModel
import org.medtroniclabs.uhis.data.registration.PatientRemoveRequest
import org.medtroniclabs.uhis.data.registration.PregnancyCreateRequest
import org.medtroniclabs.uhis.data.registration.PregnancyRiskUpdate
import org.medtroniclabs.uhis.data.registration.PrescriptionPredictionResponse
import org.medtroniclabs.uhis.data.registration.RegionSiteModel
import org.medtroniclabs.uhis.data.registration.RequestPatientDetail
import org.medtroniclabs.uhis.data.registration.SearchModel
import org.medtroniclabs.uhis.data.registration.SessionEncounterRequest
import org.medtroniclabs.uhis.data.registration.SingleWindowModel
import org.medtroniclabs.uhis.data.registration.SiteRoleModel
import org.medtroniclabs.uhis.data.registration.TerminateSessionModel
import org.medtroniclabs.uhis.data.registration.TransferCreateRequest
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.model.LabTestListRequest
import org.medtroniclabs.uhis.model.PatientDetailRequest
import org.medtroniclabs.uhis.model.ReferralDetailRequest
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisRequestResponse
import org.medtroniclabs.uhis.ncd.data.PatientVisitRequest
import org.medtroniclabs.uhis.network.ApiHelper
import retrofit2.Response
import javax.inject.Inject

class MedicalReviewRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper,
) {
    suspend fun getPatientDetails(request: PatientDetailRequest) = apiHelper.getPatientDetails(request)

    suspend fun getPatientBPLogList(request: ReqBPBGLogList) = apiHelper.getPatientBPLogList(request)

    suspend fun riskFactorListing() = roomHelper.getAllRiskFactorEntityList()

    suspend fun getPatientBloodGlucoseList(request: ReqBPBGLogList) = apiHelper.getPatientBloodGlucoseList(request)

    suspend fun getSessionGraph(request: AssessmentListRequest) = apiHelper.getSessionGraph(request)

    suspend fun searchMedication(request: MedicationSearchReqModel) = apiHelper.searchMedication(request)

    suspend fun createPregnancy(request: PregnancyCreateRequest) = apiHelper.createPregnancy(request)

    suspend fun updatePregnancy(request: PregnancyCreateRequest) = apiHelper.updatePregnancy(request)

    suspend fun getPatientPregnancyDetails(request: PatientPregnancyModel) = apiHelper.getPatientPregnancyDetails(request)

    suspend fun confirmDiagnosis(request: ConfirmDiagnosesRequest) = apiHelper.confirmDiagnosis(request)

    suspend fun updateConfirmDiagnosis(request: NCDDiagnosisRequestResponse) = apiHelper.createConfirmDiagonsis(request)

    suspend fun getMentalHealthDetails(request: HashMap<String, Any>) = apiHelper.getMentalHealthDetails(request)

    suspend fun getPatientMedicalReviewSummary(request: MedicalReviewBaseRequest) = apiHelper.getPatientMedicalReviewSummary(request)

    suspend fun treatmentPlanDetails(request: HashMap<String, Any>) = apiHelper.treatmentPlanDetails(request)

    suspend fun updateTreatmentPlan(request: HashMap<String, Any>) = apiHelper.updateTreatmentPlan(request)

    suspend fun getScreeningDetails(request: RequestPatientDetail) = apiHelper.getScreeningDetails(request)

    suspend fun getTreatmentPlanData() = roomHelper.getTreatmentPlanData()

    suspend fun getUpazilaList(): Any = roomHelper.getUpazilaList()

    suspend fun getCountyById(countyId: Long) = 0

    suspend fun getSubCountyById(subCountyId: Long) = 0

    suspend fun createBpLog(request: HashMap<String, Any>) = apiHelper.createBpLog(request)

    suspend fun createGlucoseLog(request: HashMap<String, Any>) = apiHelper.createGlucoseLog(request)

    suspend fun patientRemove(request: PatientRemoveRequest) = apiHelper.patientRemove(request)

    suspend fun getDiagnosisList() = roomHelper.getDiagnosisList()

    suspend fun clearRedRisk(request: HashMap<String, Any>) = apiHelper.clearRedRisk(request)

    suspend fun getInstructions() = apiHelper.getInstructions()

    suspend fun updatePregnancyRisk(request: PregnancyRiskUpdate) = apiHelper.updatePregnancyRisk(request)

    suspend fun getSessionQuestions(
        session: Int,
        age: Int,
    ) = apiHelper.getSessionQuestions(session, age)

    suspend fun getPatientFamilyOtherDetails(data: Lifestyle): Response<APIResponse<HashMap<String, Any>>> = apiHelper.getPatientFamilyOtherDetails(data)

    suspend fun getShortageReasonList(type: String) = roomHelper.getShortageReason(type)

    suspend fun postSessionTerminate(request: TerminateSessionModel): Response<APIResponse<HashMap<String, Any>>> = apiHelper.postSessionTerminate(request)

    suspend fun medicationList(request: MedicationSearchReqModel) = apiHelper.medicationList(request)

    suspend fun getComorbidityBasedOnWorkflow(workflowList: ArrayList<String>) = roomHelper.getComorbidityBasedOnWorkflow(workflowList)

    suspend fun getComplication() = roomHelper.getComplication()

    suspend fun getLifeStyle() = roomHelper.getLifeStyleList()

    suspend fun getDiagnosis(
        gender: ArrayList<String>,
        type: ArrayList<String>,
    ) = roomHelper.getDiagnosis(gender, type)

    suspend fun getCurrentMedicationList() = roomHelper.getCurrentMedicationList()

    suspend fun getCurrentMedicationList(type: String) = roomHelper.getCurrentMedicationList(type)

    suspend fun getPhysicalExaminationList(workFlowList: ArrayList<String>) = roomHelper.getPhysicalExaminationList(workFlowList)

    suspend fun getChiefComplaints(workflowList: ArrayList<String>) = roomHelper.getChiefComplaints(workflowList)

    suspend fun createSingleWindowMR(request: SingleWindowModel) = apiHelper.createSingleWindowMR(request)

    suspend fun getBadgeCount(request: BadgeModel) = apiHelper.getBadgeCount(request)

    suspend fun searchSite(request: RegionSiteModel) = apiHelper.searchSite(request)

    suspend fun searchRoleUser(request: SiteRoleModel) = apiHelper.searchRoleUser(request)

    suspend fun createPatientTransfer(request: TransferCreateRequest) = apiHelper.createPatientTransfer(request)

    suspend fun validatePatientTransfer(request: FillPrescriptionRequest) = apiHelper.validatePatientTransfer(request)

    suspend fun sessionCreate(request: SessionEncounterRequest) = apiHelper.sessionCreate(request)

    suspend fun createNurseMedicalReview(request: NurseMrRequestModel) = apiHelper.createNurseMedicalReview(request)

    suspend fun getOperatingUnitSites(): List<SiteEntity> = roomHelper.getOperatingUnitSites()

    suspend fun getPrescriptionList(request: PrescriptionListRequest) = apiHelper.getPrescriptionList(request)

    suspend fun removePrescription(request: PatientPrescriptionModel) = apiHelper.removePrescription(request)

    suspend fun updatePrescription(body: RequestBody) = apiHelper.updatePrescription(body)

    suspend fun getFrequency() = roomHelper.getFrequencyList()

    suspend fun getDosageFrequencyList() = roomHelper.getDosageFrequencyList()

    suspend fun getPatientPrescriptionHistoryList(request: ReferralDetailRequest) = apiHelper.getPrescription(request)

    suspend fun getUnitList(type: String) = roomHelper.getUnitList(type)

    suspend fun getPrescriptionPrediction(map: HashMap<String, Any>): Response<APIResponse<PrescriptionPredictionResponse>> =
        apiHelper.getPrescriptionPrediction(map)

    suspend fun getPatientLabTestHistory(request: ReferralDetailRequest) = apiHelper.getInvestigation(request)

    suspend fun getPatientMedicalReviewHistoryList(request: MedicalReviewBaseRequest) = apiHelper.getPatientMedicalReviewHistoryList(request)

    suspend fun getPatientSessionHistoryList(request: MedicalReviewBaseRequest) = apiHelper.getPatientSessionHistoryList(request)

    suspend fun getPatientLabTestRecommendation(request: InvestigationReq): Response<APIResponse<List<ResLabTestRecommendations>>> =
        apiHelper.getPatientLabTestRecommendation(request)

    suspend fun getPatientLabTests(request: LabTestListRequest) = apiHelper.getLabTestList(request)

    suspend fun searchLabTest(request: SearchModel) = apiHelper.searchLabTest(request)

    suspend fun getLabTestResult(labTestId: Long) = apiHelper.getLabTestResult(labTestId)

    suspend fun getNudgesList(request: HashMap<String, Any>) = apiHelper.getNudgesList(request)

    suspend fun referLabTest(request: HashMap<String, Any>) = apiHelper.referLabTest(request)

    suspend fun createLabTestResult(request: HashMap<String, Any>) = apiHelper.createLabTestResult(request)

    suspend fun getLabTestResultDetails(request: HashMap<String, Any>) = apiHelper.getLabTestResultDetails(request)

    suspend fun removeLabTest(request: HashMap<String, Any>) = apiHelper.removeLabTest(request)

    suspend fun reviewLabTestResult(request: HashMap<String, Any>) = apiHelper.reviewLabTestResult(request)

    suspend fun updatePatientType(request: HashMap<String, Any>) = apiHelper.updatePatientType(request)

    suspend fun getPatientFillPrescriptionList(request: FillPrescriptionRequest) = apiHelper.getPatientFillPrescriptionList(request)

    suspend fun fillPrescriptionUpdate(request: FillPrescriptionUpdateRequest) = apiHelper.fillPrescriptionUpdate(request)

    suspend fun getPrescriptionRefillHistory(request: org.medtroniclabs.uhis.data.registration.PatientPrescriptionModel) =
        apiHelper.getPrescriptionRefillHistory(request)

    suspend fun createPatientVisit(request: PatientVisitRequest) = apiHelper.createPatientVisit(request)
}
