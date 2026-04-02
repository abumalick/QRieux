package net.hilson.qrieux

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import net.hilson.qrieux.scanner.ScanOverlay
import net.hilson.qrieux.ui.QrGeneratorScreen
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.ui.theme.QRieuxTheme
import net.hilson.qrieux.util.QrContentType
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

/**
 * Legacy App composable — used only for screenshot generation mode.
 * Normal app flow uses the tab-specific screens (ScanScreen, CreateScreen, HelpScreenHost)
 * hosted in SwiftUI TabView.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
fun App(sharedImage: UIImage? = null, sharedText: String? = null) {
    QRieuxTheme {
        val screenshotContent = NSProcessInfo.processInfo.environment["SCREENSHOT_CONTENT"] as? String
        if (screenshotContent != null) {
            val bgPath = NSProcessInfo.processInfo.environment["SCREENSHOT_BACKGROUND"] as? String
            val bgBitmap = remember(bgPath) { bgPath?.let { loadImageBitmap(it) } }
            Scaffold(containerColor = Color.Black) { _ ->
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    bgBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (screenshotContent == "__SCANNER__") {
                        val originalDirection = LocalLayoutDirection.current
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) { ScanOverlay() }
                                val btnColors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    contentColor = Color.White
                                )
                                FilledIconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 24.dp).size(64.dp),
                                    colors = btnColors
                                ) { Icon(Icons.Default.FlashOn, null, Modifier.size(32.dp)) }
                                FilledIconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 24.dp).size(64.dp),
                                    colors = btnColors
                                ) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(32.dp)) }
                            }
                        }
                    } else if (screenshotContent == "__GENERATOR__") {
                        QrGeneratorScreen(
                            platformContext = IosContext(),
                            screenshotPayload = "https://www.wikipedia.org"
                        )
                    } else {
                        ScanResultOverlay(
                            contentType = QrContentType.fromRawValue(screenshotContent),
                            onDismiss = {},
                        )
                    }
                }
            }
            return@QRieuxTheme
        }

        // Fallback: if not in screenshot mode, show scan screen
        // (This path is only used by legacy MainViewController calls)
        ScanScreen(sharedImage = sharedImage)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadImageBitmap(path: String): ImageBitmap? {
    val uiImage = UIImage.imageWithContentsOfFile(path) ?: return null
    val data = UIImagePNGRepresentation(uiImage) ?: return null
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
