package com.argumentor.core.ui.components

import android.speech.SpeechRecognizer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.argumentor.R
import com.argumentor.core.voice.VoiceInputController

@Composable
fun VoiceInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { VoiceInputController(SpeechRecognizer.createSpeechRecognizer(context)) }
    val state by controller.state.collectAsState()

    LaunchedEffect(state.text) {
        if (state.text.isNotBlank() && state.text != value) {
            onValueChange(state.text)
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.close() }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    if (state.isListening) controller.stop() else controller.start(prompt = label)
                },
                enabled = SpeechRecognizer.isRecognitionAvailable(context)
            ) {
                val icon = if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic
                Icon(icon, contentDescription = stringResource(id = R.string.voice_input_toggle))
            }
        },
        supportingText = {
            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
