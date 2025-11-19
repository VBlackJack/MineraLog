package com.argumentor.ui.fallacies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.argumentor.R
import com.argumentor.domain.model.Fallacy

@Composable
fun FallacyScreen(
    onBack: () -> Unit,
    viewModel: FallacyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) { Text(stringResource(id = R.string.back_action)) }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(state.items) { fallacy ->
                FallacyCard(fallacy)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FallacyCard(fallacy: Fallacy) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(fallacy.name, style = MaterialTheme.typography.titleMedium)
            Text(fallacy.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = stringResource(id = R.string.fallacy_example, fallacy.example), style = MaterialTheme.typography.bodySmall)
        }
    }
}
