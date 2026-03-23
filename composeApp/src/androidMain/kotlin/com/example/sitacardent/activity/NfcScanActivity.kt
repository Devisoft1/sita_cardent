package com.example.sitacardent.activity

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
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
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.request.error
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sitacardent.LocalStorage
import com.example.sitacardent.R
import com.example.sitacardent.ScannedCardData
import com.example.sitacardent.network.MemberRepository
import com.example.sitacardent.model.VerifyMemberResponse
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay


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
    
    // NFC Adapter
    private var nfcAdapter: NfcAdapter? = null
    private val repository = MemberRepository()

    // UI Elements
    private lateinit var tvDisplayName: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var imgLogo: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var btnStopScanning: Button
    private lateinit var scrollView: ScrollView
    
    // Member Details UI
    private lateinit var cvMemberDetails: View
    private lateinit var tvMemberId: TextView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvExpiryDate: TextView
    private lateinit var tvCurrentBalance: TextView
    
    // Transaction UI
    private lateinit var cvTransaction: View
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button
    private lateinit var pbLoader: android.widget.ProgressBar

    // Current Member Data
    private var currentVerifiedMemberId: Long? = null
    private var currentVerifiedCardMfid: String? = null
    private var currentPassword = ""

    private val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timeoutRunnable = Runnable { 
        Log.d(TAG, "NFC Scan Timeout reached.")
        showStatus("No card detected\nPlease try again", true)
        stopScanning()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use edge-to-edge for the proper full screen capability
        enableEdgeToEdge()

        setContentView(R.layout.activity_nfc_scan)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "User"
        val displayName = userEmail.substringBefore("@")

        initViews()
        
        tvDisplayName.text = displayName
        
        // Load Shop Logo
        val logoUrl = LocalStorage.getLogoUrl()
        if (!logoUrl.isNullOrBlank()) {
             imgLogo.load(logoUrl) {
                 placeholder(R.drawable.sita_logo)
                 error(R.drawable.sita_logo)
                 crossfade(true)
             }
        }
        
        setupListeners()
        resetState()
    }
    
    private fun initViews() {
        val appBar = findViewById<View>(R.id.includeAppBar)
        tvDisplayName = appBar.findViewById<TextView>(R.id.tvAppBarTitle)
        btnLogout = appBar.findViewById<ImageButton>(R.id.btnAppBarAction)
        
        imgLogo = findViewById<ImageView>(R.id.imgLogo)
        tvStatus = findViewById<TextView>(R.id.tvStatus)
        btnStopScanning = findViewById<Button>(R.id.btnStopScanning)
        
        cvMemberDetails = findViewById<View>(R.id.cvMemberDetails)
        tvMemberId = findViewById<TextView>(R.id.tvMemberId)
        tvCompanyName = findViewById<TextView>(R.id.tvCompanyName)
        tvExpiryDate = findViewById<TextView>(R.id.tvExpiryDate)
        tvCurrentBalance = findViewById<TextView>(R.id.tvCurrentBalance)
        
        cvTransaction = findViewById<View>(R.id.cvTransaction)
        etAmount = findViewById<TextInputEditText>(R.id.etAmount)
        btnCancel = findViewById<Button>(R.id.btnCancel)
        btnConfirm = findViewById<Button>(R.id.btnConfirm)
        pbLoader = findViewById<android.widget.ProgressBar>(R.id.pbLoader)
        scrollView = findViewById<ScrollView>(R.id.scrollView)
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
                timeoutHandler.postDelayed({
                    onCardScanned(ScannedCardData("1001", "Test Company Ltd", "DDOO904GHYTEC", "debugPass123"))
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
        showStatus("Searching for card...\nHold it near the back", false)
        btnStopScanning.visibility = View.VISIBLE
        cvMemberDetails.visibility = View.GONE
        cvTransaction.visibility = View.GONE
        pbLoader.visibility = View.VISIBLE
        enableForegroundDispatch()
        
        // Sync with Composable's 60s timeout
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 60000)
    }

    private fun stopScanning() {
        isScanning = false 
        btnStopScanning.visibility = View.GONE
        pbLoader.visibility = View.GONE
        disableForegroundDispatch()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, arrayOf(arrayOf(MifareClassic::class.java.name)))
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
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {

            Log.d(TAG, "NFC Tag Detected")
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                lastScannedTag = it
                if (pendingWriteAmount != null) {
                    val success = writeAmountToTag(it, pendingWriteAmount!!)
                    if (success) {
                        runOnUiThread {
                            Toast.makeText(this@NfcScanActivity, "Card Updated! Balance: $pendingWriteAmount", Toast.LENGTH_LONG).show()
                            resetState()
                        }
                        pendingWriteAmount = null
                    } else {
                        runOnUiThread {
                            showStatus("Write Failed. Please tap again.", true, false)
                        }
                    }
                } else if (isScanning) {
                    val result = readMifareClassicData(it)
                    result.onSuccess { data ->
                        scannedData = data
                        onCardScanned(data)
                        stopScanning()
                    }.onFailure { e ->
                        if (e.message == "Card is empty" || e.message == "Read Failed") {
                             scanError = e.message
                             showStatus(scanError ?: "Error", true)
                             stopScanning()
                        } else {
                             Log.e(TAG, "Scan failed silently: ${e.message}")
                        }
                    }
                }
            }
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
                return Result.failure(Exception("Card Authentication Failed"))
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
        showStatus("Processing...", false)
        pbLoader.visibility = View.VISIBLE
        currentPassword = data.password
        btnStopScanning.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.verifyMember(
                memberId = data.memberId,
                companyName = data.companyName,
                password = data.password,
                cardMfid = data.cardMfid,
                cardValidity = data.validity,
                cardType = data.cardType
            )
            result.onSuccess { response ->
                displayMemberInfo(response, data.cardMfid)
            }.onFailure { e ->
                Log.d(TAG, "Verify failed ($e), attempting fallback search for ID: ${data.memberId}")
                val searchResult = repository.getMemberById(data.memberId)
                
                searchResult.onSuccess { member ->
                    Log.d(TAG, "Search success: ${member.companyName}")
                    
                    // If the name from search is different (likely full vs truncated), retry verification
                    if (member.companyName != null && member.companyName != data.companyName) {
                        Log.d(TAG, "Retrying verification with full name: ${member.companyName}")
                        lifecycleScope.launch {
                            val retryResult = repository.verifyMember(
                                memberId = data.memberId,
                                companyName = member.companyName,
                                password = data.password,
                                cardMfid = data.cardMfid,
                                cardValidity = data.validity,
                                cardType = data.cardType
                            )
                            retryResult.onSuccess { retryResponse ->
                                Log.d(TAG, "Retry verification success!")
                                displayMemberInfo(retryResponse, data.cardMfid)
                            }.onFailure { retryError ->
                                Log.e(TAG, "Retry verification failed: ${retryError.message}")
                                pbLoader.visibility = View.GONE
                                showStatus("Card Not Registered: ${retryError.message}", true)
                                btnStopScanning.visibility = View.VISIBLE // Allow them to cancel
                                isScanning = true
                            }
                        }
                    } else {
                        // Names matched, but primary verifyMember STILL failed. This implies backend genuinely rejected it (e.g. bad password or expired).
                        pbLoader.visibility = View.GONE
                        showStatus("Verification Rejected: ${e.message}", true)
                        btnStopScanning.visibility = View.VISIBLE
                        isScanning = true
                    }
                }.onFailure { fallbackError ->
                    pbLoader.visibility = View.GONE
                    showStatus("Verification Failed: ${e.message}", true)
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
        tvCompanyName.text = member.companyName
        tvExpiryDate.text = formatDate(member.validity)
        tvCurrentBalance.text = member.currentTotal.toString()

        // Scroll down to show the transaction card
        scrollView.postDelayed({
            scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height)
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

    private fun addAmount() {
        val memberId = currentVerifiedMemberId
        if (memberId == null) {
            showStatus("Please verify member first", true)
            return
        }
        
        val amountStr = etAmount.text.toString()
        val amount = amountStr.toDoubleOrNull()?.toInt()
        if (amount == null || amount <= 0) {
            showStatus("Please enter a valid amount", true)
            return
        }

        if (currentPassword.isBlank()) {
            showStatus("Card authentication failed (Missing Password)", true)
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
            Toast.makeText(this@NfcScanActivity, "Success! Card Updated. Total: $expectedNewTotalStr", Toast.LENGTH_LONG).show()
            resetState()

            lifecycleScope.launch {
                val result = repository.addAmount(memberId.toString(), amount, currentVerifiedCardMfid ?: "", currentPassword)
                result.onFailure {
                    Log.e(TAG, "Background API sync failed: ${it.message}")
                }
            }
        } else {
            showStatus("Processing...", false)
            pbLoader.visibility = View.VISIBLE
            btnConfirm.isEnabled = false

            lifecycleScope.launch {
                val result = repository.addAmount(memberId.toString(), amount, currentVerifiedCardMfid ?: "", currentPassword)
                result.onSuccess { response ->
                    val newAmountStr = response.newCardTotal.toString()
                    Toast.makeText(this@NfcScanActivity, "Success! System Updated. Total: $newAmountStr", Toast.LENGTH_LONG).show()
                    resetState()
                }.onFailure { e ->
                    pbLoader.visibility = View.GONE
                    showStatus("Transaction Failed: ${e.message}", true)
                    btnConfirm.isEnabled = true
                }
            }
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
}