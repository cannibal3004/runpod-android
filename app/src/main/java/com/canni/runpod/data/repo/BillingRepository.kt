package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.ListBillingResponse
import com.canni.runpod.data.api.dto.ListPodBillingResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val api: RunPodApi,
) {
    suspend fun summary(bucketSize: String, lastN: Int): ListBillingResponse {
        val res = api.billing(bucketSize = bucketSize, lastN = lastN)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun pods(bucketSize: String, lastN: Int): ListPodBillingResponse {
        val res = api.podBilling(bucketSize = bucketSize, lastN = lastN)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }
}
