package net.hilson.qrieux

import androidx.compose.runtime.Composable
import net.hilson.qrieux.ui.QrGeneratorScreen
import net.hilson.qrieux.ui.theme.QRieuxTheme

@Composable
fun CreateScreen(initialText: String? = null) {
    QRieuxTheme {
        QrGeneratorScreen(
            platformContext = IosContext(),
            initialText = initialText
        )
    }
}
