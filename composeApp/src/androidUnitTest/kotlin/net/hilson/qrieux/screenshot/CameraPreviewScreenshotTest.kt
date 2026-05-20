package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.scanner.CameraPreview

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30, 34])
class CameraPreviewScreenshotTest {

    @Test
    fun demo_mode() = captureMatrix("camera-preview") {
        CameraPreview(
            onQrCodeDetected = {},
            isScanning = true,
            onGalleryClick = {},
            demoMode = true,
            demoBackgroundPath = null,
        )
    }
}
