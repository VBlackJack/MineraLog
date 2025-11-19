package com.argumentor.core.voice

data class VoiceInputState(
    val text: String = "",
    val isListening: Boolean = false,
    val error: String? = null
)
