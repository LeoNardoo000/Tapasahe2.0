package com.example.rfidpaymentsys.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val userID: String = "",
    val name: String = "",
    val role: String = "CLIENT",
    val contactInfo: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val password: String = "",
    val gcashNumber: String = "", // Legacy column, no longer used. Kept so existing databases still open.
    val balance: Double = 0.0, // Added balance to UserAccount as requested
    val passengerType: String = "REGULAR", // REGULAR, STUDENT, PWD or SENIOR
    val username: String = "",
    val accountStatus: String = "ACTIVE" // ACTIVE, PENDING (operator application) or REJECTED
)
