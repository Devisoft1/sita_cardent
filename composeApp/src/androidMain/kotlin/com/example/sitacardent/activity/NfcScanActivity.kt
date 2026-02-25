package com.example.sitacardent.activity

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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
    
    // NFC Adapter
    private var nfcAdapter: NfcAdapter? = null
    private val repository = MemberRepository()

    // UI Elements
    private lateinit var tvDisplayName: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var imgLogo: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var btnStopScanning: Button
    
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
        
        setupListeners()
        resetState()
    }
    
    private fun initViews() {
        val appBar = findViewById<View>(R.id.includeAppBar)
        tvDisplayName = appBar.findViewById(R.id.tvAppBarTitle)
        btnLogout = appBar.findViewById(R.id.btnAppBarAction)
        
        imgLogo = findViewById(R.id.imgLogo)
        tvStatus = findViewById(R.id.tvStatus)
        btnStopScanning = findViewById(R.id.btnStopScanning)
        
        cvMemberDetails = findViewById(R.id.cvMemberDetails)
        tvMemberId = findViewById(R.id.tvMemberId)
        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvExpiryDate = findViewById(R.id.tvExpiryDate)
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance)
        
        cvTransaction = findViewById(R.id.cvTransaction)
        etAmount = findViewById(R.id.etAmount)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
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
    }
    
    private fun resetState() {
        currentVerifiedMemberId = null
        currentVerifiedCardMfid = null
        currentPassword = ""
        etAmount.setText("")
        scanError = null
        
        cvMemberDetails.visibility = View.GONE
        cvTransaction.visibility = View.GONE
        btnStopScanning.visibility = View.GONE
        
        showStatus("Ready to Verify\nTap logo to scan", false)
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
        enableForegroundDispatch()
        
        // Sync with Composable's 60s timeout
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 60000)
    }

    private fun stopScanning() {
        isScanning = false 
        btnStopScanning.visibility = View.GONE
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
                if (isScanning) {
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

                if (id.isNotEmpty()) {
                    val mfid = bytesToHex(tag.id)
                    Log.d(TAG, "Read Success: $id, $company, $mfid")
                    // Note: password might be empty if sector 4 failed or block 18 was empty
                    return Result.success(ScannedCardData(id, company, mfid, password))
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
        currentPassword = data.password
        btnStopScanning.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.verifyMember(data.memberId, data.companyName, data.password)
            result.onSuccess { response ->
                displayMemberInfo(response, data.cardMfid)
            }.onFailure { e ->
                Log.d(TAG, "Verify failed ($e), attempting fallback search")
                val searchResult = repository.getMemberById(data.memberId)
                
                searchResult.onSuccess { member ->
                    val response = VerifyMemberResponse(
                        memberId = member.memberId ?: 0,
                        companyName = member.companyName ?: "",
                        validity = member.validity ?: "",
                        currentTotal = member.total ?: 0.0
                    )
                    displayMemberInfo(response, data.cardMfid)
                }.onFailure { fallbackError ->
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
        showStatus("Card Verified successfully", false, true)
        
        tvMemberId.text = member.memberId.toString()
        tvCompanyName.text = member.companyName
        tvExpiryDate.text = formatDate(member.validity)
        tvCurrentBalance.text = member.currentTotal.toString()
    }

    private fun formatDate(dateString: String): String {
        if (dateString == "--" || dateString.isBlank()) return dateString
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
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showStatus("Please enter a valid amount", true)
            return
        }

        if (currentPassword.isBlank()) {
            showStatus("Card authentication failed (Missing Password)", true)
            return
        }

        showStatus("Processing...", false)
        btnConfirm.isEnabled = false

        lifecycleScope.launch {
            val result = repository.addAmount(memberId.toString(), amount, currentVerifiedCardMfid ?: "", currentPassword)
            result.onSuccess { response ->
                showStatus("Success! New Total: ${response.newCardTotal}", false, true)
                tvCurrentBalance.text = response.newCardTotal.toString()
                etAmount.setText("")
                
                delay(2000)
                resetState()
                btnConfirm.isEnabled = true
            }.onFailure { e ->
                showStatus("Transaction Failed: ${e.message}", true)
                btnConfirm.isEnabled = true
            }
        }
    }
}