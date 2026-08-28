package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.ListEndpointReleasesResponse
import com.canni.runpod.data.api.dto.ListEndpointWorkersResponse
import com.canni.runpod.data.api.dto.ServerlessEndpoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerlessRepository @Inject constructor(
    private val api: RunPodApi,
) {
    suspend fun listEndpoints(): List<ServerlessEndpoint> {
        val res = api.listEndpoints()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).endpoints
    }

    suspend fun getEndpoint(id: String): ServerlessEndpoint {
        val res = api.getEndpoint(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun deleteEndpoint(id: String) {
        val res = api.deleteEndpoint(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
    }

    suspend fun listReleases(id: String): ListEndpointReleasesResponse {
        val res = api.listEndpointReleases(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun listWorkers(id: String): ListEndpointWorkersResponse {
        val res = api.listEndpointWorkers(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }
}
