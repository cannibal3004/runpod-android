package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListNetworkVolumesResponse(
    val networkVolumes: List<NetworkVolume> = emptyList(),
)

@Serializable
data class NetworkVolume(
    val id: String,
    val name: String = "",
    val size: Int? = null,
    val dataCenter: String? = null,
    val type: VolumeType? = null,
)

@Serializable
enum class VolumeType {
    STANDARD,
    HIGH_PERFORMANCE,
}

@Serializable
data class CreateNetworkVolumeRequest(
    val name: String,
    val size: Int,
    val dataCenter: String,
    val type: VolumeType? = null,
)

@Serializable
data class UpdateNetworkVolumeRequest(
    val name: String? = null,
    val size: Int? = null,
)
