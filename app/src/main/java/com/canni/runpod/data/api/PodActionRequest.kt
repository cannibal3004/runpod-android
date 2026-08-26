package com.canni.runpod.data.api

import kotlinx.serialization.Serializable

@Serializable
data class PodActionRequest(
    val action: com.canni.runpod.data.api.dto.PodAction,
)
