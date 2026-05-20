package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.util.QrContentType

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30, 34])
class ScanResultOverlayScreenshotTest {

    @Test
    fun url() = captureMatrix("scan-result-url") {
        ScanResultOverlay(
            contentType = QrContentType.Url("https://example.com"),
            onDismiss = {},
        )
    }

    @Test
    fun email() = captureMatrix("scan-result-email") {
        ScanResultOverlay(
            contentType = QrContentType.Email("hello@example.com"),
            onDismiss = {},
        )
    }

    @Test
    fun phone() = captureMatrix("scan-result-phone") {
        ScanResultOverlay(
            contentType = QrContentType.Phone("+15551234567"),
            onDismiss = {},
        )
    }

    @Test
    fun wifi() = captureMatrix("scan-result-wifi") {
        ScanResultOverlay(
            contentType = QrContentType.Wifi(
                ssid = "MyNetwork",
                password = "secret",
                authType = "WPA",
                hidden = false,
            ),
            onDismiss = {},
        )
    }

    @Test
    fun contact() = captureMatrix("scan-result-contact") {
        ScanResultOverlay(
            contentType = QrContentType.Contact(
                fullName = "Jane Doe",
                phone = "+15551234567",
                email = "jane@example.com",
                organization = "Acme Co",
                rawVCard = "BEGIN:VCARD\nVERSION:3.0\nFN:Jane Doe\nTEL:+15551234567\nEMAIL:jane@example.com\nORG:Acme Co\nEND:VCARD",
            ),
            onDismiss = {},
        )
    }

    @Test
    fun text() = captureMatrix("scan-result-text") {
        ScanResultOverlay(
            contentType = QrContentType.Text("Plain text payload"),
            onDismiss = {},
        )
    }
}
