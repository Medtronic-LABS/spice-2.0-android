package org.medtroniclabs.uhis.network

import com.google.gson.JsonObject
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
import org.medtroniclabs.uhis.data.medicalreview.ReqBPBGLogList
import org.medtroniclabs.uhis.data.medicalreview.ResLabTestRecommendations
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
import org.medtroniclabs.uhis.data.model.PatientDetailsFollowUp
import org.medtroniclabs.uhis.data.model.PatientEncounterResponse
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.model.PatientTypeCreateRequest
import org.medtroniclabs.uhis.data.model.PncSubmitResponse
import org.medtroniclabs.uhis.data.model.RegisterCallRequest
import org.medtroniclabs.uhis.data.model.RegistrationResponse
import org.medtroniclabs.uhis.data.model.RequestChangePassword
import org.medtroniclabs.uhis.data.model.RequestMemberDetails
import org.medtroniclabs.uhis.data.model.ResponseChangePassword
import org.medtroniclabs.uhis.data.model.SiteRoleResponse
import org.medtroniclabs.uhis.data.model.TbHistory
import org.medtroniclabs.uhis.data.model.TbMedicalReviewCreateRequest
import org.medtroniclabs.uhis.data.model.UpdatePatientCallRegister
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
import org.medtroniclabs.uhis.data.telesupport.TCPatientDetailsResponse
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
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import org.medtroniclabs.uhis.data.model.RegisterCallResponse as ModelRegisterCallResponse

interface ApiService {
    @POST("/auth-service/session")
    suspend fun doLogin(
        @Body request: RequestBody,
    ): Response<LoginResponse>

    @POST("/spice-service/static-data/user-data")
    suspend fun getMetaDataInformation(): Response<APIResponse<MetaDataResponse>>

    @POST("/spice-service/static-data/form-data")
    suspend fun getForms(
        @Body formRequest: FormRequest,
    ): Response<APIResponse<FormResponse>>

    @POST("/spice-service/static-data/meta-data")
    suspend fun getFormMetadata(
        @Body request: FormMetaRequest,
    ): Response<APIResponse<UserSymptomsEntity>>

    @POST("/offline-service/offline-sync/create")
    @JvmSuppressWildcards
    suspend fun postOfflineSyncData(
        @Body map: Map<String, Any>,
    ): Response<SyncResponse>

    @POST("/offline-service/offline-sync/status")
    suspend fun getOfflineSyncStatus(
        @Body request: RequestGetSyncStatus,
    ): Response<SyncResponse>

    @POST("/spice-service/household/list")
    suspend fun getHouseholdDetails(
        @Body request: RequestAllEntities,
    ): Response<APIResponse<List<HouseHold>>>

    @POST("/offline-service/offline-sync/fetch-synced-data")
    suspend fun fetchSyncedData(
        @Body request: RequestAllEntities,
    ): Response<ResponseBody>

    @POST("/offline-service/offline-sync/member-assessment-history")
    suspend fun fetchMemberAssessmentHistory(
        @Body request: RequestAllEntities,
    ): Response<List<MemberAssessmentHistoryEntity>>

    @POST("spice-service/patient/list")
    suspend fun getPatients(
        @Body request: PatientsDataModel,
    ): APIResponse<SearchAndListResponse>

    @POST("spice-service/patient/search")
    suspend fun patientSearch(
        @Body request: PatientsDataModel,
    ): APIResponse<SearchAndListResponse>

    @POST("spice-service/patient/patientDetails")
    suspend fun getPatient(
        @Body request: PatientDetailRequest,
    ): Response<APIResponse<PatientListRespModel>>

    @POST("/spice-service/static-data/meta-data/iccm-abovefive")
    suspend fun getAboveFiveYearsMetaData(): Response<APIResponse<AboveFiveYearsMetaResponse>>

    @POST("/spice-service/static-data/meta-data/iccm-under-five-years")
    suspend fun getUnderFiveYearsMetaData(): Response<APIResponse<UnderFiveYearsMetaResponse>>

    @POST("/spice-service/patient/referral-tickets")
    suspend fun getReferralsDetails(
        @Body request: ReferralDetailRequest,
    ): Response<APIResponse<ReferralData>>

    @POST("/spice-service/medical-review/iccm-general/create")
    suspend fun createAboveFiveYearsResult(
        @Body request: AboveFiveYearsSubmitRequest,
    ): Response<APIResponse<AboveFiveYearsSummaryDetails>>

    @POST("/spice-service/medical-review/iccm-general/details")
    suspend fun getAboveFiveYearsSummaryDetails(
        @Body id: AboveFiveYearsSummaryRequest,
    ): Response<APIResponse<AboveFiveYearsSummaryDetails>>

    @POST("/spice-service/static-data/meta-data/mother-neonate-anc")
    suspend fun getMotherNeoNateAncStaticData(): Response<APIResponse<MotherNeonateAncMetaResponse>>

    @POST("/spice-service/medical-review/labour-mother-neonate/create")
    suspend fun createMedicalReviewForLaborDelivery(
        @Body labourDeliveryRequest: CreateLabourDeliveryRequest,
    ): Response<APIResponse<CreateLabourDeliveryResponse>>

    @POST("/spice-service/static-data/meta-data/iccm-under-two-months")
    suspend fun getUnderTwoMonthsMetaData(): Response<APIResponse<UnderTwoMonthsMetaResponse>>

    @POST("/spice-service/static-data/meta-data/mother-neonate-pnc-mother")
    suspend fun getMotherPncStaticData(): Response<APIResponse<MotherPncResponse>>

    @POST("/spice-service/static-data/meta-data/mother-neonate-pnc-baby")
    suspend fun getNeonatePncStaticData(): Response<APIResponse<NeonatePncResponse>>

    @POST("/spice-service/medical-review/summary-create")
    suspend fun createSummarySubmit(
        @Body request: MedicalReviewSummarySubmitRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/static-data/meta-data/mother-delivery")
    suspend fun getLabourDeliveryMetaData(): Response<APIResponse<LabourDeliveryMetaResponse>>

    @POST("/spice-service/medical-review/labour-mother-neonate/details")
    suspend fun getLabourDeliverySummaryDetails(
        @Body request: LabourDeliverySummaryDetails,
    ): Response<APIResponse<CreateLabourDeliveryRequest>>

    @POST("/admin-service/medication/search")
    suspend fun searchMedicationByName(
        @Body request: MedicationSearchRequest,
    ): Response<APIResponse<ArrayList<MedicationResponse>>>

    @POST("/admin-service/medication/list-by-group")
    suspend fun searchMedicationGroupByName(
        @Body request: MedicationGroupSearchRequest,
    ): Response<APIResponse<ArrayList<MedicationResponse>>>

    @POST("/spice-service/medical-review/iccm-under-2months/create")
    suspend fun createMedicalReviewForUnderTwoMonths(
        @Body request: CreateUnderTwoMonthsRequest,
    ): Response<APIResponse<CreateUnderTwoMonthsResponse>>

    @POST("/spice-service/patient/patient-status")
    suspend fun getPatientStatus(
        @Body request: PatientStatusRequest,
    ): Response<APIResponse<PatientStatusResponse>>

    @POST("/spice-service/medical-review/iccm-under-2months/details")
    suspend fun getUnderTwoMonthsSummaryDetails(
        @Body request: CreateUnderTwoMonthsResponse,
    ): Response<APIResponse<SummaryDetails>>

    @POST("/spice-service/medical-review/anc-pregnancy/create")
    suspend fun saveMotherNeonateAnc(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<PatientEncounterResponse>>

    @POST("/spice-service/medical-review/pnc/create")
    suspend fun saveMotherNeonatePnc(
        @Body motherNeonatePncRequest: MotherNeonatePncRequest,
    ): Response<APIResponse<PncSubmitResponse>>

    @POST("/spice-service/medical-review/anc-pregnancy/details")
    suspend fun fetchSummary(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<MotherNeonateAncSummaryModel>>

    @POST("/spice-service/medical-review/weight")
    suspend fun fetchWeight(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<BpAndWeightResponse>>

    @POST("/spice-service/medical-review/bp")
    suspend fun fetchBloodPressure(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<BpAndWeightResponse>>

    @POST("/spice-service/medical-review/weight/create")
    suspend fun createWeight(
        @Body bpAndWeightRequestModel: BpAndWeightRequestModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/bp/create")
    suspend fun createBloodPressure(
        @Body bpAndWeightRequestModel: BpAndWeightRequestModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/confirm-diagnosis")
    suspend fun saveUpdateDiagnosis(
        @Body request: DiagnosisSaveUpdateRequest,
    ): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>>

    @POST("/spice-service/patient/diagnosis-details")
    suspend fun getDiagnosisDetails(
        @Body request: CreateUnderTwoMonthsResponse,
    ): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>>

    @POST("/admin-service/healthfacilities-by-district-id")
    suspend fun getHealthFacilityMetaData(
        @Body request: ReferPatientAPIRequest,
    ): Response<APIResponse<List<ReferPatientHealthFacilityItem>>>

    @POST("/user-service/user/users-by-tenant-id")
    suspend fun getReferPatientMobileUserList(
        @Body tenantId: ReferPatientRequest,
    ): Response<APIResponse<List<ReferPatientNameNumber>>>

    @POST("/spice-service/patient/referral-tickets/create")
    suspend fun createReferPatientResult(
        @Body request: ReferPatientResult,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/prescription-request/create")
    suspend fun createPrescriptionRequest(
        @Body request: RequestBody,
    ): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/prescription-request/list")
    suspend fun getPrescriptionList(
        @Body request: PrescriptionListRequest,
    ): Response<APIResponse<ArrayList<Prescription>>>

    @POST("spice-service/prescription-request/remove")
    suspend fun removePrescription(
        @Body request: RemovePrescriptionRequest,
    ): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/prescription-request/remove")
    suspend fun removeCommunityPrescription(
        @Body request: List<RemovePrescriptionRequest>,
    ): Response<APIResponse<Map<String, Any>>>

    @POST("/spice-service/medical-review/iccm-under-5years/details")
    suspend fun getUnderFiveYearsSummaryDetails(
        @Body request: CreateUnderTwoMonthsResponse,
    ): Response<APIResponse<SummaryDetails>>

    @POST("/spice-service/medical-review/iccm-under-5years/create")
    suspend fun createMedicalReviewForUnderFiveYears(
        @Body request: CreateUnderFiveYearsRequest,
    ): Response<APIResponse<CreateUnderTwoMonthsResponse>>

    @POST("/spice-service/prescription-request/prescribed-details")
    suspend fun getPrescription(
        @Body request: ReferralDetailRequest,
    ): Response<APIResponse<HistoryEntity>>

    @POST("/spice-service/medical-review/history")
    suspend fun getMedicalReviewHistory(
        @Body request: ReferralDetailRequest,
    ): Response<APIResponse<MedicalReviewHistory>>

    @POST("/spice-service/medical-review/pnc/details")
    suspend fun getPncSummaryDetails(
        @Body request: MotherNeonatePncSummaryRequest,
    ): Response<APIResponse<MotherNeonatePncSummaryResponse>>

    @POST("/spice-service/medical-review/mother-neonate/summary-create")
    suspend fun summaryCreatePncData(
        @Body request: SummaryCreateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/medical-review/birth-history")
    suspend fun getBirthHistoryDetails(
        @Body request: BirthHistoryRequest,
    ): Response<APIResponse<BirthHistoryResponse>>

    @POST("spice-service/household/create-member")
    suspend fun addNewMember(
        @Body request: AddMemberRegRequest,
    ): Response<APIResponse<String>>

    @POST("/admin-service/lab-test-customization/list")
    suspend fun searchLabTestByName(
        @Body request: SearchRequestLabTest,
    ): Response<APIResponse<ArrayList<SearchLabTestResponse>>>

    @POST("spice-service/investigation/create")
    suspend fun createLabTest(
        @Body request: LabTestCreateRequest,
    ): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/investigation/result/create")
    suspend fun updateLabTest(
        @Body request: LabTestCreateRequest,
    ): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/investigation/list")
    suspend fun getLabTestList(
        @Body request: LabTestListRequest,
    ): Response<APIResponse<ArrayList<LabTestListResponse>>>

    @POST("/spice-service/investigation/remove")
    suspend fun removeLabTest(
        @Body request: RemoveLabTestRequest,
    ): Response<APIResponse<Map<String, Any>>>

    @POST("/spice-service/medical-review/mother-neonate/summary-create")
    suspend fun summaryCreateMotherNeonate(
        @Body request: LabourDeliverySummaryRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @GET("user-service/user/peer-supervisor/linked-chw")
    suspend fun getPeerSupervisorLinkedChwList(): Response<APIResponse<List<ChwVillageFilterModel>>>

    @POST("spice-service/report/chw-performance")
    suspend fun getPeerSupervisorReport(
        @Body request: PerformanceReportRequest,
    ): Response<APIResponse<List<CHWPerformanceMonitoring>>>

    @POST("user-service/user/preferences")
    suspend fun getUserFilterPreference(
        @Body request: FilterPreference,
    ): Response<APIResponse<FilterPreference>>

    @POST("user-service/user/preferences/save")
    suspend fun saveUserFilterPreference(
        @Body request: FilterPreference,
    ): Response<APIResponse<FilterPreference>>

    @POST("/spice-service/medical-review/pnc-history")
    suspend fun getMedicalReviewHistoryPNC(
        @Body request: ReferralDetailRequest,
    ): Response<APIResponse<PncChildMedicalReview>>

    @POST("/spice-service/investigation/history-list")
    suspend fun getInvestigationHistoryList(
        @Body request: ReferralDetailRequest,
    ): Response<APIResponse<HistoryEntity>>

    @POST("/user-service/user/forgot-password/{email}/{client}")
    suspend fun forgotPassword(
        @Path("email") email: String,
        @Path("client") client: String,
    ): Response<APIResponse<String?>>

    @POST("/user-service/user/verify-token/{token}")
    suspend fun verifyToken(
        @Path("token") token: String,
    ): Response<APIResponse<String?>>

    @POST("/user-service/user/reset-password/{token}")
    suspend fun resetPassword(
        @Path("token") token: String,
        @Body request: RequestChangePassword,
    ): Response<APIResponse<ResponseChangePassword>>

    @POST("/offline-service/offline-sync/upload-signatures")
    suspend fun uploadAllConsentSignatures(
        @Body request: RequestBody,
    ): Response<APIResponse<List<ResponseSignatureUpload>>>

    @POST("spice-service/static-data/app-version")
    suspend fun checkAppVersion(): Response<APIResponse<Boolean>>

    @POST("/spice-service/screening/create")
    suspend fun createScreening(
        @Body createRequest: RequestBody,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient/register")
    suspend fun registerPatient(
        @Body request: RequestBody,
    ): Response<APIResponse<RegistrationResponse>>

    @POST("spice-service/static-data/ncd-medical-review")
    suspend fun getNcdMRStaticData(): Response<APIResponse<NcdMRStaticDataModel>>

    @POST("spice-service/bplog/create")
    suspend fun bpLogCreate(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/glucoselog/create")
    suspend fun glucoseLogCreate(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/bplog/list")
    suspend fun bpLogList(
        @Body request: BPBGListModel,
    ): Response<APIResponse<BPBGListModel>>

    @POST("spice-service/glucoselog/list")
    suspend fun glucoseLogList(
        @Body request: BPBGListModel,
    ): Response<APIResponse<BPBGListModel>>

    @POST("spice-service/assessment/create")
    suspend fun createAssessmentNCD(
        @Body request: JsonObject,
    ): Response<HashMap<String, Any>>

    @POST("spice-service/patient/pregnancy/create")
    suspend fun ncdPregnancyCreate(
        @Body request: PregnancyDetailsModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient/pregnancy/details")
    suspend fun ncdPregnancyDetails(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<PregnancyDetailsModel>>

    @POST("/spice-service/patientvisit/create")
    suspend fun createPatientVisit(
        @Body request: PatientVisitRequest,
    ): Response<APIResponse<PatientVisitResponse>>

    @POST("/spice-service/medical-review/ncd/create")
    suspend fun createNCDMedicalReview(
        @Body request: MedicalReviewRequestResponse,
    ): Response<APIResponse<MedicalReviewResponse>>

    @POST("/spice-service/medical-review/ncd/details")
    suspend fun fetchNCDMRSummary(
        @Body request: MedicalReviewResponse,
    ): Response<APIResponse<MRSummaryResponse>>

    @POST("spice-service/prescription-request/history-list")
    suspend fun getPatientPrescriptionHistoryList(
        @Body request: RemovePrescriptionRequest,
    ): Response<APIResponse<ArrayList<Prescription>>>

    @POST("/spice-service/medical-review/confirm-diagnosis/update")
    suspend fun createConfirmDiagonsis(
        @Body request: NCDDiagnosisRequestResponse,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/get-diagnosis-details")
    suspend fun getConfirmDiagonsis(
        @Body request: NCDDiagnosisGetRequest,
    ): Response<APIResponse<NCDDiagnosisGetResponse>>

    @POST("/spice-service/medical-review/patient-status/create")
    suspend fun createNCDPatientStatus(
        @Body request: NCDPatientStatusRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-treatment-plan/update")
    suspend fun updateNCDTreatmentPlan(
        @Body request: NCDTreatmentPlanModel,
    ): Response<APIResponse<NCDTreatmentPlanModel>>

    @POST("/spice-service/patient-treatment-plan/details")
    suspend fun getNCDTreatmentPlan(
        @Body request: NCDTreatmentPlanModelDetails,
    ): Response<APIResponse<NCDTreatmentPlanModelDetails>>

    @POST("/spice-service/medical-review/ncd/summary-create")
    suspend fun createNCDMRSummaryCreate(
        @Body request: NCDMRSummaryRequestResponse,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient-nutrition-lifestyle/create")
    suspend fun createLifestyle(
        @Body request: NCDCounselingModel,
    ): Response<APIResponse<NCDCounselingModel>>

    @PUT("spice-service/patient-nutrition-lifestyle/update")
    suspend fun updateLifestyle(
        @Body request: AssessmentResultModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient-nutrition-lifestyle/list")
    suspend fun getLifestyleList(
        @Body request: NCDCounselingModel,
    ): Response<APIResponse<ArrayList<NCDCounselingModel>>>

    @POST("spice-service/patient-nutrition-lifestyle/remove")
    suspend fun removeLifestyle(
        @Body request: NCDCounselingModel,
    ): Response<APIResponse<NCDCounselingModel>>

    @POST("spice-service/medical-review/patient-psychology/create")
    suspend fun createPsychological(
        @Body request: NCDCounselingModel,
    ): Response<APIResponse<NCDCounselingModel>>

    @PUT("spice-service/medical-review/patient-psychology/update")
    suspend fun updatePsychological(
        @Body request: AssessmentResultModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/medical-review/patient-psychology/list")
    suspend fun getPsychological(
        @Body request: NCDCounselingModel,
    ): Response<APIResponse<ArrayList<NCDCounselingModel>>>

    @POST("spice-service/medical-review/patient-psychology/remove")
    suspend fun removePsychological(
        @Body request: NCDCounselingModel,
    ): Response<APIResponse<NCDCounselingModel>>

    @POST("/spice-service/prescription-request/fill-prescription/list")
    suspend fun getPrescriptionDispenseList(
        @Body request: DispenseUpdateRequest,
    ): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>>

    @POST("/spice-service/prescription-request/fill-prescription/update")
    suspend fun updateDispensePrescription(
        @Body request: DispensePrescriptionRequest,
    ): Response<APIResponse<DispenseUpdateResponse>>

    @POST("/spice-service/prescription-request/refill-prescription/history")
    suspend fun getDispensePrescriptionHistory(
        @Body request: DispenseUpdateRequest,
    ): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>>

    @POST("/spice-service/medical-review/ncd/history-list")
    suspend fun getNCDMedicalReviewHistory(
        @Body request: ReferralDetailRequest,
    ): Response<APIResponse<NCDMedicalReviewHistory>>

    @POST("spice-service/patient/validate")
    suspend fun validatePatient(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @GET("/spice-service/medical-review/get-instructions")
    suspend fun ncdGetInstructions(): Response<APIResponse<NCDInstructionModel>>

    @PUT("/spice-service/patient/pregnancy-anc-risk/update")
    suspend fun ncdUpdatePregnancyRisk(
        @Body request: NCDPregnancyRiskUpdate,
    ): Response<APIResponse<Boolean>>

    @POST("/spice-service/patient/calculate-wgs")
    suspend fun getWazWhzScore(
        @Body request: WazWhzScoreRequest,
    ): Response<APIResponse<WazWhzScoreResponse>>

    @POST("/spice-service/screening/dashboard-count")
    suspend fun getUserDashboardDetails(
        @Body request: NCDUserDashboardRequest,
    ): Response<APIResponse<NCDUserDashboardResponse>>

    @POST("spice-service/medical-review/count")
    suspend fun getBadgeNotifications(
        @Body request: BadgeNotificationModel,
    ): Response<APIResponse<BadgeNotificationModel>>

    @PUT("spice-service/medical-review/update-view-status")
    suspend fun updateBadgeNotifications(
        @Body request: BadgeNotificationModel,
    ): Response<APIResponse<Boolean>>

    @POST("/spice-service/medical-review/patient-lifestyle-details")
    suspend fun getNcdLifeStyleDetails(
        @Body request: LifeStyleRequest,
    ): Response<APIResponse<ArrayList<LifeStyleResponse>>>

    @POST("/spice-service/patient/delete")
    suspend fun ncdPatientRemove(
        @Body request: NCDPatientRemoveRequest,
    ): Response<APIResponse<Boolean>>

    @POST("/spice-service/assessment/bp-log-create")
    suspend fun bpLogCreateForNurse(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/assessment/glucose-log-create")
    suspend fun glucoseLogCreateForNurse(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/update")
    suspend fun ncdUpdatePatientDetail(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("admin-service/terms-and-conditions/details")
    suspend fun getUserTermsAndConditions(
        @Body request: TermsAndConditionsModel,
    ): Response<APIResponse<TermsAndConditionsModel>>

    @POST("user-service/user/terms-and-conditions/update")
    suspend fun updateTermsAndConditionsStatus(
        @Body request: TermsAndConditionsModel,
    ): Response<APIResponse<TermsAndConditionsModel>>

    @POST("/spice-service/medical-review/ncd/date/update")
    suspend fun ncdUpdateNextVisitDate(
        @Body request: NCDMedicalReviewUpdateModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-transfer/validate")
    suspend fun validatePatientTransfer(
        @Body request: NCDPatientTransferValidate,
    ): Response<HashMap<String, Any>>

    @POST("/spice-service/patient-transfer/create")
    suspend fun createPatientTransfer(
        @Body request: NCDTransferCreateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/admin-service/country/healthfacility-list")
    suspend fun searchSite(
        @Body request: NCDRegionSiteModel,
    ): Response<APIResponse<ArrayList<RegionSiteResponse>>>

    @POST("/user-service/user/role-user-list")
    suspend fun searchRoleUser(
        @Body request: NCDSiteRoleModel,
    ): Response<APIResponse<ArrayList<NCDSiteRoleResponse>>>

    @POST("/spice-service/patient-transfer/list")
    suspend fun getPatientListTransfer(
        @Body request: NCDPatientTransferNotificationCountRequest,
    ): Response<APIResponse<PatientTransferListResponse>>

    @POST("/spice-service/patient-transfer/notification-count")
    suspend fun patientTransferNotificationCount(
        @Body request: NCDPatientTransferNotificationCountRequest,
    ): Response<APIResponse<NCDPatientTransferNotificationCountResponse>>

    @POST("/spice-service/patient-transfer/update")
    suspend fun patientTransferUpdate(
        @Body request: NCDPatientTransferUpdateRequest,
    ): Response<APIResponse<String>>

    @POST("/spice-service/prescription-request/prediction")
    suspend fun getNudgesList(
        @Body prescriptionNudgeRequest: PredictionRequest,
    ): Response<APIResponse<PrescriptionNudgeResponse>>

    @POST("/spice-service/investigation/prediction")
    suspend fun getLabTestNudgeList(
        @Body predictionRequest: PredictionRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/follow-up/ncd/list")
    suspend fun ncdFollowUpList(
        @Body request: FollowUpRequest,
    ): APIResponse<List<PatientFollowUpEntity>>

    @GET("/spice-service/follow-up")
    suspend fun getPatientCallRegister(): Response<APIResponse<RegisterCallResponse>>

    @POST("/spice-service/follow-up/ncd/update")
    suspend fun updatePatientCallRegister(
        @Body request: FollowUpUpdateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/devicedetails")
    suspend fun updateDeviceDetails(
        @Body request: DeviceDetails,
    ): Response<APIResponse<DeviceDetails>>

    @POST("/spice-service/medical-review/patient-status/create")
    suspend fun createMentalHealthStatus(
        @Body request: NCDMentalHealthStatusRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/patient-status/details")
    suspend fun ncdPatientDiagnosisStatus(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/create")
    suspend fun ncdMentalHealthMedicalReviewCreateA(
        @Body request: JsonObject,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/condition-create")
    suspend fun ncdMentalHealthMedicalReviewCreateS(
        @Body request: JsonObject,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/details")
    suspend fun ncdMentalHealthMedicalReviewDetailsA(
        @Body request: NCDMentalHealthMedicalReviewDetails,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/condition-details")
    suspend fun ncdMentalHealthMedicalReviewDetailsS(
        @Body request: NCDMentalHealthMedicalReviewDetails,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/investigation/review")
    suspend fun markAsReviewed(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/user-service/user/update-culture")
    suspend fun cultureLocaleUpdate(
        @Body request: CultureLocaleModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/immunisation/list")
    suspend fun getImmunisationList(
        @Body request: RequestVaccinationList,
    ): Response<APIResponse<ArrayList<VaccinationDetail>>>

    @POST("/spice-service/immunisation/create")
    suspend fun saveImmunisationList(
        @Body request: RequestCreateImmunisation,
    ): Response<APIResponse<ResponseCreateImmunisation>>

    @POST("/spice-service/immunisation/detail")
    suspend fun getImmunisationSummaryDetails(
        @Body request: RequestImmunisationSummaryDetail,
    ): Response<APIResponse<ResponseImmunisationSummaryDetails>>

    @POST("/spice-service/immunisation/summary-create")
    suspend fun saveImmunisationSummaryDetails(
        @Body request: RequestImmunisationSummaryCreate,
    ): Response<APIResponse<ResponseImmunisationSummaryCreate>>

    @POST("/spice-service/community-profile/get-community-profile")
    suspend fun getCommunityProfileDetails(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<CommunityProfileDetails>>

    @POST("/spice-service/community-profile/create")
    suspend fun createCommunityProfile(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/community-profile/update")
    suspend fun updateCommunityProfile(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/user-service/user/support")
    suspend fun createSupportRequest(
        @Body request: NCDSupportRequest,
    ): Response<APIResponse<String>>

    @POST("/spice-service/static-data/meta-data/tb")
    suspend fun getTbStaticData(): Response<APIResponse<TbMetaResponse>>

    @POST("/spice-service/medical-review/height/create")
    suspend fun createHeight(
        @Body request: BpAndWeightRequestModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/height")
    suspend fun fetchHeight(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<BpAndWeightResponse>>

    @POST("/spice-service/medical-review/bmi")
    suspend fun fetchBmi(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<BpAndWeightResponse>>

    @POST("/spice-service/medical-review/bmi-history")
    suspend fun fetchList(
        @Body motherNeonateAncRequest: MotherNeonateAncRequest,
    ): Response<APIResponse<List<BpAndWeightResponse>>>

    @POST("/spice-service/medical-review/tb/details")
    suspend fun fetchTbAssessmentDetails(
        @Body request: MotherNeonateAncRequest,
    ): Response<APIResponse<TbHistory>>

    @POST("/spice-service/medical-review/tb/create")
    suspend fun saveTbMedicalReview(
        @Body request: TbMedicalReviewCreateRequest,
    ): Response<APIResponse<PatientEncounterResponse>>

    @POST("/spice-service/medical-review/patient-type/create")
    suspend fun createPatientType(
        @Body request: PatientTypeCreateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/patient-type")
    suspend fun getPatientType(
        @Body request: MotherNeonateAncRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/birth-details")
    suspend fun getBirthDetails(
        @Body request: RequestBirthDetails,
    ): Response<APIResponse<BirthDetails>>

    @POST("spice-service/static-data/meta-data/hiv")
    suspend fun getHIVStaticData(): Response<APIResponse<HivMetaResponse>>

    @POST("/spice-service/hiv/screening/create")
    suspend fun createHivScreening(
        @Body request: HivScreeningRequest,
    ): Response<APIResponse<HivScreeningResponse>>

    @POST("/spice-service/hiv/screening/detail")
    suspend fun getHivScreeningDetails(
        @Body request: HivScreeningResponse,
    ): Response<APIResponse<HivCreateScreeningSummaryResponse>>

    @POST("/notification-service/inapp-notification/list")
    suspend fun getCBSNotificationDetails(
        @Body peerSupervisorNotificationRequest: PeerSupervisorNotificationRequest,
    ): Response<APIResponse<ArrayList<PeerSupervisorNotificationResponse>>>

    @POST(" /notification-service/inapp-notification/clear")
    suspend fun updateCBSNotification(
        @Body peerSupervisorNotificationRequest: PeerSupervisorNotificationRequest,
    ): Response<APIResponse<Unit>>

    @POST("/spice-service/medical-review/bmi/create")
    suspend fun createBMI(
        @Body bpAndWeightRequestModel: BpAndWeightRequestModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/family-planning/create")
    suspend fun createFamilyPlanningMR(
        @Body request: FamilyPlanningContraceptivesRequest,
    ): Response<APIResponse<FamilyPlanningCreateResponse>>

    @POST("/spice-service/static-data/meta-data/family-planning")
    suspend fun getFamilyPlanningStaticData(): Response<APIResponse<FamilyPlanningMetaResponse>>

    @POST("/spice-service/medical-review/family-planning/details")
    suspend fun getFamilyPlanningMRSummaryDetails(
        @Body id: AboveFiveYearsSummaryRequest,
    ): Response<APIResponse<FamilyPlanningSummaryResponse>>

    @POST("/spice-service/hiv/medical-review/create")
    suspend fun createHivImrCmr(
        @Body request: HivRequestData,
    ): Response<APIResponse<PatientEncounterResponse>>

    @POST("/spice-service/hiv/medical-review/details")
    suspend fun fetchHivSummaryDetails(
        @Body request: MotherNeonateAncRequest,
    ): Response<APIResponse<HivSummaryResponse>>

    @POST("/spice-service/hiv/opportunistic-infection-details")
    suspend fun getOpportunisticInfection(
        @Body request: MotherNeonateAncRequest,
    ): Response<APIResponse<HashMap<String, HashMap<String, String>?>>>

    @POST("/spice-service/medical-review/emtct-visit/create")
    suspend fun createEmtct(
        @Body eMTCTVisitStatusRequest: EMTCTVisitStatusRequest,
    ): Response<APIResponse<EMTCTVisitStatusResponse>>

    @POST("/spice-service/medical-review/vital-details")
    suspend fun getHivVitalsDetails(
        @Body hivVitalsRequest: HivVitalsRequest,
    ): Response<APIResponse<HivVitalsResponse>>

    @POST("/spice-service/medical-review/who-clinical-stage/create")
    suspend fun createWhoClinicalStage(
        @Body request: WhoClinicalStageCreateRequest,
    ): Response<APIResponse<HivClinicalInfoResponse>>

    @POST("/spice-service/hiv/cd4-details")
    suspend fun getHivCD4Details(
        @Body request: CD4DetailsRequest,
    ): Response<APIResponse<ArrayList<CD4DetailsResponse>>>

    @POST("/spice-service/hiv/viral-load/recommendation")
    suspend fun checkRecommendationInvestigations(
        @Body request: MotherNeonateAncRequest,
    ): Response<APIResponse<HashMap<String, Boolean?>?>>

    @POST("/spice-service/hiv/viral-load/list")
    suspend fun getViralLoadData(
        @Body request: ViralLoadRequest,
    ): Response<APIResponse<List<ViralLoadResponse>>>

    @POST("/spice-service/prescription-request/regimen-list")
    suspend fun getARTData(
        @Body request: ArtRequest,
    ): Response<APIResponse<List<ARTResponse>>>

    @POST("/spice-service/patient/pregnancy-details")
    suspend fun getPatientSummaryDetails(
        @Body request: PregnancySummaryRequest,
    ): Response<APIResponse<PregnancyDetailsModel>>

    @POST("/spice-service/patient/search")
    suspend fun searchPatientById(
        @Body request: PatientDataModel,
    ): APIResponse<ArrayList<PatientListResModel>>

    @POST("/spice-service/patient/member-details")
    suspend fun getMemberDetails(
        @Body request: RequestMemberDetails,
    ): Response<ResponseBody>

    @POST("/spice-service/patient/followup-list")
    suspend fun patientFollowUpList(
        @Body request: PatientDataModel,
    ): APIResponse<ArrayList<PatientListResModel>>

    @POST("/spice-service/patient/list")
    suspend fun patientList(
        @Body request: PatientDataModel,
    ): APIResponse<ArrayList<PatientListResModel>>

    @POST("/spice-service/patient/validate-duplicate-nudge")
    suspend fun validatePatient(
        @Body request: JsonObject,
    ): Response<APIResponse<Boolean>>

    @POST("/spice-service/patient/register")
    suspend fun createPatient(
        @Body request: JsonObject,
    ): Response<APIResponse<PatientCreateResponse>>

    @POST("/spice-service/patient/validate-qr-code")
    suspend fun validateQRCodeValidation(
        @Body qrCodeRequest: QRCodeRequest,
    ): Response<QRCodeResponse>

    @POST("/spice-service/patientvisit/create")
    suspend fun createPatientVisit(
        @Body request: MedicalReviewBaseRequest,
    ): Response<APIResponse<PatientVisit>>

    @POST("/spice-service/screeninglog/create")
    suspend fun createScreeningLog(
        @Body createRequest: JsonObject,
    ): Response<ScreeningPatientResponse>

    @GET("/user-service/user/validate")
    suspend fun validateSession(): Response<ResponseBody>

    @POST("/spice-service/patient/patientDetails")
    suspend fun getPatientDetails(
        @Body request: PatientDetailRequest,
    ): Response<APIResponse<PatientDetailsModel>>

    @POST("/spice-service/bplog/list")
    suspend fun getPatientBPLogList(
        @Body request: ReqBPBGLogList,
    ): Response<APIResponse<BPLogListResponse>>

    @POST("/spice-service/glucoselog/list")
    suspend fun getPatientBloodGlucoseList(
        @Body request: ReqBPBGLogList,
    ): Response<APIResponse<BloodGlucoseListResponse>>

    @POST("/spice-service/para-counselling/graph-details")
    suspend fun getSessionGraph(
        @Body request: AssessmentListRequest,
    ): Response<APIResponse<SessionGraphModel>>

    @POST("/admin-service/medication/search")
    suspend fun searchMedication(
        @Body request: MedicationSearchReqModel,
    ): Response<APIResponse<ArrayList<PrescriptionModel>>>

    @POST("/spice-service/patient/pregnancy-details/create")
    suspend fun createPregnancy(
        @Body request: PregnancyCreateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/pregnancy-details/update")
    suspend fun updatePregnancy(
        @Body request: PregnancyCreateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/pregnancy-details/info")
    suspend fun getPatientPregnancyDetails(
        @Body request: PatientPregnancyModel,
    ): Response<APIResponse<PregnancyCreateRequest>>

    @PATCH("/spice-service/patient/confirm-diagnosis/update")
    suspend fun confirmDiagnosis(
        @Body request: ConfirmDiagnosesRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/details")
    suspend fun getMentalHealthDetails(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/summary")
    suspend fun getPatientMedicalReviewSummary(
        @Body request: MedicalReviewBaseRequest,
    ): Response<APIResponse<SummaryResponse>>

    @PUT("/spice-service/patient-treatment-plan/update")
    suspend fun updateTreatmentPlan(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-treatment-plan/details")
    suspend fun treatmentPlanDetails(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/get-by-id")
    suspend fun getScreeningDetails(
        @Body request: RequestPatientDetail,
    ): Response<APIResponse<ResponsePatientDetail>>

    @POST("/spice-service/assessment/bplog-create")
    suspend fun createBpLog(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/assessment/glucoselog-create")
    suspend fun createGlucoseLog(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/remove")
    suspend fun patientRemove(
        @Body request: PatientRemoveRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/red-risk/update")
    suspend fun clearRedRisk(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @GET("/spice-service/medical-review/get-instructions")
    suspend fun getInstructions(): Response<APIResponse<InstructionModel>>

    @PATCH("/spice-service/patient/pregnancy-anc-risk/update")
    suspend fun updatePregnancyRisk(
        @Body request: PregnancyRiskUpdate,
    ): Response<APIResponse<Boolean>>

    @GET("/spice-service/para-counselling/get-subjective-questions")
    suspend fun getSessionQuestions(
        @Query("session") session: Int,
        @Query("age") age: Int,
    ): Response<APIResponse<ArrayList<SessionModelResponse>>>

    @POST("/spice-service/patient/family-other-details")
    suspend fun getPatientFamilyOtherDetails(
        @Body data: Lifestyle,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/para-counselling/session-terminate")
    suspend fun postSessionTerminate(
        @Body request: TerminateSessionModel,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/admin-service/medication/recommendation")
    suspend fun medicationList(
        @Body request: MedicationSearchReqModel,
    ): Response<APIResponse<ArrayList<PrescriptionModel>>>

    @POST("/spice-service/medical-review/create")
    suspend fun createSingleWindowMR(
        @Body request: SingleWindowModel,
    ): Response<APIResponse<InitialEncounterResponse>>

    @POST("/spice-service/medical-review/count")
    suspend fun getBadgeCount(
        @Body request: BadgeModel,
    ): Response<APIResponse<BadgeResponseModel>>

    @POST("/admin-service/site/country/site-list")
    suspend fun searchSite(
        @Body request: RegionSiteModel,
    ): Response<APIResponse<ArrayList<RegionSiteResponse>>>

    @POST("/user-service/user/role-user-list")
    suspend fun searchRoleUser(
        @Body request: SiteRoleModel,
    ): Response<APIResponse<ArrayList<SiteRoleResponse>>>

    @POST("/spice-service/patient-transfer/create")
    suspend fun createPatientTransfer(
        @Body request: TransferCreateRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-transfer/validate")
    suspend fun validatePatientTransfer(
        @Body request: FillPrescriptionRequest,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/para-counselling/session-create")
    suspend fun sessionCreate(
        @Body request: SessionEncounterRequest,
    ): Response<APIResponse<InitialEncounterResponse>>

    @POST("/spice-service/medical-review/create")
    suspend fun createNurseMedicalReview(
        @Body request: NurseMrRequestModel,
    ): Response<APIResponse<NurseCreateResponse>>

    @POST("/spice-service/prescription/list")
    suspend fun getNursePrescriptionList(
        @Body request: PatientPrescriptionModel,
    ): Response<APIResponse<ArrayList<PrescriptionModel>>>

    @PUT("/spice-service/prescription/remove")
    suspend fun removePrescription(
        @Body request: PatientPrescriptionModel,
    ): Response<APIResponse<ResponseDataModel>>

    @POST("/spice-service/prescription/update")
    suspend fun updatePrescription(
        @Body body: RequestBody,
    ): Response<APIResponse<ResponseDataModel>>

    @POST("/spice-service/prescription-history/list")
    suspend fun getPatientPrescriptionHistoryList(
        @Body request: PatientHistoryRequest,
    ): Response<APIResponse<PatientPrescriptionHistoryResponse>>

    @POST("/spice-service/prescription/prediction")
    suspend fun prescriptionPrediction(
        @Body map: HashMap<String, Any>,
    ): Response<APIResponse<PrescriptionPredictionResponse>>

    @POST("/spice-service/patient-labtest/list")
    suspend fun getPatientLabTestHistory(
        @Body request: PatientHistoryRequest,
    ): Response<APIResponse<PatientLabTestHistoryResponse>>

    @POST("/spice-service/medical-review/list")
    suspend fun getPatientMedicalReviewHistoryList(
        @Body request: MedicalReviewBaseRequest,
    ): Response<APIResponse<PatientMedicalReviewHistoryResponse>>

    @POST("/spice-service/para-counselling/history")
    suspend fun getPatientSessionHistoryList(
        @Body request: MedicalReviewBaseRequest,
    ): Response<APIResponse<SessionHistoryResponse>>

    @POST("/admin-service/lab-test-customization/recommendation")
    suspend fun getPatientLabTestRecommendation(
        @Body countryId: InvestigationReq,
    ): Response<APIResponse<List<ResLabTestRecommendations>>>

    @POST("/spice-service/patient-labtest/list")
    suspend fun getPatientLabTests(
        @Body request: HashMap<String, Any?>,
    ): Response<APIResponse<org.medtroniclabs.uhis.data.registration.LabTestListResponse>>

    @POST("/spice-service/patient-labtest/search")
    suspend fun searchLabTest(
        @Body request: SearchModel,
    ): Response<APIResponse<ArrayList<LabTestSearchResponse>>>

    @GET("/spice-service/patient-labtest/result/list/{labTestId}")
    suspend fun getLabTestResult(
        @Path("labTestId") labTestId: Long,
    ): Response<APIResponse<ArrayList<HashMap<String, Any>>>>

    @POST("/spice-service/patient-labtest/prediction")
    suspend fun getNudgesList(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<InvestigationNudgesModel>>

    @POST("/spice-service/patient-labtest/create")
    suspend fun referLabTest(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-labtest/result/create")
    suspend fun createLabTestResult(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-labtest/result/details")
    suspend fun getLabTestResultDetails(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-labtest/remove")
    suspend fun removeLabTest(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-labtest/review")
    suspend fun reviewLabTestResult(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<HashMap<String, Any>>>

    @PUT("/spice-service/medical-review/update-prescribed-site")
    suspend fun updatePatientType(
        @Body request: HashMap<String, Any>,
    ): Response<APIResponse<Boolean>>

    @POST("/spice-service/fill-prescription/update")
    suspend fun fillPrescriptionUpdate(
        @Body request: FillPrescriptionUpdateRequest,
    ): Response<APIResponse<ArrayList<FillMedicineResponse>>>

    @POST("/spice-service/fill-prescription/list")
    suspend fun getPatientFillPrescriptionList(
        @Body request: FillPrescriptionRequest,
    ): Response<APIResponse<ArrayList<FillPrescriptionListResponse>>>

    @POST("/spice-service/prescription/refill-history")
    suspend fun getPrescriptionRefillHistory(
        @Body request: org.medtroniclabs.uhis.data.registration.PatientPrescriptionModel,
    ): Response<APIResponse<ArrayList<PrescriptionRefillHistoryResponse>>>

    @POST("/spice-service/screening/dashboard-count")
    suspend fun getProviderDashboardDetails(
        @Body request: UserDashboardRequest,
    ): Response<APIResponse<UserDashboardResponse>>

    @POST("/spice-service/tele-support/patient-record")
    suspend fun getTCPatientRecord(
        @Body request: PatientDetailsFollowUp,
    ): Response<APIResponse<TCPatientDetailsResponse>>

    @POST("spice-service/call-register")
    suspend fun callRegister(
        @Body request: RegisterCallRequest,
    ): Response<APIResponse<ModelRegisterCallResponse>>

    @GET("spice-service/call-register")
    suspend fun getCallRegister(): Response<APIResponse<ModelRegisterCallResponse>>

    @PUT("spice-service/call-register")
    suspend fun updateStatusCallRegister(
        @Body request: UpdatePatientCallRegister,
    ): Response<APIResponse<UpdatePatientCallRegister>>
}
