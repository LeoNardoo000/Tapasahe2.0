package com.example.rfidpaymentsys.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rfid_cards")
data class RfidCard(
    @PrimaryKey val cardID: String = "",
    val userID: String = "",
    val lastReload: Long = 0L,
    val status: String = "ACTIVE"
)
