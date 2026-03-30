package com.example.sitacardent

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun isCardExpired(validity: String): Boolean {
    if (validity.length != 8) return false

    return try {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "ddMMyyyy"
        
        val validityDate = formatter.dateFromString(validity) ?: return false
        
        // validityDate is parsed as 00:00:00 of that day.
        // We add 86400 seconds (24 hours) to cover the whole day.
        // So the expiry timestamp is validityDate + 1 day
        val expiryTimeSeconds = validityDate.timeIntervalSince1970 + 86400.0
        val currentTimeSeconds = NSDate().timeIntervalSince1970
        
        // Expired if current time is past the expiry time
        currentTimeSeconds > expiryTimeSeconds
    } catch (e: Exception) {
        false
    }
}
