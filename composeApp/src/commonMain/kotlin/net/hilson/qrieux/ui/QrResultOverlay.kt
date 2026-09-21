package net.hilson.qrieux.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import net.hilson.qrieux.GeneratedQrCode
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.generator_edit_button
import qr_scanner.composeapp.generated.resources.generator_result_title
import qr_scanner.composeapp.generated.resources.generator_share_qr
import qr_scanner.composeapp.generated.resources.navigate_back

@Composable
fun QrResultOverlay(
    generatedQr: GeneratedQrCode?,
    isGenerating: Boolean,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.TopCenter
    ) {
        val frame = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)

        if (maxWidth > maxHeight) {
            // Stacked on a landscape phone the square alone is taller than the
            // window, and pushing the buttons below it puts them off screen. Side
            // by side the code gets the whole height and the buttons the width the
            // stacked layout wasted.
            Row(
                modifier = frame
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ResultTitle(onBack ?: onEdit)
                    Spacer(modifier = Modifier.height(16.dp))
                    QrPanel(
                        generatedQr = generatedQr,
                        isGenerating = isGenerating,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        QrResultActions(generatedQr, onShare, onEdit)
                    }
                }
            }
        } else {
            Column(
                // Cap content width so the square QR container doesn't blow past the viewport
                // on tablets (where fillMaxWidth + aspectRatio(1f) would be ~2560dp).
                modifier = frame
                    .widthIn(max = 500.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ResultTitle(onBack ?: onEdit)
                Spacer(modifier = Modifier.height(24.dp))
                QrPanel(
                    generatedQr = generatedQr,
                    isGenerating = isGenerating,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
                Spacer(modifier = Modifier.height(32.dp))
                QrResultActions(generatedQr, onShare, onEdit)
            }
        }
    }
}

@Composable
private fun ResultTitle(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { testTag = "qr_result_back" }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.navigate_back),
                tint = Color.White
            )
        }
        Text(
            text = stringResource(Res.string.generator_result_title),
            color = Color.White,
            fontSize = 28.sp,
            modifier = Modifier.weight(1f).semantics { testTag = "qr_result_title" },
            textAlign = TextAlign.Center
        )
        // Balance the row so title stays centered
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun QrPanel(
    generatedQr: GeneratedQrCode?,
    isGenerating: Boolean,
    modifier: Modifier
) {
    // A code that runs off the screen cannot be scanned, which is the whole point
    // of the screen, so the panel takes the shorter side of the space it is given
    // rather than the width alone.
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(min(maxWidth, maxHeight))
                .background(Color.White, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator()
            } else if (generatedQr != null) {
                Image(
                    bitmap = generatedQr.image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun QrResultActions(
    generatedQr: GeneratedQrCode?,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onShare,
            enabled = generatedQr != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.generator_share_qr),
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
        }

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.generator_edit_button),
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
