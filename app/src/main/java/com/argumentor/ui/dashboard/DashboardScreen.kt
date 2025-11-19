package com.argumentor.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.argumentor.R
import com.argumentor.core.i18n.labelRes
import com.argumentor.core.ui.components.VoiceInputTextField
import com.argumentor.domain.model.TopicOverview
import com.argumentor.domain.model.TopicStance
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardScreen(
    onTopic: (Long) -> Unit,
    onDebate: (Long) -> Unit,
    onStats: (Long) -> Unit,
    onBackup: () -> Unit,
    onFallacies: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var quickTitle by remember { mutableStateOf("") }
    var quickSummary by remember { mutableStateOf("") }
    var quickStance by remember { mutableStateOf(TopicStance.NEUTRAL) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { createdId ->
            if (createdId > 0) {
                onTopic(createdId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.search_topics)) }
        )
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.quick_topic_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quickTitle,
                    onValueChange = { quickTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.topic_title_label)) }
                )
                Spacer(Modifier.height(8.dp))
                VoiceInputTextField(
                    value = quickSummary,
                    onValueChange = { quickSummary = it },
                    label = stringResource(id = R.string.topic_summary_label),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TopicStance.values().forEach { stance ->
                        FilterChip(
                            selected = stance == quickStance,
                            onClick = { quickStance = stance },
                            label = { Text(text = stringResource(id = stance.labelRes())) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    if (quickTitle.isNotBlank()) {
                        viewModel.quickCreateTopic(quickTitle, quickSummary, quickStance)
                        quickTitle = ""
                        quickSummary = ""
                    }
                }) {
                    Text(text = stringResource(id = R.string.create_topic_action))
                }
                TextButton(onClick = { onTopic(0L) }) {
                    Text(stringResource(id = R.string.open_editor_action))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBackup) {
                Text(stringResource(id = R.string.backup_center))
            }
            TextButton(onClick = onFallacies) {
                Text(stringResource(id = R.string.fallacy_catalog))
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.topics) { overview ->
                TopicCard(
                    overview = overview,
                    onOpen = { onTopic(overview.topic.id) },
                    onDebate = { onDebate(overview.topic.id) },
                    onStats = { onStats(overview.topic.id) }
                )
            }
        }
    }
}

@Composable
private fun TopicCard(
    overview: TopicOverview,
    onOpen: () -> Unit,
    onDebate: () -> Unit,
    onStats: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onOpen() }) {
        Column(Modifier.padding(16.dp)) {
            Text(overview.topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(overview.topic.summary, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.claim_count_label, overview.claimCount))
                Spacer(Modifier.width(12.dp))
                Text(text = stringResource(id = R.string.support_vs_challenge, overview.supportCount, overview.challengeCount))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDebate) { Text(stringResource(id = R.string.mode_debate)) }
                TextButton(onClick = onStats) { Text(stringResource(id = R.string.stats_title)) }
            }
        }
    }
}
