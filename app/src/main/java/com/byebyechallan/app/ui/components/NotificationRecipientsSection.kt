package com.byebyechallan.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.byebyechallan.app.data.model.NotificationRecipientState

@Composable
fun NotificationRecipientsSection(
    state: NotificationRecipientState,
    onStateChange: (NotificationRecipientState) -> Unit,
    defaultEmail: String = "",
    defaultMobile: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Notification recipients",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Turn on a channel to receive expiry alerts. You can add multiple recipients per channel, " +
                "or leave them blank and update later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        RecipientChannelRow(
            label = "Email",
            enabled = state.emailEnabled,
            recipients = state.emailRecipients,
            keyboardType = KeyboardType.Email,
            defaultRecipient = defaultEmail,
            onEnabledChange = { enabled ->
                onStateChange(
                    state.copy(
                        emailEnabled = enabled,
                        emailRecipients = ensureRecipientsOnEnable(enabled, state.emailRecipients, defaultEmail)
                    )
                )
            },
            onRecipientsChange = { onStateChange(state.copy(emailRecipients = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))

        RecipientChannelRow(
            label = "WhatsApp",
            enabled = state.whatsAppEnabled,
            recipients = state.whatsAppRecipients,
            keyboardType = KeyboardType.Phone,
            defaultRecipient = defaultMobile,
            onEnabledChange = { enabled ->
                onStateChange(
                    state.copy(
                        whatsAppEnabled = enabled,
                        whatsAppRecipients = ensureRecipientsOnEnable(enabled, state.whatsAppRecipients, defaultMobile)
                    )
                )
            },
            onRecipientsChange = { onStateChange(state.copy(whatsAppRecipients = it)) }
        )
        Spacer(modifier = Modifier.height(12.dp))

        RecipientChannelRow(
            label = "SMS",
            enabled = state.smsEnabled,
            recipients = state.smsRecipients,
            keyboardType = KeyboardType.Phone,
            defaultRecipient = defaultMobile,
            onEnabledChange = { enabled ->
                onStateChange(
                    state.copy(
                        smsEnabled = enabled,
                        smsRecipients = ensureRecipientsOnEnable(enabled, state.smsRecipients, defaultMobile)
                    )
                )
            },
            onRecipientsChange = { onStateChange(state.copy(smsRecipients = it)) }
        )
    }
}

@Composable
fun DocumentChannelNotAvailableInfo(
    channelLabel: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "No $channelLabel recipient on this profile. Add one in Profile settings to enable here.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun DocumentNotificationToggleRow(
    label: String,
    checked: Boolean,
    availableOnProfile: Boolean,
    profileLoaded: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showInfo by remember { mutableStateOf(false) }
    val notAvailable = profileLoaded && !availableOnProfile

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (notAvailable) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Why is this disabled?",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Box(contentAlignment = Alignment.Center) {
                Switch(
                    checked = if (availableOnProfile) checked else false,
                    onCheckedChange = { if (availableOnProfile) onCheckedChange(it) },
                    enabled = availableOnProfile
                )
                if (notAvailable) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showInfo = true }
                    )
                }
            }
        }
        if (notAvailable) {
            DocumentChannelNotAvailableInfo(channelLabel = label)
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("$label notifications unavailable") },
            text = {
                Text(
                    "You have not added any contact or recipient for $label on this profile, " +
                        "so you cannot enable $label notifications here. " +
                        "Open Profile settings, turn on $label, and add at least one recipient."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun RecipientChannelRow(
    label: String,
    enabled: Boolean,
    recipients: List<String>,
    keyboardType: KeyboardType,
    defaultRecipient: String,
    onEnabledChange: (Boolean) -> Unit,
    onRecipientsChange: (List<String>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            val fields = recipients.ifEmpty { listOf("") }
            fields.forEachIndexed { index, value ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            val updated = fields.toMutableList()
                            updated[index] = newValue
                            onRecipientsChange(updated)
                        },
                        label = { Text("$label recipient ${index + 1}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier = Modifier.weight(1f)
                    )
                    if (fields.size > 1) {
                        IconButton(
                            onClick = {
                                val updated = fields.toMutableList()
                                updated.removeAt(index)
                                onRecipientsChange(updated)
                            }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove recipient")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            TextButton(
                onClick = {
                    val next = fields + defaultRecipient.takeIf {
                        it.isNotBlank() && fields.none { existing -> existing == it }
                    }.orEmpty()
                    onRecipientsChange(next)
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add recipient")
            }
        }
    }
}

private fun ensureRecipientsOnEnable(
    enabled: Boolean,
    current: List<String>,
    defaultRecipient: String
): List<String> {
    if (!enabled) return current
    if (current.isNotEmpty()) return current
    return if (defaultRecipient.isNotBlank()) listOf(defaultRecipient) else listOf("")
}
