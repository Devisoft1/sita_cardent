package com.example.sitacardent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

@Composable
fun App() {
    MaterialTheme {
        // Check for active session immediately on startup
        var isSessionActive by remember { 
            mutableStateOf(LocalStorage.isLoggedIn()) 
        }

        if (!isSessionActive) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    isSessionActive = true
                }
            )
        } else {
            val user = LocalStorage.getUser()
            NfcScanScreen(
                userEmail = user?.first ?: "User",
                onBackClick = {
                    // Treat back as logout: clear session but keep credentials
                    LocalStorage.setLoggedIn(false)
                    isSessionActive = false
                }
            )
        }
    }
}
