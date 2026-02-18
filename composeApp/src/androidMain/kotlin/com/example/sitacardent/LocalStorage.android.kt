package com.example.sitacardent

import android.content.Context
import android.content.SharedPreferences

actual object LocalStorage {
    private const val PREFS_NAME = "LoginPrefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_SHOP_ID = "shop_id"
    private const val KEY_LOGO_URL = "logo_url"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Call once from an Android entry point (Activity/Application) if you use the KMP Compose UI on Android.
     * iOS does not require init.
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveAuth(token: String, name: String, email: String, shopId: Int, logoUrl: String?) {
        println("LoginDebug: LocalStorage (Android) - Saving Auth Data. Name: $name, Email: $email, LogoUrl: $logoUrl")
        val p = prefs ?: return
        p.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putInt(KEY_SHOP_ID, shopId)
            .putString(KEY_LOGO_URL, logoUrl)
            .apply()
        println("LoginDebug: LocalStorage (Android) - Data Saved Successfully")
    }

    actual fun getAuthToken(): String? {
        val token = prefs?.getString(KEY_TOKEN, null)
        println("LoginDebug: LocalStorage (Android) - getAuthToken called. Found: ${token != null}")
        return token
    }

    actual fun getUserInfo(): Triple<String, String, Int>? {
        val p = prefs ?: return null
        val name = p.getString(KEY_NAME, null)
        val email = p.getString(KEY_EMAIL, null)
        val shopId = p.getInt(KEY_SHOP_ID, -1)
        
        println("LoginDebug: LocalStorage (Android) - getUserInfo called. Found: ${name != null}")
        
        return if (name != null && email != null && shopId != -1) {
            Triple(name, email, shopId)
        } else {
            null
        }
    }

    actual fun getLogoUrl(): String? {
        val logoUrl = prefs?.getString(KEY_LOGO_URL, null)
        println("LoginDebug: LocalStorage (Android) - getLogoUrl called. Found: ${logoUrl != null}")
        return logoUrl
    }

    actual fun clearAuth() {
        println("LoginDebug: LocalStorage (Android) - Clearing Auth Data")
        val p = prefs ?: return
        p.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_SHOP_ID)
            .remove(KEY_LOGO_URL)
            .apply()
        println("LoginDebug: LocalStorage (Android) - Auth Data Cleared")
    }
}

