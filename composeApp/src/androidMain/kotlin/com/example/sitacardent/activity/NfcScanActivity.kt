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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.sitacardent.NfcScanScreen
import com.example.sitacardent.ScannedCardData
import java.nio.charset.Charset

class NfcScanActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NfcScanActivity"
    }

    // State holders for Compose
    private var isScanning by mutableStateOf(false)
    private var scannedData by mutableStateOf<ScannedCardData?>(null)
    
    // NFC Adapter
    private var nfcAdapter: NfcAdapter? = null

    // Timeout logic handled by Composable mostly, but we ensure dispatch is disabled
    private val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timeoutRunnable = Runnable { 
        Log.d(TAG, "NFC Scan Timeout reached (Activity side).")
        // We just stop the dispatch. The UI 'isTimeout' state will handle the message.
        stopScanning()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use edge-to-edge for the proper full screen capability
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "User"

        setContent {
            NfcScanScreen(
                userEmail = userEmail,
                onBackClick = { 
                   // Navigate back to Login (MainActivity)
                   val intent = Intent(this, MainActivity::class.java)
                   intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                   startActivity(intent)
                   finish()
                },
                isExternalScanning = isScanning,
                onExternalScanRequest = {
                    startScanning()
                },
                externalScannedData = scannedData,
                onExternalDataConsumed = {
                    // Reset scanned data so it doesn't trigger again immediately if not cleared
                    // But typically we keep it until next scan.
                    // The Composable 'LaunchedEffect' uses it to fill fields. 
                    // We can clear it when new scan starts.
                },
                onLogoLongClick = {
                    // DEBUG: Simulate a successful scan
                    runOnUiThread {
                         android.widget.Toast.makeText(this, "Debug: Simulating Scan...", android.widget.Toast.LENGTH_SHORT).show()
                         // Simulate delay
                         timeoutHandler.postDelayed({
                             scannedData = ScannedCardData("1001", "Test Company Ltd", "DDOO904GHYTEC")
                             stopScanning()
                         }, 1000)
                    }
                }
            )
        }
    }

    private fun startScanning() {
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC not supported")
            return
        }
        isScanning = true
        scannedData = null // Clear previous data
        enableForegroundDispatch()
        
        // Sync with Composable's 60s timeout
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 60000)
    }

    private fun stopScanning() {
        isScanning = false // Updates UI to 'Scanning' false (if not timeout)
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
                    val data = readMifareClassicData(it)
                    if (data != null) {
                        scannedData = data
                        stopScanning() // Scan successful, stop scanning
                    } else {
                        // Failed to read, maybe show error? 
                        // For now we just stay in scanning mode or stop?
                        // Let's keep scanning.
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

    private fun readMifareClassicData(tag: Tag): ScannedCardData? {
        val mifare = MifareClassic.get(tag) ?: return null
        try {
            mifare.connect()
            if (authenticate(mifare, 3)) {
                val id = readBlockString(mifare, 12)
                val company = readBlockString(mifare, 13)
                // We ignore validity string (block 14) for now as the API fetches it?
                // The API 'verifyMember' only needs ID and Company.
                // The original code passed 'validity' to displayMemberInfo.
                // But the Compose 'verifyMember' fetches fresh data from API including validity.
                // So we only need ID and Company to call verify.
                
                if (id.isNotEmpty()) {
                    val mfid = bytesToHex(tag.id)
                    return ScannedCardData(id, company, mfid)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read Error", e)
        } finally {
            try { mifare.close() } catch (e: Exception) {}
        }
        return null
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
}