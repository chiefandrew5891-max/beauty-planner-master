package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.ClientSuggestion
import com.andrey.beautyplanner.ClientSuggestions
import com.andrey.beautyplanner.Locales

@Composable
fun ClientPickerDialog(
    clients: List<ClientSuggestion>,
    selectedClientName: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSelect: (ClientSuggestion) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    var query by remember { mutableStateOf("") }

    val filtered = remember(clients, query) {
        if (query.isBlank()) {
            clients.sortedBy { it.displayName.lowercase() }
        } else {
            ClientSuggestions.filter(
                clients = clients,
                query = query,
                limit = clients.size.coerceAtLeast(1)
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Locales.t("stats_client_picker_title"),
                fontSize = (19 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(Locales.t("stats_client_filter")) }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 360.dp)
                ) {
                    itemsIndexed(filtered) { index, client ->
                        val isSelected = selectedClientName != null &&
                                selectedClientName.equals(client.displayName, ignoreCase = true)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(client) }
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = client.displayName,
                                fontSize = (14 * fontScale).sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) {
                                    MaterialTheme.colors.primary
                                } else {
                                    MaterialTheme.colors.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (client.phone.isNotBlank()) {
                                Text(
                                    text = client.phone,
                                    fontSize = (12 * fontScale).sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (index != filtered.lastIndex) {
                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Locales.t("close"))
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text(Locales.t("stats_clear_client_filter"))
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
}