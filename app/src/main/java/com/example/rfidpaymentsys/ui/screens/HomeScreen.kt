package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar
import android.app.Activity
import androidx.compose.material.icons.filled.Email

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.components.LottieAnimationView
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.Lock
import com.example.rfidpaymentsys.R
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import kotlin.time.Duration.Companion.seconds
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import com.example.rfidpaymentsys.data.entity.UserAccount
import java.util.Locale


@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val users by viewModel.recentUsers.collectAsState()
    val operator = viewModel.currentUser // Logged in operator

    val activity = LocalContext.current as? Activity
    BackHandler {
        // A remembered user just leaves the app; logging out is done with the logout button.
        if (viewModel.isSessionRemembered) activity?.moveTaskToBack(true) else viewModel.showLogoutDialog = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandTopAppBar(
                title = {
                    Column {
                        Text(operator?.name ?: "Operator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("UID: ${operator?.userID ?: "-"}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                IconButton(onClick = { viewModel.showLogoutDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                }
                
                IconButton(onClick = { viewModel.navigateTo(Screen.OperatorProfit.route) }) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "My Profit")
                }

                IconButton(onClick = { viewModel.navigateTo(Screen.OperatorCashOut.route) }) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Cash out")
                }

                IconButton(onClick = { viewModel.navigateTo(Screen.OperatorSupport.route) }) {
                    BadgedBox(badge = {
                        val unread = viewModel.supportThreads.firstOrNull()?.clientUnread ?: 0
                        if (unread > 0) Badge { Text(unread.toString()) }
                    }) {
                        Icon(Icons.Default.Email, contentDescription = "Inbox")
                    }
                }

                IconButton(onClick = { viewModel.showChangePasswordDialog = true }) {
                    Icon(Icons.Default.Lock, contentDescription = "Change Password")
                }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(96.dp).padding(bottom = 12.dp),
                    )

                    Text(text = "Operator Terminal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(8.dp))
                    OperatorFareCard(viewModel)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Scanner : ", fontSize = 18.sp)
                        Text(
                            text = if (viewModel.isConnected) "Ready" else "Not Found",
                            fontSize = 18.sp,
                            color = if (viewModel.isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                        if (!viewModel.isConnected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.connectArduino() }) { Text("Search", fontSize = 14.sp) }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    

                    if (!viewModel.isConnected && viewModel.scannerStatus != null) {
                        Text(text = viewModel.scannerStatus!!, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (viewModel.scannedUid == null) {
                        Text(text = "Scan Card to Process Payment", fontSize = 20.sp, color = Color.Gray)
                    } else {
                        val scannedUser = viewModel.scannedUser
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "UID: ${viewModel.scannedUid}", fontSize = 16.sp, color = Color.Gray)

                            if (viewModel.paymentStatus != null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                if (scannedUser != null) {
                                    Text(
                                        text = scannedUser.name,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                viewModel.paymentStatus?.let { status ->
                                    LaunchedEffect(status) {
                                        if (status.contains("Paid") || status.contains("Registered")) {
                                            delay(5.seconds)
                                            viewModel.clearStatus()
                                        }
                                    }

                                    AlertDialog(
                                        onDismissRequest = { if (!status.contains("Processing")) viewModel.clearStatus() },
                                        title = {
                                            Text(
                                                text = when {
                                                    status.contains("Processing") -> "Processing"
                                                    status.contains("Paid") || status.contains("Registered") -> "Success"
                                                    status.contains("New Card") -> "System"
                                                    else -> "Notice"
                                                },
                                                color = when {
                                                    status.contains("Processing") -> Color.Gray
                                                    status.contains("Paid") || status.contains("Registered") -> Color(0xFF2E7D32)
                                                    status.contains("New Card") -> MaterialTheme.colorScheme.primary
                                                    else -> Color.Red
                                                }
                                            )
                                        },
                                        text = {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                if (status.contains("Processing")) {
                                                    LottieAnimationView(resId = R.raw.processing, modifier = Modifier.size(100.dp))
                                                } else if (status.contains("Paid") || status.contains("Registered")) {
                                                    LottieAnimationView(resId = R.raw.success, modifier = Modifier.size(100.dp), iterations = 1)
                                                }

                                                Text(text = status, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        },
                                        confirmButton = {
                                            if (!status.contains("Processing")) {
                                                Button(onClick = { viewModel.clearStatus() }) { Text("OK") }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                }

                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Recent Users", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(users.filter { it.userID.isNotBlank() }) { user ->
                            UserListItem(user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: UserAccount) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "ID: ${user.userID}", fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = "₱${String.format(Locale.US, "%.2f", user.balance)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
