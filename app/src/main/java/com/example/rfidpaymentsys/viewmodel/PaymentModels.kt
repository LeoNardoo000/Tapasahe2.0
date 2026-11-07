package com.example.rfidpaymentsys.viewmodel

/** A top-up the client has started and is about to pay. */
data class PaymentSession(
    val reference: String,
    val userId: String,
    val amount: Double
)

/** Shown to the client after a successful top-up. */
data class PaymentReceipt(
    val reference: String,
    val amount: Double,
    val methodLabel: String,
    val timestamp: Long,
    val newBalance: Double
)

/** Shown to the operator after a successful cash-out. */
data class CashOutReceipt(
    val reference: String,
    val amount: Double,
    val destination: String,
    val maskedAccount: String,
    val timestamp: Long,
    val newBalance: Double
)

/** Result of tapping a card outside the operator terminal: its ID and who it belongs to (if anyone). */
data class CardLookup(
    val uid: String,
    val ownerName: String?,
    val timestamp: Long = System.currentTimeMillis()
)
