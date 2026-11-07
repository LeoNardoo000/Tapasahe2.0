package com.example.rfidpaymentsys.data.dao

import androidx.room.*
import com.example.rfidpaymentsys.data.entity.UserAccount
import com.example.rfidpaymentsys.data.entity.RfidCard
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // User Account Queries
    @Query("SELECT * FROM user_accounts WHERE userID = :id")
    suspend fun getUserById(id: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE name = :name COLLATE NOCASE")
    suspend fun getUserByName(name: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE contactInfo = :contact COLLATE NOCASE LIMIT 1")
    suspend fun getUserByContact(contact: String): UserAccount?

    @Query("SELECT * FROM user_accounts")
    suspend fun getAllUsersOnce(): List<UserAccount>

    @Query("SELECT * FROM rfid_cards")
    suspend fun getAllCardsOnce(): List<RfidCard>

    @Query("SELECT * FROM user_accounts ORDER BY registrationDate DESC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Update
    suspend fun updateUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    // RFID Card Queries
    @Query("SELECT * FROM rfid_cards WHERE cardID = :cardId")
    suspend fun getCardById(cardId: String): RfidCard?

    @Query("SELECT * FROM rfid_cards WHERE userID = :userId")
    suspend fun getCardByUserId(userId: String): RfidCard?

    @Query("SELECT * FROM rfid_cards WHERE userID = :userId")
    fun getCardByUserIdFlow(userId: String): Flow<RfidCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: RfidCard)

    @Update
    suspend fun updateCard(card: RfidCard)

    @Query("DELETE FROM rfid_cards WHERE cardID = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("DELETE FROM user_accounts")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM rfid_cards")
    suspend fun deleteAllCards()
}
