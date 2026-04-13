package com.example.sitacardent

import platform.Foundation.*

actual object DateUtils {
    actual fun isCardExpired(validity: String): Boolean {
        if (validity.length != 8) return false

        return try {
            val formatter = NSDateFormatter()
            formatter.dateFormat = "ddMMyyyy"
            
            val validityDate = formatter.dateFromString(validity) ?: return false
            
            val now = NSDate()
            val expiryDate = validityDate.dateByAddingTimeInterval(86400.0)
            
            now.compare(expiryDate) == NSOrderedDescending
        } catch (e: Exception) {
            false
        }
    }
}
