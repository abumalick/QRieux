package net.hilson.qrieux.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.hilson.qrieux.GeneratedQrCode
import net.hilson.qrieux.PlatformContext
import net.hilson.qrieux.generateQrCode
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.shareImage
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.generator_share_qr

@Composable
fun HistoryGenerateDetail(
    entry: HistoryEntry,
    platformContext: PlatformContext,
    onBack: () -> Unit,
    onEditInCreateTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var generatedQr by remember { mutableStateOf<GeneratedQrCode?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    val shareLabel = stringResource(Res.string.generator_share_qr)

    LaunchedEffect(entry.rawValue) {
        isGenerating = true
        generatedQr = withContext(Dispatchers.Default) {
            generateQrCode(entry.rawValue, size = 768)
        }
        isGenerating = false
    }

    Box(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        QrResultOverlay(
            generatedQr = generatedQr,
            isGenerating = isGenerating,
            onShare = { generatedQr?.let { shareImage(platformContext, it.pngData, shareLabel) } },
            onEdit = onEditInCreateTab,
            onBack = onBack
        )
    }
}
