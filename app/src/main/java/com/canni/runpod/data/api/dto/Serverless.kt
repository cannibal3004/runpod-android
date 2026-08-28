package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun JsonElement.contentOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

@Serializable
data class ListEndpointsResponse(
    val endpoints: List<ServerlessEndpoint> = emptyList(),
)

@Serializable
data class ServerlessEndpoint(
    val id: String,
    val name: String = "",
    val type: String? = null,
    val image: String? = null,
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val registry: String? = null,
    val requestUrls: EndpointRequestUrls? = null,
    val gpu: EndpointGpuConfig? = null,
    val cpu: List<EndpointCpuConfig>? = null,
    val workers: EndpointWorkers? = null,
    val scaling: JsonElement? = null,
    val dataCenterIds: List<String>? = null,
    val networkVolumes: List<String>? = null,
    val timeout: Int? = null,
    val flashboot: String? = null,
    val createdAt: String? = null,
) {
    val isLoadBalancer: Boolean get() = type.equals("LOAD_BALANCER", ignoreCase = true)

    val primaryUrl: String?
        get() = requestUrls?.takeIf { isLoadBalancer }?.base
            ?: requestUrls?.run
            ?: requestUrls?.runSync

    val scalingLabel: String?
        get() {
            val obj = scaling as? JsonObject ?: return null
            val t = obj["type"]?.contentOrNull() ?: return null
            val detail = when (t.uppercase()) {
                "REQUEST_COUNT" -> obj["requestCount"]?.contentOrNull()
                    ?.let { "per $it requests" }
                "QUEUE_DELAY" -> obj["queueDelay"]?.contentOrNull()
                    ?.let { "${it}s queue delay" }
                else -> null
            }
            return detail?.let { "$t ($it)" } ?: t
        }

    val gpuLabel: String?
        get() {
            val g = gpu ?: return cpu?.firstOrNull()?.let {
                val mem = it.memory?.let { m -> " / ${m} GB" } ?: ""
                "${it.vcpuCount ?: "?"} vCPU${mem}"
            }
            val pools = g.pools?.joinToString { it.lowercase().replace('_', ' ') } ?: return null
            return "${g.count ?: 1}x $pools"
        }
}

@Serializable
data class EndpointRequestUrls(
    val run: String? = null,
    val runSync: String? = null,
    val status: String? = null,
    val stream: String? = null,
    val cancel: String? = null,
    val retry: String? = null,
    val purgeQueue: String? = null,
    val base: String? = null,
    val health: String? = null,
)

@Serializable
data class EndpointGpuConfig(
    val pools: List<String> = emptyList(),
    val excludedTypes: List<String>? = null,
    val count: Int? = null,
    val allowedCudaVersions: List<String>? = null,
    val minCudaVersion: String? = null,
)

@Serializable
data class EndpointCpuConfig(
    val id: String? = null,
    val vcpuCount: Int? = null,
    val memory: Int? = null,
)

@Serializable
data class EndpointWorkers(
    val min: Int? = null,
    val max: Int? = null,
    val idleTimeout: Int? = null,
)

@Serializable
data class ListEndpointReleasesResponse(
    val endpointVersion: Int? = null,
    val rollout: RolloutSummary? = null,
    val releases: List<Release> = emptyList(),
)

@Serializable
data class RolloutSummary(
    val inProgress: Boolean? = null,
    val workersOnLatest: Int? = null,
    val workersTotal: Int? = null,
    val percentOnLatest: Int? = null,
)

@Serializable
data class Release(
    val id: String,
    val version: Int? = null,
    val source: String? = null,
    val buildId: String? = null,
    val createdByUserId: String? = null,
    val workerCount: Int? = null,
    val createdAt: String? = null,
    val diff: List<ReleaseDiffEntry> = emptyList(),
)

@Serializable
data class ReleaseDiffEntry(
    val field: String? = null,
    val old: JsonElement? = null,
    val new: JsonElement? = null,
) {
    val oldText: String
        get() = when (old) {
            null, is JsonNull -> ""
            is JsonPrimitive -> old.content
            else -> old.toString()
        }
    val newText: String
        get() = when (new) {
            null, is JsonNull -> ""
            is JsonPrimitive -> new.content
            else -> new.toString()
        }
}

@Serializable
data class ListEndpointWorkersResponse(
    val workers: List<EndpointWorker> = emptyList(),
    val summary: WorkerSummary? = null,
    val endpointVersion: Int? = null,
)

@Serializable
data class EndpointWorker(
    val id: String,
    val status: String? = null,
    val isStale: Boolean? = null,
    val version: Int? = null,
    val gpuCount: Int? = null,
    val image: String? = null,
    val uptimeSeconds: Int? = null,
    val gpuTypeId: String? = null,
    val dataCenterId: String? = null,
    val startedAt: String? = null,
)

@Serializable
data class WorkerSummary(
    val running: Int = 0,
    val idle: Int = 0,
    val initializing: Int = 0,
    val throttled: Int = 0,
    val unhealthy: Int = 0,
    val total: Int = 0,
)

@Serializable
data class CreateEndpointRequest(
    val name: String,
    val type: String,
    val image: String? = null,
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val registry: String? = null,
    val gpu: EndpointGpuConfig? = null,
    val cpu: List<EndpointCpuConfig>? = null,
    val workers: EndpointWorkers? = null,
    val scaling: JsonElement? = null,
    val dataCenterIds: List<String>? = null,
    val networkVolumes: List<String>? = null,
    val templateId: String? = null,
    val timeout: Int? = null,
    val flashboot: String? = null,
)

@Serializable
data class UpdateEndpointRequest(
    val name: String? = null,
    val image: String? = null,
    val args: String? = null,
    val disk: Int? = null,
    val env: Map<String, String>? = null,
    val ports: List<String>? = null,
    val registry: String? = null,
    val gpu: EndpointGpuConfig? = null,
    val cpu: List<EndpointCpuConfig>? = null,
    val workers: EndpointWorkers? = null,
    val scaling: JsonElement? = null,
    val dataCenterIds: List<String>? = null,
    val networkVolumes: List<String>? = null,
    val timeout: Int? = null,
    val flashboot: String? = null,
)
