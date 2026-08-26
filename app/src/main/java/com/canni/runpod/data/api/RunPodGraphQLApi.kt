package com.canni.runpod.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class GraphQLQuery(
    val query: String,
)

@Serializable
data class GraphQLResponse(
    val data: JsonElement? = null,
    val errors: List<GraphQLError>? = null,
)

@Serializable
data class GraphQLError(
    val message: String? = null,
)

interface RunPodGraphQLApi {

    @POST("graphql")
    suspend fun execute(@Body body: GraphQLQuery): Response<GraphQLResponse>
}
