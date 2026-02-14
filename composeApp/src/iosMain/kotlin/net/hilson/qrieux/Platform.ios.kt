package net.hilson.qrieux

import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.NetworkExtension.NEHotspotConfiguration
import platform.NetworkExtension.NEHotspotConfigurationErrorAlreadyAssociated
import platform.NetworkExtension.NEHotspotConfigurationManager
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIWindowScene

class IosContext : PlatformContext

actual fun vibrate(context: PlatformContext) {
    val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    generator.prepare()
    generator.impactOccurred()
}

actual fun openUrl(context: PlatformContext, url: String) {
    val components = NSURLComponents(string = url) ?: return
    components.URL?.let { nsUrl ->
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>()) { _ -> }
    }
}

actual fun sendEmail(context: PlatformContext, email: String) {
    NSURL.URLWithString("mailto:$email")?.let { nsUrl ->
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>()) { _ -> }
    }
}

actual fun dialPhone(context: PlatformContext, phone: String) {
    NSURL.URLWithString("tel:$phone")?.let { nsUrl ->
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>()) { _ -> }
    }
}

actual fun shareText(context: PlatformContext, text: String, title: String) {
    val activityController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    getRootViewController()?.presentViewController(
        activityController,
        animated = true,
        completion = null
    )
}

private fun getRootViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    val windowScene = scenes.firstOrNull {
        (it as? UIWindowScene)?.activationState == UISceneActivationStateForegroundActive
    } as? UIWindowScene
    return windowScene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
        ?.let { (it as? UIWindow)?.rootViewController }
}

actual fun copyToClipboard(context: PlatformContext, text: String, label: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun showToast(context: PlatformContext, message: String) {
    // iOS doesn't have native toast; handled at UI level if needed
}

actual fun connectToWifi(context: PlatformContext, ssid: String, password: String, authType: String, hidden: Boolean, onResult: (Boolean) -> Unit) {
    val configuration = when (authType.uppercase()) {
        "NOPASS", "" -> NEHotspotConfiguration(sSID = ssid)
        "WEP" -> NEHotspotConfiguration(sSID = ssid, passphrase = password, isWEP = true)
        else -> NEHotspotConfiguration(sSID = ssid, passphrase = password, isWEP = false)
    }
    configuration.hidden = hidden
    NEHotspotConfigurationManager.sharedManager.applyConfiguration(configuration) { error ->
        val success = error == null || error.code == NEHotspotConfigurationErrorAlreadyAssociated
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
            onResult(success)
        }
    }
}

actual fun openAppSettings(context: PlatformContext) {
    NSURL.URLWithString("app-settings:")?.let { nsUrl ->
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>()) { _ -> }
    }
}

actual fun isOnboardingCompleted(context: PlatformContext): Boolean =
    NSUserDefaults.standardUserDefaults.boolForKey("onboarding_completed")

actual fun setOnboardingCompleted(context: PlatformContext) {
    NSUserDefaults.standardUserDefaults.setBool(true, forKey = "onboarding_completed")
}
