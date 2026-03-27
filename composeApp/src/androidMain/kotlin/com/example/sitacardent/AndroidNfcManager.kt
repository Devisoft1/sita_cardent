package com.example.sitacardent

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.nio.charset.Charset

class AndroidNfcManager(private val activity: Activity) : NfcManager {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val pendingIntent: PendingIntent = PendingIntent.getActivity(
        activity, 0,
        Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_MUTABLE
    )

    private val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

    override val detectedTag: State<Tag?> = mutableStateOf(null)
    override val detectedTagId: State<String?> = mutableStateOf(null)
    override val isMultipleTagsDetected: State<Boolean> = mutableStateOf(false)

    private var lastTagId: String? = null
    private var lastTagTimestamp: Long = 0

    override fun startScanning() {
        (detectedTag as MutableState<Tag?>).value = null
        (detectedTagId as MutableState<String?>).value = null
        (isMultipleTagsDetected as MutableState<Boolean>).value = false
        lastTagId = null
        lastTagTimestamp = 0
        try {
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, null)
        } catch (e: IllegalStateException) {
            platformLog("SITACardent", "Failed to enable foreground dispatch: ${e.message}")
        }
    }

    override fun stopScanning() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (e: IllegalStateException) {
            platformLog("SITACardent", "Failed to disable foreground dispatch: ${e.message}")
        }
    }

    override fun clearScanData() {
        (detectedTag as MutableState<Tag?>).value = null
        (detectedTagId as MutableState<String?>).value = null
        (isMultipleTagsDetected as MutableState<Boolean>).value = false
        lastTagId = null
    }

    fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            var currentTagId: String? = null
            tag?.let {
                val tagId = it.id.joinToString("") { byte -> "%02X".format(byte) }
                val techs = it.techList
                val uniqueTechs = techs.distinct()
                if (techs.size != uniqueTechs.size) {
                    (isMultipleTagsDetected as MutableState<Boolean>).value = true
                }

                val currentTime = System.currentTimeMillis()
                if (lastTagId != null && lastTagId != tagId && (currentTime - lastTagTimestamp) < 4000) {
                    (isMultipleTagsDetected as MutableState<Boolean>).value = true
                }
                
                lastTagId = tagId
                lastTagTimestamp = currentTime
                currentTagId = tagId
            }
            (detectedTag as MutableState<Tag?>).value = tag
            (detectedTagId as MutableState<String?>).value = currentTagId
        }
    }

    private fun authenticateSector(mifare: MifareClassic, sector: Int): Boolean {
        val commonKeys = arrayOf(
            MifareClassic.KEY_DEFAULT,
            MifareClassic.KEY_NFC_FORUM,
            MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()),
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
            byteArrayOf(0x4D.toByte(), 0x31.toByte(), 0x30.toByte(), 0x31.toByte(), 0x32.toByte(), 0x33.toByte()),
            byteArrayOf(0x1A.toByte(), 0x2B.toByte(), 0x3C.toByte(), 0x4D.toByte(), 0x5E.toByte(), 0x6F.toByte()),
            byteArrayOf(0xA0.toByte(), 0xB0.toByte(), 0xC0.toByte(), 0xD0.toByte(), 0xE0.toByte(), 0xF0.toByte()),
            byteArrayOf(0x11.toByte(), 0x22.toByte(), 0x33.toByte(), 0x44.toByte(), 0x55.toByte(), 0x66.toByte()),
            byteArrayOf(0x88.toByte(), 0x88.toByte(), 0x88.toByte(), 0x88.toByte(), 0x88.toByte(), 0x88.toByte())
        )

        for (key in commonKeys) {
            try {
                if (mifare.authenticateSectorWithKeyA(sector, key)) return true
            } catch (e: Exception) { }
            try {
                if (mifare.authenticateSectorWithKeyB(sector, key)) return true
            } catch (e: Exception) { }
        }
        return false
    }

    override fun writeCard(
        memberId: String,
        companyName: String,
        password: String,
        validUpto: String,
        totalBuy: String,
        cardType: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val tag = detectedTag.value
        if (tag == null) {
            onResult(false, "No card detected.")
            return
        }
        val mifare = MifareClassic.get(tag as Tag) ?: run {
            onResult(false, "Not a Mifare Classic card.")
            return
        }

        Thread {
            try {
                mifare.connect()
                if (authenticateSector(mifare, 3)) {
                    writeHexBlock(mifare, 12, stringToHex(memberId))
                    writeHexBlock(mifare, 13, stringToHex(companyName))
                    writeHexBlock(mifare, 14, validUpto.replace("-", "").replace("/", ""))
                    
                    if (authenticateSector(mifare, 4)) {
                        writeHexBlock(mifare, 16, stringToHex(totalBuy))
                        val today = java.text.SimpleDateFormat("ddMMyyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        writeHexBlock(mifare, 17, today)
                        writeHexBlock(mifare, 18, stringToHex(password))

                        if (authenticateSector(mifare, 5)) {
                            writeHexBlock(mifare, 20, stringToHex(cardType))
                            onResult(true, "Data written successfully!")
                        } else onResult(false, "Card not detected properly please scan again")
                    } else onResult(false, "Card not detected properly please scan again")
                } else onResult(false, "Card not detected properly please scan again")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            } finally {
                try { mifare.close() } catch (e: Exception) {}
            }
        }.start()
    }

    override fun writeLogoUrl(url: String, onResult: (Boolean, String) -> Unit) {
        val tag = detectedTag.value as? Tag ?: run {
            onResult(false, "No card detected.")
            return
        }
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            Thread {
                try {
                    ndef.connect()
                    val uriRecord = NdefRecord.createUri(url)
                    val message = NdefMessage(arrayOf(uriRecord))
                    ndef.writeNdefMessage(message)
                    onResult(true, "Logo URL written successfully!")
                } catch (e: Exception) {
                    onResult(false, "Write failed: ${e.message}")
                } finally {
                    try { ndef.close() } catch (e: Exception) {}
                }
            }.start()
        } else {
            val mifare = MifareClassic.get(tag)
            if (mifare != null) {
                Thread {
                    try {
                        mifare.connect()
                        if (authenticateSector(mifare, 5)) {
                            writeHexBlock(mifare, 21, stringToHex(url))
                            onResult(true, "Logo URL written successfully!")
                        } else onResult(false, "Card not detected properly please scan again")
                    } catch (e: Exception) {
                        onResult(false, "Write failed: ${e.message}")
                    } finally {
                        try { mifare.close() } catch (e: Exception) {}
                    }
                }.start()
            } else onResult(false, "This card is not supported.")
        }
    }

    override fun readCard(onResult: (Boolean, Map<String, String>?, String) -> Unit) {
        val tag = detectedTag.value as? Tag ?: run {
            onResult(false, null, "No card detected.")
            return
        }
        val mifare = MifareClassic.get(tag) ?: run {
            onResult(false, null, "Card not supported.")
            return
        }

        Thread {
            try {
                mifare.connect()
                val data = mutableMapOf<String, String>()
                data["card_mfid"] = tag.id.joinToString("") { byte -> "%02X".format(byte) }

                if (authenticateSector(mifare, 3)) {
                    val memberIdHex = bytesToHex(mifare.readBlock(12))
                    if (memberIdHex.replace(" ", "").all { it == '0' }) {
                        onResult(true, null, "Blank card")
                    } else {
                        data["memberId"] = smartDecode(memberIdHex)
                        data["companyName"] = hexToString(bytesToHex(mifare.readBlock(13))).trimNulls()
                        data["validUpto"] = formatHexDate(bytesToHex(mifare.readBlock(14)))
                        
                        if (authenticateSector(mifare, 4)) {
                            data["totalBuy"] = hexToString(bytesToHex(mifare.readBlock(16))).trimNulls()
                            data["lastBuyDate"] = formatHexDate(bytesToHex(mifare.readBlock(17)))
                            data["password"] = hexToString(bytesToHex(mifare.readBlock(18))).trimNulls()

                            if (authenticateSector(mifare, 5)) {
                                data["cardType"] = hexToString(bytesToHex(mifare.readBlock(20))).trimNulls()
                            }
                        }
                        onResult(true, data, "Data read successfully")
                    }
                } else onResult(false, null, "Card not detected properly please scan again")
            } catch (e: Exception) {
                onResult(false, null, "Read error: ${e.message}")
            } finally {
                try { mifare.close() } catch (e: Exception) {}
            }
        }.start()
    }

    override fun clearCard(onResult: (Boolean, String) -> Unit) {
        val tag = detectedTag.value as? Tag ?: run {
            onResult(false, "No card detected.")
            return
        }
        val mifare = MifareClassic.get(tag) ?: run {
            onResult(false, "Not a Mifare Classic card.")
            return
        }
        Thread {
            try {
                mifare.connect()
                if (authenticateSector(mifare, 3)) {
                    mifare.writeBlock(12, ByteArray(16))
                    mifare.writeBlock(13, ByteArray(16))
                    mifare.writeBlock(14, ByteArray(16))
                    if (authenticateSector(mifare, 4)) {
                        mifare.writeBlock(16, ByteArray(16))
                        mifare.writeBlock(17, ByteArray(16))
                        mifare.writeBlock(18, ByteArray(16))
                        if (authenticateSector(mifare, 5)) {
                            mifare.writeBlock(20, ByteArray(16))
                            onResult(true, "Card cleared successfully.")
                        } else onResult(false, "Card not detected properly please scan again")
                    } else onResult(false, "Card not detected properly please scan again")
                } else onResult(false, "Card not detected properly please scan again")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            } finally {
                try { mifare.close() } catch (e: Exception) {}
            }
        }.start()
    }

    override fun deleteCardData(onResult: (Boolean, String) -> Unit) = clearCard(onResult)

    private fun smartDecode(hexStr: String): String {
        val ascii = hexToString(hexStr).trimNulls()
        if (ascii.any { it.code < 32 && it.code != 0 }) return hexStr.replace(" ", "").replace("00", "")
        return ascii
    }

    private fun hexToString(hex: String): String {
        val cleanHex = hex.replace(" ", "")
        val result = StringBuilder()
        for (i in 0 until cleanHex.length - 1 step 2) {
            try {
                val charCode = cleanHex.substring(i, i + 2).toInt(16)
                if (charCode != 0) result.append(charCode.toChar())
            } catch (e: Exception) {}
        }
        return result.toString()
    }

    private fun stringToHex(input: String): String {
        return input.toByteArray(Charset.forName("US-ASCII")).joinToString("") { "%02X".format(it) }
    }

    private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it) }

    private fun formatHexDate(hex: String): String {
        val clean = hex.replace(" ", "")
        if (clean.length >= 8) {
            val d = clean.substring(0, 2); val m = clean.substring(2, 4); val y = clean.substring(4, 8)
            if (d.all { it.isDigit() } && m.all { it.isDigit() } && y.all { it.isDigit() }) return "$d-$m-$y"
        }
        return ""
    }

    private fun String.trimNulls(): String = filter { it != '\u0000' }.trim()

    private fun writeHexBlock(mifare: MifareClassic, blockIndex: Int, hexString: String) {
        val bytes = ByteArray(16)
        val cleanHex = if (hexString.length % 2 != 0) "0$hexString" else hexString
        val dataBytes = ByteArray(cleanHex.length / 2)
        for (i in 0 until cleanHex.length step 2) {
            dataBytes[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) + Character.digit(cleanHex[i + 1], 16)).toByte()
        }
        System.arraycopy(dataBytes, 0, bytes, 0, minOf(dataBytes.size, 16))
        mifare.writeBlock(blockIndex, bytes)
    }

    override fun extractUrl(tag: Any?): String? {
        val nfcTag = tag as? Tag ?: return null
        try {
            val ndef = Ndef.get(nfcTag)
            if (ndef != null) {
                ndef.connect()
                val msg = ndef.ndefMessage
                if (msg != null) {
                    for (record in msg.records) {
                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && java.util.Arrays.equals(record.type, NdefRecord.RTD_URI)) {
                            val uri = record.toUri()
                            if (uri != null) {
                                ndef.close()
                                return uri.toString()
                            }
                        }
                    }
                }
                ndef.close()
            }
        } catch (e: Exception) {}
        return null
    }
}

@Composable
actual fun rememberNfcManager(): NfcManager {
    val context = LocalContext.current
    val activity = context as Activity
    val manager = remember { AndroidNfcManager(activity) }
    
    DisposableEffect(Unit) {
        onDispose {
            manager.stopScanning()
        }
    }
    
    return manager
}
