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

class MemberRepository {
    private val client: HttpClient = createHttpClient()
    private val baseUrl = "https://apisita.shanti-pos.com/api/members"

    suspend fun verifyMember(memberId: String, companyName: String): Result<VerifyMemberResponse> {
        println("MemberRepository: Verifying Member - ID: $memberId, Company: $companyName")
        return try {
            val responseText = client.post("$baseUrl/verify") {
                contentType(ContentType.Application.Json)
                setBody(VerifyMemberRequest(memberId, companyName))
            }.bodyAsText()
            println("MemberRepository: Raw Response: $responseText")
            val response = createJson().decodeFromString<VerifyMemberResponse>(responseText)
            println("MemberRepository: Verify Success - $response")
            Result.success(response)
        } catch (e: Exception) {
            println("MemberRepository: Verify Failed - ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addAmount(memberId: String, amount: Double, cardMfid: String): Result<AddAmountResponse> {
        println("MemberRepository: Adding Amount - ID: $memberId, Amount: $amount, Card MFID: $cardMfid")
        return try {
            val response: AddAmountResponse = client.post("$baseUrl/add-amount") {
                contentType(ContentType.Application.Json)
                setBody(AddAmountRequest(memberId, amount, cardMfid))
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
