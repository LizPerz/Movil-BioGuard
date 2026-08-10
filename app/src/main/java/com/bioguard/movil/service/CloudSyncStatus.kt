package com.bioguard.movil.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CloudSyncPhase { IDLE, RUNNING, SUCCESS, NO_NETWORK, FAILED }

data class CloudSyncStatus(
    val phase: CloudSyncPhase = CloudSyncPhase.IDLE,
    val pendingItems: Int? = null,
    val completedAtMillis: Long? = null
)

object CloudSyncStatusStore {
    private val mutableStatus = MutableStateFlow(CloudSyncStatus())
    val status = mutableStatus.asStateFlow()

    fun publish(status: CloudSyncStatus) {
        mutableStatus.value = status
    }
}
