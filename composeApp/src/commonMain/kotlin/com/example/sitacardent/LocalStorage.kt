package com.example.sitacardent

/**
 * Platform storage for remembering login across app launches.
 *
 * - iOS: NSUserDefaults
 * - Android: SharedPreferences
 */
expect object LocalStorage {
    fun getUser(): Pair<String, String>?
    fun saveUser(email: String, pass: String)
    fun clearUser()
    fun isLoggedIn(): Boolean
    fun setLoggedIn(loggedIn: Boolean)
}
