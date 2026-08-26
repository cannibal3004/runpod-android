package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ListGpuTypesResponse(
    val gpus: List<GpuType> = emptyList(),
)

@Serializable
data class GpuType(
    val id: String,
    val name: String = "",
    val pool: String? = null,
    val manufacturer: String? = null,
    val memory: Int = 0,
    val secure: Boolean = false,
    val community: Boolean = false,
    val price: GpuPrice = GpuPrice(),
    val maxCount: GpuMaxCount = GpuMaxCount(),
    val availability: AvailabilityLevel? = null,
    val dataCenters: List<DataCenterAvailability>? = null,
    val cudaVersions: List<CudaVersionAvailability>? = null,
)

@Serializable
data class GpuPrice(
    val secure: Double = 0.0,
    val community: Double = 0.0,
    val serverless: Double? = null,
)

@Serializable
data class GpuMaxCount(
    val secure: Int = 1,
    val community: Int = 1,
)

@Serializable
enum class AvailabilityLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

@Serializable
data class DataCenterAvailability(
    val id: String,
    val name: String,
    val availability: AvailabilityLevel? = null,
)

@Serializable
data class CudaVersionAvailability(
    val version: String,
    val available: Boolean,
)

@Serializable
data class ListDataCentersResponse(
    val dataCenters: List<DataCenter> = emptyList(),
)

@Serializable
data class DataCenter(
    val id: String,
    val name: String,
    val region: String? = null,
    val globalNetwork: Boolean? = null,
    val networkVolumeTypes: List<VolumeType>? = null,
    val compliance: List<JsonElement>? = null,
    val gpuAvailability: List<JsonElement>? = null,
    val cpuAvailability: List<JsonElement>? = null,
)

@Serializable
data class ListTemplatesResponse(
    val templates: List<Template> = emptyList(),
)

@Serializable
data class Template(
    val id: String,
    val name: String = "",
    val image: String = "",
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val registry: String? = null,
    val mounts: Mounts? = null,
    val serverless: Boolean? = null,
    val public: Boolean? = null,
    val category: String? = null,
    val startSsh: Boolean? = null,
    val startJupyter: Boolean? = null,
    val allowedCudaVersions: List<String>? = null,
)
