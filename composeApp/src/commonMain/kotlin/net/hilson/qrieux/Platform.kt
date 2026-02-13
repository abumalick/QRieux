package net.hilson.qrieux

interface PlatformContext

expect fun vibrate(context: PlatformContext)
expect fun openUrl(context: PlatformContext, url: String)
expect fun sendEmail(context: PlatformContext, email: String)
expect fun dialPhone(context: PlatformContext, phone: String)
expect fun shareText(context: PlatformContext, text: String, title: String)
expect fun copyToClipboard(context: PlatformContext, text: String, label: String)
expect fun showToast(context: PlatformContext, message: String)
expect fun openAppSettings(context: PlatformContext)
expect fun isOnboardingCompleted(context: PlatformContext): Boolean
expect fun setOnboardingCompleted(context: PlatformContext)
