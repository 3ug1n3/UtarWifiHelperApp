package com.student.wifihelper.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.student.wifihelper.data.model.LoginLog
import com.student.wifihelper.data.model.PortalEndpoint
import com.student.wifihelper.data.model.UserConfig

@Database(entities = [UserConfig::class, LoginLog::class, PortalEndpoint::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun logDao(): LogDao
    abstract fun portalDao(): PortalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "utar_wifi_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
