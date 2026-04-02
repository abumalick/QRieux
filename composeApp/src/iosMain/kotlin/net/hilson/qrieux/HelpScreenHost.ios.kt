package net.hilson.qrieux

import androidx.compose.runtime.Composable
import net.hilson.qrieux.ui.HelpScreen
import net.hilson.qrieux.ui.theme.QRieuxTheme

@Composable
fun HelpScreenHost() {
    QRieuxTheme {
        HelpScreen()
    }
}
