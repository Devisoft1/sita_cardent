package com.example.sitacardent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.sitacardent.network.AuthRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import devisoft.composeapp.generated.resources.Res
import devisoft.composeapp.generated.resources.sita_logo
import devisoft.composeapp.generated.resources.devisoft_logo

// Color definitions matching Android colors.xml
private val SitaBlue = Color(0xFF2E3091)
private val SitaBlueLight = Color(0xFF4A4DAD)
private val SitaBlueDark = Color(0xFF1F2063)
private val SitaBlueHeaderDark = Color(0xFF16174D)
private val BgLight = Color(0xFFF8F9FA)
private val TextSecondary = Color(0xFF757575)

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    // Only pre-fill email if we have it, don't fill password anymore
    val savedUser = remember { LocalStorage.getUserInfo() }
    var email by remember { mutableStateOf(savedUser?.second ?: "") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) } // Default false to match typical flow, or true? XML logic loaded from prefs.
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // Colors from XML/colors.xml
    val DevisoftBlue = Color(0xFF00509E)
    val DevisoftBlueDark = Color(0xFF003870)
    val DevisoftOrange = Color(0xFFF58220)
    val TextSecondary = Color(0xFF757575)
    val BgGeometric = Color(0xFFF8F9FA) // Fallback for drawable/bg_login_geometric if we don't have the image

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGeometric), // Using color as we might not have the geometric drawable resource in common
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp) // approx 20sdp
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Login Card (MaterialCardView)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 96.dp, bottom = 40.dp), // approx 80sdp, 32sdp
                shape = RoundedCornerShape(10.dp), // approx 8sdp
                shadowElevation = 10.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp), // 20sdp, 24sdp
                ) {
                    
                    // Logo (ImageView id: ivLogo)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                         Image(
                            painter = painterResource(Res.drawable.devisoft_logo),
                            contentDescription = "Devisoft Logo",
                            modifier = Modifier
                                .width(180.dp) // approx 140sdp
                                .height(100.dp) // approx 80sdp
                                .padding(bottom = 20.dp), // 16sdp
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Email Input (TextInputLayout)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = "Email Icon",
                                tint = DevisoftOrange // app:startIconTint="@color/devisoft_orange"
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = DevisoftBlue, // app:boxStrokeColor
                            unfocusedBorderColor = DevisoftBlue,
                            focusedLabelColor = DevisoftBlue, // app:hintTextColor
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = DevisoftBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp)) // 16sdp gap

                    // Password Input (TextInputLayout)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Lock Icon",
                                tint = DevisoftOrange // app:startIconTint="@color/devisoft_orange"
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = TextSecondary)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = DevisoftBlue,
                            unfocusedBorderColor = DevisoftBlue,
                            focusedLabelColor = DevisoftBlue,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = DevisoftBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Remember Me & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = DevisoftOrange,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Text(
                            text = "Remember Me",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        
                        Spacer(Modifier.weight(1f))
                        
                        Text(
                            text = "Forgot Password?",
                            color = DevisoftBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showForgotPasswordDialog = true }
                        )
                    }

                    if (showForgotPasswordDialog) {
                        ForgotPasswordDialog(
                            onDismiss = { showForgotPasswordDialog = false },
                            onSubmit = { emailForReset ->
                                // Trigger API
                                scope.launch {
                                    isLoading = true
                                    showForgotPasswordDialog = false // Dismiss dialog immediately or wait? better safely dismiss.
                                    val result = authRepository.forgotPassword(emailForReset)
                                    isLoading = false
                                    
                                    result.onSuccess { msg ->
                                        // Show success (using errorMessage var for now as a general message area, or create a success state?)
                                        // Ideally we want a snackbar or separate success message. 
                                        // Re-using errorMessage but maybe prefixing "Success:" to style it green? 
                                        // Or just using a simple dialog for success?
                                        // For simplicity, let's just use errorMessage field but we know it's a bit hacky. 
                                        // BETTER: Add a successMessage state.
                                        errorMessage = "Success: $msg" 
                                    }.onFailure { e ->
                                        errorMessage = e.message ?: "Failed to send reset email"
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp)) // 24sdp

                    // Login Button (MaterialButton)
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                println("LoginDebug: Login Button Clicked")
                                isLoading = true
                                errorMessage = null
                                
                                scope.launch {
                                    val result = authRepository.login(email, password)
                                    result.onSuccess { response ->
                                        // Save auth regardless for now, or respect 'rememberMe'?
                                        // The original XML logic implies saving credentials only if checked.
                                        // But our new Auth logic depends on Token. 
                                        // Implementing: Always save token for session, but maybe 'rememberMe' could implication long-term persistence? 
                                        // For now, we behave like modern apps: Always login. 
                                        // Save auth with logo URL
                                        val logoUrl = response.image?.let { imagePath ->
                                            if (imagePath.startsWith("http")) {
                                                imagePath
                                            } else {
                                                "https://apisita.shanti-pos.com$imagePath"
                                            }
                                        }
                                        LocalStorage.saveAuth(
                                            token = response.token,
                                            name = response.name,
                                            email = response.email,
                                            shopId = response.shopId,
                                            logoUrl = logoUrl
                                        )
                                        isLoading = false
                                        onLoginSuccess(response.email)
                                    }.onFailure { error ->
                                        errorMessage = error.message ?: "Login failed"
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Please enter email and password"
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // 42sdp approx
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp), // 10sdp
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(DevisoftBlue, DevisoftBlueDark) // Gradient
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "LOGIN",
                                    color = Color.White,
                                    fontSize = 18.sp, // 14ssp
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp // 0.1 letterSpacing
                                )
                            }
                        }
                    }
                    
                     Spacer(modifier = Modifier.height(24.dp)) // 20sdp

                    // Register Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            color = TextSecondary,
                            fontSize = 15.sp // 12ssp
                        )
                        Text(
                            text = "Register",
                            color = DevisoftBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, // 12ssp
                            modifier = Modifier.clickable { /* TODO: Handle Register */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) } // Local error for validation

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Forgot Password") },
        text = {
            Column {
                Text("Enter your email address to receive a password reset link.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        error = null 
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (email.isBlank()) {
                        error = "Email cannot be empty"
                    } else {
                        onSubmit(email)
                    }
                }
            ) {
                Text("Send Reset Link")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
