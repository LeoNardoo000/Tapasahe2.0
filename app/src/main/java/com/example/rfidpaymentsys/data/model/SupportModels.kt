package com.example.rfidpaymentsys.data.model

/** What a support conversation is about. */
object SupportKind {
    const val CLIENT = "CLIENT"
    const val OPERATOR = "OPERATOR"
    const val FORGOT_PASSWORD = "FORGOT_PASSWORD"
}

fun supportKindLabel(kind: String): String = when (kind) {
    SupportKind.OPERATOR -> "Operator Concern"
    SupportKind.FORGOT_PASSWORD -> "Forgot Password Concern"
    else -> "Client Concern"
}

/** One chat message between a user and the admin. */
data class SupportMessage(
    val id: String = "",
    val senderRole: String = "CLIENT", // ADMIN, CLIENT, OPERATOR or GUEST
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

/** One conversation, used for the admin inbox and unread badges. */
data class SupportThread(
    val clientId: String = "",          // the conversation id (a user id, or FP-<uid> for password recovery)
    val clientName: String = "",
    val kind: String = SupportKind.CLIENT,
    val refUserId: String = "",         // the UID the person typed (password recovery)
    val refEmail: String = "",          // the email the person typed (password recovery)
    val lastMessage: String = "",
    val lastAt: Long = 0L,
    val lastSenderRole: String = "",
    val adminUnread: Int = 0,
    val clientUnread: Int = 0
)
