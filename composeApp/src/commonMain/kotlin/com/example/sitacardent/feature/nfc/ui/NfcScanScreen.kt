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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.sitacardent.network.MemberRepository

import com.example.sitacardent.network.AuthRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import devisoft.composeapp.generated.resources.Res
import devisoft.composeapp.generated.resources.sita_logo


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
    val cardMfid: String,
    val password: String,
    val validity: String = "",
    val cardType: String = ""
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NfcScanScreen(
    userEmail: String,
    onBackClick: () -> Unit,
    isExternalScanning: Boolean? = null,
    onExternalScanRequest: (() -> Unit)? = null,
    onExternalScanCancel: (() -> Unit)? = null,
    externalScannedData: ScannedCardData? = null,
    onExternalDataConsumed: () -> Unit = {},
    externalScanError: String? = null,
    onExternalErrorConsumed: () -> Unit = {},
    onLogoLongClick: (() -> Unit)? = null // Added for secret settings maybe?
) {
    var isScanningInternal by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var imageUrl by remember { mutableStateOf(LocalStorage.getImageUrl()) }
    var logoUrlValue by remember { mutableStateOf(LocalStorage.getLogoUrl()) }

    val authRepository = remember { AuthRepository() }

    var images by remember { mutableStateOf(LocalStorage.getImages()) }


    LaunchedEffect(Unit) {
        val token = LocalStorage.getAuthToken()
        if (token != null) {
            authRepository.getProfile(token).onSuccess { response ->
                val backendImages = response.images ?: emptyList()
                val formattedImages = backendImages.map { imagePath ->
                    if (imagePath.startsWith("http")) imagePath
                    else "https://apisita.shanti-pos.com$imagePath"
                }
                
                if (formattedImages.isNotEmpty()) {
                    images = formattedImages
                    imageUrl = formattedImages.firstOrNull()
                }

                val newLogoUrl = response.logo?.let { logoPath ->
                    if (logoPath.startsWith("http")) logoPath
                    else "https://apisita.shanti-pos.com$logoPath"
                }

                logoUrlValue = newLogoUrl

                LocalStorage.saveAuth(
                    token = token,
                    name = response.name,
                    email = response.email,
                    shopId = response.shopId,
                    logoUrl = newLogoUrl,
                    images = response.images
                )
            }
        }
    }

    var invoiceAmount by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // API State
    val scope = rememberCoroutineScope()
    val repository = remember { MemberRepository() }
    
    // Member Data

    
    var verifiedMemberId by remember { mutableStateOf<Long?>(null) }
    var verifiedCompanyName by remember { mutableStateOf<String?>(null) }
    var verifiedCardMfid by remember { mutableStateOf<String?>(null) }
    var memberValidity by remember { mutableStateOf<String?>(null) }
    var memberCurrentTotal by remember { mutableStateOf<Double?>(null) }
    
    var showSuccessDialog by remember { mutableStateOf(false) }

    
    // Use external scanning state if provided, otherwise local
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

    fun resetState() {
        apiError = null
        successMessage = null
        verifiedMemberId = null
        verifiedCompanyName = null
        verifiedCardMfid = null
        memberValidity = null
        memberCurrentTotal = null
        invoiceAmount = ""
        password = ""
    }

    fun verifyMember(
        idToVerify: String,
        companyToVerify: String,
        cardMfid: String,
        password: String,
        cardValidity: String = "",
        cardType: String = ""
    ) {
        if (idToVerify.isBlank() || companyToVerify.isBlank()) {
            apiError = "Please enter Member ID and Company Name"
            return
        }
        
        isLoading = true
        apiError = null
        successMessage = null
        
        scope.launch {
            // Pass password to verifyMember along with card details
            val result = repository.verifyMember(
                memberId = idToVerify,
                companyName = companyToVerify,
                password = password,
                cardMfid = cardMfid,
                cardValidity = cardValidity,
                cardType = cardType
            )
            result.onSuccess { response ->
                verifiedMemberId = response.memberId
                verifiedCompanyName = response.companyName
                verifiedCardMfid = cardMfid
                memberValidity = response.validity
                println("DEBUG: API Response Validity: ${response.validity}")
                memberCurrentTotal = response.currentTotal
                isLoading = false
            }.onFailure { e ->
                // Fallback: If verification failed (likely due to company/password mismatch from bad card write),
                // try to fetch member by ID directly.
                println("Verify failed ($e), attempting fallback search for ID: $idToVerify")
                val searchResult = repository.getMemberById(idToVerify)
                
                searchResult.onSuccess { member ->
                    val fullName = member.companyName ?: ""
                    
                    // If the name from search is different (likely full vs truncated), retry verification
                    if (fullName != companyToVerify) {
                        println("Retrying verification with full name: $fullName")
                        scope.launch {
                            val retryResult = repository.verifyMember(
                                memberId = idToVerify,
                                companyName = fullName,
                                password = password,
                                cardMfid = cardMfid,
                                cardValidity = cardValidity,
                                cardType = cardType
                            )
                            retryResult.onSuccess { retryResponse ->
                                println("Retry verification success!")
                                verifiedMemberId = retryResponse.memberId
                                verifiedCompanyName = retryResponse.companyName
                                verifiedCardMfid = cardMfid
                                memberValidity = retryResponse.validity
                                memberCurrentTotal = retryResponse.currentTotal
                                isLoading = false
                            }.onFailure { retryError ->
                                println("Retry verification failed: ${retryError.message}")
                                apiError = "Card Not Registered: ${retryError.message}"
                                verifiedMemberId = null
                                isLoading = false
                            }
                        }
                    } else {
                        apiError = "Verification Rejected: ${e.message}"
                        verifiedMemberId = null
                        isLoading = false
                    }
                }.onFailure { fallbackError ->
                     apiError = "Verification Failed: ${e.message}"
                     verifiedMemberId = null
                     verifiedCardMfid = null
                     isLoading = false
                }
            }
        }
    }

    // Handle externally scanned data
    LaunchedEffect(externalScannedData) {
        externalScannedData?.let { data ->
            // Pass data directly to ensure immediate verification
            verifyMember(
                idToVerify = data.memberId,
                companyToVerify = data.companyName,
                cardMfid = data.cardMfid,
                password = data.password,
                cardValidity = data.validity,
                cardType = data.cardType
            )
            password = data.password
            onExternalDataConsumed()
        }
    }

    // Handle external scan errors (e.g. Empty Card)
    LaunchedEffect(externalScanError) {
        externalScanError?.let { error ->
            apiError = error
            isLoading = false
            onExternalErrorConsumed()
        }
    }

    fun addAmount() {
        if (verifiedMemberId == null) {
            apiError = "Please verify member first"
            return
        }
        
        val amount = invoiceAmount.toDoubleOrNull()?.toInt()
        if (amount == null || amount <= 0) {
            apiError = "Please enter a valid amount"
            return
        }

        if (password.isBlank()) {
            apiError = "Card authentication failed (Missing Password)"
            return
        }

        isLoading = true
        apiError = null
        successMessage = null

        scope.launch {
            val result = repository.addAmount(verifiedMemberId.toString(), amount, verifiedCardMfid ?: "", password)
            result.onSuccess { response ->
                successMessage = "Success! New Total: ${formatAmount(response.newCardTotal)}"
                memberCurrentTotal = response.newCardTotal
                invoiceAmount = ""
                password = ""
                isLoading = false
                showSuccessDialog = true
         
                // Wait for 2 seconds then reset the screen
                delay(2000)
                resetState() 
                
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
            .imePadding()
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
                // Header background simplified to just the image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    // Background Image Carousel
                    val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })

                    LaunchedEffect(images) {
                        if (images.size > 1) {
                            println("LoginDebug: NfcScanScreen - Starting carousel auto-scroll for ${images.size} images.")
                            while (true) {
                                delay(20000) // 20 seconds
                                val nextPage = (pagerState.currentPage + 1) % images.size
                                println("LoginDebug: NfcScanScreen - Auto-scrolling to page $nextPage")
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    }


                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val currentImageUrl = images.getOrNull(page) ?: imageUrl
                        if (!currentImageUrl.isNullOrBlank()) {
                             coil3.compose.AsyncImage(
                                model = currentImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                alpha = 0.8f 
                            )
                        }
                    }




                    // Content Container with Safe Area Padding
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Username title matching layout_app_bar_main.xml (18sp)
                            // Using SitaBlue for visibility on white background
                            Text(
                                text = displayName,
                                color = SitaBlue,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.05.sp
                            )

                            // Logout Button matching layout_app_bar_main.xml
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Color.Red,
                                    modifier = Modifier.size(28.dp).padding(4.dp)
                                )
                            }
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
                                if (apiError != null || successMessage != null) {
                                    resetState()
                                } else if (onExternalScanRequest != null) {
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
                    coil3.compose.AsyncImage(
                        model = logoUrlValue,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.Center),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,

                        placeholder = painterResource(Res.drawable.sita_logo),
                        error = painterResource(Res.drawable.sita_logo),
                        fallback = painterResource(Res.drawable.sita_logo)
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

            if (isLoading || isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                        .size(40.dp),
                    color = SitaBlue,
                    strokeWidth = 4.dp
                )
            }

            if (apiError != null) {
                Button(
                    onClick = {
                        resetState()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }

            if (isScanning && apiError == null && successMessage == null && !showMemberInfo) {
                TextButton(
                    onClick = {
                        if (onExternalScanCancel != null) {
                            onExternalScanCancel()
                        } else {
                            isScanningInternal = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    Text("Stop Scanning", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }



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
                        MemberInfoRow("Current Balance", formatAmount(memberCurrentTotal))
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
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )

                        Spacer(Modifier.height(10.dp))



                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            TextButton(
                                onClick = {
                                    resetState()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 12.sp,
                                    color = Color.Red
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
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp),
                                enabled = !isLoading
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(SitaBlue, SitaBlueDark)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Confirm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success") },
            text = { Text("Amount added successfully!\nNew Balance: ${formatAmount(memberCurrentTotal)}") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


fun formatAmount(amount: Double?): String {
    if (amount == null) return "0.00"
    // Use a simple manual formatting for common module
    val rounded = ((amount * 100.0).toLong() / 100.0)
    val parts = rounded.toString().split(".")
    val integerPart = parts[0]
    var decimalPart = if (parts.size > 1) parts[1] else "00"
    
    // Ensure two decimal places
    if (decimalPart.length < 2) decimalPart += "0"
    else if (decimalPart.length > 2) decimalPart = decimalPart.substring(0, 2)
    
    // Add commas for thousands
    val regex = "(\\d)(?=(\\d{3})+(?!\\d))".toRegex()
    val formattedInteger = integerPart.replace(regex, "$1,")
    
    return "$formattedInteger.$decimalPart"
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
        // Handle ISO format (2036-02-03T...) by taking first part
        val cleanDate = dateString.split("T")[0]
        val parts = cleanDate.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
