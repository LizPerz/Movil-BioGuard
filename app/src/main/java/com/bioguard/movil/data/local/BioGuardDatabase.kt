package com.bioguard.movil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        PendingReadingEntity::class, PendingGpsEntity::class, PendingEventEntity::class, PendingAlertEntity::class,
        CachedReadingEntity::class, CachedEventEntity::class, CachedAlertEntity::class,
        PendingPredictionMlEntity::class
    ],
    version = 12,
    exportSchema = true
)
abstract class BioGuardDatabase : RoomDatabase() {

    abstract fun pendingDataDao(): PendingDataDao
    abstract fun cachedDataDao(): CachedDataDao
    abstract fun pendingPredictionMlDao(): PendingPredictionMlDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_readings` ADD COLUMN `sourceMessageId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_readings_sourceMessageId` " +
                        "ON `pending_readings` (`sourceMessageId`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_readings` ADD COLUMN `pasos` INTEGER")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `accelX` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `accelY` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `accelZ` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `grasaCorporalPct` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `masaMuscularKg` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `faseSueno` TEXT NOT NULL DEFAULT 'Sueño Profundo'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cached_readings` ADD COLUMN `glucosaEstimadaMgDl` REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_predictions_ml` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`probabilidadPico` REAL NOT NULL, " +
                        "`nivelRiesgo` TEXT NOT NULL, " +
                        "`casoClinico` TEXT, " +
                        "`accionAutomatizada` TEXT, " +
                        "`imc` REAL, " +
                        "`z` REAL, " +
                        "`pPico` REAL, " +
                        "`recomendacion` TEXT, " +
                        "`horasEstimadas` INTEGER, " +
                        "`modeloVersion` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`intentosSincronizacion` INTEGER NOT NULL DEFAULT 0, " +
                        "`ultimaSincronizacion` INTEGER, " +
                        "`sincronizado` INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_predictions_ml_sincronizado` ON `pending_predictions_ml` (`sincronizado`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_predictions_ml_createdAt` ON `pending_predictions_ml` (`createdAt`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_readings` ADD COLUMN `probabilidadPico` REAL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_readings` ADD COLUMN `glucosaEstimadaMgDl` REAL")
            }
        }

        // v11 -> v12: GSR (sudoracionGsr) reemplazado por Estrés/HRV % (estresPct)
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_readings_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pulsoBpm` REAL NOT NULL, " +
                        "`temperaturaC` REAL NOT NULL, " +
                        "`estresPct` REAL NOT NULL, " +
                        "`hrv` REAL, " +
                        "`spo2` REAL, " +
                        "`pasos` INTEGER, " +
                        "`glucosaEstimadaMgDl` REAL, " +
                        "`probabilidadPico` REAL, " +
                        "`timestamp` TEXT NOT NULL, " +
                        "`sourceMessageId` TEXT)"
                )
                db.execSQL(
                    "INSERT INTO `pending_readings_new` (" +
                        "`id`, `pulsoBpm`, `temperaturaC`, `estresPct`, `hrv`, `spo2`, `pasos`, " +
                        "`glucosaEstimadaMgDl`, `probabilidadPico`, `timestamp`, `sourceMessageId`) " +
                        "SELECT `id`, `pulsoBpm`, `temperaturaC`, `sudoracionGsr`, `hrv`, `spo2`, `pasos`, " +
                        "`glucosaEstimadaMgDl`, `probabilidadPico`, `timestamp`, `sourceMessageId` FROM `pending_readings`"
                )
                db.execSQL("DROP TABLE `pending_readings`")
                db.execSQL("ALTER TABLE `pending_readings_new` RENAME TO `pending_readings`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_readings_timestamp` ON `pending_readings` (`timestamp`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_readings_sourceMessageId` ON `pending_readings` (`sourceMessageId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_readings_new` (" +
                        "`id` TEXT PRIMARY KEY NOT NULL, " +
                        "`pacienteId` TEXT NOT NULL, " +
                        "`pulsoBpm` REAL NOT NULL, " +
                        "`temperaturaC` REAL NOT NULL, " +
                        "`estresPct` REAL NOT NULL, " +
                        "`hrv` REAL NOT NULL, " +
                        "`spo2` REAL NOT NULL, " +
                        "`pasos` INTEGER NOT NULL, " +
                        "`calorias` REAL NOT NULL, " +
                        "`accelX` REAL NOT NULL DEFAULT 0.0, " +
                        "`accelY` REAL NOT NULL DEFAULT 0.0, " +
                        "`accelZ` REAL NOT NULL DEFAULT 0.0, " +
                        "`grasaCorporalPct` REAL NOT NULL DEFAULT 0.0, " +
                        "`masaMuscularKg` REAL NOT NULL DEFAULT 0.0, " +
                        "`faseSueno` TEXT NOT NULL DEFAULT 'Sueño Profundo', " +
                        "`glucosaEstimadaMgDl` REAL NOT NULL DEFAULT 0.0, " +
                        "`fechaHora` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `cached_readings_new` (" +
                        "`id`, `pacienteId`, `pulsoBpm`, `temperaturaC`, `estresPct`, `hrv`, `spo2`, `pasos`, " +
                        "`calorias`, `accelX`, `accelY`, `accelZ`, `grasaCorporalPct`, `masaMuscularKg`, " +
                        "`faseSueno`, `glucosaEstimadaMgDl`, `fechaHora`, `timestamp`) " +
                        "SELECT `id`, `pacienteId`, `pulsoBpm`, `temperaturaC`, `sudoracionGsr`, `hrv`, `spo2`, `pasos`, " +
                        "`calorias`, `accelX`, `accelY`, `accelZ`, `grasaCorporalPct`, `masaMuscularKg`, " +
                        "`faseSueno`, `glucosaEstimadaMgDl`, `fechaHora`, `timestamp` FROM `cached_readings`"
                )
                db.execSQL("DROP TABLE `cached_readings`")
                db.execSQL("ALTER TABLE `cached_readings_new` RENAME TO `cached_readings`")
            }
        }

        fun getInstance(context: Context): BioGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = com.bioguard.movil.util.SecurityUtils.getOrCreateDatabasePassphrase(context)
                System.loadLibrary("sqlcipher")
                EncryptedRoomMigration.migratePlaintextIfNeeded(
                    context.applicationContext,
                    "bioguard_offline_db",
                    passphrase
                )
                val factory = SupportOpenHelperFactory(passphrase)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BioGuardDatabase::class.java,
                    "bioguard_offline_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .fallbackToDestructiveMigration()
                    .openHelperFactory(factory)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): BioGuardDatabase = getInstance(context)
    }
}
