package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.ui.OnboardingScreen

// Sized as a phone held upright rather than Robolectric's default screen, which is
// shorter than any phone this app runs on and loses the step text on its own.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30, 34], qualifiers = "w411dp-h891dp-port")
class OnboardingScreenScreenshotTest {

    @Test
    fun first_step() = captureMatrix("onboarding") {
        OnboardingScreen(onFinish = {})
    }
}
