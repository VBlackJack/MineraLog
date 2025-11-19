package com.argumentor.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
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
import com.argumentor.core.i18n.argumentStrengthLabelRes
import com.argumentor.core.i18n.claimPositionLabelRes
import com.argumentor.domain.model.DistributionSlice

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.padding(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(id = R.string.back_action)) }
        Text(state.topicTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(text = stringResource(id = R.string.stance_distribution))
        DistributionChart(
            slices = state.stance,
            labelResolver = { label ->
                claimPositionLabelRes(label)?.let { stringResource(id = it) } ?: label
            }
        )
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.strength_distribution))
        DistributionChart(
            slices = state.strength,
            labelResolver = { label ->
                argumentStrengthLabelRes(label)?.let { stringResource(id = it) } ?: label
            }
        )
    }
}

@Composable
private fun DistributionChart(
    slices: List<DistributionSlice>,
    labelResolver: @Composable (String) -> String
) {
    val total = slices.sumOf { it.count }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEach { slice ->
            Column(Modifier.fillMaxWidth()) {
                val labelText = labelResolver(slice.label)
                Text(text = stringResource(id = R.string.distribution_slice_label, labelText, slice.count))
                LinearProgressIndicator(
                    progress = slice.count.toFloat() / total.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
