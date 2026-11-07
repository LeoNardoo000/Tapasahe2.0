package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar
import com.example.rfidpaymentsys.ui.components.PasswordField
import com.example.rfidpaymentsys.util.InputValidation
import com.example.rfidpaymentsys.ui.components.PassengerTypeSelector

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.components.WeeklyProfitChart
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import com.example.rfidpaymentsys.data.entity.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: MainViewModel) {
    val users by viewModel.allUsers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Transactions", "Audit", "Fares", "Withdrawals", "Support", "System")
    var showAddUserDialog by remember { mutableStateOf(false) }
    val pendingFareCount = viewModel.fareRequests.count { it.status == "PENDING" }
    val pendingWithdrawalCount = viewModel.withdrawals.count { it.status == "PENDING" }
    val unreadChatCount = viewModel.supportThreads.count { it.adminUnread > 0 }

    BackHandler {
        if (selectedTab != 0) selectedTab = 0
        else viewModel.navigateTo(Screen.Welcome.route)
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedTab != 0) selectedTab = 0 
                        else viewModel.navigateTo(Screen.Welcome.route)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showAddUserDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add New User")
                        }
                    }
                    IconButton(onClick = { viewModel.showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Red)
                    }
                    IconButton(onClick = { viewModel.showChangePasswordDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Change Password")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                        val badge = when (title) {
                            "Fares" -> pendingFareCount
                            "Withdrawals" -> pendingWithdrawalCount
                            "Support" -> unreadChatCount
                            else -> 0
                        }
                        Text(if (badge > 0) "$title ($badge)" else title, maxLines = 1)
                    }
                    )
                }
            }

            when (selectedTab) {
                0 -> UserManagementTab(viewModel, users)
                1 -> TransactionHistoryTab(viewModel, transactions)
                2 -> AuditLogTab(auditLogs)
                3 -> FareRatesTab(viewModel)
                4 -> WithdrawalsTab(viewModel)
                5 -> SupportTab(viewModel)
                6 -> SystemToolsTab(viewModel)
            }
        }

        if (showAddUserDialog) {
            AdminAddUserDialog(
                onDismiss = { showAddUserDialog = false },
                onConfirm = { uid, name, balance, role, pass, email, phone, passengerType, username ->
                    viewModel.addUser(uid, name, balance, role, pass, email, phone, passengerType, username)
                    showAddUserDialog = false
                }
            )
        }
    }
}

@Composable
fun AdminAddUserDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, String, String, String, String, String, String) -> Unit) {
    var uid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CLIENT") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passengerType by remember { mutableStateOf(PassengerType.REGULAR) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Account") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it.filter { c -> c.isLetterOrDigit() }.take(32); error = null },
                    label = { Text("UID / User ID") },
                    supportingText = { Text("Leave empty to create one automatically") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = InputValidation.sanitizeNameInput(it); error = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = InputValidation.sanitizeUsernameInput(it); error = null },
                    label = { Text("Username") },
                    supportingText = { Text("Needed to log in (not required for Admin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = InputValidation.sanitizeEmailInput(it); error = null },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 13) {
                            phone = input
                            error = null
                        }
                    },
                    label = { Text("Phone (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = "Password",
                    modifier = Modifier.fillMaxWidth()
                )

                if (role == "CLIENT") {
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { input ->
                            if (input.length <= 7 && input.count { c -> c == '.' } <= 1 && input.all { c -> c.isDigit() || c == '.' }) {
                                balance = input
                                error = null
                            }
                        },
                        label = { Text("Initial Balance") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Passenger type:", style = MaterialTheme.typography.labelLarge)
                    PassengerTypeSelector(selected = passengerType, onSelect = { passengerType = it })
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Role Selection:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "CLIENT", onClick = { role = "CLIENT" })
                        Text("Client")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "OPERATOR", onClick = { role = "OPERATOR" })
                        Text("Operator")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "ADMIN", onClick = { role = "ADMIN" })
                        Text("Admin")
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val problem = InputValidation.nameError(name)
                    ?: (if (role != "ADMIN" || username.isNotBlank()) InputValidation.usernameError(username) else null)
                    ?: (if (email.isNotBlank()) InputValidation.emailError(email) else null)
                    ?: (if (phone.isNotEmpty() && !InputValidation.isValidPhone(phone)) "Phone number must be 10 to 13 digits." else null)
                    ?: InputValidation.passwordError(password)
                if (problem != null) {
                    error = problem
                } else {
                    onConfirm(
                        uid,
                        InputValidation.normalizeName(name),
                        balance.toDoubleOrNull() ?: 0.0,
                        role,
                        password,
                        InputValidation.normalizeEmail(email),
                        phone,
                        passengerType,
                        InputValidation.normalizeUsername(username)
                    )
                }
            }) {
                Text("ADD ACCOUNT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

/** Edit form. Its fields are created fresh every time it opens, so it never shows another user's data. */
@Composable
fun EditUserDialog(user: UserAccount, onDismiss: () -> Unit, onSave: (UserAccount) -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var username by remember { mutableStateOf(user.username) }
    var contact by remember { mutableStateOf(user.contactInfo) }
    var newPassword by remember { mutableStateOf("") }
    var passengerType by remember { mutableStateOf(user.passengerType) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${user.role} Info") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = InputValidation.sanitizeNameInput(it); error = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = InputValidation.sanitizeUsernameInput(it); error = null },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = InputValidation.sanitizeEmailInput(it); error = null },
                    label = { Text("Contact (email or phone)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = "New password (leave empty to keep)",
                    modifier = Modifier.fillMaxWidth()
                )
                if (user.role == "CLIENT") {
                    Text("Passenger type:", style = MaterialTheme.typography.labelLarge)
                    PassengerTypeSelector(selected = passengerType, onSelect = { passengerType = it })
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val problem = InputValidation.nameError(name)
                    ?: (if (username.isNotBlank()) InputValidation.usernameError(username) else null)
                    ?: InputValidation.contactError(contact, required = false)
                    ?: (if (newPassword.isNotEmpty()) InputValidation.passwordError(newPassword) else null)
                if (problem != null) {
                    error = problem
                } else {
                    val cleanContact = contact.trim().let {
                        if (InputValidation.isValidEmail(it)) InputValidation.normalizeEmail(it) else it
                    }
                    onSave(
                        user.copy(
                            name = InputValidation.normalizeName(name),
                            username = InputValidation.normalizeUsername(username),
                            contactInfo = cleanContact,
                            password = if (newPassword.isEmpty()) user.password else newPassword,
                            passengerType = passengerType
                        )
                    )
                }
            }) { Text("SAVE CHANGES") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementTab(viewModel: MainViewModel, users: List<UserAccount>) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("All", "Clients", "Operators")
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Account Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        val applicants = users.filter { it.role == "OPERATOR" && it.accountStatus == "PENDING" }
        if (applicants.isNotEmpty()) {
            OperatorApplicationsCard(viewModel, applicants)
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex
                ) { Text(label) }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val filteredUsers = users.filter { user ->
            val matchesRole = when (selectedIndex) {
                1 -> user.role == "CLIENT"
                2 -> user.role == "OPERATOR"
                else -> true
            }
            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) || 
                               user.userID.contains(searchQuery, ignoreCase = true)
            matchesRole && matchesSearch
        }

        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching accounts found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredUsers, key = { it.userID }) { user ->
                    AdminUserItem(viewModel, user)
                }
            }
        }
    }
}

@Composable
fun AdminUserItem(viewModel: MainViewModel, user: UserAccount) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (user.role) {
                "ADMIN" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                "OPERATOR" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = when (user.role) {
                        "ADMIN" -> MaterialTheme.colorScheme.primary
                        "OPERATOR" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = user.role,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (user.role == "CLIENT") MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("ID: ${user.userID}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (user.username.isNotBlank()) {
                    Text("Username: ${user.username}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (user.role == "OPERATOR" && user.accountStatus != "ACTIVE") {
                    Text(
                        if (user.accountStatus == "PENDING") "Application pending approval" else "Application rejected",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (user.accountStatus == "PENDING") Color(0xFFE65100) else Color(0xFFC62828)
                    )
                }
                if (user.role == "CLIENT") {
                    Text("Passenger: ${passengerTypeLabel(user.passengerType)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (user.contactInfo.isNotBlank()) Text("Contact: ${user.contactInfo}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                if (user.role == "CLIENT" || user.role == "OPERATOR") {
                    Text(
                        text = if (user.role == "CLIENT") "Balance: ₱${String.format(Locale.US, "%.2f", user.balance)}" 
                               else "Earnings: ₱${String.format(Locale.US, "%.2f", user.balance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (user.role == "CLIENT") Color(0xFF2E7D32) else Color(0xFF1B6FE8),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row {
                if (user.role == "CLIENT") {
                    IconButton(onClick = { showTopUpDialog = true }) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Top up", tint = Color(0xFF2E7D32))
                    }
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${user.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog) {
        EditUserDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateUserFully(updated)
                showEditDialog = false
            }
        )
    }

    if (showTopUpDialog) {
        AlertDialog(
            onDismissRequest = { showTopUpDialog = false },
            title = { Text("Reload Balance") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Adding money to ${user.name}")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = topUpAmount,
                        onValueChange = { input ->
                            if (input.length <= 7 && input.count { c -> c == '.' } <= 1 && input.all { c -> c.isDigit() || c == '.' }) topUpAmount = input
                        },
                        label = { Text("Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = topUpAmount.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.topUpBalance(user.userID, amount)
                        showTopUpDialog = false
                        topUpAmount = ""
                    }
                }) { Text("CONFIRM TOP-UP") }
            },
            dismissButton = {
                TextButton(onClick = { showTopUpDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SystemToolsTab(viewModel: MainViewModel) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("System Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        viewModel.adminStatus?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Data Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val context = LocalContext.current
                OutlinedButton(onClick = { viewModel.exportToCsv(context) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("EXPORT DATABASE TO CSV")
                }
                
                Button(onClick = { viewModel.refreshFromServer() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("FORCE REFRESH")
                }
                
                Button(onClick = { viewModel.generateDailyReports() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("GENERATE INCOME REPORTS")
                }
            }
        }

        Text("Danger Zone", color = Color.Red, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showResetConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("RESET LOGS (KEEP PROFIT)")
                }
                
                Button(onClick = { showClearConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("CLEAR ALL TRANSACTION LOGS")
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Transactions") },
            text = { Text("This will delete all Top-up and Registration logs, keeping only the RFID logs. Continue?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetTransactionsKeepProfit()
                    showResetConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("RESET") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Logs") },
            text = { Text("This will permanently delete EVERY transaction log. Continue?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAllTransactions()
                    showClearConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("CLEAR ALL") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun TransactionHistoryTab(viewModel: MainViewModel, transactions: List<Transaction>) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
    val rfidTransactions = transactions.filter { it.method == "RFID" }
    val totalRevenue = rfidTransactions.sumOf { it.amount }
    val weeklyProfit = viewModel.getWeeklyProfit()

    val operatorProfits = rfidTransactions
        .groupBy { it.driverID }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      item {
        Column {
        Text("Accounting Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Total Net Profit (All Operators)", style = MaterialTheme.typography.titleMedium)
                }
                Text(text = "₱${String.format(Locale.US, "%.2f", totalRevenue)}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Weekly Performance (Aggregated)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        WeeklyProfitChart(weeklyProfit)

        if (operatorProfits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Profit Breakdown by Operator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    operatorProfits.take(3).forEach { (opId, profit) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (opId.isBlank()) "Unknown Driver" else "Driver: $opId", style = MaterialTheme.typography.bodySmall)
                            Text(text = "₱${String.format(Locale.US, "%.2f", profit)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Detailed Transaction Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        }
        }
        if (transactions.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No transactions yet.", color = Color.Gray) } }
        } else {
            run {
                items(transactions) { tx ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("User: ${tx.userID}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (tx.driverID.isNotBlank()) Text("Driver: ${tx.driverID}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("${tx.displayTitle()} - ${tx.status}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                Text(dateFormat.format(Date(tx.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(text = (if (tx.isDebit()) "- ₱" else "+ ₱") + String.format(Locale.US, "%.2f", tx.amount), color = if (tx.isDebit()) Color.Red else Color(0xFF2E7D32), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogTab(logs: List<AuditLog>) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Security Audit Logs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No security logs recorded yet.", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = log.action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = log.details, style = MaterialTheme.typography.bodySmall)
                            Text(text = "Admin ID: ${log.adminUid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Pending operator applications, with Approve and Reject. */
@Composable
private fun OperatorApplicationsCard(viewModel: MainViewModel, applicants: List<UserAccount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Operator applications (${applicants.size})", fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
            applicants.forEach { applicant ->
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(applicant.name, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Username: ${applicant.username}", fontSize = 12.sp, color = Color.DarkGray)
                Text("Contact: ${applicant.contactInfo}", fontSize = 12.sp, color = Color.DarkGray)
                Text("UID: ${applicant.userID}", fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { viewModel.rejectOperator(applicant) }, modifier = Modifier.weight(1f)) {
                        Text("REJECT", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { viewModel.approveOperator(applicant) }, modifier = Modifier.weight(1f)) {
                        Text("APPROVE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
