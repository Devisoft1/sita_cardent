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
import com.example.sitacardent.network.MemberRepository
import kotlinx.coroutines.launch
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

data class ScannedCardData(
    val memberId: String,
    val companyName: String,
    val cardMfid: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NfcScanScreen(
    userEmail: String,
    onBackClick: () -> Unit,
    isExternalScanning: Boolean? = null,
    onExternalScanRequest: (() -> Unit)? = null,
    externalScannedData: ScannedCardData? = null,
    onExternalDataConsumed: () -> Unit = {},
    onLogoLongClick: (() -> Unit)? = null
) {
    var invoiceAmount by remember { mutableStateOf("") }
    
    // API State
    val scope = rememberCoroutineScope()
    val repository = remember { MemberRepository() }
    var isLoading by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Member Data

    
    var verifiedMemberId by remember { mutableStateOf<Long?>(null) }
    var verifiedCompanyName by remember { mutableStateOf<String?>(null) }
    var verifiedCardMfid by remember { mutableStateOf<String?>(null) }
    var memberValidity by remember { mutableStateOf<String?>(null) }
    var memberCurrentTotal by remember { mutableStateOf<Double?>(null) }
    
    // Use external scanning state if provided, otherwise local
    var isScanningInternal by remember { mutableStateOf(false) }
    val isScanning = isExternalScanning ?: isScanningInternal
    
    var isTimeout by remember { mutableStateOf(false) }

    // Timeout logic: Stop scanning after 1 minute
    LaunchedEffect(isScanning) {
        if (isScanning) {
            isTimeout = false
            delay(60_000L) // 1 minute
            // Reset appropriate state
            if (isScanning) {
                if (isExternalScanning == null) {
                    isScanningInternal = false
                }
                isTimeout = true
            }
        }
    }



    val displayName = userEmail.substringBefore("@")
    val showMemberInfo = verifiedMemberId != null

    fun verifyMember(
        idToVerify: String,
        companyToVerify: String,
        cardMfid: String
    ) {
        if (idToVerify.isBlank() || companyToVerify.isBlank()) {
            apiError = "Please enter Member ID and Company Name"
            return
        }
        
        isLoading = true
        apiError = null
        successMessage = null
        
        scope.launch {
            val result = repository.verifyMember(idToVerify, companyToVerify)
            result.onSuccess { response ->
                verifiedMemberId = response.memberId
                verifiedCompanyName = response.companyName
                verifiedCardMfid = cardMfid
                memberValidity = response.validity
                println("DEBUG: API Response Validity: ${response.validity}")
                memberCurrentTotal = response.currentTotal
                isLoading = false
            }.onFailure { e ->
                apiError = "Verification Failed: ${e.message}"
                verifiedMemberId = null
                verifiedCardMfid = null
                isLoading = false
            }
        }
    }

    // Handle externally scanned data
    LaunchedEffect(externalScannedData) {
        externalScannedData?.let { data ->
            // Pass data directly to ensure immediate verification
            verifyMember(data.memberId, data.companyName, data.cardMfid)
            onExternalDataConsumed()
        }
    }

    fun addAmount() {
        if (verifiedMemberId == null) {
            apiError = "Please verify member first"
            return
        }
        
        val amount = invoiceAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            apiError = "Please enter a valid amount"
            return
        }

        isLoading = true
        apiError = null
        successMessage = null

        scope.launch {
            val result = repository.addAmount(verifiedMemberId.toString(), amount, verifiedCardMfid ?: "")
            result.onSuccess { response ->
                successMessage = "Success! New Total: ${response.newCardTotal}"
                memberCurrentTotal = response.newCardTotal
                invoiceAmount = ""
                isLoading = false
            }.onFailure { e ->
                apiError = "Transaction Failed: ${e.message}"
                isLoading = false
            }
        }
    }

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
                        .combinedClickable(
                            onClick = {
                                if (onExternalScanRequest != null) {
                                    onExternalScanRequest()
                                } else {
                                    isScanningInternal = !isScanningInternal // Toggle scanning for demo
                                }
                            },
                            onLongClick = {
                                onLogoLongClick?.invoke()
                            }
                        )
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
            
            Text(
                text = when {
                    isLoading -> "Processing..."
                    successMessage != null -> "Transaction Successful"
                    apiError != null -> apiError ?: "Error"
                    isTimeout -> "No card detected\nPlease try again"
                    isScanning -> "Searching for card...\nHold it near the back"
                    else -> "Ready to Verify\nTap logo to scan"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = when {
                    apiError != null || isTimeout -> Color.Red
                    successMessage != null -> StatusGreen
                    else -> TextSecondary
                }
            )



            /* ================= MEMBER DETAILS CARD ================= */

            if (showMemberInfo) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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

                        MemberInfoRow("Member ID", verifiedMemberId.toString())
                        Spacer(Modifier.height(4.dp))
                        MemberInfoRow("Company Name", verifiedCompanyName ?: "--")
                        Spacer(Modifier.height(4.dp))
                        MemberInfoRow("Expiry Date", formatDate(memberValidity ?: "--"))
                        Spacer(Modifier.height(4.dp))
                        MemberInfoRow("Current Balance", memberCurrentTotal.toString())
                    }
                }
            }

            /* ================= TRANSACTION CARD ================= */

            if (showMemberInfo) {
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
                            text = "Add Amount",
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
                                    // Resetting verified status logic if needed
                                    verifiedMemberId = null
                                    successMessage = null
                                    apiError = null
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
                                onClick = { addAmount() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(6.dp),
                                elevation = ButtonDefaults.buttonElevation(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SitaBlue
                                ),
                                enabled = !isLoading
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

fun formatDate(dateString: String): String {
    println("DEBUG: formatDate input: '$dateString'")
    if (dateString == "--" || dateString.isBlank()) return dateString
    return try {
        // As api returns YYYY-MM-DD
        val parts = dateString.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
