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
    private const val KEY_IMAGE_URL = "image_url"
    private const val KEY_IMAGES = "image_urls"
    private const val KEY_SHOP_UID = "shop_uid"


    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Call once from an Android entry point (Activity/Application) if you use the KMP Compose UI on Android.
     * iOS does not require init.
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveAuth(token: String, name: String, email: String, shopId: Int, logoUrl: String?, images: List<String>?, shopUId: String?) {
        println("LoginDebug: LocalStorage (Android) - Saving Auth. Name: $name, ImagesCount: ${images?.size ?: 0}")
        val p = prefs ?: return
        val imagesString = images?.joinToString(",")
        println("LoginDebug: LocalStorage (Android) - Images String: $imagesString")
        println("LoginDebug: LocalStorage (Android) - Saving shopUId: $shopUId")
        p.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putInt(KEY_SHOP_ID, shopId)
            .putString(KEY_LOGO_URL, logoUrl)
            .putString(KEY_IMAGES, imagesString)
            .putString(KEY_IMAGE_URL, images?.firstOrNull())
            .putString(KEY_SHOP_UID, shopUId)
            .apply()
    }




    actual fun getAuthToken(): String? {
        val token = prefs?.getString(KEY_TOKEN, null)
        println("LoginDebug: LocalStorage (Android) - getAuthToken called. Found: ${token != null}")
        return token
    }

    actual fun getShopId(): Int? {
        val id = prefs?.getInt(KEY_SHOP_ID, 0)
        return if (id == 0) null else id
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
    
    actual fun getImageUrl(): String? {
        val imageUrl = prefs?.getString(KEY_IMAGE_URL, null)
        println("LoginDebug: LocalStorage (Android) - getImageUrl called. Found: ${imageUrl != null}")
        return imageUrl
    }

    actual fun getShopUId(): String? {
        val uid = prefs?.getString(KEY_SHOP_UID, null)
        println("LoginDebug: LocalStorage (Android) - getShopUId called. Found: ${uid != null}, UID: $uid")
        return uid
    }

    actual fun getImages(): List<String> {

        val imagesString = prefs?.getString(KEY_IMAGES, null)
        val images = imagesString?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        println("LoginDebug: LocalStorage (Android) - getImages called. Count: ${images.size}")
        return images
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
            .remove(KEY_IMAGE_URL)
            .apply()
        println("LoginDebug: LocalStorage (Android) - Auth Data Cleared")
    }

    actual fun saveRememberMe(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_REMEMBER_ME, enabled)?.apply()
    }

    actual fun isRememberMe(): Boolean {
        return prefs?.getBoolean(KEY_REMEMBER_ME, false) ?: false
    }

    actual fun saveCredentials(email: String, password: String) {
        prefs?.edit()
            ?.putString(KEY_SAVED_EMAIL, email)
            ?.putString(KEY_SAVED_PASSWORD, password)
            ?.apply()
    }

    actual fun getSavedEmail(): String? {
        return prefs?.getString(KEY_SAVED_EMAIL, null)
    }

    actual fun getSavedPassword(): String? {
        return prefs?.getString(KEY_SAVED_PASSWORD, null)
    }

    actual fun clearSavedCredentials() {
        prefs?.edit()
            ?.remove(KEY_SAVED_EMAIL)
            ?.remove(KEY_SAVED_PASSWORD)
            ?.apply()
    }
}

