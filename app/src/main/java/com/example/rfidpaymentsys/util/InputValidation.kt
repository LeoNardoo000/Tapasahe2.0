package com.example.rfidpaymentsys.util

import android.util.Patterns

/** One place for the rules about what users are allowed to type. */
object InputValidation {
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_NAME_LENGTH = 50
    const val MAX_EMAIL_LENGTH = 100

    private val notAllowedInName = Regex("[^\\p{L} .'\\-]")
    private val validName = Regex("^\\p{L}[\\p{L} .'\\-]*$")
    private val extraSpaces = Regex("\\s+")

    // ---------- Names ----------

    /** Use while typing: removes every character that cannot be part of a person's name. */
    fun sanitizeNameInput(input: String): String =
        input.replace('\u2019', '\'')
            .replace(notAllowedInName, "")
            .take(MAX_NAME_LENGTH)

    /** Trims and collapses repeated spaces. */
    fun normalizeName(input: String): String = input.trim().replace(extraSpaces, " ")

    /** Returns an error message, or null when the name is acceptable. */
    fun nameError(raw: String): String? {
        val name = normalizeName(raw)
        return when {
            name.isEmpty() -> "Please enter your name."
            name.count { it.isLetter() } < 2 -> "Your name must have at least 2 letters."
            name.length > MAX_NAME_LENGTH -> "Your name is too long (maximum $MAX_NAME_LENGTH characters)."
            !validName.matches(name) ->
                "Names can only contain letters, spaces, periods, hyphens and apostrophes."
            else -> null
        }
    }

    // ---------- Username ----------

    const val MAX_USERNAME_LENGTH = 20
    private val validUsername = Regex("^[a-zA-Z][a-zA-Z0-9._]{3,19}$")

    /** Use while typing: only letters, numbers, dots and underscores. */
    fun sanitizeUsernameInput(input: String): String =
        input.filter { (it.isLetterOrDigit() && it.code < 128) || it == '.' || it == '_' }.take(MAX_USERNAME_LENGTH)

    fun normalizeUsername(raw: String): String = raw.trim().lowercase()

    fun usernameError(raw: String): String? {
        val username = normalizeUsername(raw)
        return when {
            username.isEmpty() -> "Please choose a username."
            !validUsername.matches(username) ->
                "Username must be 4 to 20 characters, start with a letter, and use only letters, numbers, dots or underscores."
            else -> null
        }
    }

    // ---------- Email / phone ----------

    /** Use while typing: no spaces in an email address. */
    fun sanitizeEmailInput(input: String): String =
        input.filter { !it.isWhitespace() }.take(MAX_EMAIL_LENGTH)

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun isValidEmail(raw: String): Boolean {
        val email = normalizeEmail(raw)
        return email.isNotEmpty() && email.length <= MAX_EMAIL_LENGTH &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun emailError(raw: String): String? = when {
        raw.isBlank() -> "Please enter your email address."
        !isValidEmail(raw) -> "Please enter a valid email address."
        else -> null
    }

    fun isValidPhone(raw: String): Boolean = raw.length in 10..13 && raw.all { it.isDigit() }

    /** Contact info can be an email or a phone number. Blank is allowed unless [required]. */
    fun contactError(raw: String, required: Boolean): String? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> if (required) "Please enter an email address or phone number." else null
            isValidEmail(value) || isValidPhone(value) -> null
            else -> "Enter a valid email address or a phone number (digits only)."
        }
    }

    // ---------- Password ----------

    fun passwordError(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
        password.isBlank() -> "Password cannot be blank."
        else -> null
    }
}
