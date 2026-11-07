package com.example.rfidpaymentsys.ui.screens
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rfidpaymentsys.ui.components.DetailRow
import com.example.rfidpaymentsys.ui.components.OptionCard
import com.example.rfidpaymentsys.ui.components.formatDateTime
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import com.example.rfidpaymentsys.viewmodel.PaymentReceipt
import kotlinx.coroutines.delay

/**
 * Full-screen checkout for balance top-ups.
 * Appears whenever viewModel.paymentSession is not null.
 * Stages: choose method -> processing -> receipt (or error).
 */
@Composable
fun PaymentCheckout(viewModel: MainViewModel) {
    val session = viewModel.paymentSession ?: return
    val receipt = viewModel.paymentReceipt
    val error = viewModel.paymentError

    var selected by remember(session.reference) { mutableStateOf("GCASH") }
    var processing by remember(session.reference) { mutableStateOf(false) }

    LaunchedEffect(processing) {
        if (processing) {
            delay(1800)
            viewModel.completePayment(selected)
        }
    }

    // Drawn as part of the screen (not a pop-up window), inside the safe area,
    // so the buttons can never end up under the phone's navigation bar.
    BackHandler {
        when {
            receipt != null || error != null -> viewModel.closePayment()
            !processing -> viewModel.cancelPayment()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {},
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            receipt != null -> PaymentSuccessView(receipt) { viewModel.closePayment() }
            error != null -> PaymentErrorView(error) { viewModel.closePayment() }
            else -> CheckoutView(
                amount = session.amount,
                reference = session.reference,
                selected = selected,
                processing = processing,
                onSelect = { selected = it },
                onPay = { processing = true },
                onCancel = { viewModel.cancelPayment() }
            )
        }
    }
}

@Composable
private fun CheckoutView(
    amount: Double,
    reference: String,
    selected: String,
    processing: Boolean,
    onSelect: (String) -> Unit,
    onPay: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, enabled = !processing) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text("Checkout", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Secure payment", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Tapasahe", fontWeight = FontWeight.Bold)
                            Text("Balance top-up", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Amount due", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPeso(amount), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Pay with", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            MethodOption("GCASH", "GCash", "Pay with your GCash wallet", Icons.Default.PhoneAndroid, selected == "GCASH", !processing, onSelect)
            Spacer(modifier = Modifier.height(10.dp))
            MethodOption("MAYA", "Maya", "Pay with your Maya wallet", Icons.Default.AccountBalanceWallet, selected == "MAYA", !processing, onSelect)
            Spacer(modifier = Modifier.height(10.dp))
            MethodOption("CARD", "Credit / Debit Card", "Visa or Mastercard", Icons.Default.CreditCard, selected == "CARD", !processing, onSelect)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Reference: $reference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (processing) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Processing your payment...", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Button(
                    onClick = onPay,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Pay " + formatPeso(amount), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun MethodOption(
    code: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    OptionCard(selected = selected, enabled = enabled, onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null, enabled = enabled)
        }
    }
}

@Composable
private fun PaymentSuccessView(receipt: PaymentReceipt, onDone: () -> Unit) {
    // The content scrolls if the phone is small; the Done button always stays on screen.
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFE6F4EA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Payment successful", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatPeso(receipt.amount), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow("Payment method", receipt.methodLabel)
                    DetailRow("Reference", receipt.reference)
                    DetailRow("Date", formatDateTime(receipt.timestamp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("New balance", formatPeso(receipt.newBalance))
                }
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Done", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PaymentErrorView(message: String, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFFDECEA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Payment unsuccessful", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Close", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}
