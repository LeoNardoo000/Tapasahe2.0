package com.example.rfidpaymentsys.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.entity.paymentMethodLabel
import com.example.rfidpaymentsys.data.model.WithdrawalRequest
import com.example.rfidpaymentsys.ui.components.DetailRow
import com.example.rfidpaymentsys.ui.components.OptionCard
import com.example.rfidpaymentsys.ui.components.PasswordField
import com.example.rfidpaymentsys.ui.components.formatDateTime
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.ui.components.maskMobile
import com.example.rfidpaymentsys.viewmodel.MainViewModel

/** Client form: ask the admin to pay out part of the balance. */
@Composable
fun WithdrawDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val user = viewModel.currentUser
    val balance = user?.balance ?: 0.0

    var destination by remember { mutableStateOf("GCASH") }
    var account by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val busy = viewModel.withdrawBusy
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val accountValid = Regex("^09\\d{9}$").matches(account)
    val amountValid = amount >= viewModel.minWithdraw && amount <= balance
    val canSubmit = accountValid && amountValid && password.isNotEmpty() && !busy

    // When the request goes through, the "thank you" notice takes over from this form.
    LaunchedEffect(viewModel.withdrawalNotice) {
        if (viewModel.withdrawalNotice != null) onDismiss()
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Withdraw balance", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Available: " + formatPeso(balance), fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        selected = destination == "GCASH",
                        onClick = { destination = "GCASH" },
                        modifier = Modifier.weight(1f),
                        enabled = !busy
                    ) { Text("GCash", fontWeight = FontWeight.Bold) }
                    OptionCard(
                        selected = destination == "MAYA",
                        onClick = { destination = "MAYA" },
                        modifier = Modifier.weight(1f),
                        enabled = !busy
                    ) { Text("Maya", fontWeight = FontWeight.Bold) }
                }

                OutlinedTextField(
                    value = account,
                    onValueChange = { input -> if (input.all { it.isDigit() } && input.length <= 11) account = input },
                    label = { Text("${paymentMethodLabel(destination)} mobile number") },
                    singleLine = true,
                    enabled = !busy,
                    isError = account.isNotEmpty() && !accountValid,
                    supportingText = { Text("11 digits, starts with 09") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> if (input.all { it.isDigit() } && input.length <= 6) amountText = input },
                    label = { Text("Amount") },
                    prefix = { Text("₱") },
                    singleLine = true,
                    enabled = !busy,
                    isError = amountText.isNotEmpty() && !amountValid,
                    supportingText = {
                        Text(
                            if (amountText.isNotEmpty() && amount > balance) "Higher than your balance"
                            else "Minimum " + formatPeso(viewModel.minWithdraw)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (balance >= viewModel.minWithdraw) {
                    FilterChip(
                        selected = amountText == balance.toLong().toString(),
                        onClick = { amountText = balance.toLong().toString() },
                        enabled = !busy,
                        label = { Text("Withdraw all") }
                    )
                }

                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Confirm with your password",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                )

                viewModel.withdrawError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Text(
                    "The admin reviews every withdrawal. Your amount is set aside right away and returned if the request is rejected.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.requestWithdrawal(amount, destination, account, password) },
                enabled = canSubmit
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("SUBMIT REQUEST")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("CANCEL") } }
    )
}

/** The "thank you" message shown right after a withdrawal request is sent. */
@Composable
fun WithdrawalSubmittedDialog(request: WithdrawalRequest, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        icon = {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
        },
        title = { Text("Request received", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Thank you! Please wait for your transaction to be reviewed within an hour.")
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Amount", formatPeso(request.amount))
                DetailRow("Send to", paymentMethodLabel(request.destination) + " " + maskMobile(request.accountNumber))
                DetailRow("Requested", formatDateTime(request.createdAt))
            }
        },
        confirmButton = { Button(onClick = onDone) { Text("OK") } }
    )
}

/** List of the client's own withdrawal requests with their status. */
@Composable
fun WithdrawalsSection(requests: List<WithdrawalRequest>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            requests.forEachIndexed { index, request ->
                val (label, color) = when (request.status) {
                    "APPROVED" -> "Paid" to Color(0xFF2E7D32)
                    "REJECTED" -> "Refunded" to Color(0xFFC62828)
                    else -> "Pending review" to Color(0xFFE65100)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Withdrawal to " + paymentMethodLabel(request.destination),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            formatDateTime(request.createdAt),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatPeso(request.amount), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
                    }
                }
                if (index < requests.lastIndex) HorizontalDivider()
            }
        }
    }
}
