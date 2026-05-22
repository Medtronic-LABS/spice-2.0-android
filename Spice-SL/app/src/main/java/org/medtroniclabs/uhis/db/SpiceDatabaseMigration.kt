package org.medtroniclabs.uhis.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.medtroniclabs.uhis.common.SecuredPreference

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
}
