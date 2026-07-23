package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.ClientSuggestion
import com.andrey.beautyplanner.ClientSuggestions
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.appcontent.appFontFamily

@Composable
fun ClientPickerDialog(
    clients: List<ClientSuggestion>,
    selectedClientName: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSelect: (ClientSuggestion) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
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
                color = onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            text = Locales.t("stats_client_filter"),
                            fontSize = (13 * fontScale).sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(
                        fontFamily = appFontFamily(),
                        fontSize = (15 * fontScale).sp,
                        color = onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = onSurface,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.60f),
                        cursorColor = MaterialTheme.colors.primary,
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                                    onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (client.phone.isNotBlank()) {
                                Text(
                                    text = client.phone,
                                    fontSize = (12 * fontScale).sp,
                                    color = onSurface.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (index != filtered.lastIndex) {
                            Divider(color = onSurface.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = Locales.t("close"),
                    color = MaterialTheme.colors.primary,
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClear
            ) {
                Text(
                    text = Locales.t("stats_clear_client_filter"),
                    color = onSurface.copy(alpha = 0.70f),
                    fontSize = (14 * fontScale).sp
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}