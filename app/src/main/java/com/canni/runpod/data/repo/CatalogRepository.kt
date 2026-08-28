package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.CpuType
import com.canni.runpod.data.api.dto.DataCenter
import com.canni.runpod.data.api.dto.GpuType
import com.canni.runpod.data.api.dto.Template
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val api: RunPodApi,
) {
    suspend fun listGpuTypes(): List<GpuType> {
        val res = api.listGpuTypes()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).gpus
    }

    suspend fun listServerlessGpuTypes(): List<GpuType> {
        val res = api.listGpuTypes(include = "AVAILABILITY", product = "SERVERLESS")
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).gpus
    }

    suspend fun listCpuTypes(): List<CpuType> {
        val res = api.listCpuTypes()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).cpus
    }

    suspend fun listDataCenters(): List<DataCenter> {
        val res = api.listDataCenters()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).dataCenters
    }

    suspend fun listOwnTemplates(): List<Template> {
        val res = api.listTemplates()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).templates
    }

    suspend fun listPublicTemplates(): List<Template> {
        val res = api.listPublicTemplates()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).templates
    }

    suspend fun listAllTemplates(): List<Template> {
        val pub = runCatching { listPublicTemplates() }.getOrNull() ?: emptyList()
        val own = runCatching { listOwnTemplates() }.getOrNull() ?: emptyList()
        return own + pub.filter { p -> own.none { it.id == p.id } }
    }
}
