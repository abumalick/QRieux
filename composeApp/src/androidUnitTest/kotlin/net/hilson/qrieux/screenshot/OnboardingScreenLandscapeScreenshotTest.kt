package net.hilson.qrieux.screenshot

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import net.hilson.qrieux.ui.OnboardingScreen

// Every step is laid out the same way, so one page is enough to tell whether the
// words survive a landscape window — and the words are the whole point of the
// screen: a first-run user who only sees the pictures learns nothing.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-land")
class OnboardingScreenLandscapeScreenshotTest {

    @Test
    fun first_step() = captureLandscape("onboarding") {
        OnboardingScreen(onFinish = {})
    }
}
