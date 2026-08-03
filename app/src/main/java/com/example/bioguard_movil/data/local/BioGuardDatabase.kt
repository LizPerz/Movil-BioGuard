package com.example.bioguard_movil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PendingReadingEntity::class, PendingGpsEntity::class, PendingEventoEntity::class, PendingAlertaEntity::class, PendingHeartbeatEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BioGuardDatabase : RoomDatabase() {

    abstract fun pendingDataDao(): PendingDataDao

    companion object {
        @Volatile
        private var INSTANCE: BioGuardDatabase? = null

        fun getDatabase(context: Context): BioGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BioGuardDatabase::class.java,
                    "bioguard_offline_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
