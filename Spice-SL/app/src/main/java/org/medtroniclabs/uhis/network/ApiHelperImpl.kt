package org.medtroniclabs.uhis.network

import android.content.Context
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context,
) : ApiHelper {
    override suspend fun doLogin(loginRequest: MultipartBody): Response<LoginResponse> = apiService.doLogin(loginRequest)

    override suspend fun getMetaDataInformation(): Response<APIResponse<MetaDataResponse>> {
        // Mock data - return JSON from assets
//        val jsonString = CommonUtils.getStringFromAssets("mock_static_data.json", context.assets)
//        val type: Type = object : TypeToken<APIResponse<MetaDataResponse>>() {}.type
//        val mockResponse: APIResponse<MetaDataResponse> = Gson().fromJson(jsonString, type)
//        return Response.success(mockResponse)

        // Uncomment below line when backend is ready to use actual API
        return apiService.getMetaDataInformation()
    }

    override suspend fun getForms(formRequest: FormRequest): Response<APIResponse<FormResponse>> = apiService.getForms(formRequest)

    override suspend fun getFormMetadata(request: FormMetaRequest): Response<APIResponse<UserSymptomsEntity>> = apiService.getFormMetadata(request)

    override suspend fun getPatients(request: PatientsDataModel): APIResponse<SearchAndListResponse> = apiService.getPatients(request)

    override suspend fun postOfflineSync(request: Map<String, Any>): Response<SyncResponse> = apiService.postOfflineSyncData(request)

    override suspend fun getOfflineSyncStatus(request: RequestGetSyncStatus): Response<SyncResponse> = apiService.getOfflineSyncStatus(request)

    override suspend fun fetchSyncedData(request: RequestAllEntities): Response<ResponseBody> = apiService.fetchSyncedData(request)

    override suspend fun fetchMemberAssessmentHistory(request: RequestAllEntities): Response<List<MemberAssessmentHistoryEntity>> =
        apiService.fetchMemberAssessmentHistory(request)

    override suspend fun getMemberDetails(request: RequestMemberDetails): Response<ResponseBody> = apiService.getMemberDetails(request)

    override suspend fun getHouseholdAndMembers(request: RequestAllEntities): Response<APIResponse<List<HouseHold>>> = apiService.getHouseholdDetails(request)

    override suspend fun patientSearch(request: PatientsDataModel): APIResponse<SearchAndListResponse> = apiService.patientSearch(request)

    override suspend fun getPatient(request: PatientDetailRequest): Response<APIResponse<PatientListRespModel>> = apiService.getPatient(request)

    override suspend fun getAboveFiveYearsMetaData(): Response<APIResponse<AboveFiveYearsMetaResponse>> = apiService.getAboveFiveYearsMetaData()

    override suspend fun getReferralsDetails(request: ReferralDetailRequest): Response<APIResponse<ReferralData>> = apiService.getReferralsDetails(request)

    override suspend fun createAboveFiveYearsResult(request: AboveFiveYearsSubmitRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>> =
        apiService.createAboveFiveYearsResult(request)

    override suspend fun getAboveFiveYearsSummaryDetails(patientId: AboveFiveYearsSummaryRequest): Response<APIResponse<AboveFiveYearsSummaryDetails>> =
        apiService.getAboveFiveYearsSummaryDetails(patientId)

    override suspend fun getMotherNeoNateAncStaticData(): Response<APIResponse<MotherNeonateAncMetaResponse>> = apiService.getMotherNeoNateAncStaticData()

    override suspend fun getMotherPncStaticData(): Response<APIResponse<MotherPncResponse>> = apiService.getMotherPncStaticData()

    override suspend fun getNeonatePncStaticData(): Response<APIResponse<NeonatePncResponse>> = apiService.getNeonatePncStaticData()

    override suspend fun getUnderTwoMonthsMetaData(): Response<APIResponse<UnderTwoMonthsMetaResponse>> = apiService.getUnderTwoMonthsMetaData()

    override suspend fun createSummarySubmit(request: MedicalReviewSummarySubmitRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createSummarySubmit(request)

    override suspend fun getLabourDeliveryMetaData(): Response<APIResponse<LabourDeliveryMetaResponse>> = apiService.getLabourDeliveryMetaData()

    override suspend fun getLabourDeliverySummaryDetails(request: LabourDeliverySummaryDetails): Response<APIResponse<CreateLabourDeliveryRequest>> =
        apiService.getLabourDeliverySummaryDetails(request)

    override suspend fun getPatientStatus(request: PatientStatusRequest): Response<APIResponse<PatientStatusResponse>> = apiService.getPatientStatus(request)

    override suspend fun searchMedicationByName(request: MedicationSearchRequest): Response<APIResponse<ArrayList<MedicationResponse>>> =
        apiService.searchMedicationByName(request)

    override suspend fun searchMedicationGroupByName(request: MedicationGroupSearchRequest): Response<APIResponse<ArrayList<MedicationResponse>>> =
        apiService.searchMedicationGroupByName(request)

    override suspend fun createMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>> =
        apiService.createMedicalReviewForUnderTwoMonths(request)

    override suspend fun getMedicalReviewForUnderTwoMonths(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>> =
        apiService.getUnderTwoMonthsSummaryDetails(request)

    override suspend fun saveMotherNeonateAnc(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<PatientEncounterResponse>> =
        apiService.saveMotherNeonateAnc(motherNeonateAncRequest)

    override suspend fun saveMotherNeonatePnc(motherNeonatePncRequest: MotherNeonatePncRequest): Response<APIResponse<PncSubmitResponse>> =
        apiService.saveMotherNeonatePnc(motherNeonatePncRequest)

    override suspend fun fetchSummary(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<MotherNeonateAncSummaryModel>> =
        apiService.fetchSummary(motherNeonateAncRequest)

    override suspend fun fetchWeight(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>> =
        apiService.fetchWeight(motherNeonateAncRequest)

    override suspend fun fetchBloodPressure(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>> =
        apiService.fetchBloodPressure(motherNeonateAncRequest)

    override suspend fun createWeight(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createWeight(bpAndWeightRequestModel)

    override suspend fun createBloodPressure(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createBloodPressure(bpAndWeightRequestModel)

    override suspend fun saveUpdateDiagnosis(request: DiagnosisSaveUpdateRequest): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>> =
        apiService.saveUpdateDiagnosis(request)

    override suspend fun getDiagnosisDetails(request: CreateUnderTwoMonthsResponse): Response<APIResponse<ArrayList<DiagnosisDiseaseModel>>> =
        apiService.getDiagnosisDetails(request)

    override suspend fun getHealthFacilityMetaData(request: ReferPatientAPIRequest): Response<APIResponse<List<ReferPatientHealthFacilityItem>>> =
        apiService.getHealthFacilityMetaData(request)

    override suspend fun getReferPatientMobileUserList(tenantId: ReferPatientRequest): Response<APIResponse<List<ReferPatientNameNumber>>> =
        apiService.getReferPatientMobileUserList(tenantId)

    override suspend fun createReferPatientResult(request: ReferPatientResult): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createReferPatientResult(request)

    override suspend fun createPrescriptionRequest(request: RequestBody): Response<APIResponse<Map<String, Any>>> =
        apiService.createPrescriptionRequest(request)

    override suspend fun getPrescriptionList(request: PrescriptionListRequest): Response<APIResponse<ArrayList<Prescription>>> =
        apiService.getPrescriptionList(request)

    override suspend fun removePrescription(request: RemovePrescriptionRequest): Response<APIResponse<Map<String, Any>>> =
        apiService.removePrescription(request)

    override suspend fun removePrescription(request: PatientPrescriptionModel): Response<APIResponse<ResponseDataModel>> =
        apiService.removePrescription(request)

    override suspend fun removeCommunityPrescription(request: List<RemovePrescriptionRequest>): Response<APIResponse<Map<String, Any>>> =
        apiService.removeCommunityPrescription(request)

    override suspend fun getUnderFiveYearsMetaData(): Response<APIResponse<UnderFiveYearsMetaResponse>> = apiService.getUnderFiveYearsMetaData()

    override suspend fun createMedicalReviewForUnderFiveYears(request: CreateUnderFiveYearsRequest): Response<APIResponse<CreateUnderTwoMonthsResponse>> =
        apiService.createMedicalReviewForUnderFiveYears(request)

    override suspend fun getUnderFiveYearsSummaryDetails(request: CreateUnderTwoMonthsResponse): Response<APIResponse<SummaryDetails>> =
        apiService.getUnderFiveYearsSummaryDetails(request)

    override suspend fun getPrescription(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>> = apiService.getPrescription(request)

    override suspend fun getMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<MedicalReviewHistory>> =
        apiService.getMedicalReviewHistory(request)

    override suspend fun getMedicalReviewHistoryPNC(request: ReferralDetailRequest): Response<APIResponse<PncChildMedicalReview>> =
        apiService.getMedicalReviewHistoryPNC(request)

    override suspend fun getPncSummaryDetails(request: MotherNeonatePncSummaryRequest): Response<APIResponse<MotherNeonatePncSummaryResponse>> =
        apiService.getPncSummaryDetails(request)

    override suspend fun summaryCreatePncData(summaryCreateRequest: SummaryCreateRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.summaryCreatePncData(summaryCreateRequest)

    override suspend fun getBirthHistoryDetails(request: BirthHistoryRequest): Response<APIResponse<BirthHistoryResponse>> =
        apiService.getBirthHistoryDetails(request)

    override suspend fun createMedicalReviewLabourDelivery(request: CreateLabourDeliveryRequest): Response<APIResponse<CreateLabourDeliveryResponse>> =
        apiService.createMedicalReviewForLaborDelivery(request)

    override suspend fun addNewMember(request: AddMemberRegRequest): Response<APIResponse<String>> = apiService.addNewMember(request)

    override suspend fun searchLabTestByName(request: SearchRequestLabTest): Response<APIResponse<ArrayList<SearchLabTestResponse>>> =
        apiService.searchLabTestByName(request)

    override suspend fun createLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>> = apiService.createLabTest(request)

    override suspend fun updateLabTest(request: LabTestCreateRequest): Response<APIResponse<Map<String, Any>>> = apiService.updateLabTest(request)

    override suspend fun getLabTestList(request: LabTestListRequest): Response<APIResponse<ArrayList<LabTestListResponse>>> = apiService.getLabTestList(request)

    override suspend fun removeLabTest(request: RemoveLabTestRequest): Response<APIResponse<Map<String, Any>>> = apiService.removeLabTest(request)

    override suspend fun createSummaryMotherNeonate(request: LabourDeliverySummaryRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.summaryCreateMotherNeonate(request)

    override suspend fun getPeerSupervisorLinkedChwList(): Response<APIResponse<List<ChwVillageFilterModel>>> = apiService.getPeerSupervisorLinkedChwList()

    override suspend fun getPeerSupervisorReport(request: PerformanceReportRequest): Response<APIResponse<List<CHWPerformanceMonitoring>>> =
        apiService.getPeerSupervisorReport(request)

    override suspend fun getUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>> =
        apiService.getUserFilterPreference(request)

    override suspend fun saveUserFilterPreference(request: FilterPreference): Response<APIResponse<FilterPreference>> =
        apiService.saveUserFilterPreference(request)

    override suspend fun getInvestigation(request: ReferralDetailRequest): Response<APIResponse<HistoryEntity>> =
        apiService.getInvestigationHistoryList(request)

    override suspend fun forgotPassword(
        email: String,
        clientConstant: String,
    ): Response<APIResponse<String?>> = apiService.forgotPassword(email, clientConstant)

    override suspend fun verifyToken(token: String): Response<APIResponse<String?>> = apiService.verifyToken(token)

    override suspend fun resetPassword(
        token: String,
        request: RequestChangePassword,
    ): Response<APIResponse<ResponseChangePassword>> = apiService.resetPassword(token, request)

    override suspend fun uploadAllConsentSignatures(request: RequestBody): Response<APIResponse<List<ResponseSignatureUpload>>> =
        apiService.uploadAllConsentSignatures(request)

    override suspend fun checkAppVersion(): Response<APIResponse<Boolean>> = apiService.checkAppVersion()

    override suspend fun registerPatient(hashMap: RequestBody): Response<APIResponse<RegistrationResponse>> = apiService.registerPatient(hashMap)

    override suspend fun createScreening(createRequest: RequestBody): Response<APIResponse<HashMap<String, Any>>> = apiService.createScreening(createRequest)

    override suspend fun getNcdMRStaticData(): Response<APIResponse<NcdMRStaticDataModel>> = apiService.getNcdMRStaticData()

    override suspend fun bpLogCreate(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.bpLogCreate(request)

    override suspend fun glucoseLogCreate(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.glucoseLogCreate(request)

    override suspend fun bpLogList(request: BPBGListModel): Response<APIResponse<BPBGListModel>> = apiService.bpLogList(request)

    override suspend fun glucoseLogList(request: BPBGListModel): Response<APIResponse<BPBGListModel>> = apiService.glucoseLogList(request)

    override suspend fun createAssessmentNCD(request: JsonObject): Response<HashMap<String, Any>> = apiService.createAssessmentNCD(request)

    override suspend fun ncdPregnancyCreate(request: PregnancyDetailsModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdPregnancyCreate(request)

    override suspend fun ncdPregnancyDetails(request: HashMap<String, Any>): Response<APIResponse<PregnancyDetailsModel>> =
        apiService.ncdPregnancyDetails(request)

    override suspend fun createPatientVisit(request: PatientVisitRequest): Response<APIResponse<PatientVisitResponse>> = apiService.createPatientVisit(request)

    override suspend fun createPatientVisit(request: MedicalReviewBaseRequest): Response<APIResponse<PatientVisit>> = apiService.createPatientVisit(request)

    override suspend fun createNCDMedicalReview(request: MedicalReviewRequestResponse): Response<APIResponse<MedicalReviewResponse>> =
        apiService.createNCDMedicalReview(request)

    override suspend fun fetchNCDMRSummary(request: MedicalReviewResponse): Response<APIResponse<MRSummaryResponse>> = apiService.fetchNCDMRSummary(request)

    override suspend fun getPatientPrescriptionHistoryList(request: RemovePrescriptionRequest): Response<APIResponse<ArrayList<Prescription>>> =
        apiService.getPatientPrescriptionHistoryList(request)

    override suspend fun createConfirmDiagonsis(request: NCDDiagnosisRequestResponse): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createConfirmDiagonsis(request)

    override suspend fun getConfirmDiagonsis(request: NCDDiagnosisGetRequest): Response<APIResponse<NCDDiagnosisGetResponse>> =
        apiService.getConfirmDiagonsis(request)

    override suspend fun createNCDPatientStatus(request: NCDPatientStatusRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createNCDPatientStatus(request)

    override suspend fun updateNCDTreatmentPlan(request: NCDTreatmentPlanModel): Response<APIResponse<NCDTreatmentPlanModel>> =
        apiService.updateNCDTreatmentPlan(request)

    override suspend fun getNCDTreatmentPlan(request: NCDTreatmentPlanModelDetails): Response<APIResponse<NCDTreatmentPlanModelDetails>> =
        apiService.getNCDTreatmentPlan(request)

    override suspend fun createNCDMRSummaryCreate(request: NCDMRSummaryRequestResponse): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createNCDMRSummaryCreate(request)

    override suspend fun getPrescriptionDispenseList(request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>> =
        apiService.getPrescriptionDispenseList(request)

    override suspend fun updateDispensePrescription(request: DispensePrescriptionRequest): Response<APIResponse<DispenseUpdateResponse>> =
        apiService.updateDispensePrescription(request)

    override suspend fun getDispensePrescriptionHistory(request: DispenseUpdateRequest): Response<APIResponse<ArrayList<DispensePrescriptionResponse>>> =
        apiService.getDispensePrescriptionHistory(request)

    override suspend fun createLifestyle(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> = apiService.createLifestyle(request)

    override suspend fun updateLifestyle(request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>> = apiService.updateLifestyle(request)

    override suspend fun getLifestyleList(request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>> =
        apiService.getLifestyleList(request)

    override suspend fun removeLifestyle(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> = apiService.removeLifestyle(request)

    override suspend fun createPsychological(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> = apiService.createPsychological(request)

    override suspend fun updatePsychological(request: AssessmentResultModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.updatePsychological(request)

    override suspend fun getPsychological(request: NCDCounselingModel): Response<APIResponse<ArrayList<NCDCounselingModel>>> =
        apiService.getPsychological(request)

    override suspend fun removePsychological(request: NCDCounselingModel): Response<APIResponse<NCDCounselingModel>> = apiService.removePsychological(request)

    override suspend fun getNCDMedicalReviewHistory(request: ReferralDetailRequest): Response<APIResponse<NCDMedicalReviewHistory>> =
        apiService.getNCDMedicalReviewHistory(request)

    override suspend fun validatePatient(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.validatePatient(request)

    override suspend fun ncdGetInstructions(): Response<APIResponse<NCDInstructionModel>> = apiService.ncdGetInstructions()

    override suspend fun ncdUpdatePregnancyRisk(request: NCDPregnancyRiskUpdate): Response<APIResponse<Boolean>> = apiService.ncdUpdatePregnancyRisk(request)

    override suspend fun getWazWhzScore(request: WazWhzScoreRequest): Response<APIResponse<WazWhzScoreResponse>> = apiService.getWazWhzScore(request)

    override suspend fun getUserDashboardDetails(request: NCDUserDashboardRequest): Response<APIResponse<NCDUserDashboardResponse>> =
        apiService.getUserDashboardDetails(request)

    override suspend fun getBadgeNotifications(request: BadgeNotificationModel): Response<APIResponse<BadgeNotificationModel>> =
        apiService.getBadgeNotifications(request)

    override suspend fun updateBadgeNotifications(request: BadgeNotificationModel): Response<APIResponse<Boolean>> =
        apiService.updateBadgeNotifications(request)

    override suspend fun getNcdLifeStyleDetails(request: LifeStyleRequest): Response<APIResponse<ArrayList<LifeStyleResponse>>> =
        apiService.getNcdLifeStyleDetails(request)

    override suspend fun ncdPatientRemove(request: NCDPatientRemoveRequest): Response<APIResponse<Boolean>> = apiService.ncdPatientRemove(request)

    override suspend fun bpLogCreateForNurse(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.bpLogCreateForNurse(request)

    override suspend fun glucoseLogCreateForNurse(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.glucoseLogCreateForNurse(request)

    override suspend fun ncdUpdatePatientDetail(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdUpdatePatientDetail(request)

    override suspend fun getUserTermsAndConditions(request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>> =
        apiService.getUserTermsAndConditions(request)

    override suspend fun updateTermsAndConditionsStatus(request: TermsAndConditionsModel): Response<APIResponse<TermsAndConditionsModel>> =
        apiService.updateTermsAndConditionsStatus(request)

    override suspend fun ncdUpdateNextVisitDate(request: NCDMedicalReviewUpdateModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdUpdateNextVisitDate(request)

    override suspend fun validatePatientTransfer(request: NCDPatientTransferValidate): Response<HashMap<String, Any>> =
        apiService.validatePatientTransfer(request)

    override suspend fun createPatientTransfer(request: NCDTransferCreateRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createPatientTransfer(request)

    override suspend fun searchSite(request: NCDRegionSiteModel): Response<APIResponse<ArrayList<RegionSiteResponse>>> = apiService.searchSite(request)

    override suspend fun searchRoleUser(request: NCDSiteRoleModel): Response<APIResponse<ArrayList<NCDSiteRoleResponse>>> = apiService.searchRoleUser(request)

    override suspend fun getPatientListTransfer(request: NCDPatientTransferNotificationCountRequest): Response<APIResponse<PatientTransferListResponse>> =
        apiService.getPatientListTransfer(request)

    override suspend fun patientTransferNotificationCount(
        request: NCDPatientTransferNotificationCountRequest,
    ): Response<APIResponse<NCDPatientTransferNotificationCountResponse>> = apiService.patientTransferNotificationCount(request)

    override suspend fun patientTransferUpdate(request: NCDPatientTransferUpdateRequest): Response<APIResponse<String>> =
        apiService.patientTransferUpdate(request)

    override suspend fun getNudgesList(prescriptionNudgeRequest: PredictionRequest): Response<APIResponse<PrescriptionNudgeResponse>> =
        apiService.getNudgesList(prescriptionNudgeRequest)

    override suspend fun getLabTestNudgeList(predictionRequest: PredictionRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.getLabTestNudgeList(predictionRequest)

    override suspend fun ncdFollowUpList(request: FollowUpRequest): APIResponse<List<PatientFollowUpEntity>> = apiService.ncdFollowUpList(request)

    override suspend fun getPatientCallRegister(): Response<APIResponse<RegisterCallResponse>> = apiService.getPatientCallRegister()

    override suspend fun updatePatientCallRegister(request: FollowUpUpdateRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.updatePatientCallRegister(request)

    override suspend fun updateDeviceDetails(request: DeviceDetails): Response<APIResponse<DeviceDetails>> = apiService.updateDeviceDetails(request)

    override suspend fun createMentalHealthStatus(request: NCDMentalHealthStatusRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createMentalHealthStatus(request)

    override suspend fun ncdMentalHealthMedicalReviewCreateA(request: JsonObject): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdMentalHealthMedicalReviewCreateA(request)

    override suspend fun ncdPatientDiagnosisStatus(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdPatientDiagnosisStatus(request)

    override suspend fun ncdMentalHealthMedicalReviewCreateS(request: JsonObject): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdMentalHealthMedicalReviewCreateS(request)

    override suspend fun ncdMentalHealthMedicalReviewDetailsA(request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdMentalHealthMedicalReviewDetailsA(request)

    override suspend fun ncdMentalHealthMedicalReviewDetailsS(request: NCDMentalHealthMedicalReviewDetails): Response<APIResponse<HashMap<String, Any>>> =
        apiService.ncdMentalHealthMedicalReviewDetailsS(request)

    override suspend fun markAsReviewed(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.markAsReviewed(request)

    override suspend fun cultureLocaleUpdate(request: CultureLocaleModel): Response<APIResponse<HashMap<String, Any>>> = apiService.cultureLocaleUpdate(request)

    override suspend fun getVaccinationList(request: RequestVaccinationList): Response<APIResponse<ArrayList<VaccinationDetail>>> =
        apiService.getImmunisationList(request)

    override suspend fun saveImmunisationList(request: RequestCreateImmunisation): Response<APIResponse<ResponseCreateImmunisation>> =
        apiService.saveImmunisationList(request)

    override suspend fun getImmunisationSummaryDetails(request: RequestImmunisationSummaryDetail): Response<APIResponse<ResponseImmunisationSummaryDetails>> =
        apiService.getImmunisationSummaryDetails(request)

    override suspend fun saveImmunisationSummaryDetails(request: RequestImmunisationSummaryCreate): Response<APIResponse<ResponseImmunisationSummaryCreate>> =
        apiService.saveImmunisationSummaryDetails(request)

    override suspend fun createCommunityProfile(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createCommunityProfile(request)

    override suspend fun getCommunityProfile(request: HashMap<String, Any>): Response<APIResponse<CommunityProfileDetails>> =
        apiService.getCommunityProfileDetails(request)

    override suspend fun updateCommunityProfile(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.updateCommunityProfile(request)

    override suspend fun createSupportRequest(request: NCDSupportRequest): Response<APIResponse<String>> = apiService.createSupportRequest(request)

    override suspend fun getTbStaticData(): Response<APIResponse<TbMetaResponse>> = apiService.getTbStaticData()

    override suspend fun createHeight(request: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>> = apiService.createHeight(request)

    override suspend fun fetchHeight(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>> =
        apiService.fetchHeight(motherNeonateAncRequest)

    override suspend fun fetchBmi(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<BpAndWeightResponse>> =
        apiService.fetchBmi(motherNeonateAncRequest)

    override suspend fun fetchList(motherNeonateAncRequest: MotherNeonateAncRequest): Response<APIResponse<List<BpAndWeightResponse>>> =
        apiService.fetchList(motherNeonateAncRequest)

    override suspend fun fetchTbAssessmentDetails(request: MotherNeonateAncRequest): Response<APIResponse<TbHistory>> =
        apiService.fetchTbAssessmentDetails(request)

    override suspend fun saveTbMedicalReview(request: TbMedicalReviewCreateRequest): Response<APIResponse<PatientEncounterResponse>> =
        apiService.saveTbMedicalReview(request)

    override suspend fun createPatientType(request: PatientTypeCreateRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createPatientType(request)

    override suspend fun getPatientType(request: MotherNeonateAncRequest): Response<APIResponse<HashMap<String, Any>>> = apiService.getPatientType(request)

    override suspend fun getBirthDetails(request: RequestBirthDetails): Response<APIResponse<BirthDetails>> = apiService.getBirthDetails(request)

    override suspend fun getHivStaticData(): Response<APIResponse<HivMetaResponse>> = apiService.getHIVStaticData()

    override suspend fun createHivScreening(requset: HivScreeningRequest): Response<APIResponse<HivScreeningResponse>> = apiService.createHivScreening(requset)

    override suspend fun getHivScreeningDetails(request: HivScreeningResponse): Response<APIResponse<HivCreateScreeningSummaryResponse>> =
        apiService.getHivScreeningDetails(request)

    override suspend fun getCBSNotificationDetails(
        request: PeerSupervisorNotificationRequest,
    ): Response<APIResponse<ArrayList<PeerSupervisorNotificationResponse>>> = apiService.getCBSNotificationDetails(request)

    override suspend fun updateCBSNotification(request: PeerSupervisorNotificationRequest): Response<APIResponse<Unit>> =
        apiService.updateCBSNotification(request)

    override suspend fun createBMI(bpAndWeightRequestModel: BpAndWeightRequestModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createBMI(bpAndWeightRequestModel)

    override suspend fun createFamilyPlanningMR(request: FamilyPlanningContraceptivesRequest): Response<APIResponse<FamilyPlanningCreateResponse>> =
        apiService.createFamilyPlanningMR(request)

    override suspend fun getFamilyPlanningStaticData(): Response<APIResponse<FamilyPlanningMetaResponse>> = apiService.getFamilyPlanningStaticData()

    override suspend fun getFamilyPlanningMRSummaryDetails(request: AboveFiveYearsSummaryRequest): Response<APIResponse<FamilyPlanningSummaryResponse>> =
        apiService.getFamilyPlanningMRSummaryDetails(request)

    override suspend fun createHivImrCmr(request: HivRequestData): Response<APIResponse<PatientEncounterResponse>> = apiService.createHivImrCmr(request)

    override suspend fun fetchHivSummaryDetails(request: MotherNeonateAncRequest): Response<APIResponse<HivSummaryResponse>> =
        apiService.fetchHivSummaryDetails(request)

    override suspend fun getViralLoadData(request: ViralLoadRequest): Response<APIResponse<List<ViralLoadResponse>>> = apiService.getViralLoadData(request)

    override suspend fun getARTData(request: ArtRequest): Response<APIResponse<List<ARTResponse>>> = apiService.getARTData(request)

    override suspend fun getOpportunisticInfection(request: MotherNeonateAncRequest): Response<APIResponse<HashMap<String, HashMap<String, String>?>>> =
        apiService.getOpportunisticInfection(request)

    override suspend fun createEMTCT(request: EMTCTVisitStatusRequest): Response<APIResponse<EMTCTVisitStatusResponse>> = apiService.createEmtct(request)

    override suspend fun getHivVitalsDetails(request: HivVitalsRequest): Response<APIResponse<HivVitalsResponse>> = apiService.getHivVitalsDetails(request)

    override suspend fun createWhoClinicalStage(request: WhoClinicalStageCreateRequest): Response<APIResponse<HivClinicalInfoResponse>> =
        apiService.createWhoClinicalStage(request)

    override suspend fun getHivCD4Details(request: CD4DetailsRequest): Response<APIResponse<ArrayList<CD4DetailsResponse>>> =
        apiService.getHivCD4Details(request)

    override suspend fun getPatientSummaryDetails(request: PregnancySummaryRequest): Response<APIResponse<PregnancyDetailsModel>> =
        apiService.getPatientSummaryDetails(request)

    override suspend fun checkRecommendationInvestigations(request: MotherNeonateAncRequest): Response<APIResponse<HashMap<String, Boolean?>?>> =
        apiService.checkRecommendationInvestigations(request)

    override suspend fun searchPatientById(request: PatientDataModel): APIResponse<ArrayList<PatientListResModel>> {
        // Removed screeningReferral for search patient for all user
        request.patientFilter = request.patientFilter?.copy(screeningReferral = null)
        return apiService.searchPatientById(request)
    }

    override suspend fun patientFollowUpList(request: PatientDataModel): APIResponse<ArrayList<PatientListResModel>> = apiService.patientFollowUpList(request)

    override suspend fun patientsList(request: PatientDataModel): APIResponse<ArrayList<PatientListResModel>> = apiService.patientList(request)

    override suspend fun validatePatient(request: JsonObject): Response<APIResponse<Boolean>> = apiService.validatePatient(request)

    override suspend fun createPatient(request: JsonObject): Response<APIResponse<PatientCreateResponse>> = apiService.createPatient(request)

    override suspend fun validateQRCodeValidation(qrCodeRequest: QRCodeRequest): Response<QRCodeResponse> = apiService.validateQRCodeValidation(qrCodeRequest)

    override suspend fun createScreeningLog(createRequest: JsonObject): Response<ScreeningPatientResponse> = apiService.createScreeningLog(createRequest)

    override suspend fun validateSession(): Response<ResponseBody> = apiService.validateSession()

    override suspend fun getPatientDetails(request: PatientDetailsModel): Response<APIResponse<PatientDetailsModel>> = apiService.getPatientDetails(request)

    override suspend fun getPatientBPLogList(request: AssessmentListRequest): Response<APIResponse<BPLogListResponse>> = apiService.getPatientBPLogList(request)

    override suspend fun getPatientBloodGlucoseList(request: AssessmentListRequest): Response<APIResponse<BloodGlucoseListResponse>> =
        apiService.getPatientBloodGlucoseList(request)

    override suspend fun getSessionGraph(request: AssessmentListRequest): Response<APIResponse<SessionGraphModel>> = apiService.getSessionGraph(request)

    override suspend fun searchMedication(request: MedicationSearchReqModel): Response<APIResponse<ArrayList<PrescriptionModel>>> =
        apiService.searchMedication(request)

    override suspend fun createPregnancy(request: PregnancyCreateRequest): Response<APIResponse<HashMap<String, Any>>> = apiService.createPregnancy(request)

    override suspend fun updatePregnancy(request: PregnancyCreateRequest): Response<APIResponse<HashMap<String, Any>>> = apiService.updatePregnancy(request)

    override suspend fun getPatientPregnancyDetails(request: PatientPregnancyModel): Response<APIResponse<PregnancyCreateRequest>> =
        apiService.getPatientPregnancyDetails(request)

    override suspend fun confirmDiagnosis(request: ConfirmDiagnosesRequest): Response<APIResponse<HashMap<String, Any>>> = apiService.confirmDiagnosis(request)

    override suspend fun getMentalHealthDetails(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.getMentalHealthDetails(request)

    override suspend fun getPatientMedicalReviewSummary(request: MedicalReviewBaseRequest): Response<APIResponse<SummaryResponse>> =
        apiService.getPatientMedicalReviewSummary(request)

    override suspend fun updateTreatmentPlan(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.updateTreatmentPlan(request)

    override suspend fun treatmentPlanDetails(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.treatmentPlanDetails(request)

    override suspend fun getScreeningDetails(request: RequestPatientDetail): Response<APIResponse<ResponsePatientDetail>> =
        apiService.getScreeningDetails(request)

    override suspend fun createBpLog(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.createBpLog(request)

    override suspend fun createGlucoseLog(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.createGlucoseLog(request)

    override suspend fun patientRemove(request: PatientRemoveRequest): Response<APIResponse<HashMap<String, Any>>> = apiService.patientRemove(request)

    override suspend fun clearRedRisk(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.clearRedRisk(request)

    override suspend fun getInstructions(): Response<APIResponse<InstructionModel>> = apiService.getInstructions()

    override suspend fun updatePregnancyRisk(request: PregnancyRiskUpdate): Response<APIResponse<Boolean>> = apiService.updatePregnancyRisk(request)

    override suspend fun getSessionQuestions(
        session: Int,
        age: Int,
    ): Response<APIResponse<ArrayList<SessionModelResponse>>> = apiService.getSessionQuestions(session, age)

    override suspend fun getPatientFamilyOtherDetails(data: Lifestyle): Response<APIResponse<HashMap<String, Any>>> =
        apiService.getPatientFamilyOtherDetails(data)

    override suspend fun postSessionTerminate(request: TerminateSessionModel): Response<APIResponse<HashMap<String, Any>>> =
        apiService.postSessionTerminate(request)

    override suspend fun medicationList(request: MedicationSearchReqModel): Response<APIResponse<ArrayList<PrescriptionModel>>> =
        apiService.medicationList(request)

    override suspend fun createSingleWindowMR(request: SingleWindowModel): Response<APIResponse<InitialEncounterResponse>> =
        apiService.createSingleWindowMR(request)

    override suspend fun getBadgeCount(request: BadgeModel): Response<APIResponse<BadgeResponseModel>> = apiService.getBadgeCount(request)

    override suspend fun searchSite(request: RegionSiteModel): Response<APIResponse<ArrayList<RegionSiteResponse>>> = apiService.searchSite(request)

    override suspend fun searchRoleUser(request: SiteRoleModel): Response<APIResponse<ArrayList<SiteRoleResponse>>> = apiService.searchRoleUser(request)

    override suspend fun createPatientTransfer(request: TransferCreateRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createPatientTransfer(request)

    override suspend fun validatePatientTransfer(request: FillPrescriptionRequest): Response<APIResponse<HashMap<String, Any>>> =
        apiService.validatePatientTransfer(request)

    override suspend fun sessionCreate(request: SessionEncounterRequest): Response<APIResponse<InitialEncounterResponse>> = apiService.sessionCreate(request)

    override suspend fun createNurseMedicalReview(request: NurseMrRequestModel): Response<APIResponse<NurseCreateResponse>> =
        apiService.createNurseMedicalReview(request)

    override suspend fun getNursePrescriptionList(request: PatientPrescriptionModel): Response<APIResponse<ArrayList<PrescriptionModel>>> =
        apiService.getNursePrescriptionList(request)

    override suspend fun updatePrescription(body: RequestBody): Response<APIResponse<ResponseDataModel>> = apiService.updatePrescription(body)

    override suspend fun getPatientPrescriptionHistoryList(request: PatientHistoryRequest): Response<APIResponse<PatientPrescriptionHistoryResponse>> =
        apiService.getPatientPrescriptionHistoryList(request)

    override suspend fun getPrescriptionPrediction(map: HashMap<String, Any>): Response<APIResponse<PrescriptionPredictionResponse>> =
        apiService.prescriptionPrediction(map)

    override suspend fun getPatientLabTestHistory(request: PatientHistoryRequest): Response<APIResponse<PatientLabTestHistoryResponse>> =
        apiService.getPatientLabTestHistory(request)

    override suspend fun getPatientMedicalReviewHistoryList(request: MedicalReviewBaseRequest): Response<APIResponse<PatientMedicalReviewHistoryResponse>> =
        apiService.getPatientMedicalReviewHistoryList(request)

    override suspend fun getPatientSessionHistoryList(request: MedicalReviewBaseRequest): Response<APIResponse<SessionHistoryResponse>> =
        apiService.getPatientSessionHistoryList(request)

    override suspend fun getPatientLabTestRecommendation(request: InvestigationReq): Response<APIResponse<List<NurseLabTest>>> =
        apiService.getPatientLabTestRecommendation(request)

    override suspend fun getPatientLabTests(
        request: HashMap<String, Any?>,
    ): Response<APIResponse<org.medtroniclabs.uhis.data.registration.LabTestListResponse>> = apiService.getPatientLabTests(request)

    override suspend fun searchLabTest(request: SearchModel): Response<APIResponse<ArrayList<LabTestSearchResponse>>> = apiService.searchLabTest(request)

    override suspend fun getLabTestResult(labTestId: Long): Response<APIResponse<ArrayList<HashMap<String, Any>>>> = apiService.getLabTestResult(labTestId)

    override suspend fun getNudgesList(request: HashMap<String, Any>): Response<APIResponse<InvestigationNudgesModel>> = apiService.getNudgesList(request)

    override suspend fun referLabTest(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.referLabTest(request)

    override suspend fun createLabTestResult(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.createLabTestResult(request)

    override suspend fun getLabTestResultDetails(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.getLabTestResultDetails(request)

    override suspend fun removeLabTest(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> = apiService.removeLabTest(request)

    override suspend fun reviewLabTestResult(request: HashMap<String, Any>): Response<APIResponse<HashMap<String, Any>>> =
        apiService.reviewLabTestResult(request)

    override suspend fun updatePatientType(request: HashMap<String, Any>): Response<APIResponse<Boolean>> = apiService.updatePatientType(request)

    override suspend fun getPatientFillPrescriptionList(request: FillPrescriptionRequest): Response<APIResponse<ArrayList<FillPrescriptionListResponse>>> =
        apiService.getPatientFillPrescriptionList(request)

    override suspend fun fillPrescriptionUpdate(request: FillPrescriptionUpdateRequest): Response<APIResponse<ArrayList<FillMedicineResponse>>> =
        apiService.fillPrescriptionUpdate(request)

    override suspend fun getPrescriptionRefillHistory(
        request: org.medtroniclabs.uhis.data.registration.PatientPrescriptionModel,
    ): Response<APIResponse<ArrayList<PrescriptionRefillHistoryResponse>>> = apiService.getPrescriptionRefillHistory(request)

    override suspend fun getProviderDashboardDetails(request: UserDashboardRequest): Response<APIResponse<UserDashboardResponse>> =
        apiService.getProviderDashboardDetails(request)
}
