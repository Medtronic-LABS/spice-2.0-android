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

interface ApiHelper {
    suspend fun doLogin(loginRequest: MultipartBody): Response<LoginResponse>
    suspend fun getMetaDataInformation(): Response<APIResponse<MetaDataResponse>>
    suspend fun getForms(formRequest: FormRequest): Response<APIResponse<FormResponse>>
    suspend fun getFormMetadata(request: FormMetaRequest): Response<APIResponse<UserSymptomsEntity>>
    suspend fun postOfflineSync(request: Map<String, Any>): Response<SyncResponse>
    suspend fun getOfflineSyncStatus(request: RequestGetSyncStatus): Response<SyncResponse>
    suspend fun getHouseholdAndMembers(request: RequestAllEntities): Response<APIResponse<List<HouseHold>>>

    suspend fun fetchSyncedData(request: RequestAllEntities): Response<ResponseBody>

    suspend fun getPatients(request: PatientsDataModel): APIResponse<SearchAndListResponse>
    suspend fun patientSearch(request: PatientsDataModel): APIResponse<SearchAndListResponse>
    suspend fun getPatient(request: PatientDetailRequest): Response<APIResponse<PatientListRespModel>>
    suspend fun getAboveFiveYearsMetaData(): Response<APIResponse<AboveFiveYearsMetaResponse>>
    suspend fun getReferralsDetails(request: ReferralDetailRequest): Response<APIResponse<ReferralData>>
    suspend fun createAboveFiveYearsResult(request: AboveFiveYearsSubmitRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>>
    suspend fun getAboveFiveYearsSummaryDetails(patientId: AboveFiveYearsSummaryRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>>
    suspend fun getMotherNeoNateAncStaticData(): Response<APIResponse<MotherNeonateAncMetaResponse>>
    suspend fun getMotherPncStaticData(): Response<APIResponse<MotherPncResponse>>
    suspend fun getNeonatePncStaticData(): Response<APIResponse<NeonatePncResponse>>

    suspend fun getUnderTwoMonthsMetaData(): Response<APIResponse<UnderTwoMonthsMetaResponse>>
    suspend fun createSummarySubmit(request: MedicalReviewSummarySubmitRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getLabourDeliveryMetaData(): Response<APIResponse<LabourDeliveryMetaResponse>>
    suspend fun getLabourDeliverySummaryDetails(request: LabourDeliverySummaryDetails): Response<APIResponse<CreateLabourDeliveryRequest>>
    suspend fun getPatientStatus(request: PatientStatusRequest): Response<APIResponse<PatientStatusResponse>>
    suspend fun searchMedicationByName(request: MedicationSearchRequest): Response<APIResponse<ArrayList<MedicationResponse>>>
    suspend fun createMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>>
    suspend fun getMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>>
    suspend fun saveMotherNeonateAnc(motherNeonateAncRequest: MotherNeonateAncRequest):Response<APIResponse<PatientEncounterResponse>>
    suspend fun saveMotherNeonatePnc(motherNeonatePncRequest: MotherNeonatePncRequest):Response<APIResponse<PncSubmitResponse>>
    suspend fun fetchSummary(motherNeonateAncRequest : MotherNeonateAncRequest) : Response<APIResponse<MotherNeonateAncSummaryModel>>
    suspend fun fetchWeight(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>>
    suspend fun fetchBloodPressure(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>>
    suspend fun createWeight(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>>
    suspend fun createBloodPressure(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>>
    suspend fun saveUpdateDiagnosis(request: DiagnosisSaveUpdateRequest): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>>
    suspend fun getDiagnosisDetails(request: CreateUnderTwoMonthsResponse): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>>
    suspend fun getHealthFacilityMetaData(request: ReferPatientAPIRequest): Response<APIResponse<List<ReferPatientHealthFacilityItem>>>
    suspend fun getReferPatientMobileUserList(tenantId: ReferPatientRequest): Response<APIResponse<List<ReferPatientNameNumber>>>
    suspend fun createReferPatientResult(request: ReferPatientResult): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getUnderFiveYearsMetaData(): Response<APIResponse<UnderFiveYearsMetaResponse>>
    suspend fun createMedicalReviewForUnderFiveYears(request: CreateUnderFiveYearsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>>
    suspend fun getUnderFiveYearsSummaryDetails(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>>
    suspend fun createPrescriptionRequest(request: RequestBody): Response<APIResponse<Map<String, Any>>>
    suspend fun getPrescriptionList(request: PrescriptionListRequest): Response<APIResponse<ArrayList<Prescription>>>
    suspend fun removePrescription(request: RemovePrescriptionRequest): Response<APIResponse<Map<String, Any>>>
    suspend fun getPrescription(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>>
    suspend fun getMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<MedicalReviewHistory>>
    suspend fun getPncSummaryDetails(request: MotherNeonatePncSummaryRequest): Response<APIResponse<MotherNeonatePncSummaryResponse>>
    suspend fun summaryCreatePncData(summaryCreateRequest: SummaryCreateRequest):Response<APIResponse<HashMap<String, Any>>>
    suspend fun getBirthHistoryDetails(request: BirthHistoryRequest): Response<APIResponse<BirthHistoryResponse>>
    suspend fun createMedicalReviewLabourDelivery(request: CreateLabourDeliveryRequest): Response<APIResponse<CreateLabourDeliveryResponse>>
    suspend fun searchLabTestByName(request: SearchRequestLabTest): Response<APIResponse<ArrayList<SearchLabTestResponse>>>
    suspend fun createLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>>
    suspend fun updateLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>>
    suspend fun getLabTestList(request: LabTestListRequest): Response<APIResponse<ArrayList<LabTestListResponse>>>
    suspend fun removeLabTest(request: RemoveLabTestRequest): Response<APIResponse<Map<String, Any>>>
    suspend fun  addNewMember(request: AddMemberRegRequest) : Response<APIResponse<String>>
    suspend fun createSummaryMotherNeonate(request: LabourDeliverySummaryRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getInvestigation(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>>
    suspend fun getMedicalReviewHistoryPNC(request: ReferralDetailRequest): Response<APIResponse<PncChildMedicalReview>>
    suspend fun getPeerSupervisorLinkedChwList(): Response<APIResponse<List<ChwVillageFilterModel>>>

    suspend fun getPeerSupervisorReport(request: PerformanceReportRequest): Response<APIResponse<List<CHWPerformanceMonitoring>>>

    suspend fun getUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>>

    suspend fun saveUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>>

    suspend fun forgotPassword(email: String, clientConstant: String): Response<APIResponse<String?>>

    suspend fun verifyToken(token: String): Response<APIResponse<String?>>

    suspend fun resetPassword(token: String, request: RequestChangePassword): Response<APIResponse<ResponseChangePassword>>

    suspend fun uploadAllConsentSignatures(request: RequestBody): Response<APIResponse<List<ResponseSignatureUpload>>>

    suspend fun  checkAppVersion() : Response<APIResponse<Boolean>>
    suspend fun registerPatient(hashMap: RequestBody) : Response<APIResponse<RegistrationResponse>>
    suspend fun createScreening(createRequest: RequestBody): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getNcdMRStaticData(): Response<APIResponse<NcdMRStaticDataModel>>
    suspend fun bpLogCreate(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun glucoseLogCreate(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun bpLogList(request: BPBGListModel): Response<APIResponse<BPBGListModel>>
    suspend fun glucoseLogList(request: BPBGListModel): Response<APIResponse<BPBGListModel>>
    suspend fun createAssessmentNCD(request: JsonObject): Response<HashMap<String, Any>>
    suspend fun ncdPregnancyCreate(request: PregnancyDetailsModel): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdPregnancyDetails(request: HashMap<String, Any>): Response<APIResponse<PregnancyDetailsModel>>
    suspend fun createPatientVisit(request: PatientVisitRequest): Response<APIResponse<PatientVisitResponse>>
    suspend fun createNCDMedicalReview(request: MedicalReviewRequestResponse): Response<APIResponse<MedicalReviewResponse>>
    suspend fun fetchNCDMRSummary(request: MedicalReviewResponse): Response<APIResponse<MRSummaryResponse>>
    suspend fun createConfirmDiagonsis(request: NCDDiagnosisRequestResponse): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getConfirmDiagonsis(request: NCDDiagnosisGetRequest): Response<APIResponse<NCDDiagnosisGetResponse>>

    suspend fun createNCDPatientStatus(request: NCDPatientStatusRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun updateNCDTreatmentPlan(request: NCDTreatmentPlanModel): Response<APIResponse<NCDTreatmentPlanModel>>
    suspend fun getNCDTreatmentPlan(request: NCDTreatmentPlanModelDetails): Response<APIResponse<NCDTreatmentPlanModelDetails>>
    suspend fun createNCDMRSummaryCreate(request: NCDMRSummaryRequestResponse): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getPrescriptionDispenseList(request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>>
    suspend fun updateDispensePrescription(request: DispensePrescriptionRequest): Response<APIResponse<DispenseUpdateResponse>>
    suspend fun getDispensePrescriptionHistory(request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>>
    suspend fun createLifestyle(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>
    suspend fun updateLifestyle(request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getLifestyleList(request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>>
    suspend fun removeLifestyle(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>
    suspend fun createPsychological(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>
    suspend fun updatePsychological(request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getPsychological(request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>>
    suspend fun removePsychological(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>
    suspend fun getPatientPrescriptionHistoryList(request: RemovePrescriptionRequest): Response<APIResponse<ArrayList<Prescription>>>
    suspend fun getNCDMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<NCDMedicalReviewHistory>>
    suspend fun validatePatient(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdGetInstructions(): Response<APIResponse<NCDInstructionModel>>
    suspend fun ncdUpdatePregnancyRisk(request: NCDPregnancyRiskUpdate): Response<APIResponse<Boolean>>
    suspend fun getWazWhzScore(request: WazWhzScoreRequest): Response<APIResponse<WazWhzScoreResponse>>
    suspend fun getUserDashboardDetails(request: NCDUserDashboardRequest): Response<APIResponse<NCDUserDashboardResponse>>
    suspend fun getBadgeNotifications(request: BadgeNotificationModel): Response<APIResponse<BadgeNotificationModel>>
    suspend fun updateBadgeNotifications(request: BadgeNotificationModel): Response<APIResponse<Boolean>>
    suspend fun getNcdLifeStyleDetails(request: LifeStyleRequest): Response<APIResponse<ArrayList<LifeStyleResponse>>>
    suspend fun ncdPatientRemove(request: NCDPatientRemoveRequest): Response<APIResponse<Boolean>>
    suspend fun bpLogCreateForNurse(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun glucoseLogCreateForNurse(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdUpdatePatientDetail(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun getUserTermsAndConditions(request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>>
    suspend fun updateTermsAndConditionsStatus(request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>>
    suspend fun ncdUpdateNextVisitDate(request: NCDMedicalReviewUpdateModel): Response<APIResponse<HashMap<String, Any>>>
    suspend fun validatePatientTransfer(request: NCDPatientTransferValidate): Response<HashMap<String, Any>>
    suspend fun createPatientTransfer(request: NCDTransferCreateRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun searchSite(request: NCDRegionSiteModel): Response<APIResponse<ArrayList<RegionSiteResponse>>>
    suspend fun searchRoleUser(request: NCDSiteRoleModel): Response<APIResponse<ArrayList<NCDSiteRoleResponse>>>
    suspend fun getPatientListTransfer(request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<PatientTransferListResponse>>
    suspend fun patientTransferNotificationCount(request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<NCDPatientTransferNotificationCountResponse>>
    suspend fun patientTransferUpdate(request: NCDPatientTransferUpdateRequest): Response<APIResponse<String>>
    suspend fun getNudgesList(prescriptionNudgeRequest: PredictionRequest): Response<APIResponse<PrescriptionNudgeResponse>>
    suspend fun getLabTestNudgeList(predictionRequest: PredictionRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdFollowUpList(request: FollowUpRequest): APIResponse<List<PatientFollowUpEntity>>
    suspend fun getPatientCallRegister(): Response<APIResponse<RegisterCallResponse>>
    suspend fun updatePatientCallRegister(request: FollowUpUpdateRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun updateDeviceDetails(request: DeviceDetails): Response<APIResponse<DeviceDetails>>
    suspend fun createMentalHealthStatus(request: NCDMentalHealthStatusRequest): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdMentalHealthMedicalReviewCreateA(request: JsonObject): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdPatientDiagnosisStatus(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdMentalHealthMedicalReviewCreateS(request: JsonObject): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdMentalHealthMedicalReviewDetailsA(request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>>
    suspend fun ncdMentalHealthMedicalReviewDetailsS(request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>>
    suspend fun markAsReviewed(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    suspend fun cultureLocaleUpdate(request: CultureLocaleModel): Response<APIResponse<HashMap<String, Any>>>
}