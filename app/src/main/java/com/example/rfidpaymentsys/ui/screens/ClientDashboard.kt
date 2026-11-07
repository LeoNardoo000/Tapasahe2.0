package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.example.rfidpaymentsys.navigation.Screen
import androidx.compose.material.icons.filled.ArrowUpward

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import com.example.rfidpaymentsys.data.entity.Transaction
import com.example.rfidpaymentsys.data.entity.passengerTypeLabel
import com.example.rfidpaymentsys.ui.components.ActionTile
import com.example.rfidpaymentsys.ui.components.GradientBalanceCard
import com.example.rfidpaymentsys.ui.components.TransactionRow
import com.example.rfidpaymentsys.ui.components.formatPeso
import com.example.rfidpaymentsys.ui.components.maskMobile
import com.example.rfidpaymentsys.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDashboard(viewModel: MainViewModel) {
    val user = viewModel.currentUser
    val recentTransactions by if (user != null) {
        viewModel.getRecentTransactions(user.userID).collectAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList<Transaction>()) }
    }

    var showLoadDialog by remember { mutableStateOf(false) }
    var loadAmount by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf<String?>(null) }
    var balanceVisible by remember { mutableStateOf(true) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showCardConcerns by remember { mutableStateOf(false) }

    val activity = LocalContext.current as? Activity
    BackHandler {
        // A remembered user just leaves the app; logging out is done with the logout button.
        if (viewModel.isSessionRemembered) activity?.moveTaskToBack(true) else viewModel.showLogoutDialog = true
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = {
                    Column {
                        Text(user?.name ?: "Client", fontWeight = FontWeight.Bold)
                        Text("UID: ${user?.userID ?: "-"}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ClientSupport.route) }) {
                        BadgedBox(badge = {
                            val unread = viewModel.supportThreads.firstOrNull()?.clientUnread ?: 0
                            if (unread > 0) Badge { Text(unread.toString()) }
                        }) {
                            Icon(Icons.Default.Email, contentDescription = "Inbox")
                        }
                    }
                    IconButton(onClick = { viewModel.showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("User not found. Please login again.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                GradientBalanceCard(
                    title = "Available balance",
                    amountText = if (balanceVisible) formatPeso(user.balance) else "₱ ••••••",
                    footer = "Fare: " + formatPeso(viewModel.fareFor(user.passengerType)) +
                        " (" + passengerTypeLabel(user.passengerType) + ")",
                    trailing = {
                        IconButton(onClick = { balanceVisible = !balanceVisible }) {
                            Icon(
                                if (balanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Show or hide balance",
                                tint = Color.White
                            )
                        }
                    }
                )

                if (user.balance < viewModel.regularFare * 3) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (user.balance < viewModel.regularFare) "Your balance is too low for your next ride. Please load now."
                                else "Low balance. Please load soon.",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(Icons.Default.Add, "Load", { showLoadDialog = true }, Modifier.weight(1f))
                    ActionTile(Icons.Default.ArrowUpward, "Withdraw", { viewModel.withdrawError = null; showWithdrawDialog = true }, Modifier.weight(1f))
                    ActionTile(Icons.Default.CreditCard, "Card", { showCardConcerns = true }, Modifier.weight(1f))
                    ActionTile(Icons.Default.Lock, "Password", { viewModel.showChangePasswordDialog = true }, Modifier.weight(1f))
                }

                if (viewModel.currentCard?.status == "DISABLED") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Your card is disabled and cannot be used to pay.",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showCardConcerns = true }) { Text("MANAGE") }
                        }
                    }
                }

                FareNoticeCard(viewModel.fareNotice, user.registrationDate)

                Spacer(modifier = Modifier.height(16.dp))
                SupportPromptCard(
                    unread = viewModel.supportThreads.firstOrNull()?.clientUnread ?: 0,
                    onClick = { viewModel.navigateTo(Screen.ClientSupport.route) }
                )

                Spacer(modifier = Modifier.height(28.dp))
                Text("Recent activity", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (recentTransactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            "No activity yet. Your rides and top-ups will show up here.",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            recentTransactions.forEachIndexed { index, tx ->
                                TransactionRow(tx)
                                if (index < recentTransactions.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
                val myWithdrawals = viewModel.withdrawals
                    .filter { it.userId == user.userID }
                    .sortedByDescending { it.createdAt }
                    .take(5)
                if (myWithdrawals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Withdrawals", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    WithdrawalsSection(myWithdrawals)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawDialog(viewModel) {
            showWithdrawDialog = false
            viewModel.withdrawError = null
        }
    }
    viewModel.pendingFareNotice?.let { notice ->
        FareNoticeDialog(notice) { viewModel.dismissFareNotice() }
    }
    if (showCardConcerns) {
        CardConcernsDialog(viewModel) { showCardConcerns = false }
    }
    viewModel.withdrawalNotice?.let { notice ->
        WithdrawalSubmittedDialog(notice) { viewModel.clearWithdrawalNotice() }
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false; loadError = null },
            title = { Text("Load balance", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Choose an amount or enter your own.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("50", "100", "200", "500").chunked(2).forEach { rowValues ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowValues.forEach { v ->
                                FilterChip(
                                    selected = loadAmount == v,
                                    onClick = { loadAmount = v; loadError = null },
                                    label = {
                                        Text("₱$v", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = loadAmount,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                loadAmount = input
                                loadError = null
                            }
                        },
                        label = { Text("Amount") },
                        prefix = { Text("₱") },
                        singleLine = true,
                        supportingText = { Text("From ₱20 to ₱5,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    loadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = loadAmount.toDoubleOrNull() ?: 0.0
                    if (viewModel.createPayment(amount)) {
                        showLoadDialog = false
                        loadAmount = ""
                        loadError = null
                    } else {
                        loadError = "Enter an amount from ₱20 to ₱5,000."
                    }
                }) { Text("CONTINUE") }
            },
            dismissButton = {
                TextButton(onClick = { showLoadDialog = false; loadError = null }) { Text("CANCEL") }
            }
        )
    }

    PaymentCheckout(viewModel)
}
