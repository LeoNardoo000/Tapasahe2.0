package com.example.rfidpaymentsys.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_reports")
data class IncomeReport(
    @PrimaryKey(autoGenerate = true) val reportID: Int = 0,
    val driverID: String = "",       // Matches Operator ID
    val totalAmount: Double = 0.0,
    val dateGenerated: Long = System.currentTimeMillis(),
    val period: String = "DAILY"     // "DAILY", "WEEKLY"
)
