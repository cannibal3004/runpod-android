package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatePodRequest(
    val name: String,
    val image: String? = null,
    val templateId: String? = null,
    val gpu: CreateGpuConfig? = null,
    val cloud: Cloud? = null,
    val dataCenterIds: List<String>? = null,
    val mounts: Mounts? = null,
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val startSsh: Boolean? = null,
    val startJupyter: Boolean? = null,
)

@Serializable
data class CreateGpuConfig(
    val id: String,
    val count: Int? = null,
)
