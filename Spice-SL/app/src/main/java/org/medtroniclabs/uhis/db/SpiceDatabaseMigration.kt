package org.medtroniclabs.uhis.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.db.entity.COLUMN_PRACTITIONER_ID
import org.medtroniclabs.uhis.db.entity.COLUMN_REFERRAL_FACILITY_TYPE
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP_CALL
import org.medtroniclabs.uhis.db.entity.EntitiesName.MEMBER_ASSESSMENT_HISTORY_ENTITY
import org.medtroniclabs.uhis.db.entity.INDEX_PRACTITIONER_ID

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
     * Migration for changing [FOLLOW_UP_CALL]
     * - Drop patientStatus and reason as are not required
     * - Add callType, isWillingToVisitUHC, visitRejectReason, otherVisitRejectReason, unSuccessfulCallReason
     * - Add wrongNumber, calledByUserId, calledByUserName, calledByUserRole
     * - Add [COLUMN_PRACTITIONER_ID], serviceProvidedByName, serviceProvidedByRole to [MEMBER_ASSESSMENT_HISTORY_ENTITY]
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN calledByUserName TEXT NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN calledByUserRole TEXT NOT NULL;")

            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN $COLUMN_PRACTITIONER_ID TEXT;")
            db.execSQL("CREATE INDEX IF NOT EXISTS $INDEX_PRACTITIONER_ID ON $MEMBER_ASSESSMENT_HISTORY_ENTITY($COLUMN_PRACTITIONER_ID)")

            db.execSQL("ALTER TABLE $FOLLOW_UP ADD COLUMN $COLUMN_REFERRAL_FACILITY_TYPE TEXT;")

            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN serviceProvidedByName TEXT;")
            db.execSQL("ALTER TABLE $MEMBER_ASSESSMENT_HISTORY_ENTITY ADD COLUMN serviceProvidedByRole TEXT;")
        }
    }
}
