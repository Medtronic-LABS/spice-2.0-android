package org.medtroniclabs.uhis.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.data.CulturesEntity
import org.medtroniclabs.uhis.data.DiseaseCategoryItems
import org.medtroniclabs.uhis.data.DosageFrequency
import org.medtroniclabs.uhis.data.ExaminationListItems
import org.medtroniclabs.uhis.data.LabourDeliveryMetaEntity
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.ProgramEntity
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.db.converters.OfflineStatusTypeConverter
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
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.db.entity.MenuEntity
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
import org.medtroniclabs.uhis.ui.assessment.AssessmentNCDEntity

@Database(
    entities = [
        HouseholdEntity::class, HouseholdMemberEntity::class, SignsAndSymptomsEntity::class, AssessmentEntity::class, MenuEntity::class,
        UserProfileEntity::class, VillageEntity::class, HealthFacilityEntity::class, ClinicalWorkflowEntity::class, FormEntity::class,
        ClinicalWorkflowConditionEntity::class, MedicalReviewMetaItems::class, DiseaseCategoryItems::class,
        ExaminationListItems::class, LabourDeliveryMetaEntity::class,
        FollowUp::class, FollowUpCall::class, PregnancyDetail::class, FrequencyEntity::class, ConsentForm::class,
        LinkHouseholdMember::class, CallHistory::class, ProgramEntity::class, CulturesEntity::class, ConsentEntity::class,
        MentalHealthEntity::class, MedicalComplianceEntity::class, ChiefDomEntity::class, DistrictEntity::class, ScreeningEntity::class,
        RiskFactorEntity::class, LifestyleEntity::class, NCDMedicalReviewMetaEntity::class, AssessmentNCDEntity::class, UnitMetricEntity::class,
        DosageFrequency::class, NCDDiagnosisEntity::class, TreatmentPlanEntity::class,
        ShortageReasonEntity::class, DosageDurationEntity::class, NCDFollowUp::class,
        LinkedVillageEntity::class, NCDCallDetails::class, NCDPatientDetailsEntity::class,
        CommunityProfile::class, RxBuddyDetails::class, TreatmentDetailsEntity::class,
        RxBuddyFollowUpEntity::class,
        SubVillageEntity::class, ShasthyaShebikaEntity::class, ShasthyaShebikaLinkedVillageEntity::class,
        MemberAssessmentHistoryEntity::class, DiagnosisEntity::class,
        SiteEntity::class, ComorbidityEntity::class,
        ComplicationEntity::class, CurrentMedicationEntity::class, PhysicalExaminationEntity::class, ComplaintsEntity::class,
        SymptomEntity::class, ShasthyaKormiEntity::class, ShasthyaKormiLinkedVillageEntity::class,
    ],
    version = 6,
    autoMigrations = [
        AutoMigration(1, 2),
        AutoMigration(2, 3),
        AutoMigration(4, 5),
    ],
)
@TypeConverters(OfflineStatusTypeConverter::class)
abstract class SpiceDataBase : RoomDatabase() {
    abstract fun householdDAO(): HouseholdDAO

    abstract fun memberDAO(): MemberDAO

    abstract fun assessmentDAO(): AssessmentDAO

    abstract fun metaDataDAO(): MetaDataDAO

    abstract fun examinationsComplaintsDAO(): ExaminationsComplaintsDAO

    abstract fun diagnosisDAO(): DiagnosisDAO

    abstract fun aboveFiveYearsDAO(): AboveFiveYearsDAO

    abstract fun examinationsDAO(): ExaminationsDAO

    abstract fun labourDeliveryDAO(): LabourDeliveryDAO

    abstract fun followUpDao(): FollowUpDao

    abstract fun followUpCallsDao(): FollowUpCallsDao

    abstract fun pregnancyDetailDao(): PregnancyDetailDao

    abstract fun frequencyDao(): FrequencyDAO

    abstract fun consentFormDao(): ConsentFormDao

    abstract fun linkHouseholdMemberDao(): LinkHouseholdMemberDao

    abstract fun callHistoryDao(): CallHistoryDao

    abstract fun screeningDAO(): ScreeningDAO

    abstract fun riskFactorDao(): RiskFactorDAO

    abstract fun ncdMedicalReviewDao(): NcdMedicalReviewDao

    abstract fun ncdFollowUpDao(): NCDFollowUpDao

    abstract fun communityDetailsDao(): CommunityDetailsDAO

    abstract fun rxBuddyDao(): RxBuddyDetailsDAO

    abstract fun treatmentDetailsDao(): TreatmentDetailsDAO

    abstract fun rxBuddyFollowUpDao(): RxBuddyFollowUpDAO

    abstract fun hivMetaDataDAO(): HivMetaDataDAO

    abstract fun memberAssessmentHistoryDao(): MemberAssessmentHistoryDao

    companion object {
        private const val DATABASE_NAME = "SpiceDataBase"

        @Volatile
        private var instance: SpiceDataBase? = null

        fun getInstance(context: Context): SpiceDataBase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context): SpiceDataBase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(BuildConfig.ROOM_DB_ENCRYPTION_KEY.toByteArray(Charsets.UTF_8))
            val db = Room
                .databaseBuilder(
                    context.applicationContext,
                    SpiceDataBase::class.java,
                    DATABASE_NAME,
                ).addMigrations(SpiceDatabaseMigration.MIGRATION_3_4)
                .addMigrations(SpiceDatabaseMigration.MIGRATION_5_6)
            if (!BuildConfig.DEBUG) {
                db.openHelperFactory(factory)
            }

            return db.build()
        }
    }
}
