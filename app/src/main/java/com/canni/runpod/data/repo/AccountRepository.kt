package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiError
import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.GraphQLQuery
import com.canni.runpod.data.api.GraphQLResponse
import com.canni.runpod.data.api.RunPodGraphQLApi
import com.canni.runpod.data.api.dto.AccountBalance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class AccountRepository @Inject constructor(
    private val api: RunPodGraphQLApi,
) {

    suspend fun balance(): AccountBalance {
        val res = api.execute(GraphQLQuery(BALANCE_QUERY))
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        val body = res.body() ?: throw ApiError(500, "Empty response.")
        throwIfGqlError(body)
        val me = body.data?.jsonObject?.get("myself")?.jsonObject ?: return AccountBalance()
        return AccountBalance(
            clientBalance = me.doubleOrNull("clientBalance"),
            currentSpendPerHr = me.doubleOrNull("currentSpendPerHr"),
            spendLimit = me.doubleOrNull("spendLimit"),
        )
    }

    private fun throwIfGqlError(body: GraphQLResponse) {
        body.errors?.firstOrNull()?.message
            ?.takeIf { it.isNotBlank() }
            ?.let { throw ApiError(400, it) }
    }

    private fun JsonObject.doubleOrNull(key: String): Double? =
        get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.toDoubleOrNull()

    companion object {
        private const val BALANCE_QUERY =
            "query { myself { clientBalance currentSpendPerHr spendLimit } }"
    }
}
