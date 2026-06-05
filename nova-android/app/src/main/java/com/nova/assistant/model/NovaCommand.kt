package com.nova.assistant.model

data class NovaCommand(
    val id: String,
    val action: String,
    val params: Map<String, Any>
)
