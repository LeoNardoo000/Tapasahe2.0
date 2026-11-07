package com.example.rfidpaymentsys.data.model

/** An operator's request to change the fare rates. Stored in Firebase under FareRequests. */
data class FareRequest(
    val id: String = "",
    val operatorId: String = "",
    val operatorName: String = "",
    val proposedRegular: Double = 0.0,
    val proposedStudent: Double = 0.0,
    val currentRegular: Double = 0.0,
    val currentStudent: Double = 0.0,
    val reason: String = "",
    val status: String = "PENDING", // PENDING, APPROVED or REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedBy: String = "",
    val reviewedAt: Long = 0L
)

/** The latest fare change, shown to clients after they log in. Stored in Firebase under Settings/FareNotice. */
data class FareNotice(
    val regular: Double = 0.0,
    val student: Double = 0.0,
    val previousRegular: Double = 0.0,
    val previousStudent: Double = 0.0,
    val reason: String = "",
    val changedAt: Long = 0L
)
