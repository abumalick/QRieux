package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.ui.PermissionScreen

// Open Settings is the only way back for someone who denied the permission for
// good, and it is the last thing in the column — so it is the first thing a short
// screen loses.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-land")
class PermissionScreenLandscapeScreenshotTest {

    @Test
    fun default_state() = captureLandscape("permission-screen") {
        PermissionScreen(
            showRationale = false,
            onRequestPermission = {},
            contentPadding = TAB_BAR_PADDING,
        )
    }
}
