package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiError
import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.GraphQLQuery
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.RunPodGraphQLApi
import com.canni.runpod.data.api.dto.CreatePodRequest
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.PodAction
import com.canni.runpod.data.api.dto.PodEditResult
import com.canni.runpod.data.api.dto.PodMigration
import com.canni.runpod.data.api.dto.UpdatePodLockedRequest
import com.canni.runpod.data.api.PodActionRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Singleton
class PodRepository @Inject constructor(
    private val api: RunPodApi,
    private val graphQL: RunPodGraphQLApi,
    private val json: Json,
) {
    suspend fun listPods(): List<Pod> {
        val res = api.listPods()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).pods
    }

    suspend fun getPod(id: String): Pod {
        val res = api.getPod(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun act(id: String, action: PodAction): Pod {
        val res = api.podAction(id, PodActionRequest(action))
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun setLocked(id: String, locked: Boolean): Pod {
        val res = api.updatePod(id, UpdatePodLockedRequest(locked))
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun createPod(request: CreatePodRequest): Pod {
        val res = api.createPod(request)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun migratePod(podId: String): PodMigration {
        val data = executeGraphql(
            query = """
                mutation CreatePodMigration(${DOLLAR}input: MigratePodInput!) {
                  migratePod(input: ${DOLLAR}input) {
                    id
                    sourcePodId
                    targetPodId
                    migrationType
                    sourceMount
                    status
                    createdAt
                    updatedAt
                  }
                }
            """,
            variables = buildJsonObject {
                put("input", buildJsonObject { put("podId", podId) })
            },
            root = "migratePod",
        )
        return json.decodeFromJsonElement<PodMigration>(requireNotNull(data))
    }

    suspend fun migrationStatus(migrationId: String): PodMigration? {
        val data = executeGraphql(
            query = """
                query PodMigrationStatus(${DOLLAR}migrationId: String!) {
                  podMigrationById(migrationId: ${DOLLAR}migrationId) {
                    id
                    status
                    progress
                    message
                    migrationType
                    sourcePodId
                    targetPodId
                    sourceMachineId
                    targetMachineId
                    sourceMount
                    createdAt
                    updatedAt
                  }
                }
            """,
            variables = buildJsonObject { put("migrationId", migrationId) },
            root = "podMigrationById",
            nullableRoot = true,
        )
        return data?.let { json.decodeFromJsonElement<PodMigration>(it) }
    }

    suspend fun resumePodWithCpuOnly(podId: String) {
        executeGraphql(
            query = """
                mutation podResumeZeroGpu(${DOLLAR}input: podResumeZeroGpuInput!) {
                  podResumeZeroGpu(input: ${DOLLAR}input) {
                    id
                    desiredStatus
                    gpuCount
                    vcpuCount
                    memoryInGb
                  }
                }
            """,
            variables = buildJsonObject {
                put("input", buildJsonObject { put("podId", podId) })
            },
            root = "podResumeZeroGpu",
        )
    }

    suspend fun editPod(
        podId: String,
        imageName: String,
        dockerArgs: String,
        containerDiskInGb: Int,
        volumeInGb: Int?,
        volumeMountPath: String?,
        ports: String,
        env: List<Pair<String, String>>,
    ): PodEditResult {
        val data = executeGraphql(
            query = """
                mutation editPodJob(${DOLLAR}input: PodEditJobInput!) {
                  podEditJob(input: ${DOLLAR}input) {
                    id
                    env
                    port
                    ports
                    dockerArgs
                    imageName
                    containerDiskInGb
                    volumeInGb
                    volumeMountPath
                  }
                }
            """,
            variables = buildJsonObject {
                put(
                    "input",
                    buildJsonObject {
                        put("podId", podId)
                        put("imageName", imageName)
                        put("dockerArgs", dockerArgs)
                        put("containerDiskInGb", containerDiskInGb)
                        if (volumeInGb != null) {
                            put("volumeInGb", volumeInGb)
                        } else {
                            put("volumeInGb", JsonNull)
                        }
                        if (volumeMountPath != null) {
                            put("volumeMountPath", volumeMountPath)
                        }
                        put("ports", ports)
                        put(
                            "env",
                            buildJsonArray {
                                env.forEach { (key, value) ->
                                    add(buildJsonObject {
                                        put("key", key)
                                        put("value", value)
                                    })
                                }
                            },
                        )
                        put("containerRegistryAuthId", JsonNull)
                    },
                )
            },
            root = "podEditJob",
        )
        return json.decodeFromJsonElement<PodEditResult>(requireNotNull(data))
    }

    private suspend fun executeGraphql(
        query: String,
        variables: JsonObject,
        root: String,
        nullableRoot: Boolean = false,
    ): JsonElement? {
        val res = graphQL.execute(
            GraphQLQuery(
                query = query.trimIndent(),
                variables = variables,
            ),
        )
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        val body = requireNotNull(res.body())
        body.errors?.firstOrNull()?.message?.let { message ->
            throw ApiError(400, message)
        }
        val rootElement = body.data?.jsonObject?.get(root)
        if (rootElement == null || rootElement is JsonNull) {
            if (nullableRoot) return null
            throw ApiError(400, "GraphQL response is missing '$root'.")
        }
        return rootElement
    }

    private companion object {
        private const val DOLLAR = "\$"
    }
}

