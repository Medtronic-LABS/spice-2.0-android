package com.medtroniclabs.opensource.network

import com.google.gson.JsonObject
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.AboveFiveYearsMetaResponse
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryDetails
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryRequest
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
import com.medtroniclabs.opensource.data.MotherPncResponse
import com.medtroniclabs.opensource.data.NCDUserDashboardRequest
import com.medtroniclabs.opensource.data.NCDUserDashboardResponse
import com.medtroniclabs.opensource.data.NeonatePncResponse
import com.medtroniclabs.opensource.data.PatientStatusRequest
import com.medtroniclabs.opensource.data.PatientStatusResponse
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.PrescriptionListRequest
import com.medtroniclabs.opensource.data.ReferPatientAPIRequest
import com.medtroniclabs.opensource.data.ReferPatientHealthFacilityItem
import com.medtroniclabs.opensource.data.ReferPatientNameNumber
import com.medtroniclabs.opensource.data.ReferPatientRequest
import com.medtroniclabs.opensource.data.ReferPatientResult
import com.medtroniclabs.opensource.data.RemovePrescriptionRequest
import com.medtroniclabs.opensource.data.UnderFiveYearsMetaResponse
import com.medtroniclabs.opensource.data.UnderTwoMonthsMetaResponse
import com.medtroniclabs.opensource.data.UserSymptomsEntity
import com.medtroniclabs.opensource.data.history.HistoryEntity
import com.medtroniclabs.opensource.data.history.MedicalReviewHistory
import com.medtroniclabs.opensource.data.model.AboveFiveYearsSubmitRequest
import com.medtroniclabs.opensource.data.model.BpAndWeightRequestModel
import com.medtroniclabs.opensource.data.model.BpAndWeightResponse
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryRequest
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryResponse
import com.medtroniclabs.opensource.data.model.LabourDeliverySummaryDetails
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.data.model.MotherNeonatePncRequest
import com.medtroniclabs.opensource.data.model.PatientEncounterResponse
import com.medtroniclabs.opensource.data.model.PncSubmitResponse
import com.medtroniclabs.opensource.data.model.RequestChangePassword
import com.medtroniclabs.opensource.data.model.ResponseChangePassword
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHold
import com.medtroniclabs.opensource.data.offlinesync.model.RequestGetSyncStatus
import com.medtroniclabs.opensource.data.offlinesync.model.ResponseSignatureUpload
import com.medtroniclabs.opensource.data.offlinesync.model.SyncResponse
import com.medtroniclabs.opensource.data.BirthHistoryRequest
import com.medtroniclabs.opensource.data.BirthHistoryResponse
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryRequest
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryResponse
import com.medtroniclabs.opensource.data.SummaryCreateRequest
import com.medtroniclabs.opensource.data.PncChildMedicalReview
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.data.history.NCDMedicalReviewHistory
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountRequest
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountResponse
import com.medtroniclabs.opensource.data.model.RegistrationResponse
import com.medtroniclabs.opensource.data.resource.LabourDeliverySummaryRequest
import com.medtroniclabs.opensource.data.performance.CHWPerformanceMonitoring
import com.medtroniclabs.opensource.data.performance.ChwVillageFilterModel
import com.medtroniclabs.opensource.data.performance.FilterPreference
import com.medtroniclabs.opensource.data.performance.PerformanceReportRequest
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
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreResponse
import com.medtroniclabs.opensource.ncd.data.DeviceDetails
import com.medtroniclabs.opensource.ncd.data.FollowUpRequest
import com.medtroniclabs.opensource.ncd.data.FollowUpUpdateRequest
import com.medtroniclabs.opensource.ncd.data.NCDMentalHealthMedicalReviewDetails
import com.medtroniclabs.opensource.ncd.data.NCDMentalHealthStatusRequest
import com.medtroniclabs.opensource.ncd.data.TermsAndConditionsModel
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferValidate
import com.medtroniclabs.opensource.ncd.data.NCDTransferCreateRequest
import com.medtroniclabs.opensource.ncd.data.PatientTransferListResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferUpdateRequest
import com.medtroniclabs.opensource.ncd.data.NCDRegionSiteModel
import com.medtroniclabs.opensource.ncd.data.RegionSiteResponse
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleModel
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleResponse
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity
import com.medtroniclabs.opensource.ncd.data.PredictionRequest
import com.medtroniclabs.opensource.ncd.data.PrescriptionNudgeResponse
import com.medtroniclabs.opensource.ncd.data.RegisterCallResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("/auth-service/session")
    suspend fun doLogin(@Body request: RequestBody): Response<LoginResponse>

    @POST("/spice-service/static-data/user-data")
    suspend fun getMetaDataInformation(): Response<APIResponse<MetaDataResponse>>

    @POST("/spice-service/static-data/form-data")
    suspend fun getForms(@Body formRequest: FormRequest): Response<APIResponse<FormResponse>>

    @POST("/spice-service/static-data/meta-data")
    suspend fun getFormMetadata(@Body request: FormMetaRequest): Response<APIResponse<UserSymptomsEntity>>

    @POST("/offline-service/offline-sync/create")
    @JvmSuppressWildcards
    suspend fun postOfflineSyncData(@Body map: Map<String, Any>): Response<SyncResponse>

    @POST("/offline-service/offline-sync/status")
    suspend fun getOfflineSyncStatus(@Body request: RequestGetSyncStatus): Response<SyncResponse>

    @POST("/spice-service/household/list")
    suspend fun getHouseholdDetails(@Body request: RequestAllEntities): Response<APIResponse<List<HouseHold>>>

    @POST("/offline-service/offline-sync/fetch-synced-data")
    suspend fun fetchSyncedData(@Body request: RequestAllEntities): Response<ResponseBody>

    @POST("spice-service/patient/list")
    suspend fun getPatients(@Body request: PatientsDataModel): APIResponse<SearchAndListResponse>

    @POST("spice-service/patient/search")
    suspend fun patientSearch(@Body request: PatientsDataModel): APIResponse<SearchAndListResponse>

    @POST("spice-service/patient/patientDetails")
    suspend fun getPatient(@Body request: PatientDetailRequest): Response<APIResponse<PatientListRespModel>>

    @POST("/spice-service/static-data/meta-data/iccm-abovefive")
    suspend fun getAboveFiveYearsMetaData(): Response<APIResponse<AboveFiveYearsMetaResponse>>

    @POST("/spice-service/static-data/meta-data/iccm-under-five-years")
    suspend fun getUnderFiveYearsMetaData(): Response<APIResponse<UnderFiveYearsMetaResponse>>

    @POST("/spice-service/patient/referral-tickets")
    suspend fun getReferralsDetails(@Body request: ReferralDetailRequest): Response<APIResponse<ReferralData>>

    @POST("/spice-service/medical-review/iccm-general/create")
    suspend fun createAboveFiveYearsResult(@Body request: AboveFiveYearsSubmitRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>>

    @POST("/spice-service/medical-review/iccm-general/details")
    suspend fun getAboveFiveYearsSummaryDetails(@Body id: AboveFiveYearsSummaryRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>>

    @POST("/spice-service/static-data/meta-data/mother-neonate-anc")
    suspend fun getMotherNeoNateAncStaticData(): Response<APIResponse<MotherNeonateAncMetaResponse>>

    @POST("/spice-service/medical-review/labour-mother-neonate/create")
    suspend fun createMedicalReviewForLaborDelivery(@Body labourDeliveryRequest: CreateLabourDeliveryRequest): Response<APIResponse<CreateLabourDeliveryResponse>>

    @POST("/spice-service/static-data/meta-data/iccm-under-two-months")
    suspend fun getUnderTwoMonthsMetaData(): Response<APIResponse<UnderTwoMonthsMetaResponse>>

    @POST("/spice-service/static-data/meta-data/mother-neonate-pnc-mother")
    suspend fun getMotherPncStaticData(): Response<APIResponse<MotherPncResponse>>

    @POST("/spice-service/static-data/meta-data/mother-neonate-pnc-baby")
    suspend fun getNeonatePncStaticData(): Response<APIResponse<NeonatePncResponse>>

    @POST("/spice-service/medical-review/summary-create")
    suspend fun createSummarySubmit(@Body request: MedicalReviewSummarySubmitRequest): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/static-data/meta-data/mother-delivery")
    suspend fun getLabourDeliveryMetaData(): Response<APIResponse<LabourDeliveryMetaResponse>>

    @POST("/spice-service/medical-review/labour-mother-neonate/details")
    suspend fun getLabourDeliverySummaryDetails(@Body request: LabourDeliverySummaryDetails): Response<APIResponse<CreateLabourDeliveryRequest>>

    @POST("/admin-service/medication/search")
    suspend fun searchMedicationByName(@Body request: MedicationSearchRequest): Response<APIResponse<ArrayList<MedicationResponse>>>

    @POST("/spice-service/medical-review/iccm-under-2months/create")
    suspend fun createMedicalReviewForUnderTwoMonths(@Body request: CreateUnderTwoMonthsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>>

    @POST("/spice-service/patient/patient-status")
    suspend fun getPatientStatus(@Body request: PatientStatusRequest): Response<APIResponse<PatientStatusResponse>>

    @POST("/spice-service/medical-review/iccm-under-2months/details")
    suspend fun getUnderTwoMonthsSummaryDetails(@Body request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>>

    @POST("/spice-service/medical-review/anc-pregnancy/create")
    suspend fun saveMotherNeonateAnc(@Body motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<PatientEncounterResponse>>

    @POST("/spice-service/medical-review/pnc/create")
    suspend fun saveMotherNeonatePnc(@Body motherNeonatePncRequest: MotherNeonatePncRequest): Response<APIResponse<PncSubmitResponse>>

    @POST("/spice-service/medical-review/anc-pregnancy/details")
    suspend fun fetchSummary(@Body motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<MotherNeonateAncSummaryModel>>

    @POST("/spice-service/medical-review/weight")
    suspend fun fetchWeight(@Body motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>>

    @POST("/spice-service/medical-review/bp")
    suspend fun fetchBloodPressure(@Body motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>>

    @POST("/spice-service/medical-review/weight/create")
    suspend fun createWeight(@Body bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/bp/create")
    suspend fun createBloodPressure(@Body bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/confirm-diagnosis")
    suspend fun saveUpdateDiagnosis(@Body request: DiagnosisSaveUpdateRequest): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>>

    @POST("/spice-service/patient/diagnosis-details")
    suspend fun getDiagnosisDetails(@Body request: CreateUnderTwoMonthsResponse): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>>

    @POST("/admin-service/healthfacilities-by-district-id")
    suspend fun getHealthFacilityMetaData(@Body request: ReferPatientAPIRequest): Response<APIResponse<List<ReferPatientHealthFacilityItem>>>

    @POST("/user-service/user/users-by-tenant-id")
    suspend fun getReferPatientMobileUserList(@Body tenantId: ReferPatientRequest): Response<APIResponse<List<ReferPatientNameNumber>>>

    @POST("/spice-service/patient/referral-tickets/create")
    suspend fun createReferPatientResult(@Body request: ReferPatientResult): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/prescription-request/create")
    suspend fun createPrescriptionRequest(@Body request: RequestBody): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/prescription-request/list")
    suspend fun getPrescriptionList(@Body request: PrescriptionListRequest): Response<APIResponse<ArrayList<Prescription>>>

    @POST("spice-service/prescription-request/remove")
    suspend fun removePrescription(@Body request: RemovePrescriptionRequest): Response<APIResponse<Map<String, Any>>>

    @POST("/spice-service/medical-review/iccm-under-5years/details")
    suspend fun getUnderFiveYearsSummaryDetails(@Body request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>>

    @POST("/spice-service/medical-review/iccm-under-5years/create")
    suspend fun createMedicalReviewForUnderFiveYears(@Body request: CreateUnderFiveYearsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>>

    @POST("/spice-service/prescription-request/prescribed-details")
    suspend fun getPrescription(@Body request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>>

    @POST("/spice-service/medical-review/history")
    suspend fun getMedicalReviewHistory(@Body request: ReferralDetailRequest): Response<APIResponse<MedicalReviewHistory>>

    @POST("/spice-service/medical-review/pnc/details")
    suspend fun getPncSummaryDetails(@Body request: MotherNeonatePncSummaryRequest): Response<APIResponse<MotherNeonatePncSummaryResponse>>

    @POST("/spice-service/medical-review/mother-neonate/summary-create")
    suspend fun summaryCreatePncData(@Body request: SummaryCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/medical-review/birth-history")
    suspend fun getBirthHistoryDetails(@Body request: BirthHistoryRequest): Response<APIResponse<BirthHistoryResponse>>

    @POST("spice-service/household/create-member")
    suspend fun addNewMember(@Body request: AddMemberRegRequest): Response<APIResponse<String>>

    @POST("/admin-service/lab-test-customization/list")
    suspend fun searchLabTestByName(@Body request: SearchRequestLabTest): Response<APIResponse<ArrayList<SearchLabTestResponse>>>

    @POST("spice-service/investigation/create")
    suspend fun createLabTest(@Body request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/investigation/result/create")
    suspend fun updateLabTest(@Body request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>>

    @POST("spice-service/investigation/list")
    suspend fun getLabTestList(@Body request: LabTestListRequest): Response<APIResponse<ArrayList<LabTestListResponse>>>

    @POST("/spice-service/investigation/remove")
    suspend fun removeLabTest(@Body request: RemoveLabTestRequest): Response<APIResponse<Map<String, Any>>>

    @POST("/spice-service/medical-review/mother-neonate/summary-create")
    suspend fun summaryCreateMotherNeonate(@Body request: LabourDeliverySummaryRequest): Response<APIResponse<HashMap<String, Any>>>
    @GET("user-service/user/peer-supervisor/linked-chw")
    suspend fun getPeerSupervisorLinkedChwList(): Response<APIResponse<List<ChwVillageFilterModel>>>

    @POST("spice-service/report/chw-performance")
    suspend fun getPeerSupervisorReport(@Body request: PerformanceReportRequest): Response<APIResponse<List<CHWPerformanceMonitoring>>>

    @POST("user-service/user/preferences")
    suspend fun getUserFilterPreference(@Body request: FilterPreference): Response<APIResponse<FilterPreference>>

    @POST("user-service/user/preferences/save")
    suspend fun saveUserFilterPreference(@Body request: FilterPreference): Response<APIResponse<FilterPreference>>


    @POST("/spice-service/medical-review/pnc-history")
    suspend fun getMedicalReviewHistoryPNC(@Body request: ReferralDetailRequest): Response<APIResponse<PncChildMedicalReview>>

    @POST("/spice-service/investigation/history-list")
    suspend fun getInvestigationHistoryList(@Body request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>>

    @POST("/user-service/user/forgot-password/{email}/{client}")
    suspend fun forgotPassword(@Path("email") email: String, @Path("client") client: String): Response<APIResponse<String?>>

    @POST("/user-service/user/verify-token/{token}")
    suspend fun verifyToken(@Path("token") token: String): Response<APIResponse<String?>>

    @POST("/user-service/user/reset-password/{token}")
    suspend fun resetPassword(@Path("token") token: String, @Body request: RequestChangePassword): Response<APIResponse<ResponseChangePassword>>

    @POST("/offline-service/offline-sync/upload-signatures")
    suspend fun uploadAllConsentSignatures(@Body request: RequestBody): Response<APIResponse<List<ResponseSignatureUpload>>>

    @POST("spice-service/static-data/app-version")
    suspend fun checkAppVersion(): Response<APIResponse<Boolean>>

    @POST("/spice-service/screening/create")
    suspend fun createScreening(@Body createRequest: RequestBody): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient/register")
    suspend fun registerPatient(@Body request: RequestBody): Response<APIResponse<RegistrationResponse>>

    @POST("spice-service/static-data/ncd-medical-review")
    suspend fun getNcdMRStaticData(): Response<APIResponse<NcdMRStaticDataModel>>

    @POST("spice-service/bplog/create")
    suspend fun bpLogCreate(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/glucoselog/create")
    suspend fun glucoseLogCreate(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/bplog/list")
    suspend fun bpLogList(@Body request: BPBGListModel): Response<APIResponse<BPBGListModel>>

    @POST("spice-service/glucoselog/list")
    suspend fun glucoseLogList(@Body request: BPBGListModel): Response<APIResponse<BPBGListModel>>

    @POST("spice-service/assessment/create")
    suspend fun createAssessmentNCD(@Body request: JsonObject): Response<HashMap<String, Any>>

    @POST("spice-service/patient/pregnancy/create")
    suspend fun ncdPregnancyCreate(@Body request: PregnancyDetailsModel): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient/pregnancy/details")
    suspend fun ncdPregnancyDetails(@Body request: HashMap<String, Any>): Response<APIResponse<PregnancyDetailsModel>>

    @POST("/spice-service/patientvisit/create")
    suspend fun createPatientVisit(@Body request: PatientVisitRequest): Response<APIResponse<PatientVisitResponse>>

    @POST("/spice-service/medical-review/ncd/create")
    suspend fun createNCDMedicalReview(@Body request: MedicalReviewRequestResponse): Response<APIResponse<MedicalReviewResponse>>

    @POST("/spice-service/medical-review/ncd/details")
    suspend fun fetchNCDMRSummary(@Body request: MedicalReviewResponse): Response<APIResponse<MRSummaryResponse>>

    @POST("spice-service/prescription-request/history-list")
    suspend fun getPatientPrescriptionHistoryList(@Body request: RemovePrescriptionRequest) : Response<APIResponse<ArrayList<Prescription>>>

    @POST("/spice-service/medical-review/confirm-diagnosis/update")
    suspend fun createConfirmDiagonsis(@Body request: NCDDiagnosisRequestResponse): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/get-diagnosis-details")
    suspend fun getConfirmDiagonsis(@Body request: NCDDiagnosisGetRequest): Response<APIResponse<NCDDiagnosisGetResponse>>

    @POST("/spice-service/medical-review/patient-status/create")
    suspend fun createNCDPatientStatus(@Body request: NCDPatientStatusRequest): Response<APIResponse<HashMap<String, Any>>>
    @POST("/spice-service/patient-treatment-plan/update")
    suspend fun updateNCDTreatmentPlan(@Body request: NCDTreatmentPlanModel): Response<APIResponse<NCDTreatmentPlanModel>>

    @POST("/spice-service/patient-treatment-plan/details")
    suspend fun getNCDTreatmentPlan(@Body request: NCDTreatmentPlanModelDetails): Response<APIResponse<NCDTreatmentPlanModelDetails>>

    @POST("/spice-service/medical-review/ncd/summary-create")
    suspend fun createNCDMRSummaryCreate(@Body request: NCDMRSummaryRequestResponse): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient-nutrition-lifestyle/create")
    suspend fun createLifestyle(@Body request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>

    @PUT("spice-service/patient-nutrition-lifestyle/update")
    suspend fun updateLifestyle(@Body request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/patient-nutrition-lifestyle/list")
    suspend fun getLifestyleList(@Body request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>>

    @POST("spice-service/patient-nutrition-lifestyle/remove")
    suspend fun removeLifestyle(@Body request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>

    @POST("spice-service/medical-review/patient-psychology/create")
    suspend fun createPsychological(@Body request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>

    @PUT("spice-service/medical-review/patient-psychology/update")
    suspend fun updatePsychological(@Body request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/medical-review/patient-psychology/list")
    suspend fun getPsychological(@Body request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>>

    @POST("spice-service/medical-review/patient-psychology/remove")
    suspend fun removePsychological(@Body request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>>

    @POST("/spice-service/prescription-request/fill-prescription/list")
    suspend fun getPrescriptionDispenseList(@Body request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>>

    @POST("/spice-service/prescription-request/fill-prescription/update")
    suspend fun updateDispensePrescription(@Body request: DispensePrescriptionRequest):  Response<APIResponse<DispenseUpdateResponse>>

    @POST("/spice-service/prescription-request/refill-prescription/history")
    suspend fun getDispensePrescriptionHistory(@Body request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>>


    @POST("/spice-service/medical-review/ncd/history-list")
    suspend fun getNCDMedicalReviewHistory(@Body request: ReferralDetailRequest): Response<APIResponse<NCDMedicalReviewHistory>>

    @POST("spice-service/patient/validate")
    suspend fun validatePatient(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @GET("/spice-service/medical-review/get-instructions")
    suspend fun ncdGetInstructions(): Response<APIResponse<NCDInstructionModel>>

    @PUT("/spice-service/patient/pregnancy-anc-risk/update")
    suspend fun ncdUpdatePregnancyRisk(@Body request: NCDPregnancyRiskUpdate): Response<APIResponse<Boolean>>

    @POST("/spice-service/patient/calculate-wgs")
    suspend fun getWazWhzScore(@Body request: WazWhzScoreRequest): Response<APIResponse<WazWhzScoreResponse>>


    @POST("/spice-service/screening/dashboard-count")
    suspend fun getUserDashboardDetails(@Body request: NCDUserDashboardRequest): Response<APIResponse<NCDUserDashboardResponse>>

    @POST("spice-service/medical-review/count")
    suspend fun getBadgeNotifications(@Body request: BadgeNotificationModel): Response<APIResponse<BadgeNotificationModel>>

    @PUT("spice-service/medical-review/update-view-status")
    suspend fun updateBadgeNotifications(@Body request: BadgeNotificationModel): Response<APIResponse<Boolean>>

    @POST("/spice-service/medical-review/patient-lifestyle-details")
    suspend fun getNcdLifeStyleDetails(@Body request: LifeStyleRequest): Response<APIResponse<ArrayList<LifeStyleResponse>>>

    @POST("/spice-service/patient/delete")
    suspend fun ncdPatientRemove(@Body request: NCDPatientRemoveRequest): Response<APIResponse<Boolean>>

    @POST("/spice-service/assessment/bp-log-create")
    suspend fun bpLogCreateForNurse(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/assessment/glucose-log-create")
    suspend fun glucoseLogCreateForNurse(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient/update")
    suspend fun ncdUpdatePatientDetail(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>
    @POST("admin-service/terms-and-conditions/details")
    suspend fun getUserTermsAndConditions(@Body request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>>

    @POST("user-service/user/terms-and-conditions/update")
    suspend fun updateTermsAndConditionsStatus(@Body request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>>

    @POST("/spice-service/medical-review/ncd/date/update")
    suspend fun ncdUpdateNextVisitDate(@Body request: NCDMedicalReviewUpdateModel): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/patient-transfer/validate")
    suspend fun validatePatientTransfer(@Body request: NCDPatientTransferValidate): Response<HashMap<String, Any>>

    @POST("/spice-service/patient-transfer/create")
    suspend fun createPatientTransfer(@Body request: NCDTransferCreateRequest): Response<APIResponse<HashMap<String, Any>>>

    @POST("/admin-service/country/healthfacility-list")
    suspend fun searchSite(@Body request: NCDRegionSiteModel): Response<APIResponse<ArrayList<RegionSiteResponse>>>

    @POST("/user-service/user/role-user-list")
    suspend fun searchRoleUser(@Body request: NCDSiteRoleModel): Response<APIResponse<ArrayList<NCDSiteRoleResponse>>>

    @POST("/spice-service/patient-transfer/list")
    suspend fun getPatientListTransfer(@Body request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<PatientTransferListResponse>>

    @POST("/spice-service/patient-transfer/notification-count")
    suspend fun patientTransferNotificationCount(@Body request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<NCDPatientTransferNotificationCountResponse>>

    @POST("/spice-service/patient-transfer/update")
    suspend fun patientTransferUpdate(@Body request: NCDPatientTransferUpdateRequest): Response<APIResponse<String>>

    @POST("/spice-service/prescription-request/prediction")
    suspend fun getNudgesList(@Body prescriptionNudgeRequest: PredictionRequest): Response<APIResponse<PrescriptionNudgeResponse>>

    @POST("/spice-service/investigation/prediction")
    suspend fun getLabTestNudgeList(@Body predictionRequest: PredictionRequest): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/follow-up/ncd/list")
    suspend fun ncdFollowUpList(@Body request: FollowUpRequest): APIResponse<List<PatientFollowUpEntity>>

    @GET("/spice-service/follow-up")
    suspend fun getPatientCallRegister(): Response<APIResponse<RegisterCallResponse>>

    @POST("/spice-service/follow-up/ncd/update")
    suspend fun updatePatientCallRegister(@Body request: FollowUpUpdateRequest): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/devicedetails")
    suspend fun updateDeviceDetails(@Body request: DeviceDetails): Response<APIResponse<DeviceDetails>>

    @POST("/spice-service/medical-review/patient-status/create")
    suspend fun createMentalHealthStatus(@Body request: NCDMentalHealthStatusRequest): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/medical-review/patient-status/details")
    suspend fun ncdPatientDiagnosisStatus(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/create")
    suspend fun ncdMentalHealthMedicalReviewCreateA(@Body request: JsonObject): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/condition-create")
    suspend fun ncdMentalHealthMedicalReviewCreateS(@Body request: JsonObject): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/details")
    suspend fun ncdMentalHealthMedicalReviewDetailsA(@Body request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>>

    @POST("/spice-service/mentalhealth/condition-details")
    suspend fun ncdMentalHealthMedicalReviewDetailsS(@Body request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>>

    @POST("spice-service/investigation/review")
    suspend fun markAsReviewed(@Body request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>>

    @POST("/user-service/user/update-culture")
    suspend fun cultureLocaleUpdate(@Body request: CultureLocaleModel): Response<APIResponse<HashMap<String, Any>>>
}