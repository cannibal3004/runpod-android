package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListPodsResponse(
    val pods: List<Pod> = emptyList(),
)

@Serializable
data class Pod(
    val id: String,
    val name: String = "",
    val status: PodStatus = PodStatus.TERMINATED,
    val actions: List<PodAction> = emptyList(),
    val image: String = "",
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val registry: String? = null,
    val mounts: Mounts? = null,
    val gpu: GpuConfig? = null,
    val cpu: CpuConfig? = null,
    val cloud: Cloud? = null,
    val dataCenterId: String? = null,
    val cudaVersion: String? = null,
    val ssh: PodSsh? = null,
    val template: String? = null,
    val cost: Double? = null,
    val locked: Boolean? = null,
    val globalNetworking: PodGlobalNetworking? = null,
    val runtime: PodRuntime? = null,
    val createdAt: String? = null,
    val startedAt: String? = null,
)

@Serializable
enum class PodStatus {
    PROVISIONING,
    STARTING,
    RUNNING,
    EXITED,
    ERROR,
    TERMINATED,
}

@Serializable
enum class PodAction {
    start,
    stop,
    restart,
    terminate,
}

@Serializable
enum class Cloud {
    SECURE,
    COMMUNITY,
}

@Serializable
data class GpuConfig(
    val id: String? = null,
    val count: Int? = null,
)

@Serializable
data class CpuConfig(
    val id: String? = null,
    val vcpuCount: Int? = null,
    val memory: Int? = null,
)

@Serializable
data class Mounts(
    val persistent: PersistentMount? = null,
    val network: List<NetworkMount>? = null,
)

@Serializable
data class PersistentMount(
    val size: Int? = null,
    val path: String? = null,
)

@Serializable
data class NetworkMount(
    val volumeId: String? = null,
    val path: String? = null,
)

@Serializable
data class PodSsh(
    val proxy: PodSshEndpoint? = null,
    val direct: PodSshEndpoint? = null,
)

@Serializable
data class PodSshEndpoint(
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val command: String? = null,
)

@Serializable
data class PodGlobalNetworking(
    val enabled: Boolean? = null,
    val ip: String? = null,
    val internalDns: String? = null,
)

@Serializable
data class PodRuntime(
    val uptime: Long? = null,
    val gpus: List<PodGpuUtilization>? = null,
    val cpu: Utilization? = null,
    val memory: Utilization? = null,
    val ports: List<PodRuntimePort>? = null,
)

@Serializable
data class PodGpuUtilization(
    val util: Int? = null,
    val memoryUtil: Int? = null,
)

@Serializable
data class Utilization(
    val util: Int? = null,
)

@Serializable
data class PodRuntimePort(
    @kotlinx.serialization.SerialName("private") val privatePort: Int? = null,
    @kotlinx.serialization.SerialName("public") val publicPort: Int? = null,
    val type: String? = null,
    val ip: String? = null,
)
