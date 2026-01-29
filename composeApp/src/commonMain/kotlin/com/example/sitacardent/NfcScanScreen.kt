package com.example.sitacardent

import androidx.compose.foundation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.painterResource
import sitacardent.composeapp.generated.resources.Res
import sitacardent.composeapp.generated.resources.sita_logo

// Android color constants - matching colors.xml exactly
private val SitaBlue = Color(0xFF2E3091)
private val SitaBlueDark = Color(0xFF16174D)
private val BgLight = Color(0xFFF8F9FA)
private val TextSecondary = Color(0xFF757575)
private val StatusGreen = Color(0xFF4CAF50)
private val DividerColor = Color(0xFFEEEEEE)

@Composable
fun NfcScanScreen(
    userEmail: String,
    onBackClick: () -> Unit
) {
    var invoiceAmount by remember { mutableStateOf("") }
    // Sample data for testing - matching Android preview (set to null to hide Member Verified card)
    var memberId by remember { mutableStateOf<String?>("12345") }
    var companyName by remember { mutableStateOf<String?>("Devisoft") }
    var validUpto by remember { mutableStateOf<String?>("31/12/2026") }
    var isScanning by remember { mutableStateOf(false) }
    var isTimeout by remember { mutableStateOf(false) }

    // Timeout logic: Stop scanning after 1 minute
    LaunchedEffect(isScanning) {
        if (isScanning) {
            isTimeout = false
            delay(60_000L) // 1 minute
            if (isScanning) {
                isScanning = false
                isTimeout = true
            }
        }
    }

    val displayName = userEmail.substringBefore("@")
    val showMemberInfo = memberId != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            /* ================= HEADER WITH LOGO ================= */
            
            // The header section with overlapping logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Blue gradient header background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SitaBlue,
                                    SitaBlueDark
                                ),
                                start = Offset.Zero,
                                end = Offset(1000f, 1000f)
                            )
                        )
                ) {
                    // Content Container with Safe Area Padding
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        // Back Button
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 8.dp)
                                .size(52.dp)
                                .zIndex(2f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Username title
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.05.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 20.dp)
                        )

                        // Logout Button
                        IconButton(
                            onClick = onBackClick, // Using onBackClick as it already handles logout logic in App.kt
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 8.dp)
                                .size(52.dp)
                                .zIndex(2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Logo positioned to overlap header and content
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 160.dp)
                        .size(180.dp)
                        .zIndex(10f)
                        .clickable { 
                            isScanning = true 
                            isTimeout = false
                        }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.sita_logo),
                        contentDescription = "Tap to Scan",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }

            /* ============ INSTRUCTION TEXT ============ */

            // Spacer to account for logo overlap
            
            Text(
                text = when {
                    isTimeout -> "No card detected\nPlease try again"
                    isScanning -> "Searching for card...\nHold it near the back"
                    else -> "Ready to Scan\nTap the logo and bring card close"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (isTimeout) Color.Red else TextSecondary
            )

            if (isScanning) {
                TextButton(
                    onClick = { isScanning = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Cancel Scanning",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            /* ================= MEMBER DETAILS CARD ================= */

            if (showMemberInfo) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    border = BorderStroke(1.dp, DividerColor),
                    colors = CardDefaults.cardColors(Color.White)
                ) {
                    Column(Modifier.padding(10.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Member Verified",
                                color = StatusGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(StatusGreen, CircleShape)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = DividerColor
                        )

                        MemberInfoRow("Member ID", memberId ?: "--")
                        Spacer(Modifier.height(4.dp))
                        MemberInfoRow("Company Name", companyName ?: "--")
                        Spacer(Modifier.height(4.dp))
                        MemberInfoRow("Expiry Date", validUpto ?: "--")
                    }
                }
            }

            /* ================= TRANSACTION CARD ================= */

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 32.dp
                    ),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(Modifier.padding(10.dp)) {

                    Text(
                        text = "Invoice Amount",
                        color = SitaBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = invoiceAmount,
                        onValueChange = { invoiceAmount = it },
                        label = { Text("Enter Amount") },
                        leadingIcon = {
                            Text(
                                text = "Rs.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SitaBlue
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SitaBlue,
                            unfocusedBorderColor = SitaBlue,
                            focusedLabelColor = SitaBlue,
                            unfocusedLabelColor = SitaBlue
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        TextButton(
                            onClick = {
                                invoiceAmount = ""
                                memberId = null
                                companyName = null
                                validUpto = null
                                isScanning = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { 
                                isScanning = true 
                                isTimeout = false
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            shape = RoundedCornerShape(6.dp),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SitaBlue
                            )
                        ) {
                            Text(
                                text = "Confirm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
