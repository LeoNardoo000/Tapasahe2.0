package com.example.rfidpaymentsys.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val transactionID: Long = 0,
    val userID: String = "",
    val driverID: String = "",
    val amount: Double = 0.0,
    val method: String = "RFID",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS",
    val syncFlag: Boolean = true
)
