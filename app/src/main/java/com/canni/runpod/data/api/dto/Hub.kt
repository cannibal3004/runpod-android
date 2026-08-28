package com.canni.runpod.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HubListing(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val category: String? = null,
    val type: String? = null,
    val stars: Int? = null,
    val deploys: Int? = null,
    val listedRelease: HubRelease? = null,
)

@Serializable
data class HubRelease(
    val id: String? = null,
    val name: String? = null,
    val tagName: String? = null,
    val branch: String? = null,
    val license: String? = null,
    val deploys: Int? = null,
    val config: String? = null,
    val build: HubBuild? = null,
)

@Serializable
data class HubBuild(
    val id: String? = null,
    val imageName: String? = null,
)

@Serializable
data class HubReleaseConfig(
    val runsOn: String? = null,
    val gpuCount: Int? = null,
    val gpuIds: String? = null,
    val containerDiskInGb: Int? = null,
    val env: List<HubConfigEnv>? = null,
)

@Serializable
data class HubConfigEnv(
    val key: String = "",
    val input: HubConfigEnvInput? = null,
)

@Serializable
data class HubConfigEnvInput(
    val name: String? = null,
    val type: String? = null,
    val description: String? = null,
    @SerialName("default")
    val defaultValue: JsonElement? = null,
    val advanced: Boolean? = null,
)
