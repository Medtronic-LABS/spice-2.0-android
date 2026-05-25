package org.medtroniclabs.uhis.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.medtroniclabs.uhis.data.CulturesEntity
import org.medtroniclabs.uhis.data.DosageFrequency
import org.medtroniclabs.uhis.data.ProgramEntity
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.data.community.CommunityPopulationStatistics
import org.medtroniclabs.uhis.data.community.CommunityProfileDetail
import org.medtroniclabs.uhis.db.entity.ChiefDomEntity
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowConditionEntity
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowEntity
import org.medtroniclabs.uhis.db.entity.ComorbidityEntity
import org.medtroniclabs.uhis.db.entity.ComplaintsEntity
import org.medtroniclabs.uhis.db.entity.ComplicationEntity
import org.medtroniclabs.uhis.db.entity.ConsentEntity
import org.medtroniclabs.uhis.db.entity.CurrentMedicationEntity
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.db.entity.DistrictEntity
import org.medtroniclabs.uhis.db.entity.DosageDurationEntity
import org.medtroniclabs.uhis.db.entity.FormEntity
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.LinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.MedicalComplianceEntity
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.db.entity.NCDAssessmentClinicalWorkflow
import org.medtroniclabs.uhis.db.entity.PhysicalExaminationEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiLinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaLinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.SymptomEntity
import org.medtroniclabs.uhis.db.entity.TreatmentPlanEntity
import org.medtroniclabs.uhis.db.entity.UserProfileEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity

@Dao
interface MetaDataDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthFacility(healthFacilityEntityList: HealthFacilityEntity)

    @Query("SELECT * FROM HealthFacilityEntity WHERE isDefault = 1")
    suspend fun getDefaultHealthFacility(): HealthFacilityEntity?

    @Query("DELETE FROM HealthFacilityEntity")
    suspend fun deleteAllHealthFacility()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillages(villageEntityList: List<VillageEntity>)

    @Query("DELETE FROM VillageEntity")
    suspend fun deleteAllVillages()

    @Query("SELECT * FROM VillageEntity WHERE chiefdomId =:chiefdomId OR chiefdomId IS NULL ORDER BY name ASC")
    suspend fun getVillagesByChiefDom(chiefdomId: Long): List<VillageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenus(menuEntity: MenuEntity)

    @Query("SELECT * FROM MenuEntity  ORDER BY displayOrder ASC")
    suspend fun getMenus(): List<MenuEntity>

    @Query("DELETE FROM MenuEntity")
    suspend fun deleteAllMenus()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfileDetails(userProfileEntity: UserProfileEntity)

    @Query("DELETE FROM UserProfileEntity")
    suspend fun deleteAllUserProfileDetails()

    @Query("SELECT * FROM UserProfileEntity")
    suspend fun getUserProfile(): UserProfileEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClinicalWorkflows(clinicalWorkflows: List<ClinicalWorkflowEntity>)

    @Query("DELETE FROM ClinicalWorkflowEntity")
    suspend fun deleteAllClinicalWorkflow()

    @Query("SELECT id FROM ClinicalWorkflowEntity")
    suspend fun getAllClinicalWorkflowIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveForms(forms: List<FormEntity>)

    @Query("DELETE FROM FormEntity")
    suspend fun deleteAllForms()

    @Query("SELECT formInput FROM FormEntity where formType =:formType")
    suspend fun getFormData(
        formType: String,
    ): String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptomEntity: List<SignsAndSymptomsEntity>)

    @Query("DELETE FROM SymptomEntity")
    suspend fun deleteAllSymptoms()

    @Query("SELECT * FROM VillageEntity ORDER BY name ASC")
    suspend fun getVillages(): List<VillageEntity>

    @Query("SELECT * FROM VillageEntity where id = :villageId")
    suspend fun getVillageByID(villageId: Long): VillageEntity

    @Query("SELECT * FROM ClinicalWorkflowEntity")
    suspend fun getMenuForClinicalWorkflows(): List<ClinicalWorkflowEntity>

    @Query("DELETE FROM ClinicalWorkflowConditionEntity")
    suspend fun deleteClinicalWorkflowConditions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalWorkflowConditions(clinicalWorkflowConditions: List<ClinicalWorkflowConditionEntity>)

    @Query("SELECT DISTINCT wf.*, wfc.subModule FROM ClinicalWorkflowEntity AS wf LEFT JOIN ClinicalWorkflowConditionEntity AS wfc ON wf.id = wfc.clinicalWorkflowId WHERE (LOWER(wfc.gender) = 'both' OR LOWER(wfc.gender) = LOWER(:gender)) AND (wfc.maxAge IS NULL OR wfc.maxAge > :age) AND (wfc.minAge IS NULL OR wfc.minAge <= :age) AND (wfc.moduleType = :moduleType ) ORDER BY wf.displayOrder")
    suspend fun getClinicalWorkflowId(
        gender: String,
        age: Int,
        moduleType: String,
    ): List<NCDAssessmentClinicalWorkflow>

    @Query("SELECT * FROM HealthFacilityEntity Order by isDefault DESC")
    suspend fun getNearestHealthFacility(): List<HealthFacilityEntity>

    @Query("SELECT id FROM SubVillageEntity")
    suspend fun getSubVillageIds(): List<Long>

    @Query("SELECT * FROM SubVillageEntity")
    suspend fun getSubVillages(): List<SubVillageEntity>

    @Query("SELECT * FROM HealthFacilityEntity Where isUserSite =:isUserSite")
    suspend fun getUserHealthFacility(isUserSite: Boolean): List<HealthFacilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programEntity: List<ProgramEntity>)

    @Query("SELECT * FROM ProgramEntity")
    suspend fun getPrograms(): List<ProgramEntity>

    @Query("DELETE FROM ProgramEntity")
    suspend fun deletePrograms()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCultures(culturesEntity: List<CulturesEntity>)

    @Query("SELECT * FROM CulturesEntity")
    suspend fun getCultures(): List<CulturesEntity>

    @Query("DELETE FROM CulturesEntity")
    suspend fun deleteCultures()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveForm(forms: FormEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsent(consentEntity: ConsentEntity)

    @Query("SELECT formInput FROM ConsentEntity where formType=:formType")
    fun getConsent(formType: String): LiveData<String>

    @Query("SELECT formInput FROM ConsentEntity where formType=:formType")
    suspend fun getConsentString(formType: String): String

    @Query("DELETE FROM ConsentEntity")
    suspend fun deleteConsent()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelQuestions(mentalHealthEntity: List<MentalHealthEntity>)

    @Query("SELECT * FROM MentalHealthEntity where formType=:formType")
    suspend fun getModelQuestions(formType: String): MentalHealthEntity

    @Query("DELETE FROM MentalHealthEntity")
    suspend fun deleteModelQuestions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalCompliance(list: List<MedicalComplianceEntity>)

    @Query("SELECT * FROM MedicalComplianceEntity where parent_compliance_id IS NULL OR parent_compliance_id = '' ORDER BY display_order")
    suspend fun getMedicalComplianceList(): List<MedicalComplianceEntity>

    @Query("SELECT * FROM MedicalComplianceEntity where parent_compliance_id =:parentId ORDER BY display_order")
    suspend fun getMedicalComplianceList(parentId: Long): List<MedicalComplianceEntity>

    @Query("DELETE FROM MedicalComplianceEntity")
    suspend fun deleteMedicalComplianceList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(counties: List<DistrictEntity>)

    @Query("SELECT * FROM DistrictEntity where countryId=:countryId ORDER BY name ASC")
    suspend fun getDistricts(countryId: Long): List<DistrictEntity>

    @Query("DELETE FROM DistrictEntity")
    suspend fun deleteCounties()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChiefDoms(counties: List<ChiefDomEntity>)

    @Query("SELECT * FROM ChiefDomEntity where districtId=:districtId ORDER BY name ASC")
    suspend fun getChiefDoms(districtId: Long): List<ChiefDomEntity>

    @Query("SELECT * FROM ChiefDomEntity")
    suspend fun getAllChiefDoms(): List<ChiefDomEntity>

    @Query("DELETE FROM ChiefDomEntity")
    suspend fun deleteChiefDoms()

    @Query("SELECT * FROM MentalHealthEntity where formType=:formType")
    fun getMentalQuestion(formType: String): LiveData<MentalHealthEntity?>

    @Query("SELECT * FROM HealthFacilityEntity Order by isDefault DESC")
    fun getSites(): LiveData<List<HealthFacilityEntity>>

    @Query("SELECT formInput FROM FormEntity where formType =:formType OR formType =:customizedFormType")
    suspend fun getNCDForm(
        formType: String,
        customizedFormType: String,
    ): List<String>

    @Query("SELECT formInput FROM FormEntity where formType IN (:formTypes) AND workflowName =:workFlow")
    fun getAssessmentFormData(
        formTypes: List<String>,
        workFlow: String,
    ): List<String>

    @Query("SELECT formInput FROM FormEntity where formType =:formType AND workflowName =:workFlow")
    fun getAssessmentFormData(
        formType: String,
        workFlow: String,
    ): LiveData<String>

    @Query("SELECT cwe.id, cwe.name, cwe.workflowName, cwce.category, cwce.groupName, cwce.cultureGroupName, cwce.subModule, cwe.displayOrder FROM ClinicalWorkflowEntity AS cwe JOIN ClinicalWorkflowConditionEntity AS cwce ON cwe.id = cwce.clinicalWorkflowId WHERE cwce.gender = :gender AND cwce.moduleType = :moduleType ORDER BY cwe.displayOrder")
    suspend fun getAssessmentClinicalWorkflow(
        gender: String,
        moduleType: String,
    ): List<NCDAssessmentClinicalWorkflow>

    @Query("SELECT * FROM unit_metric_entity where type=:type")
    suspend fun getUnitList(type: String): List<UnitMetricEntity>

    @Query("DELETE FROM unit_metric_entity")
    suspend fun deleteUnitMetric()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDosageFrequencyList(list: ArrayList<DosageFrequency>)

    @Query("SELECT * FROM dosage_frequency_entity ")
    suspend fun getDosageFrequencyList(): List<DosageFrequency>

    @Query("DELETE FROM dosage_frequency_entity")
    suspend fun deleteDosageFrequencyList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitMetricList(frequencyList: List<UnitMetricEntity>): List<Long>

    @Query("SELECT * FROM VillageEntity Where isUserVillage =:isUserVillage ORDER BY name ASC")
    suspend fun getUserVillages(isUserVillage: Boolean): List<VillageEntity>

    @Query("DELETE FROM DosageDurationEntity")
    suspend fun deleteDosageDurations()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDosageDurations(dosageDurationEntity: List<DosageDurationEntity>)

    @Query("SELECT * FROM DosageDurationEntity")
    suspend fun getDosageDurationsList(): List<DosageDurationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkedVillages(linkedVillages: List<LinkedVillageEntity>)

    @Query("DELETE FROM LinkedVillageEntity")
    suspend fun deleteAllLinkedVillages()

    @Query("SELECT villageId as id, tenantId, name, villagecode, chiefdomId, countryId, districtId, isUserVillage, chiefdomCode, districtCode FROM LinkedVillageEntity WHERE tenantId = :tenantId")
    suspend fun getLinkedVillages(tenantId: Long): List<VillageEntity>

    @Query("SELECT v.id as villageId, v.name as villageName, COUNT(h.id) as houseHoldCount, COUNT(cd.villageId) as isCommunityProfileDetailAvailable FROM VillageEntity v LEFT JOIN Household h ON v.id = h.village_id LEFT JOIN CommunityProfile cd ON cd.villageId = v.id WHERE (:searchText = '' OR v.name LIKE '%' || :searchText || '%') GROUP BY v.id")
    fun filterCommunityProfile(searchText: String): LiveData<List<CommunityProfileDetail>>

    @Query(
        """
        SELECT COUNT(DISTINCT household_id) as householdCount,
        COUNT(CASE WHEN isActive = 1 THEN 1 END) as populationCount,
        0 as pregnantCount,
        COUNT(CASE WHEN substr(date_of_birth, 1, 10) > date('now','-1 year') AND isActive = 1 THEN 1 END ) as belowOneYearCount,
        COUNT(CASE WHEN substr(date_of_birth, 1, 10) > date('now', '-5 years')
               AND substr(date_of_birth, 1, 10) <= date('now', '-1 year') AND isActive = 1 THEN 1 END) AS belowFiveYearCount,
        COUNT(CASE WHEN gender = 'female'
               AND substr(date_of_birth, 1, 10) <= date('now','-10 years')
               AND substr(date_of_birth, 1, 10) > date('now','-49 years') AND isActive = 1 THEN 1 END) AS childBearingAgeOfWomen
        FROM HOUSEHOLDMEMBER
        WHERE villageId = :villageId""",
    )
    suspend fun getCommunityPopulationStatistics(villageId: Long): CommunityPopulationStatistics

    @Query(
        """
    SELECT h.*
    FROM HealthFacilityEntity h
    INNER JOIN VillageEntity v ON v.healthFacilityId = h.id
    WHERE v.id = :villageId
    """,
    )
    suspend fun getHealthFacilityBasedOnVillageId(villageId: Long): List<HealthFacilityEntity>

    // SubVillage methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubVillages(subVillageEntityList: List<SubVillageEntity>)

    @Query("DELETE FROM SubVillageEntity")
    suspend fun deleteAllSubVillages()

    // ShasthyaShebika methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShasthyaShebikas(shasthyaShebikaEntityList: List<ShasthyaShebikaEntity>)

    @Query("DELETE FROM ShasthyaShebikaEntity")
    suspend fun deleteAllShasthyaShebikas()

    @Query("SELECT * FROM ShasthyaShebikaEntity WHERE shasthyaKormiId = :shasthyaKormiId")
    suspend fun getShasthyaShebikaByShasthyaKormiId(shasthyaKormiId: Long): List<ShasthyaShebikaEntity>

    @Query("SELECT * FROM ShasthyaShebikaEntity WHERE id = :id LIMIT 1")
    suspend fun getShasthyaShebikaById(id: Long): ShasthyaShebikaEntity?

    @Query(
        "SELECT cd.* FROM ChiefDomEntity cd INNER JOIN VillageEntity v ON v.chiefdomId = cd.id WHERE v.id = :villageId",
    )
    suspend fun getChiefdomByVillageId(villageId: Long): List<ChiefDomEntity>

    @Query("SELECT * FROM ChiefDomEntity ORDER BY name ASC")
    suspend fun getAllChiefdoms(): List<ChiefDomEntity>

    @Query(
        """
        SELECT DISTINCT cd.* FROM ChiefDomEntity cd
        INNER JOIN VillageEntity v ON v.chiefdomId = cd.id
        INNER JOIN ShasthyaKormiLinkedVillageEntity sklv ON sklv.villageId = v.id
        WHERE sklv.shasthyaKormiId = :shasthyaKormiId
        """,
    )
    suspend fun getChiefdomByShasthyaKormiId(shasthyaKormiId: Long): List<ChiefDomEntity>

    @Query(
        """
        SELECT DISTINCT cd.* FROM ChiefDomEntity cd
        INNER JOIN VillageEntity v ON v.chiefdomId = cd.id
        INNER JOIN ShasthyaKormiLinkedVillageEntity sklv ON sklv.villageId = v.id
        WHERE sklv.shasthyaKormiId IN (:shasthyaKormiIds)
        """,
    )
    suspend fun getChiefdomByShasthyaKormiIds(shasthyaKormiIds: List<Long>): List<ChiefDomEntity>

    @Query("SELECT * FROM ChiefDomEntity WHERE id = :chiefdomId LIMIT 1")
    suspend fun getChiefdomById(chiefdomId: Long): ChiefDomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShasthyaKormis(shasthyaKormiEntityList: List<ShasthyaKormiEntity>)

    @Query("DELETE FROM ShasthyaKormiEntity")
    suspend fun deleteAllShasthyaKormis()

    @Query(
        """
        SELECT DISTINCT sk.* FROM ShasthyaKormiEntity sk
        INNER JOIN ShasthyaKormiLinkedVillageEntity lk ON lk.shasthyaKormiId = sk.id
        WHERE lk.villageId = :villageId
        """,
    )
    suspend fun getShasthyaKormiByVillageId(villageId: Long): List<ShasthyaKormiEntity>

    @Query("SELECT * FROM ShasthyaKormiEntity ORDER BY lastName ASC, firstName ASC")
    suspend fun getAllShasthyaKormis(): List<ShasthyaKormiEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShasthyaKormiLinkedVillages(linkedVillages: List<ShasthyaKormiLinkedVillageEntity>)

    @Query("DELETE FROM ShasthyaKormiLinkedVillageEntity")
    suspend fun deleteAllShasthyaKormiLinkedVillages()

    @Query(
        """
        SELECT DISTINCT sv.* FROM SubVillageEntity sv
        INNER JOIN ShasthyaKormiLinkedVillageEntity sklv ON sv.villageId = sklv.villageId
        WHERE sklv.shasthyaKormiId = :shasthyaKormiId
        """,
    )
    suspend fun getSubVillagesByShasthyaKormiId(shasthyaKormiId: Long): List<SubVillageEntity>

    @Query(
        """
        SELECT DISTINCT sv.* FROM SubVillageEntity sv
        INNER JOIN ShasthyaKormiLinkedVillageEntity sklv ON sv.villageId = sklv.villageId
        WHERE sklv.shasthyaKormiId IN (:shasthyaKormiIds)
        """,
    )
    suspend fun getSubVillagesByShasthyaKormiIds(shasthyaKormiIds: List<Long>): List<SubVillageEntity>

    @Query(
        """
        SELECT v.* FROM VillageEntity v
        INNER JOIN ShasthyaKormiLinkedVillageEntity sklv ON v.id = sklv.villageId
        WHERE sklv.shasthyaKormiId = :shasthyaKormiId AND v.chiefdomId = :chiefdomId
        """,
    )
    suspend fun getVillagesForShasthyaKormiAndChiefdom(
        shasthyaKormiId: Long,
        chiefdomId: Long,
    ): List<VillageEntity>

    // ShasthyaShebikaLinkedVillage methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShasthyaShebikaLinkedVillages(linkedVillages: List<ShasthyaShebikaLinkedVillageEntity>)

    @Query("DELETE FROM ShasthyaShebikaLinkedVillageEntity")
    suspend fun deleteAllShasthyaShebikaLinkedVillages()

    @Query(
        "SELECT sv.* FROM SubVillageEntity sv " +
            "INNER JOIN ShasthyaShebikaLinkedVillageEntity sslv ON sv.id = sslv.subVillageId " +
            "WHERE sslv.shasthyaShebikaId = :shasthyaShebikaId",
    )
    suspend fun getSubVillagesByShasthyaShebikaId(shasthyaShebikaId: Long): List<SubVillageEntity>

    @Query(
        """
    SELECT DISTINCT sv.*
    FROM SubVillageEntity sv
    INNER JOIN ShasthyaShebikaLinkedVillageEntity sslv
        ON sv.id = sslv.subVillageId
    WHERE sslv.shasthyaShebikaId IN (:shasthyaShebikaIds)
    """,
    )
    suspend fun getSubVillagesByShasthyaShebikaIds(shasthyaShebikaIds: List<Long>): List<SubVillageEntity>

    @Query("SELECT * FROM FormEntity where formType=:formTypeOne OR formType=:formTypeTwo")
    suspend fun getFormBasedOnType(
        formTypeOne: String,
        formTypeTwo: String,
    ): List<FormEntity>

    @Query("SELECT * FROM VillageEntity ORDER BY name ASC")
    suspend fun getVillageList(): List<VillageEntity>

    @Query("SELECT * FROM VillageEntity")
    suspend fun getOtherVillage(): VillageEntity

    @Query("SELECT * FROM ProgramEntity")
    suspend fun getProgramList(): List<ProgramEntity>

    @Query("SELECT * FROM ProgramEntity where id = :selectedParent")
    suspend fun getProgramList(selectedParent: Long): List<ProgramEntity>

    @Query("SELECT * FROM SiteEntity where subCountyId =:districtID AND siteLevel = 'Level 1'")
    suspend fun getUpazilaListByDistrict(districtID: Long): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity where siteLevel = 'Level 1'")
    suspend fun getUpazilaList(): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity Where userId=:userId")
    suspend fun getAccountSiteList(userId: Long): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity Where userId=:userId AND siteLevel=:level")
    suspend fun getAccountSiteListByLevel(
        userId: Long,
        level: String,
    ): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity where siteLevel = 'Level 1' AND eyeCare = 1")
    suspend fun getUpazilaListEyeCareOnly(): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity where siteLevel = 'Level 1' AND cataract = 1")
    suspend fun getUpazilaListCataractOnly(): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity Where userSite=:userSite AND userId=:userId")
    suspend fun getSiteEntityList(
        userSite: Boolean,
        userId: Long,
    ): List<SiteEntity>

    @Query("SELECT * FROM SiteEntity where id=:upazilaId")
    suspend fun getUpazilaById(upazilaId: Long): SiteEntity

    @Query("SELECT * FROM VillageEntity where id=:villageId")
    suspend fun getVillageById(villageId: Long): VillageEntity

    @Query("SELECT * FROM TreatmentPlanEntity")
    suspend fun getTreatmentPlanData(): List<TreatmentPlanEntity>

    @Query("SELECT * FROM diagnosis")
    suspend fun getDiagnosisList(): List<DiagnosisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDiagnosis(diseaseEntityList: ArrayList<DiagnosisEntity>)

    @Query("DELETE FROM DiagnosisEntity")
    suspend fun deleteDiagnosisList()

    @Query("SELECT * FROM shortageReason where type = :type")
    suspend fun getShortageEntries(type: String): List<ShortageReasonEntity>

    @Query("SELECT * FROM comorbidity WHERE (type IN (:workflowList)) OR type  IS NULL ORDER BY display_order ASC")
    suspend fun getComorbidityBasedOnWorkflow(workflowList: ArrayList<String>): List<ComorbidityEntity>

    @Query("SELECT * FROM comorbidity ORDER BY display_order ASC")
    suspend fun getComorbidity(): List<ComorbidityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveComorbidity(list: ArrayList<ComorbidityEntity>)

    @Query("DELETE FROM comorbidity")
    suspend fun deleteComorbidity()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveComplication(list: ArrayList<ComplicationEntity>)

    @Query("DELETE FROM complication")
    suspend fun deleteComplication()

    @Query("SELECT * FROM complication ORDER BY display_order ASC")
    suspend fun getComplication(): List<ComplicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCurrentMedication(list: ArrayList<CurrentMedicationEntity>)

    @Query("DELETE FROM current_medication")
    suspend fun deleteCurrentMedication()

    @Query("SELECT * FROM current_medication ORDER BY display_order ASC")
    suspend fun getCurrentMedicationList(): List<CurrentMedicationEntity>

    @Query("SELECT * FROM current_medication where type =:type or type = 'Other' ORDER BY display_order ASC")
    suspend fun getCurrentMedicationList(type: String): List<CurrentMedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePhysicalExamination(list: ArrayList<PhysicalExaminationEntity>)

    @Query("DELETE FROM physical_examination")
    suspend fun deletePhysicalExamination()

    @Query("SELECT * FROM physical_examination WHERE (type IN (:workFlowList)) OR type  IS NULL ORDER BY display_order ASC")
    suspend fun getPhysicalExaminationList(workFlowList: ArrayList<String>): List<PhysicalExaminationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveComplaints(list: ArrayList<ComplaintsEntity>)

    @Query("DELETE FROM complaints")
    suspend fun deleteComplaints()

    @Query("SELECT * FROM complaints WHERE (type IN (:workflowList)) OR type  IS NULL ORDER BY display_order ASC")
    suspend fun getChiefComplaints(workflowList: ArrayList<String>): List<ComplaintsEntity>

    @Query("SELECT * FROM diagnosis where (UPPER(gender) IN (:gender) OR UPPER(diagnosis) = 'OTHER') AND (UPPER(type) NOT IN (:type)) ORDER BY display_order ASC")
    suspend fun getDiagnosis(
        gender: ArrayList<String>,
        type: ArrayList<String>,
    ): List<DiagnosisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomsList(symptomEntity: List<SymptomEntity>)

    @Query("DELETE FROM Symptom")
    suspend fun deleteSymptomList()

    @Query("SELECT * FROM Symptom ORDER BY display_order")
    suspend fun getSymptomList(): List<SymptomEntity>

    @Query("SELECT * FROM Symptom WHERE LOWER(type) = LOWER(:type) ORDER BY display_order")
    suspend fun getSymptomsListByType(type: String): List<SymptomEntity>

    @Query("SELECT * FROM SiteEntity where siteLevel = 'Level 6'")
    suspend fun getOperatingUnitSites(): List<SiteEntity>

    @Query("DELETE FROM SiteEntity")
    suspend fun deleteSiteList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiteDetails(siteEntity: List<SiteEntity>)

    @Query("SELECT * FROM SubVillageEntity WHERE villageId =:villageId")
    suspend fun getSubVillage(villageId: Long): List<SubVillageEntity>
}
