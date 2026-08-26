package com.canni.runpod.di

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class AppLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isSse = request.url.encodedPath.endsWith("/logs")
        val start = System.nanoTime()

        val requestBody = if (!isSse) {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                redact(buffer.readUtf8()).take(MAX_LOG_CHARS)
            }.orEmpty()
        } else {
            ""
        }
        Log.i(
            TAG,
            "--> ${request.method} ${request.url}" +
                if (requestBody.isEmpty()) "" else "\n$requestBody",
        )

        val response = chain.proceed(request)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        if (response.isSuccessful) {
            Log.i(TAG, "<-- ${response.code} ${response.request.url} (${elapsedMs}ms)")
            return response
        }

        val body = response.body
        val errorText = body?.source()?.readUtf8().orEmpty()
        val rebuilt = if (body != null) {
            response.newBuilder()
                .body(errorText.toResponseBody(body.contentType()))
                .build()
        } else {
            response
        }
        Log.i(
            TAG,
            "<-- ${response.code} ${response.request.url} (${elapsedMs}ms)" +
                if (errorText.isEmpty()) "" else "\n${redact(errorText).take(MAX_LOG_CHARS)}",
        )
        return rebuilt
    }

    private fun redact(text: String): String =
        SECRET_PATTERN.replace(text) { match ->
            match.value.substringBefore(":") + ":\"***\""
        }

    companion object {
        private const val TAG = "RunPodHttp"
        private const val MAX_LOG_CHARS = 4_000
        private val SECRET_PATTERN = Regex(
            "\"[A-Za-z0-9_.-]*(?:token|key|secret|password|authorization)[A-Za-z0-9_.-]*\"\\s*:\\s*\"[^\"]*\"",
            RegexOption.IGNORE_CASE,
        )
    }
}
