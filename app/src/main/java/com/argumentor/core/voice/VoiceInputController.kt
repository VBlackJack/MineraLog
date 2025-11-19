package com.argumentor.core.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.Closeable
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VoiceInputController(private val speechRecognizer: SpeechRecognizer) : Closeable {
    private val _state = MutableStateFlow(VoiceInputState())
    val state: StateFlow<VoiceInputState> = _state

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true, error = null)
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false)
        }

        override fun onError(error: Int) {
            _state.value = _state.value.copy(
                isListening = false,
                error = error.toString()
            )
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _state.value = _state.value.copy(text = matches.first(), isListening = false)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _state.value = _state.value.copy(text = matches.first(), isListening = true)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    init {
        speechRecognizer.setRecognitionListener(listener)
    }

    fun start(locale: Locale = Locale.getDefault(), prompt: String? = null) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            prompt?.let { putExtra(RecognizerIntent.EXTRA_PROMPT, it) }
        }
        speechRecognizer.startListening(intent)
    }

    fun stop() {
        speechRecognizer.stopListening()
        _state.value = _state.value.copy(isListening = false)
    }

    override fun close() {
        speechRecognizer.destroy()
    }
}
