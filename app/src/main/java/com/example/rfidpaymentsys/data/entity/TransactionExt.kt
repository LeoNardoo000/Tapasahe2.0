package com.example.rfidpaymentsys.data.entity

/** True when the transaction takes money OUT of the account owner's balance. */
fun Transaction.isDebit(): Boolean =
    method == "RFID" || method.startsWith("CASH_OUT") || method.startsWith("WITHDRAW_")

fun paymentMethodLabel(code: String): String = when (code.uppercase()) {
    "GCASH" -> "GCash"
    "MAYA" -> "Maya"
    "CARD" -> "Credit / Debit Card"
    else -> code
}

/** Human-friendly title for lists and receipts. */
fun Transaction.displayTitle(): String = when {
    method == "RFID" -> "Ride payment"
    method == "TOP_UP" -> "Balance top-up"
    method == "DEPOSIT" -> "Initial deposit"
    method.startsWith("ONLINE_") -> "Top-up via " + paymentMethodLabel(method.removePrefix("ONLINE_"))
    method.startsWith("CASH_OUT_") -> "Cash out to " + paymentMethodLabel(method.removePrefix("CASH_OUT_"))
    method.startsWith("WITHDRAW_") -> "Withdrawal to " + paymentMethodLabel(method.removePrefix("WITHDRAW_"))
    else -> method
}
