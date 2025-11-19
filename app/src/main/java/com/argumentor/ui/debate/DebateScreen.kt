package com.argumentor.ui.debate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.argumentor.R
import com.argumentor.core.i18n.labelRes

@Composable
fun DebateScreen(
    onBack: () -> Unit,
    viewModel: DebateViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showBack by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = state.topicTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (state.cards.isEmpty()) {
            Text(stringResource(id = R.string.no_flashcards))
        } else {
            val card = state.cards[state.currentIndex]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (showBack) {
                        Text(text = stringResource(id = R.string.rebuttal_title), style = MaterialTheme.typography.titleMedium)
                        card.rebuttals.forEach { rebuttal ->
                            val styleLabel = stringResource(id = rebuttal.style.labelRes())
                            Text(text = stringResource(id = R.string.rebuttal_item, styleLabel, rebuttal.text))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(id = R.string.question_prompt_title))
                        card.questions.forEach { question ->
                            Text(
                                text = stringResource(
                                    id = R.string.question_item,
                                    question.prompt,
                                    question.expectedAnswer
                                )
                            )
                        }
                    } else {
                        Text(text = card.claim.text, style = MaterialTheme.typography.titleMedium)
                        val positionLabel = stringResource(id = card.claim.position.labelRes())
                        val strengthLabel = stringResource(id = card.claim.strength.labelRes())
                        Text(text = stringResource(id = R.string.claim_meta, positionLabel, strengthLabel))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            RowButtons(
                onFlip = { showBack = !showBack },
                onNext = {
                    showBack = false
                    viewModel.advance()
                },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun RowButtons(onFlip: () -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text(stringResource(id = R.string.back_action)) }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onFlip) { Text(stringResource(id = R.string.flip_card)) }
        Button(onClick = onNext) { Text(stringResource(id = R.string.next_card)) }
    }
}
