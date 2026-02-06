package com.example.sitacardent.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyMemberRequest(
    val memberId: String,
    val companyName: String
)

@Serializable
data class VerifyMemberResponse(
    val memberId: Long,
    val companyName: String,
    val validity: String,
    val currentTotal: Double
)

@Serializable
data class AddAmountRequest(
    val memberId: String,
    val amount: Double,
    val card_mfid: String
)

@Serializable
data class AddAmountResponse(
    val message: String,
    val memberId: Long? = null,
    val addedAmount: Double? = null,
    val newCardTotal: Double? = null,
    val newGlobalTotal: Double? = null
)
