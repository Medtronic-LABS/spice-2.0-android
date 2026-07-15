package org.medtroniclabs.uhis.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP_CALL
import org.medtroniclabs.uhis.db.entity.EntitiesName.HOUSEHOLD
import org.medtroniclabs.uhis.db.entity.EntitiesName.HOUSEHOLD_MEMBER
import org.medtroniclabs.uhis.db.entity.EntitiesName.MEMBER_ASSESSMENT_HISTORY_ENTITY
import org.medtroniclabs.uhis.db.entity.EntitiesName.PREGNANCY_DETAIL
import org.medtroniclabs.uhis.db.entity.FU_COLUMN_REFERRAL_FACILITY_TYPE
import org.medtroniclabs.uhis.db.entity.HH_COLUMN_MONTHLY_INCOME_RANGE
import org.medtroniclabs.uhis.db.entity.INDEX_MAH_MEMBER_SERVICE_VISIT
import org.medtroniclabs.uhis.db.entity.INDEX_MAH_MEMBER_VISIT
import org.medtroniclabs.uhis.db.entity.INDEX_MAH_PRACTITIONER_ID
import org.medtroniclabs.uhis.db.entity.MAH_COLUMN_PRACTITIONER_ID
import org.medtroniclabs.uhis.db.entity.MAH_COLUMN_REFERRAL_FACILITY_TYPE

object SpiceDatabaseMigration {
    /**
     * We had added a fix for created at and updated at timestamp update
     * which requires users to fetch all the records from server.
     * To achieve that, we are a adding dummy migration for clearing last sync time
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            SecuredPreference.remove(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED)
        }
    }

    /**
     * Database migration from version 5 to 6.
     *
     * Clears [SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED] so assessment history is
     * refetched from the server with the new schema.
     *
     * [FOLLOW_UP_CALL]:
     * - Drop patientStatus, reason, and duration (duration is re-added as REAL).
     * - Add callType, isWillingToVisitUHC, visitRejectReason, otherVisitRejectReason,
     *   unSuccessfulCallReason.
     * - Add wrongNumber, calledByUserId, calledByUserFullName, calledByUserRole.
     *
     * [MEMBER_ASSESSMENT_HISTORY_ENTITY]:
     * - Add [MAH_COLUMN_PRACTITIONER_ID], serviceProvidedByName, serviceProvidedByRole,
     *   observations, [MAH_COLUMN_REFERRAL_FACILITY_TYPE].
     * - Add indexes on practitionerId, memberFhirId, (memberId, visitDate), and
     *   (memberId, serviceProvided, visitDate).
     *
     * [FOLLOW_UP]: add [FU_COLUMN_REFERRAL_FACILITY_TYPE].
     *
     * [HOUSEHOLD_MEMBER]: add created_by_role_name — tracks which role registered the member;
     * used to scope SK external-member lists.
     *
     * [HOUSEHOLD]: add [HH_COLUMN_MONTHLY_INCOME_RANGE].
     *
     * [PREGNANCY_DETAIL]: add index on (householdMemberLocalId, endAt).
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Clear last synced time, so that the assessment history will get refetched with latest data
            SecuredPreference.remove(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED)

            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN patientStatus;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN reason;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN duration;")

            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN duration REAL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN callType TEXT;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN isWillingToVisitUHC INTEGER;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN visitRejectReason TEXT;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN otherVisitRejectReason TEXT;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN unSuccessfulCallReason TEXT;")

            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN wrongNumber INTEGER NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN calledByUserId TEXT NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN calledByUserFullName TEXT NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN calledByUserRole TEXT NOT NULL;")

            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN $MAH_COLUMN_PRACTITIONER_ID TEXT;")
            db.execSQL("CREATE INDEX IF NOT EXISTS $INDEX_MAH_PRACTITIONER_ID ON $MEMBER_ASSESSMENT_HISTORY_ENTITY($MAH_COLUMN_PRACTITIONER_ID)")

            db.execSQL("ALTER TABLE $FOLLOW_UP ADD COLUMN $FU_COLUMN_REFERRAL_FACILITY_TYPE TEXT;")

            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN serviceProvidedByName TEXT;")
            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN serviceProvidedByRole TEXT;")
            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN observations TEXT;")
            db.execSQL("ALTER TABLE $HOUSEHOLD_MEMBER ADD COLUMN created_by_role_name TEXT;")

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_pregnancy_detail_member_local_id ON $PREGNANCY_DETAIL(householdMemberLocalId, endAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_member_assessment_history_member_fhir_id ON $MEMBER_ASSESSMENT_HISTORY_ENTITY(memberFhirId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS $INDEX_MAH_MEMBER_VISIT ON $MEMBER_ASSESSMENT_HISTORY_ENTITY(memberId, visitDate)")

            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN $MAH_COLUMN_REFERRAL_FACILITY_TYPE TEXT;")

            db.execSQL("CREATE INDEX IF NOT EXISTS $INDEX_MAH_MEMBER_SERVICE_VISIT ON $MEMBER_ASSESSMENT_HISTORY_ENTITY(memberId, serviceProvided, visitDate)")

            db.execSQL("ALTER TABLE $HOUSEHOLD ADD COLUMN $HH_COLUMN_MONTHLY_INCOME_RANGE TEXT;")
        }
    }
}
