package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.CreatePodRequest
import com.canni.runpod.data.api.dto.Pod
import com.canni.runpod.data.api.dto.PodAction
import com.canni.runpod.data.api.PodActionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodRepository @Inject constructor(
    private val api: RunPodApi,
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

    suspend fun createPod(request: CreatePodRequest): Pod {
        val res = api.createPod(request)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }
}
