package com.argumentor.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.argumentor.R
import com.argumentor.core.i18n.labelRes
import com.argumentor.core.ui.components.VoiceInputTextField
import com.argumentor.domain.model.ArgumentStrength
import com.argumentor.domain.model.ClaimDetail
import com.argumentor.domain.model.ClaimPosition
import com.argumentor.domain.model.EvidenceQuality
import com.argumentor.domain.model.EvidenceType
import com.argumentor.domain.model.RebuttalStyle
import com.argumentor.domain.model.TopicStance

@Composable
fun TopicEditorScreen(
    onNavigateBack: () -> Unit,
    onOpenDebate: (Long) -> Unit,
    onOpenStats: (Long) -> Unit,
    viewModel: TopicEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var claimText by remember { mutableStateOf("") }
    var claimPosition by remember { mutableStateOf(ClaimPosition.SUPPORT) }
    var claimStrength by remember { mutableStateOf(ArgumentStrength.MODERATE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.topic_title_label)) }
        )
        Spacer(Modifier.height(8.dp))
        VoiceInputTextField(
            value = state.summary,
            onValueChange = viewModel::updateSummary,
            label = stringResource(id = R.string.topic_summary_label),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopicStance.values().forEach { stance ->
                FilterChip(
                    selected = stance == state.stance,
                    onClick = { viewModel.updateStance(stance) },
                    label = { Text(stringResource(id = stance.labelRes())) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            viewModel.saveTopic { id ->
                if (state.topicId == 0L && id > 0L) onNavigateBack()
            }
        }) {
            Text(text = stringResource(id = R.string.save_topic))
        }
        if (state.topicId > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenStats(state.topicId) }) {
                    Text(stringResource(id = R.string.stats_title))
                }
                TextButton(onClick = { onOpenDebate(state.topicId) }) {
                    Text(stringResource(id = R.string.mode_debate))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(id = R.string.add_claim_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        VoiceInputTextField(
            value = claimText,
            onValueChange = { claimText = it },
            label = stringResource(id = R.string.claim_hint),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClaimPosition.values().forEach { position ->
                FilterChip(
                    selected = position == claimPosition,
                    onClick = { claimPosition = position },
                    label = { Text(stringResource(id = position.labelRes())) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArgumentStrength.values().forEach { strength ->
                FilterChip(
                    selected = strength == claimStrength,
                    onClick = { claimStrength = strength },
                    label = { Text(stringResource(id = strength.labelRes())) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (claimText.isNotBlank()) {
                viewModel.addClaim(claimText, claimPosition, claimStrength)
                claimText = ""
            }
        }, enabled = state.topicId > 0) {
            Text(stringResource(id = R.string.add_claim_action))
        }
        Spacer(Modifier.height(16.dp))
        state.claims.forEach { claimDetail ->
            ClaimCard(claimDetail = claimDetail, onAddEvidence = { type, text, quality ->
                viewModel.addEvidence(claimDetail.claim.id, type, text, quality)
            }, onAddRebuttal = { style, text ->
                viewModel.addRebuttal(claimDetail.claim.id, text, style)
            }, onAddQuestion = { prompt, answer ->
                viewModel.addQuestion(claimDetail.claim.id, prompt, answer)
            })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ClaimCard(
    claimDetail: ClaimDetail,
    onAddEvidence: (EvidenceType, String, EvidenceQuality) -> Unit,
    onAddRebuttal: (RebuttalStyle, String) -> Unit,
    onAddQuestion: (String, String) -> Unit
) {
    var evidenceText by remember(claimDetail.claim.id) { mutableStateOf("") }
    var rebuttalText by remember(claimDetail.claim.id) { mutableStateOf("") }
    var questionText by remember(claimDetail.claim.id) { mutableStateOf("") }
    var answerText by remember(claimDetail.claim.id) { mutableStateOf("") }
    var evidenceType by remember { mutableStateOf(EvidenceType.CITATION) }
    var evidenceQuality by remember { mutableStateOf(EvidenceQuality.MEDIUM) }
    var rebuttalStyle by remember { mutableStateOf(RebuttalStyle.COUNTER_EXAMPLE) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(claimDetail.claim.text, style = MaterialTheme.typography.titleMedium)
            val positionLabel = stringResource(id = claimDetail.claim.position.labelRes())
            val strengthLabel = stringResource(id = claimDetail.claim.strength.labelRes())
            Text(text = stringResource(id = R.string.claim_meta, positionLabel, strengthLabel))
            Spacer(Modifier.height(8.dp))
            if (claimDetail.evidences.isNotEmpty()) {
                claimDetail.evidences.forEach { evidence ->
                    val typeLabel = stringResource(id = evidence.type.labelRes())
                    Text(text = stringResource(id = R.string.evidence_item, typeLabel, evidence.content))
                }
                Spacer(Modifier.height(4.dp))
            }
            if (claimDetail.questions.isNotEmpty()) {
                Divider()
                claimDetail.questions.forEach { question ->
                    Text(text = stringResource(id = R.string.question_item, question.prompt, question.expectedAnswer))
                }
            }
            if (claimDetail.rebuttals.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                claimDetail.rebuttals.forEach { rebuttal ->
                    val styleLabel = stringResource(id = rebuttal.style.labelRes())
                    Text(text = stringResource(id = R.string.rebuttal_item, styleLabel, rebuttal.text))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = evidenceText,
                onValueChange = { evidenceText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.add_evidence_hint)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvidenceType.values().forEach { type ->
                    FilterChip(
                        selected = type == evidenceType,
                        onClick = { evidenceType = type },
                        label = { Text(stringResource(id = type.labelRes())) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvidenceQuality.values().forEach { quality ->
                    FilterChip(
                        selected = quality == evidenceQuality,
                        onClick = { evidenceQuality = quality },
                        label = { Text(stringResource(id = quality.labelRes())) }
                    )
                }
            }
            TextButton(onClick = {
                if (evidenceText.isNotBlank()) {
                    onAddEvidence(evidenceType, evidenceText, evidenceQuality)
                    evidenceText = ""
                }
            }) {
                Text(stringResource(id = R.string.add_evidence_action))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rebuttalText,
                onValueChange = { rebuttalText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.add_rebuttal_hint)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RebuttalStyle.values().forEach { style ->
                    FilterChip(
                        selected = style == rebuttalStyle,
                        onClick = { rebuttalStyle = style },
                        label = { Text(stringResource(id = style.labelRes())) }
                    )
                }
            }
            TextButton(onClick = {
                if (rebuttalText.isNotBlank()) {
                    onAddRebuttal(rebuttalStyle, rebuttalText)
                    rebuttalText = ""
                }
            }) { Text(stringResource(id = R.string.add_rebuttal_action)) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.add_question_hint)) }
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.add_answer_hint)) }
            )
            TextButton(onClick = {
                if (questionText.isNotBlank()) {
                    onAddQuestion(questionText, answerText)
                    questionText = ""
                    answerText = ""
                }
            }) { Text(stringResource(id = R.string.add_question_action)) }
        }
    }
}
