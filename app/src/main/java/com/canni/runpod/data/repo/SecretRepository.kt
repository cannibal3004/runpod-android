package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiError
import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.GraphQLQuery
import com.canni.runpod.data.api.RunPodGraphQLApi
import com.canni.runpod.data.api.dto.Secret
import javax.inject.Inject
import javax.inject.Singleton
import com.canni.runpod.data.api.GraphQLResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

@Singleton
class SecretRepository @Inject constructor(
    private val api: RunPodGraphQLApi,
    private val json: Json,
) {

    suspend fun listSecrets(): List<Secret> {
        val res = api.execute(GraphQLQuery(LIST_QUERY))
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        val body = res.body() ?: return emptyList()
        throwIfGqlError(body)
        val arr = body.data?.jsonObject?.get("myself")?.jsonObject?.get("secrets")?.jsonArray
            ?: return emptyList()
        return arr.map { json.decodeFromJsonElement(Secret.serializer(), it) }
    }

    suspend fun createSecret(name: String, value: String, description: String?): Secret {
        val input = buildString {
            append("{ name: ").append(gqlString(name))
            append(", value: ").append(gqlString(value))
            if (!description.isNullOrBlank()) {
                append(", description: ").append(gqlString(description.trim()))
            }
            append(" }")
        }
        val query = "mutation { secretCreate(input: $input) { id name description createdAt updatedAt } }"
        val res = api.execute(GraphQLQuery(query))
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        val body = res.body() ?: throw ApiError(500, "Empty response.")
        throwIfGqlError(body)
        val el = body.data?.jsonObject?.get("secretCreate")
            ?: throw ApiError(500, "Unexpected response: no secret returned.")
        return json.decodeFromJsonElement(Secret.serializer(), el)
    }

    suspend fun deleteSecret(id: String) {
        val query = "mutation { secretDelete(id: ${gqlString(id)}) }"
        val res = api.execute(GraphQLQuery(query))
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        val body = res.body() ?: return
        throwIfGqlError(body)
    }

    private fun throwIfGqlError(body: GraphQLResponse) {
        body.errors?.firstOrNull()?.message
            ?.takeIf { it.isNotBlank() }
            ?.let { throw ApiError(400, it) }
    }

    private fun gqlString(value: String): String = json.encodeToString(serializer<String>(), value)

    companion object {
        private const val LIST_QUERY =
            "query { myself { secrets { id name description createdAt updatedAt } } }"
    }
}
