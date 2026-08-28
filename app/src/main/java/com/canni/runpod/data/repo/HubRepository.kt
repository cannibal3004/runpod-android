package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiError
import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.GraphQLQuery
import com.canni.runpod.data.api.RunPodGraphQLApi
import com.canni.runpod.data.api.dto.HubListing
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Singleton
class HubRepository @Inject constructor(
    private val graphQL: RunPodGraphQLApi,
    private val json: Json,
) {
    suspend fun listServerless(): List<HubListing> {
        val data = executeGraphql(
            query = """
                query GetHubListings(${DOLLAR}input: ListingsInput!) {
                  listings(input: ${DOLLAR}input) {
                    id
                    title
                    description
                    repoName
                    repoOwner
                    category
                    type
                    stars
                    deploys
                    listedRelease {
                      id
                      name
                      tagName
                    }
                  }
                }
            """,
            variables = buildJsonObject {
                put("input", buildJsonObject {})
            },
            root = "listings",
        )
        return json.decodeFromJsonElement<List<HubListing>>(data)
            .filter { it.type == "SERVERLESS" }
    }

    suspend fun getListing(id: String): HubListing {
        val data = executeGraphql(
            query = """
                query GetHubListing(${DOLLAR}id: String!) {
                  listing(id: ${DOLLAR}id) {
                    id
                    title
                    description
                    repoName
                    repoOwner
                    category
                    type
                    stars
                    deploys
                    listedRelease {
                      id
                      name
                      tagName
                      branch
                      license
                      deploys
                      config
                      build {
                        id
                        imageName
                      }
                    }
                  }
                }
            """,
            variables = buildJsonObject { put("id", id) },
            root = "listing",
        )
        return json.decodeFromJsonElement<HubListing>(data)
    }

    private suspend fun executeGraphql(
        query: String,
        variables: JsonObject,
        root: String,
    ): JsonElement {
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
            throw ApiError(404, "GraphQL response is missing '$root'.")
        }
        return rootElement
    }

    private companion object {
        private const val DOLLAR = "\$"
    }
}
