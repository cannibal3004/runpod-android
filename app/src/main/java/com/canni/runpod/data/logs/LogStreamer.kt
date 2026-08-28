package com.canni.runpod.data.logs

import com.canni.runpod.di.RunPodConfig
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LogStreamer @Inject constructor(
    @Named("sse") private val sseClient: OkHttpClient,
    private val config: RunPodConfig,
    private val json: Json,
) {
    interface Listener {
        fun onOpen()
        fun onEvent(event: LogEvent)
        fun onFailure(message: String, code: Int?)
        fun onClosed()
    }

    private var eventSource: EventSource? = null
    @Volatile
    private var generation = 0L

    fun stream(
        podId: String,
        source: String?,
        tail: Int,
        listener: Listener,
    ) {
        streamLogs("pods/$podId/logs", source, tail, listener)
    }

    fun streamWorkerLogs(
        endpointId: String,
        workerId: String,
        source: String?,
        tail: Int,
        listener: Listener,
    ) {
        streamLogs("serverless/$endpointId/workers/$workerId/logs", source, tail, listener)
    }

    fun streamLogs(
        logsPath: String,
        source: String?,
        tail: Int,
        listener: Listener,
    ) {
        stop()
        generation += 1
        val gen = generation
        val url = config.baseUrl.toHttpUrl()
            .resolve(logsPath)!!
            .newBuilder()
            .apply {
                addQueryParameter("tail", tail.toString())
                if (source != null) addQueryParameter("source", source)
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        eventSource = EventSources.createFactory(sseClient).newEventSource(
            request,
            object : EventSourceListener() {
                override fun onOpen(es: EventSource, response: Response) {
                    if (gen == generation) listener.onOpen()
                }

                override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                    if (gen != generation) return
                    runCatching { json.decodeFromString(LogEvent.serializer(), data) }
                        .onSuccess { listener.onEvent(it) }
                }

                override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                    if (gen != generation) return
                    val code = response?.code
                    val message = when {
                        code != null -> "HTTP $code"
                        else -> t?.message ?: "Connection lost"
                    }
                    listener.onFailure(message, code)
                }

                override fun onClosed(es: EventSource) {
                    if (gen != generation) listener.onClosed()
                }
            },
        )
    }

    fun stop() {
        generation += 1
        eventSource?.cancel()
        eventSource = null
    }
}
