package com.example.sitacardent

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual object DateUtils {
    actual fun isCardExpired(validity: String): Boolean {
        // If the format is not EXACTLY 8 characters (ddMMyyyy), we assume it's invalid or we just pass it to the backend.
        if (validity.length != 8) return false

        return try {
            val format = SimpleDateFormat("ddMMyyyy", Locale.US)
            val validityDate = format.parse(validity) ?: return false
            
            // Push to end of day
            val cal = Calendar.getInstance()
            cal.time = validityDate
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            
            // Return true if the end of the validity day is strictly before the current time
            cal.time.before(Date())
        } catch (e: Exception) {
            // Log the exception if needed, but return false to not block unexpectedly
            false
        }
    }
}
