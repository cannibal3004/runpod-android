package com.canni.runpod.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SshKeys(
    val keys: List<String> = emptyList(),
)

@Serializable
data class UpdateSshKeysRequest(
    val keys: List<String>,
)
