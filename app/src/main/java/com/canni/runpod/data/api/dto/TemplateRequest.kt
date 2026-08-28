package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TemplateRequest(
    val name: String? = null,
    val image: String? = null,
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val mounts: JsonElement? = null,
    val serverless: Boolean? = null,
    val public: Boolean? = null,
    val category: String? = null,
    val startSsh: Boolean? = null,
    val startJupyter: Boolean? = null,
    val allowedCudaVersions: List<String>? = null,
)
