package net.hilson.qrieux

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast

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

actual fun copyToClipboard(context: PlatformContext, text: String, label: String) {
    val ctx = (context as AndroidContext).context
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

actual fun showToast(context: PlatformContext, message: String) {
    val ctx = (context as AndroidContext).context
    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
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
