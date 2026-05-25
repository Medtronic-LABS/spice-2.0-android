package org.medtroniclabs.uhis.db.local

import androidx.lifecycle.LiveData
import org.medtroniclabs.uhis.data.CulturesEntity
import org.medtroniclabs.uhis.data.DiseaseCategoryItems
import org.medtroniclabs.uhis.data.DosageFrequency
import org.medtroniclabs.uhis.data.ExaminationListItems
import org.medtroniclabs.uhis.data.FollowUpPatientModel
import org.medtroniclabs.uhis.data.LabourDeliveryMetaEntity
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.ProgramEntity
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.data.community.CommunityPopulationStatistics
import org.medtroniclabs.uhis.data.community.CommunityProfileDetail
import org.medtroniclabs.uhis.data.model.HouseholdCardDetail
import org.medtroniclabs.uhis.data.offlinesync.model.HHSignatureDetail
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHold
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHoldMember
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberCallRegisterDto
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberFhirId
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberStatus
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddyFollowUpDetails
import org.medtroniclabs.uhis.data.offlinesync.model.RxBuddyRegisterDetail
import org.medtroniclabs.uhis.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import org.medtroniclabs.uhis.db.dao.HouseholdSortOrder
import org.medtroniclabs.uhis.db.entity.AssessmentEntity
import org.medtroniclabs.uhis.db.entity.CallHistory
import org.medtroniclabs.uhis.db.entity.ChiefDomEntity
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowConditionEntity
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowEntity
import org.medtroniclabs.uhis.db.entity.CommunityProfile
import org.medtroniclabs.uhis.db.entity.ComorbidityEntity
import org.medtroniclabs.uhis.db.entity.ComplaintsEntity
import org.medtroniclabs.uhis.db.entity.ComplicationEntity
import org.medtroniclabs.uhis.db.entity.ConsentEntity
import org.medtroniclabs.uhis.db.entity.ConsentForm
import org.medtroniclabs.uhis.db.entity.CurrentMedicationEntity
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.db.entity.DistrictEntity
import org.medtroniclabs.uhis.db.entity.DosageDurationEntity
import org.medtroniclabs.uhis.db.entity.FollowUp
import org.medtroniclabs.uhis.db.entity.FollowUpCall
import org.medtroniclabs.uhis.db.entity.FormEntity
import org.medtroniclabs.uhis.db.entity.FrequencyEntity
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.HouseholdEntity
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.LifestyleEntity
import org.medtroniclabs.uhis.db.entity.LinkHouseholdMember
import org.medtroniclabs.uhis.db.entity.LinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.MedicalComplianceEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MemberClinicalEntity
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.db.entity.NCDAssessmentClinicalWorkflow
import org.medtroniclabs.uhis.db.entity.NCDCallDetails
import org.medtroniclabs.uhis.db.entity.NCDDiagnosisEntity
import org.medtroniclabs.uhis.db.entity.NCDFollowUp
import org.medtroniclabs.uhis.db.entity.NCDMedicalReviewMetaEntity
import org.medtroniclabs.uhis.db.entity.NCDPatientDetailsEntity
import org.medtroniclabs.uhis.db.entity.PhysicalExaminationEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.db.entity.RiskFactorEntity
import org.medtroniclabs.uhis.db.entity.RxBuddyDetails
import org.medtroniclabs.uhis.db.entity.RxBuddyFollowUpEntity
import org.medtroniclabs.uhis.db.entity.ScreeningEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiLinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaLinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.SymptomEntity
import org.medtroniclabs.uhis.db.entity.TreatmentDetailsEntity
import org.medtroniclabs.uhis.db.entity.TreatmentPlanEntity
import org.medtroniclabs.uhis.db.entity.UserProfileEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.db.response.DashboardCountsRow
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.db.response.HouseholdMemberCount
import org.medtroniclabs.uhis.db.response.MemberAssessmentHistoryResponse
import org.medtroniclabs.uhis.model.MemberDobGenderModel
import org.medtroniclabs.uhis.model.assessment.AssessmentDetails
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.model.services.ServiceMemberCounts
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.ui.assessment.AssessmentNCDEntity

interface RoomHelper {
    suspend fun saveHouseHoldEntry(householdEntity: HouseholdEntity): Long

    suspend fun updateHousehold(householdEntity: HouseholdEntity)

    suspend fun getLastHouseholdNo(villageId: Long): Long?

    suspend fun checkHouseholdNumberExists(householdNo: Long): Boolean

    suspend fun getHouseHoldDetailsById(houseHoldId: Long): HouseholdEntity

    suspend fun registerMember(householdMemberEntity: HouseholdMemberEntity): Long

    /**
     * Retrieves all unique National IDs from the database for the given ID type.
     *
     * @param idType The type of ID to filter by.
     * @return A list of unique National IDs.
     */
    suspend fun getAllNationalIds(idType: String): List<String>

    suspend fun getAllHouseHoldMemberList(houseHoldId: Long): ArrayList<HouseholdMemberEntity>

    suspend fun getMemberDetailsByID(memberId: Long): HouseholdMemberEntity

    suspend fun getMemberDetailsByParentId(memberId: String): List<HouseholdMemberEntity>

    suspend fun saveAssessment(assessmentEntity: AssessmentEntity): Long

    suspend fun updateOtherAssessmentDetails(assessmentEntity: AssessmentEntity)

    suspend fun getLatestAssessmentForMember(memberId: Long): AssessmentEntity?

    suspend fun insertSymptomList(symptoms: List<SignsAndSymptomsEntity>)

    suspend fun getSymptomListByType(type: String): List<SignsAndSymptomsEntity>

    /**
     * Updates no of people with actual count of members for given household
     * where no of people is less than actual members
     */
    suspend fun updateHeadCountIfUnderCounted(householdId: Long)

    suspend fun getMemberCountPerHouseHold(householdId: Long): Int

    suspend fun saveHealthFacility(healthFacilityEntityList: HealthFacilityEntity)

    suspend fun deleteAllHealthFacility()

    suspend fun saveVillage(villageEntityList: List<VillageEntity>)

    suspend fun getAllVillageEntity(): List<VillageEntity>

    suspend fun getAllLinkedVillageEntity(): List<VillageEntity>

    suspend fun insertLinkedVillages(linkedVillages: List<LinkedVillageEntity>)

    suspend fun deleteAllLinkedVillages()

    suspend fun getVillagesByChiefDom(chiefdomId: Long): List<VillageEntity>

    // SubVillage methods
    suspend fun saveSubVillages(subVillageEntityList: List<SubVillageEntity>)

    suspend fun deleteAllSubVillages()

    // ShasthyaShebika methods
    suspend fun getShasthyaShebikaById(id: Long): ShasthyaShebikaEntity?

    suspend fun saveShasthyaShebikas(shasthyaShebikaEntityList: List<ShasthyaShebikaEntity>)

    suspend fun deleteAllShasthyaShebikas()

    suspend fun getShasthyaShebikaByShasthyaKormiId(shasthyaKormiId: Long): List<ShasthyaShebikaEntity>

    // Chiefdom methods
    suspend fun saveChiefdoms(chiefdomEntityList: List<ChiefDomEntity>)

    suspend fun deleteAllChiefdoms()

    suspend fun getChiefdomByVillageId(villageId: Long): List<ChiefDomEntity>

    suspend fun getAllChiefdoms(): List<ChiefDomEntity>

    suspend fun getChiefdomByShasthyaKormiId(shasthyaKormiId: Long): List<ChiefDomEntity>

    suspend fun getChiefdomByShasthyaKormiIds(shasthyaKormiIds: List<Long>): List<ChiefDomEntity>

    suspend fun getChiefdomById(chiefdomId: Long): ChiefDomEntity?

    // ShasthyaKormi methods
    suspend fun saveShasthyaKormis(shasthyaKormiEntityList: List<ShasthyaKormiEntity>)

    suspend fun deleteAllShasthyaKormis()

    suspend fun getShasthyaKormiByVillageId(villageId: Long): List<ShasthyaKormiEntity>

    suspend fun getAllShasthyaKormis(): List<ShasthyaKormiEntity>

    // ShasthyaKormiLinkedVillage methods
    suspend fun insertShasthyaKormiLinkedVillages(linkedVillages: List<ShasthyaKormiLinkedVillageEntity>)

    suspend fun deleteAllShasthyaKormiLinkedVillages()

    suspend fun getSubVillagesByShasthyaKormiId(shasthyaKormiId: Long): List<SubVillageEntity>

    suspend fun getSubVillagesByShasthyaKormiIds(shasthyaKormiIds: List<Long>): List<SubVillageEntity>

    suspend fun getVillagesForShasthyaKormiAndChiefdom(
        shasthyaKormiId: Long,
        chiefdomId: Long,
    ): List<VillageEntity>

    // ShasthyaShebikaLinkedVillage methods
    suspend fun insertShasthyaShebikaLinkedVillages(linkedVillages: List<ShasthyaShebikaLinkedVillageEntity>)

    suspend fun deleteAllShasthyaShebikaLinkedVillages()

    suspend fun getSubVillagesByShasthyaShebikaId(shasthyaShebikaId: Long): List<SubVillageEntity>

    suspend fun getSubVillagesByShasthyaShebikaIds(shasthyaShebikaIds: List<Long>): List<SubVillageEntity>

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

    suspend fun getFormData(formType: String): String

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
        age: Int,
    ): List<NCDAssessmentClinicalWorkflow>

    suspend fun getDobAndGenderById(memberId: Long): MemberDobGenderModel

    suspend fun getAllUnSyncedHouseHolds(hhIds: List<String>): List<HouseHold>

    suspend fun getAllUnSyncedHouseHoldMembers(
        houseHoldId: Long,
        memberIds: List<Long> = listOf(),
    ): List<HouseHoldMember>

    suspend fun getOtherHouseholdMembers(memberIds: List<String>): List<HouseHoldMember>

    suspend fun updateFhirId(
        tableName: String,
        id: String,
        fhirId: String?,
        status: String,
    )

    fun getFilteredHouseholdsLiveData(
        searchInput: String,
        filterByVillage: List<Long> = emptyList(),
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        filterByHhIds: List<Long> = emptyList(),
        sortOrder: HouseholdSortOrder = HouseholdSortOrder.DEFAULT,
    ): LiveData<List<HouseHoldEntityWithLastActivity>>

    suspend fun getNearestHealthFacility(): List<HealthFacilityEntity>

    suspend fun getUnSyncedHouseholdCount(): Int

    suspend fun getUnSyncedHouseholdMemberCount(): Int

    suspend fun getPatientVisitCountByType(
        type: String,
        hhmLocalId: Long,
    ): MemberClinicalEntity?

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
        clinicalDate: String?,
    )

    suspend fun getSummaryDetailMetaItems(type: String): List<MedicalReviewMetaItems>

    suspend fun deleteExaminationsComplaintsForAnc(type: String)

    fun getExaminationsComplaintsForAnc(
        category: String,
        type: String,
    ): LiveData<List<MedicalReviewMetaItems>>

    suspend fun deleteExaminationsList(menuType: String)

    suspend fun saveExaminationsList(examinationList: ArrayList<ExaminationListItems>)

    suspend fun insertLabourDelivery(symptomEntity: List<LabourDeliveryMetaEntity>)

    suspend fun deleteLabourDelivery()

    suspend fun getLabourDelivery(): List<LabourDeliveryMetaEntity>

    suspend fun getDiagnosisList(diagnosisType: String): List<DiseaseCategoryItems>

    suspend fun getDiagnosisList(): List<DiagnosisEntity>

    suspend fun getExaminationQuestionsByWorkFlow(workFlowType: String): ExaminationListItems

    suspend fun insertFollowUp(followUp: FollowUp): Long

    suspend fun deleteAllFollowUps()

    suspend fun deleteAllUnAssignedMember()

    suspend fun deleteAllCallHistory()

    suspend fun deleteAllCommunityProfiles()

    fun getFollowUpPatientListLiveData(
        type: String,
        search: String? = null,
        villageIds: List<Long> = listOf(),
        fromDate: String = "",
        toDate: String = "",
    ): LiveData<List<FollowUpPatientModel>>

    suspend fun getAllSubVillageIds(): List<Long>

    suspend fun getSubVillages(): List<SubVillageEntity>

    suspend fun deleteAllPregnancyDetails()

    suspend fun getPatientIdByFhirId(fhirId: String): String?

    suspend fun insertUpdatePregnancyDetailFromBE(pregnancyDetail: PregnancyDetail)

    suspend fun addCallHistory(
        oldFollowUp: FollowUp,
        history: FollowUpCall,
        newFollowUp: FollowUp? = null,
    )

    suspend fun deleteAllFollowUpCalls()

    suspend fun getFollowUpById(id: Long): FollowUp

    suspend fun getAllFollowUpRequests(): List<FollowUp>

    suspend fun getAllFollowUpCalls(id: Long): List<FollowUpCall>

    suspend fun getUnSyncedFollowUpCount(): Int

    suspend fun getUnSyncedCommunityProfileCount(): Int

    fun getExaminationsComplaintByTypeLiveData(category: String): LiveData<List<MedicalReviewMetaItems>>

    fun getHouseholdCardDetailLiveData(id: Long): LiveData<HouseholdCardDetail>

    fun getAllHouseHoldMembersLiveData(hhId: Long): LiveData<List<HouseholdMemberWithTb>>

    fun getAliveHouseHoldMembersLiveData(hhId: Long): List<HouseholdMemberEntity>

    suspend fun updateOtherDuplicateTickets(
        id: Long,
        followUp: FollowUp,
    )

    suspend fun updateDuplicateTicketsAsCompleted(
        id: Long,
        followUp: FollowUp,
    )

    suspend fun updateOtherFollowUpForWrongNumber(
        id: Long,
        fhirId: String,
    )

    suspend fun updateOnTreatmentStatus(
        id: Long,
        followUp: FollowUp,
        updatedAt: Long,
    )

    suspend fun changeHouseholdStatus(
        idList: List<String>,
        syncStatus: String,
    )

    suspend fun changeHouseholdMemberStatus(
        idList: List<String>,
        syncStatus: String,
    )

    suspend fun changeAssessmentStatus(
        idList: List<String>,
        syncStatus: String,
    )

    suspend fun changeFollowUpStatus(
        idList: List<Long>,
        syncStatus: String,
    )

    suspend fun changeFollowUpCallStatus(idList: List<Long>)

    suspend fun changeHHMLinkCallStatus(
        idList: List<String>,
        syncStatus: String,
    )

    suspend fun changeCommunityProfileStatus(
        idList: List<Long>,
        syncStatus: String,
    )

    suspend fun changeAssignHHMStatus(
        idList: List<String>,
        syncStatus: String,
    )

    suspend fun insertOrUpdateHHFromBE(entity: HouseholdEntity): Long

    suspend fun insertOrUpdateHHMFromBE(entity: HouseholdMemberEntity): Long

    suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail?

    suspend fun savePregnancyDetail(detail: PregnancyDetail): Long

    fun getExaminationsComplaintsForPnc(
        category: String,
        type: String,
    ): LiveData<List<MedicalReviewMetaItems>>

    suspend fun deleteAllFrequencyList()

    suspend fun saveFrequencyList(frequencyList: List<FrequencyEntity>): List<Long>

    suspend fun saveUnitMetric(list: ArrayList<UnitMetricEntity>)

    suspend fun getFrequencyList(): List<FrequencyEntity>

    suspend fun getInstructionList(): List<MedicalReviewMetaItems>

    suspend fun getDosageFrequencyList(): List<DosageFrequency>

    suspend fun insertOrUpdateFollowUp(entity: FollowUp)

    suspend fun deleteCompletedFollowUp()

    suspend fun saveForm(forms: FormEntity)

    suspend fun updateNeonatePatientId(
        hhmLocalId: Long,
        neonateId: Long,
    )

    suspend fun getMemberDetailsByPatientId(patientId: String): HouseholdMemberEntity?

    suspend fun getChildPatientId(parentId: Long): Long?

    suspend fun getUserHealthFacility(isUserSite: Boolean): ArrayList<HealthFacilityEntity>

    suspend fun updateMemberDeceasedStatus(
        id: Long,
        status: Boolean,
    )

    suspend fun getPatientIdById(id: Long): String

    suspend fun insertConsentForm(form: ConsentForm): Long

    suspend fun getConsentFormByType(type: String): ConsentForm?

    suspend fun deleteAllConsentForm()

    suspend fun getHHSignatureDetails(): List<HHSignatureDetail>

    suspend fun updatePhoneNumberForHouseholdHead(
        id: Long,
        phoneNumber: String?,
        phoneNumberCategory: String?,
    )

    suspend fun insertLinkHouseholdMembers(insertList: List<LinkHouseholdMember>)

    suspend fun deleteLinkHouseholdMembersById(deleteListIds: List<String>)

    fun getUnAssignedHouseholdMembersLiveData(): LiveData<List<UnAssignedHouseholdMemberDetail>>

    suspend fun addLinkMemberCall(callHistory: CallHistory): Long

    suspend fun getUnSyncedCallHistoryForHHMLink(): List<HouseholdMemberCallRegisterDto>

    suspend fun changeMemberDetailsToNotSynced(id: Long)

    suspend fun updateMemberAsAssigned(memberId: String)

    suspend fun saveConsent(consentEntity: ConsentEntity)

    fun getConsent(formType: String): LiveData<String>

    suspend fun getConsentString(formType: String): String

    suspend fun deleteConsent()

    suspend fun saveModelQuestions(mentalHealthEntity: List<MentalHealthEntity>)

    suspend fun getModelQuestions(formType: String): MentalHealthEntity

    suspend fun deleteModelQuestions()

    suspend fun saveMedicalCompliance(list: List<MedicalComplianceEntity>)

    suspend fun getMedicalParentComplianceList(): List<MedicalComplianceEntity>

    suspend fun getMedicalChildComplianceList(parentId: Long): List<MedicalComplianceEntity>

    suspend fun deleteMedicalCompliance()

    suspend fun saveDistricts(districts: List<DistrictEntity>)

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

    suspend fun savePatientScreeningInformation(screeningEntity: ScreeningEntity): ScreeningEntity

    fun getScreenedPatientCount(
        startDate: Long,
        endDate: Long,
        userId: String,
    ): LiveData<Long>

    fun getScreenedPatientReferredCount(
        startDate: Long,
        endDate: Long,
        userId: String,
        isReferred: Boolean,
    ): LiveData<Long>

    suspend fun getAllScreeningRecords(uploadStatus: Boolean): List<ScreeningEntity>?

    suspend fun deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds: Long)

    suspend fun updateScreeningRecordById(
        id: Long,
        uploadStatus: Boolean,
    )

    suspend fun insertRiskFactor(riskFactorEntity: RiskFactorEntity)

    fun getRiskFactorEntity(): LiveData<List<RiskFactorEntity>>

    suspend fun getAllRiskFactorEntityList(): List<RiskFactorEntity>

    suspend fun deleteRiskFactor()

    fun getSymptomListByTypeForNCD(type: String): LiveData<List<SignsAndSymptomsEntity>>

    suspend fun deleteTreatmentPlan()

    suspend fun insertTreatmentPlan(items: List<TreatmentPlanEntity>)

    suspend fun deleteNCDMedicalReviewMeta()

    suspend fun insertNCDMedicalReviewMeta(items: List<NCDMedicalReviewMetaEntity>)

    suspend fun deleteLifestyle()

    suspend fun insertLifestyle(items: List<LifestyleEntity>)

    fun getComorbidities(
        type: String?,
        category: String,
    ): LiveData<List<NCDMedicalReviewMetaEntity>>

    fun getLifeStyle(): LiveData<List<LifestyleEntity>>

    suspend fun getLifeStyleList(): List<LifestyleEntity>

    fun getAssessmentFormData(
        formTypes: List<String>,
        workFlow: String,
    ): List<String>

    fun getAssessmentFormData(
        formType: String,
        workFlow: String,
    ): LiveData<String>

    suspend fun getSymptomList(): List<SignsAndSymptomsEntity>

    suspend fun saveAssessmentInformation(assessmentEntity: AssessmentNCDEntity): AssessmentNCDEntity

    suspend fun getAllAssessmentRecords(uploadStatus: Boolean): List<AssessmentNCDEntity>

    suspend fun updateAssessmentUploadStatus(
        id: Long,
        uploadStatus: Boolean,
    )

    suspend fun deleteAssessmentList(isUploaded: Boolean)

    suspend fun getAssessmentClinicalWorkflow(
        gender: String,
        name: String,
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
        isPregnant: Boolean,
    ): LiveData<List<NCDDiagnosisEntity>>

    fun getFrequencies(): LiveData<List<TreatmentPlanEntity>>

    suspend fun getNCDShortageReason(type: String): List<ShortageReasonEntity>

    suspend fun deleteNCDShortageReason()

    suspend fun saveNCDShortageReason(shortageReasonEntity: List<ShortageReasonEntity>)

    suspend fun getUnAssignedParentFhirId(parentId: String): List<HouseholdMemberFhirId>

    suspend fun getUnAssignedChildFhirIds(patientId: String): List<HouseholdMemberFhirId>

    suspend fun updateHouseholdHeadAndRelationShip(
        fhirIds: List<String>,
        householdId: Long,
    )

    suspend fun updateMembersAsAssigned(fhirIds: List<String>)

    suspend fun getNCDForm(
        type: String,
        customizedType: String,
    ): List<String>

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
        reason: String?,
    ): LiveData<List<NCDFollowUp>>

    suspend fun updatedCallInitiatedCall(ncdFollowUp: NCDFollowUp): NCDFollowUp

    suspend fun getNCDInitiatedCallFollowUp(): NCDFollowUp?

    suspend fun insertNCDCallDetails(followUp: NCDCallDetails): NCDCallDetails?

    suspend fun updateRetryAttempts(
        id: Long,
        retryAttempts: Long,
    )

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

    suspend fun updateMemberDeceasedReason(
        id: Long,
        status: Boolean,
        deceasedReason: String?,
    )

    suspend fun getHouseholdHeadDob(householdId: Long): String

    fun getFilterVillagesWithHouseholdsCount(searchInput: String): LiveData<List<CommunityProfileDetail>>

    suspend fun getCommunityStatistics(villageId: Long): CommunityPopulationStatistics

    suspend fun insertCommunityDetails(communityProfile: CommunityProfile): Long

    suspend fun getCommunityDetails(id: Long): CommunityProfile?

    suspend fun getCommunityProfileId(villageId: Long): Long?

    suspend fun updateCommunityDetails(communityProfile: CommunityProfile)

    suspend fun updateUnSynStatus(
        villageId: Long,
        synStatus: String,
    )

    suspend fun getHealthFacilityBasedOnVillageId(villageId: Long): List<HealthFacilityEntity>

    suspend fun getAssessment(assessmentId: Long): AssessmentEntity

    suspend fun getUnSyncedCommunityDetails(): List<CommunityProfile>

    suspend fun insertOrUpdateFromBE(communityProfile: CommunityProfile): Long

    fun householdMemberWithTbStatus(hhId: Long): LiveData<List<HouseholdMemberEntity>>

    suspend fun updateTBContactTraceStatus(
        hhmId: Long,
        tbContactTracingStatus: Int,
    )

    suspend fun updatePregnantStatus(
        memberId: Long,
        isPregnant: Boolean,
    )

    suspend fun getSymptomListByTypes(types: List<String>): List<SignsAndSymptomsEntity>

    suspend fun insertRxBuddyDetails(rxBuddyDetails: RxBuddyDetails): Long

    suspend fun getRxBuddyDetails(patientMemberId: String): RxBuddyDetails?

    suspend fun getOtherHouseholdExcludeTBPatient(
        householdId: Long,
        patientId: Long,
    ): List<HouseholdMemberEntity>

    suspend fun insertTreatmentDetails(treatmentDetails: TreatmentDetailsEntity): Long

    suspend fun updateTreatmentDetails(treatmentDetails: TreatmentDetailsEntity): Int

    suspend fun getTreatmentDetails(memberId: String): TreatmentDetailsEntity?

    suspend fun insertRxBuddyFollowUp(rxBuddyFollowUp: RxBuddyFollowUpEntity): Long

    suspend fun getHivMetaData(): List<MedicalReviewMetaItems>

    suspend fun getAllUnSyncedRxBuddyRegister(): List<RxBuddyRegisterDetail>

    suspend fun getHouseholdMemberForRxBuddy(hhmId: Long): HouseHoldMember

    suspend fun getUnSyncedRxBuddyFollowUpWithoutRxBuddyId(rxBuddyLocalId: Long): List<RxBuddyFollowUpEntity>

    suspend fun getUnSyncedRxBuddyFollowUpWithRxBuddyId(): List<RxBuddyFollowUpDetails>

    suspend fun updateNextVisitDateRxBuddyRegister(
        nextVisitDate: String,
        id: Long,
    )

    suspend fun updateNextVisitDateRxBuddyFollowUp(
        nextVisitDate: String,
        id: Long,
    )

    suspend fun insertOrUpdateRxBuddyFromBE(entity: RxBuddyDetails): Long

    suspend fun deleteAllTreatmentDetails()

    suspend fun deleteAllRxBuddyDetails()

    suspend fun deleteAllRxBuddyFollowUp()

    suspend fun getUnSyncedRxBuddyRegisterCount(): Int

    suspend fun getUnSyncedRxBuddyFollowUpCount(): Int

    suspend fun updateRxBuddyRegisterSyncStatus(
        idList: List<Long>,
        syncStatus: String,
    )

    suspend fun updateRxBuddyFollowUpSyncStatus(
        idList: List<Long>,
        syncStatus: String,
    )

    suspend fun deleteDisableRxBuddies(ids: List<Long>)

    suspend fun getHouseholdMemberIdAndStatusByFhirId(fhirId: String): HouseholdMemberStatus?

    suspend fun getUnSyncedHouseHoldByMemberId(hhmId: Long): HouseHold?

    suspend fun getMemberFhirIdByLocalId(hhmId: Long): String?

    suspend fun getAllUnSyncedRxBuddyDetailWithHHM(
        hhmId: Long,
        rxBuddiesId: List<Long>,
    ): List<RxBuddyRegisterDetail>

    suspend fun getTbPatientLocalIdByHouseholdId(householdId: Long): MutableList<Long>

    suspend fun updateContactTracingStatus(
        memberId: Long,
        status: Int?,
    )

    suspend fun updateContactTracingForLinkTbPatient(
        tbHHMId: Long,
        householdId: Long,
    )

    suspend fun getHouseholdsCountBasedSubVillage(subVillageId: Long): Int

    suspend fun getDisabilityMembersCountForHousehold(householdId: Long): Int

    /**
     * Updates no of disabilities with actual count disability members for given household
     * where no of disabilities is less than actual members
     */
    suspend fun updateDisabilityPersonsCountIfUnderCounted(householdId: Long)

    suspend fun updateGuardianHhIds(): Int

    /**
     * Updates no of people with actual count of members in household
     * where no of people is less than actual members
     */
    suspend fun updateUndercountedHouseholds(): Int

    /**
     * Updates no of disabilities with actual count disability members in household
     * where no of disabilities is less than actual members
     */
    suspend fun updateUndercountedDisabilityHouseholds(): Int

    fun getServiceMembers(
        searchInput: String,
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        staticFilter: ServiceStaticFilter,
        allowNullHousehold: Boolean = false,
    ): LiveData<List<HouseholdMemberWithTb>>

    /**
     * Returns aggregated counts for all service static filters in one pass.
     *
     * Dynamic filters are identical to [getServiceMembers] so list and counters stay aligned.
     */
    suspend fun getAllServiceMemberCounts(
        searchInput: String = "",
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        allowNullHousehold: Boolean = false,
    ): ServiceMemberCounts

    suspend fun getMemberAssessmentHistory(
        memberFhirId: String?,
        memberId: Long?,
        visitDate: String?,
        serviceProvided: String?,
    ): MemberAssessmentHistoryEntity?

    suspend fun insertMemberAssessmentHistory(historyList: List<MemberAssessmentHistoryEntity>)

    suspend fun deleteAllMemberAssessmentHistory()

    fun getMemberWithAssessmentHistory(memberId: Long): LiveData<MemberAssessmentHistoryResponse?>

    suspend fun getDashboardCounts(
        startDate: String?,
        endDate: String?,
        ssIds: List<Long>,
        subVillageIds: List<Long>,
    ): DashboardCountsRow

    suspend fun insertMemberAssessmentHistory(assessmentHistory: MemberAssessmentHistoryEntity): Long

    suspend fun updateMemberAssessmentHistory(assessmentHistory: MemberAssessmentHistoryEntity)

    suspend fun getLastServiceHistory(
        memberLocalId: Long,
        serviceTypeFor: String,
    ): MemberAssessmentHistoryEntity?

    suspend fun decrementAssessmentHistoryVisitDate(
        memberId: Long,
        noOfDays: Int,
    )

    suspend fun deleteDuplicateAssessmentHistory(date: String)

    suspend fun getFormBasedOnType(
        formTypeOne: String,
        formTypeTwo: String,
    ): List<FormEntity>

    suspend fun getVillageList(selectedParent: Long): List<VillageEntity>

    suspend fun getOtherVillage(): VillageEntity

    suspend fun getProgramList(site: String): Any

    suspend fun getProgramList(
        selectedParent: Long,
        site: String,
    ): Any

    suspend fun getUpazilaListByDistrict(districtID: Long): List<SiteEntity>

    suspend fun getUpazilaList(): Any

    suspend fun updateSequenceCode(
        villageId: Long,
        newSequenceCode: Long,
    )

    suspend fun getScreeningRecordById(id: Long): ScreeningEntity

    suspend fun getAccountSiteList(userId: Long): List<SiteEntity>

    suspend fun getAccountSiteListByLevel(
        userId: Long,
        level: String,
    ): List<SiteEntity>

    suspend fun getUpazilaListEyeCareOnly(): Any

    suspend fun getUpazilaListCataractOnly(): Any

    suspend fun getSiteEntity(
        userSite: Boolean,
        userId: Long,
    ): List<SiteEntity>

    suspend fun getUpazilaById(upazilaId: Long): SiteEntity?

    suspend fun getVillageById(villageId: Long): VillageEntity?

    suspend fun getTreatmentPlanData(): List<TreatmentPlanEntity>

    suspend fun getShortageReason(type: String): List<ShortageReasonEntity>

    suspend fun getComorbidityBasedOnWorkflow(workflowList: ArrayList<String>): List<ComorbidityEntity>

    suspend fun saveComorbidity(list: ArrayList<ComorbidityEntity>)

    suspend fun deleteComorbidity()

    suspend fun getComorbidity(): List<ComorbidityEntity>

    suspend fun getComplication(): List<ComplicationEntity>

    suspend fun saveComplication(list: ArrayList<ComplicationEntity>)

    suspend fun deleteComplication()

    suspend fun saveCurrentMedication(list: ArrayList<CurrentMedicationEntity>)

    suspend fun deleteCurrentMedication()

    suspend fun getCurrentMedicationList(): List<CurrentMedicationEntity>

    suspend fun getCurrentMedicationList(type: String): List<CurrentMedicationEntity>

    suspend fun savePhysicalExamination(list: ArrayList<PhysicalExaminationEntity>)

    suspend fun deletePhysicalExamination()

    suspend fun getPhysicalExaminationList(workFlowList: ArrayList<String>): List<PhysicalExaminationEntity>

    suspend fun saveCompliants(list: ArrayList<ComplaintsEntity>)

    suspend fun deleteCompliants()

    suspend fun getChiefComplaints(workflowList: ArrayList<String>): List<ComplaintsEntity>

    suspend fun getDiagnosis(
        gender: ArrayList<String>,
        type: ArrayList<String>,
    ): List<DiagnosisEntity>

    suspend fun saveSymptomList(symptoms: List<SymptomEntity>)

    suspend fun deleteSymptoms()

    suspend fun getSymptomsList(): List<SymptomEntity>

    suspend fun getSymptomsListByType(type: String): List<SymptomEntity>

    suspend fun getOperatingUnitSites(): List<SiteEntity>

    suspend fun saveDiagnosis(diseaseEntityList: ArrayList<DiagnosisEntity>)

    suspend fun deleteDiagnosisList()

    suspend fun deleteAllSiteCache()

    suspend fun saveSiteList(siteEntity: List<SiteEntity>)

    suspend fun getAllChiefDoms(): List<ChiefDomEntity>

    suspend fun getSubVillage(villageId: Long): List<SubVillageEntity>
}
