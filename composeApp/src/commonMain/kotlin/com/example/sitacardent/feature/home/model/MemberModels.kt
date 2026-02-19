package com.example.sitacardent.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyMemberRequest(
    val memberId: String,
    val companyName: String,
    val password: String
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
    val card_mfid: String,
    val password: String
)

@Serializable
data class AddAmountResponse(
    val message: String,
    val memberId: Long? = null,
    val addedAmount: Double? = null,
    val newCardTotal: Double? = null,
    val newGlobalTotal: Double? = null
)

@Serializable
data class SearchMemberResponse(
    val members: List<MemberDto>,
    val total: Int,
    val totalPages: Int,
    val currentPage: Int
)

@Serializable
data class MemberDto(
    val _id: String,
    val memberId: Long,
    val companyName: String,
    val password: String? = null, // In logs it appeared inside cards array or root? Logs show root password for some, but let's check.
    // Actually logs for 1010 show `cards: [{..., password: "demo1234"}]`.
    // But logs for 1009 show `password` in Verify Request.
    // The search response for 1010 (Step 66) shows `cards` array with password.
    val cards: List<CardDto>? = null,
    val validity: String? = null,
    val total: Double? = null
)

@Serializable
data class CardDto(
    val card_mfid: String,
    val password: String
)
