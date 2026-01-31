package com.example.sitacardent.activity

import android.app.PendingIntent
import android.content.Context
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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.sitacardent.R
import com.example.sitacardent.network.MemberRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class NfcScanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NfcScanActivity"
    }

    // UPDATED: Added companyName to match our card data
    data class MemberData(
        val id: String,
        val companyName: String,
        val validUpto: String
    )

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var tvInstruction: TextView
    private lateinit var ivNfcLogo: ImageView
    private lateinit var groupMemberInfo: Group
    private lateinit var tvMemberId: TextView
    private lateinit var tvMemberName: TextView // We'll use this for Company Name
    private lateinit var tvValidUpto: TextView
    private lateinit var etInvoiceAmount: TextInputEditText
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var btnCancelScanning: Button
    private lateinit var btnLogout: View
    private lateinit var btnBack: View
    private lateinit var tvHeaderTitle: TextView
    private lateinit var cardLogo: View

    private var currentTag: Tag? = null
    private var pendingAmountToWrite: String? = null
    private var isScanning = false

    private val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timeoutRunnable = Runnable { 
        Log.d(TAG, "NFC Scan Timeout reached (1 min).")
        isScanning = false
        disableForegroundDispatch()
        btnCancelScanning.visibility = View.GONE
        tvInstruction.text = "No card detected\nPlease try again"
        tvInstruction.setTextColor(android.graphics.Color.RED)
    }

    // REPOSITORY & STATE
    private val repository = MemberRepository()
    private var verifiedMemberId: Long? = null
    private var verifiedCompanyName: String? = null

    private fun verifyMemberApi(id: String, company: String) {
        tvInstruction.text = "Verifying Member..."
        tvInstruction.setTextColor(android.graphics.Color.GRAY)
        
        lifecycleScope.launch {
            val result = repository.verifyMember(id, company)
            result.onSuccess { response ->
                Log.d(TAG, "API Verify Success: $response")
                verifiedMemberId = response.memberId
                verifiedCompanyName = response.companyName
                
                displayMemberInfo(MemberData(
                    id = response.memberId.toString(),
                    companyName = response.companyName,
                    validUpto = response.validity
                ))
                Toast.makeText(this@NfcScanActivity, "Balance: ${response.currentTotal}", Toast.LENGTH_SHORT).show()
                
            }.onFailure { e ->
                Log.e(TAG, "API Verify Failed: ${e.message}")
                tvInstruction.text = "Verification Failed: ${e.message}"
                tvInstruction.setTextColor(android.graphics.Color.RED)
                isScanning = false
            }
        }
    }

    private fun addAmountApi(amount: Double) {
        val memId = verifiedMemberId
        if (memId == null) {
             Toast.makeText(this, "Please verify member first", Toast.LENGTH_SHORT).show()
             return
        }

        tvInstruction.text = "Processing Transaction..."
        tvInstruction.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = repository.addAmount(memId.toString(), amount)
            result.onSuccess { response ->
                Log.d(TAG, "API Add Amount Success: $response")
                resetUI()
                tvInstruction.text = "Success! New Balance: ${response.newTotal}"
                tvInstruction.setTextColor(android.graphics.Color.GREEN)
                Toast.makeText(this@NfcScanActivity, "Added ${response.addedAmount}. New Total: ${response.newTotal}", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Log.e(TAG, "API Add Amount Failed: ${e.message}")
                tvInstruction.text = "Transaction Failed: ${e.message}"
                tvInstruction.setTextColor(android.graphics.Color.RED)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_scan)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        tvInstruction = findViewById(R.id.tvInstruction)
        ivNfcLogo = findViewById(R.id.ivNfcLogo)
        cardLogo = findViewById(R.id.cardLogo)
        groupMemberInfo = findViewById(R.id.groupMemberInfo)
        tvMemberId = findViewById(R.id.tvMemberId)
        tvMemberName = findViewById(R.id.tvMemberName)
        tvValidUpto = findViewById(R.id.tvValidUpto)
        etInvoiceAmount = findViewById(R.id.etInvoiceAmount)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)
        btnCancelScanning = findViewById(R.id.btnCancelScanning)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        // Set logged in user email (Username only)
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "User"
        val displayName = userEmail.substringBefore("@")
        tvHeaderTitle.text = displayName
        tvHeaderTitle.isAllCaps = false

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogout.setOnClickListener {
            // Logout logic: Just go back to login screen.
            // MainActivity's logic already handles autofill if credentials exist.
            
            // Navigate back to Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        cardLogo.setOnClickListener {
            if (nfcAdapter == null) {
                Log.e(TAG, "NFC is not supported.")
            } else {
                isScanning = true
                enableForegroundDispatch()
                tvInstruction.text = "Searching for card...\nHold it near the back"
                tvInstruction.setTextColor(android.graphics.Color.GRAY) // Reset to standard color
                btnCancelScanning.visibility = View.VISIBLE
                
                Log.d(TAG, "User started scanning (Logo tap)")
                // Start timeout timer (1 minute)
                timeoutHandler.removeCallbacks(timeoutRunnable)
                timeoutHandler.postDelayed(timeoutRunnable, 60000)
                Log.d(TAG, "Scan timeout timer started (60s)")
            }
        }

        // DEBUG: Long press to manually enter member data for API testing
        cardLogo.setOnLongClickListener {
            showManualEntryDialog()
            true
        }

        btnConfirm.setOnClickListener {
            val amountStr = etInvoiceAmount.text.toString()
            val amount = amountStr.toDoubleOrNull()
            if (amount != null && amount > 0) {
                // Call API instead of writing to card
                addAmountApi(amount)
            } else {
                etInvoiceAmount.error = "Enter valid amount"
            }
        }
        btnCancel.setOnClickListener { resetUI() }
        btnCancelScanning.setOnClickListener { resetUI() }
    }

    // NEW: Manual Entry Dialog for Testing
    private fun showManualEntryDialog() {
        // Construct layout programmatically
        val inputId = TextInputEditText(this).apply { hint = "Member ID (e.g. 12345)" }
        val inputCompany = TextInputEditText(this).apply { hint = "Company Name (e.g. Tech Corp)" }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
            addView(inputId)
            addView(inputCompany)
        }

        AlertDialog.Builder(this)
            .setTitle("Manual API Test")
            .setView(layout)
            .setPositiveButton("Verify") { _, _ ->
                val id = inputId.text.toString()
                val company = inputCompany.text.toString()
                if (id.isNotEmpty() && company.isNotEmpty()) {
                    verifyMemberApi(id, company)
                } else {
                    Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // NEW: Helper to authenticate with multiple keys
    private fun authenticate(mifare: MifareClassic, sector: Int): Boolean {
        return mifare.authenticateSectorWithKeyA(sector, MifareClassic.KEY_DEFAULT) ||
                mifare.authenticateSectorWithKeyA(sector, MifareClassic.KEY_NFC_FORUM)
    }

    // NEW: Helper to read and clean block data
    private fun readBlockString(mifare: MifareClassic, block: Int): String {
        val bytes = mifare.readBlock(block)
        val hex = bytes.joinToString("") { "%02X ".format(it) }
        val data = String(bytes, Charset.forName("US-ASCII")).trim { it <= ' ' || it == '\u0000' }
        Log.d(TAG, "Read Block $block: Hex=[$hex] String=\"$data\"")
        return data
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {

            Log.d(TAG, "NFC Tag Detected via action: ${intent.action}")
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                currentTag = it
                
                // Check if we have a pending write operation
                val amountToWrite = pendingAmountToWrite
                if (amountToWrite != null) {
                    Log.d(TAG, "Executing pending write...")
                    if (writeAmountToCard(it, amountToWrite)) {
                        pendingAmountToWrite = null
                        resetUI()
                    } else {
                        tvInstruction.text = "Write failed. Try tapping again."
                    }
                } else if (isScanning) {
                    // ONLY READ if the user specifically tapped the logo to scan
                    Log.d(TAG, "Executing requested read...")
                    val memberData = readMifareClassicData(it)
                    if (memberData != null) {
                        // verify via API
                        verifyMemberApi(memberData.id, memberData.companyName)
                    } else {
                        tvInstruction.text = "Card detected but couldn't read member data."
                    }
                } else {
                    Log.d(TAG, "Tag detected but app is not in scanning mode. Ignoring.")
                }
            }
        }
    }

    private fun writeAmountToCard(tag: Tag, amount: String): Boolean {
        val mifare = MifareClassic.get(tag) ?: return false
        try {
            mifare.connect()
            Log.d(TAG, "Mifare Classic connected for writing.")
            
            // We'll write to Sector 4 (Block 16)
            val sector = 4
            val block = 16
            
            Log.d(TAG, "Attempting to write amount \"$amount\" to Sector $sector, Block $block...")
            
            if (authenticate(mifare, sector)) {
                Log.d(TAG, "Sector $sector authenticated for writing.")
                
                // Mifare blocks are exactly 16 bytes
                val data = amount.padEnd(16, '\u0000').toByteArray(Charset.forName("US-ASCII"))
                val finalData = if (data.size > 16) data.copyOf(16) else data
                
                val hexToWrite = finalData.joinToString("") { "%02X ".format(it) }
                Log.d(TAG, "Writing Hex: [$hexToWrite]")
                
                mifare.writeBlock(block, finalData)
                Log.d(TAG, "Block $block written successfully.")
                
                // Verify write
                val verifiedData = readBlockString(mifare, block)
                Log.d(TAG, "Write Verification Complete: Read back \"$verifiedData\"")
                return true
            } else {
                Log.e(TAG, "Failed to authenticate Sector $sector for writing.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Write Error: ${e.message}", e)
        } finally {
            try { mifare.close(); Log.d(TAG, "Mifare connection closed.") } catch (e: Exception) {}
        }
        return false
    }

    // COMPLETELY NEW READ LOGIC (Matches your 1st App)
    private fun readMifareClassicData(tag: Tag): MemberData? {
        val mifare = MifareClassic.get(tag) ?: return null
        try {
            mifare.connect()

            // 1. Unlocked Sector 3
            Log.d(TAG, "Attempting to authenticate Sector 3...")
            if (authenticate(mifare, 3)) {
                Log.d(TAG, "Sector 3 Authenticated successfully")
                val id = readBlockString(mifare, 12)
                val company = readBlockString(mifare, 13)
                val validity = readBlockString(mifare, 14)

                if (id.isNotEmpty()) {
                    Log.d(TAG, "Found Member: ID=$id, Company=$company, Validity=$validity")
                    return MemberData(id, company, validity)
                } else {
                    Log.w(TAG, "Block 12 (ID) was empty.")
                }
            } else {
                Log.e(TAG, "Failed to authenticate Sector 3")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read Error", e)
        } finally {
            try { mifare.close() } catch (e: Exception) {}
        }
        return null
    }

    private fun displayMemberInfo(memberData: MemberData) {
        tvMemberId.text = memberData.id
        tvMemberName.text = memberData.companyName
        tvValidUpto.text = formatDate(memberData.validUpto)

        tvInstruction.visibility = View.GONE
        groupMemberInfo.visibility = View.VISIBLE

        // STOP SCANNING once data is shown
        isScanning = false
        disableForegroundDispatch()
        btnCancelScanning.visibility = View.GONE
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Log.d(TAG, "Scan timeout timer cancelled (Data displayed)")
        Log.d(TAG, "Member data shown in center. Scanner disabled.")
    }

    private fun formatDate(rawDate: String): String {
        return try {
            val inputFormats = arrayOf("yyyyMMdd", "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy")
            var parsedDate: java.util.Date? = null
            for (format in inputFormats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                    sdf.isLenient = false
                    parsedDate = sdf.parse(rawDate)
                    if (parsedDate != null) break
                } catch (e: Exception) { continue }
            }
            if (parsedDate != null) {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(parsedDate)
            } else { rawDate }
        } catch (e: Exception) { rawDate }
    }

    private fun resetUI() {
        tvInstruction.visibility = View.VISIBLE
        groupMemberInfo.visibility = View.GONE
        etInvoiceAmount.text?.clear()
        tvInstruction.text = "Tap the SITA logo to scan a new card"
        tvInstruction.setTextColor(android.graphics.Color.GRAY)
        isScanning = false
        currentTag = null
        pendingAmountToWrite = null // Clear pending state
        btnCancelScanning.visibility = View.GONE
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Log.d(TAG, "Scan timeout timer cancelled (UI Reset)")
        disableForegroundDispatch()
        Log.d(TAG, "UI Reset complete. Scanner disabled.")
    }

    // Standard Dispatching logic
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
            Log.e(TAG, "Error disabling NFC dispatch: ${e.message}")
        }
    }

    override fun onResume() { 
        super.onResume()
        if (isScanning) enableForegroundDispatch() 
    }

    override fun onPause() { 
        super.onPause()
        Log.d(TAG, "onPause: Disabling foreground dispatch")
        disableForegroundDispatch()
    }
}