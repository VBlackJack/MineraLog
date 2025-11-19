package com.argumentor.ui.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.argumentor.R
import java.io.File

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var jsonInput by remember { mutableStateOf("") }
    var topicIdField by remember { mutableStateOf("") }
    var markdownPreview by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text(stringResource(id = R.string.back_action)) }
        Text(text = stringResource(id = R.string.backup_center), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        val status = when {
            state.messageRes != null -> stringResource(id = state.messageRes)
            !state.messageText.isNullOrBlank() -> state.messageText
            else -> ""
        }
        Text(text = status)
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            viewModel.exportDatabase(dir)
        }) { Text(stringResource(id = R.string.export_json_action)) }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = jsonInput,
            onValueChange = { jsonInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.import_json_label)) },
            minLines = 4
        )
        Button(onClick = { viewModel.importDatabase(jsonInput) }) {
            Text(stringResource(id = R.string.import_json_action))
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = topicIdField,
            onValueChange = { topicIdField = it.filter { char -> char.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.topic_id_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            topicIdField.toLongOrNull()?.let { id ->
                val dir = File(context.filesDir, "exports").apply { mkdirs() }
                val pdfFile = File(dir, "topic_$id.pdf")
                viewModel.exportTopicPdf(id, pdfFile) {}
            }
        }) { Text(stringResource(id = R.string.export_pdf_action)) }
        Button(onClick = {
            topicIdField.toLongOrNull()?.let { id ->
                viewModel.exportTopicMarkdown(id) { markdownPreview = it }
            }
        }) { Text(stringResource(id = R.string.export_markdown_action)) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = markdownPreview,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.markdown_preview_label)) },
            readOnly = true,
            minLines = 6
        )
    }
}
