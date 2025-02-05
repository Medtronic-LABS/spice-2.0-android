package com.medtroniclabs.opensource.app.analytics.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.medtroniclabs.opensource.app.analytics.model.Analytics
import com.medtroniclabs.opensource.app.analytics.model.UserJourneyAnalytics

@Database(entities = [Analytics::class, UserJourneyAnalytics::class], version = 1)
abstract class AnalyticsDatabase: RoomDatabase() {

    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        private const val DB_NAME = "mdt_analytics_db"

        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null

        fun getInstance(context: Context): AnalyticsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }


        private fun buildDatabase(context: Context): AnalyticsDatabase {
            val db = Room.databaseBuilder(
                context,
                AnalyticsDatabase::class.java,
                DB_NAME
            )
            return db.build()
        }
    }

}