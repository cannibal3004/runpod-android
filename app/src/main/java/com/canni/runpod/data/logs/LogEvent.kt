package com.canni.runpod.data.logs

import kotlinx.serialization.Serializable

@Serializable
data class LogEvent(
    val ts: String? = null,
    val source: String? = null,
    val line: String? = null,
)
