package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CloudSyncLogger
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ServiceTemplate
import com.andrey.beautyplanner.remote.MasterProfileSync
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


@Composable
fun ServiceTemplatesScreen() {
    val fontScale = AppSettings.getFontScale()
    val scope = rememberCoroutineScope()

    var editingItem by remember { mutableStateOf<ServiceTemplate?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<ServiceTemplate?>(null) }
    val services = AppSettings.serviceTemplates.filter { it.isActive }

    fun syncServiceTemplates() {
        scope.launch {
            MasterProfileSync.syncIfAuthenticated()
                .onFailure {
                    CloudSyncLogger.log("syncServiceTemplates: failed: ${it.message}")
                }
        }
    }

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.t("my_services"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            Text(
                text = Locales.t("my_services_hint"),
                fontSize = (14 * fontScale).sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )

            PrimaryActionButton(
                text = Locales.t("service_add"),
                onClick = { showCreateDialog = true }
            )

            if (services.isEmpty()) {
                Text(
                    text = Locales.t("service_empty_list"),
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(services, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = 2.dp,
                            backgroundColor = MaterialTheme.colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayServiceTitle(item),
                                        color = MaterialTheme.colors.onSurface,
                                        fontSize = (16 * fontScale).sp
                                    )
                                    if (item.defaultPrice.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${Locales.t("service_default_price")}: ${item.defaultPrice} ${AppSettings.currencySymbol()}",
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                                            fontSize = (13 * fontScale).sp
                                        )
                                    }
                                }

                                IconButton(onClick = { editingItem = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = Locales.t("service_edit")
                                    )
                                }

                                IconButton(onClick = { deletingItem = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Locales.t("delete_btn")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ServiceTemplateEditorDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, price ->
                val newItem = ServiceTemplate(
                    id = "custom_" + Clock.System.now().toEpochMilliseconds(),
                    title = title,
                    defaultPrice = price,
                    isActive = true
                )
                AppSettings.upsertServiceTemplate(newItem)
                syncServiceTemplates()
                showCreateDialog = false
            }
        )
    }

    editingItem?.let { item ->
        ServiceTemplateEditorDialog(
            initial = item,
            onDismiss = { editingItem = null },
            onSave = { title, price ->
                AppSettings.upsertServiceTemplate(
                    item.copy(
                        title = title,
                        defaultPrice = price
                    )
                )
                syncServiceTemplates()
                editingItem = null
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(Locales.t("delete_title")) },
            text = { Text(Locales.t("service_delete_confirm")) },
            confirmButton = {
                Button(onClick = {
                    AppSettings.removeServiceTemplate(item.id)
                    syncServiceTemplates()
                    deletingItem = null
                }) {
                    Text(Locales.t("delete_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ServiceTemplateEditorDialog(
    initial: ServiceTemplate?,
    onDismiss: () -> Unit,
    onSave: (title: String, price: String) -> Unit
) {
    var title by remember(initial) { mutableStateOf(resolveEditableTitle(initial)) }
    var price by remember(initial) { mutableStateOf(initial?.defaultPrice ?: "") }
    var triedSave by remember { mutableStateOf(false) }
    val titleValid = title.trim().isNotBlank()
    val fontScale = AppSettings.getFontScale()

    AppDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (initial == null) {
                            Locales.t("service_add")
                        } else {
                            Locales.t("service_edit")
                        },
                        color = MaterialTheme.colors.onSurface,
                        fontSize = (18 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(Locales.t("service_name")) },
                        singleLine = true,
                        isError = triedSave && !titleValid,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            color = MaterialTheme.colors.onSurface
                        ),
                        colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                            cursorColor = MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.surface,
                            placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f),
                            errorBorderColor = MaterialTheme.colors.error,
                            errorCursorColor = MaterialTheme.colors.error
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                            val firstSeparatorIndex = filtered.indexOfFirst { it == '.' || it == ',' }

                            price = if (firstSeparatorIndex == -1) {
                                filtered
                            } else {
                                buildString {
                                    filtered.forEachIndexed { index, ch ->
                                        if (ch.isDigit() || index == firstSeparatorIndex) {
                                            append(ch)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(Locales.t("service_default_price")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            color = MaterialTheme.colors.onSurface
                        ),
                        colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                            cursorColor = MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.surface,
                            placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
                        )
                    )

                    if (triedSave && !titleValid) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Locales.t("booking_fill_required_fields"),
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        triedSave = true
                        if (!titleValid) return@Button
                        onSave(title.trim(), price.trim())
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    ),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                ) {
                    Text(Locales.t("save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                    )
                ) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = AppDialogShape
        )
    }
}

private fun displayServiceTitle(item: ServiceTemplate): String {
    return if (item.title.startsWith("service_")) {
        Locales.t(item.title)
    } else {
        item.title
    }
}

private fun resolveEditableTitle(item: ServiceTemplate?): String {
    if (item == null) return ""
    return if (item.title.startsWith("service_")) {
        Locales.t(item.title)
    } else {
        item.title
    }
}