package com.example.sitacardent

import platform.Foundation.NSUserDefaults

actual object LocalStorage {
    private const val KEY_TOKEN = "com.example.sitacardent.auth_token"
    private const val KEY_NAME = "com.example.sitacardent.user_name"
    private const val KEY_EMAIL = "com.example.sitacardent.user_email"
    private const val KEY_SHOP_ID = "com.example.sitacardent.shop_id"
    private const val KEY_LOGO_URL = "com.example.sitacardent.logo_url"

    actual fun saveAuth(token: String, name: String, email: String, shopId: Int, logoUrl: String?) {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setObject(token, forKey = KEY_TOKEN)
        userDefaults.setObject(name, forKey = KEY_NAME)
        userDefaults.setObject(email, forKey = KEY_EMAIL)
        userDefaults.setInteger(shopId.toLong(), forKey = KEY_SHOP_ID)
        if (logoUrl != null) {
            userDefaults.setObject(logoUrl, forKey = KEY_LOGO_URL)
        } else {
            userDefaults.removeObjectForKey(KEY_LOGO_URL)
        }
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

    actual fun getLogoUrl(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_LOGO_URL)
    }

    actual fun clearAuth() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.removeObjectForKey(KEY_TOKEN)
        userDefaults.removeObjectForKey(KEY_NAME)
        userDefaults.removeObjectForKey(KEY_EMAIL)
        userDefaults.removeObjectForKey(KEY_SHOP_ID)
        userDefaults.removeObjectForKey(KEY_LOGO_URL)
        userDefaults.synchronize()
    }
}
