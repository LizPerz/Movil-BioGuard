package com.example.bioguard_movil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_heartbeats", indices = [Index(value = ["timestamp"])])
data class PendingHeartbeatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pacienteId: String? = null,
    val bateria: Int? = null,
    val sensoresActivos: List<String>? = null,
    val timestamp: String = ""
)