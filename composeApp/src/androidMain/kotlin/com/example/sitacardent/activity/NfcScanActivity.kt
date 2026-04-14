package com.example.sitacardent.activity

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.request.error
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sitacardent.LocalStorage
import com.example.sitacardent.network.AuthRepository
import kotlinx.coroutines.launch

import com.example.sitacardent.R
import com.example.sitacardent.ScannedCardData
import com.example.sitacardent.network.MemberRepository
import com.example.sitacardent.model.VerifyMemberResponse
import com.example.sitacardent.DateUtils
import com.google.android.material.textfield.TextInputEditText
import java.nio.charset.Charset
import android.os.Handler
import android.os.Looper


class NfcScanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NfcScanActivity"
    }

    // State
    private var isScanning = false
    private var scannedData: ScannedCardData? = null
    private var scanError: String? = null
    private var pendingWriteAmount: String? = null
    private var lastScannedTag: Tag? = null
    private var lastTagIdString: String? = null
    private var lastTagTimestamp: Long = 0
    
    // NFC Adapter
    private var nfcAdapter: NfcAdapter? = null
    private val repository = MemberRepository()

    // UI Elements
    private lateinit var tvDisplayName: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var imgLogo: ImageView
    private lateinit var imgBackgroundLogo: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var btnStopScanning: Button
    private lateinit var scrollView: ScrollView
    
    private lateinit var cvMemberDetails: View


    private var images: List<String> = emptyList()
    private var currentImageIndex = 0
    private val carouselHandler = Handler(Looper.getMainLooper())
    private val carouselRunnable = object : Runnable {
        override fun run() {
            if (images.size > 1) {
                currentImageIndex = (currentImageIndex + 1) % images.size
                Log.d("LoginDebug", "NfcScanActivity - 10 seconds passed, rotating to image index $currentImageIndex")
                updateCarouselImage()
            }
            carouselHandler.postDelayed(this, 10000) // 10 seconds
        }
    }

    private lateinit var tvMemberId: TextView
    private lateinit var tvMemberName: TextView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvExpiryDate: TextView
    private lateinit var tvCurrentBalance: TextView
    
    // Transaction UI
    private lateinit var cvTransaction: View
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button
    private lateinit var pbLoader: View

    // Current Member Data
    private var currentVerifiedMemberId: Long? = null
    private var currentVerifiedCardMfid: String? = null
    private var currentPassword = ""

    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize LocalStorage
        LocalStorage.init(this)

        setContentView(R.layout.activity_nfc_scan)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "User"
        val displayName = userEmail.substringBefore("@")

        initViews()
        
        tvDisplayName.text = displayName
        
        // Load Branding from LocalStorage
        val logoUrl = LocalStorage.getLogoUrl()
        val initialImageUrl = LocalStorage.getImageUrl()
        images = LocalStorage.getImages()
        
        Log.d("LoginDebug", "NfcScanActivity - Stored Logo: $logoUrl")
        Log.d("LoginDebug", "NfcScanActivity - Stored Image: $initialImageUrl")
        Log.d("LoginDebug", "NfcScanActivity - Stored Images Count: ${images.size}")

        if (!logoUrl.isNullOrBlank()) {
             val formattedLogo = if (logoUrl.startsWith("http")) logoUrl 
                               else "https://apisita.shanti-pos.com$logoUrl"
            Log.d("LoginDebug", "NfcScanActivity - Initial Logo load: $formattedLogo")
             imgLogo.load(formattedLogo) {
                 placeholder(R.drawable.sita_logo)
                 error(R.drawable.sita_logo)
                 crossfade(true)
             }
        }
        
        if (!initialImageUrl.isNullOrBlank()) {
             val formattedUrl = if (initialImageUrl.startsWith("http")) initialImageUrl 
                              else "https://apisita.shanti-pos.com$initialImageUrl"
            Log.d("LoginDebug", "NfcScanActivity - Initial Image load: $formattedUrl")
             imgBackgroundLogo.load(formattedUrl) {
                 crossfade(true)
             }
        }
        
        setupListeners()
        resetState()

        // Initial carousel update
        if (images.isNotEmpty()) {
            updateCarouselImage()
        }


        // Background refresh of shop profile data (logo, images)
        lifecycleScope.launch {
            LocalStorage.getAuthToken()?.let { token ->
                val authRepository = AuthRepository()
                authRepository.getProfile(token, LocalStorage.getShopUId(), LocalStorage.getShopId()).onSuccess { response ->
                    val newImagesUrl = response.allImages.firstOrNull()?.let { imagePath ->
                        if (imagePath.startsWith("http")) imagePath
                        else "https://apisita.shanti-pos.com$imagePath"
                    }
                    val newLogoUrl = response.logo?.let { logoPath ->
                        if (logoPath.startsWith("http")) logoPath
                        else "https://apisita.shanti-pos.com$logoPath"
                    }

                    if (!newLogoUrl.isNullOrBlank()) {
                        imgLogo.load(newLogoUrl) {
                            placeholder(R.drawable.sita_logo)
                            error(R.drawable.sita_logo)
                            crossfade(true)
                        }
                    }

                    if (!newImagesUrl.isNullOrBlank()) {
                        imgBackgroundLogo.load(newImagesUrl) {
                            crossfade(true)
                        }
                    }
                    
                    // Update carousel list
                    images = response.allImages
                    if (images.isNotEmpty()) {
                        currentImageIndex = 0
                        updateCarouselImage()
                    }

                    // Update LocalStorage for persistence

                    LocalStorage.saveAuth(
                        token = token,
                        name = response.name,
                        email = response.email,
                        shopId = response.shopId,
                        logoUrl = newLogoUrl,
                        images = response.allImages,
                        shopUId = response._id ?: LocalStorage.getShopUId()
                    )

                }
            }
        }
    }

    
    private fun initViews() {
        val appBar = findViewById<View>(R.id.includeAppBar)
        tvDisplayName = appBar.findViewById<TextView>(R.id.tvAppBarTitle)
        btnLogout = appBar.findViewById<ImageButton>(R.id.btnAppBarAction)
        
        imgLogo = findViewById<ImageView>(R.id.imgLogo)
        imgBackgroundLogo = findViewById<ImageView>(R.id.imgBackgroundLogo)
        tvStatus = findViewById<TextView>(R.id.tvStatus)
        btnStopScanning = findViewById<Button>(R.id.btnStopScanning)
        
        cvMemberDetails = findViewById<View>(R.id.cvMemberDetails)
        tvMemberId = findViewById<TextView>(R.id.tvMemberId)
        tvMemberName = findViewById<TextView>(R.id.tvMemberName)
        tvCompanyName = findViewById<TextView>(R.id.tvCompanyName)
        tvExpiryDate = findViewById<TextView>(R.id.tvExpiryDate)
        tvCurrentBalance = findViewById<TextView>(R.id.tvCurrentBalance)
        
        cvTransaction = findViewById(R.id.cvTransaction)
        etAmount = findViewById(R.id.etAmount)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
        pbLoader = findViewById(R.id.pbLoader)
        scrollView = findViewById(R.id.scrollView)
        // Note: The ScrollView has android:id="@+id/scrollView" in activity_nfc_scan.xml
        
        // Handle IME insets manually because of enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, imeInsets.bottom + systemBars.bottom)
            
            // If keyboard is appearing and amount field is focused, scroll to bottom
            if (imeInsets.bottom > 0 && etAmount.hasFocus()) {
                view.postDelayed({
                    scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height)
                }, 200)
            }
            insets
        }

        findViewById<View>(R.id.llPoweredBy).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://devisoft.co.in"))
            startActivity(intent)
        }
        // Note: btnBack was removed from layout_app_bar_main.xml per previous instructions
    }
    
    private fun setupListeners() {
        btnLogout.setOnClickListener {
            LocalStorage.clearAuth() 
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        imgLogo.setOnClickListener {
            if (scanError != null || tvStatus.text.toString() == "Transaction Successful") {
                resetState()
            } else {
                startScanning()
            }
        }
        
        imgLogo.setOnLongClickListener {
            // DEBUG: Simulate a successful scan
            runOnUiThread {
                android.widget.Toast.makeText(this, "Debug: Simulating Scan...", android.widget.Toast.LENGTH_SHORT).show()
                countDownTimer?.cancel() // Cancel any active timer for debug
                pbLoader.visibility = View.VISIBLE
                btnStopScanning.visibility = View.VISIBLE
                btnConfirm.isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    stopScanning()
                }, 1000)
            }
            true
        }
        
        btnStopScanning.setOnClickListener {
            stopScanning()
            resetState()
        }
        
        btnCancel.setOnClickListener {
            resetState()
        }
        
        btnConfirm.setOnClickListener {
            addAmount()
        }

        etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height)
                }, 300)
            }
        }
    }
    
    private fun resetState() {
        currentVerifiedMemberId = null
        currentVerifiedCardMfid = null
        currentPassword = ""
        etAmount.setText("")
        scanError = null
        pendingWriteAmount = null
        lastScannedTag = null
        pbLoader.visibility = View.GONE
        
        cvMemberDetails.visibility = View.GONE
        cvTransaction.visibility = View.GONE
        btnStopScanning.visibility = View.GONE
        btnConfirm.isEnabled = true
        
        showStatus("Ready to Verify\nTap logo to scan", false)
        countDownTimer?.cancel() // Ensure timer is cancelled on reset
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
    
    private fun showStatus(message: String, isError: Boolean = false, isSuccess: Boolean = false) {
        tvStatus.text = message
        when {
            isError -> tvStatus.setTextColor(android.graphics.Color.RED)
            isSuccess -> tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            else -> tvStatus.setTextColor(android.graphics.Color.parseColor("#757575"))
        }
    }

    private fun startScanning() {
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC not supported")
            return
        }
        isScanning = true
        scannedData = null
        scanError = null
        cvMemberDetails.visibility = View.GONE
        cvTransaction.visibility = View.GONE
        enableForegroundDispatch()
        lastTagIdString = null
        lastTagTimestamp = 0
        
        countDownTimer?.cancel()
        countDownTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = (millisUntilFinished / 1000).toInt()
                showStatus("Searching for card...\nHold it near the back\nTime Elapse: $remainingSeconds Seconds", false)
            }

            override fun onFinish() {
                if (isScanning) {
                    stopScanning()
                    showResultPopup("Timeout", "No card detected (or multiple cards present)\nPlease try again", isError = true, onOk = ::resetState)
                }
            }
        }.start()

        pbLoader.visibility = View.VISIBLE
        btnStopScanning.visibility = View.VISIBLE
        btnConfirm.isEnabled = false
    }
    private fun stopScanning() {
        isScanning = false 
        btnStopScanning.visibility = View.GONE
        pbLoader.visibility = View.GONE
        btnConfirm.isEnabled = true
        disableForegroundDispatch()
        countDownTimer?.cancel()
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        // Pass null for techLists to capture everything
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    private fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onResume() { 
        super.onResume()
        if (isScanning) enableForegroundDispatch()
    }

    override fun onPause() { 
        super.onPause()
        disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        // Check for NDEF URL first
        if (handleNdefUrl(intent)) return

        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            
            Log.d(TAG, "MULTIPLE_CARD_CHECK: New Intent Received - Action: ${intent.action}")

            // Note: EXTRA_TAGS is a hidden API, relying on techList duplicate check instead

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val currentTagId = bytesToHex(it.id)
                val currentTime = System.currentTimeMillis()
                
                // Heuristic 1: Duplicate Techs (Key indicator)
                val techs = it.techList
                Log.d(TAG, "MULTIPLE_CARD_CHECK: Tag ID: $currentTagId, Techs: ${techs.joinToString(", ")}")
                val uniqueTechs = techs.distinct()
                if (techs.size != uniqueTechs.size) {
                    Log.w(TAG, "MULTIPLE_CARD_CHECK: FAILED - Duplicate technologies detected in techList!")
                    runOnUiThread {
                        stopScanning()
                        resetState()
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Multiple Cards Detected")
                            .setMessage("Multiple cards detected! Please hold one card only.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    return
                }

                // Heuristic 2: Rapid ID change
                if (lastTagIdString != null && lastTagIdString != currentTagId && (currentTime - lastTagTimestamp) < 4000) {
                    Log.w(TAG, "MULTIPLE_CARD_CHECK: FAILED - Rapid ID change detected (Last: $lastTagIdString, Current: $currentTagId, Delta: ${currentTime - lastTagTimestamp}ms)")
                    runOnUiThread {
                        stopScanning()
                        resetState()
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Multiple Cards Detected")
                            .setMessage("Multiple cards detected! Please hold one card only.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    return
                }
                
                Log.d(TAG, "MULTIPLE_CARD_CHECK: PASSED - No multiple cards detected.")
                
                lastTagIdString = currentTagId
                lastTagTimestamp = currentTime
                lastScannedTag = it
                
                if (pendingWriteAmount != null) {
                    val success = writeAmountToTag(it, pendingWriteAmount!!)
                    if (success) {
                        val balanceFormatter = java.text.DecimalFormat("#,###.00")
                        val formattedTotal = try { balanceFormatter.format(pendingWriteAmount?.toDouble() ?: 0.0) } catch(e: Exception) { pendingWriteAmount }
                        showResultPopup("Success", "Amount added successfully!\nNew Balance: $formattedTotal", isError = false, onOk = ::resetState)
                        pendingWriteAmount = null
                    } else {
                        runOnUiThread {
                        showResultPopup("Write Error", "Write Failed. Please tap again.", isError = true, onOk = ::resetState)
                        }
                    }
                } else if (isScanning) {
                    val result = readMifareClassicData(it)
                    result.onSuccess { data: ScannedCardData ->
                        scannedData = data
                        onCardScanned(data)
                        stopScanning()
                    }.onFailure { e ->
                        if (e.message == "Card is empty" || e.message == "Read Failed") {
                             scanError = e.message
                             val title = if (e.message == "Card is empty") "Empty Card" else "Read Error"
                             val msg = if (e.message == "Card is empty") "card is empty not assigne to any member" else (scanError ?: "Error")
                             showResultPopup(title, msg, isError = true, onOk = ::resetState)
                             stopScanning()
                        } else {
                             Log.e(TAG, "Scan failed silently: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun handleNdefUrl(intent: Intent): Boolean {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null) {
            for (rawMsg in rawMessages) {
                val msg = rawMsg as NdefMessage
                for (record in msg.records) {
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN && 
                        java.util.Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                        val uri = record.toUri()
                        if (uri != null) {
                            runOnUiThread {
                                openExternalUrl(uri.toString())
                            }
                            return true
                        }
                    }
                }
            }
        }
        // Fallback: Check if the tag itself has NDEF tech even if ACTION_NDEF_DISCOVERED wasn't triggered
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    val msg = ndef.ndefMessage
                    if (msg != null) {
                        for (record in msg.records) {
                            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && 
                                java.util.Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                                val uri = record.toUri()
                                if (uri != null) {
                                    val url = uri.toString()
                                    ndef.close()
                                    runOnUiThread {
                                        openExternalUrl(url)
                                    }
                                    return true
                                }
                            }
                        }
                    }
                    ndef.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading NDEF from tag", e)
                }
            }
        }
        return false
    }

    private fun openExternalUrl(url: String) {
        try {
            Log.d(TAG, "Opening URL from NFC: $url")
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
            stopScanning()
            resetState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
            showResultPopup("Error", "Failed to open URL", isError = true, onOk = ::resetState)
        }
    }

    // AUTHENTICATION & READ LOGIC (Preserved from original)
    private fun authenticate(mifare: MifareClassic, sector: Int): Boolean {
        return mifare.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT) ||
                mifare.authenticateSectorWithKeyA(sector, MifareClassic.KEY_NFC_FORUM)
    }

    private fun readBlockString(mifare: MifareClassic, block: Int): String {
        val bytes = mifare.readBlock(block)
        val data = String(bytes, Charset.forName("US-ASCII")).trim { it <= ' ' || it == '\u0000' }
        return data
    }

    private fun decodeBlock(bytes: ByteArray): String {
        // Stop at first null byte
        val validLen = bytes.indexOfFirst { it == 0.toByte() }.let { if (it == -1) bytes.size else it }
        if (validLen == 0) return ""
        val validBytes = bytes.copyOfRange(0, validLen)

        // Check if all bytes are printable ASCII (32..126)
        // If not, assume it's binary/hex pointer and convert to Hex String
        val isPrintableAscii = validBytes.all { it >= 32 && it <= 126 }

        return if (isPrintableAscii) {
            String(validBytes, Charset.forName("US-ASCII")).trim()
        } else {
            bytesToHex(validBytes)
        }
    }

    private fun readMifareClassicData(tag: Tag): Result<ScannedCardData> {
        val mifare = MifareClassic.get(tag) ?: return Result.failure(Exception("Not a Mifare Classic tag"))
        try {
            mifare.connect()
            Log.d(TAG, "Mifare connected")
            
            if (authenticate(mifare, 3)) {
                Log.d(TAG, "Sector 3 authenticated")
                
                val idBytes = mifare.readBlock(12)
                Log.d(TAG, "Block 12 Raw: ${bytesToHex(idBytes)}")
                val id = decodeBlock(idBytes)
                Log.d(TAG, "Block 12 Decoded: '$id'")

                val companyBytes = mifare.readBlock(13)
                Log.d(TAG, "Block 13 Raw: ${bytesToHex(companyBytes)}")
                val company = decodeBlock(companyBytes)
                Log.d(TAG, "Block 13 Decoded: '$company'")

                // Read Validity from Block 14
                val validityBytes = mifare.readBlock(14)
                Log.d(TAG, "Block 14 Raw: ${bytesToHex(validityBytes)}")
                val validity = decodeBlock(validityBytes)
                Log.d(TAG, "Block 14 Decoded: '$validity'")
                
                // Read Password from Block 18 (Sector 4)
                var password = ""
                if (authenticate(mifare, 4)) {
                     Log.d(TAG, "Sector 4 authenticated")
                     val pwdBytes = mifare.readBlock(18)
                     Log.d(TAG, "Block 18 Raw: ${bytesToHex(pwdBytes)}")
                     password = decodeBlock(pwdBytes)
                     Log.d(TAG, "Block 18 Decoded: '$password'")
                } else {
                     Log.e(TAG, "Sector 4 Failed to authenticate")
                }

                // Read Card Type from Block 20 (Sector 5)
                var cardType = ""
                if (authenticate(mifare, 5)) {
                     Log.d(TAG, "Sector 5 authenticated")
                     val typeBytes = mifare.readBlock(20)
                     Log.d(TAG, "Block 20 Raw: ${bytesToHex(typeBytes)}")
                     cardType = decodeBlock(typeBytes)
                     // Normalize truncated card types due to 16-byte block limit
                     if (cardType == "Company Executiv") {
                         cardType = "Company Executive"
                     }
                     Log.d(TAG, "Block 20 Decoded: '$cardType'")
                } else {
                     Log.d(TAG, "Sector 5 Failed to authenticate (Might not exist on some cards)")
                }

                if (id.isNotEmpty()) {
                    val mfid = bytesToHex(tag.id)
                    Log.d(TAG, "Read Success: $id, $company, $mfid")
                    // Note: password might be empty if sector 4 failed or block 18 was empty
                    return Result.success(ScannedCardData(id, company, mfid, password, validity, cardType))
                } else {
                    Log.e(TAG, "Read Failed: ID is empty")
                    return Result.failure(Exception("Card is empty"))
                }

            } else {
                Log.e(TAG, "Sector 3 Failed to authenticate")
                return Result.failure(Exception("Card not detected properly please scan again"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read Error during execution", e)
            return Result.failure(e)
        } finally {
            try { mifare.close() } catch (e: Exception) {}
        }
    }


    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[j * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun onCardScanned(data: ScannedCardData) {
        if (DateUtils.isCardExpired(data.validity)) {
            showResultPopup("Expired", "Card validity is expired. Renew your membership", isError = true, onOk = ::resetState)
            stopScanning()
            return
        }

        showStatus("Processing...", false)
        pbLoader.visibility = View.VISIBLE
        currentPassword = data.password
        btnStopScanning.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.verifyMember(
                memberId = data.memberId,
                companyName = data.companyName,
                password = data.password,
                shopId = LocalStorage.getShopId() ?: 0,
                cardMfid = data.cardMfid,
                cardValidity = data.validity,
                cardType = data.cardType
            )
            result.onSuccess { response: VerifyMemberResponse ->
                val backendValidity = response.validity ?: response.cardValidity
                if (backendValidity != null && isServerDateExpired(backendValidity)) {
                    Log.d(TAG, "Backend validity expired check caught an expired date: $backendValidity")
                    pbLoader.visibility = View.GONE
                    showResultPopup("Expired", "Card validity is expired. Renew your membership", isError = true, onOk = ::resetState)
                    btnStopScanning.visibility = View.VISIBLE
                    return@launch
                }
                displayMemberInfo(response, data.cardMfid)
            }.onFailure { e: Throwable ->
                Log.d(TAG, "Verify failed ($e), attempting fallback search for ID: ${data.memberId}")
                val searchResult = repository.getMemberById(data.memberId, LocalStorage.getShopId() ?: 0)
                
                searchResult.onSuccess { member ->
                    Log.d(TAG, "Search success: ${member.companyName}")
                    
                    // Always retry using the exact values from the backend's database if available.
                    // This circumvents physical card data truncation or formatting issues that the verify endpoint rejects.
                    val backendCard = member.cards?.find { it.card_mfid == data.cardMfid }
                    
                    lifecycleScope.launch {
                        val retryResult = repository.verifyMember(
                            memberId = data.memberId,
                            companyName = member.companyName ?: data.companyName,
                            password = data.password,
                            shopId = LocalStorage.getShopId() ?: 0,
                            cardMfid = data.cardMfid,
                            cardValidity = backendCard?.cardValidity ?: data.validity,
                            cardType = backendCard?.cardType ?: data.cardType
                        )
                        retryResult.onSuccess { retryResponse ->
                            Log.d(TAG, "Retry verification success!")
                            val backendValidity = retryResponse.validity ?: retryResponse.cardValidity
                            if (backendValidity != null && isServerDateExpired(backendValidity)) {
                                Log.d(TAG, "Retry backend validity expired check caught an expired date: $backendValidity")
                                pbLoader.visibility = View.GONE
                                showResultPopup("Expired", "Card validity is expired. Renew your membership", isError = true, onOk = ::resetState)
                                btnStopScanning.visibility = View.VISIBLE
                                return@launch
                            }
                            displayMemberInfo(retryResponse, data.cardMfid)
                        }.onFailure { retryError ->
                             Log.e(TAG, "Retry verification failed: ${retryError.message}")
                             pbLoader.visibility = View.GONE
                             val errorMsg = retryError.message ?: ""
                             if (errorMsg.contains("not found", ignoreCase = true) || errorMsg.contains("mismatch", ignoreCase = true)) {
                                 showResultPopup("Expired", "Card validity is expired. Renew your membership", isError = true, onOk = ::resetState)
                             } else {
                                 showResultPopup("Verification Failed", "Verification Failed: $errorMsg", isError = true, onOk = ::resetState)
                             }
                             btnStopScanning.visibility = View.VISIBLE // Allow them to cancel
                             isScanning = true
                         }
                     }
                  }.onFailure { fallbackError ->
                      pbLoader.visibility = View.GONE
                      val errorMsg = e.message ?: ""
                      if (errorMsg.contains("not found", ignoreCase = true) || errorMsg.contains("mismatch", ignoreCase = true)) {
                          showResultPopup("Expired", "Card validity is expired. Renew your membership", isError = true, onOk = ::resetState)
                      } else {
                          showResultPopup("Verification Failed", "Verification Failed: $errorMsg", isError = true, onOk = ::resetState)
                      }
                      scanError = "Verification Failed"
                  }
             }
         }
     }

    private fun displayMemberInfo(member: VerifyMemberResponse, mfid: String) {
        currentVerifiedMemberId = member.memberId
        currentVerifiedCardMfid = mfid
        
        cvMemberDetails.visibility = View.VISIBLE
        cvTransaction.visibility = View.VISIBLE
        pbLoader.visibility = View.GONE
        showStatus("Card Verified successfully", false, true)
        
        tvMemberId.text = member.memberId.toString()
        tvMemberName.text = member.companyName ?: ""
        tvCompanyName.text = member.email ?: member.companyName
        tvExpiryDate.text = formatDate(member.validity)
        val balanceFormatter = java.text.DecimalFormat("#,###.00")
        tvCurrentBalance.text = balanceFormatter.format(member.currentTotal)

        // Scroll down to show the transaction card and focus the amount field
        scrollView.postDelayed({
            scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height)
            etAmount.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etAmount, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun formatDate(dateString: String?): String {
        if (dateString == null || dateString == "--" || dateString.isBlank()) return "--"
        return try {
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

    private fun isServerDateExpired(dateString: String?): Boolean {
        Log.d(TAG, "Checking server date expiry for string: '$dateString'")
        if (dateString.isNullOrBlank() || dateString == "--") return false
        
        return try {
            val cleanDate = dateString.split("T")[0]
            var parsedDate: java.util.Date? = null
            
            val formats = arrayOf(
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US),
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US),
                java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
            )
            
            for (format in formats) {
                try {
                    parsedDate = format.parse(cleanDate)
                    if (parsedDate != null) break
                } catch (e: Exception) {}
            }
            
            if (parsedDate == null) {
                Log.d(TAG, "Failed to parse server date: '$dateString'")
                return false
            }
            
            val cal = java.util.Calendar.getInstance()
            cal.time = parsedDate
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            
            val isExpired = cal.time.before(java.util.Date())
            Log.d(TAG, "Server date '$dateString' -> Parsed Date: ${cal.time} -> Expired: $isExpired")
            isExpired
        } catch (e: Exception) {
            Log.e(TAG, "Error checking server date expiry", e)
            false
        }
    }

    private fun addAmount() {
        val memberId = currentVerifiedMemberId
        if (memberId == null) {
            showResultPopup("Error", "Please verify member first", isError = true)
            return
        }
        
        val amountStr = etAmount.text.toString()
        val amount = amountStr.toDoubleOrNull()?.toInt()
        if (amount == null || amount <= 0) {
            showResultPopup("Invalid Amount", "Please enter a valid amount", isError = true)
            return
        }

        if (currentPassword.isBlank()) {
            showResultPopup("Auth Failed", "Card authentication failed (Missing Password)", isError = true)
            return
        }

        hideKeyboard()
        val currentBalStr = tvCurrentBalance.text.toString()
        val currentBal = currentBalStr.toDoubleOrNull() ?: 0.0
        val expectedNewTotalStr = (currentBal + amount).toString()

        var writeSuccess = false
        lastScannedTag?.let { tag ->
            writeSuccess = writeAmountToTag(tag, expectedNewTotalStr)
        }

        if (writeSuccess) {
            val balanceFormatter = java.text.DecimalFormat("#,###.00")
            val formattedTotal = try { balanceFormatter.format(expectedNewTotalStr.toDouble()) } catch(e: Exception) { expectedNewTotalStr }
            showResultPopup("Success", "Transaction of ₹$amount completed", isError = false, onOk = ::resetState)

            lifecycleScope.launch {
                val result = repository.addAmount(
                    memberId = memberId.toString(),
                    amount = amount,
                    cardMfid = currentVerifiedCardMfid ?: "",
                    password = currentPassword,
                    shopId = LocalStorage.getShopId() ?: 0
                )
                result.onFailure {
                    Log.e(TAG, "Background API sync failed: ${it.message}")
                }
            }
        } else {
            showStatus("Processing...", false)
            pbLoader.visibility = View.VISIBLE
            btnConfirm.isEnabled = false

            lifecycleScope.launch {
                val result = repository.addAmount(
                    memberId = memberId.toString(),
                    amount = amount,
                    cardMfid = currentVerifiedCardMfid ?: "",
                    password = currentPassword,
                    shopId = LocalStorage.getShopId() ?: 0
                )
                result.onSuccess { response: com.example.sitacardent.model.AddAmountResponse ->
                    val addedAmount = response.addedAmount?.toInt() ?: amount
                    val balanceFormatter = java.text.DecimalFormat("#,###.00")
                    val formattedTotal = balanceFormatter.format(response.newCardTotal)
                    showResultPopup("Success", "Transaction of ₹$addedAmount completed", isError = false, onOk = ::resetState)
                }.onFailure { e: Throwable ->
                    pbLoader.visibility = View.GONE
                    showResultPopup("Transaction Failed", "Transaction Failed: ${e.message}", isError = true, onOk = ::resetState)
                    btnConfirm.isEnabled = true
                }
            }
        }
    }

    private fun showResultPopup(title: String, message: String, isError: Boolean = true, onOk: (() -> Unit)? = null) {
        if (isFinishing || isDestroyed) return
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onOk?.invoke()
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        
        // Use green for success
        if (!isError) {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        }
    }

    private fun writeAmountToTag(tag: Tag, amount: String): Boolean {
        val mifare = MifareClassic.get(tag) ?: return false
        return try {
            mifare.connect()
            if (authenticate(mifare, 4)) {
                writeHexBlock(mifare, 16, stringToHex(amount))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            try { mifare.close() } catch (e: Exception) {}
        }
    }

    private fun stringToHex(input: String): String {
        return input.toByteArray(Charset.forName("US-ASCII")).joinToString("") { "%02X".format(it) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun writeHexBlock(mifare: MifareClassic, blockIndex: Int, hexString: String) {
        val bytes = ByteArray(16)
        val paddedHex = if (hexString.length % 2 != 0) "0$hexString" else hexString
        try {
            val dataBytes = hexStringToByteArray(paddedHex)
            System.arraycopy(dataBytes, 0, bytes, 0, minOf(dataBytes.size, 16))
            mifare.writeBlock(blockIndex, bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Write error: ${e.message}", e)
        }
    }
    override fun onStart() {
        super.onStart()
        if (images.size > 1) {
            carouselHandler.postDelayed(carouselRunnable, 10000)
        }
    }

    override fun onStop() {
        super.onStop()
        carouselHandler.removeCallbacks(carouselRunnable)
        countDownTimer?.cancel()
    }

    private fun updateCarouselImage() {
        if (images.isEmpty()) return
        val url = images.getOrNull(currentImageIndex)
        android.util.Log.d("LoginDebug", "NfcScanActivity - loading image: $url")
        if (!url.isNullOrBlank()) {
            val formattedUrl = if (url.startsWith("http")) url 
                             else "https://apisita.shanti-pos.com$url"
            imgBackgroundLogo.load(formattedUrl) {
                crossfade(true)
                crossfade(1000)
            }
        }
    }
}