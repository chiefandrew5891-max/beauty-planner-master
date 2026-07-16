package com.andrey.beautyplanner.appcontent

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
import com.andrey.beautyplanner.ClientSuggestion
import com.andrey.beautyplanner.ClientSuggestions
import com.andrey.beautyplanner.ContactSuggestion
import com.andrey.beautyplanner.ContactsAutocomplete
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ServiceTemplate
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle
import com.andrey.beautyplanner.appcontent.appFontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

private fun displayServiceTitle(title: String): String {
    return if (title.startsWith("service_")) {
        Locales.t(title)
    } else {
        title
    }
}
private fun isValidPriceInput(value: String): Boolean {
    if (value.isBlank()) return false
    return value.matches(Regex("""^\d+([.,]\d{0,2})?$"""))
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BookingDialog(
    time: String,
    initialData: Appointment?,
    readOnly: Boolean,
    localClientSuggestions: List<ClientSuggestion>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, String, String, String, String, Boolean) -> Unit,
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
    val isNewAppointment = initialData == null

    var name by remember(initKey) { mutableStateOf(initialData?.clientName ?: "") }
    var phone by remember(initKey) { mutableStateOf(initialData?.phone ?: "") }
    var serviceKey by remember(initKey) { mutableStateOf(initialData?.serviceName ?: "") }
    var price by remember(initKey) { mutableStateOf(initialData?.price ?: "") }
    var notes by remember(initKey) { mutableStateOf(initialData?.notes ?: "") }
    var paymentDeferred by remember(initKey) { mutableStateOf(initialData?.paymentDeferred == true) }

    val otherKey = "service_other"
    var serviceIsOther by remember(initKey) { mutableStateOf(false) }
    var customServiceText by remember(initKey) { mutableStateOf("") }

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
    val startAbsMinutes = remember(startTime) {
        parseHmToMinutes(startTime) ?: (startBaseHour * 60)
    }

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
    val serviceOk = if (serviceIsOther) {
        customServiceText.trim().isNotBlank()
    } else {
        serviceKey.trim().isNotBlank()
    }
    val priceOk = isValidPriceInput(price.trim())

    val endAbs = parseHmToMinutes(endTime)
    val endOk = endAbs != null && endAbs > startAbsMinutes
    val formOk = nameOk && phoneOk && serviceOk && priceOk && endOk

    var mergedSuggestions by remember { mutableStateOf<List<ClientSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }

    val contactsPermissionGranted by remember {
        derivedStateOf { ContactsAutocomplete.isPermissionGranted() }
    }

    LaunchedEffect(name, editEnabled, contactsPermissionGranted, localClientSuggestions) {
        if (!editEnabled) {
            mergedSuggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }

        val query = name.trim()
        if (query.length < 1) {
            mergedSuggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }

        delay(180)

        val localFound = ClientSuggestions.filter(
            clients = localClientSuggestions,
            query = query,
            limit = 8
        )

        val contactsFound = if (contactsPermissionGranted) {
            ContactsAutocomplete.findSuggestions(query, limit = 8)
        } else {
            emptyList()
        }

        val merged = ClientSuggestions.merge(
            local = localFound,
            contacts = contactsFound,
            limit = 8
        )

        mergedSuggestions = merged
        showSuggestions = merged.isNotEmpty()
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
                }) {
                    Text(Locales.t("yes"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableEditConfirm = false }) {
                    Text(Locales.t("cancel"))
                }
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
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (initialData == null) {
                            Locales.t("booking_new_title")
                        } else {
                            Locales.t("view_appointment_title")
                        },
                        fontSize = (22 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp, end = 12.dp)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Locales.t("close"),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${Locales.t("start_time")}: $startTime • ${Locales.t("end_time")}: $endTime",
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                                label = { Text(Locales.t("start_time")) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expStart)
                                },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = appFontFamily(),
                                    color = MaterialTheme.colors.onSurface
                                ),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
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

                            ExposedDropdownMenu(
                                expanded = expStart,
                                onDismissRequest = { expStart = false }
                            ) {
                                minuteOptions.forEach { m ->
                                    DropdownMenuItem(onClick = {
                                        startMinutesPart = m
                                        expStart = false
                                        endTime = defaultEnd()
                                    }) {
                                        Text(
                                            "${startBaseHour.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
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
                                label = { Text(Locales.t("end_time")) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expEnd)
                                },
                                shape = RoundedCornerShape(14.dp),
                                isError = triedSave && editEnabled && !endOk,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = appFontFamily(),
                                    color = MaterialTheme.colors.onSurface
                                ),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
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

                            ExposedDropdownMenu(
                                expanded = expEnd,
                                onDismissRequest = { expEnd = false }
                            ) {
                                endOptions.forEach { t ->
                                    DropdownMenuItem(onClick = {
                                        endTime = t
                                        expEnd = false
                                    }) {
                                        Text(t)
                                    }
                                }
                            }
                        }
                    }
                }

                if (triedSave && editEnabled && !endOk) {
                    Text(
                        text = Locales.t("booking_end_after_start"),
                        color = MaterialTheme.colors.error,
                        fontSize = (12 * fontScale).sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        name = newValue
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
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = appFontFamily(),
                        color = MaterialTheme.colors.onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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

                if (editEnabled && !contactsPermissionGranted) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Locales.t("contacts_permission_hint"),
                        fontSize = (11 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (editEnabled && showSuggestions && mergedSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            mergedSuggestions.forEachIndexed { index, suggestion ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = suggestion.displayName
                                            if (suggestion.phone.isNotBlank()) {
                                                phone = suggestion.phone
                                            }
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

                                    if (suggestion.phone.isNotBlank()) {
                                        Text(
                                            text = suggestion.phone,
                                            fontSize = (12 * fontScale).sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (index != mergedSuggestions.lastIndex) {
                                    Divider(
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue -> phone = newValue },
                    enabled = editEnabled,
                    label = { Text(Locales.t("phone")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    isError = triedSave && editEnabled && !phoneOk,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = appFontFamily(),
                        color = MaterialTheme.colors.onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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

                Spacer(Modifier.height(12.dp))

                val services = remember(AppSettings.serviceTemplates) {
                    AppSettings.getActiveServiceTemplates() + ServiceTemplate(
                        id = otherKey,
                        title = otherKey,
                        defaultPrice = "",
                        isActive = true
                    )
                }

                LaunchedEffect(initKey) {
                    val existing = AppSettings.getActiveServiceTemplates().any { it.title == serviceKey }
                    val builtIn = serviceKey.startsWith("service_")
                    val empty = serviceKey.isBlank()

                    if (!empty && !builtIn && !existing) {
                        serviceIsOther = true
                        customServiceText = serviceKey
                    }
                }

                if (!serviceIsOther) {
                    var serviceExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = serviceExpanded,
                        onExpandedChange = { if (editEnabled) serviceExpanded = !serviceExpanded }
                    ) {
                        val displayText = if (serviceKey.isBlank()) "" else displayServiceTitle(serviceKey)

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = editEnabled,
                            label = { Text(Locales.t("service")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Brush, null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded)
                            },
                            isError = triedSave && editEnabled && !serviceOk,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = appFontFamily(),
                                color = MaterialTheme.colors.onSurface
                            ),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
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

                        ExposedDropdownMenu(
                            expanded = serviceExpanded,
                            onDismissRequest = { serviceExpanded = false }
                        ) {
                            services.forEach { item ->
                                DropdownMenuItem(
                                    onClick = {
                                        serviceExpanded = false

                                        if (item.id == otherKey) {
                                            serviceIsOther = true
                                            customServiceText = ""
                                            serviceKey = ""
                                        } else {
                                            serviceIsOther = false
                                            serviceKey = item.title

                                            val shouldAutofillPrice =
                                                item.defaultPrice.isNotBlank() &&
                                                        (price.isBlank() || isNewAppointment)

                                            if (shouldAutofillPrice) {
                                                price = item.defaultPrice
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (item.id == otherKey) {
                                            Locales.t(otherKey)
                                        } else {
                                            displayServiceTitle(item.title)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customServiceText,
                        onValueChange = { newValue ->
                            customServiceText = newValue
                            if (newValue.isBlank()) {
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
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Back to list"
                                )
                            }
                        },
                        isError = triedSave && editEnabled && !serviceOk,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            color = MaterialTheme.colors.onSurface
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
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

                    Text(
                        text = Locales.t("booking_other_mode_hint"),
                        fontSize = (12 * fontScale).sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                        val separatorsCount = filtered.count { it == '.' || it == ',' }

                        price = when {
                            filtered.isEmpty() -> ""
                            separatorsCount <= 1 -> filtered
                            else -> price
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = editEnabled,
                    label = { Text(Locales.t("price") + " (${AppSettings.currencySymbol()})") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Payments, null) },
                    isError = triedSave && editEnabled && !priceOk,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = appFontFamily(),
                        color = MaterialTheme.colors.onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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
                if (triedSave && editEnabled && !priceOk) {
                    Text(
                        text = "Enter numbers only",
                        color = MaterialTheme.colors.error,
                        fontSize = (12 * fontScale).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { newValue -> notes = newValue },
                    enabled = editEnabled,
                    label = { Text(Locales.t("booking_comment")) },
                    placeholder = {
                        Text(Locales.t("booking_comment_hint"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    textStyle = TextStyle(
                        fontFamily = appFontFamily(),
                        color = MaterialTheme.colors.onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
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

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Locales.t("booking_payment_deferred"),
                        fontSize = (15 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface
                    )

                    AppSwitch(
                        checked = paymentDeferred,
                        onCheckedChange = { paymentDeferred = it },
                        enabled = editEnabled
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (editEnabled) {
                    if (triedSave && !formOk) {
                        Text(
                            text = Locales.t("booking_fill_required_fields"),
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

                            val serviceToStore = if (serviceIsOther) {
                                customServiceText.trim()
                            } else {
                                serviceKey.trim()
                            }

                            onSave(
                                startTime,
                                durationMinutes,
                                name.trim(),
                                phone.trim(),
                                serviceToStore,
                                price.trim(),
                                initialData?.currency ?: AppSettings.selectedCurrency,
                                notes.trim(),
                                paymentDeferred
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            text = Locales.t("save").uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { showEnableEditConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            text = Locales.t("edit").uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (initialData != null) {
                    TextButton(
                        onClick = { onTransferRequest(initialData) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = Locales.t("transfer_appt"),
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}