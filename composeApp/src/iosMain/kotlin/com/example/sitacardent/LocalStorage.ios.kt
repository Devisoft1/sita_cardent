package com.example.sitacardent

import platform.Foundation.NSUserDefaults

actual object LocalStorage {
    private const val KEY_EMAIL = "com.example.sitacardent.user_email"
    private const val KEY_PASS = "com.example.sitacardent.user_password"
    private const val KEY_LOGGED_IN = "com.example.sitacardent.is_logged_in"

    actual fun getUser(): Pair<String, String>? {
        val userDefaults = NSUserDefaults.standardUserDefaults
        val email = userDefaults.stringForKey(KEY_EMAIL)
        val pass = userDefaults.stringForKey(KEY_PASS)
        
        return if (email != null && email.isNotEmpty() && pass != null && pass.isNotEmpty()) {
            Pair(email, pass)
        } else {
            null
        }
    }

    actual fun saveUser(email: String, pass: String) {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setObject(email, forKey = KEY_EMAIL)
        userDefaults.setObject(pass, forKey = KEY_PASS)
        userDefaults.synchronize()
    }

    actual fun clearUser() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.removeObjectForKey(KEY_EMAIL)
        userDefaults.removeObjectForKey(KEY_PASS)
        userDefaults.synchronize()
    }

    actual fun isLoggedIn(): Boolean {
        return NSUserDefaults.standardUserDefaults.boolForKey(KEY_LOGGED_IN)
    }

    actual fun setLoggedIn(loggedIn: Boolean) {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setBool(loggedIn, forKey = KEY_LOGGED_IN)
        userDefaults.synchronize()
    }
}
