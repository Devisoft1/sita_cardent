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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.jetbrains.compose.resources.painterResource
import sitacardent.composeapp.generated.resources.Res
import sitacardent.composeapp.generated.resources.sita_logo

import androidx.compose.ui.zIndex

// Color definitions matching Android colors.xml
private val SitaBlue = Color(0xFF2E3091)
private val SitaBlueLight = Color(0xFF4A4DAD)
private val SitaBlueDark = Color(0xFF1F2063)
private val SitaBlueHeaderDark = Color(0xFF16174D)
private val BgLight = Color(0xFFF8F9FA)
private val TextSecondary = Color(0xFF757575)

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val savedUser = remember { LocalStorage.getUser() }
    var email by remember { mutableStateOf(savedUser?.first ?: "") }
    var password by remember { mutableStateOf(savedUser?.second ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rememberMe by remember { mutableStateOf(savedUser != null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Image(
                    painter = painterResource(Res.drawable.sita_logo),
                    contentDescription = "SITA Logo",
                    modifier = Modifier
                        .size(200.dp)
                        .zIndex(1f),
                    contentScale = ContentScale.Fit
                )

                // Login Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp
                        ), // Reduced from 32.dp to 16.dp
                    shape = RoundedCornerShape(12.dp), // Approx 8sdp
                    shadowElevation = 12.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), // Reduced from 24.dp to 16.dp
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Email Input
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = "Email Icon",
                                    tint = SitaBlue
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = SitaBlue,
                                unfocusedBorderColor = TextSecondary,
                                focusedLabelColor = SitaBlue,
                                cursorColor = SitaBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Lock Icon",
                                    tint = SitaBlue
                                )
                            },
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff

                                val description =
                                    if (passwordVisible) "Hide password" else "Show password"

                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, description)
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = SitaBlue,
                                unfocusedBorderColor = TextSecondary,
                                focusedLabelColor = SitaBlue,
                                cursorColor = SitaBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Remember Me Checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = SitaBlue)
                            )
                            Text(
                                text = "Remember Me",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }

                        // Forgot Password
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = SitaBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { /* TODO: Handle Forgot Password */ }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Login Button with Gradient
                        Button(
                            onClick = {
                                if (email == "g3goddodroad@gmail.com" && password == "dnpp@4488") {
                                    if (rememberMe) {
                                        LocalStorage.saveUser(email, password)
                                    } else {
                                        LocalStorage.clearUser()
                                    }
                                    LocalStorage.setLoggedIn(true)
                                    onLoginSuccess(email)
                                } else {
                                    errorMessage = "Invalid Credentials"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(SitaBlue, SitaBlueDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Login",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Register Link
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Register",
                                color = SitaBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.clickable { /* TODO: Handle Register */ }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
