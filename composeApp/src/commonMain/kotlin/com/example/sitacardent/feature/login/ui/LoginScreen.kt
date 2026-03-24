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
import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
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
private val DevisoftBlue = Color(0xFF00509E)
private val DevisoftBlueDark = Color(0xFF003870)
private val DevisoftBluePale = Color(0xFFF2F7FD)
private val DevisoftBlueSoft = Color(0xFFE1EBFB)
private val DevisoftOrange = Color(0xFFF58220)

private val TextSecondary = Color(0xFF757575)
private val BgLight = Color(0xFFF8F9FA)


@Composable
fun GeometricBackground() {
    val paleBlue = Color(0xFFF2F7FD)
    val softBlue = Color(0xFFE1EBFB)
    val orangeAccent = Color(0xFFF58220).copy(alpha = 0.15f)

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Base Gradient
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, paleBlue),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )

        // Top Left Abstract Shape (matching path M0,0 L280,0 C200,80 180,240 0,280 Z)
        val topLeftPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(width * 0.77f, 0f)
            cubicTo(width * 0.55f, height * 0.12f, width * 0.5f, height * 0.37f, 0f, height * 0.43f)
            close()
        }
        drawPath(topLeftPath, paleBlue)

        // Top Right Accent (matching path M360,0 L360,180 C300,160 280,60 360,0 Z)
        val topRightPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width, 0f)
            lineTo(width, height * 0.28f)
            cubicTo(width * 0.83f, height * 0.25f, width * 0.77f, height * 0.09f, width, 0f)
            close()
        }
        drawPath(topRightPath, orangeAccent)

        // Bottom Wave overlay (matching path M0,640 L360,640 L360,500 C260,580 140,480 0,560 Z)
        val bottomPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height)
            lineTo(width, height)
            lineTo(width, height * 0.78f)
            cubicTo(width * 0.72f, height * 0.9f, width * 0.38f, height * 0.75f, 0f, height * 0.87f)
            close()
        }
        drawPath(bottomPath, softBlue)
    }
}




@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    // Only pre-fill email if we have it, don't fill password anymore
    val savedUser = remember { LocalStorage.getUserInfo() }
    var email by remember { mutableStateOf(savedUser?.second ?: "") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val images = remember { LocalStorage.getImages() }
    val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })

    if (images.size > 1) {
        LaunchedEffect(Unit) {
            println("LoginDebug: LoginScreen - Starting carousel auto-scroll for ${images.size} images.")
            while (true) {
                kotlinx.coroutines.delay(20000)
                val nextPage = (pagerState.currentPage + 1) % images.size
                println("LoginDebug: LoginScreen - Auto-scrolling to page $nextPage")
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }


    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern Geometric Background
        GeometricBackground()
        
        if (images.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                val imageUrl = images.getOrNull(page)
                if (!imageUrl.isNullOrBlank()) {
                    val formattedUrl = if (imageUrl.startsWith("http")) imageUrl 
                                     else "https://apisita.shanti-pos.com$imageUrl"
                                     
                    coil3.compose.AsyncImage(
                        model = formattedUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.15f // Subtle background
                    )
                }
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header texts removed to match activity_login.xml



            // Login Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 12.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo with proper padding matching activity_login.xml
                    Image(
                        painter = painterResource(Res.drawable.devisoft_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .width(220.dp)
                            .height(110.dp)
                            .padding(bottom = 24.dp),
                        contentScale = ContentScale.Fit
                    )


                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = "Email Icon",
                                tint = DevisoftOrange
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DevisoftBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = DevisoftBlue,
                            cursorColor = DevisoftBlue
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )


                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Lock Icon",
                                tint = DevisoftOrange
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
                            focusedBorderColor = DevisoftBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = DevisoftBlue,
                            cursorColor = DevisoftBlue
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                        )
                    }

                    // Remember Me & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = DevisoftOrange
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = authRepository.login(email, password)
                                    result.onSuccess { response ->
                                         val imagesUrl = response.images?.firstOrNull()?.let { imagePath ->
                                             if (imagePath.startsWith("http")) imagePath
                                             else "https://apisita.shanti-pos.com$imagePath"
                                         }
                                         val logoUrl = response.logo?.let { logoPath ->
                                             if (logoPath.startsWith("http")) logoPath
                                             else "https://apisita.shanti-pos.com$logoPath"
                                         }
                                         LocalStorage.saveAuth(
                                             token = response.token,
                                             name = response.name,
                                             email = response.email,
                                             shopId = response.shopId,
                                             logoUrl = logoUrl,
                                             images = response.images
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
                            .height(56.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(DevisoftBlue, DevisoftBlueDark)
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
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false },
            onSubmit = { emailForReset ->
                scope.launch {
                    isLoading = true
                    showForgotPasswordDialog = false
                    val result = authRepository.forgotPassword(emailForReset)
                    isLoading = false
                    result.onSuccess { msg ->
                        errorMessage = "Success: $msg" 
                    }.onFailure { e ->
                        errorMessage = e.message ?: "Failed to send reset email"
                    }
                }
            }
        )
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
