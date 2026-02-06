package com.example.sitacardent.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val _id: String,
    val shopId: Int,
    val name: String,
    val email: String,
    val image: String? = null,
    val token: String
)
