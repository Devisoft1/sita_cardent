package com.example.sitacardent

import platform.Foundation.NSUserDefaults

actual object LocalStorage {
    private const val KEY_TOKEN = "com.example.sitacardent.auth_token"
    private const val KEY_NAME = "com.example.sitacardent.user_name"
    private const val KEY_EMAIL = "com.example.sitacardent.user_email"
    private const val KEY_SHOP_ID = "com.example.sitacardent.shop_id"

    actual fun saveAuth(token: String, name: String, email: String, shopId: Int) {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setObject(token, forKey = KEY_TOKEN)
        userDefaults.setObject(name, forKey = KEY_NAME)
        userDefaults.setObject(email, forKey = KEY_EMAIL)
        userDefaults.setInteger(shopId.toLong(), forKey = KEY_SHOP_ID)
        userDefaults.synchronize()
    }

    actual fun getAuthToken(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_TOKEN)
    }

    actual fun getUserInfo(): Triple<String, String, Int>? {
        val userDefaults = NSUserDefaults.standardUserDefaults
        val name = userDefaults.stringForKey(KEY_NAME)
        val email = userDefaults.stringForKey(KEY_EMAIL)
        val shopId = userDefaults.integerForKey(KEY_SHOP_ID).toInt()
        
        // NSUserDefaults integerForKey returns 0 if not found, checking if we have token implies auth
        if (name != null && email != null) {
            return Triple(name, email, shopId)
        }
        return null
    }

    actual fun clearAuth() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.removeObjectForKey(KEY_TOKEN)
        userDefaults.removeObjectForKey(KEY_NAME)
        userDefaults.removeObjectForKey(KEY_EMAIL)
        userDefaults.removeObjectForKey(KEY_SHOP_ID)
        userDefaults.synchronize()
    }
}
