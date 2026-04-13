package com.example.sitacardent

expect object DateUtils {
    fun isCardExpired(validity: String): Boolean
}
