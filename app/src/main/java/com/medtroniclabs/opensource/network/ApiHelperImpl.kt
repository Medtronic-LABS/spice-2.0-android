package com.medtroniclabs.opensource.network

import com.google.gson.JsonObject
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.AboveFiveYearsMetaResponse
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryDetails
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryRequest
import com.medtroniclabs.opensource.data.BirthHistoryRequest
import com.medtroniclabs.opensource.data.BirthHistoryResponse
import com.medtroniclabs.opensource.data.DiagnosisDiseaseModel
import com.medtroniclabs.opensource.data.DiagnosisSaveUpdateRequest
import com.medtroniclabs.opensource.data.DispensePrescriptionRequest
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.data.DispenseUpdateRequest
import com.medtroniclabs.opensource.data.DispenseUpdateResponse
import com.medtroniclabs.opensource.data.FormMetaRequest
import com.medtroniclabs.opensource.data.FormRequest
import com.medtroniclabs.opensource.data.FormResponse
import com.medtroniclabs.opensource.data.LabourDeliveryMetaResponse
import com.medtroniclabs.opensource.data.LoginResponse
import com.medtroniclabs.opensource.data.MedicalReviewSummarySubmitRequest
import com.medtroniclabs.opensource.data.MedicationResponse
import com.medtroniclabs.opensource.data.MedicationSearchRequest
import com.medtroniclabs.opensource.data.MetaDataResponse
import com.medtroniclabs.opensource.data.MotherNeonateAncMetaResponse
import com.medtroniclabs.opensource.data.MotherNeonateAncSummaryModel
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryRequest
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryResponse
import com.medtroniclabs.opensource.data.MotherPncResponse
import com.medtroniclabs.opensource.data.NCDUserDashboardRequest
import com.medtroniclabs.opensource.data.NCDUserDashboardResponse
import com.medtroniclabs.opensource.data.NeonatePncResponse
import com.medtroniclabs.opensource.data.PatientStatusRequest
import com.medtroniclabs.opensource.data.PatientStatusResponse
import com.medtroniclabs.opensource.data.PncChildMedicalReview
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.PrescriptionListRequest
import com.medtroniclabs.opensource.data.ReferPatientAPIRequest
import com.medtroniclabs.opensource.data.ReferPatientHealthFacilityItem
import com.medtroniclabs.opensource.data.ReferPatientNameNumber
import com.medtroniclabs.opensource.data.ReferPatientRequest
import com.medtroniclabs.opensource.data.ReferPatientResult
import com.medtroniclabs.opensource.data.RemovePrescriptionRequest
import com.medtroniclabs.opensource.data.SummaryCreateRequest
import com.medtroniclabs.opensource.data.UnderFiveYearsMetaResponse
import com.medtroniclabs.opensource.data.UnderTwoMonthsMetaResponse
import com.medtroniclabs.opensource.data.UserSymptomsEntity
import com.medtroniclabs.opensource.data.history.HistoryEntity
import com.medtroniclabs.opensource.data.history.MedicalReviewHistory
import com.medtroniclabs.opensource.data.history.NCDMedicalReviewHistory
import com.medtroniclabs.opensource.data.model.AboveFiveYearsSubmitRequest
import com.medtroniclabs.opensource.data.model.BpAndWeightRequestModel
import com.medtroniclabs.opensource.data.model.BpAndWeightResponse
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryRequest
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryResponse
import com.medtroniclabs.opensource.data.model.LabourDeliverySummaryDetails
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.data.model.MotherNeonatePncRequest
import com.medtroniclabs.opensource.data.model.PatientEncounterResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountRequest
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountResponse
import com.medtroniclabs.opensource.data.model.PncSubmitResponse
import com.medtroniclabs.opensource.data.model.RegistrationResponse
import com.medtroniclabs.opensource.data.model.RequestChangePassword
import com.medtroniclabs.opensource.data.model.ResponseChangePassword
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHold
import com.medtroniclabs.opensource.data.offlinesync.model.RequestGetSyncStatus
import com.medtroniclabs.opensource.data.offlinesync.model.ResponseSignatureUpload
import com.medtroniclabs.opensource.data.offlinesync.model.SyncResponse
import com.medtroniclabs.opensource.data.performance.CHWPerformanceMonitoring
import com.medtroniclabs.opensource.data.performance.ChwVillageFilterModel
import com.medtroniclabs.opensource.data.performance.FilterPreference
import com.medtroniclabs.opensource.data.performance.PerformanceReportRequest
import com.medtroniclabs.opensource.data.resource.LabourDeliverySummaryRequest
import com.medtroniclabs.opensource.data.resource.RequestAllEntities
import com.medtroniclabs.opensource.model.CultureLocaleModel
import com.medtroniclabs.opensource.model.LabTestCreateRequest
import com.medtroniclabs.opensource.model.LabTestListRequest
import com.medtroniclabs.opensource.model.LabTestListResponse
import com.medtroniclabs.opensource.model.NcdMRStaticDataModel
import com.medtroniclabs.opensource.model.PatientDetailRequest
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.PatientsDataModel
import com.medtroniclabs.opensource.model.ReferralData
import com.medtroniclabs.opensource.model.ReferralDetailRequest
import com.medtroniclabs.opensource.model.RemoveLabTestRequest
import com.medtroniclabs.opensource.model.SearchAndListResponse
import com.medtroniclabs.opensource.model.medicalreview.AddMemberRegRequest
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderFiveYearsRequest
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsRequest
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.model.medicalreview.SearchLabTestResponse
import com.medtroniclabs.opensource.model.medicalreview.SearchRequestLabTest
import com.medtroniclabs.opensource.model.medicalreview.SummaryDetails
import com.medtroniclabs.opensource.ncd.data.AssessmentResultModel
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.ncd.data.BadgeNotificationModel
import com.medtroniclabs.opensource.ncd.data.MRSummaryResponse
import com.medtroniclabs.opensource.ncd.data.MedicalReviewRequestResponse
import com.medtroniclabs.opensource.ncd.data.MedicalReviewResponse
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetRequest
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetResponse
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisRequestResponse
import com.medtroniclabs.opensource.ncd.data.NCDInstructionModel
import com.medtroniclabs.opensource.ncd.data.NCDMRSummaryRequestResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientStatusRequest
import com.medtroniclabs.opensource.ncd.data.NCDPregnancyRiskUpdate
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModel
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModelDetails
import com.medtroniclabs.opensource.ncd.data.LifeStyleResponse
import com.medtroniclabs.opensource.ncd.data.LifeStyleRequest
import com.medtroniclabs.opensource.ncd.data.NCDMedicalReviewUpdateModel
import com.medtroniclabs.opensource.ncd.data.PatientVisitRequest
import com.medtroniclabs.opensource.ncd.data.PatientVisitResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import com.medtroniclabs.opensource.ncd.data.TermsAndConditionsModel
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferValidate
import com.medtroniclabs.opensource.ncd.data.NCDTransferCreateRequest
import com.medtroniclabs.opensource.ncd.data.PatientTransferListResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferUpdateRequest
import com.medtroniclabs.opensource.ncd.data.NCDRegionSiteModel
import com.medtroniclabs.opensource.ncd.data.RegionSiteResponse
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleModel
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleResponse
import com.medtroniclabs.opensource.ncd.data.PredictionRequest
import com.medtroniclabs.opensource.ncd.data.PrescriptionNudgeResponse
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreResponse
import com.medtroniclabs.opensource.ncd.data.DeviceDetails
import com.medtroniclabs.opensource.ncd.data.FollowUpRequest
import com.medtroniclabs.opensource.ncd.data.FollowUpUpdateRequest
import com.medtroniclabs.opensource.ncd.data.NCDMentalHealthMedicalReviewDetails
import com.medtroniclabs.opensource.ncd.data.NCDMentalHealthStatusRequest
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity
import com.medtroniclabs.opensource.ncd.data.RegisterCallResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(private val apiService: ApiService) : ApiHelper {
    override suspend fun doLogin(loginRequest: MultipartBody): Response<LoginResponse> {
        return apiService.doLogin(loginRequest)
    }

    override suspend fun getMetaDataInformation(): Response<APIResponse<MetaDataResponse>> {
        return apiService.getMetaDataInformation()
    }

    override suspend fun getForms(formRequest: FormRequest): Response<APIResponse<FormResponse>> {
        return apiService.getForms(formRequest)
    }

    override suspend fun getFormMetadata(request: FormMetaRequest): Response<APIResponse<UserSymptomsEntity>> {
        return apiService.getFormMetadata(request)
    }

    override suspend fun getPatients(request: PatientsDataModel): APIResponse<SearchAndListResponse> {
        return apiService.getPatients(request)
    }

    override suspend fun postOfflineSync(request: Map<String, Any>): Response<SyncResponse> {
        return apiService.postOfflineSyncData(request)
    }

    override suspend fun getOfflineSyncStatus(request: RequestGetSyncStatus): Response<SyncResponse> {
        return apiService.getOfflineSyncStatus(request)
    }

    override suspend fun fetchSyncedData(request: RequestAllEntities): Response<ResponseBody> {
        return apiService.fetchSyncedData(request)
    }

    override suspend fun getHouseholdAndMembers(request: RequestAllEntities): Response<APIResponse<List<HouseHold>>> {
        return apiService.getHouseholdDetails(request)
    }

    override suspend fun patientSearch(request: PatientsDataModel): APIResponse<SearchAndListResponse> {
        return apiService.patientSearch(request)
    }

    override suspend fun getPatient(request: PatientDetailRequest): Response<APIResponse<PatientListRespModel>> {
        return apiService.getPatient(request)
    }

    override suspend fun getAboveFiveYearsMetaData(): Response<APIResponse<AboveFiveYearsMetaResponse>> {
        return apiService.getAboveFiveYearsMetaData()
    }

    override suspend fun getReferralsDetails(request: ReferralDetailRequest): Response<APIResponse<ReferralData>> {
        return apiService.getReferralsDetails(request)
    }

    override suspend fun createAboveFiveYearsResult(request: AboveFiveYearsSubmitRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>> {
        return apiService.createAboveFiveYearsResult(request)
    }

    override suspend fun getAboveFiveYearsSummaryDetails(patientId: AboveFiveYearsSummaryRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>> {
        return apiService.getAboveFiveYearsSummaryDetails(patientId)
    }

    override suspend fun getMotherNeoNateAncStaticData(): Response<APIResponse<MotherNeonateAncMetaResponse>> {
        return apiService.getMotherNeoNateAncStaticData()
    }

    override suspend fun getMotherPncStaticData(): Response<APIResponse<MotherPncResponse>> {
        return apiService.getMotherPncStaticData()
    }
    override suspend fun getNeonatePncStaticData(): Response<APIResponse<NeonatePncResponse>> {
        return apiService.getNeonatePncStaticData()
    }

    override suspend fun getUnderTwoMonthsMetaData(): Response<APIResponse<UnderTwoMonthsMetaResponse>> {
        return apiService.getUnderTwoMonthsMetaData()
    }

    override suspend fun createSummarySubmit(request: MedicalReviewSummarySubmitRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createSummarySubmit(request)
    }

    override suspend fun getLabourDeliveryMetaData(): Response<APIResponse<LabourDeliveryMetaResponse>> {
        return apiService.getLabourDeliveryMetaData()
    }
    override suspend fun getLabourDeliverySummaryDetails(request: LabourDeliverySummaryDetails): Response<APIResponse<CreateLabourDeliveryRequest>> {
        return apiService.getLabourDeliverySummaryDetails(request)
    }

    override suspend fun getPatientStatus(request: PatientStatusRequest): Response<APIResponse<PatientStatusResponse>> {
        return apiService.getPatientStatus(request)
    }

    override suspend fun searchMedicationByName(request: MedicationSearchRequest): Response<APIResponse<ArrayList<MedicationResponse>>> {
        return apiService.searchMedicationByName(request)
    }

    override suspend fun createMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>> {
        return apiService.createMedicalReviewForUnderTwoMonths(request)
    }

    override suspend fun getMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>> {
        return apiService.getUnderTwoMonthsSummaryDetails(request)
    }


    override suspend fun saveMotherNeonateAnc(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<PatientEncounterResponse>> {
        return apiService.saveMotherNeonateAnc(motherNeonateAncRequest)
    }
    override suspend fun saveMotherNeonatePnc(motherNeonatePncRequest: MotherNeonatePncRequest): Response<APIResponse<PncSubmitResponse>> {
        return apiService.saveMotherNeonatePnc(motherNeonatePncRequest)
    }


    override suspend fun fetchSummary(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<MotherNeonateAncSummaryModel>> {
        return apiService.fetchSummary(motherNeonateAncRequest)
    }

    override suspend fun fetchWeight(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>> {
        return apiService.fetchWeight(motherNeonateAncRequest)
    }

    override suspend fun fetchBloodPressure(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>> {
        return apiService.fetchBloodPressure(motherNeonateAncRequest)
    }

    override suspend fun createWeight(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createWeight(bpAndWeightRequestModel)
    }

    override suspend fun createBloodPressure(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createBloodPressure(bpAndWeightRequestModel)
    }

    override suspend fun saveUpdateDiagnosis(request: DiagnosisSaveUpdateRequest): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>> {
        return apiService.saveUpdateDiagnosis(request)
    }

    override suspend fun getDiagnosisDetails(request: CreateUnderTwoMonthsResponse): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>> {
        return apiService.getDiagnosisDetails(request)
    }

    override suspend fun getHealthFacilityMetaData(request: ReferPatientAPIRequest): Response<APIResponse<List<ReferPatientHealthFacilityItem>>> {
        return apiService.getHealthFacilityMetaData(request)
    }

    override suspend fun getReferPatientMobileUserList(tenantId: ReferPatientRequest): Response<APIResponse<List<ReferPatientNameNumber>>> {
        return apiService.getReferPatientMobileUserList(tenantId)
    }

    override suspend fun createReferPatientResult(request: ReferPatientResult): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createReferPatientResult(request)
    }

    override suspend fun createPrescriptionRequest(request: RequestBody): Response<APIResponse<Map<String, Any>>> {
        return apiService.createPrescriptionRequest(request)
    }

    override suspend fun getPrescriptionList(request: PrescriptionListRequest): Response<APIResponse<ArrayList<Prescription>>> {
        return apiService.getPrescriptionList(request)
    }

    override suspend fun removePrescription(request: RemovePrescriptionRequest): Response<APIResponse<Map<String, Any>>> {
        return apiService.removePrescription(request)
    }

    override suspend fun getUnderFiveYearsMetaData(): Response<APIResponse<UnderFiveYearsMetaResponse>> {
        return apiService.getUnderFiveYearsMetaData()
    }

    override suspend fun createMedicalReviewForUnderFiveYears(request: CreateUnderFiveYearsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>> {
        return apiService.createMedicalReviewForUnderFiveYears(request)
    }

    override suspend fun getUnderFiveYearsSummaryDetails(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>> {
        return apiService.getUnderFiveYearsSummaryDetails(request)
    }


    override suspend fun getPrescription(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>> {
        return apiService.getPrescription(request)
    }

    override suspend fun getMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<MedicalReviewHistory>> {
        return apiService.getMedicalReviewHistory(request)
    }

    override suspend fun getMedicalReviewHistoryPNC(request: ReferralDetailRequest): Response<APIResponse<PncChildMedicalReview>> {
        return apiService.getMedicalReviewHistoryPNC(request)
    }

    override suspend fun getPncSummaryDetails(request: MotherNeonatePncSummaryRequest): Response<APIResponse<MotherNeonatePncSummaryResponse>> {
        return apiService.getPncSummaryDetails(request)
    }

    override suspend fun summaryCreatePncData(summaryCreateRequest: SummaryCreateRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.summaryCreatePncData(summaryCreateRequest)
    }
    override suspend fun getBirthHistoryDetails(request: BirthHistoryRequest): Response<APIResponse<BirthHistoryResponse>> {
        return apiService.getBirthHistoryDetails(request)
    }

    override suspend fun createMedicalReviewLabourDelivery(request: CreateLabourDeliveryRequest): Response<APIResponse<CreateLabourDeliveryResponse>> {
        return apiService.createMedicalReviewForLaborDelivery(request)
    }

    override suspend fun addNewMember(request: AddMemberRegRequest): Response<APIResponse<String>> {
        return apiService.addNewMember(request)
    }

    override suspend fun searchLabTestByName(request: SearchRequestLabTest): Response<APIResponse<ArrayList<SearchLabTestResponse>>> {
        return apiService.searchLabTestByName(request)
    }

    override suspend fun createLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>> {
        return apiService.createLabTest(request)
    }

    override suspend fun updateLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>> {
        return apiService.updateLabTest(request)
    }

    override suspend fun getLabTestList(request: LabTestListRequest): Response<APIResponse<ArrayList<LabTestListResponse>>> {
        return apiService.getLabTestList(request)
    }

    override suspend fun removeLabTest(request: RemoveLabTestRequest): Response<APIResponse<Map<String, Any>>> {
        return apiService.removeLabTest(request)
    }
    override suspend fun createSummaryMotherNeonate(request: LabourDeliverySummaryRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.summaryCreateMotherNeonate(request)
    }

    override suspend fun getPeerSupervisorLinkedChwList(): Response<APIResponse<List<ChwVillageFilterModel>>> {
        return apiService.getPeerSupervisorLinkedChwList()
    }

    override suspend fun getPeerSupervisorReport(request: PerformanceReportRequest): Response<APIResponse<List<CHWPerformanceMonitoring>>> {
        return apiService.getPeerSupervisorReport(request)
    }

    override suspend fun getUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>> {
        return apiService.getUserFilterPreference(request)
    }

    override suspend fun saveUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>> {
        return apiService.saveUserFilterPreference(request)
    }

    override suspend fun getInvestigation(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>> {
        return apiService.getInvestigationHistoryList(request)
    }

    override suspend fun forgotPassword(email: String, clientConstant: String): Response<APIResponse<String?>> {
        return apiService.forgotPassword(email, clientConstant)
    }

    override suspend fun verifyToken(token: String): Response<APIResponse<String?>> {
        return apiService.verifyToken(token)
    }

    override suspend fun resetPassword(token: String, request: RequestChangePassword): Response<APIResponse<ResponseChangePassword>> {
        return apiService.resetPassword(token, request)
    }

    override suspend fun uploadAllConsentSignatures(request: RequestBody): Response<APIResponse<List<ResponseSignatureUpload>>> {
        return apiService.uploadAllConsentSignatures(request)
    }

    override suspend fun checkAppVersion(): Response<APIResponse<Boolean>> {
        return apiService.checkAppVersion()
    }

    override suspend fun registerPatient(hashMap: RequestBody): Response<APIResponse<RegistrationResponse>> {
        return apiService.registerPatient(hashMap)
    }

    override suspend fun createScreening(createRequest: RequestBody): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createScreening(createRequest)
    }

    override suspend fun getNcdMRStaticData(): Response<APIResponse<NcdMRStaticDataModel>> {
        return apiService.getNcdMRStaticData()
    }

    override suspend fun bpLogCreate(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.bpLogCreate(request)
    }

    override suspend fun glucoseLogCreate(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.glucoseLogCreate(request)
    }

    override suspend fun bpLogList(request: BPBGListModel): Response<APIResponse<BPBGListModel>> {
        return apiService.bpLogList(request)
    }

    override suspend fun glucoseLogList(request: BPBGListModel): Response<APIResponse<BPBGListModel>> {
        return apiService.glucoseLogList(request)
    }
    override suspend fun createAssessmentNCD(request: JsonObject): Response<HashMap<String, Any>> {
        return apiService.createAssessmentNCD(request)
    }

    override suspend fun ncdPregnancyCreate(request: PregnancyDetailsModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdPregnancyCreate(request)
    }

    override suspend fun ncdPregnancyDetails(request: HashMap<String, Any>): Response<APIResponse<PregnancyDetailsModel>> {
        return apiService.ncdPregnancyDetails(request)
    }

    override suspend fun createPatientVisit(request: PatientVisitRequest): Response<APIResponse<PatientVisitResponse>> {
        return apiService.createPatientVisit(request)
    }

    override suspend fun createNCDMedicalReview(request: MedicalReviewRequestResponse): Response<APIResponse<MedicalReviewResponse>> {
        return apiService.createNCDMedicalReview(request)
    }

    override suspend fun fetchNCDMRSummary(request: MedicalReviewResponse): Response<APIResponse<MRSummaryResponse>> {
        return apiService.fetchNCDMRSummary(request)
    }

    override suspend fun getPatientPrescriptionHistoryList(request: RemovePrescriptionRequest): Response<APIResponse<ArrayList<Prescription>>> {
        return apiService.getPatientPrescriptionHistoryList(request)
    }

    override suspend fun createConfirmDiagonsis(request: NCDDiagnosisRequestResponse): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createConfirmDiagonsis(request)
    }

    override suspend fun getConfirmDiagonsis(request: NCDDiagnosisGetRequest): Response<APIResponse<NCDDiagnosisGetResponse>> {
        return apiService.getConfirmDiagonsis(request)
    }

    override suspend fun createNCDPatientStatus(request: NCDPatientStatusRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createNCDPatientStatus(request)
    }

    override suspend fun updateNCDTreatmentPlan(request: NCDTreatmentPlanModel): Response<APIResponse<NCDTreatmentPlanModel>> {
        return apiService.updateNCDTreatmentPlan(request)
    }

    override suspend fun getNCDTreatmentPlan(request: NCDTreatmentPlanModelDetails): Response<APIResponse<NCDTreatmentPlanModelDetails>> {
        return apiService.getNCDTreatmentPlan(request)
    }

    override suspend fun createNCDMRSummaryCreate(request: NCDMRSummaryRequestResponse): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createNCDMRSummaryCreate(request)
    }

    override suspend fun getPrescriptionDispenseList(request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>> {
        return apiService.getPrescriptionDispenseList(request)
    }

    override suspend fun updateDispensePrescription(request: DispensePrescriptionRequest): Response<APIResponse<DispenseUpdateResponse>> {
        return apiService.updateDispensePrescription(request)
    }

    override suspend fun getDispensePrescriptionHistory(request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>> {
        return apiService.getDispensePrescriptionHistory(request)
    }

    override suspend fun createLifestyle(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> {
        return apiService.createLifestyle(request)
    }

    override suspend fun updateLifestyle(request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.updateLifestyle(request)
    }

    override suspend fun getLifestyleList(request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>> {
        return apiService.getLifestyleList(request)
    }

    override suspend fun removeLifestyle(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> {
        return apiService.removeLifestyle(request)
    }

    override suspend fun createPsychological(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> {
        return apiService.createPsychological(request)
    }

    override suspend fun updatePsychological(request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.updatePsychological(request)
    }

    override suspend fun getPsychological(request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>> {
        return apiService.getPsychological(request)
    }

    override suspend fun removePsychological(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> {
        return apiService.removePsychological(request)
    }

    override suspend fun getNCDMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<NCDMedicalReviewHistory>> {
        return apiService.getNCDMedicalReviewHistory(request)
    }

    override suspend fun validatePatient(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.validatePatient(request)
    }

    override suspend fun ncdGetInstructions(): Response<APIResponse<NCDInstructionModel>> {
        return apiService.ncdGetInstructions()
    }

    override suspend fun ncdUpdatePregnancyRisk(request: NCDPregnancyRiskUpdate): Response<APIResponse<Boolean>> {
        return apiService.ncdUpdatePregnancyRisk(request)
    }

    override suspend fun getWazWhzScore(request: WazWhzScoreRequest): Response<APIResponse<WazWhzScoreResponse>> {
        return apiService.getWazWhzScore(request)
    }

    override suspend fun getUserDashboardDetails(request: NCDUserDashboardRequest): Response<APIResponse<NCDUserDashboardResponse>> {
        return apiService.getUserDashboardDetails(request)
    }

    override suspend fun getBadgeNotifications(request: BadgeNotificationModel): Response<APIResponse<BadgeNotificationModel>> {
        return apiService.getBadgeNotifications(request)
    }

    override suspend fun updateBadgeNotifications(request: BadgeNotificationModel): Response<APIResponse<Boolean>> {
        return apiService.updateBadgeNotifications(request)
    }

    override suspend fun getNcdLifeStyleDetails(request: LifeStyleRequest): Response<APIResponse<ArrayList<LifeStyleResponse>>> {
        return apiService.getNcdLifeStyleDetails(request)
    }

    override suspend fun ncdPatientRemove(request: NCDPatientRemoveRequest): Response<APIResponse<Boolean>> {
        return apiService.ncdPatientRemove(request)
    }

    override suspend fun bpLogCreateForNurse(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.bpLogCreateForNurse(request)
    }

    override suspend fun glucoseLogCreateForNurse(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.glucoseLogCreateForNurse(request)
    }

    override suspend fun ncdUpdatePatientDetail(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return  apiService.ncdUpdatePatientDetail(request)
    }

    override suspend fun getUserTermsAndConditions(request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>> {
        return apiService.getUserTermsAndConditions(request)
    }

    override suspend fun updateTermsAndConditionsStatus(request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>> {
        return apiService.updateTermsAndConditionsStatus(request)
    }

    override suspend fun ncdUpdateNextVisitDate(request: NCDMedicalReviewUpdateModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdUpdateNextVisitDate(request)
    }

    override suspend fun validatePatientTransfer(request: NCDPatientTransferValidate): Response<HashMap<String, Any>> {
        return apiService.validatePatientTransfer(request)
    }

    override suspend fun createPatientTransfer(request: NCDTransferCreateRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createPatientTransfer(request)
    }

    override suspend fun searchSite(request: NCDRegionSiteModel): Response<APIResponse<ArrayList<RegionSiteResponse>>> {
        return apiService.searchSite(request)
    }

    override suspend fun searchRoleUser(request: NCDSiteRoleModel): Response<APIResponse<ArrayList<NCDSiteRoleResponse>>> {
        return apiService.searchRoleUser(request)
    }

    override suspend fun getPatientListTransfer(request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<PatientTransferListResponse>> {
        return apiService.getPatientListTransfer(request)
    }

    override suspend fun patientTransferNotificationCount(request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<NCDPatientTransferNotificationCountResponse>> {
        return apiService.patientTransferNotificationCount(request)
    }

    override suspend fun patientTransferUpdate(request: NCDPatientTransferUpdateRequest): Response<APIResponse<String>> {
        return apiService.patientTransferUpdate(request)
    }

    override suspend fun getNudgesList(prescriptionNudgeRequest: PredictionRequest): Response<APIResponse<PrescriptionNudgeResponse>> {
        return apiService.getNudgesList(prescriptionNudgeRequest)
    }

    override suspend fun getLabTestNudgeList(predictionRequest: PredictionRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.getLabTestNudgeList(predictionRequest)
    }

    override suspend fun ncdFollowUpList(request: FollowUpRequest): APIResponse<List<PatientFollowUpEntity>> {
        return apiService.ncdFollowUpList(request)
    }

    override suspend fun getPatientCallRegister(): Response<APIResponse<RegisterCallResponse>> {
        return apiService.getPatientCallRegister()
    }

    override suspend fun updatePatientCallRegister(request: FollowUpUpdateRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.updatePatientCallRegister(request)
    }

    override suspend fun updateDeviceDetails(request: DeviceDetails): Response<APIResponse<DeviceDetails>> {
        return apiService.updateDeviceDetails(request)
    }

    override suspend fun createMentalHealthStatus(request: NCDMentalHealthStatusRequest): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.createMentalHealthStatus(request)
    }

    override suspend fun ncdMentalHealthMedicalReviewCreateA(request: JsonObject): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdMentalHealthMedicalReviewCreateA(request)
    }

    override suspend fun ncdPatientDiagnosisStatus(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdPatientDiagnosisStatus(request)
    }

    override suspend fun ncdMentalHealthMedicalReviewCreateS(request: JsonObject): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdMentalHealthMedicalReviewCreateS(request)
    }

    override suspend fun ncdMentalHealthMedicalReviewDetailsA(request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdMentalHealthMedicalReviewDetailsA(request)
    }

    override suspend fun ncdMentalHealthMedicalReviewDetailsS(request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.ncdMentalHealthMedicalReviewDetailsS(request)
    }

    override suspend fun markAsReviewed(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.markAsReviewed(request)
    }

    override suspend fun cultureLocaleUpdate(request: CultureLocaleModel): Response<APIResponse<HashMap<String, Any>>> {
        return apiService.cultureLocaleUpdate(request)
    }
}