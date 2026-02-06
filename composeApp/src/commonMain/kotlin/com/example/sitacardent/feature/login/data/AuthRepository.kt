package com.example.sitacardent.network

import com.example.sitacardent.model.LoginRequest
import com.example.sitacardent.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
}
