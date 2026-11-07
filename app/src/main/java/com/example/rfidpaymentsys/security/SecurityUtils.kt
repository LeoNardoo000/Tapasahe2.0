package com.example.rfidpaymentsys.security

import org.mindrot.jbcrypt.BCrypt

object SecurityUtils {
    
    /**
     * Hashes a plain-text password using BCrypt.
     * Use this during registration or password change.
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Checks if a plain-text password matches a stored hash.
     * This handles plain-text fallback for old accounts (optional but helpful for migration).
     */
    fun verifyPassword(password: String, storedValue: String): Boolean {
        return try {
            // Check if stored value is a BCrypt hash
            if (storedValue.startsWith("$2a$") || storedValue.startsWith("$2b$") || storedValue.startsWith("$2y$")) {
                BCrypt.checkpw(password, storedValue)
            } else {
                // Fallback for old plain-text accounts (for defense, you can explain this is a migration path)
                password == storedValue
            }
        } catch (e: Exception) {
            false
        }
    }
}
