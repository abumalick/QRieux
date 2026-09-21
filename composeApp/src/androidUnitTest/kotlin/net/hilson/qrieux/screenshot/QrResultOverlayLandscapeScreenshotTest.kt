package net.hilson.qrieux.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.ui.QrResultOverlay

// A generated code that does not fit on screen is worthless: it is there to be
// scanned off the display. The square is the part that can overflow, so the check
// is a phone-sized landscape window rather than the default screen rotated.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-land")
class QrResultOverlayLandscapeScreenshotTest {

    @Test
    fun generated() = captureLandscape("qr-result-overlay") {
        // Both callers draw the overlay inside the Scaffold padding, so the box it
        // really gets is the window minus the tab bar.
        Box(modifier = Modifier.fillMaxSize().padding(TAB_BAR_PADDING)) {
            QrResultOverlay(
                generatedQr = stubGeneratedQrCode(),
                isGenerating = false,
                onShare = {},
                onEdit = {},
            )
        }
    }
}
