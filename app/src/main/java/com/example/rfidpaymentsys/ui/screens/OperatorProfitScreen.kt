package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.components.WeeklyProfitChart
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorProfitScreen(viewModel: MainViewModel) {
    val operator = viewModel.currentUser
    val allTransactions by viewModel.allTransactions.collectAsState()
    
    // Calculate stats reactively based on the transaction list
    val dailyProfit = remember(allTransactions) {
        operator?.let { viewModel.getDailyProfit(it.userID) } ?: 0.0
    }
    val profitHistory = remember(allTransactions) {
        operator?.let { viewModel.getProfitHistory(it.userID) } ?: emptyList()
    }
    val weeklyPerformance = remember(allTransactions) {
        operator?.let { viewModel.getWeeklyProfit(it.userID) } ?: emptyList()
    }

    BackHandler {
        viewModel.navigateTo(Screen.OperatorTerminal.route)
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Profit Reports") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.OperatorTerminal.route) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Profit Summary", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Total Earnings Balance
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "TOTAL EARNINGS BALANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    Text(text = "₱${String.format(Locale.US, "%.2f", operator?.balance ?: 0.0)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.OperatorCashOut.route) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    ) { Text("CASH OUT", fontWeight = FontWeight.Bold) }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "TODAY'S PROFIT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(text = "₱${String.format(Locale.US, "%.2f", dailyProfit)}", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "LAST 7 DAYS PERFORMANCE", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            WeeklyProfitChart(data = weeklyPerformance)
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "DAILY PROFIT HISTORY", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            
            if (profitHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No profit history found.", color = Color.Gray) }
            } else {
                profitHistory.forEach { (date, amount) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = date, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(text = "₱${String.format(Locale.US, "%.2f", amount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Summary based on all successful transactions processed by you.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
