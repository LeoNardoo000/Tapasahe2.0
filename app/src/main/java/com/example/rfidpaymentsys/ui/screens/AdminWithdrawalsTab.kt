package com.example.rfidpaymentsys.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.entity.paymentMethodLabel
import com.example.rfidpaymentsys.data.model.WithdrawalRequest
import com.example.rfidpaymentsys.ui.components.formatDateTime
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.viewmodel.MainViewModel

/** Admin tab: confirm or reject client withdrawals. */
@Composable
fun WithdrawalsTab(viewModel: MainViewModel) {
    val pending = viewModel.withdrawals.filter { it.status == "PENDING" }.sortedBy { it.createdAt }
    val decided = viewModel.withdrawals.filter { it.status != "PENDING" }.sortedByDescending { it.createdAt }.take(10)

    // Pair(request, true = confirm payout / false = reject)
    var pendingAction by remember { mutableStateOf<Pair<WithdrawalRequest, Boolean>?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Client Withdrawals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Send the money to the client's e-wallet first, then tap Confirm payout. " +
                "Rejecting returns the amount to the client's balance.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (pending.isEmpty()) {
            Text("No withdrawals waiting for review.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        } else {
            pending.forEach { request ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(request.userName.ifBlank { "Client" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("ID: ${request.userId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatPeso(request.amount), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Send to: " + paymentMethodLabel(request.destination) + " " + request.accountNumber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Requested " + formatDateTime(request.createdAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { pendingAction = request to false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("REJECT", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { pendingAction = request to true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("CONFIRM PAYOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        if (decided.isNotEmpty()) {
            Text("Recent decisions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            decided.forEach { request ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(request.userName.ifBlank { request.userId }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (request.status == "APPROVED") "PAID" else "REFUNDED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (request.status == "APPROVED") Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Text(
                            formatPeso(request.amount) + " to " + paymentMethodLabel(request.destination),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    pendingAction?.let { (request, approve) ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(if (approve) "Confirm payout" else "Reject withdrawal") },
            text = {
                Text(
                    if (approve)
                        "Have you sent " + formatPeso(request.amount) + " to " +
                            paymentMethodLabel(request.destination) + " " + request.accountNumber + "?"
                    else
                        "This returns " + formatPeso(request.amount) + " to " +
                            request.userName.ifBlank { "the client" } + "'s balance."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (approve) viewModel.approveWithdrawal(request) else viewModel.rejectWithdrawal(request)
                        pendingAction = null
                    },
                    colors = if (approve) ButtonDefaults.buttonColors()
                    else ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text(if (approve) "YES, CONFIRM" else "REJECT & REFUND") }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("CANCEL") } }
        )
    }
}
