package com.example.sitacardent.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class LoginResponse(
    val _id: String,
    val shopId: Int,
    val name: String,
    val email: String,
    val images: List<String>? = null,
    val image: List<String>? = null, // Backend uses 'image' in /api/shops/:id
    val logo: String? = null,
    val token: String
) {
    val allImages: List<String> get() = images ?: image ?: emptyList()
}

@Serializable
data class ShopProfileResponse(
    val _id: String,
    val shopId: Int,
    val name: String,
    val email: String,
    val images: List<String>? = null,
    val image: List<String>? = null, // Backend uses 'image' in /api/shops/:id
    val logo: String? = null
) {
    val allImages: List<String> get() = images ?: image ?: emptyList()
}


