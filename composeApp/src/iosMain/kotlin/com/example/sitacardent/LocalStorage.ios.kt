package com.example.sitacardent

import platform.Foundation.NSUserDefaults

actual object LocalStorage {
    private const val KEY_TOKEN = "com.example.sitacardent.auth_token"
    private const val KEY_NAME = "com.example.sitacardent.user_name"
    private const val KEY_EMAIL = "com.example.sitacardent.user_email"
    private const val KEY_SHOP_ID = "com.example.sitacardent.shop_id"
    private const val KEY_LOGO_URL = "com.example.sitacardent.logo_url"
    private const val KEY_REMEMBER_ME = "com.example.sitacardent.remember_me"
    private const val KEY_SAVED_EMAIL = "com.example.sitacardent.saved_email"
    private const val KEY_SAVED_PASSWORD = "com.example.sitacardent.saved_password"
    private const val KEY_IMAGES = "com.example.sitacardent.image_urls"
    private const val KEY_IMAGE_URL = "com.example.sitacardent.image_url"
    private const val KEY_SHOP_UID = "com.example.sitacardent.shop_uid"


    actual fun saveAuth(token: String, name: String, email: String, shopId: Int, logoUrl: String?, images: List<String>?, shopUId: String?) {
        println("LoginDebug: LocalStorage (iOS) - Saving Auth. Name: $name, ImagesCount: ${images?.size ?: 0}")
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setObject(token, forKey = KEY_TOKEN)
        userDefaults.setObject(name, forKey = KEY_NAME)
        userDefaults.setObject(email, forKey = KEY_EMAIL)
        userDefaults.setInteger(shopId.toLong(), forKey = KEY_SHOP_ID)
        userDefaults.setObject(shopUId, forKey = KEY_SHOP_UID)
        
        // Simplified logoUrl saving
        userDefaults.setObject(logoUrl, forKey = KEY_LOGO_URL)
        
        // Simplified images saving
        if (images != null && images.isNotEmpty()) {
            userDefaults.setObject(images, forKey = KEY_IMAGES) // NSUserDefaults can store NSArray directly
            userDefaults.setObject(images.firstOrNull(), forKey = KEY_IMAGE_URL)
        } else {
            userDefaults.removeObjectForKey(KEY_IMAGES)
            userDefaults.removeObjectForKey(KEY_IMAGE_URL)
        }
        
        userDefaults.synchronize()
        println("LoginDebug: LocalStorage (iOS) - Data Saved Succesfully")
    }



    actual fun getAuthToken(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_TOKEN)
    }

    actual fun getShopId(): Int? {
        val id = NSUserDefaults.standardUserDefaults.integerForKey(KEY_SHOP_ID).toInt()
        return if (id == 0) null else id
    }

    actual fun getImageUrl(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_IMAGE_URL)
    }

    actual fun getShopUId(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_SHOP_UID)
    }


    actual fun getImages(): List<String> {
        val userDefaults = NSUserDefaults.standardUserDefaults
        val images = userDefaults.arrayForKey(KEY_IMAGES)?.filterIsInstance<String>() ?: emptyList()
        println("LoginDebug: LocalStorage (iOS) - getImages called. Count: ${images.size}")
        return images
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

    actual fun saveRememberMe(enabled: Boolean) {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setBool(enabled, forKey = KEY_REMEMBER_ME)
        userDefaults.synchronize()
    }

    actual fun isRememberMe(): Boolean {
        return NSUserDefaults.standardUserDefaults.boolForKey(KEY_REMEMBER_ME)
    }

    actual fun saveCredentials(email: String, password: String) {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setObject(email, forKey = KEY_SAVED_EMAIL)
        userDefaults.setObject(password, forKey = KEY_SAVED_PASSWORD)
        userDefaults.synchronize()
    }

    actual fun getSavedEmail(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_SAVED_EMAIL)
    }

    actual fun getSavedPassword(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_SAVED_PASSWORD)
    }

    actual fun clearSavedCredentials() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.removeObjectForKey(KEY_SAVED_EMAIL)
        userDefaults.removeObjectForKey(KEY_SAVED_PASSWORD)
        userDefaults.synchronize()
    }
}
