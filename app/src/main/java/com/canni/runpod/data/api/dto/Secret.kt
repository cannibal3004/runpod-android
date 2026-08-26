package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class Secret(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val reference: String get() = "{{ RUNPOD_SECRET_$name }}"
}
