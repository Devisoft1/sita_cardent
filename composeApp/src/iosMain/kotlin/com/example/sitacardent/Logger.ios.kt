package com.example.sitacardent

import platform.Foundation.NSLog
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun platformLog(tag: String, message: String) {
    NSLog("[%s] %s", tag, message)
}

actual fun platformNow(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm:ss"
    return formatter.stringFromDate(NSDate())
}
