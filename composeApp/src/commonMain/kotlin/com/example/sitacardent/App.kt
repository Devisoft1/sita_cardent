package com.example.sitacardent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.sitacardent.network.AuthRepository
import kotlinx.coroutines.launch


@Composable
fun App() {
    MaterialTheme {
        // Check for active session immediately on startup
        var isSessionActive by remember { 
            mutableStateOf(LocalStorage.getAuthToken() != null) 
        }

        val authRepository = remember { AuthRepository() }

        LaunchedEffect(isSessionActive) {
            if (isSessionActive) {
                val token = LocalStorage.getAuthToken()
                if (token != null) {
                    val result = authRepository.getProfile(token, LocalStorage.getShopUId(), LocalStorage.getShopId())
                    result.onSuccess { response ->
                        // Only update if we got meaningful data back
                        if (!response.name.isNullOrBlank()) {
                            val logoUrl = response.logo?.let { logoPath ->
                                if (logoPath.startsWith("http")) logoPath
                                else "https://apisita.shanti-pos.com$logoPath"
                            }
                            
                            LocalStorage.saveAuth(
                                token = token,
                                name = response.name,
                                email = response.email,
                                shopId = response.shopId,
                                logoUrl = logoUrl ?: LocalStorage.getLogoUrl(),
                                images = if (response.allImages.isEmpty()) LocalStorage.getImages() else response.allImages,
                                shopUId = response._id
                            )
                            println("LoginDebug: App - Profile refreshed successfully. Images: ${response.images?.size ?: 0}")
                        }
                    }


                }
            }
        }


        if (!isSessionActive) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    isSessionActive = true
                }
            )
        } else {
            val user = LocalStorage.getUserInfo()
            NfcScanScreen(
                userEmail = user?.first ?: "User", // user.first is name in Triple<Name, Email, ShopId>
                onBackClick = {
                    // Treat back as logout: clear session
                    LocalStorage.clearAuth()
                    isSessionActive = false
                }
            )
        }
    }
}
