package com.medtroniclabs.opensource.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SpiceSLMigration {

    val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(database: SupportSQLiteDatabase) {
            /* 1. Household Entity Migration
            *   1.1. Changing household_no from NOT Null field to Nullable Field
            * */
            database.execSQL(
                """
            CREATE TABLE Household_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                household_no INTEGER,
                name TEXT NOT NULL,
                village_id INTEGER NOT NULL,
                landmark TEXT,
                head_phone_number TEXT,
                head_phone_number_category TEXT,
                no_of_people INTEGER NOT NULL,
                is_owned_an_improved_latrine INTEGER NOT NULL,
                is_owned_hand_washing_facility_with_soap INTEGER NOT NULL,
                is_owned_a_treated_bed_net INTEGER NOT NULL,
                bed_net_count INTEGER,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                version TEXT,
                lastUpdated TEXT,
                fhir_id TEXT,
                sync_status TEXT NOT NULL DEFAULT 'NotSynced',
                created_by INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
        """
            )

            database.execSQL(
                """
            INSERT INTO Household_temp (id, household_no, name, village_id, landmark,
                                                head_phone_number, no_of_people, is_owned_an_improved_latrine,
                                                is_owned_hand_washing_facility_with_soap, is_owned_a_treated_bed_net,
                                                bed_net_count, latitude, longitude, version, lastUpdated, fhir_id,
                                                sync_status, created_by, updated_at, created_at)
            SELECT id, household_no, name, village_id, landmark,
                   head_phone_number, no_of_people, is_owned_an_improved_latrine,
                   is_owned_hand_washing_facility_with_soap, is_owned_a_treated_bed_net,
                   bed_net_count, latitude, longitude, version, lastUpdated, fhir_id,
                   sync_status, created_by, updated_at, created_at
            FROM Household
            """
            )

            database.execSQL("DROP TABLE Household")
            database.execSQL("ALTER TABLE Household_temp RENAME TO Household")
            database.execSQL("Update Household SET head_phone_number_category = (SELECT member.phone_number_category  From HouseholdMember AS member WHERE member.household_id = Household.id AND member.household_head_relationship = 'HouseholdHead')")

            /* 2. Assessment Entity Migration
            *   2.1. Added householdMemberLocalId - for keeping local id as foreign key for assessment
            *   2.2. Changing patientId from NOT Null field to Nullable Field
            *   2.3. Setting householdMemberLocalId by using member patient id and member fhir id
            * */

            database.execSQL("ALTER TABLE Assessment ADD COLUMN householdMemberLocalId INTEGER NOT NULL DEFAULT 0")
            database.execSQL(
                """
            UPDATE Assessment
            SET householdMemberLocalId = (
                SELECT member.id
                FROM HouseholdMember AS member
                WHERE member.patient_id = Assessment.patientId AND (member.fhir_id IS NULL OR member.fhir_id = Assessment.memberId)
            )
            WHERE patientId IS NOT NULL 
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS Assessment_new (
                 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                 householdMemberLocalId INTEGER NOT NULL DEFAULT 0,
                 memberId TEXT,
                 householdId TEXT,
                 patientId TEXT,
                 villageId TEXT NOT NULL,
                 assessmentType TEXT NOT NULL,
                 assessmentDetails TEXT NOT NULL,
                 otherDetails TEXT,
                 isReferred INTEGER NOT NULL,
                 referralStatus TEXT NOT NULL,
                 referredReason TEXT,
                 followUpId INTEGER,
                 latitude REAL NOT NULL,
                 longitude REAL NOT NULL,
                 fhir_id TEXT,
                 sync_status TEXT NOT NULL,
                 created_by INTEGER NOT NULL,
                 updated_at INTEGER NOT NULL,
                 created_at INTEGER NOT NULL
                 )
            """.trimIndent()
            )

            database.execSQL(
                """
            INSERT INTO Assessment_new (id, householdMemberLocalId, memberId, householdId, patientId, villageId, assessmentType, assessmentDetails, otherDetails, isReferred, referralStatus, referredReason, followUpId, latitude, longitude, fhir_id, sync_status, created_by, updated_at, created_at)
            SELECT id, householdMemberLocalId, memberId, householdId, patientId, villageId, assessmentType, assessmentDetails, otherDetails, isReferred, referralStatus, referredReason, followUpId, latitude, longitude, fhir_id, sync_status, created_by, updated_at, created_at
            FROM Assessment
            """.trimIndent()
            )

            database.execSQL("DROP TABLE Assessment")
            database.execSQL("ALTER TABLE Assessment_new RENAME TO Assessment")

            /*
            * Mother Reference Id for kid
            * */

            database.execSQL("ALTER TABLE HouseholdMember ADD COLUMN motherReferenceId INTEGER")

            database.execSQL(
                """
                UPDATE HouseholdMember
                    SET motherReferenceId = (
                    SELECT member.id
                    FROM HouseholdMember AS member
                    WHERE member.patient_id = HouseholdMember.parentId
                )
                WHERE parentId IS NOT NULL
                """.trimIndent()
                )

            database.execSQL("UPDATE HouseholdMember SET household_head_relationship = \"Wife / Husband\" WHERE household_head_relationship = \"Spouse / Partner\"")

            /* 3. PregnancyDetail Entity Migration
            *   3.1. Added householdMemberLocalId - for keeping local id as foreign key for PregnancyDetail
            *   3.2. Added neonateHouseholdMemberLocalId - for keeping neonate local id  as foreign key for PregnancyDetail
            *   3.3. Setting householdMemberLocalId by using member patient id and member fhir id
            *   3.4. Setting neonateHouseholdMemberLocalId by using member patient id
            * */

            database.execSQL("ALTER TABLE PregnancyDetail ADD COLUMN householdMemberLocalId INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE PregnancyDetail ADD COLUMN neonateHouseholdMemberLocalId INTEGER")

            database.execSQL(
                """
            UPDATE PregnancyDetail
            SET householdMemberLocalId = (
                SELECT member.id
                FROM HouseholdMember AS member
                WHERE member.patient_id = PregnancyDetail.patientId AND (member.fhir_id IS NULL OR member.fhir_id = PregnancyDetail.householdMemberId)
            )
            WHERE patientId IS NOT NULL
            """.trimIndent()
            )

            database.execSQL(
                """
            UPDATE PregnancyDetail
            SET neonateHouseholdMemberLocalId = (
                SELECT member.id
                FROM HouseholdMember AS member
                WHERE member.patient_id = PregnancyDetail.neonatePatientId
            )
            WHERE neonatePatientId IS NOT NULL
            """.trimIndent()
            )

            // Create newly added table LinkHouseholdMember
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS LinkHouseholdMember (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    memberId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    syncStatus TEXT
                )
                """.trimIndent()
            )

            // Create newly added table CallHistory
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS CallHistory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    referenceId TEXT NOT NULL,
                    callStartTime INTEGER NOT NULL,
                    callEndTime INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent())

            database.execSQL("ALTER TABLE DiagnosisEntity ADD COLUMN cultureValue TEXT")
            database.execSQL("ALTER TABLE FrequencyEntity ADD COLUMN displayValue TEXT")
            database.execSQL("ALTER TABLE ClinicalWorkflowConditionEntity ADD COLUMN groupName TEXT")
            database.execSQL("ALTER TABLE ClinicalWorkflowConditionEntity ADD COLUMN category TEXT")

            // Create a new table with the updated schema
            database.execSQL("""
            CREATE TABLE IF NOT EXISTS VillageEntity_new (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                villagecode TEXT,
                chiefdomId INTEGER,
                countryId INTEGER NOT NULL,
                districtId INTEGER,
                chiefdomCode TEXT,
                districtCode TEXT
            )
            """.trimIndent()
            )

            database.execSQL("""
            INSERT INTO VillageEntity_new (id, name, villagecode, chiefdomId, countryId, districtId, chiefdomCode, districtCode)
            SELECT id, name, villagecode, chiefdomId, countryId, districtId, chiefdomCode, districtCode FROM VillageEntity
            """.trimIndent()
            )

            database.execSQL("DROP TABLE VillageEntity")
            database.execSQL("ALTER TABLE VillageEntity_new RENAME TO VillageEntity")

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS ProgramEntity (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                health_facilities TEXT NOT NULL,
                tenant_id INTEGER NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS CulturesEntity (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent())

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS ConsentEntity (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                formType TEXT NOT NULL,
                formInput TEXT NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `MentalHealthEntity` (
                `formType` TEXT NOT NULL,
                `formInput` TEXT,
                PRIMARY KEY(`formType`)
            )
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS MedicalComplianceEntity (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                display_order INTEGER,
                display_value TEXT,
                parent_compliance_id INTEGER,
                child_exists INTEGER NOT NULL DEFAULT 0,
                culture_value TEXT
            )
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS ChiefDomEntity (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                code TEXT,
                districtId INTEGER NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS DistrictEntity (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                code TEXT,
                countryId INTEGER NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `ScreeningEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `screeningDetails` TEXT NOT NULL,
                `generalDetails` TEXT NOT NULL,
                `uploadStatus` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `userId` TEXT,
                `signature` BLOB,
                `isReferred` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `RiskFactorEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nonLabEntity` TEXT NOT NULL,
                `uploadStatus` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `LifestyleEntity` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `name` TEXT NOT NULL,
                `displayValue` TEXT,
                `value` TEXT,
                `answers` TEXT NOT NULL, 
                `type` TEXT NOT NULL,
                `displayOrder` INTEGER NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `NCDMedicalReviewMetaEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `displayValue` TEXT NOT NULL,
                `displayOrder` INTEGER NOT NULL,
                `type` TEXT,
                `category` TEXT,
                `value` TEXT
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `AssessmentNCDEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `assessmentDetails` TEXT NOT NULL, 
                `uploadStatus` INTEGER NOT NULL DEFAULT 0, 
                `createdAt` INTEGER NOT NULL, 
                `userId` INTEGER
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `unit_metric_entity` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `unit` TEXT NOT NULL,
                `type` TEXT,
                `displayOrder` INTEGER NOT NULL,
                `description` TEXT,
                `status` INTEGER NOT NULL,
                `displayValue` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `answerDependent` INTEGER NOT NULL,
                `childExists` INTEGER NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `dosage_frequency_entity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `displayOrder` INTEGER NOT NULL,
                `frequency` INTEGER NOT NULL DEFAULT 1,
                `description` TEXT,
                `status` INTEGER NOT NULL,
                `displayValue` TEXT,
                `isDefault` INTEGER NOT NULL,
                `answerDependent` INTEGER NOT NULL,
                `childExists` INTEGER NOT NULL
            )
            """.trimIndent()
            )

            database.execSQL(
                """
            CREATE TABLE IF NOT EXISTS NCDDiagnosisEntity (
                id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                displayOrder INTEGER NOT NULL,
                value TEXT,
                type TEXT,
                gender TEXT
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `TreatmentPlanEntity` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `name` TEXT NOT NULL,
                `displayValue` TEXT,
                `displayOrder` INTEGER NOT NULL,
                `duration` TEXT NOT NULL,
                `period` TEXT NOT NULL,
                `riskLevel` TEXT,
                `type` TEXT
            )
            """.trimIndent()
            )

            database.execSQL("""
            CREATE TABLE IF NOT EXISTS `shortageReason` (
                `id` INTEGER NOT NULL PRIMARY KEY, 
                `name` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `displayOrder` INTEGER NOT NULL, 
                `displayValue` TEXT NOT NULL
            )
            """.trimIndent()
            )

        }
    }
}