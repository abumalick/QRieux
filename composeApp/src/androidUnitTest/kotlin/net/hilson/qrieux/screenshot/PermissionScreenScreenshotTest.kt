package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.ui.PermissionScreen

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30, 34])
class PermissionScreenScreenshotTest {

    @Test
    fun default_state() = captureMatrix("permission-screen") {
        PermissionScreen(
            showRationale = false,
            onRequestPermission = {},
        )
    }
}
