package org.medtroniclabs.uhis.db.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
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
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.dao.AboveFiveYearsDAO
import org.medtroniclabs.uhis.db.dao.AssessmentDAO
import org.medtroniclabs.uhis.db.dao.CallHistoryDao
import org.medtroniclabs.uhis.db.dao.CommunityDetailsDAO
import org.medtroniclabs.uhis.db.dao.ConsentFormDao
import org.medtroniclabs.uhis.db.dao.DiagnosisDAO
import org.medtroniclabs.uhis.db.dao.ExaminationsComplaintsDAO
import org.medtroniclabs.uhis.db.dao.ExaminationsDAO
import org.medtroniclabs.uhis.db.dao.FollowUpCallsDao
import org.medtroniclabs.uhis.db.dao.FollowUpDao
import org.medtroniclabs.uhis.db.dao.FrequencyDAO
import org.medtroniclabs.uhis.db.dao.HivMetaDataDAO
import org.medtroniclabs.uhis.db.dao.HouseholdDAO
import org.medtroniclabs.uhis.db.dao.HouseholdSortOrder
import org.medtroniclabs.uhis.db.dao.LabourDeliveryDAO
import org.medtroniclabs.uhis.db.dao.LinkHouseholdMemberDao
import org.medtroniclabs.uhis.db.dao.MemberAssessmentHistoryDao
import org.medtroniclabs.uhis.db.dao.MemberDAO
import org.medtroniclabs.uhis.db.dao.MetaDataDAO
import org.medtroniclabs.uhis.db.dao.NCDFollowUpDao
import org.medtroniclabs.uhis.db.dao.NcdMedicalReviewDao
import org.medtroniclabs.uhis.db.dao.PregnancyDetailDao
import org.medtroniclabs.uhis.db.dao.RiskFactorDAO
import org.medtroniclabs.uhis.db.dao.RxBuddyDetailsDAO
import org.medtroniclabs.uhis.db.dao.RxBuddyFollowUpDAO
import org.medtroniclabs.uhis.db.dao.ScreeningDAO
import org.medtroniclabs.uhis.db.dao.TreatmentDetailsDAO
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
import org.medtroniclabs.uhis.db.entity.EntitiesName
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
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.boarding.MenuTypeEnums
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import javax.inject.Inject

class RoomHelperImpl @Inject constructor(
    private val householdDAO: HouseholdDAO,
    private val memberDAO: MemberDAO,
    private val assessmentDAO: AssessmentDAO,
    private val metaDataDAO: MetaDataDAO,
    private val examinationsComplaintsDAO: ExaminationsComplaintsDAO,
    private val diagnosisDAO: DiagnosisDAO,
    private val aboveFiveYearsDAO: AboveFiveYearsDAO,
    private val examinationsDAO: ExaminationsDAO,
    private val labourDeliveryDAO: LabourDeliveryDAO,
    private val followUpDao: FollowUpDao,
    private val followUpCallsDao: FollowUpCallsDao,
    private val pregnancyDetailDao: PregnancyDetailDao,
    private val frequencyDAO: FrequencyDAO,
    private val consentFormDao: ConsentFormDao,
    private val linkHouseholdMemberDao: LinkHouseholdMemberDao,
    private val callHistoryDao: CallHistoryDao,
    private val screeningDAO: ScreeningDAO,
    private val riskFactorDAO: RiskFactorDAO,
    private val ncdMedicalReviewDao: NcdMedicalReviewDao,
    private val ncdFollowUpDao: NCDFollowUpDao,
    private val communityDAO: CommunityDetailsDAO,
    private val rxBuddyDetailsDAO: RxBuddyDetailsDAO,
    private val treatmentDetailsDAO: TreatmentDetailsDAO,
    private val rxBuddyFollowUpDAO: RxBuddyFollowUpDAO,
    private val hivMetaDataDAO: HivMetaDataDAO,
    private val memberAssessmentHistoryDao: MemberAssessmentHistoryDao,
) : RoomHelper {
    override suspend fun saveHouseHoldEntry(householdEntity: HouseholdEntity): Long = householdDAO.insertHouseHold(householdEntity)

    override suspend fun updateHousehold(householdEntity: HouseholdEntity) = householdDAO.updateHouseHold(householdEntity)

    override suspend fun getLastHouseholdNo(villageId: Long): Long? = householdDAO.getLastHouseholdNo(villageId)

    override suspend fun checkHouseholdNumberExists(householdNo: Long): Boolean = householdDAO.checkHouseholdNumberExists(householdNo) > 0

    override suspend fun getHouseHoldDetailsById(houseHoldId: Long): HouseholdEntity = householdDAO.getHouseHoldDetailsById(houseHoldId)

    override fun getMemberCountInHouseholdLiveData(houseHoldId: Long): LiveData<HouseholdMemberCount> =
        householdDAO.getHouseholdMemberCountLiveData(houseHoldId)

    override suspend fun registerMember(householdMemberEntity: HouseholdMemberEntity): Long = memberDAO.insertMember(householdMemberEntity)

    override suspend fun getAllHouseHoldMemberList(houseHoldId: Long): ArrayList<HouseholdMemberEntity> =
        ArrayList(memberDAO.getAllHouseHoldMemberList(houseHoldId))

    override suspend fun getMemberDetailsByID(memberId: Long): HouseholdMemberEntity = memberDAO.getMemberDetailsById(memberId)

    override suspend fun getMemberDetailsByParentId(memberId: String): List<HouseholdMemberEntity> = memberDAO.getMemberDetailsByParentId(memberId)

    override suspend fun getMemberCountPerHouseHold(householdId: Long): Int = memberDAO.getMemberCountPerHouseHold(householdId)

    override suspend fun getLastPatientId(patientIdStarts: String): String? = memberDAO.getLastPatientId(patientIdStarts)

    override suspend fun getAllNationalIds(idType: String): List<String> = memberDAO.getAllNationalIds(idType)

    override suspend fun getAllUnSyncedHouseHolds(hhIds: List<String>): List<HouseHold> = householdDAO.getAllUnSyncedHouseHolds(hhIds)

    override suspend fun getAllUnSyncedHouseHoldMembers(
        houseHoldId: Long,
        memberIds: List<Long>,
    ): List<HouseHoldMember> = memberDAO.getAllUnSyncedHouseHoldMembers(houseHoldId, memberIds)

    override suspend fun getOtherHouseholdMembers(memberIds: List<String>): List<HouseHoldMember> = memberDAO.getOtherHouseholdMembers(memberIds)

    override suspend fun saveAssessment(assessmentEntity: AssessmentEntity): Long = assessmentDAO.insertAssessment(assessmentEntity)

    override suspend fun updateOtherAssessmentDetails(assessmentEntity: AssessmentEntity) = assessmentDAO.updateOtherAssessmentDetails(assessmentEntity)

    override suspend fun getLatestAssessmentForMember(memberId: Long): AssessmentEntity? = assessmentDAO.getLatestAssessmentForMember(memberId)

    override suspend fun insertSymptomList(symptoms: List<SignsAndSymptomsEntity>) {
        assessmentDAO.insertSymptoms(symptoms)
    }

    override suspend fun getSymptomListByType(type: String): List<SignsAndSymptomsEntity> = assessmentDAO.getSymptomListByType(type)

    override suspend fun saveHealthFacility(healthFacilityEntityList: HealthFacilityEntity) {
        metaDataDAO.insertHealthFacility(healthFacilityEntityList)
    }

    override suspend fun updateHeadCountIfUnderCounted(householdId: Long) = householdDAO.updateHeadCountIfUnderCounted(householdId)

    override suspend fun deleteAllHealthFacility() {
        metaDataDAO.deleteAllHealthFacility()
    }

    override suspend fun saveVillage(villageEntityList: List<VillageEntity>) {
        metaDataDAO.insertVillages(villageEntityList)
    }

    override suspend fun getAllVillageEntity(): List<VillageEntity> = metaDataDAO.getVillages()

    override suspend fun getAllLinkedVillageEntity(): List<VillageEntity> = metaDataDAO.getLinkedVillages(SecuredPreference.getTenantId())

    override suspend fun getVillagesByChiefDom(chiefdomId: Long): List<VillageEntity> = metaDataDAO.getVillagesByChiefDom(chiefdomId)

    override suspend fun saveSubVillages(subVillageEntityList: List<SubVillageEntity>) {
        metaDataDAO.insertSubVillages(subVillageEntityList)
    }

    override suspend fun deleteAllSubVillages() {
        metaDataDAO.deleteAllSubVillages()
    }

    override suspend fun saveShasthyaShebikas(shasthyaShebikaEntityList: List<ShasthyaShebikaEntity>) {
        metaDataDAO.insertShasthyaShebikas(shasthyaShebikaEntityList)
    }

    override suspend fun deleteAllShasthyaShebikas() {
        metaDataDAO.deleteAllShasthyaShebikas()
    }

    override suspend fun getShasthyaShebikaByShasthyaKormiId(shasthyaKormiId: Long): List<ShasthyaShebikaEntity> =
        metaDataDAO.getShasthyaShebikaByShasthyaKormiId(shasthyaKormiId)

    override suspend fun getShasthyaShebikaById(id: Long): ShasthyaShebikaEntity? = metaDataDAO.getShasthyaShebikaById(id)

    override suspend fun saveChiefdoms(chiefdomEntityList: List<ChiefDomEntity>) {
        metaDataDAO.insertChiefDoms(chiefdomEntityList)
    }

    override suspend fun deleteAllChiefdoms() {
        metaDataDAO.deleteChiefDoms()
    }

    override suspend fun getChiefdomByVillageId(villageId: Long): List<ChiefDomEntity> = metaDataDAO.getChiefdomByVillageId(villageId)

    override suspend fun getAllChiefdoms(): List<ChiefDomEntity> = metaDataDAO.getAllChiefdoms()

    override suspend fun getChiefdomByShasthyaKormiId(shasthyaKormiId: Long): List<ChiefDomEntity> = metaDataDAO.getChiefdomByShasthyaKormiId(shasthyaKormiId)

    override suspend fun getChiefdomByShasthyaKormiIds(shasthyaKormiIds: List<Long>): List<ChiefDomEntity> =
        if (shasthyaKormiIds.isEmpty()) {
            emptyList()
        } else {
            metaDataDAO.getChiefdomByShasthyaKormiIds(shasthyaKormiIds)
        }

    override suspend fun getChiefdomById(chiefdomId: Long): ChiefDomEntity? = metaDataDAO.getChiefdomById(chiefdomId)

    override suspend fun saveShasthyaKormis(shasthyaKormiEntityList: List<ShasthyaKormiEntity>) {
        metaDataDAO.insertShasthyaKormis(shasthyaKormiEntityList)
    }

    override suspend fun deleteAllShasthyaKormis() {
        metaDataDAO.deleteAllShasthyaKormis()
    }

    override suspend fun getShasthyaKormiByVillageId(villageId: Long): List<ShasthyaKormiEntity> = metaDataDAO.getShasthyaKormiByVillageId(villageId)

    override suspend fun getAllShasthyaKormis(): List<ShasthyaKormiEntity> = metaDataDAO.getAllShasthyaKormis()

    override suspend fun insertShasthyaKormiLinkedVillages(linkedVillages: List<ShasthyaKormiLinkedVillageEntity>) {
        metaDataDAO.insertShasthyaKormiLinkedVillages(linkedVillages)
    }

    override suspend fun deleteAllShasthyaKormiLinkedVillages() {
        metaDataDAO.deleteAllShasthyaKormiLinkedVillages()
    }

    override suspend fun getSubVillagesByShasthyaKormiId(shasthyaKormiId: Long): List<SubVillageEntity> =
        metaDataDAO.getSubVillagesByShasthyaKormiId(shasthyaKormiId)

    override suspend fun getSubVillagesByShasthyaKormiIds(shasthyaKormiIds: List<Long>): List<SubVillageEntity> =
        if (shasthyaKormiIds.isEmpty()) {
            emptyList()
        } else {
            metaDataDAO.getSubVillagesByShasthyaKormiIds(shasthyaKormiIds)
        }

    override suspend fun getVillagesForShasthyaKormiAndChiefdom(
        shasthyaKormiId: Long,
        chiefdomId: Long,
    ): List<VillageEntity> = metaDataDAO.getVillagesForShasthyaKormiAndChiefdom(shasthyaKormiId, chiefdomId)

    override suspend fun insertShasthyaShebikaLinkedVillages(linkedVillages: List<ShasthyaShebikaLinkedVillageEntity>) {
        metaDataDAO.insertShasthyaShebikaLinkedVillages(linkedVillages)
    }

    override suspend fun deleteAllShasthyaShebikaLinkedVillages() {
        metaDataDAO.deleteAllShasthyaShebikaLinkedVillages()
    }

    override suspend fun getSubVillagesByShasthyaShebikaId(shasthyaShebikaId: Long): List<SubVillageEntity> =
        metaDataDAO.getSubVillagesByShasthyaShebikaId(shasthyaShebikaId)

    override suspend fun getSubVillagesByShasthyaShebikaIds(shasthyaShebikaIds: List<Long>): List<SubVillageEntity> =
        metaDataDAO.getSubVillagesByShasthyaShebikaIds(shasthyaShebikaIds)

    override suspend fun getDefaultHealthFacility(): HealthFacilityEntity? = metaDataDAO.getDefaultHealthFacility()

    override suspend fun getClinicalWorkflowId(
        gender: String,
        age: Int,
    ): List<NCDAssessmentClinicalWorkflow> = metaDataDAO.getClinicalWorkflowId(gender, age, MenuTypeEnums.assessment.name)

    override suspend fun deleteAllVillages() {
        metaDataDAO.deleteAllVillages()
    }

    override suspend fun deleteAllHouseholds() {
        householdDAO.deleteAllHouseholds()
    }

    override suspend fun deleteAllHouseholdMembers() {
        memberDAO.deleteAllHouseholdMembers()
    }

    override suspend fun saveMenus(menuEntity: MenuEntity) {
        metaDataDAO.insertMenus(menuEntity)
    }

    override suspend fun saveClinicalWorkflows(clinicalWorkflows: List<ClinicalWorkflowEntity>) = metaDataDAO.saveClinicalWorkflows(clinicalWorkflows)

    override suspend fun deleteAllClinicalWorkflow() = metaDataDAO.deleteAllClinicalWorkflow()

    override suspend fun saveForms(forms: List<FormEntity>) = metaDataDAO.saveForms(forms)

    override suspend fun deleteAllForms() = metaDataDAO.deleteAllForms()

    override suspend fun getAllClinicalWorkflowIds(): List<Int> = metaDataDAO.getAllClinicalWorkflowIds()

    override suspend fun insertSymptoms(symptomEntity: List<SignsAndSymptomsEntity>) {
        metaDataDAO.insertSymptoms(symptomEntity)
    }

    override suspend fun deleteAllSymptoms() {
        metaDataDAO.deleteAllSymptoms()
    }

    override suspend fun getFormData(formType: String): String = metaDataDAO.getFormData(formType)

    override suspend fun deleteAllMenus() {
        metaDataDAO.deleteAllMenus()
    }

    override suspend fun saveUserProfileDetails(userProfileEntity: UserProfileEntity) {
        metaDataDAO.insertUserProfileDetails(userProfileEntity)
    }

    override suspend fun deleteAllUserProfileDetails() {
        metaDataDAO.deleteAllUserProfileDetails()
    }

    override suspend fun getMenus(): List<MenuEntity> = metaDataDAO.getMenus()

    override suspend fun getUserProfile(): UserProfileEntity = metaDataDAO.getUserProfile()

    override suspend fun getVillageByID(villageId: Long): VillageEntity = metaDataDAO.getVillageByID(villageId)

    override suspend fun getMenuForClinicalWorkflows(): List<ClinicalWorkflowEntity> = metaDataDAO.getMenuForClinicalWorkflows()

    override suspend fun deleteClinicalWorkflowConditions() {
        metaDataDAO.deleteClinicalWorkflowConditions()
    }

    override suspend fun insertClinicalWorkflowConditions(clinicalWorkflowConditions: List<ClinicalWorkflowConditionEntity>) {
        metaDataDAO.insertClinicalWorkflowConditions(clinicalWorkflowConditions)
    }

    override suspend fun getDobAndGenderById(memberId: Long): MemberDobGenderModel = memberDAO.getDobAndGenderById(memberId)

    override suspend fun updateFhirId(
        tableName: String,
        id: String,
        fhirId: String?,
        status: String,
    ) {
        when (tableName) {
            EntitiesName.RX_BUDDY -> {
                fhirId?.let {
                    rxBuddyDetailsDAO.updateRxBuddyId(id = id.toLong(), rxBuddyId = it.toLong())
                    rxBuddyFollowUpDAO.updateRxBuddyId(id = id.toLong(), rxBuddyId = it.toLong())
                }
            }

            EntitiesName.HOUSEHOLD_MEMBER,
            EntitiesName.HOUSEHOLD,
            -> {
                val query =
                    "UPDATE $tableName SET fhir_id = ?, sync_status = CASE WHEN sync_status = 'InProgress' THEN ? WHEN sync_status = 'NetworkError' THEN ? ELSE sync_status END WHERE id = ?"
                householdDAO.updateFhirId(
                    SimpleSQLiteQuery(
                        query,
                        arrayOf(fhirId, status, status, id),
                    ),
                )
            }

            else -> {
                val updatedAt = System.currentTimeMillis()
                val query =
                    "UPDATE $tableName SET fhir_id = ?, updated_at = ?, sync_status = CASE WHEN sync_status = 'InProgress' THEN ? WHEN sync_status = 'NetworkError' THEN ? ELSE sync_status END WHERE id = ?"
                householdDAO.updateFhirId(
                    SimpleSQLiteQuery(
                        query,
                        arrayOf<Any?>(fhirId, updatedAt, status, status, id),
                    ),
                )
            }
        }
    }

    override fun getFilteredHouseholdsLiveData(
        searchInput: String,
        filterByVillage: List<Long>,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        filterByHhIds: List<Long>,
        sortOrder: HouseholdSortOrder,
    ): LiveData<List<HouseHoldEntityWithLastActivity>> =
        householdDAO.getHouseholdsWithLastActivity(
            searchTerm = searchInput,
            villageIds = filterByVillage,
            shasthyaShebikaIds = filterBySs,
            subVillageIds = filterBySubVillages,
            hhIds = filterByHhIds,
            sortOrder = sortOrder,
        )

    override suspend fun getUnSyncedHouseholdCount(): Int = householdDAO.getUnSyncedCount()

    override suspend fun getUnSyncedHouseholdMemberCount(): Int = memberDAO.getUnSyncedCount()

    override suspend fun getNearestHealthFacility(): List<HealthFacilityEntity> = metaDataDAO.getNearestHealthFacility()

    override suspend fun getPatientVisitCountByType(
        type: String,
        hhmLocalId: Long,
    ): MemberClinicalEntity? =
        when (type) {
            RMNCH.ANC -> pregnancyDetailDao.getAncDetail(hhmLocalId)
            RMNCH.PNC -> pregnancyDetailDao.getPncDetail(hhmLocalId)
            else -> pregnancyDetailDao.getChildhoodVisitDetail(hhmLocalId)
        }

    override suspend fun savePatientVisitCountByType(memberClinicalEntity: MemberClinicalEntity) {
        //  return memberClinicalDAO.savePatientVisitCountByType(memberClinicalEntity = memberClinicalEntity)
    }

    override suspend fun deleteExaminationsComplaints(menuType: String) {
        examinationsComplaintsDAO.deleteExaminationsComplaints(menuType)
    }

    override suspend fun insertExaminationsComplaint(symptomEntity: List<MedicalReviewMetaItems>) {
        examinationsComplaintsDAO.insertExaminationsComplaints(symptomEntity)
    }

    override suspend fun deleteDiagnosisList(diagnosisType: String) {
        diagnosisDAO.deleteDiagnosisList(diagnosisType)
    }

    override suspend fun saveDiagnosisList(diagnosisList: ArrayList<DiseaseCategoryItems>) {
        diagnosisDAO.saveDiagnosisList(diagnosisList)
    }

    override suspend fun getHouseholdIdByFhirId(fhirId: String?): Long? =
        if (fhirId != null) {
            householdDAO.getHouseholdIdByFhirId(fhirId)
        } else {
            null
        }

    override suspend fun getHouseholdMemberIdByFhirId(fhirId: String?): Long? =
        if (fhirId != null) {
            memberDAO.getHouseholdMemberIdByFhirId(fhirId)
        } else {
            null
        }

    override suspend fun getExaminationsComplaintByType(type: String): List<MedicalReviewMetaItems> =
        examinationsComplaintsDAO.getExaminationsComplaintByType(type)

    override suspend fun getAssessmentMemberDetails(id: Long): AssessmentMemberDetails = memberDAO.getAssessmentMemberDetails(id)

    override suspend fun getOtherUnSyncedAssessments(addedAssessmentIds: List<String>): List<AssessmentDetails> =
        assessmentDAO.getOtherUnSyncedAssessments(addedAssessmentIds)

    override suspend fun getUnSyncedAssessmentByHHMId(hhmId: Long): List<AssessmentDetails> = assessmentDAO.getUnSyncedAssessmentByHHMId(hhmId)

    override suspend fun getUnSyncedAssessmentCount(): Int = assessmentDAO.getUnSyncedCount()

    override suspend fun updatePregnancyAncDetail(
        hhmLocalId: Long,
        visitCount: Long,
        clinicalDate: String?,
    ) {
        pregnancyDetailDao.updatePregnancyAnc(hhmLocalId)
    }

    override suspend fun getSummaryDetailMetaItems(type: String): List<MedicalReviewMetaItems> = aboveFiveYearsDAO.getSummaryDetailMetaItems(type)

    override suspend fun deleteExaminationsComplaintsForAnc(type: String) {
        examinationsComplaintsDAO.deleteExaminationsComplaintsForAnc(type)
    }

    override fun getExaminationsComplaintsForAnc(
        category: String,
        type: String,
    ): LiveData<List<MedicalReviewMetaItems>> = examinationsComplaintsDAO.getExaminationsComplaintsForAnc(category, type)

    override suspend fun deleteExaminationsList(menuType: String) {
        examinationsDAO.deleteExaminationsList(menuType)
    }

    override suspend fun saveExaminationsList(examinationList: ArrayList<ExaminationListItems>) {
        examinationsDAO.saveExaminationsList(examinationList)
    }

    override suspend fun insertLabourDelivery(symptomEntity: List<LabourDeliveryMetaEntity>) {
        labourDeliveryDAO.insertLabourDelivery(symptomEntity)
    }

    override suspend fun deleteLabourDelivery() {
        labourDeliveryDAO.deleteLabourDelivery()
    }

    override suspend fun getLabourDelivery(): List<LabourDeliveryMetaEntity> = labourDeliveryDAO.getLabourDelivery()

    override suspend fun getDiagnosisList(diagnosisType: String): List<DiseaseCategoryItems> = diagnosisDAO.getDiagnosisList(diagnosisType)

    override suspend fun getDiagnosisList(): List<DiagnosisEntity> = metaDataDAO.getDiagnosisList()

    override suspend fun insertFollowUp(followUp: FollowUp): Long = followUpDao.insertFollowUp(followUp)

    override suspend fun deleteAllFollowUps() {
        followUpDao.deleteAllFollowUps()
    }

    override suspend fun deleteAllUnAssignedMember() {
        linkHouseholdMemberDao.deleteAllLinkHouseholdMember()
    }

    override suspend fun deleteAllCallHistory() {
        callHistoryDao.deleteAllCallHistory()
    }

    override suspend fun deleteAllCommunityProfiles() {
        communityDAO.deleteAllCommunityProfiles()
    }

    override fun getFollowUpPatientListLiveData(
        type: String,
        search: String?,
        shashthyaShebikaIds: List<Long>,
        shashthyaShebikaIdsSize: Int,
        villageIds: List<Long>,
        villageIdsSize: Int,
        selectedReferralReasonTypes: List<String>,
        selectedReferralReasonTypesSize: Int,
        ncdSelectedReason: String?,
        ncdReferralFacility: String?,
        fromDate: String,
        toDate: String,
    ): LiveData<List<FollowUpPatientModel>> {
        if (type == FollowUpDefinedParams.FU_TYPE_REFERRED) {
            return followUpDao.getReferredFollowUpPatientListLiveData(
                type = type,
                search = search,
                shashthyaShebikaIds = shashthyaShebikaIds,
                shashthyaShebikaIdsSize = shashthyaShebikaIdsSize,
                villageIds = villageIds,
                villageIdsSize = villageIdsSize,
                selectedReferralReasonTypes = selectedReferralReasonTypes,
                selectedReferralReasonTypesSize = selectedReferralReasonTypesSize,
                ncdSelectedReason = ncdSelectedReason,
                fromDate = fromDate,
                toDate = toDate,
            )
        } else {
            return followUpDao.getOtherFollowUpPatientListLiveData(
                type = type,
                search = search,
                shashthyaShebikaIds = shashthyaShebikaIds,
                shashthyaShebikaIdsSize = shashthyaShebikaIdsSize,
                villageIds = villageIds,
                villageIdsSize = villageIdsSize,
                selectedReferralReasonTypes = selectedReferralReasonTypes,
                selectedReferralReasonTypesSize = selectedReferralReasonTypesSize,
                ncdSelectedReason = ncdSelectedReason,
                fromDate = fromDate,
                toDate = toDate,
            )
        }
    }

    override suspend fun getAllSubVillageIds(): List<Long> = metaDataDAO.getSubVillageIds()

    override suspend fun getSubVillages() = metaDataDAO.getSubVillages()

    override suspend fun getExaminationQuestionsByWorkFlow(workFlowType: String): ExaminationListItems = examinationsDAO.getExaminationsByType(workFlowType)

    override suspend fun getPatientIdByFhirId(fhirId: String): String? = memberDAO.getPatientIdByFhirId(fhirId)

    override suspend fun deleteAllPregnancyDetails() {
        pregnancyDetailDao.deleteAllPregnancyDetails()
    }

    override suspend fun insertUpdatePregnancyDetailFromBE(pregnancyDetail: PregnancyDetail) {
        pregnancyDetailDao.insertOrUpdateFromBE(pregnancyDetail)
    }

    override suspend fun addCallHistory(
        oldFollowUp: FollowUp,
        history: FollowUpCall,
        newFollowUp: FollowUp?,
    ) {
        followUpCallsDao.insertFollowUpCall(history)
        followUpDao.insertFollowUp(oldFollowUp)
        newFollowUp?.let {
            followUpDao.insertFollowUp(it)
        }
    }

    override suspend fun getAllFollowUpRequests(): List<FollowUp> = followUpDao.getAllFollowUps()

    override suspend fun getAllFollowUpCalls(id: Long): List<FollowUpCall> = followUpCallsDao.getAllFollowUpCalls(id)

    override suspend fun getFollowUpById(id: Long): FollowUp = followUpDao.getFollowUpDetailsById(id)

    override suspend fun deleteAllFollowUpCalls() = followUpCallsDao.deleteAllFollowUpCalls()

    override suspend fun getUnSyncedFollowUpCount(): Int = followUpDao.getUnSyncedCount()

    override suspend fun getUnSyncedCommunityProfileCount(): Int = communityDAO.getUnSyncedCount()

    override suspend fun deleteAllAssessments() = assessmentDAO.deleteAllAssessments()

    override fun getExaminationsComplaintByTypeLiveData(category: String): LiveData<List<MedicalReviewMetaItems>> =
        examinationsComplaintsDAO.getExaminationsComplaintByTypeLiveData(category)

    override fun getHouseholdCardDetailLiveData(id: Long): LiveData<HouseholdCardDetail> = householdDAO.getHouseholdCardDetailLiveData(id)

    override fun getAllHouseHoldMembersLiveData(hhId: Long): LiveData<List<HouseholdMemberWithTb>> = memberDAO.getAllHouseHoldMembersLiveData(hhId)

    override fun getAliveHouseHoldMembersLiveData(hhId: Long): List<HouseholdMemberEntity> = memberDAO.getAliveHouseHoldMembers(hhId, true)

    override suspend fun updateOtherDuplicateTickets(
        id: Long,
        followUp: FollowUp,
    ) {
        followUpDao.updateOtherDuplicateTickets(
            id,
            followUp.memberId,
            followUp.type,
            followUp.encounterType,
            followUp.reason,
        )
    }

    override suspend fun updateDuplicateTicketsAsCompleted(
        id: Long,
        followUp: FollowUp,
    ) {
        if (followUp.type == FollowUpDefinedParams.FU_TYPE_HH_VISIT) {
            followUpDao.updateHHVisitTicketsOnRecovered(id, followUp.memberId, followUp.type, followUp.encounterType, followUp.reason)
        } else {
            val types = listOf(FollowUpDefinedParams.FU_TYPE_REFERRED, FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW)
            if (followUp.encounterType == FollowUpDefinedParams.FU_ENCOUNTER_TYPE_RMNCH) {
                followUpDao.closeTicketsForRMNCH(id, followUp.memberId, types, followUp.encounterType)
            } else {
                followUpDao.closeTicketsForNonRMNCH(id, followUp.memberId, followUp.type, types, followUp.encounterType)
            }
        }
    }

    override suspend fun updateOnTreatmentStatus(
        id: Long,
        followUp: FollowUp,
        updatedAt: Long,
    ) {
        followUpDao.updateOnTreatmentStatus(
            id,
            followUp.memberId,
            followUp.type,
            updatedAt,
            followUp.encounterType,
            followUp.reason,
        )
    }

    override suspend fun insertOrUpdateHHFromBE(entity: HouseholdEntity): Long = householdDAO.insertOrUpdateFromBE(entity)

    override suspend fun insertOrUpdateHHMFromBE(entity: HouseholdMemberEntity): Long = memberDAO.insertOrUpdateFromBE(entity)

    override suspend fun changeHouseholdStatus(
        idList: List<String>,
        syncStatus: String,
    ) {
        householdDAO.updateInProgress(idList, syncStatus)
    }

    override suspend fun changeHouseholdMemberStatus(
        idList: List<String>,
        syncStatus: String,
    ) {
        memberDAO.updateInProgress(idList, syncStatus)
    }

    override suspend fun changeAssessmentStatus(
        idList: List<String>,
        syncStatus: String,
    ) {
        assessmentDAO.updateInProgress(idList, syncStatus)
    }

    override suspend fun changeFollowUpStatus(
        idList: List<Long>,
        syncStatus: String,
    ) {
        followUpDao.updateInProgress(idList, syncStatus)
    }

    override suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail? = pregnancyDetailDao.getPregnancyDetailByPatientId(hhmLocalId)

    override suspend fun savePregnancyDetail(detail: PregnancyDetail): Long = pregnancyDetailDao.savePregnancyDetail(detail)

    override suspend fun deleteAllFrequencyList() = frequencyDAO.deleteAllVillages()

    override suspend fun saveFrequencyList(frequencyList: List<FrequencyEntity>): List<Long> = frequencyDAO.insertFrequencyList(frequencyList)

    override suspend fun getFrequencyList(): List<FrequencyEntity> = frequencyDAO.getFrequencyList()

    override suspend fun getInstructionList(): List<MedicalReviewMetaItems> =
        aboveFiveYearsDAO.getSummaryDetailMetaItems(MedicalReviewTypeEnums.PRESCRIPTION_INSTRUCTION.name)

    override fun getExaminationsComplaintsForPnc(
        category: String,
        type: String,
    ): LiveData<List<MedicalReviewMetaItems>> = examinationsComplaintsDAO.getExaminationsComplaintsForPnc(category, type)

    override suspend fun updateOtherFollowUpForWrongNumber(
        id: Long,
        fhirId: String,
    ) {
        followUpDao.updateOtherFollowUpForWrongNumber(id, fhirId)
    }

    override suspend fun insertOrUpdateFollowUp(entity: FollowUp) {
        followUpDao.insertOrUpdateFromBE(entity)
    }

    override suspend fun deleteCompletedFollowUp() {
        followUpDao.deleteCompletedFollowUp()
    }

    override suspend fun getUserHealthFacility(isUserSite: Boolean): ArrayList<HealthFacilityEntity> = ArrayList(metaDataDAO.getUserHealthFacility(isUserSite))

    override suspend fun updateMemberDeceasedStatus(
        id: Long,
        status: Boolean,
    ) {
        memberDAO.updateMemberDeceasedStatus(id, status, OfflineSyncStatus.NotSynced)
        rxBuddyDetailsDAO.updateRxBuddyStatus(id, status)
    }

    override suspend fun saveForm(forms: FormEntity) = metaDataDAO.saveForm(forms)

    override suspend fun saveConsent(consentEntity: ConsentEntity) = metaDataDAO.insertConsent(consentEntity)

    override fun getConsent(formType: String): LiveData<String> = metaDataDAO.getConsent(formType)

    override suspend fun getConsentString(formType: String): String = metaDataDAO.getConsentString(formType)

    override suspend fun deleteConsent() = metaDataDAO.deleteConsent()

    override suspend fun saveModelQuestions(mentalHealthEntity: List<MentalHealthEntity>) = metaDataDAO.insertModelQuestions(mentalHealthEntity)

    override suspend fun getModelQuestions(formType: String): MentalHealthEntity = metaDataDAO.getModelQuestions(formType)

    override suspend fun deleteModelQuestions() = metaDataDAO.deleteModelQuestions()

    override suspend fun changeFollowUpCallStatus(idList: List<Long>) {
        followUpCallsDao.updateSyncSuccess(idList)
    }

    override suspend fun updateNeonatePatientId(
        hhmLocalId: Long,
        neonateId: Long,
    ) {
        pregnancyDetailDao.updateNeonatePatientId(hhmLocalId, neonateId)
    }

    override suspend fun changeHHMLinkCallStatus(
        idList: List<String>,
        syncStatus: String,
    ) {
        callHistoryDao.updateInProgress(idList, syncStatus)
    }

    override suspend fun changeCommunityProfileStatus(
        idList: List<Long>,
        syncStatus: String,
    ) {
        communityDAO.updateInStatus(idList, syncStatus)
    }

    override suspend fun changeAssignHHMStatus(
        idList: List<String>,
        syncStatus: String,
    ) {
        linkHouseholdMemberDao.updateInProgress(idList, syncStatus)
    }

    override suspend fun getMemberDetailsByPatientId(patientId: String): HouseholdMemberEntity? = memberDAO.getMemberDetailsByPatientId(patientId)

    override suspend fun getChildPatientId(parentId: Long): Long? = pregnancyDetailDao.getChildPatientId(parentId)

    override suspend fun getPatientIdById(id: Long): String = memberDAO.getPatientIdById(id)

    override suspend fun insertConsentForm(form: ConsentForm): Long = consentFormDao.insert(form)

    override suspend fun getConsentFormByType(type: String): ConsentForm? = consentFormDao.getConsentFormByType(type)

    override suspend fun deleteAllConsentForm() {
        consentFormDao.delete()
    }

    override suspend fun getHHSignatureDetails(): List<HHSignatureDetail> = memberDAO.getHHSignatureDetails()

    override suspend fun updatePhoneNumberForHouseholdHead(
        id: Long,
        phoneNumber: String?,
        phoneNumberCategory: String?,
    ) = memberDAO.updatePhoneNumberForHouseholdHead(id, phoneNumber)

    override suspend fun insertLinkHouseholdMembers(insertList: List<LinkHouseholdMember>) {
        linkHouseholdMemberDao.insert(insertList)
    }

    override suspend fun deleteLinkHouseholdMembersById(deleteListIds: List<String>) {
        linkHouseholdMemberDao.delete(deleteListIds)
    }

    override fun getUnAssignedHouseholdMembersLiveData(): LiveData<List<UnAssignedHouseholdMemberDetail>> =
        linkHouseholdMemberDao.getUnAssignedHouseholdMembersLiveData()

    override suspend fun addLinkMemberCall(callHistory: CallHistory): Long = callHistoryDao.insert(callHistory)

    override suspend fun getUnSyncedCallHistoryForHHMLink(): List<HouseholdMemberCallRegisterDto> = callHistoryDao.getUnSyncedCallHistoryForHHMLink()

    override suspend fun changeMemberDetailsToNotSynced(id: Long) {
        memberDAO.changeMemberDetailsToNotSynced(id)
    }

    override suspend fun updateMemberAsAssigned(memberId: String) {
        linkHouseholdMemberDao.updateMemberAsAssigned(memberId)
    }

    override suspend fun saveMedicalCompliance(list: List<MedicalComplianceEntity>) {
        metaDataDAO.insertMedicalCompliance(list)
    }

    override suspend fun getMedicalParentComplianceList(): List<MedicalComplianceEntity> = metaDataDAO.getMedicalComplianceList()

    override suspend fun getMedicalChildComplianceList(parentId: Long): List<MedicalComplianceEntity> = metaDataDAO.getMedicalComplianceList(parentId)

    override suspend fun deleteMedicalCompliance() {
        metaDataDAO.deleteMedicalComplianceList()
    }

    override suspend fun saveDistricts(districts: List<DistrictEntity>) {
        metaDataDAO.insertDistricts(districts)
    }

    override suspend fun getDistricts(countryId: Long): List<DistrictEntity> = metaDataDAO.getDistricts(countryId)

    override suspend fun deleteDistricts() {
        metaDataDAO.deleteCounties()
    }

    override suspend fun saveChiefDoms(chiefdoms: List<ChiefDomEntity>) {
        metaDataDAO.insertChiefDoms(chiefdoms)
    }

    override suspend fun getChiefDoms(districtId: Long): List<ChiefDomEntity> = metaDataDAO.getChiefDoms(districtId)

    override suspend fun deleteChiefDoms() {
        metaDataDAO.deleteChiefDoms()
    }

    override suspend fun savePrograms(programs: List<ProgramEntity>) {
        metaDataDAO.insertPrograms(programs)
    }

    override suspend fun getPrograms(): List<ProgramEntity> = metaDataDAO.getPrograms()

    override suspend fun deletePrograms() {
        metaDataDAO.deletePrograms()
    }

    override fun getMentalQuestion(formType: String): LiveData<MentalHealthEntity?> = metaDataDAO.getMentalQuestion(formType)

    override fun getSites(): LiveData<List<HealthFacilityEntity>> = metaDataDAO.getSites()

    override suspend fun savePatientScreeningInformation(screeningEntity: ScreeningEntity): ScreeningEntity {
        val id = screeningDAO.insertScreening(screeningEntity)
        return screeningDAO.getScreeningById(id)
    }

    override fun getScreenedPatientCount(
        startDate: Long,
        endDate: Long,
        userId: String,
    ): LiveData<Long> = screeningDAO.getScreenedPatientCount(startDate, endDate, userId)

    override fun getScreenedPatientReferredCount(
        startDate: Long,
        endDate: Long,
        userId: String,
        isReferred: Boolean,
    ): LiveData<Long> = screeningDAO.getScreenedPatientReferredCount(startDate, endDate, userId, isReferred)

    override suspend fun getAllScreeningRecords(uploadStatus: Boolean): List<ScreeningEntity> = screeningDAO.getAllScreeningRecords(uploadStatus)

    override suspend fun deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds: Long) =
        screeningDAO.deleteUploadedScreeningRecords(todayDateTimeInMilliSeconds)

    override suspend fun updateScreeningRecordById(
        id: Long,
        uploadStatus: Boolean,
    ) = screeningDAO.updateScreeningRecordById(id, uploadStatus)

    override fun getAssessmentFormData(
        formType: String,
        workFlow: String,
    ): LiveData<String> = metaDataDAO.getAssessmentFormData(formType, workFlow)

    override suspend fun insertRiskFactor(riskFactorEntity: RiskFactorEntity) {
        riskFactorDAO.insertRiskFactor(riskFactorEntity)
    }

    override fun getRiskFactorEntity(): LiveData<List<RiskFactorEntity>> = riskFactorDAO.getAllRiskFactorEntity()

    override suspend fun getAllRiskFactorEntityList(): List<RiskFactorEntity> = riskFactorDAO.getAllRiskFactorEntityList()

    override suspend fun deleteRiskFactor() = riskFactorDAO.deleteRiskFactor()

    override fun getSymptomListByTypeForNCD(type: String): LiveData<List<SignsAndSymptomsEntity>> = assessmentDAO.getSymptomListByTypeForNCD(type)

    override suspend fun deleteTreatmentPlan() = ncdMedicalReviewDao.deleteTreatmentPlan()

    override suspend fun insertTreatmentPlan(items: List<TreatmentPlanEntity>) = ncdMedicalReviewDao.insertTreatmentPlan(items)

    override suspend fun deleteNCDMedicalReviewMeta() = ncdMedicalReviewDao.deleteNCDMedicalReviewMeta()

    override suspend fun insertNCDMedicalReviewMeta(items: List<NCDMedicalReviewMetaEntity>) = ncdMedicalReviewDao.insertNCDMedicalReviewMeta(items)

    override fun getComorbidities(
        type: String?,
        category: String,
    ): LiveData<List<NCDMedicalReviewMetaEntity>> = ncdMedicalReviewDao.getComorbidities(type, category)

    override suspend fun deleteLifestyle() = ncdMedicalReviewDao.deleteLifestyle()

    override suspend fun insertLifestyle(items: List<LifestyleEntity>) = ncdMedicalReviewDao.insertLifestyle(items)

    override fun getLifeStyle(): LiveData<List<LifestyleEntity>> = ncdMedicalReviewDao.getLifeStyle()

    override suspend fun getLifeStyleList(): List<LifestyleEntity> = ncdMedicalReviewDao.getLifeStyleList()

    override fun getAssessmentFormData(
        formTypes: List<String>,
        workFlow: String,
    ): List<String> = metaDataDAO.getAssessmentFormData(formTypes, workFlow)

    override suspend fun getSymptomList(): List<SignsAndSymptomsEntity> = assessmentDAO.getSymptomList()

    override suspend fun saveAssessmentInformation(assessmentEntity: AssessmentNCDEntity): AssessmentNCDEntity {
        val id = assessmentDAO.saveAssessmentInformation(assessmentEntity)
        return assessmentDAO.getAssessmentById(id)
    }

    override suspend fun getAllAssessmentRecords(uploadStatus: Boolean): List<AssessmentNCDEntity> = assessmentDAO.getAllAssessmentRecords(uploadStatus)

    override suspend fun updateAssessmentUploadStatus(
        id: Long,
        uploadStatus: Boolean,
    ) = assessmentDAO.updateAssessmentUploadStatus(id, uploadStatus)

    override suspend fun deleteAssessmentList(isUploaded: Boolean) = assessmentDAO.deleteAssessmentList()

    override suspend fun getAssessmentClinicalWorkflow(
        gender: String,
        name: String,
    ): List<NCDAssessmentClinicalWorkflow> = metaDataDAO.getAssessmentClinicalWorkflow(gender, name)

    override fun getUnSyncedDataCountForNCDScreening(): LiveData<Long> = screeningDAO.getUnSyncedDataCountForNCDScreening()

    override fun getUnSyncedNCDAssessmentCount(): LiveData<Long> = assessmentDAO.getUnSyncedNCDAssessmentCount()

    override suspend fun saveNCDDiagnosisList(diseaseEntityList: ArrayList<NCDDiagnosisEntity>) = ncdMedicalReviewDao.saveNCDDiagnosisList(diseaseEntityList)

    override suspend fun deleteNCDDiagnosisList() = ncdMedicalReviewDao.deleteNCDDiagnosisList()

    override fun getNCDDiagnosisList(
        types: List<String>,
        gender: String,
        isPregnant: Boolean,
    ): LiveData<List<NCDDiagnosisEntity>> = ncdMedicalReviewDao.getNCDDiagnosisList(types, gender, isPregnant)

    override fun getFrequencies(): LiveData<List<TreatmentPlanEntity>> = ncdMedicalReviewDao.getFrequencies()

    override suspend fun getNCDShortageReason(type: String): List<ShortageReasonEntity> = ncdMedicalReviewDao.getNCDShortageEntries(type)

    override suspend fun deleteNCDShortageReason() = ncdMedicalReviewDao.deleteNCDShortageReason()

    override suspend fun saveNCDShortageReason(shortageReasonEntity: List<ShortageReasonEntity>) =
        ncdMedicalReviewDao.saveNCDShortageReason(shortageReasonEntity)

    override suspend fun getNCDForm(
        type: String,
        customizedType: String,
    ): List<String> = metaDataDAO.getNCDForm(type, customizedType)

    override suspend fun getUserVillages(): List<VillageEntity> = metaDataDAO.getUserVillages(true)

    override suspend fun deleteDosageDurations() = metaDataDAO.deleteDosageDurations()

    override suspend fun insertDosageDurations(items: List<DosageDurationEntity>) = metaDataDAO.insertDosageDurations(items)

    override suspend fun getDosageDurations(): List<DosageDurationEntity> = metaDataDAO.getDosageDurationsList()

    override suspend fun getUnitList(type: String): List<UnitMetricEntity> = metaDataDAO.getUnitList(type)

    override suspend fun saveUnitMetric(list: ArrayList<UnitMetricEntity>) {
        metaDataDAO.insertUnitMetricList(list)
    }

    override suspend fun getDosageFrequencyList(): List<DosageFrequency> = metaDataDAO.getDosageFrequencyList()

    override suspend fun deleteUnitMetric() {
        metaDataDAO.deleteUnitMetric()
    }

    override suspend fun saveDosageFrequencyList(list: ArrayList<DosageFrequency>) {
        metaDataDAO.insertDosageFrequencyList(list)
    }

    override suspend fun deleteDosageFrequencyList() {
        metaDataDAO.deleteDosageFrequencyList()
    }

    override suspend fun getUnAssignedChildFhirIds(patientId: String): List<HouseholdMemberFhirId> = linkHouseholdMemberDao.getUnAssignedChildFhirIds(patientId)

    override suspend fun getUnAssignedParentFhirId(parentId: String): List<HouseholdMemberFhirId> = linkHouseholdMemberDao.getUnAssignedParentFhirId(parentId)

    override suspend fun updateHouseholdHeadAndRelationShip(
        fhirIds: List<String>,
        householdId: Long,
    ) {
        memberDAO.updateHouseholdHeadAndRelationShip(fhirIds, householdId)
    }

    override suspend fun updateMembersAsAssigned(fhirIds: List<String>) {
        linkHouseholdMemberDao.updateMembersAsAssigned(fhirIds)
    }

    override suspend fun deleteAllNCDFollowUp() {
        ncdFollowUpDao.deleteAllNCDFollowUps()
    }

    override suspend fun insertNCDFollowUp(followUp: NCDFollowUp): Long = ncdFollowUpDao.insertNCDFollowUp(followUp)

    override fun getNCDFollowUpData(
        type: String,
        searchText: String,
        dateBasedOnChip: Pair<Long?, Long?>?,
        isScreened: Boolean?,
        reason: String?,
    ): LiveData<List<NCDFollowUp>> =
        ncdFollowUpDao.getFilteredNCDFollowUp(
            type,
            searchText,
            dateBasedOnChip?.first,
            dateBasedOnChip?.second,
            isScreened,
            reason,
            DateUtils.dateToLong(),
        )

    override suspend fun updatedCallInitiatedCall(ncdFollowUp: NCDFollowUp): NCDFollowUp {
        ncdFollowUpDao.updatedCallInitiated(ncdFollowUp) // Perform the update/insert
        return ncdFollowUpDao.getNCDFollowUpById(ncdFollowUp.id) // Retrieve the updated object
    }

    override suspend fun getNCDInitiatedCallFollowUp(): NCDFollowUp? = ncdFollowUpDao.getNCDInitiatedCallFollowUp()

    @Transaction
    override suspend fun insertNCDCallDetails(followUp: NCDCallDetails): NCDCallDetails? {
        val id = ncdFollowUpDao.insertNCDCallDetails(followUp)
        if (followUp.reason == FollowUpDefinedParams.WRONG_NUMBER) {
            // Update isWrongNumber in NCDFollowUp
            ncdFollowUpDao.markAsWrongNumber(followUp.id)
        }
        ncdFollowUpDao.updateNCDInitiatedCallFollowUp(followUp.id)
        return ncdFollowUpDao.getNCDCallDetails(id)
    }

    override suspend fun updateRetryAttempts(
        id: Long,
        retryAttempts: Long,
    ) = ncdFollowUpDao.updateRetryAttempts(id, retryAttempts)

    override suspend fun getAttemptsById(id: Long): Long? = ncdFollowUpDao.getAttemptsById(id)

    override suspend fun getNCDFollowUpById(id: Long): NCDFollowUp = ncdFollowUpDao.getNCDFollowUpById(id)

    override suspend fun getAllNCDCallDetails(): List<NCDCallDetails> = ncdFollowUpDao.getAllNCDCallDetails()

    override suspend fun insertNCDPatientDetails(patients: NCDPatientDetailsEntity): Long = ncdFollowUpDao.insertNCDPatientDetails(patients)

    override suspend fun deleteAllNCDPatientDetails() = ncdFollowUpDao.deleteAllNCDPatientDetails()

    override suspend fun getPatientBasedOnId(id: String): NCDPatientDetailsEntity = ncdFollowUpDao.getPatientBasedOnId(id)

    override suspend fun deleteCallDetails(id: Long) = ncdFollowUpDao.deleteCallDetails(id)

    override fun getUnSyncedNCDFollowUpCount(): LiveData<Long> = ncdFollowUpDao.getUnSyncedNCDFollowUpCount()

    override suspend fun saveCultures(cultures: List<CulturesEntity>) {
        metaDataDAO.insertCultures(cultures)
    }

    override suspend fun getCultures(): List<CulturesEntity> = metaDataDAO.getCultures()

    override suspend fun deleteCultures() {
        metaDataDAO.deleteCultures()
    }

    override suspend fun insertLinkedVillages(linkedVillages: List<LinkedVillageEntity>) {
        metaDataDAO.insertLinkedVillages(linkedVillages)
    }

    override suspend fun deleteAllLinkedVillages() {
        metaDataDAO.deleteAllLinkedVillages()
    }

    override suspend fun updateMemberDeceasedReason(
        id: Long,
        status: Boolean,
        deceasedReason: String?,
    ) {
        memberDAO.updateMemberDeceasedReason(id, status, OfflineSyncStatus.NotSynced, deceasedReason)
        rxBuddyDetailsDAO.updateRxBuddyStatus(id, status)
    }

    override suspend fun getHouseholdHeadDob(householdId: Long): String = memberDAO.getHouseholdHeadDob(householdId)

    override fun getFilterVillagesWithHouseholdsCount(searchInput: String): LiveData<List<CommunityProfileDetail>> =
        metaDataDAO.filterCommunityProfile(searchInput)

    override suspend fun getCommunityStatistics(villageId: Long): CommunityPopulationStatistics = metaDataDAO.getCommunityPopulationStatistics(villageId)

    override suspend fun insertCommunityDetails(communityProfile: CommunityProfile): Long = communityDAO.insertCommunity(communityProfile)

    override suspend fun getCommunityDetails(id: Long): CommunityProfile? = communityDAO.getCommunityDetailsById(id)

    override suspend fun getCommunityProfileId(villageId: Long): Long? = communityDAO.getCommunityProfileId(villageId)

    override suspend fun updateCommunityDetails(communityProfile: CommunityProfile) = communityDAO.updateCommunity(communityProfile)

    override suspend fun updateUnSynStatus(
        villageId: Long,
        synStatus: String,
    ) = communityDAO.updateSyncStatus(villageId, synStatus)

    override suspend fun getHealthFacilityBasedOnVillageId(villageId: Long): List<HealthFacilityEntity> =
        metaDataDAO.getHealthFacilityBasedOnVillageId(villageId)

    override suspend fun getAssessment(assessmentId: Long): AssessmentEntity = assessmentDAO.getAssessment(assessmentId)

    override suspend fun getUnSyncedCommunityDetails(): List<CommunityProfile> = communityDAO.getUnSyncedCommunityDetails()

    override suspend fun insertOrUpdateFromBE(communityProfile: CommunityProfile): Long = communityDAO.insertOrUpdateFromBE(communityProfile)

    override fun householdMemberWithTbStatus(hhId: Long): LiveData<List<HouseholdMemberEntity>> = memberDAO.getHouseholdMemberWithTBContactTraceStatus(hhId)

    override suspend fun updateTBContactTraceStatus(
        hhmId: Long,
        tbContactTracingStatus: Int,
    ) = memberDAO.updateTBContactTraceStatus(hhmId, tbContactTracingStatus)

    override suspend fun updatePregnantStatus(
        memberId: Long,
        isPregnant: Boolean,
    ) = memberDAO.updatePregnantStatus(memberId, isPregnant, syncStatus = OfflineSyncStatus.NotSynced.name)

    override suspend fun getSymptomListByTypes(types: List<String>): List<SignsAndSymptomsEntity> = assessmentDAO.getSymptomListByTypes(types)

    override suspend fun insertRxBuddyDetails(rxBuddyDetails: RxBuddyDetails): Long = rxBuddyDetailsDAO.insertRxBuddyDetails(rxBuddyDetails)

    override suspend fun getRxBuddyDetails(patientMemberId: String): RxBuddyDetails? = rxBuddyDetailsDAO.getRxBuddyDetailsByPatientMemberId(patientMemberId)

    override suspend fun getOtherHouseholdExcludeTBPatient(
        householdId: Long,
        patientId: Long,
    ): List<HouseholdMemberEntity> = memberDAO.getOtherHouseholdExcludeTBPatient(householdId, patientId)

    override suspend fun insertTreatmentDetails(treatmentDetails: TreatmentDetailsEntity): Long = treatmentDetailsDAO.insertTreatmentDetails(treatmentDetails)

    override suspend fun updateTreatmentDetails(treatmentDetails: TreatmentDetailsEntity): Int = treatmentDetailsDAO.updateTreatmentDetails(treatmentDetails)

    override suspend fun getTreatmentDetails(memberId: String): TreatmentDetailsEntity? = treatmentDetailsDAO.getTreatmentDetailsByMemberId(memberId)

    override suspend fun insertRxBuddyFollowUp(rxBuddyFollowUp: RxBuddyFollowUpEntity): Long = rxBuddyFollowUpDAO.insertRxBuddyFollowUp(rxBuddyFollowUp)

    override suspend fun getHivMetaData(): List<MedicalReviewMetaItems> = hivMetaDataDAO.getHivMetaItems()

    override suspend fun getAllUnSyncedRxBuddyRegister(): List<RxBuddyRegisterDetail> = rxBuddyDetailsDAO.getAllUnSyncedRxBuddyRegister()

    override suspend fun getHouseholdMemberForRxBuddy(hhmId: Long): HouseHoldMember = memberDAO.getHouseholdMemberForRxBuddy(hhmId)

    override suspend fun getUnSyncedRxBuddyFollowUpWithoutRxBuddyId(rxBuddyLocalId: Long): List<RxBuddyFollowUpEntity> =
        rxBuddyFollowUpDAO.getUnSyncedRxBuddyFollowUpWithoutRxBuddyId(rxBuddyLocalId)

    override suspend fun getUnSyncedRxBuddyFollowUpWithRxBuddyId(): List<RxBuddyFollowUpDetails> = rxBuddyFollowUpDAO.getUnSyncedRxBuddyFollowUpWithRxBuddyId()

    override suspend fun updateNextVisitDateRxBuddyRegister(
        nextVisitDate: String,
        id: Long,
    ) {
        rxBuddyDetailsDAO.updateNextVisitDate(id, nextVisitDate)
    }

    override suspend fun updateNextVisitDateRxBuddyFollowUp(
        nextVisitDate: String,
        id: Long,
    ) {
        rxBuddyFollowUpDAO.updateNextVisitDate(id, nextVisitDate)
    }

    override suspend fun insertOrUpdateRxBuddyFromBE(entity: RxBuddyDetails): Long = rxBuddyDetailsDAO.insertOrUpdateFromBE(entity)

    override suspend fun deleteAllTreatmentDetails() {
        treatmentDetailsDAO.deleteAllTreatmentDetails()
    }

    override suspend fun deleteAllRxBuddyDetails() {
        rxBuddyDetailsDAO.deleteAllRxBuddyDetails()
    }

    override suspend fun deleteAllRxBuddyFollowUp() {
        rxBuddyFollowUpDAO.deleteAllRxBuddyFollowUp()
    }

    override suspend fun getUnSyncedRxBuddyRegisterCount(): Int = rxBuddyDetailsDAO.getUnSyncedCount()

    override suspend fun getUnSyncedRxBuddyFollowUpCount(): Int = rxBuddyFollowUpDAO.getUnSyncedCount()

    override suspend fun updateRxBuddyRegisterSyncStatus(
        idList: List<Long>,
        syncStatus: String,
    ) {
        rxBuddyDetailsDAO.updateSyncStatus(idList, syncStatus)
    }

    override suspend fun updateRxBuddyFollowUpSyncStatus(
        idList: List<Long>,
        syncStatus: String,
    ) {
        rxBuddyFollowUpDAO.updateSyncStatus(idList, syncStatus)
    }

    override suspend fun deleteDisableRxBuddies(ids: List<Long>) {
        rxBuddyDetailsDAO.deleteAllDisabledRxBuddies(ids)
    }

    override suspend fun getHouseholdMemberIdAndStatusByFhirId(fhirId: String): HouseholdMemberStatus? = memberDAO.getHouseholdMemberIdAndStatusByFhirId(fhirId)

    override suspend fun getUnSyncedHouseHoldByMemberId(hhmId: Long): HouseHold? = householdDAO.getUnSyncedHouseHoldByMemberId(hhmId)

    override suspend fun getMemberFhirIdByLocalId(hhmId: Long): String? = memberDAO.getMemberFhirIdByLocalId(hhmId)

    override suspend fun getAllUnSyncedRxBuddyDetailWithHHM(
        hhmId: Long,
        rxBuddiesId: List<Long>,
    ): List<RxBuddyRegisterDetail> = rxBuddyDetailsDAO.getAllUnSyncedRxBuddyDetailWithHHM(hhmId, rxBuddiesId)

    override suspend fun getTbPatientLocalIdByHouseholdId(householdId: Long): MutableList<Long> = memberDAO.getTbPatientLocalIdByHouseholdId(householdId)

    override suspend fun updateContactTracingStatus(
        memberId: Long,
        status: Int?,
    ) {
        memberDAO.updateContactTracingStatus(memberId, status)
    }

    override suspend fun updateContactTracingForLinkTbPatient(
        tbHHMId: Long,
        householdId: Long,
    ) {
        memberDAO.updateContactTracingForLinkTbPatient(tbHHMId, householdId)
    }

    override suspend fun getHouseholdsCountBasedSubVillage(subVillageId: Long): Int = householdDAO.getHouseholdsCountBasedSubVillage(subVillageId)

    override suspend fun getDisabilityMembersCountForHousehold(householdId: Long): Int = memberDAO.getDisabilityMembersCountForHousehold(householdId)

    override suspend fun updateDisabilityPersonsCountIfUnderCounted(householdId: Long) = householdDAO.updateDisabilityPersonsCountIfUnderCounted(householdId)

    override suspend fun updateGuardianHhIds(): Int = memberDAO.updateGuardianHhIds()

    override suspend fun updateUndercountedHouseholds(): Int = householdDAO.updateUndercountedHouseholds()

    override suspend fun updateUndercountedDisabilityHouseholds(): Int = householdDAO.updateUndercountedDisabilityHouseholds()

    override fun getServiceMembers(
        searchInput: String,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        staticFilter: ServiceStaticFilter,
        allowNullHousehold: Boolean,
    ) = memberDAO.getServiceMembers(searchInput, filterBySs, filterBySubVillages, staticFilter, allowNullHousehold)

    /**
     * Delegates service-member count aggregation to [MemberDAO].
     */
    override suspend fun getAllServiceMemberCounts(
        searchInput: String,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        allowNullHousehold: Boolean,
    ): ServiceMemberCounts = memberDAO.getAllServiceMemberCounts(searchInput, filterBySs, filterBySubVillages, allowNullHousehold)

    override suspend fun getMemberAssessmentHistory(
        memberFhirId: String?,
        memberId: Long?,
        visitDate: String?,
        serviceProvided: String?,
    ): MemberAssessmentHistoryEntity? = memberAssessmentHistoryDao.getAssessmentHistory(memberFhirId, memberId, visitDate, serviceProvided)

    override suspend fun insertMemberAssessmentHistory(historyList: List<MemberAssessmentHistoryEntity>) =
        memberAssessmentHistoryDao.insertMemberAssessmentHistory(historyList)

    override suspend fun deleteAllMemberAssessmentHistory() = memberAssessmentHistoryDao.deleteMemberAssessmentHistory()

    override fun getMemberWithAssessmentHistory(memberId: Long): LiveData<MemberAssessmentHistoryResponse?> =
        memberDAO.getMemberWithAssessmentHistory(memberId).map { result ->
            if (!result.isNullOrEmpty()) {
                val entry = result.entries.first()
                MemberAssessmentHistoryResponse(entry.key, entry.value)
            } else {
                null
            }
        }

    override suspend fun getDashboardCounts(
        startDate: String?,
        endDate: String?,
        ssIds: List<Long>,
        subVillageIds: List<Long>,
    ): DashboardCountsRow {
        val base = memberAssessmentHistoryDao.getDashboardCounts(
            startDate,
            endDate,
            ssIds,
            ssIds.size,
            subVillageIds,
            subVillageIds.size,
        )
        val hhCount = householdDAO.getHouseholdRegisteredCount(
            startDate,
            endDate,
            ssIds,
            ssIds.size,
            subVillageIds,
            subVillageIds.size,
        ) ?: 0
        val maternal = memberAssessmentHistoryDao.getMaternalDashboardCounts(
            startDate,
            endDate,
            ssIds,
            ssIds.size,
            subVillageIds,
            subVillageIds.size,
        )
        return base?.copy(
            householdRegisteredCount = hhCount,
            pwIdentifiedFirst4MonthsWithAncCount = maternal?.pwIdentifiedFirst4MonthsWithAncCount ?: 0,
            anc3PlusCount = maternal?.anc3PlusCount ?: 0,
        ) ?: DashboardCountsRow(
            screened = 0,
            referred = 0,
            registered = 0,
            assessed = 0,
            dispensed = 0,
            investigated = 0,
            nutritionistLifestyleCount = 0,
            psychologicalNotesCount = 0,
            familyPlanningCount = 0,
            pregnantWomenRegistrationCount = 0,
            pregnancyOutcomeCount = 0,
            ancCount = 0,
            pncCount = 0,
            childVisitCount = 0,
            tbAssessmentCount = 0,
            tbContactTracingCount = 0,
            eyeCareCount = 0,
            cataractCount = 0,
            householdRegisteredCount = hhCount,
            pwIdentifiedFirst4MonthsWithAncCount = maternal?.pwIdentifiedFirst4MonthsWithAncCount ?: 0,
            anc3PlusCount = maternal?.anc3PlusCount ?: 0,
            highRiskPregnantWomenCount = 0,
            totalNcdServicesCount = 0,
            ncdScreeningFirstServiceCount = 0,
            ncdFollowUpAssessmentCount = 0,
            ncdFollowUpReferralCount = 0,
            glassesSoldCustomStatusCount = 0,
            ncdServicesInCataractCampCount = 0,
            patientsReferredForOperationCount = 0,
        )
    }

    override suspend fun insertMemberAssessmentHistory(assessmentHistory: MemberAssessmentHistoryEntity) =
        memberAssessmentHistoryDao.insertMemberAssessmentHistory(assessmentHistory)

    override suspend fun updateMemberAssessmentHistory(assessmentHistory: MemberAssessmentHistoryEntity) =
        memberAssessmentHistoryDao.updateAssessmentHistory(assessmentHistory)

    override suspend fun getLastServiceHistory(
        memberLocalId: Long,
        serviceTypeFor: String,
    ): MemberAssessmentHistoryEntity? = memberAssessmentHistoryDao.getLatestMemberAssessmentHistoryByMemberLocalIdAndType(memberLocalId, serviceTypeFor)

    override suspend fun decrementAssessmentHistoryVisitDate(
        memberId: Long,
        noOfDays: Int,
    ) = memberAssessmentHistoryDao.decrementDateByDays(memberId, noOfDays)

    override suspend fun deleteDuplicateAssessmentHistory(date: String) {
        memberAssessmentHistoryDao.deleteDuplicateRecords(date)
    }

    override suspend fun getFormBasedOnType(
        formTypeOne: String,
        formTypeTwo: String,
    ): List<FormEntity> = metaDataDAO.getFormBasedOnType(formTypeOne, formTypeTwo)

    override suspend fun getVillageList(selectedParent: Long): List<VillageEntity> = metaDataDAO.getVillageList()

    override suspend fun getOtherVillage(): VillageEntity = metaDataDAO.getOtherVillage()

    override suspend fun getProgramList(site: String): Any = metaDataDAO.getProgramList()

    override suspend fun getProgramList(
        selectedParent: Long,
        site: String,
    ): Any = metaDataDAO.getProgramList(selectedParent)

    override suspend fun getUpazilaListByDistrict(districtID: Long): List<SiteEntity> = metaDataDAO.getUpazilaListByDistrict(districtID)

    override suspend fun getUpazilaList(): Any = metaDataDAO.getUpazilaList()

    override suspend fun updateSequenceCode(
        villageId: Long,
        newSequenceCode: Long,
    ) {
    }

    override suspend fun getScreeningRecordById(id: Long): ScreeningEntity = screeningDAO.getScreeningById(id)

    override suspend fun getAccountSiteList(userId: Long): List<SiteEntity> = metaDataDAO.getAccountSiteList(userId)

    override suspend fun getAccountSiteListByLevel(
        userId: Long,
        level: String,
    ): List<SiteEntity> = metaDataDAO.getAccountSiteListByLevel(userId, level)

    override suspend fun getUpazilaListEyeCareOnly(): Any = metaDataDAO.getUpazilaListEyeCareOnly()

    override suspend fun getUpazilaListCataractOnly(): Any = metaDataDAO.getUpazilaListCataractOnly()

    override suspend fun getSiteEntity(
        userSite: Boolean,
        userId: Long,
    ): List<SiteEntity> = metaDataDAO.getSiteEntityList(userSite, userId)

    override suspend fun getUpazilaById(upazilaId: Long): SiteEntity = metaDataDAO.getUpazilaById(upazilaId)

    override suspend fun getVillageById(villageId: Long): VillageEntity = metaDataDAO.getVillageById(villageId)

    override suspend fun getTreatmentPlanData(): List<TreatmentPlanEntity> = metaDataDAO.getTreatmentPlanData()

    override suspend fun getShortageReason(type: String): List<ShortageReasonEntity> = metaDataDAO.getShortageEntries(type)

    override suspend fun getComorbidityBasedOnWorkflow(workflowList: ArrayList<String>): List<ComorbidityEntity> =
        metaDataDAO.getComorbidityBasedOnWorkflow(workflowList)

    override suspend fun saveComorbidity(list: ArrayList<ComorbidityEntity>) {
        metaDataDAO.saveComorbidity(list)
    }

    override suspend fun getComorbidity(): List<ComorbidityEntity> = metaDataDAO.getComorbidity()

    override suspend fun deleteComorbidity() {
        metaDataDAO.deleteComorbidity()
    }

    override suspend fun saveComplication(list: ArrayList<ComplicationEntity>) {
        metaDataDAO.saveComplication(list)
    }

    override suspend fun deleteComplication() {
        metaDataDAO.deleteComplication()
    }

    override suspend fun getComplication(): List<ComplicationEntity> = metaDataDAO.getComplication()

    override suspend fun saveCurrentMedication(list: ArrayList<CurrentMedicationEntity>) {
        metaDataDAO.saveCurrentMedication(list)
    }

    override suspend fun deleteCurrentMedication() {
        metaDataDAO.deleteCurrentMedication()
    }

    override suspend fun getCurrentMedicationList(): List<CurrentMedicationEntity> = metaDataDAO.getCurrentMedicationList()

    override suspend fun getCurrentMedicationList(type: String): List<CurrentMedicationEntity> = metaDataDAO.getCurrentMedicationList(type)

    override suspend fun savePhysicalExamination(list: ArrayList<PhysicalExaminationEntity>) {
        metaDataDAO.savePhysicalExamination(list)
    }

    override suspend fun deletePhysicalExamination() {
        metaDataDAO.deletePhysicalExamination()
    }

    override suspend fun getPhysicalExaminationList(workFlowList: ArrayList<String>): List<PhysicalExaminationEntity> =
        metaDataDAO.getPhysicalExaminationList(workFlowList)

    override suspend fun saveCompliants(list: ArrayList<ComplaintsEntity>) {
        metaDataDAO.saveComplaints(list)
    }

    override suspend fun deleteCompliants() {
        metaDataDAO.deleteComplaints()
    }

    override suspend fun getChiefComplaints(workflowList: ArrayList<String>): List<ComplaintsEntity> = metaDataDAO.getChiefComplaints(workflowList)

    override suspend fun getDiagnosis(
        gender: ArrayList<String>,
        type: ArrayList<String>,
    ): List<DiagnosisEntity> = metaDataDAO.getDiagnosis(gender, type)

    override suspend fun saveSymptomList(symptoms: List<SymptomEntity>) {
        metaDataDAO.insertSymptomsList(symptoms)
    }

    override suspend fun deleteSymptoms() {
        metaDataDAO.deleteSymptomList()
    }

    override suspend fun getSymptomsList(): List<SymptomEntity> = metaDataDAO.getSymptomList()

    override suspend fun getSymptomsListByType(type: String): List<SymptomEntity> = metaDataDAO.getSymptomsListByType(type)

    override suspend fun getOperatingUnitSites(): List<SiteEntity> = metaDataDAO.getOperatingUnitSites()

    override suspend fun saveDiagnosis(diseaseEntityList: ArrayList<DiagnosisEntity>) {
        metaDataDAO.saveDiagnosis(diseaseEntityList)
    }

    override suspend fun deleteDiagnosisList() {
        metaDataDAO.deleteDiagnosisList()
    }

    override suspend fun deleteAllSiteCache() = metaDataDAO.deleteSiteList()

    override suspend fun saveSiteList(siteEntity: List<SiteEntity>) = metaDataDAO.insertSiteDetails(siteEntity)

    override suspend fun getAllChiefDoms(): List<ChiefDomEntity> = metaDataDAO.getAllChiefDoms()

    override suspend fun getSubVillage(villageId: Long): List<SubVillageEntity> = metaDataDAO.getSubVillage(villageId)
}
