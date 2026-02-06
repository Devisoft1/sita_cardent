package com.example.sitacardent

/**
 * Platform storage for remembering login across app launches.
 *
 * - iOS: NSUserDefaults
 * - Android: SharedPreferences
 */
expect object LocalStorage {
    fun saveAuth(token: String, name: String, email: String, shopId: Int)
    fun getAuthToken(): String?
    fun getUserInfo(): Triple<String, String, Int>? // Name, Email, ShopId
    fun clearAuth()
}
