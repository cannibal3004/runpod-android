package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PodMigration(
    val id: String,
    val status: String? = null,
    val progress: Double? = null,
    val message: String? = null,
    val migrationType: String? = null,
    val sourcePodId: String? = null,
    val targetPodId: String? = null,
    val sourceMount: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
