package com.example.sitacardent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        // Load Remember Me state
        val rememberMe = sharedPref.getBoolean("rememberMe", false)
        cbRememberMe.isChecked = rememberMe

        // Auto-fill if Remember Me was checked
        if (rememberMe) {
            val savedEmail = sharedPref.getString("email", "")
            val savedPassword = sharedPref.getString("password", "")
            
            etEmail.setText(savedEmail)
            etPassword.setText(savedPassword)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (email == "g3goddodroad@gmail.com" && password == "dnpp@4488") {
                    // Save credentials only if Remember Me is checked
                    with(sharedPref.edit()) {
                        if (cbRememberMe.isChecked) {
                            putString("email", email)
                            putString("password", password)
                            putBoolean("rememberMe", true)
                        } else {
                            // Clear saved credentials if Remember Me is unchecked
                            remove("email")
                            remove("password")
                            putBoolean("rememberMe", false)
                        }
                        apply()
                    }
                    
                    // Navigate to NFC Scan Activity
                    val intent = Intent(this, NfcScanActivity::class.java).apply {
                        putExtra("USER_EMAIL", email)
                    }
                    startActivity(intent)
                    finish() // Optional: close Login Activity
                } else {
                    etPassword.error = "Invalid Credentials"
                }
            } else {
                if (email.isEmpty()) etEmail.error = "Enter email"
                if (password.isEmpty()) etPassword.error = "Enter password"
            }
        }
    }
}