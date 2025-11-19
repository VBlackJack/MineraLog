package net.meshcore.mineralog.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R

/**
 * Dialog for entering password to decrypt encrypted ZIP backups.
 *
 * Features:
 * - Password field with show/hide toggle
 * - Attempt counter (max 3 attempts)
 * - Error feedback for wrong password
 * - Clear security messaging
 * - Secure password handling (converts to CharArray before passing to callback)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecryptPasswordDialog(
    attemptsRemaining: Int = 3,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    val passwordState = rememberSecurePasswordState()
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus password field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose { passwordState.clear() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (attemptsRemaining <= 1) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        title = {
            Text(
                text = stringResource(R.string.password_dialog_encrypted_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info text
                Text(
                    text = stringResource(R.string.password_dialog_encrypted_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Password field
                OutlinedTextField(
                    value = passwordState.reveal(),
                    onValueChange = { passwordState.updateFrom(it) },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )

                // Attempts remaining warning
                if (attemptsRemaining < 3) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (attemptsRemaining <= 1) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ),
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (attemptsRemaining) {
                                    0 -> "❌ No attempts remaining. Import cancelled."
                                    1 -> "⚠️ Last attempt! Incorrect password will cancel import."
                                    else -> "⚠️ $attemptsRemaining attempts remaining"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (attemptsRemaining <= 1) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val passwordChars = passwordState.export()
                    onConfirm(passwordChars)
                    passwordState.clear()
                },
                enabled = passwordState.isNotEmpty && attemptsRemaining > 0
            ) {
                Text("Decrypt & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
