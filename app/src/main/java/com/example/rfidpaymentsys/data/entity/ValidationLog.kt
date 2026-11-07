package com.example.rfidpaymentsys.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "validation_logs")
data class ValidationLog(
    @PrimaryKey(autoGenerate = true) val validationID: Int = 0,
    val transactionID: Long? = null, // Linked to Transactions table, null if DECLINED
    val validatorID: String = "",    // Matches Operator ID
    val result: String = "",         // "APPROVED" or "DECLINED"
    val timestamp: Long = System.currentTimeMillis()
)
