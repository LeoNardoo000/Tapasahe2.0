package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.entity.PassengerType
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.components.PassengerTypeSelector
import com.example.rfidpaymentsys.ui.components.PasswordField
import com.example.rfidpaymentsys.util.InputValidation
import com.example.rfidpaymentsys.viewmodel.MainViewModel

@Composable
private fun GoodDayHeader() {
    Text("Good Day!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Please log in to continue.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun RememberMeRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text("Remember me on this device")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuthScreen(viewModel: MainViewModel) {
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Admin Portal") },
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GoodDayHeader()
            Text("Admin Login", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Enter your password",
                modifier = Modifier.fillMaxWidth(),
                isError = viewModel.loginError != null,
                imeAction = ImeAction.Done,
                onDone = { viewModel.adminLogin(password) }
            )
            if (viewModel.loginError != null) {
                Text(viewModel.loginError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.adminLogin(password) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("LOGIN AS ADMIN")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorAuthScreen(viewModel: MainViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }

    // login
    var loginUsername by remember { mutableStateOf(viewModel.lastLoginName("OPERATOR")) }
    var loginPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    // registration
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val notice = viewModel.operatorApplicationNotice

    fun submitRegistration() {
        localError = InputValidation.nameError(name)
            ?: InputValidation.usernameError(username)
            ?: InputValidation.contactError(contact, required = true)
            ?: InputValidation.passwordError(password)
            ?: if (password != confirmPassword) "Passwords do not match." else null
        if (localError == null) {
            viewModel.operatorRegister(name, username, contact, password, confirmPassword)
        }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Operator Access") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.dismissOperatorNotice()
                        viewModel.navigateTo(Screen.Welcome.route)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (notice != null) {
                // The application was sent. The admin has to approve it before the operator can log in.
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(notice, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You can log in with your username and password once the admin approves it.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.dismissOperatorNotice()
                        name = ""; username = ""; contact = ""; password = ""; confirmPassword = ""
                        isLoginTab = true
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("BACK TO LOGIN") }
            } else {
                TabRow(selectedTabIndex = if (isLoginTab) 0 else 1) {
                    Tab(selected = isLoginTab, onClick = { isLoginTab = true; viewModel.loginError = null; localError = null }) {
                        Text("LOGIN", modifier = Modifier.padding(16.dp))
                    }
                    Tab(selected = !isLoginTab, onClick = { isLoginTab = false; viewModel.loginError = null; localError = null }) {
                        Text("REGISTER", modifier = Modifier.padding(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (isLoginTab) {
                    GoodDayHeader()
                    Text("Operator Login", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = loginUsername,
                        onValueChange = { loginUsername = it.filter { c -> !c.isWhitespace() }.take(32) },
                        label = { Text("Username") },
                        placeholder = { Text("Enter your username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PasswordField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        modifier = Modifier.fillMaxWidth(),
                        imeAction = ImeAction.Done,
                        onDone = { viewModel.operatorLogin(loginUsername, loginPassword, rememberMe) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RememberMeRow(rememberMe) { rememberMe = it }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.operatorLogin(loginUsername, loginPassword, rememberMe) },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("LOGIN") }
                    TextButton(onClick = { viewModel.openForgotPassword() }) {
                        Text("Forgot Password?")
                    }
                } else {
                    Text("Operator Registration", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your application will be reviewed by the admin.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = InputValidation.sanitizeNameInput(it) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = InputValidation.sanitizeUsernameInput(it) },
                        label = { Text("Username") },
                        supportingText = { Text("4 to 20 letters, numbers, dots or underscores") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = InputValidation.sanitizeEmailInput(it) },
                        label = { Text("Email or Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        modifier = Modifier.fillMaxWidth(),
                        imeAction = ImeAction.Next
                    )
                    Text(
                        "At least ${InputValidation.MIN_PASSWORD_LENGTH} characters",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        modifier = Modifier.fillMaxWidth(),
                        imeAction = ImeAction.Done,
                        onDone = { submitRegistration() }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { submitRegistration() },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("SUBMIT APPLICATION") }
                }

                val displayError = localError ?: viewModel.loginError
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientAuthScreen(viewModel: MainViewModel) {
    var isRegistering by remember { mutableStateOf(viewModel.startInRegistration) }

    // login
    var loginUsername by remember { mutableStateOf(viewModel.lastLoginName("CLIENT")) }
    var loginPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    // registration
    var uid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passengerType by remember { mutableStateOf(PassengerType.REGULAR) }

    val lookup = viewModel.cardLookup
    val registeredTo = if (isRegistering) lookup?.ownerName else null

    LaunchedEffect(Unit) { viewModel.startInRegistration = false }

    // A tapped card fills in the UID box by itself.
    LaunchedEffect(lookup) {
        if (isRegistering && lookup != null) {
            uid = if (lookup.ownerName == null) lookup.uid else ""
        }
    }

    // After a successful registration, switch to the login form with the username filled in.
    LaunchedEffect(viewModel.loginError) {
        if (isRegistering && viewModel.loginError?.startsWith("Registered") == true) {
            isRegistering = false
            loginUsername = username
            loginPassword = ""
            uid = ""; name = ""; username = ""; email = ""; password = ""; confirmPassword = ""
            passengerType = PassengerType.REGULAR
        }
    }

    fun submit() {
        if (isRegistering) {
            viewModel.clientRegister(uid, name, username, email, password, confirmPassword, passengerType)
        } else {
            viewModel.clientLogin(loginUsername, loginPassword, rememberMe)
        }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = { Text("Client Access") },
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRegistering) {
                Text("Register Card", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Scan your Card to register", fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                ReaderStatusRow(viewModel)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it.filter { c -> c.isLetterOrDigit() }.take(32) },
                    label = { Text("RFID UID") },
                    placeholder = { Text("Tap your card on the reader") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                if (registeredTo != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Text(
                            "This Card is Registered to $registeredTo",
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = InputValidation.sanitizeNameInput(it) },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Text(
                    "UID: ${uid.ifBlank { "-" }}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = InputValidation.sanitizeUsernameInput(it) },
                    label = { Text("Username") },
                    supportingText = { Text("4 to 20 letters, numbers, dots or underscores") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = InputValidation.sanitizeEmailInput(it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    modifier = Modifier.fillMaxWidth(),
                    imeAction = ImeAction.Next
                )
                Text(
                    "At least ${InputValidation.MIN_PASSWORD_LENGTH} characters",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                PasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    modifier = Modifier.fillMaxWidth(),
                    imeAction = ImeAction.Done,
                    onDone = { submit() }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("I am a:", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                PassengerTypeSelector(selected = passengerType, onSelect = { passengerType = it })

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { submit() },
                    enabled = uid.isNotBlank() && registeredTo == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("REGISTER") }
                TextButton(onClick = {
                    isRegistering = false
                    viewModel.loginError = null
                }) { Text("Already registered? Login here") }
            } else {
                GoodDayHeader()
                Text("Client Login", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = loginUsername,
                    onValueChange = { loginUsername = it.filter { c -> !c.isWhitespace() }.take(100) },
                    label = { Text("Username") },
                    placeholder = { Text("Enter your username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = "Password",
                    placeholder = "Enter your password",
                    modifier = Modifier.fillMaxWidth(),
                    imeAction = ImeAction.Done,
                    onDone = { submit() }
                )
                Spacer(modifier = Modifier.height(4.dp))
                RememberMeRow(rememberMe) { rememberMe = it }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("LOGIN")
                }
                TextButton(onClick = { viewModel.openForgotPassword() }) {
                    Text("Forgot Password?")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { isRegistering = true; viewModel.loginError = null },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = Color.Black
                    )
                ) {
                    Text("New User? Register your card here", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            if (viewModel.loginError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val message = viewModel.loginError!!
                Text(
                    text = message,
                    color = if (message.contains("Registered!") || message.contains("success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
