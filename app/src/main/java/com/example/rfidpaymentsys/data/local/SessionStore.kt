package com.example.rfidpaymentsys.data.local

import android.content.Context

/**
 * Remembers who is using this device, so they don't have to log in again.
 * Only the user ID, role and last username are stored, never the password.
 */
class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun save(userId: String, role: String) {
        prefs.edit().putString("userId", userId).putString("role", role).commit()
    }

    fun savedUserId(): String? = prefs.getString("userId", null)

    fun savedRole(): String? = prefs.getString("role", null)

    /** Forgets the signed-in user (keeps the last username so it can be pre-filled). */
    fun clear() {
        prefs.edit().remove("userId").remove("role").commit()
    }

    fun saveLastLogin(role: String, name: String) {
        prefs.edit().putString("last_$role", name).commit()
    }

    fun lastLogin(role: String): String = prefs.getString("last_$role", "") ?: ""

    fun clearLastLogin(role: String) {
        prefs.edit().remove("last_$role").commit()
    }

    /** The time of the last fare notice this user has already seen. */
    fun seenFareNotice(userId: String): Long = prefs.getLong("fareNotice_$userId", 0L)

    fun saveSeenFareNotice(userId: String, changedAt: Long) {
        prefs.edit().putLong("fareNotice_$userId", changedAt).apply()
    }

    /** When this user last sent a card request (stops repeated requests). */
    fun lastCardRequest(userId: String): Long = prefs.getLong("cardRequest_$userId", 0L)

    fun saveCardRequest(userId: String, at: Long) {
        prefs.edit().putLong("cardRequest_$userId", at).apply()
    }
}
