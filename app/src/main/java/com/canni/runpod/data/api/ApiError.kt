package com.canni.runpod.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiError(
    val code: Int,
    message: String,
) : Exception(message)

object ApiErrors {

    fun fromResponse(code: Int, body: String?): ApiError {
        val parsed = body?.let {
            runCatching {
                val el = Json.parseToJsonElement(it)
                el.jsonObject["detail"]?.jsonPrimitive?.content
                    ?: el.jsonObject["message"]?.jsonPrimitive?.content
                    ?: el.jsonObject["error"]?.jsonPrimitive?.content
            }.getOrNull()
        }
        val message = parsed?.takeIf { it.isNotBlank() } ?: ""
        return when (code) {
            401 -> ApiError(401, "Invalid or expired API key.")
            403 -> ApiError(403, message.ifEmpty { "Your API key lacks permission for this action." })
            404 -> ApiError(404, message.ifEmpty { "Resource not found." })
            else -> ApiError(code, message.ifEmpty { "Request failed (HTTP $code)." })
        }
    }
}
