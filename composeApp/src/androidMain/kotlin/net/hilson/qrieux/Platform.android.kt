package net.hilson.qrieux

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
