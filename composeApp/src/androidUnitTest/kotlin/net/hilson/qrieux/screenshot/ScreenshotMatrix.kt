package net.hilson.qrieux.screenshot

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import net.hilson.qrieux.GeneratedQrCode
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.history.HistoryEntryType
import net.hilson.qrieux.ui.theme.QRieuxTheme
import java.util.Locale

private val LOCALES = listOf("en", "ar", "ja")

// SDK matrix is driven by @Config(sdk = [30, 34]) on the test class —
// Robolectric reruns each @Test per SDK. Here we loop theme × locale.
// SDK 30 = pre-Material-You (hardcoded colors); SDK 34 = post-S (dynamic color).
// ar = RTL layout; ja = CJK font fallback.
internal fun captureMatrix(name: String, content: @Composable () -> Unit) {
    val sdk = Build.VERSION.SDK_INT
    for (darkTheme in listOf(false, true)) {
        val mode = if (darkTheme) "dark" else "light"
        for (tag in LOCALES) {
            val previous = Locale.getDefault()
            Locale.setDefault(Locale.forLanguageTag(tag))
            try {
                captureRoboImage("src/androidUnitTest/snapshots/$name-sdk$sdk-$mode-$tag.png") {
                    WithLocale(tag) {
                        QRieuxTheme(darkTheme = darkTheme, dynamicColor = true) {
                            content()
                        }
                    }
                }
            } finally {
                Locale.setDefault(previous)
            }
        }
    }
}

@Composable
private fun WithLocale(tag: String, content: @Composable () -> Unit) {
    val locale = Locale.forLanguageTag(tag)
    val config = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
    val localizedContext = LocalContext.current.createConfigurationContext(config)
    val layoutDir = if (tag == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides config,
        LocalLayoutDirection provides layoutDir,
    ) {
        content()
    }
}

internal fun stubGeneratedQrCode(): GeneratedQrCode {
    val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.WHITE)
    }
    return GeneratedQrCode(image = bmp.asImageBitmap(), pngData = ByteArray(0))
}

internal fun stubHistoryEntry(value: String = "https://example.com") =
    HistoryEntry(
        id = "stub-entry",
        timestamp = 1_700_000_000_000L,
        type = HistoryEntryType.GENERATE,
        rawValue = value,
        generatorType = "Website"
    )
