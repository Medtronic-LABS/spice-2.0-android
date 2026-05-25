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
}
