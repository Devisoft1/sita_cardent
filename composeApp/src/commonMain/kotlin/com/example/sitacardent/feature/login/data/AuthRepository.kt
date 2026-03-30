package com.example.sitacardent.network

import com.example.sitacardent.model.LoginRequest
import com.example.sitacardent.model.LoginResponse
import com.example.sitacardent.model.ForgotPasswordRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.example.sitacardent.model.ShopProfileResponse
import kotlinx.serialization.json.JsonObject


class AuthRepository {
    private val client: HttpClient = createHttpClient()
    private val baseUrl = "https://apisita.shanti-pos.com/api/shop-auth"

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        println("LoginDebug: AuthRepository - POST request to $baseUrl/login")
        println("LoginDebug: AuthRepository - Payload Email: $email")
        return try {
            val response = client.post("$baseUrl/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }

            if (response.status.value in 200..299) {
                val responseBody = response.body<LoginResponse>()
                println("LoginDebug: AuthRepository - Success! User: ${responseBody.name}, ID: ${responseBody.shopId}")
                Result.success(responseBody)
            } else {
                val errorBody = response.bodyAsText()
                println("LoginDebug: AuthRepository - Failed with status code: ${response.status}")
                println("LoginDebug: AuthRepository - Failure Body: $errorBody")
                
                // Try to parse "message" from error response
                val errorMessage = try {
                    val jsonObject = createJson().decodeFromString<JsonObject>(errorBody)
                     // Remove quotes if present
                    jsonObject["message"]?.toString()?.trim('"') 
                        ?: "Login failed: ${response.status.description}"
                } catch (e: Exception) {
                    println("LoginDebug: Json Parse Error during failure handling: ${e.message}")
                    "Error: ${response.status.description}"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("LoginDebug: AuthRepository - EXCEPTION detected: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }

    }

    suspend fun forgotPassword(email: String): Result<String> {
        println("LoginDebug: AuthRepository - POST request to $baseUrl/forgot-password")
        println("LoginDebug: AuthRepository - Payload Email: $email")
        return try {
            val response = client.post("$baseUrl/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(ForgotPasswordRequest(email))
            }

            if (response.status.value in 200..299) {
                println("LoginDebug: AuthRepository - Forgot Password Success!")
                Result.success("Password reset email sent successfully")
            } else {
                val errorBody = response.bodyAsText()
                println("LoginDebug: AuthRepository - Failed with status code: ${response.status}")
                println("LoginDebug: AuthRepository - Failure Body: $errorBody")

                // Try to parse "message" from error response
                val errorMessage = try {
                    val jsonObject = createJson().decodeFromString<JsonObject>(errorBody)
                    // Remove quotes if present
                    jsonObject["message"]?.toString()?.trim('"')
                        ?: "Request failed: ${response.status.description}"
                } catch (e: Exception) {
                    println("LoginDebug: Json Parse Error during failure handling: ${e.message}")
                    "Error: ${response.status.description}"
                }

                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("LoginDebug: AuthRepository - EXCEPTION detected: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun getProfile(token: String, shopUId: String? = null, shopId: Int? = null): Result<ShopProfileResponse> {
        println("LoginDebug: AuthRepository - Fetching profile. shopId: $shopId, shopUId: $shopUId")
        return try {
            // Try 1: Specific endpoint with Numeric Shop ID if available (Highest priority for /api/shops/:id)
            if (shopId != null && shopId != 0) {
                val url = "https://apisita.shanti-pos.com/api/shops/$shopId"
                println("LoginDebug: AuthRepository - Trying shopId URL: $url")
                val response = client.get(url) {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.value in 200..299) {
                    val body = response.body<ShopProfileResponse>()
                    println("LoginDebug: AuthRepository - Profile Success via shopId $url! User: ${body.name}")
                    return Result.success(body)
                }
                println("LoginDebug: AuthRepository - shopId URL failed with ${response.status}")
            }

            // Try 2: Specific endpoint with Shop UID if available
            if (!shopUId.isNullOrBlank()) {
                val urls = listOf("$baseUrl/$shopUId", "https://apisita.shanti-pos.com/api/shops/$shopUId")
                for (url in urls) {
                    // Check if it's a numeric path and skip if shopUId is not numeric
                    if (url.contains("/api/shops/") && shopUId.toIntOrNull() == null) {
                        println("LoginDebug: AuthRepository - Skipping numeric shop URL for hex UID: $url")
                        continue
                    }
                    
                    println("LoginDebug: AuthRepository - Trying sID URL: $url")
                    val response = client.get(url) {
                        header("Authorization", "Bearer $token")
                    }
                    if (response.status.value in 200..299) {
                        val body = response.body<ShopProfileResponse>()
                        println("LoginDebug: AuthRepository - Profile Success via $url! User: ${body.name}")
                        return Result.success(body)
                    }
                }
            }

            // Try 3: Standard "me" or "profile" endpoints
            val fallbacks = listOf("$baseUrl/profile", "$baseUrl/me", "https://apisita.shanti-pos.com/api/shops/me")
            for (url in fallbacks) {
                println("LoginDebug: AuthRepository - Trying fallback: $url")
                val response = client.get(url) {
                    header("Authorization", "Bearer $token")
                }
                if (response.status.value in 200..299) {
                    val body = response.body<ShopProfileResponse>()
                    println("LoginDebug: AuthRepository - Profile Success via fallback $url! User: ${body.name}")
                    return Result.success(body)
                }
            }

            // Final fallback: try just the base URL
            println("LoginDebug: AuthRepository - Final fallback: $baseUrl")
            val rootResponse = client.get(baseUrl) {
                header("Authorization", "Bearer $token")
            }
            
            if (rootResponse.status.value in 200..299) {
                val rootBody = rootResponse.body<ShopProfileResponse>()
                println("LoginDebug: AuthRepository - Root Profile Success! User: ${rootBody.name}")
                Result.success(rootBody)
            } else {
                println("LoginDebug: AuthRepository - All profile endpoints failed.")
                Result.failure(Exception("Failed to fetch profile"))
            }

        } catch (e: Exception) {
            println("LoginDebug: AuthRepository - Profile EXCEPTION: ${e.message}")
            Result.failure(e)
        }
    }

}


