package com.canni.runpod.data.repo

import com.canni.runpod.data.api.ApiErrors
import com.canni.runpod.data.api.RunPodApi
import com.canni.runpod.data.api.dto.Template
import com.canni.runpod.data.api.dto.TemplateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val api: RunPodApi,
) {
    suspend fun list(): List<Template> {
        val res = api.listTemplates()
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body()).templates
    }

    suspend fun get(id: String): Template {
        val res = api.getTemplate(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun create(request: TemplateRequest): Template {
        val res = api.createTemplate(request)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun update(id: String, request: TemplateRequest): Template {
        val res = api.updateTemplate(id, request)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
        return requireNotNull(res.body())
    }

    suspend fun delete(id: String) {
        val res = api.deleteTemplate(id)
        if (!res.isSuccessful) throw ApiErrors.fromResponse(res.code(), res.errorBody()?.string())
    }
}
