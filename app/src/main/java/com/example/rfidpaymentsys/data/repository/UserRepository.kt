package com.example.rfidpaymentsys.data.repository

import com.example.rfidpaymentsys.data.dao.*
import com.example.rfidpaymentsys.data.entity.*
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val auditLogDao: AuditLogDao,
    private val validationLogDao: ValidationLogDao,
    private val incomeReportDao: IncomeReportDao
) {
    val allUsers: Flow<List<UserAccount>> = userDao.getAllUsers()

    suspend fun getUserById(id: String): UserAccount? {
        return userDao.getUserById(id)
    }

    suspend fun getUserByName(name: String): UserAccount? {
        return userDao.getUserByName(name)
    }

    suspend fun getUserByUsername(username: String): UserAccount? {
        return if (username.isBlank()) null else userDao.getUserByUsername(username.trim())
    }

    suspend fun reassignTransactions(oldUserId: String, newUserId: String) {
        transactionDao.reassignUser(oldUserId, newUserId)
    }

    suspend fun getUserByContact(contact: String): UserAccount? {
        return if (contact.isBlank()) null else userDao.getUserByContact(contact.trim())
    }

    suspend fun insertUser(user: UserAccount) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserAccount) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: UserAccount) {
        userDao.deleteUser(user)
    }

    // Card logic
    suspend fun getCardById(cardId: String): RfidCard? {
        return userDao.getCardById(cardId)
    }

    suspend fun getCardByUserId(userId: String): RfidCard? {
        return userDao.getCardByUserId(userId)
    }

    fun getCardByUserIdFlow(userId: String): Flow<RfidCard?> {
        return userDao.getCardByUserIdFlow(userId)
    }

    suspend fun insertCard(card: RfidCard) {
        userDao.insertCard(card)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    fun getRecentTransactionsForUser(uid: String): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactionsForUser(uid)
    }

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Audit Logs
    val allAuditLogs: Flow<List<AuditLog>> = auditLogDao.getAllAuditLogs()

    suspend fun insertAuditLog(log: AuditLog) {
        auditLogDao.insertAuditLog(log)
    }

    suspend fun insertValidationLog(log: ValidationLog) {
        validationLogDao.insertLog(log)
    }

    suspend fun insertIncomeReport(report: IncomeReport) {
        incomeReportDao.insertReport(report)
    }

    suspend fun deleteNonProfitTransactions() {
        transactionDao.deleteNonProfitTransactions()
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    /**
     * Makes the local copy match Firebase: adds and updates users,
     * and removes users that were deleted from Firebase.
     */
    suspend fun syncUsers(cloudUsers: List<UserAccount>) {
        cloudUsers.forEach { user ->
            userDao.insertUser(user)
        }
        // An empty list is never treated as "delete everyone" (protects against a bad read).
        if (cloudUsers.isNotEmpty()) {
            val cloudIds = cloudUsers.map { it.userID }.toSet()
            userDao.getAllUsersOnce()
                .filter { it.userID !in cloudIds }
                .forEach { userDao.deleteUser(it) }
        }
    }

    suspend fun syncCards(cloudCards: List<RfidCard>) {
        cloudCards.forEach { card ->
            userDao.insertCard(card)
        }
        if (cloudCards.isNotEmpty()) {
            val cloudIds = cloudCards.map { it.cardID }.toSet()
            userDao.getAllCardsOnce()
                .filter { it.cardID !in cloudIds }
                .forEach { userDao.deleteCard(it.cardID) }
        }
    }

    suspend fun deleteCard(cardId: String) {
        userDao.deleteCard(cardId)
    }
}
