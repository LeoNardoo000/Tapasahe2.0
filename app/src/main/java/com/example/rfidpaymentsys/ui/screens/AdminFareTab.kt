package com.example.rfidpaymentsys.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.model.FareRequest
import com.example.rfidpaymentsys.ui.components.formatDateTime
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.viewmodel.MainViewModel

/** Admin tab: set the fare rates and review operator requests. */
@Composable
fun FareRatesTab(viewModel: MainViewModel) {
    val pending = viewModel.fareRequests.filter { it.status == "PENDING" }
    val decided = viewModel.fareRequests.filter { it.status != "PENDING" }.take(10)

    var regularInput by remember(viewModel.regularFare) { mutableStateOf(formatFareInput(viewModel.regularFare)) }
    var studentInput by remember(viewModel.studentFare) { mutableStateOf(formatFareInput(viewModel.studentFare)) }
    var reasonInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Fare Rates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Current fares", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(formatPeso(viewModel.regularFare), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Regular", fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatPeso(viewModel.studentFare), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Students / PWD / Senior", fontSize = 13.sp)
                    }
                }
                if (viewModel.fareUpdatedAt > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Last changed " + formatDateTime(viewModel.fareUpdatedAt) +
                            (if (viewModel.fareUpdatedBy.isNotBlank()) " by ${viewModel.fareUpdatedBy}" else ""),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Update fare rates", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                FareField("Regular fare", regularInput) { regularInput = it; message = null }
                FareField("Students / PWD / Senior fare", studentInput) { studentInput = it; message = null }
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { if (it.length <= 200) { reasonInput = it; message = null } },
                    label = { Text("Reason for the change") },
                    supportingText = { Text("Clients see this when they log in. Required if the fares change.") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                message?.let {
                    Text(
                        it,
                        fontSize = 13.sp,
                        color = if (messageIsError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }
                Button(
                    onClick = {
                        val error = viewModel.updateFareRates(
                            regularInput.toDoubleOrNull() ?: 0.0,
                            studentInput.toDoubleOrNull() ?: 0.0,
                            reasonInput
                        )
                        if (error == null) {
                            message = "Fare rates updated. Clients will see the reason when they log in."
                            messageIsError = false
                            reasonInput = ""
                        } else {
                            message = error
                            messageIsError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("SAVE FARE RATES", fontWeight = FontWeight.Bold) }
            }
        }

        Text(
            "Operator requests" + if (pending.isNotEmpty()) " (${pending.size})" else "",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (pending.isEmpty()) {
            Text("No pending requests.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        } else {
            pending.forEach { FareRequestReviewCard(viewModel, it) }
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
                            Text(request.operatorName.ifBlank { request.operatorId }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                request.status,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (request.status == "APPROVED") Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Text(
                            "Regular " + formatPeso(request.proposedRegular) + ", Students " + formatPeso(request.proposedStudent),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FareRequestReviewCard(viewModel: MainViewModel, request: FareRequest) {
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.operatorName.ifBlank { "Operator" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("ID: ${request.operatorId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Regular: " + formatPeso(request.currentRegular) + "  ->  " + formatPeso(request.proposedRegular), fontSize = 14.sp)
            Text("Students: " + formatPeso(request.currentStudent) + "  ->  " + formatPeso(request.proposedStudent), fontSize = 14.sp)
            if (request.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Reason: ${request.reason}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatDateTime(request.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.rejectFareRequest(request) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("REJECT", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { error = viewModel.approveFareRequest(request) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("APPROVE", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
