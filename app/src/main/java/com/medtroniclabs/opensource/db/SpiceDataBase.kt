package com.medtroniclabs.opensource.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medtroniclabs.opensource.data.CulturesEntity
import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.DiseaseCategoryItems
import com.medtroniclabs.opensource.data.DosageFrequency
import com.medtroniclabs.opensource.data.ExaminationListItems
import com.medtroniclabs.opensource.data.LabourDeliveryMetaEntity
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.ProgramEntity
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.data.UnitMetricEntity
import com.medtroniclabs.opensource.db.converters.OfflineStatusTypeConverter
import com.medtroniclabs.opensource.db.dao.AboveFiveYearsDAO
import com.medtroniclabs.opensource.db.dao.AssessmentDAO
import com.medtroniclabs.opensource.db.dao.CallHistoryDao
import com.medtroniclabs.opensource.db.dao.ConsentFormDao
import com.medtroniclabs.opensource.db.dao.DiagnosisDAO
import com.medtroniclabs.opensource.db.dao.ExaminationsComplaintsDAO
import com.medtroniclabs.opensource.db.dao.ExaminationsDAO
import com.medtroniclabs.opensource.db.dao.FollowUpCallsDao
import com.medtroniclabs.opensource.db.dao.FollowUpDao
import com.medtroniclabs.opensource.db.dao.FrequencyDAO
import com.medtroniclabs.opensource.db.dao.HouseholdDAO
import com.medtroniclabs.opensource.db.dao.LabourDeliveryDAO
import com.medtroniclabs.opensource.db.dao.LinkHouseholdMemberDao
import com.medtroniclabs.opensource.db.dao.MemberDAO
import com.medtroniclabs.opensource.db.dao.MetaDataDAO
import com.medtroniclabs.opensource.db.dao.NCDFollowUpDao
import com.medtroniclabs.opensource.db.dao.NcdMedicalReviewDao
import com.medtroniclabs.opensource.db.dao.PregnancyDetailDao
import com.medtroniclabs.opensource.db.dao.RiskFactorDAO
import com.medtroniclabs.opensource.db.dao.ScreeningDAO
import com.medtroniclabs.opensource.db.entity.AssessmentEntity
import com.medtroniclabs.opensource.db.entity.CallHistory
import com.medtroniclabs.opensource.db.entity.ChiefDomEntity
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowConditionEntity
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowEntity
import com.medtroniclabs.opensource.db.entity.NCDMedicalReviewMetaEntity
import com.medtroniclabs.opensource.db.entity.ConsentEntity
import com.medtroniclabs.opensource.db.entity.ConsentForm
import com.medtroniclabs.opensource.db.entity.DistrictEntity
import com.medtroniclabs.opensource.db.entity.DosageDurationEntity
import com.medtroniclabs.opensource.db.entity.FollowUp
import com.medtroniclabs.opensource.db.entity.FollowUpCall
import com.medtroniclabs.opensource.db.entity.FormEntity
import com.medtroniclabs.opensource.db.entity.FrequencyEntity
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.db.entity.LinkHouseholdMember
import com.medtroniclabs.opensource.db.entity.LifestyleEntity
import com.medtroniclabs.opensource.db.entity.MedicalComplianceEntity
import com.medtroniclabs.opensource.db.entity.MentalHealthEntity
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.db.entity.NCDCallDetails
import com.medtroniclabs.opensource.db.entity.NCDDiagnosisEntity
import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.db.entity.NCDPatientDetailsEntity
import com.medtroniclabs.opensource.db.entity.PregnancyDetail
import com.medtroniclabs.opensource.db.entity.RiskFactorEntity
import com.medtroniclabs.opensource.db.entity.ScreeningEntity
import com.medtroniclabs.opensource.db.entity.SignsAndSymptomsEntity
import com.medtroniclabs.opensource.db.entity.TreatmentPlanEntity
import com.medtroniclabs.opensource.db.entity.UserProfileEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.ui.assessment.AssessmentNCDEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [HouseholdEntity::class, HouseholdMemberEntity::class, SignsAndSymptomsEntity::class, AssessmentEntity::class, MenuEntity::class,
        UserProfileEntity::class, VillageEntity::class, HealthFacilityEntity::class, ClinicalWorkflowEntity::class, FormEntity::class,
        ClinicalWorkflowConditionEntity::class, MedicalReviewMetaItems::class, DiseaseCategoryItems::class, ExaminationListItems::class, LabourDeliveryMetaEntity::class,
        FollowUp::class, FollowUpCall::class, PregnancyDetail::class, FrequencyEntity::class, ConsentForm::class,
        LinkHouseholdMember::class, CallHistory::class, ProgramEntity::class, CulturesEntity::class, ConsentEntity::class,
        MentalHealthEntity::class, MedicalComplianceEntity::class, ChiefDomEntity::class, DistrictEntity::class, ScreeningEntity::class,
        RiskFactorEntity::class, LifestyleEntity::class, NCDMedicalReviewMetaEntity::class, AssessmentNCDEntity::class, UnitMetricEntity::class,
        DosageFrequency::class, NCDDiagnosisEntity::class, TreatmentPlanEntity::class, ShortageReasonEntity::class, DosageDurationEntity::class, NCDFollowUp::class, NCDCallDetails::class, NCDPatientDetailsEntity::class],
    version = 2
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

    companion object {
        private const val DATABASE_NAME = "SpiceDataBase"

        @Volatile
        private var INSTANCE: SpiceDataBase? = null

        fun getInstance(context: Context): SpiceDataBase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }


        private fun buildDatabase(context: Context): SpiceDataBase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(BuildConfig.DB_PASSWORD.toByteArray(Charsets.UTF_8))
            val db = Room.databaseBuilder(
                context.applicationContext,
                SpiceDataBase::class.java,
                DATABASE_NAME
            )
            if (!BuildConfig.DEBUG)
                db.openHelperFactory(factory)

            /*Migration Scripts*/
            db.addMigrations(SpiceSLMigration.MIGRATION_1_2)

            if (CommonUtils.isNonCommunity())
                db.fallbackToDestructiveMigration()

            return db.build()
        }
    }
}