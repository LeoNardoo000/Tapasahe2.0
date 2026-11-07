package com.example.rfidpaymentsys.ui.screens
import com.example.rfidpaymentsys.ui.components.BrandTopAppBar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rfidpaymentsys.data.model.SupportKind
import com.example.rfidpaymentsys.data.model.SupportMessage
import com.example.rfidpaymentsys.data.model.SupportThread
import com.example.rfidpaymentsys.data.model.supportKindLabel
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.util.InputValidation
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import com.example.rfidpaymentsys.viewmodel.UserRole
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun chatTime(timestamp: Long): String =
    if (timestamp <= 0L) "" else SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(timestamp))

/** Coloured label that says what a conversation is about. */
@Composable
fun SupportKindChip(kind: String) {
    val (background, textColor) = when (kind) {
        SupportKind.OPERATOR -> Color(0xFFD7F2EE) to Color(0xFF00695C)
        SupportKind.FORGOT_PASSWORD -> Color(0xFFFDE0D2) to Color(0xFFBF360C)
        else -> Color(0xFFDCE8FF) to Color(0xFF0B3D91)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            supportKindLabel(kind),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/** The message list and the text box. Used by clients, operators and the admin. */
@Composable
fun ChatView(
    messages: List<SupportMessage>,
    isAdminViewer: Boolean,
    emptyHint: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyHint, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message, mine = (message.senderRole == "ADMIN") == isAdminViewer)
                }
            }
        }

        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 500) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    onSend(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SupportMessage, mine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 16.dp
            ),
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            tonalElevation = if (mine) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!mine && message.senderName.isNotBlank()) {
                    Text(
                        message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    message.text,
                    color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            chatTime(message.timestamp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/** The card on the dashboards: "Do you have concerns? Report here". */
@Composable
fun SupportPromptCard(unread: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buildAnnotatedString {
                    append("Do you have concerns? ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("Report here") }
                },
                modifier = Modifier.weight(1f),
                fontSize = 15.sp
            )
            if (unread > 0) {
                Badge { Text(unread.toString()) }
            }
        }
    }
}

/** Chat with the admin. Used by clients and by operators. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSupportScreen(viewModel: MainViewModel) {
    val user = viewModel.currentUser
    val backRoute = if (viewModel.currentUserRole == UserRole.OPERATOR) {
        Screen.OperatorTerminal.route
    } else {
        Screen.ClientDashboard.route
    }
    val leave = {
        viewModel.closeSupportChat()
        viewModel.navigateTo(backRoute)
    }

    BackHandler { leave() }
    LaunchedEffect(user?.userID) {
        user?.let { viewModel.openSupportChat(it.userID) }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.closeSupportChat() }
    }
    // Messages that arrive while the chat is open count as read.
    LaunchedEffect(viewModel.supportMessages.size, viewModel.supportThreads) {
        user?.let { viewModel.markSupportRead(it.userID) }
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = {
                    Column {
                        Text("Inbox", fontWeight = FontWeight.Bold)
                        Text("Chat with the admin", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        ChatView(
            messages = viewModel.supportMessages,
            isAdminViewer = false,
            emptyHint = "Hi ${user?.name?.substringBefore(' ') ?: "there"}! Tell us about your concern and the admin will reply here.",
            onSend = { viewModel.sendSupportMessage(it) },
            modifier = Modifier.padding(padding)
        )
    }
}

/** Admin tab: every concern, labelled by type, with a chat for each. */
@Composable
fun SupportTab(viewModel: MainViewModel) {
    val openId = viewModel.openChatClientId
    var filter by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.closeSupportChat() }
    }
    BackHandler(enabled = openId != null) { viewModel.closeSupportChat() }
    LaunchedEffect(viewModel.supportMessages.size, viewModel.supportThreads, openId) {
        openId?.let { viewModel.markSupportRead(it) }
    }

    if (openId == null) {
        val all = viewModel.supportThreads.sortedByDescending { it.lastAt }
        val threads = if (filter == null) all else all.filter { it.kind == filter }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Concerns & Messages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf<String?>(null, SupportKind.CLIENT, SupportKind.OPERATOR, SupportKind.FORGOT_PASSWORD)
                filters.forEach { option ->
                    val unread = all.count { (option == null || it.kind == option) && it.adminUnread > 0 }
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = {
                            val base = if (option == null) "All" else supportKindLabel(option)
                            Text(if (unread > 0) "$base ($unread)" else base)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (threads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No concerns here yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(threads, key = { it.clientId }) { thread ->
                        val shape = RoundedCornerShape(16.dp)
                        Card(
                            modifier = Modifier.fillMaxWidth().clip(shape).clickable { viewModel.openSupportChat(thread.clientId) },
                            shape = shape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    SupportKindChip(thread.kind)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        thread.clientName.ifBlank { thread.clientId },
                                        fontWeight = if (thread.adminUnread > 0) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Text(
                                        (if (thread.lastSenderRole == "ADMIN") "You: " else "") + thread.lastMessage.replace('\n', ' '),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(chatTime(thread.lastAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (thread.adminUnread > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Badge { Text(thread.adminUnread.toString()) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val thread = viewModel.supportThreads.find { it.clientId == openId }
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeSupportChat() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to concerns")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(thread?.clientName?.ifBlank { openId } ?: openId, fontWeight = FontWeight.Bold)
                    Text("ID: ${thread?.refUserId?.ifBlank { openId } ?: openId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (thread != null) SupportKindChip(thread.kind)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (thread != null && thread.kind == SupportKind.FORGOT_PASSWORD) {
                RecoveryCard(viewModel, thread)
            }
            HorizontalDivider()
            ChatView(
                messages = viewModel.supportMessages,
                isAdminViewer = true,
                emptyHint = "No messages yet.",
                onSend = { viewModel.sendSupportMessage(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Shown in a "Forgot Password Concern": checks the UID and email, and can issue a temporary password. */
@Composable
private fun RecoveryCard(viewModel: MainViewModel, thread: SupportThread) {
    val matches by produceState<Boolean?>(initialValue = null, thread.refUserId, thread.refEmail) {
        value = viewModel.recoveryAccountMatches(thread)
    }
    val scope = rememberCoroutineScope()
    var error by remember(thread.clientId) { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("UID: ${thread.refUserId}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
            Text("Email: ${thread.refEmail}", fontSize = 13.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                when (matches) {
                    true -> "The UID and email belong to the same account."
                    false -> "No account has this UID and email together."
                    null -> "Checking the account..."
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when (matches) {
                    true -> Color(0xFF2E7D32)
                    false -> Color(0xFFC62828)
                    null -> Color.Gray
                }
            )
            error?.let {
                Text(it, color = Color(0xFFC62828), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { confirm = true },
                enabled = matches == true,
                shape = RoundedCornerShape(12.dp)
            ) { Text("RESET PASSWORD", fontWeight = FontWeight.Bold) }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Reset password?") },
            text = { Text("A temporary password will be created and sent to this person in the chat. Their old password stops working.") },
            confirmButton = {
                Button(onClick = {
                    confirm = false
                    scope.launch { error = viewModel.resetPasswordFromRequest(thread) }
                }) { Text("YES, RESET") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("CANCEL") } }
        )
    }
}

/** "Forgot password" step 1: enter the UID and email. The request goes to the admin's Support tab. */
@Composable
fun ForgotPasswordDialog(viewModel: MainViewModel) {
    if (!viewModel.showForgotPasswordDialog || viewModel.forgotThreadId != null) return

    var uid by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.closeForgotPassword() },
        title = { Text("Forgot Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Enter your UID and Email to recover your password.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it.filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' }.take(32); viewModel.forgotError = null },
                    label = { Text("UID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = InputValidation.sanitizeEmailInput(it); viewModel.forgotError = null },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                viewModel.forgotError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.submitForgotPasswordRequest(uid, email) }) { Text("SEND TO ADMIN") }
        },
        dismissButton = { TextButton(onClick = { viewModel.closeForgotPassword() }) { Text("CANCEL") } }
    )
}

/**
 * "Forgot password" step 2: the chat with the admin, where the admin's reply appears.
 * It is drawn as part of the screen (not a pop-up window), so it always fits the phone.
 */
@Composable
fun ForgotPasswordChat(viewModel: MainViewModel) {
    if (!viewModel.showForgotPasswordDialog || viewModel.forgotThreadId == null) return

    BackHandler { viewModel.closeForgotPassword() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeForgotPassword() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Column {
                    Text("Password recovery", fontWeight = FontWeight.Bold)
                    Text("Your request was sent to the admin", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            ChatView(
                messages = viewModel.supportMessages,
                isAdminViewer = false,
                emptyHint = "Sending your request...",
                onSend = { viewModel.sendSupportMessage(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
