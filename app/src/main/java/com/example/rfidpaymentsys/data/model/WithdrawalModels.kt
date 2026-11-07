package com.example.rfidpaymentsys.data.model

/**
 * A client's request to withdraw part of their balance.
 * The amount is held (taken off the balance) when the request is made.
 * The admin then confirms the payout, or rejects it and the amount is returned.
 */
data class WithdrawalRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val destination: String = "GCASH", // GCASH or MAYA
    val accountNumber: String = "",
    val status: String = "PENDING",    // PENDING, APPROVED or REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedBy: String = "",
    val reviewedAt: Long = 0L
)
