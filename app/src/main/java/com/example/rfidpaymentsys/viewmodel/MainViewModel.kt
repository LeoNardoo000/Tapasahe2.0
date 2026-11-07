package com.example.rfidpaymentsys.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidpaymentsys.data.entity.*
import com.example.rfidpaymentsys.AppConfig
import com.example.rfidpaymentsys.data.local.SessionStore
import com.example.rfidpaymentsys.data.model.SupportKind
import com.example.rfidpaymentsys.data.model.FareNotice
import com.example.rfidpaymentsys.data.model.FareRequest
import com.example.rfidpaymentsys.data.model.SupportMessage
import com.example.rfidpaymentsys.data.model.SupportThread
import com.example.rfidpaymentsys.data.model.WithdrawalRequest
import com.example.rfidpaymentsys.util.InputValidation
import com.example.rfidpaymentsys.data.remote.RealtimeDatabaseManager
import com.example.rfidpaymentsys.data.repository.UserRepository
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.security.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

enum class UserRole {
    NONE, ADMIN, OPERATOR, CLIENT
}

class MainViewModel(
    private val repository: UserRepository,
    private val realtimeManager: RealtimeDatabaseManager = RealtimeDatabaseManager(),
    private val sessionStore: SessionStore? = null,
) : ViewModel() {
    
    var onResponseToArduino: ((String) -> Unit)? = null
    var onConnectRequested: (() -> Unit)? = null
    var onPlaySound: ((SoundType) -> Unit)? = null

    enum class SoundType {
        SCAN, SUCCESS, ERROR
    }


    // Fare rates (set by admin, stored in Firebase). Defaults are used until the first sync.
    var regularFare by mutableStateOf(15.0)
    var studentFare by mutableStateOf(12.0)
    var fareUpdatedBy by mutableStateOf("")
    var fareUpdatedAt by mutableStateOf(0L)
    var fareRequests by mutableStateOf<List<FareRequest>>(emptyList())

    // Latest fare change (with the reason). Clients see it once after logging in.
    var fareNotice by mutableStateOf<FareNotice?>(null)
    private var seenFareNoticeAt by mutableStateOf(0L)

    /** A fare change this client has not seen yet (and that happened after they registered). */
    val pendingFareNotice: FareNotice?
        get() {
            val notice = fareNotice ?: return null
            val me = currentUser ?: return null
            if (currentUserRole != UserRole.CLIENT) return null
            return if (notice.changedAt > maxOf(seenFareNoticeAt, me.registrationDate)) notice else null
        }

    fun dismissFareNotice() {
        val notice = fareNotice ?: return
        currentUser?.let { sessionStore?.saveSeenFareNotice(it.userID, notice.changedAt) }
        seenFareNoticeAt = notice.changedAt
    }
    private var lastCloudUserIds: Set<String> = emptySet()

    // Support chat (client <-> admin)
    var supportThreads by mutableStateOf<List<SupportThread>>(emptyList())
    var supportMessages by mutableStateOf<List<SupportMessage>>(emptyList())
    var openChatClientId by mutableStateOf<String?>(null)

    // True when this device keeps the user signed in ("Remember me")
    var isSessionRemembered by mutableStateOf(false)

    // Banner shown right after login
    var welcomeMessage by mutableStateOf<String?>(null)

    // Cards tapped outside the operator terminal (registration page and card scanner)
    var cardLookup by mutableStateOf<CardLookup?>(null)
    var startInRegistration by mutableStateOf(false)

    // Operator applications and password-recovery requests
    var operatorApplicationNotice by mutableStateOf<String?>(null)
    var forgotThreadId by mutableStateOf<String?>(null)
    var forgotError by mutableStateOf<String?>(null)
    private var forgotUid = ""
    private var forgotEmail = ""
    private var forgotDisplayName = ""

    // Client withdrawals (reviewed by the admin)
    var withdrawals by mutableStateOf<List<WithdrawalRequest>>(emptyList())
    var withdrawBusy by mutableStateOf(false)
    var withdrawError by mutableStateOf<String?>(null)
    var withdrawalNotice by mutableStateOf<WithdrawalRequest?>(null)

    // Guards against attaching the Firebase listeners more than once. The
    // anonymous sign-in in RFIDPaymentApp is asynchronous, so we cannot
    // assume auth is ready the instant this ViewModel is constructed —
    // attaching a listener before auth completes gets it permanently
    // rejected with "permission denied" by our auth-required database
    // rules, and Firebase does NOT automatically retry it once auth
    // finishes. We wait for a confirmed signed-in user before attaching.
    private var firebaseListenersStarted = false

    private fun startFirebaseListenersWhenReady() {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        fun attachListeners() {
            if (firebaseListenersStarted) return
            firebaseListenersStarted = true
            attachFirebaseListeners()
        }

        if (auth.currentUser != null) {
            // Already signed in (common case: app was already open once).
            attachListeners()
        } else {
            // Not signed in yet — wait for RFIDPaymentApp's anonymous
            // sign-in to complete, then attach.
            auth.addAuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser != null) {
                    attachListeners()
                }
            }
        }
    }

    private fun attachFirebaseListeners() {
        realtimeManager.listenToUsers { cloudUsers ->
            viewModelScope.launch {
                repository.syncUsers(cloudUsers)
                // Keep the logged-in user (and their balance) fresh when it changes elsewhere,
                // and sign out if their account was deleted.
                val cloudIds = cloudUsers.map { it.userID }.toSet()
                val removedIds = lastCloudUserIds - cloudIds
                lastCloudUserIds = cloudIds
                currentUser?.let { me ->
                    val fresh = cloudUsers.find { it.userID == me.userID }
                    if (fresh != null) {
                        currentUser = fresh
                    } else if (me.userID in removedIds) {
                        logout()
                        loginError = "This account was removed."
                    }
                }
                
                if (cloudUsers.none { it.role == "ADMIN" }) {
                    val admin = UserAccount(
                        userID = "ADMIN001", 
                        name = "System Admin", 
                        role = "ADMIN", 
                        password = SecurityUtils.hashPassword("admin123")
                    )
                    realtimeManager.uploadUserAccount(admin)
                }
                if (cloudUsers.none { it.role == "OPERATOR" }) {
                    val op = UserAccount(
                        userID = "OP001", 
                        name = "Bus Driver", 
                        role = "OPERATOR", 
                        password = SecurityUtils.hashPassword("1234")
                    )
                    realtimeManager.uploadUserAccount(op)
                }
            }
        }
        
        realtimeManager.listenToCards { cloudCards ->
            viewModelScope.launch {
                repository.syncCards(cloudCards)
                val user = currentUser
                if (user != null && currentUserRole == UserRole.CLIENT) {
                    currentCard = repository.getCardByUserId(user.userID)
                }
            }
        }


        realtimeManager.listenToFareRates { regular, student, updatedBy, updatedAt ->
            regular?.let { regularFare = it }
            student?.let { studentFare = it }
            fareUpdatedBy = updatedBy
            fareUpdatedAt = updatedAt
        }
        realtimeManager.listenToFareNotice { notice -> fareNotice = notice }
        realtimeManager.listenToFareRequests { list -> fareRequests = list }
        realtimeManager.listenToWithdrawals { list -> withdrawals = list }
    }

    init {
        startFirebaseListenersWhenReady()
    }

    var isConnected by mutableStateOf(false)
    var scannedUid by mutableStateOf<String?>(null)
    var currentUser by mutableStateOf<UserAccount?>(null)
    var currentCard by mutableStateOf<RfidCard?>(null)
    var scannedUser by mutableStateOf<UserAccount?>(null)
    var paymentStatus by mutableStateOf<String?>(null)
    
    var currentUserRole by mutableStateOf(UserRole.NONE)
    var loginError by mutableStateOf<String?>(null)
    var currentScreen by mutableStateOf(Screen.Splash.route)

    private var isProcessing = false

    var adminStatus by mutableStateOf<String?>(null)
    var scannerStatus by mutableStateOf<String?>(null)
    var showChangePasswordDialog by mutableStateOf(false)
    var showForgotPasswordDialog by mutableStateOf(false)
    var showLogoutDialog by mutableStateOf(false)

    val allUsers: StateFlow<List<UserAccount>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentUsers: StateFlow<List<UserAccount>> = combine(
        repository.allUsers,
        repository.allTransactions,
        snapshotFlow { scannedUser }
    ) { users, transactions, currentScannedUser ->
        val lastTxMap = transactions
            .groupBy { it.userID }
            .mapValues { (_, txList) -> txList.maxOfOrNull { it.timestamp } ?: 0L }

        users.filter { it.userID.isNotBlank() }
            .sortedByDescending { user ->
                val isScanned = currentScannedUser?.userID == user.userID
                val scanBonus = if (isScanned) 1_000_000_000_000_000L else 0L
                val txTime = lastTxMap[user.userID] ?: 0L
                val regTime = user.registrationDate
                maxOf(txTime, regTime) + scanBonus
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getRecentTransactions(uid: String): Flow<List<Transaction>> {
        return repository.getRecentTransactionsForUser(uid)
    }

    private fun addAuditLog(action: String, details: String) {
        viewModelScope.launch {
            val log = AuditLog(
                action = action,
                details = details,
                adminUid = currentUser?.userID ?: "SYSTEM"
            )
            repository.insertAuditLog(log)
            realtimeManager.uploadAuditLog(log)
        }
    }

    fun updateConnectionStatus(connected: Boolean) { isConnected = connected }
    fun connectArduino() { onConnectRequested?.invoke() }

    private fun normalizeUid(raw: String): String {
        val upper = raw.uppercase().trim()
        if (upper.contains("FAIL") || upper.contains("ERROR") || upper.contains("TIMEOUT")) return ""

        val cleaned = upper
            .replace("CARD", "").replace("UID", "").replace("ID", "")
            .replace(":", "").replace("-", "").replace(" ", "")
        
        val hexOnly = cleaned.replace(Regex("[^0-9A-F]"), "")
        return if (hexOnly.length >= 4) hexOnly else ""
    }

    // --- ONLINE TOP-UP (CLIENT) ---

    /** Non-null while the checkout screen is open. */
    var paymentSession by mutableStateOf<PaymentSession?>(null)
    var paymentReceipt by mutableStateOf<PaymentReceipt?>(null)
    var paymentError by mutableStateOf<String?>(null)

    private val creditedReferences = mutableSetOf<String>()

    /** Opens the checkout. Returns false if the amount is outside 20..5000 pesos. */
    fun createPayment(amount: Double): Boolean {
        val user = currentUser ?: return false
        if (amount < 20.0 || amount > 5000.0) return false
        paymentReceipt = null
        paymentError = null
        paymentSession = PaymentSession(
            reference = "PAY-" + System.currentTimeMillis(),
            userId = user.userID,
            amount = amount
        )
        return true
    }

    fun cancelPayment() {
        paymentSession?.let {
            addAuditLog("PAYMENT_CANCELLED", "${it.reference}: ₱${it.amount} by ${it.userId}")
        }
        closePayment()
    }

    fun closePayment() {
        paymentSession = null
        paymentReceipt = null
        paymentError = null
    }

    /** Called by the checkout screen after the customer confirms payment. */
    fun completePayment(method: String) {
        val session = paymentSession ?: return
        if (paymentReceipt != null) return

        // Never credit the same payment twice
        if (!creditedReferences.add(session.reference)) return

        viewModelScope.launch {
            try {
                val user = repository.getUserById(session.userId)
                if (user == null) {
                    paymentError = "We couldn't find your account. Please log in again."
                    return@launch
                }
                val updatedUser = user.copy(balance = user.balance + session.amount)
                repository.updateUser(updatedUser)
                realtimeManager.updateUserBalance(user.userID, session.amount)

                val tx = Transaction(
                    userID = user.userID,
                    amount = session.amount,
                    method = "ONLINE_$method",
                    status = "COMPLETED"
                )
                val txId = repository.insertTransaction(tx)
                realtimeManager.uploadTransaction(tx.copy(transactionID = txId))

                if (currentUser?.userID == user.userID) currentUser = updatedUser
                addAuditLog("TOPUP_ONLINE", "${session.reference}: ₱${session.amount} via $method for ${user.userID}")

                paymentReceipt = PaymentReceipt(
                    reference = session.reference,
                    amount = session.amount,
                    methodLabel = paymentMethodLabel(method),
                    timestamp = System.currentTimeMillis(),
                    newBalance = updatedUser.balance
                )
            } catch (e: Exception) {
                Log.e("Payment", "Top-up failed", e)
                paymentError = "Something went wrong while processing your payment. Please try again."
            }
        }
    }

    // --- CASH OUT (OPERATOR) ---

    val minCashOut = 100.0

    var cashOutProcessing by mutableStateOf(false)
    var cashOutError by mutableStateOf<String?>(null)
    var cashOutReceipt by mutableStateOf<CashOutReceipt?>(null)

    fun clearCashOut() {
        cashOutReceipt = null
        cashOutError = null
    }

    fun requestCashOut(amount: Double, destination: String, accountNumber: String, password: String) {
        if (cashOutProcessing) return

        val operator = currentUser
        if (operator == null || currentUserRole != UserRole.OPERATOR) {
            cashOutError = "Only operators can cash out."
            return
        }
        if (amount < minCashOut) {
            cashOutError = "The minimum cash-out is ₱" + String.format(Locale.US, "%,.2f", minCashOut) + "."
            return
        }
        if (!Regex("^09\\d{9}$").matches(accountNumber)) {
            cashOutError = "Enter a valid 11-digit mobile number starting with 09."
            return
        }

        cashOutError = null
        cashOutProcessing = true

        viewModelScope.launch {
            try {
                val fresh = repository.getUserById(operator.userID)
                if (fresh == null) {
                    cashOutError = "We couldn't find your account. Please log in again."
                } else if (!SecurityUtils.verifyPassword(password, fresh.password)) {
                    cashOutError = "Incorrect password."
                } else if (amount > fresh.balance) {
                    cashOutError = "The amount is higher than your available balance."
                } else {
                    delay(1800.milliseconds)

                    // Read again so the deduction uses the latest balance
                    val latest = repository.getUserById(operator.userID) ?: fresh
                    if (amount > latest.balance) {
                        cashOutError = "The amount is higher than your available balance."
                    } else {
                        val updated = latest.copy(balance = latest.balance - amount)
                        repository.updateUser(updated)
                        realtimeManager.updateUserBalance(updated.userID, -amount)

                        val tx = Transaction(
                            userID = updated.userID,
                            amount = amount,
                            method = "CASH_OUT_$destination",
                            status = "COMPLETED"
                        )
                        val txId = repository.insertTransaction(tx)
                        realtimeManager.uploadTransaction(tx.copy(transactionID = txId))

                        currentUser = updated
                        addAuditLog("CASH_OUT", "₱$amount to $destination ${maskAccount(accountNumber)} by ${updated.userID}")

                        cashOutReceipt = CashOutReceipt(
                            reference = "CO-" + txId.toString().padStart(6, '0'),
                            amount = amount,
                            destination = paymentMethodLabel(destination),
                            maskedAccount = maskAccount(accountNumber),
                            timestamp = System.currentTimeMillis(),
                            newBalance = updated.balance
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CashOut", "Cash-out failed", e)
                cashOutError = "Something went wrong. Please try again."
            } finally {
                cashOutProcessing = false
            }
        }
    }

    private fun maskAccount(number: String): String =
        if (number.length == 11) number.take(4) + "***" + number.takeLast(4) else number

    // --- FARE RATES ---

    /** Students, PWD and Senior Citizens pay the discounted (student) fare. */
    fun fareFor(passengerType: String): Double =
        if (isDiscountedPassenger(passengerType)) studentFare else regularFare

    private fun validateFares(regular: Double, student: Double): String? = when {
        regular <= 0.0 || student <= 0.0 -> "Fares must be greater than zero."
        regular > 500.0 || student > 500.0 -> "Fares cannot be higher than ₱500."
        student > regular -> "The Students fare cannot be higher than the Regular fare."
        else -> null
    }

    /**
     * Admin sets the fares directly. A reason is required when the fares actually change,
     * because clients are shown it. Returns an error message, or null on success.
     */
    fun updateFareRates(regular: Double, student: Double, reason: String = ""): String? {
        validateFares(regular, student)?.let { return it }
        val changed = regular != regularFare || student != studentFare
        val cleanReason = cleanNoticeText(reason)
        if (changed && cleanReason.isBlank()) return "Please enter the reason for the fare change. Clients will see it."
        val admin = currentUser?.userID ?: "ADMIN"
        if (changed) publishFareNotice(regular, student, cleanReason)
        realtimeManager.uploadFareRates(regular, student, admin)
        regularFare = regular
        studentFare = student
        addAuditLog("FARE_UPDATE", "Regular ₱$regular, Students/PWD/Senior ₱$student" + if (changed) " - $cleanReason" else "")
        return null
    }

    private fun cleanNoticeText(text: String): String =
        text.filter { it == '\n' || !it.isISOControl() }.trim().take(200)

    /** Saves what changed and why, so clients can read it when they log in. */
    private fun publishFareNotice(regular: Double, student: Double, reason: String) {
        realtimeManager.uploadFareNotice(
            FareNotice(
                regular = regular,
                student = student,
                previousRegular = regularFare,
                previousStudent = studentFare,
                reason = reason,
                changedAt = System.currentTimeMillis()
            )
        )
    }

    /** Operator asks the admin to change the fares. Returns an error message, or null on success. */
    fun submitFareRequest(regular: Double, student: Double, reason: String): String? {
        val operator = currentUser
        if (operator == null || currentUserRole != UserRole.OPERATOR) return "Only operators can send requests."
        validateFares(regular, student)?.let { return it }
        if (regular == regularFare && student == studentFare) return "Those are already the current fares."
        if (fareRequests.any { it.operatorId == operator.userID && it.status == "PENDING" }) {
            return "You already have a request waiting for the admin."
        }
        realtimeManager.submitFareRequest(
            FareRequest(
                operatorId = operator.userID,
                operatorName = operator.name,
                proposedRegular = regular,
                proposedStudent = student,
                currentRegular = regularFare,
                currentStudent = studentFare,
                reason = reason.trim()
            )
        )
        addAuditLog("FARE_REQUEST", "${operator.userID} asked for Regular ₱$regular, Students ₱$student")
        return null
    }

    fun approveFareRequest(request: FareRequest): String? {
        validateFares(request.proposedRegular, request.proposedStudent)?.let { return it }
        val admin = currentUser?.userID ?: "ADMIN"
        publishFareNotice(
            request.proposedRegular,
            request.proposedStudent,
            cleanNoticeText(request.reason).ifBlank { "Fare adjustment approved by the admin." }
        )
        realtimeManager.uploadFareRates(request.proposedRegular, request.proposedStudent, admin)
        realtimeManager.updateFareRequestStatus(request.id, "APPROVED", admin)
        regularFare = request.proposedRegular
        studentFare = request.proposedStudent
        addAuditLog("FARE_APPROVED", "${request.operatorId}: Regular ₱${request.proposedRegular}, Students ₱${request.proposedStudent}")
        return null
    }

    fun rejectFareRequest(request: FareRequest) {
        val admin = currentUser?.userID ?: "ADMIN"
        realtimeManager.updateFareRequestStatus(request.id, "REJECTED", admin)
        addAuditLog("FARE_REJECTED", "Request from ${request.operatorId} rejected")
    }

    // --- CLIENT WITHDRAWALS (the admin confirms every payout) ---

    val minWithdraw = 100.0
    private val handledWithdrawals = mutableSetOf<String>()

    fun clearWithdrawalNotice() {
        withdrawalNotice = null
    }

    /**
     * The amount is taken off the balance right away (so it cannot be spent twice),
     * and the admin then confirms the payout or rejects it and the amount is returned.
     */
    fun requestWithdrawal(amount: Double, destination: String, accountNumber: String, password: String) {
        if (withdrawBusy) return
        val client = currentUser
        if (client == null || currentUserRole != UserRole.CLIENT) {
            withdrawError = "Only clients can withdraw."
            return
        }
        if (destination != "GCASH" && destination != "MAYA") {
            withdrawError = "Please choose GCash or Maya."
            return
        }
        if (amount < minWithdraw) {
            withdrawError = "The minimum withdrawal is ₱" + String.format(Locale.US, "%,.2f", minWithdraw) + "."
            return
        }
        if (!Regex("^09\\d{9}$").matches(accountNumber)) {
            withdrawError = "Enter a valid 11-digit mobile number starting with 09."
            return
        }

        withdrawError = null
        withdrawBusy = true

        viewModelScope.launch {
            try {
                val fresh = repository.getUserById(client.userID)
                if (fresh == null) {
                    withdrawError = "We couldn't find your account. Please log in again."
                } else if (!SecurityUtils.verifyPassword(password, fresh.password)) {
                    withdrawError = "Incorrect password."
                } else if (amount > fresh.balance) {
                    withdrawError = "The amount is higher than your balance."
                } else {
                    val updated = fresh.copy(balance = fresh.balance - amount)
                    repository.updateUser(updated)
                    realtimeManager.updateUserBalance(updated.userID, -amount)

                    val request = WithdrawalRequest(
                        userId = updated.userID,
                        userName = updated.name,
                        amount = amount,
                        destination = destination,
                        accountNumber = accountNumber
                    )
                    val id = realtimeManager.submitWithdrawal(request) ?: ""

                    currentUser = updated
                    addAuditLog("WITHDRAWAL_REQUEST", "₱$amount to $destination ${maskAccount(accountNumber)} by ${updated.userID}")
                    withdrawalNotice = request.copy(id = id)
                }
            } catch (e: Exception) {
                Log.e("Withdrawal", "Request failed", e)
                withdrawError = "Something went wrong. Please try again."
            } finally {
                withdrawBusy = false
            }
        }
    }

    /** Admin: the money has been sent, so mark the request as paid. */
    fun approveWithdrawal(request: WithdrawalRequest) {
        if (request.status != "PENDING" || !handledWithdrawals.add(request.id)) return
        val admin = currentUser?.userID ?: "ADMIN"
        viewModelScope.launch {
            try {
                realtimeManager.updateWithdrawalStatus(request.id, "APPROVED", admin)
                val tx = Transaction(
                    userID = request.userId,
                    amount = request.amount,
                    method = "WITHDRAW_${request.destination}",
                    status = "COMPLETED"
                )
                val txId = repository.insertTransaction(tx)
                realtimeManager.uploadTransaction(tx.copy(transactionID = txId))
                addAuditLog("WITHDRAWAL_APPROVED", "₱${request.amount} to ${request.destination} for ${request.userId}")
                adminStatus = "Withdrawal confirmed"
            } catch (e: Exception) {
                handledWithdrawals.remove(request.id)
                Log.e("Withdrawal", "Approve failed", e)
            }
        }
    }

    /** Admin: reject the request and give the amount back to the client. */
    fun rejectWithdrawal(request: WithdrawalRequest) {
        if (request.status != "PENDING" || !handledWithdrawals.add(request.id)) return
        val admin = currentUser?.userID ?: "ADMIN"
        viewModelScope.launch {
            try {
                realtimeManager.updateWithdrawalStatus(request.id, "REJECTED", admin)
                repository.getUserById(request.userId)?.let { client ->
                    repository.updateUser(client.copy(balance = client.balance + request.amount))
                }
                realtimeManager.updateUserBalance(request.userId, request.amount)
                addAuditLog("WITHDRAWAL_REJECTED", "₱${request.amount} returned to ${request.userId}")
                adminStatus = "Withdrawal rejected and refunded"
            } catch (e: Exception) {
                handledWithdrawals.remove(request.id)
                Log.e("Withdrawal", "Reject failed", e)
            }
        }
    }

    // --- SUPPORT CHAT (client <-> admin) ---

    private var threadsDisposer: (() -> Unit)? = null
    private var messagesDisposer: (() -> Unit)? = null
    private var lastSupportSendAt = 0L

    /** Starts listening for conversations (admin: all of them, client: their own). */
    fun watchSupportThreads() {
        threadsDisposer?.invoke()
        val me = currentUser
        threadsDisposer = when {
            currentUserRole == UserRole.ADMIN ->
                realtimeManager.listenToSupportThreads { supportThreads = it }
            (currentUserRole == UserRole.CLIENT || currentUserRole == UserRole.OPERATOR) && me != null ->
                realtimeManager.listenToSupportThread(me.userID) { thread -> supportThreads = listOfNotNull(thread) }
            else -> null
        }
    }

    fun stopWatchingSupportThreads() {
        threadsDisposer?.invoke()
        threadsDisposer = null
        supportThreads = emptyList()
    }

    fun openSupportChat(clientId: String) {
        messagesDisposer?.invoke()
        openChatClientId = clientId
        supportMessages = emptyList()
        messagesDisposer = realtimeManager.listenToSupportMessages(clientId) { supportMessages = it }
        markSupportRead(clientId)
    }

    fun closeSupportChat() {
        messagesDisposer?.invoke()
        messagesDisposer = null
        openChatClientId = null
        supportMessages = emptyList()
    }

    /** Clears the unread counter for whoever is looking at the chat. */
    fun markSupportRead(clientId: String) {
        val thread = supportThreads.find { it.clientId == clientId } ?: return
        val role = if (currentUserRole == UserRole.ADMIN) "ADMIN" else "CLIENT"
        val unread = if (role == "ADMIN") thread.adminUnread else thread.clientUnread
        if (unread > 0) realtimeManager.markSupportRead(clientId, role)
    }

    fun sendSupportMessage(text: String) {
        val threadId = openChatClientId ?: return
        val clean = text.filter { it == '\n' || !it.isISOControl() }.trim().take(500)
        if (clean.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - lastSupportSendAt < 400) return // stops accidental double sends
        lastSupportSendAt = now

        val me = currentUser
        when {
            currentUserRole == UserRole.ADMIN -> {
                val thread = supportThreads.find { it.clientId == threadId }
                realtimeManager.sendSupportMessage(
                    threadId = threadId,
                    kind = thread?.kind ?: SupportKind.CLIENT,
                    displayName = thread?.clientName ?: "",
                    refUserId = thread?.refUserId ?: "",
                    refEmail = thread?.refEmail ?: "",
                    senderRole = "ADMIN",
                    senderName = "Admin",
                    text = clean
                )
            }
            (currentUserRole == UserRole.CLIENT || currentUserRole == UserRole.OPERATOR) && me != null -> {
                realtimeManager.sendSupportMessage(
                    threadId = threadId,
                    kind = if (currentUserRole == UserRole.OPERATOR) SupportKind.OPERATOR else SupportKind.CLIENT,
                    displayName = me.name,
                    refUserId = me.userID,
                    refEmail = me.contactInfo,
                    senderRole = currentUserRole.name,
                    senderName = me.name,
                    text = clean
                )
            }
            threadId.startsWith("FP-") -> {
                // Someone who is not logged in, following up on their password request
                realtimeManager.sendSupportMessage(
                    threadId = threadId,
                    kind = SupportKind.FORGOT_PASSWORD,
                    displayName = forgotDisplayName,
                    refUserId = forgotUid,
                    refEmail = forgotEmail,
                    senderRole = "GUEST",
                    senderName = forgotDisplayName.ifBlank { "Guest" },
                    text = clean
                )
            }
        }
    }

    // --- CLIENT CARD CONCERNS (disable / enable a card, ask for a new one) ---

    /**
     * The client turns their own card off (lost) or back on (found).
     * Turning it back on needs the account password. Returns an error message, or null on success.
     */
    fun setMyCardEnabled(enable: Boolean, password: String = ""): String? {
        val user = currentUser
        val card = currentCard
        if (user == null || currentUserRole != UserRole.CLIENT) return "Please log in again."
        if (card == null) return "No card is linked to your account yet."
        if (card.status != "ACTIVE" && card.status != "DISABLED") {
            return "This card was blocked by the admin. Please send a concern in your Inbox."
        }
        if (enable && !SecurityUtils.verifyPassword(password, user.password)) return "Incorrect password."
        val newStatus = if (enable) "ACTIVE" else "DISABLED"
        if (card.status == newStatus) return null

        val updated = card.copy(status = newStatus)
        currentCard = updated
        viewModelScope.launch {
            repository.insertCard(updated)
            realtimeManager.uploadRfidCard(updated)
        }
        addAuditLog(
            if (enable) "CARD_ENABLED" else "CARD_DISABLED",
            "${user.userID} ${if (enable) "enabled" else "disabled"} card ${card.cardID}"
        )
        return null
    }

    /**
     * The client asks the admin for a new card. It arrives in the admin's Support tab (and the
     * client's Inbox) as a message. A lost card is switched off straight away.
     * reason is LOST, DAMAGED, NO_CARD or OTHER. Returns an error message, or null on success.
     */
    fun requestNewCard(reason: String, note: String): String? {
        val user = currentUser
        if (user == null || currentUserRole != UserRole.CLIENT) return "Please log in again."
        val now = System.currentTimeMillis()
        val last = sessionStore?.lastCardRequest(user.userID) ?: 0L
        if (now - last < 24L * 60 * 60 * 1000) {
            return "You already sent a card request today. The admin will reply in your Inbox."
        }
        val label = when (reason) {
            "LOST" -> "Lost card"
            "DAMAGED" -> "Damaged card"
            "NO_CARD" -> "I don't have a card yet"
            else -> "Other"
        }
        val cleanNote = note.filter { it == '\n' || !it.isISOControl() }.trim().take(250)
        val text = ("CARD REQUEST - $label" + if (cleanNote.isNotEmpty()) ": $cleanNote" else "").take(500)

        realtimeManager.sendSupportMessage(
            threadId = user.userID,
            kind = SupportKind.CLIENT,
            displayName = user.name,
            refUserId = user.userID,
            refEmail = user.contactInfo,
            senderRole = "CLIENT",
            senderName = user.name,
            text = text
        )
        sessionStore?.saveCardRequest(user.userID, now)
        if (reason == "LOST" && currentCard?.status == "ACTIVE") setMyCardEnabled(false)
        addAuditLog("CARD_REQUEST", "${user.userID} requested a new card ($label)")
        return null
    }

    // --- FORGOT PASSWORD (goes to the admin's Support tab) ---

    fun openForgotPassword() {
        forgotThreadId = null
        forgotError = null
        showForgotPasswordDialog = true
    }

    fun closeForgotPassword() {
        showForgotPasswordDialog = false
        if (forgotThreadId != null) closeSupportChat()
        forgotThreadId = null
        forgotError = null
    }

    /** Sends "I forgot my password" to the admin with the UID and email the person typed. */
    fun submitForgotPasswordRequest(uid: String, email: String) {
        val cleanUid = uid.trim()
        val cleanEmail = InputValidation.normalizeEmail(email)
        val threadId = "FP-" + cleanUid.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        val problem = (if (threadId.length <= 3) "Please enter your UID." else null)
            ?: InputValidation.emailError(cleanEmail)
        if (problem != null) {
            forgotError = problem
            return
        }
        forgotError = null

        viewModelScope.launch {
            val user = repository.getUserById(cleanUid)
            forgotUid = cleanUid
            forgotEmail = cleanEmail
            forgotDisplayName = user?.name ?: "Unknown account"
            realtimeManager.sendSupportMessage(
                threadId = threadId,
                kind = SupportKind.FORGOT_PASSWORD,
                displayName = forgotDisplayName,
                refUserId = cleanUid,
                refEmail = cleanEmail,
                senderRole = "GUEST",
                senderName = forgotDisplayName.ifBlank { "Guest" },
                text = "I forgot my password.\nUID: $cleanUid\nEmail: $cleanEmail"
            )
            // Show the admin's replies right here
            messagesDisposer?.invoke()
            openChatClientId = threadId
            supportMessages = emptyList()
            messagesDisposer = realtimeManager.listenToSupportMessages(threadId) { supportMessages = it }
            forgotThreadId = threadId
        }
    }

    /** Admin: does the UID and email in this request belong to a real account? */
    suspend fun recoveryAccountMatches(thread: SupportThread): Boolean {
        val user = repository.getUserById(thread.refUserId) ?: return false
        return user.contactInfo.trim().equals(thread.refEmail.trim(), ignoreCase = true)
    }

    private fun generateTempPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
        val random = java.security.SecureRandom()
        return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    /** Admin: gives the account a temporary password and sends it in the chat. Returns an error message, or null. */
    suspend fun resetPasswordFromRequest(thread: SupportThread): String? {
        val user = repository.getUserById(thread.refUserId) ?: return "No account was found with that UID."
        if (!user.contactInfo.trim().equals(thread.refEmail.trim(), ignoreCase = true)) {
            return "The email does not match this account."
        }
        if (user.role == "ADMIN") return "Admin passwords can't be reset from a request."

        val temporary = generateTempPassword()
        val updated = user.copy(password = SecurityUtils.hashPassword(temporary))
        repository.updateUser(updated)
        realtimeManager.uploadUserAccount(updated)
        addAuditLog("PASSWORD_RESET", "Temporary password issued for ${user.userID}")
        realtimeManager.sendSupportMessage(
            threadId = thread.clientId,
            kind = thread.kind,
            displayName = thread.clientName,
            refUserId = thread.refUserId,
            refEmail = thread.refEmail,
            senderRole = "ADMIN",
            senderName = "Admin",
            text = "Your password was reset.\nTemporary password: $temporary\nPlease log in and change it right away."
        )
        return null
    }

    // --- AUTH LOGIC ---

    private fun greet(user: UserAccount) {
        welcomeMessage = "Welcome back, ${user.name}!"
    }

    fun clearWelcome() {
        welcomeMessage = null
    }

    /** Signs a client or operator in: opens their screen, starts chat updates and shows the welcome banner. */
    private suspend fun startSession(user: UserAccount, role: UserRole, remember: Boolean, loginName: String) {
        currentUserRole = role
        currentUser = user
        adminStatus = null
        scannerStatus = null
        cardLookup = null
        when (role) {
            UserRole.OPERATOR -> currentScreen = Screen.OperatorTerminal.route
            UserRole.CLIENT -> {
                currentCard = repository.getCardByUserId(user.userID)
                seenFareNoticeAt = sessionStore?.seenFareNotice(user.userID) ?: 0L
                currentScreen = Screen.ClientDashboard.route
            }
            else -> {}
        }
        isSessionRemembered = remember
        if (remember) {
            sessionStore?.save(user.userID, user.role)
            sessionStore?.saveLastLogin(role.name, loginName)
        } else {
            sessionStore?.clear()
            sessionStore?.clearLastLogin(role.name)
        }
        watchSupportThreads()
        greet(user)
    }

    fun adminLogin(password: String) {
        viewModelScope.launch {
            val admin = repository.getUserById("ADMIN001")
            if (admin != null && SecurityUtils.verifyPassword(password, admin.password)) {
                currentUserRole = UserRole.ADMIN
                currentUser = admin
                currentScreen = Screen.AdminPanel.route
                adminStatus = null
                watchSupportThreads()
                greet(admin)
                return@launch
            }
            loginError = "Invalid Admin Credentials"
        }
    }

    /** Operators log in with the username and password they chose when they registered. */
    fun operatorLogin(username: String, password: String, remember: Boolean = false) {
        val typed = username.trim()
        viewModelScope.launch {
            val user = repository.getUserByUsername(InputValidation.normalizeUsername(typed))
                ?: repository.getUserById(typed) // older accounts that have no username yet
            if (user == null || user.role != "OPERATOR" || !SecurityUtils.verifyPassword(password, user.password)) {
                loginError = "Invalid username or password"
                return@launch
            }
            when (user.accountStatus) {
                "PENDING" -> {
                    loginError = "Your application as Operator is still being reviewed."
                    return@launch
                }
                "REJECTED" -> {
                    loginError = "Your operator application was not approved."
                    return@launch
                }
            }
            startSession(user, UserRole.OPERATOR, remember, typed)
        }
    }

    private suspend fun generateOperatorId(): String {
        repeat(20) {
            val candidate = "OP" + (100000..999999).random()
            if (repository.getUserById(candidate) == null) return candidate
        }
        return "OP" + System.currentTimeMillis()
    }

    /** An operator applies for an account. The admin must approve it before they can log in. */
    fun operatorRegister(name: String, username: String, contact: String, password: String, confirmPassword: String) {
        val cleanName = InputValidation.normalizeName(name)
        val cleanUsername = InputValidation.normalizeUsername(username)
        val problem = InputValidation.nameError(cleanName)
            ?: InputValidation.usernameError(cleanUsername)
            ?: InputValidation.contactError(contact, required = true)
            ?: InputValidation.passwordError(password)
            ?: if (password != confirmPassword) "Passwords do not match." else null
        if (problem != null) {
            loginError = problem
            return
        }

        viewModelScope.launch {
            if (repository.getUserByUsername(cleanUsername) != null) {
                loginError = "That username is already taken."
                return@launch
            }
            val cleanContact = contact.trim().let {
                if (InputValidation.isValidEmail(it)) InputValidation.normalizeEmail(it) else it
            }
            val applicant = UserAccount(
                userID = generateOperatorId(),
                name = cleanName,
                username = cleanUsername,
                password = SecurityUtils.hashPassword(password),
                role = "OPERATOR",
                contactInfo = cleanContact,
                accountStatus = "PENDING"
            )
            repository.insertUser(applicant)
            realtimeManager.uploadUserAccount(applicant)
            loginError = null
            operatorApplicationNotice = "Your application as Operator is being reviewed."
        }
    }

    fun dismissOperatorNotice() {
        operatorApplicationNotice = null
    }

    fun approveOperator(user: UserAccount) = setOperatorStatus(user, "ACTIVE", "OPERATOR_APPROVED")

    fun rejectOperator(user: UserAccount) = setOperatorStatus(user, "REJECTED", "OPERATOR_REJECTED")

    private fun setOperatorStatus(user: UserAccount, status: String, action: String) {
        viewModelScope.launch {
            val updated = user.copy(accountStatus = status)
            repository.updateUser(updated)
            realtimeManager.uploadUserAccount(updated)
            addAuditLog(action, "Operator ${user.userID} (${user.name})")
            adminStatus = if (status == "ACTIVE") "Operator approved" else "Operator application rejected"
        }
    }

    /** Clients log in with their username and password. */
    fun clientLogin(identifier: String, password: String, remember: Boolean = false) {
        val typed = identifier.trim()
        viewModelScope.launch {
            val user = repository.getUserByUsername(InputValidation.normalizeUsername(typed))
                ?: (if (typed.contains("@")) repository.getUserByContact(InputValidation.normalizeEmail(typed)) else null)
                ?: repository.getUserByName(typed) // older accounts that have no username yet
            if (user != null && user.role == "CLIENT" && SecurityUtils.verifyPassword(password, user.password)) {
                startSession(user, UserRole.CLIENT, remember, typed)
            } else {
                loginError = "Invalid username or password"
            }
        }
    }

    fun clientRegister(
        uid: String,
        name: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        passengerType: String = PassengerType.REGULAR
    ) {
        val cleanName = InputValidation.normalizeName(name)
        val cleanUsername = InputValidation.normalizeUsername(username)
        val cleanEmail = InputValidation.normalizeEmail(email)
        val problem = (if (uid.isBlank()) "Please scan your card first." else null)
            ?: InputValidation.nameError(cleanName)
            ?: InputValidation.usernameError(cleanUsername)
            ?: InputValidation.emailError(cleanEmail)
            ?: InputValidation.passwordError(password)
            ?: if (password != confirmPassword) "Passwords do not match." else null
        if (problem != null) {
            loginError = problem
            return
        }

        viewModelScope.launch {
            val cleanUid = normalizeUid(uid)
            if (cleanUid.isBlank()) {
                loginError = "Please scan your card again."
                return@launch
            }

            // Make sure we know about cards registered on other devices
            try {
                repository.syncCards(realtimeManager.fetchAllCards())
            } catch (_: Exception) {
            }

            val existingCard = repository.getCardById(cleanUid)
            if (existingCard != null) {
                val owner = repository.getUserById(existingCard.userID)
                if (owner != null) {
                    loginError = "This Card is Registered to ${owner.name}"
                    return@launch
                }
                // Left over from a deleted account: free it so the card can be used again
                repository.deleteCard(existingCard.cardID)
                realtimeManager.deleteCard(existingCard.cardID)
            }
            if (repository.getUserById(cleanUid) != null) {
                loginError = "This card is already registered."
                return@launch
            }
            if (repository.getUserByUsername(cleanUsername) != null) {
                loginError = "That username is already taken."
                return@launch
            }
            if (repository.getUserByContact(cleanEmail) != null) {
                loginError = "This email is already registered."
                return@launch
            }

            val newUser = UserAccount(
                userID = cleanUid,
                name = cleanName,
                username = cleanUsername,
                password = SecurityUtils.hashPassword(password),
                role = "CLIENT",
                balance = AppConfig.NEW_ACCOUNT_BALANCE,
                contactInfo = cleanEmail,
                passengerType = passengerType
            )
            val newCard = RfidCard(cardID = cleanUid, userID = cleanUid)

            repository.insertUser(newUser)
            repository.insertCard(newCard)
            realtimeManager.uploadUserAccount(newUser)
            realtimeManager.uploadRfidCard(newCard)
            if (AppConfig.NEW_ACCOUNT_BALANCE > 0) {
                repository.insertTransaction(Transaction(userID = cleanUid, amount = AppConfig.NEW_ACCOUNT_BALANCE, method = "DEPOSIT"))
            }
            cardLookup = null
            loginError = "Registered! You can now login with your username."
        }
    }

    /** The username this device used last time, so the login box can be pre-filled. */
    fun lastLoginName(role: String): String = sessionStore?.lastLogin(role) ?: ""

    /** Called when the splash screen ends: signs the user back in if this device remembers them. */
    fun finishSplash() {
        val savedId = sessionStore?.savedUserId()
        val savedRole = sessionStore?.savedRole()
        val role = when (savedRole) {
            "OPERATOR" -> UserRole.OPERATOR
            "CLIENT" -> UserRole.CLIENT
            else -> null
        }
        if (savedId == null || role == null) {
            navigateTo(Screen.Welcome.route)
            return
        }
        viewModelScope.launch {
            // Give the local database a moment to fill in if the app has only just started
            var user = repository.getUserById(savedId)
            var attempts = 0
            while (user == null && attempts < 8) {
                delay(500.milliseconds)
                user = repository.getUserById(savedId)
                attempts++
            }
            if (user != null && user.role == savedRole && user.accountStatus == "ACTIVE") {
                startSession(user, role, remember = true, loginName = user.username.ifBlank { user.userID })
            } else {
                sessionStore?.clear()
                navigateTo(Screen.Welcome.route)
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        val user = currentUser ?: return
        InputValidation.passwordError(newPass)?.let {
            loginError = it
            return
        }
        if (user.role != "ADMIN" && !SecurityUtils.verifyPassword(oldPass, user.password)) {
            loginError = "Incorrect current password"
            return
        }
        viewModelScope.launch {
            val updatedUser = user.copy(password = SecurityUtils.hashPassword(newPass))
            repository.updateUser(updatedUser)
            realtimeManager.uploadUserAccount(updatedUser)
            currentUser = updatedUser
            loginError = "Password updated successfully"
            showChangePasswordDialog = false
        }
    }

    fun logout() {
        stopWatchingSupportThreads()
        closeSupportChat()
        currentUserRole = UserRole.NONE
        currentScreen = Screen.Welcome.route
        currentUser = null
        currentCard = null
        scannedUid = null
        scannedUser = null
        paymentStatus = null
        loginError = null
        adminStatus = null
        scannerStatus = null
        welcomeMessage = null
        cardLookup = null
        isSessionRemembered = false
        sessionStore?.clear()
    }

    // --- RFID LOGIC ---

    /** A card was tapped outside the operator terminal: remember its ID and who owns it (if anyone). */
    private fun lookUpCard(data: String) {
        val uid = normalizeUid(data)
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                var card = repository.getCardById(uid)
                if (card == null) {
                    repository.syncCards(realtimeManager.fetchAllCards())
                    card = repository.getCardById(uid)
                }
                val owner = card?.let { repository.getUserById(it.userID) }
                if (card != null && owner == null) {
                    // Left over from a deleted account: free it so it can be registered again
                    repository.deleteCard(card.cardID)
                    realtimeManager.deleteCard(card.cardID)
                }
                cardLookup = CardLookup(uid = uid, ownerName = owner?.name)
                onPlaySound?.invoke(SoundType.SCAN)
            } catch (e: Exception) {
                Log.e("Scanner", "Card lookup failed", e)
            }
        }
    }

    fun clearCardLookup() {
        cardLookup = null
    }

    fun onUidScanned(data: String) {
        if (currentUserRole != UserRole.OPERATOR) {
            lookUpCard(data)
            return
        }
        if (isProcessing) return

        val uid = normalizeUid(data)
        if (uid.isBlank()) {
            if (data.isNotBlank()) {
                paymentStatus = "Invalid Scan"
                onPlaySound?.invoke(SoundType.ERROR)
            }
            return
        }

        isProcessing = true
        scannedUid = uid
        onPlaySound?.invoke(SoundType.SCAN)
        
        viewModelScope.launch {
            try {
                var card = repository.getCardById(uid)
                if (card == null) {
                    val cloudCards = realtimeManager.fetchAllCards()
                    repository.syncCards(cloudCards)
                    card = repository.getCardById(uid)
                }

                if (card != null && repository.getUserById(card.userID) == null) {
                    // The passenger's account was deleted but the card record was left behind
                    repository.deleteCard(card.cardID)
                    realtimeManager.deleteCard(card.cardID)
                    card = null
                }

                if (card != null) {
                    scannedUser = repository.getUserById(card.userID)
                    paymentStatus = "Processing..."
                    delay(1000.milliseconds)
                    
                    if (card.status == "ACTIVE") {
                        processDeduction(card)
                    } else { 
                        paymentStatus = "Card Blocked"
                        logValidation(null, "DECLINED_BLOCKED")
                        onPlaySound?.invoke(SoundType.ERROR)
                        isProcessing = false 
                    }
                } else { 
                    paymentStatus = "Card is not registered"
                    scannedUser = null
                    onPlaySound?.invoke(SoundType.ERROR)
                    logValidation(null, "DECLINED_UNREGISTERED")
                    isProcessing = false
                }
            } catch (e: Exception) { 
                paymentStatus = "System Error"
                Log.e("Scanner", "Processing failure", e)
                isProcessing = false 
            }
        }
    }

    private suspend fun processDeduction(card: RfidCard) {
        val user = repository.getUserById(card.userID)
        val amount = fareFor(user?.passengerType ?: PassengerType.REGULAR)
        
        if (user == null || user.balance < amount) {
            paymentStatus = "Insufficient Funds"
            onResponseToArduino?.invoke("DECLINED\n")
            logValidation(null, "DECLINED_FUNDS")
            onPlaySound?.invoke(SoundType.ERROR)
            isProcessing = false
            return
        }
        
        val userName = user.name
        val newBalance = user.balance - amount
        val newUser = user.copy(balance = newBalance)
        
        val opId = currentUser?.userID ?: ""
        val transaction = Transaction(userID = user.userID, driverID = opId, amount = amount)
        val txId = repository.insertTransaction(transaction)
        val transactionWithId = transaction.copy(transactionID = txId)
        
        repository.updateUser(newUser)
        realtimeManager.updateUserBalance(user.userID, -amount)
        
        if (opId.isNotBlank() && currentUserRole == UserRole.OPERATOR) {
            val operator = currentUser
            if (operator != null) {
                val updatedOperator = operator.copy(balance = operator.balance + amount)
                repository.updateUser(updatedOperator)
                realtimeManager.updateUserBalance(opId, amount)
                currentUser = updatedOperator
            }
        }
        
        realtimeManager.uploadTransaction(transactionWithId)
        logValidation(txId, "APPROVED")
        
        val lowBalanceMsg = if (newBalance < regularFare) "\nCRITICAL: Reload now!"
                           else if (newBalance < regularFare * 3) "\nWarning: Low Balance"
                           else ""

        paymentStatus = "Paid: ₱${String.format(Locale.US, "%.2f", amount)} ($userName - ${passengerTypeLabel(user.passengerType)})$lowBalanceMsg"
        onResponseToArduino?.invoke("APPROVED\n")
        onPlaySound?.invoke(SoundType.SUCCESS)
        
        delay(2000.milliseconds)
        isProcessing = false
    }

    private fun logValidation(txId: Long?, result: String) {
        viewModelScope.launch {
            val log = ValidationLog(transactionID = txId, validatorID = currentUser?.userID ?: "SYS", result = result)
            repository.insertValidationLog(log)
            realtimeManager.uploadValidationLog(log)
        }
    }

    fun topUpBalance(userId: String, amount: Double) {
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            val updatedUser = user.copy(balance = user.balance + amount)
            repository.updateUser(updatedUser)
            realtimeManager.updateUserBalance(user.userID, amount)
            
            repository.insertTransaction(Transaction(userID = userId, amount = amount, method = "TOP_UP"))
            adminStatus = "Topped up ₱$amount for $userId"
            addAuditLog("BALANCE_EDIT", "Added ₱$amount to $userId")
        }
    }

    fun resetTransactionsKeepProfit() {
        viewModelScope.launch {
            repository.deleteNonProfitTransactions()
            adminStatus = "Logs reset"
            addAuditLog("LOG_RESET", "Deleted non-profit transaction logs")
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            adminStatus = "All logs cleared"
            addAuditLog("LOG_CLEAR", "Permanently deleted ALL transaction logs")
        }
    }

    fun updateUserFully(updatedUser: UserAccount) {
        viewModelScope.launch {
            if (updatedUser.username.isNotBlank()) {
                val other = repository.getUserByUsername(updatedUser.username)
                if (other != null && other.userID != updatedUser.userID) {
                    adminStatus = "That username is already taken."
                    return@launch
                }
            }
            // Hash the password if it's updated (assuming updatedUser.password is already hashed or new plain text)
            // Actually, in the UI, if password changes, it's plain text.
            // Let's assume we hash it if it doesn't look like a hash.
            val finalUser = if (updatedUser.password.startsWith("$2a$")) {
                updatedUser
            } else {
                updatedUser.copy(password = SecurityUtils.hashPassword(updatedUser.password))
            }
            
            repository.updateUser(finalUser)
            realtimeManager.uploadUserAccount(finalUser)
            adminStatus = "Updated user ${finalUser.userID}"
            addAuditLog("USER_EDIT", "Modified user ${finalUser.userID}")
        }
    }

    /**
     * Deletes the account and its card here and in Firebase.
     * The money records are kept (for the operators' income), but detached from the old UID,
     * so the same card can be registered again later without inheriting the old history.
     */
    fun deleteUser(user: UserAccount) {
        if (user.userID == currentUser?.userID) {
            adminStatus = "You can't delete the account you are logged in with."
            return
        }
        if (withdrawals.any { it.userId == user.userID && it.status == "PENDING" }) {
            adminStatus = "Please confirm or reject this user's pending withdrawal first."
            return
        }
        viewModelScope.launch {
            val card = repository.getCardByUserId(user.userID)
            val archivedId = "${user.userID}~deleted${System.currentTimeMillis()}"

            repository.reassignTransactions(user.userID, archivedId)
            withdrawals.filter { it.userId == user.userID }.forEach {
                realtimeManager.updateWithdrawalOwner(it.id, archivedId)
            }

            // Firebase first, so a sync in between cannot bring the account back
            realtimeManager.deleteUser(user.userID)
            realtimeManager.deleteSupportChat(user.userID)
            card?.let { realtimeManager.deleteCard(it.cardID) }
            repository.deleteUser(user)
            card?.let { repository.deleteCard(it.cardID) }
            addAuditLog("USER_DELETE", "Deleted user ${user.userID}")
        }
    }

    fun navigateTo(screen: String) {
        currentScreen = screen
        loginError = null
        if (screen == Screen.Welcome.route) {
            cardLookup = null
            startInRegistration = false
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            try {
                val users = allUsers.value
                val transactions = allTransactions.value
                val dir = context.getExternalFilesDir(null)
                val file = File(dir, "export_${System.currentTimeMillis()}.csv")
                FileWriter(file).use { writer ->
                    writer.append("USERS\nID,Name,Role\n")
                    users.forEach { writer.append("${it.userID},${it.name},${it.role}\n") }
                    writer.append("\nTRANSACTIONS\nID,UserID,Amount,Method,Time\n")
                    transactions.forEach { writer.append("${it.transactionID},${it.userID},${it.amount},${it.method},${it.timestamp}\n") }
                }
                adminStatus = "Exported to: ${file.absolutePath}"
            } catch (_: Exception) {
                adminStatus = "Export failed" 
            }
        }
    }

    fun refreshFromServer() {
        viewModelScope.launch {
            adminStatus = "Syncing..."
            adminStatus = "Sync complete"
        }
    }

    fun addUser(
        uid: String,
        name: String,
        balance: Double,
        role: String,
        pass: String,
        email: String = "",
        phone: String = "",
        passengerType: String = PassengerType.REGULAR,
        username: String = ""
    ) {
        viewModelScope.launch {
            val cleanUid = normalizeUid(uid)
            val cleanUsername = InputValidation.normalizeUsername(username)
            val userId = when {
                cleanUid.isNotBlank() -> cleanUid
                role == "OPERATOR" -> generateOperatorId()
                else -> "user_" + (1000..9999).random()
            }

            if (repository.getUserById(userId) != null) {
                adminStatus = "That UID is already in use."
                return@launch
            }
            if (cleanUsername.isNotBlank() && repository.getUserByUsername(cleanUsername) != null) {
                adminStatus = "That username is already taken."
                return@launch
            }
            // A card left behind by a deleted account can be used again
            repository.getCardById(userId)?.let { leftover ->
                val owner = repository.getUserById(leftover.userID)
                if (owner != null) {
                    adminStatus = "That card is registered to ${owner.name}."
                    return@launch
                }
                repository.deleteCard(leftover.cardID)
                realtimeManager.deleteCard(leftover.cardID)
            }

            val newUser = UserAccount(
                userID = userId,
                name = name,
                username = cleanUsername,
                role = role,
                password = SecurityUtils.hashPassword(pass),
                contactInfo = if (email.isNotBlank()) email else phone,
                balance = balance,
                passengerType = passengerType
            )
            repository.insertUser(newUser)
            realtimeManager.uploadUserAccount(newUser)

            if (role == "CLIENT" || cleanUid.isNotBlank()) {
                val newCard = RfidCard(cardID = if (cleanUid.isNotBlank()) cleanUid else userId, userID = userId)
                repository.insertCard(newCard)
                realtimeManager.uploadRfidCard(newCard)

                if (balance > 0) {
                    repository.insertTransaction(Transaction(userID = userId, amount = balance, method = "DEPOSIT"))
                }
            }
            adminStatus = "User Added"
        }
    }

    fun clearStatus() { 
        paymentStatus = null
        scannedUser = null
        isProcessing = false 
    }

    fun generateDailyReports() {
        viewModelScope.launch {
            val txs = allTransactions.value.filter { it.method == "RFID" }
            txs.groupBy { it.driverID }.forEach { (driver, list) ->
                val report = IncomeReport(driverID = driver, totalAmount = list.sumOf { it.amount })
                repository.insertIncomeReport(report)
                realtimeManager.uploadIncomeReport(report)
            }
            adminStatus = "Reports Generated"
            addAuditLog("REPORT_GEN", "Generated daily income reports")
        }
    }

    fun getWeeklyProfit(driverId: String? = null): List<Pair<String, Double>> {
        val result = mutableListOf<Pair<String, Double>>()
        val transactions = allTransactions.value.filter { it.method == "RFID" && (driverId == null || it.driverID == driverId) }
        
        for (i in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val end = calendar.timeInMillis
            
            val profit = transactions.filter { it.timestamp in start..end }.sumOf { it.amount }
            result.add(SimpleDateFormat("EEE", Locale.US).format(calendar.time) to profit)
        }
        return result.reversed()
    }

    fun getProfitHistory(driverId: String): List<Pair<String, Double>> {
        val transactions = allTransactions.value.filter { it.driverID == driverId && it.method == "RFID" }
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        
        return transactions.groupBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            dateFormat.format(cal.time)
        }.map { (date, txs) -> 
            val maxTs = txs.maxOf { it.timestamp }
            Triple(date, txs.sumOf { it.amount }, maxTs)
        }
        .sortedByDescending { it.third }
        .map { it.first to it.second }
    }

    fun getDailyProfit(driverId: String): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        return allTransactions.value
            .filter { it.driverID == driverId && it.method == "RFID" && it.timestamp >= todayStart }
            .sumOf { it.amount }
    }
}
