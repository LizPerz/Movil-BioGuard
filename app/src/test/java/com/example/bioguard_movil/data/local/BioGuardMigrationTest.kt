package com.example.bioguard_movil.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BioGuardMigrationTest {

    private val dbName = "bioguard-migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BioGuardDatabase::class.java
    )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun createBaseDatabase(version: Int, withTimestampIndices: Boolean, seedData: Boolean) {
        context.deleteDatabase(dbName)
        context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_readings` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`pulsoBpm` REAL NOT NULL, `temperaturaC` REAL NOT NULL, " +
                    "`sudoracionGsr` REAL NOT NULL, `hrv` REAL, `spo2` REAL, " +
                    "`timestamp` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_gps` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`latitud` REAL NOT NULL, `longitud` REAL NOT NULL, " +
                    "`esEmergencia` INTEGER NOT NULL, `timestamp` TEXT NOT NULL)"
            )
            if (withTimestampIndices) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_readings_timestamp` ON `pending_readings` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_gps_timestamp` ON `pending_gps` (`timestamp`)")
            }
            if (seedData) {
                db.execSQL(
                    "INSERT INTO `pending_readings` (`pulsoBpm`,`temperaturaC`,`sudoracionGsr`,`hrv`,`spo2`,`timestamp`) " +
                        "VALUES (72.0, 36.5, 1.5, 55.0, 98.0, '2024-01-01T00:00:00Z')"
                )
                db.execSQL(
                    "INSERT INTO `pending_gps` (`latitud`,`longitud`,`esEmergencia`,`timestamp`) " +
                        "VALUES (-33.45, -70.66, 0, '2024-01-01T00:00:00Z')"
                )
            }
            db.execSQL("PRAGMA user_version = $version")
        }
    }

    private fun migrateToV3(vararg migrations: Migration): SupportSQLiteDatabase {
        val dbPath = context.getDatabasePath(dbName).absolutePath
        return helper.runMigrationsAndValidate(dbPath, 3, true, *migrations)
    }

    @Test
    fun migracionV1aV3_conservaLecturasYGps() {
        createBaseDatabase(version = 1, withTimestampIndices = false, seedData = true)

        migrateToV3(BioGuardDatabase.MIGRATION_1_2, BioGuardDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT `pulsoBpm`, `temperaturaC`, `sudoracionGsr` FROM `pending_readings`").use { cursor ->
                assertTrue("Debe conservar la lectura", cursor.moveToFirst())
                assertEquals(72.0, cursor.getDouble(0), 0.001)
                assertEquals(36.5, cursor.getDouble(1), 0.001)
                assertEquals(1.5, cursor.getDouble(2), 0.001)
            }
            db.query("SELECT `latitud`, `longitud` FROM `pending_gps`").use { cursor ->
                assertTrue("Debe conservar el gps", cursor.moveToFirst())
                assertEquals(-33.45, cursor.getDouble(0), 0.001)
                assertEquals(-70.66, cursor.getDouble(1), 0.001)
            }
            db.query("SELECT COUNT(*) FROM `pending_events`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM `pending_alerts`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migracionV2aV3_creaColasDeEventosYAlertas() {
        createBaseDatabase(version = 2, withTimestampIndices = true, seedData = true)

        migrateToV3(BioGuardDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT COUNT(*) FROM `pending_events`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM `pending_alerts`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM `pending_readings`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }
}
