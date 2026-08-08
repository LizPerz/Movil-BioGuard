package com.bioguard.movil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PendingReadingEntity::class, PendingGpsEntity::class, PendingEventEntity::class, PendingAlertEntity::class,
        CachedReadingEntity::class, CachedEventEntity::class, CachedAlertEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BioGuardDatabase : RoomDatabase() {

    abstract fun pendingDataDao(): PendingDataDao
    abstract fun cachedDataDao(): CachedDataDao

    companion object {
        @Volatile
        private var INSTANCE: BioGuardDatabase? = null

        // v1 -> v2: índices en timestamp para las tablas pendientes
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_readings_timestamp` ON `pending_readings` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_gps_timestamp` ON `pending_gps` (`timestamp`)")
            }
        }

        // v2 -> v3: cola offline para eventos y alertas del reloj
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_events` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`dispositivoMac` TEXT NOT NULL, " +
                        "`nivelRiesgo` TEXT NOT NULL, " +
                        "`probabilidadMl` REAL NOT NULL, " +
                        "`descripcion` TEXT NOT NULL, " +
                        "`timestamp` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_alerts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`tipoAlerta` TEXT NOT NULL, " +
                        "`descripcion` TEXT NOT NULL, " +
                        "`latitud` REAL, " +
                        "`longitud` REAL, " +
                        "`timestamp` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_events_timestamp` ON `pending_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_alerts_timestamp` ON `pending_alerts` (`timestamp`)")
            }
        }

        // v3 -> v4: caché de lectura offline para lecturas, eventos y alertas
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_readings` (" +
                        "`id` TEXT PRIMARY KEY NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`pulsoBpm` REAL NOT NULL, " +
                        "`temperaturaC` REAL NOT NULL, " +
                        "`sudoracionGsr` REAL NOT NULL, " +
                        "`hrv` REAL NOT NULL, " +
                        "`spo2` REAL NOT NULL, " +
                        "`pasos` INTEGER NOT NULL, " +
                        "`calorias` REAL NOT NULL, " +
                        "`fechaHora` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_events` (" +
                        "`id` TEXT PRIMARY KEY NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`dispositivoMac` TEXT NOT NULL, " +
                        "`nivelRiesgo` TEXT NOT NULL, " +
                        "`probabilidadMl` REAL NOT NULL, " +
                        "`descripcion` TEXT NOT NULL, " +
                        "`fechaHora` TEXT NOT NULL, " +
                        "`atendido` INTEGER NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_alerts` (" +
                        "`id` TEXT PRIMARY KEY NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`tipoAlerta` TEXT NOT NULL, " +
                        "`descripcion` TEXT NOT NULL, " +
                        "`latitud` REAL, " +
                        "`longitud` REAL, " +
                        "`atendida` INTEGER NOT NULL, " +
                        "`fechaHora` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): BioGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                // Ensure MasterKey and encrypted DB passphrase are created in Keystore
                val passphrase = com.bioguard.movil.util.SecurityUtils.getOrCreateDatabasePassphrase(context)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BioGuardDatabase::class.java,
                    "bioguard_offline_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): BioGuardDatabase = getInstance(context)
    }
}
