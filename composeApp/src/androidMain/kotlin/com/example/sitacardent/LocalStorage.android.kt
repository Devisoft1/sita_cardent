package com.example.sitacardent

import android.content.Context
import android.content.SharedPreferences

actual object LocalStorage {
    private const val PREFS_NAME = "LoginPrefs"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASS = "password"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Call once from an Android entry point (Activity/Application) if you use the KMP Compose UI on Android.
     * iOS does not require init.
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getUser(): Pair<String, String>? {
        val p = prefs ?: return null
        val email = p.getString(KEY_EMAIL, null)
        val pass = p.getString(KEY_PASS, null)
        return if (!email.isNullOrBlank() && !pass.isNullOrBlank()) {
            email to pass
        } else {
            null
        }
    }

    actual fun saveUser(email: String, pass: String) {
        val p = prefs ?: return
        p.edit().putString(KEY_EMAIL, email).putString(KEY_PASS, pass).apply()
    }

    actual fun clearUser() {
        val p = prefs ?: return
        p.edit().remove(KEY_EMAIL).remove(KEY_PASS).apply()
    }
}

