package org.medtroniclabs.uhis.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.db.entity.EntitiesName.FOLLOW_UP_CALL

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
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN patientStatus;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN reason;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN callType TEXT NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN isWillingToVisitUHC INTEGER NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN visitRejectReason TEXT NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN otherVisitRejectReason TEXT NOT NULL;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL ADD COLUMN unSuccessfulCallReason TEXT NOT NULL;")
        }
    }

    /**
     * Migration for changing [FOLLOW_UP_CALL]
     *
     * - Change duration from INT to REAL by dropping the column and adding column
     * - Remove not null constraint from callType, isWillingToVisitUHC, visitRejectReason,
     * otherVisitRejectReason,unSuccessfulCallReason by dropping and adding column
     * - Add new columns wrongNumber, calledByUserId, calledByUserName, calledByUserRole
     */
    val Migration_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN duration;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN callType;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN isWillingToVisitUHC;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN visitRejectReason;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN otherVisitRejectReason;")
            db.execSQL("ALTER TABLE $FOLLOW_UP_CALL DROP COLUMN unSuccessfulCallReason;")

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
        }
    }
}
