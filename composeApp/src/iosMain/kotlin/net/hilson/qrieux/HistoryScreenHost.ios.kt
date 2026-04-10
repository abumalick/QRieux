package net.hilson.qrieux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.history.HistoryEntryType
import net.hilson.qrieux.history.percentEncode
import net.hilson.qrieux.ui.HistoryGenerateDetail
import net.hilson.qrieux.ui.HistoryScreen
import net.hilson.qrieux.ui.HistoryScanDetailScreen
import net.hilson.qrieux.ui.theme.QRieuxTheme

@Composable
fun HistoryScreenHost() {
    QRieuxTheme {
        val platformContext = IosContext()
        var historyDetailEntry by remember { mutableStateOf<HistoryEntry?>(null) }

        val detail = historyDetailEntry
        if (detail != null) {
            when (detail.type) {
                HistoryEntryType.SCAN -> {
                    HistoryScanDetailScreen(
                        entry = detail,
                        platformContext = platformContext,
                        onBack = { historyDetailEntry = null }
                    )
                }
                HistoryEntryType.GENERATE -> {
                    HistoryGenerateDetail(
                        entry = detail,
                        platformContext = platformContext,
                        onBack = { historyDetailEntry = null },
                        onEditInCreateTab = {
                            historyDetailEntry = null
                            val encoded = percentEncode(detail.rawValue)
                            openUrl(platformContext, "qrieux://create?text=$encoded")
                        }
                    )
                }
            }
        } else {
            HistoryScreen(
                platformContext = platformContext,
                onEntryClick = { historyDetailEntry = it }
            )
        }
    }
}

