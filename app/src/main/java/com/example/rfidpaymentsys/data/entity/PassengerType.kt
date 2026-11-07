package com.example.rfidpaymentsys.data.entity

/** Passenger categories chosen at registration. Students, PWD and Seniors pay the discounted fare. */
object PassengerType {
    const val REGULAR = "REGULAR"
    const val STUDENT = "STUDENT"
    const val PWD = "PWD"
    const val SENIOR = "SENIOR"
}

fun passengerTypeLabel(type: String): String = when (type.uppercase()) {
    PassengerType.STUDENT -> "Student"
    PassengerType.PWD -> "PWD"
    PassengerType.SENIOR -> "Senior Citizen"
    else -> "Regular"
}

fun isDiscountedPassenger(type: String): Boolean = when (type.uppercase()) {
    PassengerType.STUDENT, PassengerType.PWD, PassengerType.SENIOR -> true
    else -> false
}
