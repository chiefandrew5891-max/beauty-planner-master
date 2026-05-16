package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.ContactSuggestion
import com.andrey.beautyplanner.ContactsAutocomplete
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ServicesCatalog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BookingDialog(
    time: String,
    initialData: Appointment?,
    readOnly: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, String, String) -> Unit,
    onTransferRequest: (Appointment) -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    fun parseHmToMinutes(hm: String): Int? {
        val p = hm.split(":")
        if (p.size != 2) return null
        val h = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun minutesToHm(total: Int): String {
        val t = total.coerceIn(0, 24 * 60 - 1)
        val h = t / 60
        val m = t % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    var editEnabled by remember(readOnly, initialData) { mutableStateOf(!readOnly) }
    var showEnableEditConfirm by remember { mutableStateOf(false) }

    val initKey = remember(time, initialData?.id) { initialData?.id ?: "new:$time" }

    var name by remember(initKey) { mutableStateOf(initialData?.clientName ?: "") }
    var phone by remember(initKey) { mutableStateOf(initialData?.phone ?: "") }
    var serviceKey by remember(initKey) { mutableStateOf(initialData?.serviceName ?: "") }
    var price by remember(initKey) { mutableStateOf(initialData?.price ?: "") }

    val otherKey = "service_other"
    var serviceIsOther by remember(initKey) {
        mutableStateOf(serviceKey.isNotBlank() && !serviceKey.startsWith("service_"))
    }
    var customServiceText by remember(initKey) {
        mutableStateOf(if (serviceIsOther) serviceKey else "")
    }

    val initialStart = remember(initKey) { initialData?.time ?: time }
    val startBaseHour = remember(initKey) { initialStart.substringBefore(":").toIntOrNull() ?: 0 }
    val initialStartMinRaw = remember(initKey) { initialStart.substringAfter(":", "00").toIntOrNull() ?: 0 }
    val minuteOptions = remember { listOf(0, 10, 20, 30, 40, 50) }

    var startMinutesPart by remember(initKey) {
        mutableStateOf(minuteOptions.lastOrNull { it <= initialStartMinRaw } ?: 0)
    }

    val startTime = remember(startBaseHour, startMinutesPart) {
        "${startBaseHour.toString().padStart(2, '0')}:${startMinutesPart.toString().padStart(2, '0')}"
    }
    val startAbsMinutes = remember(startTime) { parseHmToMinutes(startTime) ?: (startBaseHour * 60) }

    val endOptions = remember(startAbsMinutes) {
        val list = mutableListOf<String>()
        val minEnd = startAbsMinutes + 10
        val maxEnd = startAbsMinutes + 4 * 60
        var t = minEnd
        while (t <= maxEnd) {
            list.add(minutesToHm(t))
            t += 10
        }
        list
    }

    fun defaultEnd(): String {
        val x = startAbsMinutes + 60
        val rounded = (x / 10) * 10
        return minutesToHm(rounded)
    }

    val existingDurationMins = remember(initKey) {
        val dm = initialData?.durationMinutes ?: 0
        if (dm > 0) dm else ((initialData?.durationHours ?: 1) * 60)
    }

    var endTime by remember(initKey, startAbsMinutes, existingDurationMins) {
        val proposed = minutesToHm(startAbsMinutes + existingDurationMins)
        mutableStateOf(if (endOptions.contains(proposed)) proposed else defaultEnd())
    }

    var triedSave by remember { mutableStateOf(false) }

    val nameOk = name.trim().isNotBlank()
    val phoneOk = phone.trim().isNotBlank()

    val serviceOk =
        if (serviceIsOther) customServiceText.trim().isNotBlank()
        else serviceKey.trim().isNotBlank()

    val priceOk = price.trim().isNotBlank()

    val endAbs = parseHmToMinutes(endTime)
    val endOk = endAbs != null && endAbs > startAbsMinutes

    val formOk = nameOk && phoneOk && serviceOk && priceOk && endOk

    var contactSuggestions by remember { mutableStateOf<List<ContactSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }

    val contactsPermissionGranted by remember {
        derivedStateOf { ContactsAutocomplete.isPermissionGranted() }
    }

    LaunchedEffect(name, editEnabled, contactsPermissionGranted) {
        if (!editEnabled || !contactsPermissionGranted) {
            contactSuggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }

        val query = name.trim()
        if (query.length < 2) {
            contactSuggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }

        delay(180)

        val found = ContactsAutocomplete.findSuggestions(query, limit = 8)
        contactSuggestions = found
        showSuggestions = found.isNotEmpty()
    }

    if (showEnableEditConfirm) {
        AlertDialog(
            onDismissRequest = { showEnableEditConfirm = false },
            title = { Text(Locales.t("edit_appointment_title")) },
            text = { Text(Locales.t("edit_appointment_confirm")) },
            confirmButton = {
                Button(onClick = {
                    showEnableEditConfirm = false
                    editEnabled = true
                }) { Text(Locales.t("yes")) }
            },
            dismissButton = {
                TextButton(onClick = { showEnableEditConfirm = false }) { Text(Locales.t("cancel")) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            elevation = 12.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${Locales.t("start_time")}: $startTime • ${Locales.t("end_time")}: $endTime",
                        fontSize = (14 * fontScale).sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Locales.t("start_time"), fontSize = (12 * fontScale).sp, color = Color.Gray)

                        var expStart by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expStart,
                            onExpandedChange = { if (editEnabled) expStart = !expStart }
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = {},
                                readOnly = true,
                                enabled = editEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expStart) },
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(expanded = expStart, onDismissRequest = { expStart = false }) {
                                minuteOptions.forEach { m ->
                                    DropdownMenuItem(onClick = {
                                        startMinutesPart = m
                                        expStart = false
                                        endTime = defaultEnd()
                                    }) {
                                        Text("${startBaseHour.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}")
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(Locales.t("end_time"), fontSize = (12 * fontScale).sp, color = Color.Gray)

                        var expEnd by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expEnd,
                            onExpandedChange = { if (editEnabled) expEnd = !expEnd }
                        ) {
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = {},
                                readOnly = true,
                                enabled = editEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expEnd) },
                                shape = RoundedCornerShape(14.dp),
                                isError = triedSave && editEnabled && !endOk
                            )
                            ExposedDropdownMenu(expanded = expEnd, onDismissRequest = { expEnd = false }) {
                                endOptions.forEach { t ->
                                    DropdownMenuItem(onClick = {
                                        endTime = t
                                        expEnd = false
                                    }) { Text(t) }
                                }
                            }
                        }
                    }
                }

                if (triedSave && editEnabled && !endOk) {
                    Text(
                        text = when (Locales.currentLanguage) {
                            "ru" -> "Конец должен быть позже начала"
                            "uk" -> "Кінець має бути пізніше початку"
                            "it" -> "La fine deve essere dopo l'inizio"
                            else -> "End must be after start"
                        },
                        color = MaterialTheme.colors.error,
                        fontSize = (12 * fontScale).sp,
                        modifier = Modifier.align(Alignment.Start).padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showSuggestions = true
                    },
                    enabled = editEnabled,
                    label = { Text(Locales.t("client_name")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    isError = triedSave && editEnabled && !nameOk,
                    trailingIcon = {
                        if (editEnabled && !contactsPermissionGranted) {
                            IconButton(onClick = { ContactsAutocomplete.requestPermission() }) {
                                Icon(Icons.Default.Contacts, contentDescription = null)
                            }
                        }
                    }
                )

                if (editEnabled && !contactsPermissionGranted) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Locales.t("contacts_permission_hint"),
                        fontSize = (11 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (editEnabled && contactsPermissionGranted && showSuggestions && contactSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            contactSuggestions.forEachIndexed { index, suggestion ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = suggestion.displayName
                                            phone = suggestion.phone
                                            showSuggestions = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = suggestion.displayName,
                                        fontSize = (14 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = suggestion.phone,
                                        fontSize = (12 * fontScale).sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (index != contactSuggestions.lastIndex) {
                                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    enabled = editEnabled,
                    label = { Text(Locales.t("phone")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    isError = triedSave && editEnabled && !phoneOk
                )

                Spacer(Modifier.height(12.dp))

                val services = remember {
                    ServicesCatalog.keys + otherKey
                }

                if (!serviceIsOther) {
                    var serviceExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = serviceExpanded,
                        onExpandedChange = { if (editEnabled) serviceExpanded = !serviceExpanded }
                    ) {
                        val displayText = if (serviceKey.isBlank()) "" else Locales.t(serviceKey)

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = editEnabled,
                            label = { Text(Locales.t("service")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Brush, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                            isError = triedSave && editEnabled && !serviceOk
                        )
                        ExposedDropdownMenu(expanded = serviceExpanded, onDismissRequest = { serviceExpanded = false }) {
                            services.forEach { key ->
                                DropdownMenuItem(onClick = {
                                    serviceExpanded = false
                                    if (key == otherKey) {
                                        serviceIsOther = true
                                        customServiceText = ""
                                        serviceKey = ""
                                    } else {
                                        serviceIsOther = false
                                        serviceKey = key
                                    }
                                }) { Text(Locales.t(key)) }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customServiceText,
                        onValueChange = {
                            customServiceText = it
                            if (it.isBlank()) {
                                serviceIsOther = false
                                serviceKey = ""
                            }
                        },
                        enabled = editEnabled,
                        label = { Text(Locales.t("service")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = { Icon(Icons.Default.Brush, null) },
                        trailingIcon = {
                            IconButton(
                                enabled = editEnabled,
                                onClick = {
                                    serviceIsOther = false
                                    customServiceText = ""
                                    serviceKey = ""
                                }
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Back to list")
                            }
                        },
                        isError = triedSave && editEnabled && !serviceOk
                    )

                    Text(
                        text = when (Locales.currentLanguage) {
                            "ru" -> "Режим “Другое”: введите название процедуры вручную"
                            "uk" -> "Режим “Інше”: введіть назву процедури вручну"
                            "it" -> "Modalità “Altro”: inserisci manualmente il nome"
                            else -> "Other mode: type service name"
                        },
                        fontSize = (12 * fontScale).sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    enabled = editEnabled,
                    label = { Text(Locales.t("price") + " (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Payments, null) },
                    isError = triedSave && editEnabled && !priceOk
                )

                Spacer(Modifier.height(16.dp))

                if (editEnabled) {
                    if (triedSave && !formOk) {
                        Text(
                            text = when (Locales.currentLanguage) {
                                "ru" -> "Заполните обязательные поля"
                                "uk" -> "Заповніть обовʼязкові поля"
                                "it" -> "Compila i campi obbligatori"
                                else -> "Fill required fields"
                            },
                            color = MaterialTheme.colors.error,
                            fontSize = (13 * fontScale).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            triedSave = true
                            if (!formOk) return@Button

                            val durationMinutes = ((parseHmToMinutes(endTime) ?: 0) - startAbsMinutes)
                                .coerceAtLeast(10)

                            val serviceToStore =
                                if (serviceIsOther) customServiceText.trim()
                                else serviceKey.trim()

                            onSave(
                                startTime,
                                durationMinutes,
                                name.trim(),
                                phone.trim(),
                                serviceToStore,
                                price.trim()
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                    ) {
                        Text(Locales.t("save").uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showEnableEditConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                    ) {
                        Text(Locales.t("edit").uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (initialData != null) {
                    TextButton(
                        onClick = { onTransferRequest(initialData) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(Locales.t("transfer_appt"), color = MaterialTheme.colors.primary)
                    }
                }
            }
        }
    }
}