package com.example.rfidpaymentsys.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import java.util.Locale

/** 15.0 -> "15", 12.5 -> "12.50" (for text fields) */
fun formatFareInput(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

/** Up to 3 digits, optionally followed by a dot and up to 2 decimals. */
fun isValidFareInput(input: String): Boolean = Regex("^\\d{0,3}(\\.\\d{0,2})?$").matches(input)

@Composable
fun FareField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (isValidFareInput(it)) onValueChange(it) },
        label = { Text(label) },
        prefix = { Text("₱") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

/** Operator terminal card: shows the current fare rates and lets the operator ask the admin for a change. */
@Composable
fun OperatorFareCard(viewModel: MainViewModel) {
    var showRequestDialog by remember { mutableStateOf(false) }
    val operator = viewModel.currentUser
    val latest = viewModel.fareRequests
        .filter { it.operatorId == operator?.userID }
        .maxByOrNull { it.createdAt }
    val hasPending = latest?.status == "PENDING"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Fare rate", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarePill(formatPeso(viewModel.studentFare) + " - Students", Modifier.weight(1f))
                FarePill(formatPeso(viewModel.regularFare) + " - Regular", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "PWD and Senior Citizens pay the Students rate",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (latest != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val (text, color) = when (latest.status) {
                    "PENDING" -> "Fare change request sent. Waiting for admin approval." to Color(0xFFE65100)
                    "APPROVED" -> "Your last fare change request was approved." to Color(0xFF2E7D32)
                    else -> "Your last fare change request was rejected." to Color(0xFFC62828)
                }
                Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showRequestDialog = true },
                enabled = !hasPending,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (hasPending) "REQUEST PENDING" else "REQUEST FARE CHANGE", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showRequestDialog) {
        FareRequestDialog(viewModel) { showRequestDialog = false }
    }
}

@Composable
private fun FarePill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun FareRequestDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var regular by remember { mutableStateOf(formatFareInput(viewModel.regularFare)) }
    var student by remember { mutableStateOf(formatFareInput(viewModel.studentFare)) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request fare change", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Current: " + formatPeso(viewModel.regularFare) + " Regular, " +
                        formatPeso(viewModel.studentFare) + " Students",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FareField("New Regular fare", regular) { regular = it; error = null }
                Spacer(modifier = Modifier.height(8.dp))
                FareField("New Students fare", student) { student = it; error = null }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 200) reason = it },
                    label = { Text("Reason (optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val result = viewModel.submitFareRequest(
                    regular.toDoubleOrNull() ?: 0.0,
                    student.toDoubleOrNull() ?: 0.0,
                    reason
                )
                if (result == null) onDismiss() else error = result
            }) { Text("SEND TO ADMIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
