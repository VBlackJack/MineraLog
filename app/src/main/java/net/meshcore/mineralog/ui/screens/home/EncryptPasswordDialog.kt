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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R

/**
 * Dialog for setting password for encrypted ZIP backups.
 *
 * Features:
 * - Password + confirmation fields
 * - Password strength indicator (Weak/Medium/Strong)
 * - Show/hide password toggle
 * - Validation (min 8 chars, confirmation match)
 * - Clear security messaging (Argon2+AES-256)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val passwordStrength = calculatePasswordStrength(password)
    val passwordsMatch = password == confirmPassword && password.isNotEmpty()
    val isValid = password.length >= 8 && passwordsMatch

    // Auto-focus password field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.export_encrypt),
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
                    text = "Protect your backup with encryption using Argon2id + AES-256-GCM.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.export_password)) },
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
                    },
                    isError = password.isNotEmpty() && password.length < 8,
                    supportingText = {
                        if (password.isNotEmpty() && password.length < 8) {
                            Text("Password must be at least 8 characters")
                        }
                    }
                )

                // Password strength indicator
                if (password.isNotEmpty()) {
                    val strengthText = when (passwordStrength) {
                        PasswordStrength.WEAK -> "Weak"
                        PasswordStrength.MEDIUM -> "Medium"
                        PasswordStrength.STRONG -> "Strong"
                    }

                    // Track strength changes for announcements
                    var lastStrength by remember { mutableStateOf<PasswordStrength?>(null) }
                    LaunchedEffect(passwordStrength) {
                        if (lastStrength != null && lastStrength != passwordStrength) {
                            // Strength has changed - will be announced via liveRegion
                        }
                        lastStrength = passwordStrength
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.semantics {
                            contentDescription = "Password strength indicator"
                            stateDescription = "Current strength: $strengthText"
                            liveRegion = LiveRegionMode.Polite
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strength:",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = when (passwordStrength) {
                                    PasswordStrength.WEAK -> "Weak"
                                    PasswordStrength.MEDIUM -> "Medium"
                                    PasswordStrength.STRONG -> "Strong"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (passwordStrength) {
                                    PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                                    PasswordStrength.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                    PasswordStrength.STRONG -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        LinearProgressIndicator(
                            progress = when (passwordStrength) {
                                PasswordStrength.WEAK -> 0.33f
                                PasswordStrength.MEDIUM -> 0.66f
                                PasswordStrength.STRONG -> 1.0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = when (passwordStrength) {
                                PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                                PasswordStrength.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                PasswordStrength.STRONG -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                // Confirm password field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.export_password_confirm)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (confirmVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                            Text("Passwords do not match")
                        }
                    }
                )

                // Security note
                if (isValid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Your backup will be encrypted and can only be restored with this password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(password)
                    onDismiss()
                },
                enabled = isValid
            ) {
                Text("Encrypt & Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Password strength levels.
 */
private enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG
}

/**
 * Calculate password strength based on length and complexity.
 */
private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 8) return PasswordStrength.WEAK

    var score = 0

    // Length
    if (password.length >= 12) score++
    if (password.length >= 16) score++

    // Complexity
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score >= 5 -> PasswordStrength.STRONG
        score >= 3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}
