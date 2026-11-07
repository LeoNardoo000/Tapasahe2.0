package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.entity.Transaction
import com.example.rfidpaymentsys.data.entity.paymentMethodLabel
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.components.DetailRow
import com.example.rfidpaymentsys.ui.components.GradientBalanceCard
import com.example.rfidpaymentsys.ui.components.OptionCard
import com.example.rfidpaymentsys.ui.components.TransactionRow
import com.example.rfidpaymentsys.ui.components.formatDateTime
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorCashOutScreen(viewModel: MainViewModel) {
    val operator = viewModel.currentUser
    val balance = operator?.balance ?: 0.0

    val history by if (operator != null) {
        viewModel.getRecentTransactions(operator.userID).collectAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList<Transaction>()) }
    }
    val cashOuts = history.filter { it.method.startsWith("CASH_OUT") }

    var destination by remember { mutableStateOf("GCASH") }
    var account by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val processing = viewModel.cashOutProcessing
    val receipt = viewModel.cashOutReceipt

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val accountValid = Regex("^09\\d{9}$").matches(account)
    val amountValid = amount >= viewModel.minCashOut && amount <= balance
    val canSubmit = accountValid && amountValid && password.isNotEmpty() && !processing

    BackHandler {
        viewModel.clearCashOut()
        viewModel.navigateTo(Screen.OperatorTerminal.route)
    }

    LaunchedEffect(receipt) {
        if (receipt != null) {
            amountText = ""
            password = ""
        }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Cash out", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearCashOut()
                        viewModel.navigateTo(Screen.OperatorTerminal.route)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GradientBalanceCard(
                title = "Available to cash out",
                amountText = formatPeso(balance),
                footer = "Operator ID: ${operator?.userID ?: "-"}"
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Send to", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionCard(
                    selected = destination == "GCASH",
                    onClick = { destination = "GCASH" },
                    modifier = Modifier.weight(1f),
                    enabled = !processing
                ) {
                    Text("GCash", fontWeight = FontWeight.Bold)
                    Text("E-wallet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OptionCard(
                    selected = destination == "MAYA",
                    onClick = { destination = "MAYA" },
                    modifier = Modifier.weight(1f),
                    enabled = !processing
                ) {
                    Text("Maya", fontWeight = FontWeight.Bold)
                    Text("E-wallet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = account,
                onValueChange = { input -> if (input.all { it.isDigit() } && input.length <= 11) account = input },
                label = { Text("${paymentMethodLabel(destination)} mobile number") },
                singleLine = true,
                enabled = !processing,
                isError = account.isNotEmpty() && !accountValid,
                supportingText = { Text("11 digits, starts with 09") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> if (input.all { it.isDigit() } && input.length <= 6) amountText = input },
                label = { Text("Amount") },
                prefix = { Text("₱") },
                singleLine = true,
                enabled = !processing,
                isError = amountText.isNotEmpty() && !amountValid,
                supportingText = {
                    Text(
                        if (amountText.isNotEmpty() && amount > balance) "Higher than your available balance"
                        else "Minimum " + formatPeso(viewModel.minCashOut)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100.0, 500.0, 1000.0).filter { it <= balance }.forEach { quick ->
                    FilterChip(
                        selected = amountText == quick.toLong().toString(),
                        onClick = { amountText = quick.toLong().toString() },
                        enabled = !processing,
                        label = { Text("₱" + quick.toLong()) }
                    )
                }
                if (balance >= viewModel.minCashOut) {
                    FilterChip(
                        selected = amountText == balance.toLong().toString(),
                        onClick = { amountText = balance.toLong().toString() },
                        enabled = !processing,
                        label = { Text("All") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow("Amount", formatPeso(if (amountValid) amount else 0.0))
                    DetailRow("Fee", formatPeso(0.0))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("You will receive", fontWeight = FontWeight.Bold)
                        Text(formatPeso(if (amountValid) amount else 0.0), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Confirm with your password") },
                singleLine = true,
                enabled = !processing,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Show or hide password"
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            viewModel.cashOutError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { viewModel.requestCashOut(amount, destination, account, password) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (processing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (amountValid) "Cash out " + formatPeso(amount) else "Cash out",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Funds are sent to the number above. Please double-check it before confirming.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent cash-outs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (cashOuts.isEmpty()) {
                Text("No cash-outs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        cashOuts.forEachIndexed { index, tx ->
                            TransactionRow(tx)
                            if (index < cashOuts.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    receipt?.let { r ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCashOut() },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
            },
            title = { Text("Cash out successful") },
            text = {
                Column {
                    DetailRow("Amount", formatPeso(r.amount))
                    DetailRow("Sent to", r.destination + " " + r.maskedAccount)
                    DetailRow("Reference", r.reference)
                    DetailRow("Date", formatDateTime(r.timestamp))
                    DetailRow("Remaining balance", formatPeso(r.newBalance))
                }
            },
            confirmButton = { Button(onClick = { viewModel.clearCashOut() }) { Text("Done") } }
        )
    }
}
