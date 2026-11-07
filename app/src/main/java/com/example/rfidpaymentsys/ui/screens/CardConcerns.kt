package com.example.rfidpaymentsys.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.model.FareNotice
import com.example.rfidpaymentsys.ui.components.formatDateTime
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.viewmodel.MainViewModel

private const val NOTICE_CARD_DAYS = 14L

/** Shown once after login when the fares changed since the client last looked. */
@Composable
fun FareNoticeDialog(notice: FareNotice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Fare change notice", fontWeight = FontWeight.Bold) },
        text = { FareNoticeBody(notice) },
        confirmButton = { Button(onClick = onDismiss) { Text("GOT IT") } }
    )
}

/** Stays on the dashboard for two weeks after a fare change so clients can read it again. */
@Composable
fun FareNoticeCard(notice: FareNotice?, registeredAt: Long) {
    if (notice == null || notice.changedAt <= registeredAt) return
    val ageMs = System.currentTimeMillis() - notice.changedAt
    if (ageMs > NOTICE_CARD_DAYS * 24L * 60 * 60 * 1000) return
    Spacer(modifier = Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Fare change notice", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            FareNoticeBody(notice)
        }
    }
}

@Composable
private fun FareNoticeBody(notice: FareNotice) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Regular: " + formatPeso(notice.previousRegular) + "  ->  " + formatPeso(notice.regular),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Students / PWD / Senior: " + formatPeso(notice.previousStudent) + "  ->  " + formatPeso(notice.student),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (notice.reason.isNotBlank()) {
            Text("Reason: ${notice.reason}", fontSize = 14.sp)
        }
        Text(
            "Effective " + formatDateTime(notice.changedAt),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun maskCardId(id: String): String = if (id.length <= 4) id else "••••" + id.takeLast(4)

/** Client: disable a lost card, enable a found one, or ask for a new card. */
@Composable
fun CardConcernsDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val card = viewModel.currentCard
    val isActive = card?.status == "ACTIVE"
    val isDisabled = card?.status == "DISABLED"

    var confirmDisable by remember { mutableStateOf(false) }
    var showEnable by remember { mutableStateOf(false) }
    var enablePassword by remember { mutableStateOf("") }
    var enableError by remember { mutableStateOf<String?>(null) }
    var cardError by remember { mutableStateOf<String?>(null) }

    var reason by remember { mutableStateOf(if (card == null) "NO_CARD" else "LOST") }
    var note by remember { mutableStateOf("") }
    var requestError by remember { mutableStateOf<String?>(null) }
    var requestSent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card concerns", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Current card ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (card == null) {
                            Text("No card linked yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Request a card below and the admin will register it to your account.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Card ${maskCardId(card.cardID)}", fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    isActive -> "Active - can be used to pay"
                                    isDisabled -> "Disabled - payments are declined"
                                    else -> "Blocked by the admin"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isActive -> Color(0xFF2E7D32)
                                    isDisabled -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                }
                            )
                        }
                    }
                }

                if (card != null && (isActive || isDisabled)) {
                    if (isActive) {
                        OutlinedButton(
                            onClick = { cardError = null; confirmDisable = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("DISABLE CARD (LOST)", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
                    } else {
                        Button(
                            onClick = { enablePassword = ""; enableError = null; showEnable = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("ENABLE CARD (FOUND)", fontWeight = FontWeight.Bold) }
                    }
                }
                cardError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                if (card != null && !isActive && !isDisabled) {
                    Text(
                        "Please send a concern from your Inbox so the admin can help.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // --- Request a new card ---
                Text("Request a new card", fontWeight = FontWeight.Bold)
                if (requestSent) {
                    Text(
                        "Request sent. The admin will reply in your Inbox." +
                            if (reason == "LOST" && card != null) " Your old card was disabled." else "",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    val options = buildList {
                        if (card == null) add("NO_CARD" to "No card yet")
                        add("LOST" to "Lost")
                        add("DAMAGED" to "Damaged")
                        add("OTHER" to "Other")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { (value, label) ->
                            FilterChip(
                                selected = reason == value,
                                onClick = { reason = value; requestError = null },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    if (reason == "LOST" && card != null && isActive) {
                        Text(
                            "Your current card will be disabled right away.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 250) { note = it; requestError = null } },
                        label = { Text("Note (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    requestError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            val error = viewModel.requestNewCard(reason, note)
                            if (error == null) requestSent = true else requestError = error
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("SEND REQUEST", fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )

    if (confirmDisable) {
        AlertDialog(
            onDismissRequest = { confirmDisable = false },
            title = { Text("Disable this card?", fontWeight = FontWeight.Bold) },
            text = { Text("Nobody will be able to pay with it. You can enable it again with your password if you find it.") },
            confirmButton = {
                Button(onClick = {
                    cardError = viewModel.setMyCardEnabled(false)
                    confirmDisable = false
                }) { Text("DISABLE") }
            },
            dismissButton = { TextButton(onClick = { confirmDisable = false }) { Text("CANCEL") } }
        )
    }

    if (showEnable) {
        AlertDialog(
            onDismissRequest = { showEnable = false },
            title = { Text("Enable this card?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your password to turn the card back on.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enablePassword,
                        onValueChange = { enablePassword = it; enableError = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = enableError != null,
                        supportingText = enableError?.let { { Text(it) } },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val error = viewModel.setMyCardEnabled(true, enablePassword)
                    if (error == null) { showEnable = false; cardError = null } else enableError = error
                }) { Text("ENABLE") }
            },
            dismissButton = { TextButton(onClick = { showEnable = false }) { Text("CANCEL") } }
        )
    }
}
