package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.AndroidContext
import net.hilson.qrieux.generator.QrGeneratorFormData
import net.hilson.qrieux.generator.QrGeneratorType
import net.hilson.qrieux.generator.QrWifiSecurity
import net.hilson.qrieux.ui.QrGeneratorScreen

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30, 34])
class QrGeneratorScreenScreenshotTest {

    @Test
    fun text_form() = captureMatrix("qr-generator-text") {
        QrGeneratorScreen(
            platformContext = AndroidContext(RuntimeEnvironment.getApplication()),
            initialType = QrGeneratorType.Text,
            initialForm = QrGeneratorFormData(text = "Hello from Roborazzi"),
        )
    }

    @Test
    fun website_form() = captureMatrix("qr-generator-website") {
        QrGeneratorScreen(
            platformContext = AndroidContext(RuntimeEnvironment.getApplication()),
            initialType = QrGeneratorType.Website,
            initialForm = QrGeneratorFormData(website = "https://example.com"),
        )
    }

    @Test
    fun wifi_form() = captureMatrix("qr-generator-wifi") {
        QrGeneratorScreen(
            platformContext = AndroidContext(RuntimeEnvironment.getApplication()),
            initialType = QrGeneratorType.Wifi,
            initialForm = QrGeneratorFormData(
                wifiSsid = "MyNetwork",
                wifiPassword = "secret123",
                wifiSecurity = QrWifiSecurity.WpaWpa2,
                wifiHidden = false,
            ),
        )
    }

    @Test
    fun validation_error() = captureMatrix("qr-generator-validation") {
        QrGeneratorScreen(
            platformContext = AndroidContext(RuntimeEnvironment.getApplication()),
            initialType = QrGeneratorType.Website,
            initialForm = QrGeneratorFormData(website = "not a url"),
        )
    }
}
