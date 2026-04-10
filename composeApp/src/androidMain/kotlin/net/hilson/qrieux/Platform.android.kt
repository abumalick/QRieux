package net.hilson.qrieux

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.io.File

class AndroidContext(val context: Context) : PlatformContext

@Suppress("DEPRECATION")
actual fun vibrate(context: PlatformContext) {
    val ctx = (context as AndroidContext).context
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(100)
    }
}

actual fun openUrl(context: PlatformContext, url: String) {
    val ctx = (context as AndroidContext).context
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {}
}

actual fun sendEmail(context: PlatformContext, email: String) {
    val ctx = (context as AndroidContext).context
    try {
        ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    } catch (_: Exception) {}
}

actual fun dialPhone(context: PlatformContext, phone: String) {
    val ctx = (context as AndroidContext).context
    try {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (_: Exception) {}
}

actual fun shareText(context: PlatformContext, text: String, title: String) {
    val ctx = (context as AndroidContext).context
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(intent, title))
    } catch (_: Exception) {}
}

actual fun shareImage(context: PlatformContext, pngData: ByteArray, title: String) {
    val ctx = (context as AndroidContext).context
    try {
        val shareDir = File(ctx.cacheDir, "shared_qr").apply { mkdirs() }
        val shareFile = File(shareDir, "qr-${System.currentTimeMillis()}.png")
        shareFile.outputStream().use { it.write(pngData) }

        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(ctx.contentResolver, title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, title))
    } catch (_: Exception) {}
}

actual fun copyToClipboard(context: PlatformContext, text: String, label: String) {
    val ctx = (context as AndroidContext).context
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

actual fun showToast(context: PlatformContext, message: String) {
    val ctx = (context as AndroidContext).context
    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
}

actual fun dismissPlatformInput(context: PlatformContext) {
    val ctx = (context as AndroidContext).context
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    (ctx as? android.app.Activity)?.currentFocus?.let { view ->
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

actual fun connectToWifi(context: PlatformContext, ssid: String, password: String, authType: String, hidden: Boolean, onResult: (Boolean) -> Unit) {
    val ctx = (context as AndroidContext).context
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val suggestion = buildWifiSuggestion(ssid, password, authType, hidden)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bundle = Bundle().apply {
                putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
            }
            try {
                ctx.startActivity(Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply { putExtras(bundle) })
            } catch (_: Exception) {}
            onResult(true)
        } else {
            val wifiManager = ctx.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            wifiManager.removeNetworkSuggestions(listOf(suggestion))
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            onResult(status == android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS)
        }
    } else {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("WiFi Password", password))
        try {
            ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (_: Exception) {}
        onResult(true)
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
private fun buildWifiSuggestion(ssid: String, password: String, authType: String, hidden: Boolean): WifiNetworkSuggestion {
    val builder = WifiNetworkSuggestion.Builder()
        .setSsid(ssid)
        .setIsHiddenSsid(hidden)
    when (authType) {
        "WPA", "WPA2", "WPA3" -> builder.setWpa2Passphrase(password)
        "WEP" -> builder.setWpa2Passphrase(password)
    }
    return builder.build()
}

actual fun addContact(context: PlatformContext, vCardData: String) {
    val ctx = (context as AndroidContext).context
    val file = File(ctx.cacheDir, "contact.vcf")
    file.writeText(vCardData)
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/x-vcard")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

actual fun openAppSettings(context: PlatformContext) {
    val ctx = (context as AndroidContext).context
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", ctx.packageName, null)
    }
    ctx.startActivity(intent)
}

private const val PREFS_NAME = "qrieux"

actual fun isOnboardingCompleted(context: PlatformContext): Boolean {
    val ctx = (context as AndroidContext).context
    return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean("onboarding_completed", false)
}

actual fun setOnboardingCompleted(context: PlatformContext) {
    val ctx = (context as AndroidContext).context
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean("onboarding_completed", true).apply()
}

actual fun generateUuid(): String = java.util.UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun loadHistoryJson(context: PlatformContext): String {
    val ctx = (context as AndroidContext).context
    return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("history", "") ?: ""
}

actual fun saveHistoryJson(context: PlatformContext, json: String) {
    val ctx = (context as AndroidContext).context
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString("history", json).apply()
}

actual fun generateQrCode(content: String, size: Int): GeneratedQrCode? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2
        )
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)

        val pngBytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }

        GeneratedQrCode(
            image = bitmap.asImageBitmap(),
            pngData = pngBytes
        )
    } catch (_: Exception) {
        null
    }
}
