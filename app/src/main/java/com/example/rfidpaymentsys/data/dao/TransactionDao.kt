package com.example.rfidpaymentsys.data.dao

import androidx.room.*
import com.example.rfidpaymentsys.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userID = :uid ORDER BY timestamp DESC")
    fun getTransactionsForUser(uid: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userID = :uid ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTransactionsForUser(uid: String): Flow<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("DELETE FROM transactions WHERE method != 'RFID'")
    suspend fun deleteNonProfitTransactions()

    /** Used when an account is deleted: keeps the money records but detaches them from the old UID. */
    @Query("UPDATE transactions SET userID = :newId WHERE userID = :oldId")
    suspend fun reassignUser(oldId: String, newId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
