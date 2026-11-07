package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.viewmodel.MainViewModel

/** Shows whether the card reader is connected, with a button to search for it. */
@Composable
fun ReaderStatusRow(viewModel: MainViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Card reader: ", fontSize = 14.sp)
            Text(
                if (viewModel.isConnected) "Ready" else "Not found",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            if (!viewModel.isConnected) {
                TextButton(onClick = { viewModel.connectArduino() }) { Text("Search", fontSize = 13.sp) }
            }
        }
        if (!viewModel.isConnected && viewModel.scannerStatus != null) {
            Text(viewModel.scannerStatus!!, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

/** Main-screen tool: tap any card to see its ID, and whether it already belongs to someone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScannerScreen(viewModel: MainViewModel) {
    val lookup = viewModel.cardLookup
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) { viewModel.clearCardLookup() }
    BackHandler { viewModel.navigateTo(Screen.Welcome.route) }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Scan Card ID", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Welcome.route) }) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReaderStatusRow(viewModel)
            Spacer(modifier = Modifier.height(32.dp))

            if (lookup == null) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tap a card on the reader", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Its ID will show up here so you can use it to register.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Card ID", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("UID: ${lookup.uid}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(lookup.uid)) }) { Text("COPY ID") }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (lookup.ownerName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Text(
                            "This Card is Registered to ${lookup.ownerName}",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFBF360C)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA))
                    ) {
                        Text(
                            "This card is not registered yet.",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.startInRegistration = true
                            viewModel.navigateTo(Screen.ClientAuth.route)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("REGISTER THIS CARD", fontWeight = FontWeight.Bold) }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.clearCardLookup() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("SCAN ANOTHER CARD") }
            }
        }
    }
}
