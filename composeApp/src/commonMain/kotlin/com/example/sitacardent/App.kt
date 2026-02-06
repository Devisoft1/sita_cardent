package com.example.sitacardent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

@Composable
fun App() {
    MaterialTheme {
        // Check for active session immediately on startup
        var isSessionActive by remember { 
            mutableStateOf(LocalStorage.getAuthToken() != null) 
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
