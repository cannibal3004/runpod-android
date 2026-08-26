package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.CreateNetworkVolumeRequest
import com.canni.runpod.data.api.dto.NetworkVolume
import com.canni.runpod.data.api.dto.UpdateNetworkVolumeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkVolumeRepository @Inject constructor(
    private val api: RunPodApi,
) {
    suspend fun listNetworkVolumes(): List<NetworkVolume> {
        val res = api.listNetworkVolumes()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).networkVolumes
    }

    suspend fun createNetworkVolume(request: CreateNetworkVolumeRequest): NetworkVolume {
        val res = api.createNetworkVolume(request)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun updateNetworkVolume(id: String, request: UpdateNetworkVolumeRequest): NetworkVolume {
        val res = api.updateNetworkVolume(id, request)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun deleteNetworkVolume(id: String) {
        val res = api.deleteNetworkVolume(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
    }
}
