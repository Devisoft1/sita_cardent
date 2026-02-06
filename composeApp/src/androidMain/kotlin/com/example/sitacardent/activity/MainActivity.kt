package com.example.sitacardent.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sitacardent.LocalStorage
import com.example.sitacardent.R
import com.example.sitacardent.network.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize LocalStorage
        LocalStorage.init(this)

        // If already logged in, skip to NFC Scan
        // Note: The original code didn't check this explicitly in onCreate for the shared logic,
        // but App.kt did. We should replicate that behavior if we want auto-login.
        if (LocalStorage.getAuthToken() != null) {
            navigateToNfcScanAndFinish()
            return
        }

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe) // We might just use this for UI preference
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Pre-fill email/autofill logic?
        // Let's use LocalStorage to pre-fill email if available (like we did in Compose)
        val lastUser = LocalStorage.getUserInfo()
        if (lastUser != null) {
            etEmail.setText(lastUser.second)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Enter email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Enter password"
                return@setOnClickListener
            }

            // UI: Loading State
            btnLogin.isEnabled = false
            btnLogin.text = "LOGGING IN..."

            lifecycleScope.launch {
                try {
                    val result = authRepository.login(email, password)
                    
                    result.onSuccess { response ->
                        // Save Auth Data
                        LocalStorage.saveAuth(
                            token = response.token,
                            name = response.name,
                            email = response.email,
                            shopId = response.shopId
                        )
                        
                        Toast.makeText(this@MainActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                        navigateToNfcScanAndFinish()
                    }.onFailure { error ->
                        Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        btnLogin.isEnabled = true
                        btnLogin.text = "LOGIN"
                    }
                } catch (e: Exception) {
                     Toast.makeText(this@MainActivity, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
                     e.printStackTrace()
                     btnLogin.isEnabled = true
                     btnLogin.text = "LOGIN"
                }
            }
        }
        
        tvForgotPassword.setOnClickListener {
             Toast.makeText(this, "Forgot Password feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        tvRegister.setOnClickListener {
             Toast.makeText(this, "Register feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToNfcScanAndFinish() {
        val user = LocalStorage.getUserInfo()
        val intent = Intent(this, NfcScanActivity::class.java).apply {
            putExtra("USER_EMAIL", user?.second ?: "")
        }
        startActivity(intent)
        finish()
    }
}