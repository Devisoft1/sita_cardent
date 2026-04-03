package com.example.sitacardent.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.crossfade
import com.example.sitacardent.LocalStorage

import com.example.sitacardent.R
import com.example.sitacardent.network.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private var images: List<String> = emptyList()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize LocalStorage
        LocalStorage.init(this)

        // If already logged in, skip to NFC Scan
        // Note: The original code didn't check this explicitly in onCreate for the shared logic,
        // but App.kt did. We should replicate that behavior if we want auto-login.
        if (LocalStorage.getAuthToken() != null) {
            // Background refresh of shop profile data (logo, images)
            lifecycleScope.launch {
                LocalStorage.getAuthToken()?.let { token ->
                    authRepository.getProfile(token, LocalStorage.getShopUId(), LocalStorage.getShopId()).onSuccess { response ->
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
                                images = if (response.images.isNullOrEmpty()) LocalStorage.getImages() else response.images,
                                shopUId = response._id
                            )
                            android.util.Log.d("LoginDebug", "MainActivity - Profile refreshed successfully.")
                        }
                    }


                }
            }
            navigateToNfcScanAndFinish()
            return
        }


        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        images = LocalStorage.getImages()

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val llPoweredBy = findViewById<android.view.View>(R.id.llPoweredBy)

        llPoweredBy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://devisoft.co.in"))
            startActivity(intent)
        }

        // Pre-fill email/autofill logic?
        val lastUser = LocalStorage.getUserInfo()
        val isRememberMe = LocalStorage.isRememberMe()
        cbRememberMe.isChecked = isRememberMe
        
        if (isRememberMe) {
            etEmail.setText(LocalStorage.getSavedEmail() ?: "")
            etPassword.setText(LocalStorage.getSavedPassword() ?: "")
        } else if (lastUser != null) {
            etEmail.setText(lastUser.second)
        }

        val tilEmail = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword)

        // Reset errors on text change
        etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilEmail.error = null
                tilPassword.error = null // Clear general errors too if user types
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        etPassword.addTextChangedListener(object : android.text.TextWatcher {
             override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
             override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                 tilPassword.error = null
             }
             override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Clear previous errors
            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) {
                tilEmail.error = "Enter email"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tilPassword.error = "Enter password"
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
                        val logoUrl = if (response.logo?.startsWith("/") == true) {
                            "https://apisita.shanti-pos.com${response.logo}"
                        } else {
                            response.logo
                        }

                        val firstImage = response.allImages.firstOrNull()
                        val imageUrl = if (firstImage?.startsWith("/") == true) {
                            "https://apisita.shanti-pos.com$firstImage"
                        } else {
                            firstImage
                        }


                        LocalStorage.saveAuth(
                            token = response.token,
                            name = response.name,
                            email = response.email,
                            shopId = response.shopId,
                            logoUrl = logoUrl,
                            images = response.allImages,
                            shopUId = response._id
                        )


                        
                        // Handle Remember Me
                        val rememberMe = cbRememberMe.isChecked
                        LocalStorage.saveRememberMe(rememberMe)
                        if (rememberMe) {
                            LocalStorage.saveCredentials(email, password)
                        } else {
                            LocalStorage.clearSavedCredentials()
                        }
                        
                        // Toast.makeText(this@MainActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                        navigateToNfcScanAndFinish()
                    }.onFailure { error ->
                        // Show error in AlertDialog as requested
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Login Failed")
                            .setMessage(error.message ?: "Invalid email or password")
                            .setPositiveButton("OK", null)
                            .show()
                        
                        btnLogin.isEnabled = true
                        btnLogin.text = "LOGIN"
                    }
                } catch (e: Exception) {
                     com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                         .setTitle("Error")
                         .setMessage("An unexpected error occurred: ${e.message}")
                         .setPositiveButton("OK", null)
                         .show()
                     e.printStackTrace()
                     btnLogin.isEnabled = true
                     btnLogin.text = "LOGIN"
                }
            }
        }
        
        tvForgotPassword.setOnClickListener {
             // Show Forgot Password Dialog
             val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
             // Inflate the XML layout
             val inflater = android.view.LayoutInflater.from(this)
             val dialogView = inflater.inflate(R.layout.dialog_forgot_password, null)
             builder.setView(dialogView)
             
             // Get references from the inflated view
             val inputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilEmailReset)
             val input = dialogView.findViewById<TextInputEditText>(R.id.etEmailReset)

             // We need to keep a reference to the dialog to prevent auto-dismiss on positive button click if validation fails
             // However, standard AlertDialog builder positive button auto-dismisses.
             // We can override the OnClickListener after showing the dialog.
             
             // Or simpler: Use a boolean flag or just recreate the dialog logic to be custom view based if we want perfect control.
             // But let's stick to standard builder and just control logic. 
             // To prevent dismiss on error, usually we override the button listener in OnShowListener.

             builder.setPositiveButton("Send Reset Link", null) // Set null here and override in onShow
             builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
             
             val dialog = builder.create()
             
             dialog.setOnShowListener {
                 val button = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                 button.setOnClickListener {
                     val email = input.text.toString().trim()
                     // Reset error
                     inputLayout.error = null
                     
                     if (email.isEmpty()) {
                         inputLayout.error = "Email cannot be empty"
                         return@setOnClickListener
                     }

                     // Call API
                     // Disable button to prevent double click
                     button.isEnabled = false
                     button.text = "Sending..."

                     lifecycleScope.launch {
                         try {
                             val result = authRepository.forgotPassword(email)
                             result.onSuccess { msg ->
                                 dialog.dismiss()
                                 dialog.dismiss()
                                 // Show "Simple Message" (Dialog) for success as requested, using XML layout
                                 val successDialogBuilder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                                 val successView = layoutInflater.inflate(R.layout.dialog_password_reset_success, null)
                                 
                                 successDialogBuilder.setView(successView)
                                 val successDialog = successDialogBuilder.create()
                                 
                                 successView.findViewById<android.widget.Button>(R.id.btnSuccessOk).setOnClickListener {
                                     successDialog.dismiss()
                                 }
                                 
                                 successDialog.show()
                             }.onFailure { e ->
                                 inputLayout.error = e.message ?: "Failed to send email"
                                 button.isEnabled = true
                                 button.text = "Send Reset Link"
                             }
                         } catch (e: Exception) {
                             inputLayout.error = "Error: ${e.message}"
                             button.isEnabled = true
                             button.text = "Send Reset Link"
                         }
                     }
                 }
             }
             
             dialog.show()
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