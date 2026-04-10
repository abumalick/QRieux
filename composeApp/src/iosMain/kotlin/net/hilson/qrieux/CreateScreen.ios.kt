package net.hilson.qrieux

import androidx.compose.runtime.Composable
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.history.HistoryEntryType
import net.hilson.qrieux.history.addHistoryEntry
import net.hilson.qrieux.ui.QrGeneratorScreen
import net.hilson.qrieux.ui.theme.QRieuxTheme

@Composable
fun CreateScreen(initialText: String? = null) {
    QRieuxTheme {
        val platformContext = IosContext()
        QrGeneratorScreen(
            platformContext = platformContext,
            initialText = initialText,
            onGenerated = { payload, type ->
                addHistoryEntry(platformContext, HistoryEntry(
                    generateUuid(), currentTimeMillis(),
                    HistoryEntryType.GENERATE, payload, type.name
                ))
            }
        )
    }
}
