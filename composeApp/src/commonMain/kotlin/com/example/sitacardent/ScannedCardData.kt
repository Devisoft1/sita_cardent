package com.example.sitacardent

data class ScannedCardData(
    val memberId: String,
    val companyName: String,
    val cardMfid: String,
    val password: String,
    val validity: String = "",
    val cardType: String = ""
)
