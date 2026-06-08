package org.medtroniclabs.uhis.network

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.AboveFiveYearsMetaResponse
import org.medtroniclabs.uhis.data.AboveFiveYearsSummaryDetails
import org.medtroniclabs.uhis.data.AboveFiveYearsSummaryRequest
import org.medtroniclabs.uhis.data.BirthHistoryRequest
import org.medtroniclabs.uhis.data.BirthHistoryResponse
import org.medtroniclabs.uhis.data.DiagnosisDiseaseModel
import org.medtroniclabs.uhis.data.DiagnosisSaveUpdateRequest
import org.medtroniclabs.uhis.data.DispensePrescriptionRequest
import org.medtroniclabs.uhis.data.DispensePrescriptionResponse
import org.medtroniclabs.uhis.data.DispenseUpdateRequest
import org.medtroniclabs.uhis.data.DispenseUpdateResponse
import org.medtroniclabs.uhis.data.FamilyPlanningMetaResponse
import org.medtroniclabs.uhis.data.FormMetaRequest
import org.medtroniclabs.uhis.data.FormRequest
import org.medtroniclabs.uhis.data.FormResponse
import org.medtroniclabs.uhis.data.HivClinicalInfoResponse
import org.medtroniclabs.uhis.data.LabourDeliveryMetaResponse
import org.medtroniclabs.uhis.data.LoginResponse
import org.medtroniclabs.uhis.data.MedicalReviewSummarySubmitRequest
import org.medtroniclabs.uhis.data.MedicationGroupSearchRequest
import org.medtroniclabs.uhis.data.MedicationResponse
import org.medtroniclabs.uhis.data.MedicationSearchRequest
import org.medtroniclabs.uhis.data.MetaDataResponse
import org.medtroniclabs.uhis.data.MotherNeonateAncMetaResponse
import org.medtroniclabs.uhis.data.MotherNeonateAncSummaryModel
import org.medtroniclabs.uhis.data.MotherNeonatePncSummaryRequest
import org.medtroniclabs.uhis.data.MotherNeonatePncSummaryResponse
import org.medtroniclabs.uhis.data.MotherPncResponse
import org.medtroniclabs.uhis.data.NCDUserDashboardRequest
import org.medtroniclabs.uhis.data.NCDUserDashboardResponse
import org.medtroniclabs.uhis.data.NeonatePncResponse
import org.medtroniclabs.uhis.data.PatientPrescriptionModel
import org.medtroniclabs.uhis.data.PatientStatusRequest
import org.medtroniclabs.uhis.data.PatientStatusResponse
import org.medtroniclabs.uhis.data.PncChildMedicalReview
import org.medtroniclabs.uhis.data.PregnancyDetailsModel
import org.medtroniclabs.uhis.data.Prescription
import org.medtroniclabs.uhis.data.PrescriptionListRequest
import org.medtroniclabs.uhis.data.ReferPatientAPIRequest
import org.medtroniclabs.uhis.data.ReferPatientHealthFacilityItem
import org.medtroniclabs.uhis.data.ReferPatientNameNumber
import org.medtroniclabs.uhis.data.ReferPatientRequest
import org.medtroniclabs.uhis.data.ReferPatientResult
import org.medtroniclabs.uhis.data.RemovePrescriptionRequest
import org.medtroniclabs.uhis.data.ResponseDataModel
import org.medtroniclabs.uhis.data.SummaryCreateRequest
import org.medtroniclabs.uhis.data.TbMetaResponse
import org.medtroniclabs.uhis.data.UnderFiveYearsMetaResponse
import org.medtroniclabs.uhis.data.UnderTwoMonthsMetaResponse
import org.medtroniclabs.uhis.data.UserSymptomsEntity
import org.medtroniclabs.uhis.data.WhoClinicalStageCreateRequest
import org.medtroniclabs.uhis.data.history.BirthDetails
import org.medtroniclabs.uhis.data.history.HistoryEntity
import org.medtroniclabs.uhis.data.history.MedicalReviewHistory
import org.medtroniclabs.uhis.data.history.NCDMedicalReviewHistory
import org.medtroniclabs.uhis.data.model.AboveFiveYearsSubmitRequest
import org.medtroniclabs.uhis.data.model.BpAndWeightRequestModel
import org.medtroniclabs.uhis.data.model.BpAndWeightResponse
import org.medtroniclabs.uhis.data.model.CreateLabourDeliveryRequest
import org.medtroniclabs.uhis.data.model.CreateLabourDeliveryResponse
import org.medtroniclabs.uhis.data.model.FamilyPlanningContraceptivesRequest
import org.medtroniclabs.uhis.data.model.FamilyPlanningCreateResponse
import org.medtroniclabs.uhis.data.model.FamilyPlanningSummaryResponse
import org.medtroniclabs.uhis.data.model.HivCreateScreeningSummaryResponse
import org.medtroniclabs.uhis.data.model.HivMetaResponse
import org.medtroniclabs.uhis.data.model.HivRequestData
import org.medtroniclabs.uhis.data.model.HivScreeningRequest
import org.medtroniclabs.uhis.data.model.HivScreeningResponse
import org.medtroniclabs.uhis.data.model.HivSummaryResponse
import org.medtroniclabs.uhis.data.model.LabourDeliverySummaryDetails
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.model.MotherNeonateAncRequest
import org.medtroniclabs.uhis.data.model.MotherNeonatePncRequest
import org.medtroniclabs.uhis.data.model.PatientDataModel
import org.medtroniclabs.uhis.data.model.PatientEncounterResponse
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.model.PatientTypeCreateRequest
import org.medtroniclabs.uhis.data.model.PncSubmitResponse
import org.medtroniclabs.uhis.data.model.RegistrationResponse
import org.medtroniclabs.uhis.data.model.RequestChangePassword
import org.medtroniclabs.uhis.data.model.RequestMemberDetails
import org.medtroniclabs.uhis.data.model.ResponseChangePassword
import org.medtroniclabs.uhis.data.model.SiteRoleResponse
import org.medtroniclabs.uhis.data.model.TbHistory
import org.medtroniclabs.uhis.data.model.TbMedicalReviewCreateRequest
import org.medtroniclabs.uhis.data.model.ViralLoadRequest
import org.medtroniclabs.uhis.data.model.ViralLoadResponse
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHold
import org.medtroniclabs.uhis.data.offlinesync.model.RequestGetSyncStatus
import org.medtroniclabs.uhis.data.offlinesync.model.ResponseSignatureUpload
import org.medtroniclabs.uhis.data.offlinesync.model.SyncResponse
import org.medtroniclabs.uhis.data.performance.CHWPerformanceMonitoring
import org.medtroniclabs.uhis.data.performance.ChwVillageFilterModel
import org.medtroniclabs.uhis.data.performance.FilterPreference
import org.medtroniclabs.uhis.data.performance.PerformanceReportRequest
import org.medtroniclabs.uhis.data.registration.AssessmentListRequest
import org.medtroniclabs.uhis.data.registration.BPLogListResponse
import org.medtroniclabs.uhis.data.registration.BadgeModel
import org.medtroniclabs.uhis.data.registration.BadgeResponseModel
import org.medtroniclabs.uhis.data.registration.BloodGlucoseListResponse
import org.medtroniclabs.uhis.data.registration.ConfirmDiagnosesRequest
import org.medtroniclabs.uhis.data.registration.FillMedicineResponse
import org.medtroniclabs.uhis.data.registration.FillPrescriptionListResponse
import org.medtroniclabs.uhis.data.registration.FillPrescriptionRequest
import org.medtroniclabs.uhis.data.registration.FillPrescriptionUpdateRequest
import org.medtroniclabs.uhis.data.registration.InitialEncounterResponse
import org.medtroniclabs.uhis.data.registration.InstructionModel
import org.medtroniclabs.uhis.data.registration.InvestigationNudgesModel
import org.medtroniclabs.uhis.data.registration.InvestigationReq
import org.medtroniclabs.uhis.data.registration.LabTestSearchResponse
import org.medtroniclabs.uhis.data.registration.Lifestyle
import org.medtroniclabs.uhis.data.registration.MedicationSearchReqModel
import org.medtroniclabs.uhis.data.registration.NurseCreateResponse
import org.medtroniclabs.uhis.data.registration.NurseLabTest
import org.medtroniclabs.uhis.data.registration.NurseMrRequestModel
import org.medtroniclabs.uhis.data.registration.PatientCreateResponse
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PatientHistoryRequest
import org.medtroniclabs.uhis.data.registration.PatientLabTestHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientMedicalReviewHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientPregnancyModel
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.registration.PatientRemoveRequest
import org.medtroniclabs.uhis.data.registration.PatientVisit
import org.medtroniclabs.uhis.data.registration.PregnancyCreateRequest
import org.medtroniclabs.uhis.data.registration.PregnancyRiskUpdate
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.PrescriptionPredictionResponse
import org.medtroniclabs.uhis.data.registration.PrescriptionRefillHistoryResponse
import org.medtroniclabs.uhis.data.registration.QRCodeRequest
import org.medtroniclabs.uhis.data.registration.QRCodeResponse
import org.medtroniclabs.uhis.data.registration.RegionSiteModel
import org.medtroniclabs.uhis.data.registration.RequestPatientDetail
import org.medtroniclabs.uhis.data.registration.ResponsePatientDetail
import org.medtroniclabs.uhis.data.registration.SearchModel
import org.medtroniclabs.uhis.data.registration.SessionEncounterRequest
import org.medtroniclabs.uhis.data.registration.SessionGraphModel
import org.medtroniclabs.uhis.data.registration.SessionHistoryResponse
import org.medtroniclabs.uhis.data.registration.SessionModelResponse
import org.medtroniclabs.uhis.data.registration.SingleWindowModel
import org.medtroniclabs.uhis.data.registration.SiteRoleModel
import org.medtroniclabs.uhis.data.registration.SummaryResponse
import org.medtroniclabs.uhis.data.registration.TerminateSessionModel
import org.medtroniclabs.uhis.data.registration.TransferCreateRequest
import org.medtroniclabs.uhis.data.registration.UserDashboardRequest
import org.medtroniclabs.uhis.data.registration.UserDashboardResponse
import org.medtroniclabs.uhis.data.resource.CD4DetailsRequest
import org.medtroniclabs.uhis.data.resource.CD4DetailsResponse
import org.medtroniclabs.uhis.data.resource.LabourDeliverySummaryRequest
import org.medtroniclabs.uhis.data.resource.RequestAllEntities
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.model.ARTResponse
import org.medtroniclabs.uhis.model.ArtRequest
import org.medtroniclabs.uhis.model.CultureLocaleModel
import org.medtroniclabs.uhis.model.LabTestCreateRequest
import org.medtroniclabs.uhis.model.LabTestListRequest
import org.medtroniclabs.uhis.model.LabTestListResponse
import org.medtroniclabs.uhis.model.NcdMRStaticDataModel
import org.medtroniclabs.uhis.model.PatientDetailRequest
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.model.PatientsDataModel
import org.medtroniclabs.uhis.model.PregnancySummaryRequest
import org.medtroniclabs.uhis.model.ReferralData
import org.medtroniclabs.uhis.model.ReferralDetailRequest
import org.medtroniclabs.uhis.model.RemoveLabTestRequest
import org.medtroniclabs.uhis.model.SearchAndListResponse
import org.medtroniclabs.uhis.model.communityprofile.CommunityProfileDetails
import org.medtroniclabs.uhis.model.medicalreview.AddMemberRegRequest
import org.medtroniclabs.uhis.model.medicalreview.CreateUnderFiveYearsRequest
import org.medtroniclabs.uhis.model.medicalreview.CreateUnderTwoMonthsRequest
import org.medtroniclabs.uhis.model.medicalreview.CreateUnderTwoMonthsResponse
import org.medtroniclabs.uhis.model.medicalreview.EMTCTVisitStatusRequest
import org.medtroniclabs.uhis.model.medicalreview.EMTCTVisitStatusResponse
import org.medtroniclabs.uhis.model.medicalreview.HivVitalsRequest
import org.medtroniclabs.uhis.model.medicalreview.HivVitalsResponse
import org.medtroniclabs.uhis.model.medicalreview.RequestBirthDetails
import org.medtroniclabs.uhis.model.medicalreview.RequestCreateImmunisation
import org.medtroniclabs.uhis.model.medicalreview.RequestImmunisationSummaryCreate
import org.medtroniclabs.uhis.model.medicalreview.RequestImmunisationSummaryDetail
import org.medtroniclabs.uhis.model.medicalreview.RequestVaccinationList
import org.medtroniclabs.uhis.model.medicalreview.ResponseCreateImmunisation
import org.medtroniclabs.uhis.model.medicalreview.ResponseImmunisationSummaryCreate
import org.medtroniclabs.uhis.model.medicalreview.ResponseImmunisationSummaryDetails
import org.medtroniclabs.uhis.model.medicalreview.SearchLabTestResponse
import org.medtroniclabs.uhis.model.medicalreview.SearchRequestLabTest
import org.medtroniclabs.uhis.model.medicalreview.SummaryDetails
import org.medtroniclabs.uhis.model.medicalreview.VaccinationDetail
import org.medtroniclabs.uhis.model.medicalreview.WazWhzScoreRequest
import org.medtroniclabs.uhis.model.medicalreview.WazWhzScoreResponse
import org.medtroniclabs.uhis.ncd.data.AssessmentResultModel
import org.medtroniclabs.uhis.ncd.data.BPBGListModel
import org.medtroniclabs.uhis.ncd.data.BadgeNotificationModel
import org.medtroniclabs.uhis.ncd.data.DeviceDetails
import org.medtroniclabs.uhis.ncd.data.FollowUpRequest
import org.medtroniclabs.uhis.ncd.data.FollowUpUpdateRequest
import org.medtroniclabs.uhis.ncd.data.LifeStyleRequest
import org.medtroniclabs.uhis.ncd.data.LifeStyleResponse
import org.medtroniclabs.uhis.ncd.data.MRSummaryResponse
import org.medtroniclabs.uhis.ncd.data.MedicalReviewRequestResponse
import org.medtroniclabs.uhis.ncd.data.MedicalReviewResponse
import org.medtroniclabs.uhis.ncd.data.NCDCounselingModel
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisGetRequest
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisGetResponse
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisRequestResponse
import org.medtroniclabs.uhis.ncd.data.NCDInstructionModel
import org.medtroniclabs.uhis.ncd.data.NCDMRSummaryRequestResponse
import org.medtroniclabs.uhis.ncd.data.NCDMedicalReviewUpdateModel
import org.medtroniclabs.uhis.ncd.data.NCDMentalHealthMedicalReviewDetails
import org.medtroniclabs.uhis.ncd.data.NCDMentalHealthStatusRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientRemoveRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientStatusRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountResponse
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferUpdateRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferValidate
import org.medtroniclabs.uhis.ncd.data.NCDPregnancyRiskUpdate
import org.medtroniclabs.uhis.ncd.data.NCDRegionSiteModel
import org.medtroniclabs.uhis.ncd.data.NCDSiteRoleModel
import org.medtroniclabs.uhis.ncd.data.NCDSiteRoleResponse
import org.medtroniclabs.uhis.ncd.data.NCDSupportRequest
import org.medtroniclabs.uhis.ncd.data.NCDTransferCreateRequest
import org.medtroniclabs.uhis.ncd.data.NCDTreatmentPlanModel
import org.medtroniclabs.uhis.ncd.data.NCDTreatmentPlanModelDetails
import org.medtroniclabs.uhis.ncd.data.PatientFollowUpEntity
import org.medtroniclabs.uhis.ncd.data.PatientTransferListResponse
import org.medtroniclabs.uhis.ncd.data.PatientVisitRequest
import org.medtroniclabs.uhis.ncd.data.PatientVisitResponse
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationRequest
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationResponse
import org.medtroniclabs.uhis.ncd.data.PredictionRequest
import org.medtroniclabs.uhis.ncd.data.PrescriptionNudgeResponse
import org.medtroniclabs.uhis.ncd.data.RegionSiteResponse
import org.medtroniclabs.uhis.ncd.data.RegisterCallResponse
import org.medtroniclabs.uhis.ncd.data.ScreeningPatientResponse
import org.medtroniclabs.uhis.ncd.data.TermsAndConditionsModel
import retrofit2.Response
import retrofit2.http.Body

interface ApiHelper {
    suspend fun doLogin(loginRequest: MultipartBody): Response<LoginResponse>

    suspend fun getMetaDataInformation(): Response<APIResponse<MetaDataResponse>>

    suspend fun getForms(formRequest: FormRequest): Response<APIResponse<FormResponse>>

    suspend fun getFormMetadata(request: FormMetaRequest): Response<APIResponse<UserSymptomsEntity>>

    suspend fun postOfflineSync(request: Map<String, Any>): Response<SyncResponse>

    suspend fun getOfflineSyncStatus(request: RequestGetSyncStatus): Response<SyncResponse>

    suspend fun getHouseholdAndMembers(request: RequestAllEntities): Response<APIResponse<List<HouseHold>>>

    suspend fun fetchSyncedData(request: RequestAllEntities): Response<ResponseBody>

    suspend fun fetchMemberAssessmentHistory(request: RequestAllEntities): Response<List<MemberAssessmentHistoryEntity>>

    suspend fun getMemberDetails(request: RequestMemberDetails): Response<ResponseBody>

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

    suspend fun searchMedicationGroupByName(request: MedicationGroupSearchRequest): Response<APIResponse<ArrayList<MedicationResponse>>>

    suspend fun createMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>>

    suspend fun getMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>>

    suspend fun saveMotherNeonateAnc(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<PatientEncounterResponse>>

    suspend fun saveMotherNeonatePnc(motherNeonatePncRequest: MotherNeonatePncRequest): Response<APIResponse<PncSubmitResponse>>

    suspend fun fetchSummary(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<MotherNeonateAncSummaryModel>>

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

    suspend fun getNursePrescriptionList(request: PatientPrescriptionModel): Response<APIResponse<ArrayList<PrescriptionModel>>>

    suspend fun removePrescription(request: RemovePrescriptionRequest): Response<APIResponse<Map<String, Any>>>

    suspend fun removePrescription(request: PatientPrescriptionModel): Response<APIResponse<ResponseDataModel>>

    suspend fun removeCommunityPrescription(request: List<RemovePrescriptionRequest>): Response<APIResponse<Map<String, Any>>>

    suspend fun getPrescription(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>>

    suspend fun getMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<MedicalReviewHistory>>

    suspend fun getPncSummaryDetails(request: MotherNeonatePncSummaryRequest): Response<APIResponse<MotherNeonatePncSummaryResponse>>

    suspend fun summaryCreatePncData(summaryCreateRequest: SummaryCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getBirthHistoryDetails(request: BirthHistoryRequest): Response<APIResponse<BirthHistoryResponse>>

    suspend fun createMedicalReviewLabourDelivery(request: CreateLabourDeliveryRequest): Response<APIResponse<CreateLabourDeliveryResponse>>

    suspend fun searchLabTestByName(request: SearchRequestLabTest): Response<APIResponse<ArrayList<SearchLabTestResponse>>>

    suspend fun createLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>>

    suspend fun updateLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>>

    suspend fun getLabTestList(request: LabTestListRequest): Response<APIResponse<ArrayList<LabTestListResponse>>>

    suspend fun removeLabTest(request: RemoveLabTestRequest): Response<APIResponse<Map<String, Any>>>

    suspend fun addNewMember(request: AddMemberRegRequest): Response<APIResponse<String>>

    suspend fun createSummaryMotherNeonate(request: LabourDeliverySummaryRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getInvestigation(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>>

    suspend fun getMedicalReviewHistoryPNC(request: ReferralDetailRequest): Response<APIResponse<PncChildMedicalReview>>

    suspend fun getPeerSupervisorLinkedChwList(): Response<APIResponse<List<ChwVillageFilterModel>>>

    suspend fun getPeerSupervisorReport(request: PerformanceReportRequest): Response<APIResponse<List<CHWPerformanceMonitoring>>>

    suspend fun getUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>>

    suspend fun saveUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>>

    suspend fun forgotPassword(
        email: String,
        clientConstant: String,
    ): Response<APIResponse<String?>>

    suspend fun verifyToken(token: String): Response<APIResponse<String?>>

    suspend fun resetPassword(
        token: String,
        request: RequestChangePassword,
    ): Response<APIResponse<ResponseChangePassword>>

    suspend fun uploadAllConsentSignatures(request: RequestBody): Response<APIResponse<List<ResponseSignatureUpload>>>

    suspend fun checkAppVersion(): Response<APIResponse<Boolean>>

    suspend fun registerPatient(hashMap: RequestBody): Response<APIResponse<RegistrationResponse>>

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

    suspend fun createPatientVisit(request: MedicalReviewBaseRequest): Response<APIResponse<PatientVisit>>

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

    suspend fun patientTransferNotificationCount(
        request: NCDPatientTransferNotificationCountRequest,
    ): Response<APIResponse<NCDPatientTransferNotificationCountResponse>>

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

    suspend fun getVaccinationList(request: RequestVaccinationList): Response<APIResponse<ArrayList<VaccinationDetail>>>

    suspend fun saveImmunisationList(request: RequestCreateImmunisation): Response<APIResponse<ResponseCreateImmunisation>>

    suspend fun getImmunisationSummaryDetails(request: RequestImmunisationSummaryDetail): Response<APIResponse<ResponseImmunisationSummaryDetails>>

    suspend fun saveImmunisationSummaryDetails(request: RequestImmunisationSummaryCreate): Response<APIResponse<ResponseImmunisationSummaryCreate>>

    suspend fun createCommunityProfile(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun updateCommunityProfile(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getCommunityProfile(request: HashMap<String, Any>): Response<APIResponse<CommunityProfileDetails>>

    suspend fun createSupportRequest(request: NCDSupportRequest): Response<APIResponse<String>>

    suspend fun getTbStaticData(): Response<APIResponse<TbMetaResponse>>

    suspend fun createHeight(request: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>>

    suspend fun fetchHeight(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>>

    suspend fun fetchBmi(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>>

    suspend fun fetchList(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<List<BpAndWeightResponse>>>

    suspend fun fetchTbAssessmentDetails(request: MotherNeonateAncRequest): Response<APIResponse<TbHistory>>

    suspend fun saveTbMedicalReview(request: TbMedicalReviewCreateRequest): Response<APIResponse<PatientEncounterResponse>>

    suspend fun createPatientType(request: PatientTypeCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getPatientType(request: MotherNeonateAncRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getBirthDetails(request: RequestBirthDetails): Response<APIResponse<BirthDetails>>

    suspend fun getCBSNotificationDetails(request: PeerSupervisorNotificationRequest): Response<APIResponse<ArrayList<PeerSupervisorNotificationResponse>>>

    suspend fun updateCBSNotification(request: PeerSupervisorNotificationRequest): Response<APIResponse<Unit>>

    suspend fun createBMI(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getHivStaticData(): Response<APIResponse<HivMetaResponse>>

    suspend fun createHivScreening(request: HivScreeningRequest): Response<APIResponse<HivScreeningResponse>>

    suspend fun getHivScreeningDetails(request: HivScreeningResponse): Response<APIResponse<HivCreateScreeningSummaryResponse>>

    suspend fun createFamilyPlanningMR(request: FamilyPlanningContraceptivesRequest): Response<APIResponse<FamilyPlanningCreateResponse>>

    suspend fun getFamilyPlanningStaticData(): Response<APIResponse<FamilyPlanningMetaResponse>>

    suspend fun getFamilyPlanningMRSummaryDetails(request: AboveFiveYearsSummaryRequest): Response<APIResponse<FamilyPlanningSummaryResponse>>

    suspend fun createHivImrCmr(request: HivRequestData): Response<APIResponse<PatientEncounterResponse>>

    suspend fun fetchHivSummaryDetails(request: MotherNeonateAncRequest): Response<APIResponse<HivSummaryResponse>>

    suspend fun getViralLoadData(request: ViralLoadRequest): Response<APIResponse<List<ViralLoadResponse>>>

    suspend fun getARTData(request: ArtRequest): Response<APIResponse<List<ARTResponse>>>

    suspend fun getOpportunisticInfection(request: MotherNeonateAncRequest): Response<APIResponse<HashMap<String, HashMap<String, String>?>>>

    suspend fun createEMTCT(request: EMTCTVisitStatusRequest): Response<APIResponse<EMTCTVisitStatusResponse>>

    suspend fun getHivVitalsDetails(request: HivVitalsRequest): Response<APIResponse<HivVitalsResponse>>

    suspend fun createWhoClinicalStage(request: WhoClinicalStageCreateRequest): Response<APIResponse<HivClinicalInfoResponse>>

    suspend fun getHivCD4Details(request: CD4DetailsRequest): Response<APIResponse<ArrayList<CD4DetailsResponse>>>

    suspend fun checkRecommendationInvestigations(request: MotherNeonateAncRequest): Response<APIResponse<HashMap<String, Boolean?>?>>

    suspend fun getPatientSummaryDetails(request: PregnancySummaryRequest): Response<APIResponse<PregnancyDetailsModel>>

    suspend fun patientFollowUpList(request: PatientDataModel): APIResponse<ArrayList<PatientListResModel>>

    suspend fun searchPatientById(request: PatientDataModel): APIResponse<ArrayList<PatientListResModel>>

    suspend fun patientsList(request: PatientDataModel): APIResponse<ArrayList<PatientListResModel>>

    suspend fun validatePatient(request: JsonObject): Response<APIResponse<Boolean>>

    suspend fun createPatient(request: JsonObject): Response<APIResponse<PatientCreateResponse>>

    suspend fun validateQRCodeValidation(qrCodeRequest: QRCodeRequest): Response<QRCodeResponse>

    suspend fun createScreeningLog(createRequest: JsonObject): Response<ScreeningPatientResponse>

    suspend fun validateSession(): Response<ResponseBody>

    suspend fun getPatientDetails(request: PatientDetailsModel): Response<APIResponse<PatientDetailsModel>>

    suspend fun getPatientBPLogList(request: AssessmentListRequest): Response<APIResponse<BPLogListResponse>>

    suspend fun getPatientBloodGlucoseList(request: AssessmentListRequest): Response<APIResponse<BloodGlucoseListResponse>>

    suspend fun getSessionGraph(request: AssessmentListRequest): Response<APIResponse<SessionGraphModel>>

    suspend fun searchMedication(request: MedicationSearchReqModel): Response<APIResponse<ArrayList<PrescriptionModel>>>

    suspend fun createPregnancy(request: PregnancyCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun updatePregnancy(request: PregnancyCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getPatientPregnancyDetails(request: PatientPregnancyModel): Response<APIResponse<PregnancyCreateRequest>>

    suspend fun confirmDiagnosis(request: ConfirmDiagnosesRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getMentalHealthDetails(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getPatientMedicalReviewSummary(request: MedicalReviewBaseRequest): Response<APIResponse<SummaryResponse>>

    suspend fun updateTreatmentPlan(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    suspend fun treatmentPlanDetails(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getScreeningDetails(request: RequestPatientDetail): Response<APIResponse<ResponsePatientDetail>>

    suspend fun createBpLog(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun createGlucoseLog(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun patientRemove(request: PatientRemoveRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun clearRedRisk(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getInstructions(): Response<APIResponse<InstructionModel>>

    suspend fun updatePregnancyRisk(request: PregnancyRiskUpdate): Response<APIResponse<Boolean>>

    suspend fun getSessionQuestions(
        session: Int,
        age: Int,
    ): Response<APIResponse<ArrayList<SessionModelResponse>>>

    suspend fun getPatientFamilyOtherDetails(data: Lifestyle): Response<APIResponse<HashMap<String, Any>>>

    suspend fun postSessionTerminate(request: TerminateSessionModel): Response<APIResponse<HashMap<String, Any>>>

    suspend fun medicationList(request: MedicationSearchReqModel): Response<APIResponse<ArrayList<PrescriptionModel>>>

    suspend fun createSingleWindowMR(request: SingleWindowModel): Response<APIResponse<InitialEncounterResponse>>

    suspend fun getBadgeCount(request: BadgeModel): Response<APIResponse<BadgeResponseModel>>

    suspend fun searchSite(request: RegionSiteModel): Response<APIResponse<ArrayList<RegionSiteResponse>>>

    suspend fun searchRoleUser(request: SiteRoleModel): Response<APIResponse<ArrayList<SiteRoleResponse>>>

    suspend fun createPatientTransfer(request: TransferCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun validatePatientTransfer(request: FillPrescriptionRequest): Response<APIResponse<HashMap<String, Any>>>

    suspend fun sessionCreate(request: SessionEncounterRequest): Response<APIResponse<InitialEncounterResponse>>

    suspend fun createNurseMedicalReview(request: NurseMrRequestModel): Response<APIResponse<NurseCreateResponse>>

    suspend fun updatePrescription(body: RequestBody): Response<APIResponse<ResponseDataModel>>

    suspend fun getPatientPrescriptionHistoryList(request: PatientHistoryRequest): Response<APIResponse<PatientPrescriptionHistoryResponse>>

    suspend fun getPrescriptionPrediction(map: HashMap<String, Any>): Response<APIResponse<PrescriptionPredictionResponse>>

    suspend fun getPatientLabTestHistory(request: PatientHistoryRequest): Response<APIResponse<PatientLabTestHistoryResponse>>

    suspend fun getPatientMedicalReviewHistoryList(request: MedicalReviewBaseRequest): Response<APIResponse<PatientMedicalReviewHistoryResponse>>

    suspend fun getPatientSessionHistoryList(request: MedicalReviewBaseRequest): Response<APIResponse<SessionHistoryResponse>>

    suspend fun getPatientLabTestRecommendation(request: InvestigationReq): Response<APIResponse<List<NurseLabTest>>>

    suspend fun getPatientLabTests(request: HashMap<String, Any?>): Response<APIResponse<org.medtroniclabs.uhis.data.registration.LabTestListResponse>>

    suspend fun searchLabTest(request: SearchModel): Response<APIResponse<ArrayList<LabTestSearchResponse>>>

    suspend fun getLabTestResult(labTestId: Long): Response<APIResponse<ArrayList<HashMap<String, Any>>>>

    suspend fun getNudgesList(request: HashMap<String, Any>): Response<APIResponse<InvestigationNudgesModel>>

    suspend fun referLabTest(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun createLabTestResult(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun getLabTestResultDetails(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun removeLabTest(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun reviewLabTestResult(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    suspend fun updatePatientType(request: HashMap<String, Any>): Response<APIResponse<Boolean>>

    suspend fun getPatientFillPrescriptionList(request: FillPrescriptionRequest): Response<APIResponse<ArrayList<FillPrescriptionListResponse>>>

    suspend fun fillPrescriptionUpdate(request: FillPrescriptionUpdateRequest): Response<APIResponse<ArrayList<FillMedicineResponse>>>

    suspend fun getPrescriptionRefillHistory(
        request: org.medtroniclabs.uhis.data.registration.PatientPrescriptionModel,
    ): Response<APIResponse<ArrayList<PrescriptionRefillHistoryResponse>>>

    suspend fun getProviderDashboardDetails(request: UserDashboardRequest): Response<APIResponse<UserDashboardResponse>>
}
