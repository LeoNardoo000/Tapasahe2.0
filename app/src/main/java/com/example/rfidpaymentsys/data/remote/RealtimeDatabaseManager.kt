package com.example.rfidpaymentsys.data.remote

import android.util.Log
import com.example.rfidpaymentsys.data.entity.UserAccount
import com.example.rfidpaymentsys.data.entity.RfidCard
import com.example.rfidpaymentsys.data.entity.Transaction as AppTransaction
import com.example.rfidpaymentsys.data.entity.ValidationLog
import com.example.rfidpaymentsys.data.entity.IncomeReport
import com.example.rfidpaymentsys.data.entity.AuditLog
import com.example.rfidpaymentsys.data.model.FareNotice
import com.example.rfidpaymentsys.data.model.FareRequest
import com.example.rfidpaymentsys.data.model.SupportMessage
import com.example.rfidpaymentsys.data.model.SupportThread
import com.example.rfidpaymentsys.data.model.WithdrawalRequest
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

class RealtimeDatabaseManager {
    
    companion object {
        private var isPersistenceSet = false
    }

    private val db: FirebaseDatabase by lazy {
        val instance = FirebaseDatabase.getInstance("https://rfidpaymentsys-default-rtdb.asia-southeast1.firebasedatabase.app/")
        if (!isPersistenceSet) {
            try {
                instance.setPersistenceEnabled(true)
                isPersistenceSet = true
            } catch (e: Exception) {
                Log.w("FirebaseDB", "Persistence error: ${e.message}")
            }
        }
        instance
    }
    
    private val rootRef by lazy { db.reference }

    fun uploadUserAccount(user: UserAccount) {
        val values = mapOf(
            "UserID" to user.userID,
            "Name" to user.name,
            "Role" to user.role,
            "ContactInfo" to user.contactInfo,
            "RegistrationDate" to user.registrationDate,
            "Password" to user.password,
            "Balance" to user.balance,
            "PassengerType" to user.passengerType,
            "Username" to user.username,
            "AccountStatus" to user.accountStatus
        )
        rootRef.child("Users").child(user.userID).setValue(values)
    }

    fun uploadRfidCard(card: RfidCard) {
        val values = mapOf(
            "CardID" to card.cardID,
            "UserID" to card.userID,
            "LastReload" to card.lastReload,
            "Status" to card.status
        )
        rootRef.child("Cards").child(card.cardID).setValue(values)
    }

    fun deleteUser(userId: String) {
        rootRef.child("Users").child(userId).removeValue()
    }

    fun deleteCard(cardId: String) {
        rootRef.child("Cards").child(cardId).removeValue()
    }

    fun updateUserBalance(userID: String, delta: Double) {
        rootRef.child("Users").child(userID).child("Balance").setValue(ServerValue.increment(delta))
    }

    fun uploadTransaction(transaction: AppTransaction) {
        val values = mapOf(
            "TransactionID" to transaction.transactionID,
            "UserID" to transaction.userID,
            "DriverID" to transaction.driverID,
            "Amount" to transaction.amount,
            "Method" to transaction.method,
            "Timestamp" to transaction.timestamp,
            "Status" to transaction.status,
            "SyncFlag" to transaction.syncFlag
        )
        rootRef.child("Transactions").child(transaction.transactionID.toString()).setValue(values)
    }

    fun uploadValidationLog(log: ValidationLog) {
        val values = mapOf(
            "ValidationID" to log.validationID,
            "TransactionID" to log.transactionID,
            "ValidatorID" to log.validatorID,
            "Result" to log.result,
            "Timestamp" to log.timestamp
        )
        val key = if (log.validationID != 0) log.validationID.toString() else rootRef.child("ValidationLogs").push().key
        key?.let { rootRef.child("ValidationLogs").child(it).setValue(values) }
    }

    fun uploadIncomeReport(report: IncomeReport) {
        val values = mapOf(
            "ReportID" to report.reportID,
            "DriverID" to report.driverID,
            "TotalAmount" to report.totalAmount,
            "DateGenerated" to report.dateGenerated,
            "Period" to report.period
        )
        val key = if (report.reportID != 0) report.reportID.toString() else rootRef.child("IncomeReports").push().key
        key?.let { rootRef.child("IncomeReports").child(it).setValue(values) }
    }

    fun uploadAuditLog(log: AuditLog) {
        val values = mapOf(
            "LogID" to log.id,
            "Action" to log.action,
            "Details" to log.details,
            "AdminUid" to log.adminUid,
            "Timestamp" to log.timestamp
        )
        rootRef.child("AuditLogs").push().setValue(values)
    }

    // ---------- SUPPORT CHAT ----------

    fun sendSupportMessage(
        threadId: String,
        kind: String,
        displayName: String,
        refUserId: String,
        refEmail: String,
        senderRole: String,
        senderName: String,
        text: String
    ) {
        val now = System.currentTimeMillis()
        rootRef.child("SupportMessages").child(threadId).push().setValue(
            mapOf(
                "SenderRole" to senderRole,
                "SenderName" to senderName,
                "Text" to text,
                "At" to now
            )
        )
        val updates = mapOf<String, Any>(
            "ClientName" to displayName,
            "Kind" to kind,
            "RefUserId" to refUserId,
            "RefEmail" to refEmail,
            "LastMessage" to text.take(80),
            "LastAt" to now,
            "LastSenderRole" to senderRole,
            (if (senderRole == "ADMIN") "ClientUnread" else "AdminUnread") to ServerValue.increment(1)
        )
        rootRef.child("SupportThreads").child(threadId).updateChildren(updates)
    }

    fun markSupportRead(clientId: String, role: String) {
        val field = if (role == "ADMIN") "AdminUnread" else "ClientUnread"
        rootRef.child("SupportThreads").child(clientId).child(field).setValue(0)
    }

    fun deleteSupportChat(clientId: String) {
        rootRef.child("SupportMessages").child(clientId).removeValue()
        rootRef.child("SupportThreads").child(clientId).removeValue()
    }

    private fun parseThread(key: String, node: DataSnapshot) = SupportThread(
        clientId = key,
        clientName = node.child("ClientName").value?.toString() ?: "",
        kind = node.child("Kind").value?.toString() ?: "CLIENT",
        refUserId = node.child("RefUserId").value?.toString() ?: "",
        refEmail = node.child("RefEmail").value?.toString() ?: "",
        lastMessage = node.child("LastMessage").value?.toString() ?: "",
        lastAt = (node.child("LastAt").value as? Number)?.toLong() ?: 0L,
        lastSenderRole = node.child("LastSenderRole").value?.toString() ?: "",
        adminUnread = (node.child("AdminUnread").value as? Number)?.toInt() ?: 0,
        clientUnread = (node.child("ClientUnread").value as? Number)?.toInt() ?: 0
    )

    /** Admin: all conversations. Returns a function that stops listening. */
    fun listenToSupportThreads(onUpdate: (List<SupportThread>) -> Unit): () -> Unit {
        val ref = rootRef.child("SupportThreads")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.key?.let { parseThread(it, child) }
                }
                onUpdate(list.sortedByDescending { it.lastAt })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /** Client: only their own conversation. Returns a function that stops listening. */
    fun listenToSupportThread(clientId: String, onUpdate: (SupportThread?) -> Unit): () -> Unit {
        val ref = rootRef.child("SupportThreads").child(clientId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(if (snapshot.exists()) parseThread(clientId, snapshot) else null)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    fun listenToSupportMessages(clientId: String, onUpdate: (List<SupportMessage>) -> Unit): () -> Unit {
        val ref = rootRef.child("SupportMessages").child(clientId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    SupportMessage(
                        id = id,
                        senderRole = child.child("SenderRole").value?.toString() ?: "CLIENT",
                        senderName = child.child("SenderName").value?.toString() ?: "",
                        text = child.child("Text").value?.toString() ?: "",
                        timestamp = (child.child("At").value as? Number)?.toLong() ?: 0L
                    )
                }
                onUpdate(list.sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    // ---------- CLIENT WITHDRAWALS ----------

    /** Saves a new withdrawal request and returns its id. */
    fun submitWithdrawal(request: WithdrawalRequest): String? {
        val ref = rootRef.child("Withdrawals").push()
        val values = mapOf(
            "UserID" to request.userId,
            "UserName" to request.userName,
            "Amount" to request.amount,
            "Destination" to request.destination,
            "AccountNumber" to request.accountNumber,
            "Status" to request.status,
            "CreatedAt" to request.createdAt,
            "ReviewedBy" to "",
            "ReviewedAt" to 0L
        )
        ref.setValue(values)
        return ref.key
    }

    /** Used when an account is deleted: keeps the record but detaches it from the old UID. */
    fun updateWithdrawalOwner(id: String, newUserId: String) {
        if (id.isBlank()) return
        rootRef.child("Withdrawals").child(id).child("UserID").setValue(newUserId)
    }

    fun updateWithdrawalStatus(id: String, status: String, reviewer: String) {
        if (id.isBlank()) return
        val updates = mapOf(
            "Status" to status,
            "ReviewedBy" to reviewer,
            "ReviewedAt" to System.currentTimeMillis()
        )
        rootRef.child("Withdrawals").child(id).updateChildren(updates)
    }

    fun listenToWithdrawals(onUpdate: (List<WithdrawalRequest>) -> Unit) {
        rootRef.child("Withdrawals").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<WithdrawalRequest>()
                for (child in snapshot.children) {
                    try {
                        list.add(
                            WithdrawalRequest(
                                id = child.key ?: "",
                                userId = child.child("UserID").value?.toString() ?: "",
                                userName = child.child("UserName").value?.toString() ?: "",
                                amount = (child.child("Amount").value as? Number)?.toDouble() ?: 0.0,
                                destination = child.child("Destination").value?.toString() ?: "GCASH",
                                accountNumber = child.child("AccountNumber").value?.toString() ?: "",
                                status = child.child("Status").value?.toString() ?: "PENDING",
                                createdAt = (child.child("CreatedAt").value as? Number)?.toLong() ?: 0L,
                                reviewedBy = child.child("ReviewedBy").value?.toString() ?: "",
                                reviewedAt = (child.child("ReviewedAt").value as? Number)?.toLong() ?: 0L
                            )
                        )
                    } catch (_: Exception) {
                        Log.e("FirebaseDB", "Error parsing withdrawal")
                    }
                }
                onUpdate(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------- FARE RATES ----------

    fun uploadFareRates(regular: Double, student: Double, updatedBy: String) {
        val values = mapOf(
            "Regular" to regular,
            "Student" to student,
            "UpdatedBy" to updatedBy,
            "UpdatedAt" to ServerValue.TIMESTAMP
        )
        rootRef.child("Settings").child("FareRates").setValue(values)
    }

    fun listenToFareRates(onUpdate: (Double?, Double?, String, Long) -> Unit) {
        rootRef.child("Settings").child("FareRates").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val regular = (snapshot.child("Regular").value as? Number)?.toDouble()
                val student = (snapshot.child("Student").value as? Number)?.toDouble()
                val updatedBy = snapshot.child("UpdatedBy").value?.toString() ?: ""
                val updatedAt = (snapshot.child("UpdatedAt").value as? Number)?.toLong() ?: 0L
                onUpdate(regular, student, updatedBy, updatedAt)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun uploadFareNotice(notice: FareNotice) {
        val values = mapOf(
            "Regular" to notice.regular,
            "Student" to notice.student,
            "PreviousRegular" to notice.previousRegular,
            "PreviousStudent" to notice.previousStudent,
            "Reason" to notice.reason,
            "ChangedAt" to notice.changedAt
        )
        rootRef.child("Settings").child("FareNotice").setValue(values)
    }

    fun listenToFareNotice(onUpdate: (FareNotice?) -> Unit) {
        rootRef.child("Settings").child("FareNotice").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onUpdate(null)
                    return
                }
                fun number(key: String): Double = (snapshot.child(key).value as? Number)?.toDouble() ?: 0.0
                onUpdate(
                    FareNotice(
                        regular = number("Regular"),
                        student = number("Student"),
                        previousRegular = number("PreviousRegular"),
                        previousStudent = number("PreviousStudent"),
                        reason = snapshot.child("Reason").value?.toString() ?: "",
                        changedAt = (snapshot.child("ChangedAt").value as? Number)?.toLong() ?: 0L
                    )
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun submitFareRequest(request: FareRequest) {
        val values = mapOf(
            "OperatorID" to request.operatorId,
            "OperatorName" to request.operatorName,
            "ProposedRegular" to request.proposedRegular,
            "ProposedStudent" to request.proposedStudent,
            "CurrentRegular" to request.currentRegular,
            "CurrentStudent" to request.currentStudent,
            "Reason" to request.reason,
            "Status" to request.status,
            "CreatedAt" to request.createdAt
        )
        rootRef.child("FareRequests").push().setValue(values)
    }

    fun updateFareRequestStatus(id: String, status: String, reviewer: String) {
        if (id.isBlank()) return
        val updates = mapOf(
            "Status" to status,
            "ReviewedBy" to reviewer,
            "ReviewedAt" to System.currentTimeMillis()
        )
        rootRef.child("FareRequests").child(id).updateChildren(updates)
    }

    fun listenToFareRequests(onUpdate: (List<FareRequest>) -> Unit) {
        rootRef.child("FareRequests").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FareRequest>()
                for (child in snapshot.children) {
                    try {
                        fun number(key: String): Double = (child.child(key).value as? Number)?.toDouble() ?: 0.0
                        fun long(key: String): Long = (child.child(key).value as? Number)?.toLong() ?: 0L
                        list.add(
                            FareRequest(
                                id = child.key ?: "",
                                operatorId = child.child("OperatorID").value?.toString() ?: "",
                                operatorName = child.child("OperatorName").value?.toString() ?: "",
                                proposedRegular = number("ProposedRegular"),
                                proposedStudent = number("ProposedStudent"),
                                currentRegular = number("CurrentRegular"),
                                currentStudent = number("CurrentStudent"),
                                reason = child.child("Reason").value?.toString() ?: "",
                                status = child.child("Status").value?.toString() ?: "PENDING",
                                createdAt = long("CreatedAt"),
                                reviewedBy = child.child("ReviewedBy").value?.toString() ?: "",
                                reviewedAt = long("ReviewedAt")
                            )
                        )
                    } catch (_: Exception) {
                        Log.e("FirebaseDB", "Error parsing fare request")
                    }
                }
                onUpdate(list.sortedByDescending { it.createdAt })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenToUsers(onUpdate: (List<UserAccount>) -> Unit) {
        rootRef.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserAccount>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("UserID").value?.toString() ?: child.key ?: ""
                        val name = child.child("Name").value?.toString() ?: ""
                        val role = child.child("Role").value?.toString() ?: "CLIENT"
                        val info = child.child("ContactInfo").value?.toString() ?: ""
                        val date = child.child("RegistrationDate").getValue(Long::class.java) ?: 0L
                        val pass = child.child("Password").value?.toString() ?: ""
                        val username = child.child("Username").value?.toString() ?: ""
                        val accountStatus = child.child("AccountStatus").value?.toString() ?: "ACTIVE"
                        val passengerType = child.child("PassengerType").value?.toString() ?: "REGULAR"
                        val balance = when (val b = child.child("Balance").value) {
                            is Number -> b.toDouble()
                            is String -> b.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        
                        if (id.isNotBlank()) {
                            list.add(
                                UserAccount(
                                    userID = id, name = name, role = role, contactInfo = info,
                                    registrationDate = date, password = pass, balance = balance,
                                    passengerType = passengerType, username = username, accountStatus = accountStatus
                                )
                            )
                        }
                    } catch (_: Exception) {
                        Log.e("FirebaseDB", "Error parsing user account")
                    }
                }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenToCards(onUpdate: (List<RfidCard>) -> Unit) {
        rootRef.child("Cards").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RfidCard>()
                for (child in snapshot.children) {
                    try {
                        val cardID = child.child("CardID").value?.toString() ?: child.key ?: ""
                        val userID = child.child("UserID").value?.toString() ?: ""
                        val status = child.child("Status").value?.toString() ?: "ACTIVE"
                        val lastReload = when (val lr = child.child("LastReload").value) {
                            is Number -> lr.toLong()
                            is String -> lr.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        
                        if (cardID.isNotBlank()) {
                            list.add(RfidCard(cardID, userID, lastReload, status))
                        }
                    } catch (_: Exception) {
                        Log.e("FirebaseDB", "Error parsing rfid card")
                    }
                }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun fetchAllCards(): List<RfidCard> {
        return try {
            val snapshot = rootRef.child("Cards").get().await()
            val list = mutableListOf<RfidCard>()
            for (child in snapshot.children) {
                val cardID = child.child("CardID").value?.toString() ?: child.key ?: ""
                val userID = child.child("UserID").value?.toString() ?: ""
                val status = child.child("Status").value?.toString() ?: "ACTIVE"
                val lastReload = when (val lr = child.child("LastReload").value) {
                    is Number -> lr.toLong()
                    is String -> lr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                if (cardID.isNotBlank()) {
                    list.add(RfidCard(cardID, userID, lastReload, status))
                }
            }
            list
        } catch (_: Exception) { emptyList() }
    }
}
