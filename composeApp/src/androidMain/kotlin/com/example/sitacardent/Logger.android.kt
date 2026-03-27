package com.example.sitacardent

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun platformLog(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun platformNow(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
