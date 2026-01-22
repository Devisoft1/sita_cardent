package com.example.sitacardent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

@Composable
fun App() {
    MaterialTheme {
        // Check for saved credentials immediately on startup
        var loggedInEmail by remember { 
            mutableStateOf(LocalStorage.getUser()?.first) 
        }

        if (loggedInEmail == null) {
            LoginScreen(
                onLoginSuccess = { email ->
                    loggedInEmail = email
                }
            )
        } else {
            NfcScanScreen(
                userEmail = loggedInEmail!!,
                onBackClick = {
                    // Treat back as logout: clear saved login so user is asked next time
                    LocalStorage.clearUser()
                    loggedInEmail = null
                }
            )
        }
    }
}
