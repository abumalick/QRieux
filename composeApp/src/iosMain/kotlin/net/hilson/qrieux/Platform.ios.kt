package net.hilson.qrieux

import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreImage.*
import platform.Foundation.*
import platform.NetworkExtension.NEHotspotConfiguration
import platform.NetworkExtension.NEHotspotConfigurationErrorAlreadyAssociated
import platform.NetworkExtension.NEHotspotConfigurationManager
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIPasteboard
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.posix.memcpy
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIWindowScene

class IosContext : PlatformContext

actual fun vibrate(context: PlatformContext) {
    val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    generator.prepare()
    generator.impactOccurred()
}

actual fun openUrl(context: PlatformContext, url: String) {
    val components = NSURLComponents(string = url)
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

@OptIn(ExperimentalForeignApi::class)
actual fun shareImage(context: PlatformContext, pngData: ByteArray, title: String) {
    val image = UIImage.imageWithData(pngData.toNSData()) ?: return
    val activityController = UIActivityViewController(
        activityItems = listOf(image),
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

@OptIn(ExperimentalForeignApi::class)
actual fun dismissPlatformInput(context: PlatformContext) {
    UIApplication.sharedApplication.sendAction(
        action = NSSelectorFromString("resignFirstResponder"),
        to = null,
        from = null,
        forEvent = null
    )
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

@OptIn(ExperimentalForeignApi::class)
actual fun addContact(context: PlatformContext, vCardData: String) {
    val nsData = vCardData.encodeToByteArray().toNSData()
    val contacts = platform.Contacts.CNContactVCardSerialization.contactsWithData(nsData, error = null)
    val contact = contacts?.firstOrNull() as? platform.Contacts.CNContact ?: return

    val vc = platform.ContactsUI.CNContactViewController.viewControllerForUnknownContact(contact)
    vc.contactStore = platform.Contacts.CNContactStore()
    vc.allowsEditing = true
    vc.allowsActions = true

    val nav = platform.UIKit.UINavigationController(rootViewController = vc)
    getRootViewController()?.presentViewController(nav, animated = true, completion = null)
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

actual fun generateUuid(): String = NSUUID().UUIDString

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun loadHistoryJson(context: PlatformContext): String =
    NSUserDefaults.standardUserDefaults.stringForKey("history") ?: ""

actual fun saveHistoryJson(context: PlatformContext, json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, forKey = "history")
}

@OptIn(ExperimentalForeignApi::class)
actual fun generateQrCode(content: String, size: Int): GeneratedQrCode? {
    return try {
        val filter = CIFilter.QRCodeGenerator()
        filter.setValue(content.encodeToByteArray().toNSData(), forKey = "inputMessage")
        filter.setValue("M", forKey = "inputCorrectionLevel")

        val outputImage = filter.outputImage ?: return null
        val extent = outputImage.extent
        val maxDimension = maxOf(CGRectGetWidth(extent), CGRectGetHeight(extent))
        val scale = size.toDouble() / maxDimension
        val scaledImage = outputImage.imageByApplyingTransform(CGAffineTransformMakeScale(scale, scale))
        val context = CIContext.context()
        val cgImage = context.createCGImage(scaledImage, fromRect = scaledImage.extent) ?: return null
        val uiImage = UIImage.imageWithCGImage(cgImage)
        CGImageRelease(cgImage)

        val pngData = UIImagePNGRepresentation(uiImage) ?: return null
        val pngBytes = pngData.toByteArray()
        val image = Image.makeFromEncoded(pngBytes).toComposeImageBitmap()
        GeneratedQrCode(
            image = image,
            pngData = pngBytes
        )
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(bytes = pinned.addressOf(0), length = size.toULong())
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return bytes
}
