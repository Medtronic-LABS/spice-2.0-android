package com.medtroniclabs.opensource.db.local

import androidx.lifecycle.LiveData
import com.medtroniclabs.opensource.data.CulturesEntity
import com.medtroniclabs.opensource.data.DiseaseCategoryItems
import com.medtroniclabs.opensource.data.DosageFrequency
import com.medtroniclabs.opensource.data.ExaminationListItems
import com.medtroniclabs.opensource.data.FollowUpPatientModel
import com.medtroniclabs.opensource.data.LabourDeliveryMetaEntity
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.ProgramEntity
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.data.UnitMetricEntity
import com.medtroniclabs.opensource.data.model.HouseholdCardDetail
import com.medtroniclabs.opensource.data.offlinesync.model.HHSignatureDetail
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHold
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHoldMember
import com.medtroniclabs.opensource.data.offlinesync.model.HouseholdMemberCallRegisterDto
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.db.entity.AssessmentEntity
import com.medtroniclabs.opensource.db.entity.CallHistory
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowConditionEntity
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowEntity
import com.medtroniclabs.opensource.db.entity.NCDAssessmentClinicalWorkflow
import com.medtroniclabs.opensource.db.entity.ConsentEntity
import com.medtroniclabs.opensource.db.entity.ConsentForm
import com.medtroniclabs.opensource.db.entity.ChiefDomEntity
import com.medtroniclabs.opensource.db.entity.NCDMedicalReviewMetaEntity
import com.medtroniclabs.opensource.db.entity.FollowUp
import com.medtroniclabs.opensource.db.entity.FollowUpCall
import com.medtroniclabs.opensource.db.entity.FormEntity
import com.medtroniclabs.opensource.db.entity.FrequencyEntity
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.db.entity.MedicalComplianceEntity
import com.medtroniclabs.opensource.db.entity.LinkHouseholdMember
import com.medtroniclabs.opensource.db.entity.MemberClinicalEntity
import com.medtroniclabs.opensource.db.entity.MentalHealthEntity
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.db.entity.PregnancyDetail
import com.medtroniclabs.opensource.db.entity.ScreeningEntity
import com.medtroniclabs.opensource.db.entity.SignsAndSymptomsEntity
import com.medtroniclabs.opensource.db.entity.DistrictEntity
import com.medtroniclabs.opensource.db.entity.DosageDurationEntity
import com.medtroniclabs.opensource.db.entity.LifestyleEntity
import com.medtroniclabs.opensource.db.entity.NCDCallDetails
import com.medtroniclabs.opensource.db.entity.NCDDiagnosisEntity
import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.db.entity.NCDPatientDetailsEntity
import com.medtroniclabs.opensource.db.entity.RiskFactorEntity
import com.medtroniclabs.opensource.db.entity.TreatmentPlanEntity
import com.medtroniclabs.opensource.db.entity.UserProfileEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.response.HouseHoldEntityWithMemberCount
import com.medtroniclabs.opensource.db.response.HouseholdMemberCount
import com.medtroniclabs.opensource.model.MemberDobGenderModel
import com.medtroniclabs.opensource.model.assessment.AssessmentDetails
import com.medtroniclabs.opensource.model.assessment.AssessmentMemberDetails
import com.medtroniclabs.opensource.ui.assessment.AssessmentNCDEntity

interface RoomHelper {
    suspend fun saveHouseHoldEntry(householdEntity: HouseholdEntity): Long
    suspend fun updateHousehold(householdEntity: HouseholdEntity)
    suspend fun getLastHouseholdNo(villageId: Long): Long?
    suspend fun getHouseHoldDetailsById(houseHoldId: Long): HouseholdEntity
    suspend fun registerMember(householdMemberEntity: HouseholdMemberEntity): Long
    suspend fun getAllHouseHoldMemberList(houseHoldId: Long): ArrayList<HouseholdMemberEntity>
    suspend fun getMemberDetailsByID(memberId: Long): HouseholdMemberEntity
    suspend fun getMemberDetailsByParentId(memberId: String): List<HouseholdMemberEntity>
    suspend fun saveAssessment(assessmentEntity: AssessmentEntity): Long
    suspend fun updateOtherAssessmentDetails(assessmentEntity: AssessmentEntity)
    suspend fun getLatestAssessmentForMember(memberId: Long): AssessmentEntity?
    suspend fun insertSymptomList(symptoms: List<SignsAndSymptomsEntity>)
    suspend fun getSymptomListByType(type: String): List<SignsAndSymptomsEntity>
    suspend fun updateHeadCount(householdId: Long, newNoOfPeople: Int)
    suspend fun getMemberCountPerHouseHold(householdId: Long): Int
    suspend fun saveHealthFacility(healthFacilityEntityList: HealthFacilityEntity)
    suspend fun deleteAllHealthFacility()
    suspend fun saveVillage(villageEntityList: List<VillageEntity>)
    suspend fun getAllVillageEntity(): List<VillageEntity>
    suspend fun getVillagesByChiefDom(chiefdomId: Long): List<VillageEntity>
    suspend fun deleteAllVillages()
    suspend fun deleteAllHouseholds()
    suspend fun deleteAllHouseholdMembers()
    suspend fun saveMenus(menuEntity: MenuEntity)
    suspend fun deleteAllMenus()
    suspend fun saveUserProfileDetails(userProfileEntity: UserProfileEntity)
    suspend fun deleteAllUserProfileDetails()
    suspend fun getMenus(): List<MenuEntity>
    suspend fun getUserProfile(): UserProfileEntity
    suspend fun getDefaultHealthFacility(): HealthFacilityEntity?
    suspend fun saveClinicalWorkflows(clinicalWorkflows: List<ClinicalWorkflowEntity>)
    suspend fun deleteAllClinicalWorkflow()
    suspend fun getAllClinicalWorkflowIds(): List<Int>
    suspend fun saveForms(forms: List<FormEntity>)
    suspend fun deleteAllForms()
    suspend fun getFormData(
        formType: String
    ): String

    suspend fun insertSymptoms(symptomEntity: List<SignsAndSymptomsEntity>)
    suspend fun deleteAllSymptoms()
    suspend fun getMenuForClinicalWorkflows(): List<ClinicalWorkflowEntity>
    suspend fun deleteClinicalWorkflowConditions()
    suspend fun insertClinicalWorkflowConditions(clinicalWorkflowConditions: List<ClinicalWorkflowConditionEntity>)
    suspend fun getVillageByID(villageId: Long): VillageEntity
    suspend fun getLastPatientId(patientIdStarts: String): String?
    fun getMemberCountInHouseholdLiveData(houseHoldId: Long): LiveData<HouseholdMemberCount>
    suspend fun getClinicalWorkflowId(
        gender: String,
        age: Int
    ): List<NCDAssessmentClinicalWorkflow>

    suspend fun getDobAndGenderById(memberId: Long): MemberDobGenderModel
    suspend fun getAllUnSyncedHouseHolds(): List<HouseHold>

    suspend fun getAllUnSyncedHouseHoldMembers(houseHoldId: Long): List<HouseHoldMember>

    suspend fun getOtherHouseholdMembers(memberIds: List<String>): List<HouseHoldMember>
    suspend fun updateFhirId(tableName: String, id: String, fhirId: String?, status: String)
    fun getFilteredHouseholdsLiveData(
        searchInput: String,
        filterByVillage: List<Long>,
        filterByStatus: String
    ): LiveData<List<HouseHoldEntityWithMemberCount>>

    suspend fun getNearestHealthFacility(): List<HealthFacilityEntity>
    suspend fun getUnSyncedHouseholdCount(): Int
    suspend fun getUnSyncedHouseholdMemberCount(): Int
    suspend fun getPatientVisitCountByType(type: String, hhmLocalId: Long): MemberClinicalEntity?
    suspend fun savePatientVisitCountByType(memberClinicalEntity: MemberClinicalEntity)
    suspend fun deleteExaminationsComplaints(menuType: String)
    suspend fun insertExaminationsComplaint(symptomEntity: List<MedicalReviewMetaItems>)
    suspend fun deleteDiagnosisList(diagnosisType: String)
    suspend fun saveDiagnosisList(diagnosisList: ArrayList<DiseaseCategoryItems>)
    suspend fun getHouseholdIdByFhirId(fhirId: String?): Long?
    suspend fun getHouseholdMemberIdByFhirId(fhirId: String?): Long?
    suspend fun getExaminationsComplaintByType(type: String): List<MedicalReviewMetaItems>
    suspend fun getAssessmentMemberDetails(id: Long): AssessmentMemberDetails
    suspend fun getUnSyncedAssessmentByHHMId(hhmId: Long): List<AssessmentDetails>
    suspend fun getOtherUnSyncedAssessments(addedAssessmentIds: List<String>): List<AssessmentDetails>
    suspend fun getUnSyncedAssessmentCount(): Int

    suspend fun deleteAllAssessments()
    suspend fun updatePregnancyAncDetail(
        hhmLocalId: Long,
        visitCount: Long,
        clinicalDate: String?
    )

    suspend fun getSummaryDetailMetaItems(type: String): List<MedicalReviewMetaItems>
    suspend fun deleteExaminationsComplaintsForAnc(type: String)

    fun getExaminationsComplaintsForAnc(
        category: String,
        type: String
    ): LiveData<List<MedicalReviewMetaItems>>

    suspend fun deleteExaminationsList(menuType: String)
    suspend fun saveExaminationsList(diagnosisList: ArrayList<ExaminationListItems>)

    suspend fun insertLabourDelivery(symptomEntity: List<LabourDeliveryMetaEntity>)
    suspend fun deleteLabourDelivery()
    suspend fun getLabourDelivery(): List<LabourDeliveryMetaEntity>
    suspend fun getDiagnosisList(diagnosisType: String): List<DiseaseCategoryItems>

    suspend fun getExaminationQuestionsByWorkFlow(workFlowType: String): ExaminationListItems

    suspend fun insertFollowUp(followUp: FollowUp): Long

    suspend fun deleteAllFollowUps()

    suspend fun deleteAllUnAssignedMember()

    suspend fun deleteAllCallHistory()

    fun getFollowUpPatientListLiveData(
        type: String,
        search: String? = null,
        villageIds: List<Long> = listOf(),
        fromDate: String = "",
        toDate: String = ""
    ): LiveData<List<FollowUpPatientModel>>

    suspend fun getAllVillageIds(): List<Long>

    suspend fun deleteAllPregnancyDetails()
    suspend fun getPatientIdByFhirId(fhirId: String): String?

    suspend fun insertUpdatePregnancyDetailFromBE(pregnancyDetail: PregnancyDetail)

    suspend fun addCallHistory(
        oldFollowUp: FollowUp,
        history: FollowUpCall,
        newFollowUp: FollowUp? = null
    )

    suspend fun deleteAllFollowUpCalls()

    suspend fun getFollowUpById(id: Long): FollowUp

    suspend fun getAllFollowUpRequests(): List<FollowUp>

    suspend fun getAllFollowUpCalls(id: Long): List<FollowUpCall>

    suspend fun getUnSyncedFollowUpCount(): Int

    fun getExaminationsComplaintByTypeLiveData(category: String): LiveData<List<MedicalReviewMetaItems>>

    fun getHouseholdCardDetailLiveData(id: Long): LiveData<HouseholdCardDetail>

    fun getAllHouseHoldMembersLiveData(hhId: Long): LiveData<List<HouseholdMemberEntity>>

    fun getAliveHouseHoldMembersLiveData(hhId: Long): List<HouseholdMemberEntity>

    suspend fun updateOtherDuplicateTickets(id: Long, followUp: FollowUp)

    suspend fun updateDuplicateTicketsAsCompleted(id: Long, followUp: FollowUp)

    suspend fun updateOtherFollowUpForWrongNumber(id: Long, fhirId: String)

    suspend fun updateOnTreatmentStatus(id: Long, followUp: FollowUp, updatedAt: Long)

    suspend fun changeHouseholdStatus(idList: List<String>, syncStatus: String)

    suspend fun changeHouseholdMemberStatus(idList: List<String>, syncStatus: String)

    suspend fun changeAssessmentStatus(idList: List<String>, syncStatus: String)

    suspend fun changeFollowUpStatus(idList: List<Long>, syncStatus: String)

    suspend fun changeFollowUpCallStatus(idList: List<Long>)

    suspend fun changeHHMLinkCallStatus(idList: List<String>, syncStatus: String)

    suspend fun changeAssignHHMStatus(idList: List<String>, syncStatus: String)

    suspend fun insertOrUpdateHHFromBE(entity: HouseholdEntity): Long

    suspend fun insertOrUpdateHHMFromBE(entity: HouseholdMemberEntity): Long

    suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail?

    suspend fun savePregnancyDetail(detail: PregnancyDetail): Long

    fun getExaminationsComplaintsForPnc(
        category: String,
        type: String
    ): LiveData<List<MedicalReviewMetaItems>>
    suspend fun deleteAllFrequencyList()
    suspend fun saveFrequencyList(villageEntityList: List<FrequencyEntity>): List<Long>

    suspend fun saveUnitMetric(list: ArrayList<UnitMetricEntity>)

    suspend fun getFrequencyList(): List<FrequencyEntity>

    suspend fun getDosageFrequencyList(): List<DosageFrequency>

    suspend fun insertOrUpdateFollowUp(entity: FollowUp)

    suspend fun deleteCompletedFollowUp()
    suspend fun saveForm(forms: FormEntity)

    suspend fun updateNeonatePatientId( hhmLocalId: Long, neonateId: Long)

    suspend fun getMemberDetailsByPatientId(patientId: String): HouseholdMemberEntity?

    suspend fun getChildPatientId(parentId: Long): Long?

    suspend fun getUserHealthFacility(isUserSite: Boolean): ArrayList<HealthFacilityEntity>
    suspend fun updateMemberDeceasedStatus(id: Long, status: Boolean)

    suspend fun getPatientIdById(id: Long): String

    suspend fun insertConsentForm(form: ConsentForm): Long

    suspend fun getConsentFormByType(type: String): ConsentForm?

    suspend fun deleteAllConsentForm()

    suspend fun getHHSignatureDetails(): List<HHSignatureDetail>

    //UpdateHouseHoldNumber
    suspend fun updateHeadPhoneNumber(id: Long, phoneNumber: String, phoneNumberCategory: String)

    suspend fun updatePhoneNumberForHouseholdHead(id: Long, phoneNumber: String?, phoneNumberCategory: String?)

    suspend fun insertLinkHouseholdMembers(insertList: List<LinkHouseholdMember>)

    suspend fun deleteLinkHouseholdMembersById(deleteListIds: List<String>)

    fun getUnAssignedHouseholdMembersLiveData(): LiveData<List<UnAssignedHouseholdMemberDetail>>

    suspend fun addLinkMemberCall(callHistory: CallHistory): Long

    suspend fun getUnSyncedCallHistoryForHHMLink(): List<HouseholdMemberCallRegisterDto>

    suspend fun changeMemberDetailsToNotSynced(id: Long)

    suspend fun updateMemberAsAssigned(memberId: String)

    suspend fun saveConsent(consentEntity: ConsentEntity)
    fun getConsent(formType: String): LiveData<String>
    suspend fun deleteConsent()

    suspend fun saveModelQuestions(mentalHealthEntity: List<MentalHealthEntity>)
    suspend fun getModelQuestions(formType: String): MentalHealthEntity
    suspend fun deleteModelQuestions()

    suspend fun saveMedicalCompliance(list: List<MedicalComplianceEntity>)
    suspend fun getMedicalParentComplianceList(): List<MedicalComplianceEntity>
    suspend fun getMedicalChildComplianceList(parentId: Long): List<MedicalComplianceEntity>
    suspend fun deleteMedicalCompliance()
    suspend fun saveDistricts(counties: List<DistrictEntity>)
    suspend fun getDistricts(countryId: Long): List<DistrictEntity>
    suspend fun deleteDistricts()
    suspend fun saveChiefDoms(chiefdoms: List<ChiefDomEntity>)
    suspend fun getChiefDoms(districtId: Long): List<ChiefDomEntity>
    suspend fun deleteChiefDoms()
    suspend fun savePrograms(programs: List<ProgramEntity>)
    suspend fun getPrograms(): List<ProgramEntity>
    suspend fun deletePrograms()
     fun getMentalQuestion(formType: String): LiveData<MentalHealthEntity?>
    fun getSites(): LiveData<List<HealthFacilityEntity>>
    suspend fun savePatientScreeningInformation(screeningEntity: ScreeningEntity) : ScreeningEntity

    fun getScreenedPatientCount(startDate: Long, endDate: Long, userId: String): LiveData<Long>
    fun getScreenedPatientReferredCount(
        startDate: Long,
        endDate: Long,
        userId: String,
        isReferred: Boolean
    ): LiveData<Long>

    suspend fun getAllScreeningRecords(uploadStatus: Boolean): List<ScreeningEntity>?

    suspend fun deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds: Long)

    suspend fun updateScreeningRecordById(id: Long, uploadStatus: Boolean)

    suspend fun insertRiskFactor(riskFactorEntity: RiskFactorEntity)
    fun getRiskFactorEntity(): LiveData<List<RiskFactorEntity>>
    suspend fun deleteRiskFactor()

    fun getSymptomListByTypeForNCD(type: String): LiveData<List<SignsAndSymptomsEntity>>

    suspend fun deleteTreatmentPlan()
    suspend fun insertTreatmentPlan(items: List<TreatmentPlanEntity>)
    suspend fun deleteNCDMedicalReviewMeta()
    suspend fun insertNCDMedicalReviewMeta(items: List<NCDMedicalReviewMetaEntity>)

    suspend fun deleteLifestyle()
    suspend fun insertLifestyle(items: List<LifestyleEntity>)
    fun getComorbidities(type: String?,category: String): LiveData<List<NCDMedicalReviewMetaEntity>>

    fun getLifeStyle(): LiveData<List<LifestyleEntity>>
    fun getAssessmentFormData(formTypes: List<String>, workFlow: String): List<String>
    fun getAssessmentFormData(formType: String, workFlow: String): LiveData<String>
    suspend fun getSymptomList(): List<SignsAndSymptomsEntity>

    suspend fun saveAssessmentInformation(request: AssessmentNCDEntity): AssessmentNCDEntity

    suspend fun getAllAssessmentRecords(uploadStatus: Boolean):List<AssessmentNCDEntity>

    suspend fun updateAssessmentUploadStatus(id: Long, uploadStatus: Boolean)

    suspend fun deleteAssessmentList(isUploaded: Boolean)

    suspend fun getAssessmentClinicalWorkflow(
        gender: String,
        name: String
    ): List<NCDAssessmentClinicalWorkflow>
    suspend fun getUnitList(type: String): List<UnitMetricEntity>

    suspend fun deleteUnitMetric()

    suspend fun saveDosageFrequencyList(list: ArrayList<DosageFrequency>)

    suspend fun deleteDosageFrequencyList()
    fun getUnSyncedDataCountForNCDScreening(): LiveData<Long>
    fun getUnSyncedNCDAssessmentCount(): LiveData<Long>

    suspend fun saveNCDDiagnosisList(diseaseEntityList: ArrayList<NCDDiagnosisEntity>)

    suspend fun deleteNCDDiagnosisList()
    fun getNCDDiagnosisList(
        types: List<String>,
        gender: String,
        isPregnant: Boolean
    ): LiveData<List<NCDDiagnosisEntity>>
    fun getFrequencies() : LiveData<List<TreatmentPlanEntity>>

    suspend fun getNCDShortageReason(type: String): List<ShortageReasonEntity>
    suspend fun deleteNCDShortageReason()
    suspend fun saveNCDShortageReason(shortageReasonEntity: List<ShortageReasonEntity>)
    suspend fun getUnAssignedParentFhirId(parentId: String): List<String>
    suspend fun getUnAssignedChildFhirIds(patientId: String): List<String>
    suspend fun updateHouseholdHeadAndRelationShip(fhirIds: List<String>, householdId: Long)
    suspend fun updateMembersAsAssigned(fhirIds: List<String>)

    suspend fun getNCDForm(type: String, customizedType: String): List<String>
    suspend fun getUserVillages(): List<VillageEntity>
    suspend fun deleteDosageDurations()
    suspend fun insertDosageDurations(items: List<DosageDurationEntity>)
    suspend fun getDosageDurations(): List<DosageDurationEntity>
    suspend fun deleteAllNCDFollowUp()
    suspend fun insertNCDFollowUp(followUp: NCDFollowUp): Long

    fun getNCDFollowUpData(
        type: String,
        searchText: String,
        dateBasedOnChip: Pair<Long?, Long?>?,
        isScreened: Boolean?,
        reason:String?
    ): LiveData<List<NCDFollowUp>>

    suspend fun updatedCallInitiatedCall(ncdFollowUp: NCDFollowUp): NCDFollowUp

    suspend fun getNCDInitiatedCallFollowUp(): NCDFollowUp?
    suspend fun insertNCDCallDetails(followUp: NCDCallDetails): NCDCallDetails?
    suspend fun updateRetryAttempts(id: Long, retryAttempts: Long)
    suspend fun getAttemptsById(id: Long): Long?
    suspend fun getNCDFollowUpById(id: Long): NCDFollowUp

    suspend fun getAllNCDCallDetails(): List<NCDCallDetails>
    suspend fun insertNCDPatientDetails(patients: NCDPatientDetailsEntity): Long

    suspend fun deleteAllNCDPatientDetails()

    suspend fun getPatientBasedOnId(id: String): NCDPatientDetailsEntity

    suspend fun deleteCallDetails(id: Long)

    fun getUnSyncedNCDFollowUpCount(): LiveData<Long>
    suspend fun saveCultures(cultures: List<CulturesEntity>)
    suspend fun getCultures(): List<CulturesEntity>
    suspend fun deleteCultures()
}