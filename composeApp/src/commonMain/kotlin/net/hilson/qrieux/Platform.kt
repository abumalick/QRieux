package net.hilson.qrieux

import androidx.compose.ui.graphics.ImageBitmap

interface PlatformContext

data class GeneratedQrCode(
    val image: ImageBitmap,
    val pngData: ByteArray
)

expect fun vibrate(context: PlatformContext)
expect fun openUrl(context: PlatformContext, url: String)
expect fun sendEmail(context: PlatformContext, email: String)
expect fun dialPhone(context: PlatformContext, phone: String)
expect fun shareText(context: PlatformContext, text: String, title: String)
expect fun shareImage(context: PlatformContext, pngData: ByteArray, title: String)
expect fun copyToClipboard(context: PlatformContext, text: String, label: String)
expect fun showToast(context: PlatformContext, message: String)
expect fun dismissPlatformInput(context: PlatformContext)
expect fun openAppSettings(context: PlatformContext)
expect fun connectToWifi(context: PlatformContext, ssid: String, password: String, authType: String, hidden: Boolean, onResult: (Boolean) -> Unit = {})
expect fun isOnboardingCompleted(context: PlatformContext): Boolean
expect fun setOnboardingCompleted(context: PlatformContext)
expect fun generateQrCode(content: String, size: Int): GeneratedQrCode?
