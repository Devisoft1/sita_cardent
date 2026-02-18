package com.example.sitacardent.network

import com.example.sitacardent.model.AddAmountRequest
import com.example.sitacardent.model.AddAmountResponse
import com.example.sitacardent.model.VerifyMemberRequest
import com.example.sitacardent.model.VerifyMemberResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

class MemberRepository {
    private val client: HttpClient = createHttpClient()
    private val baseUrl = "https://apisita.shanti-pos.com/api/members"

    suspend fun verifyMember(memberId: String, companyName: String, password: String): Result<VerifyMemberResponse> {
        println("MemberRepository: Verifying Member - ID: $memberId, Company: $companyName, Password: [PROTECTED]")
        return try {
            val response = client.post("$baseUrl/verify") {
                contentType(ContentType.Application.Json)
                setBody(VerifyMemberRequest(memberId, companyName, password))
            }
            
            if (response.status.value in 200..299) {
                val responseBody = response.body<VerifyMemberResponse>()
                println("MemberRepository: Verify Success - $responseBody")
                Result.success(responseBody)
            } else {
                val errorBody = response.bodyAsText()
                println("MemberRepository: Verify Failed (Status: ${response.status}) - $errorBody")
                
                // Try to parse "message" from error response
                val errorMessage = try {
                    val jsonObject = createJson().decodeFromString<JsonObject>(errorBody)
                    // Remove quotes if present from the string representation
                    jsonObject["message"]?.toString()?.trim('"') 
                        ?: "Server error: ${response.status.description}"
                } catch (e: Exception) {
                    println("Json Parse Error: ${e.message}")
                    "Error: ${response.status.description}"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("MemberRepository: Verify Failed - ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addAmount(memberId: String, amount: Double, cardMfid: String, password: String): Result<AddAmountResponse> {
        println("MemberRepository: Adding Amount - ID: $memberId, Amount: $amount, Card MFID: $cardMfid, Password: [PROTECTED]")
        return try {
            val response: AddAmountResponse = client.post("$baseUrl/add-amount") {
                contentType(ContentType.Application.Json)
                setBody(AddAmountRequest(memberId, amount, cardMfid, password))
            }.body()
            println("MemberRepository: Response received: $response")
            if (response.memberId != null) {
                println("MemberRepository: Add Amount Success - $response")
                Result.success(response)
            } else {
                println("MemberRepository: Add Amount Failed (Server Message) - ${response.message}")
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            println("MemberRepository: Add Amount Failed - ${e.message}")
            Result.failure(e)
        }
    }
}
