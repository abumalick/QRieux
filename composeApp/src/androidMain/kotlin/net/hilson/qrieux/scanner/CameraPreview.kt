package net.hilson.qrieux.scanner

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import net.hilson.qrieux.AndroidContext
import net.hilson.qrieux.R
import net.hilson.qrieux.vibrate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoLibrary

@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isScanning: Boolean,
    onGalleryClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var flashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val qrAnalyzer = remember { QrAnalyzer { qrValue ->
        vibrate(AndroidContext(context))
        onQrCodeDetected(qrValue)
    } }

    LaunchedEffect(isScanning) {
        qrAnalyzer.isEnabled = isScanning
    }

    val cameraExecutor = remember(lifecycleOwner) { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Force LTR so button positions don't flip in RTL locales (camera UI is ergonomic, not directional)
    val originalLayoutDirection = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, qrAnalyzer)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            ScanOverlay()

            FilledIconButton(
                onClick = onHelpClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.help),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = {
                    flashEnabled = !flashEnabled
                    camera?.cameraControl?.enableTorch(flashEnabled)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (flashEnabled) Color.White else Color.Black.copy(alpha = 0.5f),
                    contentColor = if (flashEnabled) Color.Black else Color.White
                )
            ) {
                Icon(
                    painter = painterResource(
                        if (flashEnabled) R.drawable.ic_flash_off else R.drawable.ic_flash_on
                    ),
                    contentDescription = stringResource(
                        if (flashEnabled) R.string.flash_turn_off else R.string.flash_turn_on
                    ),
                    modifier = Modifier.size(32.dp)
                )
            }

            FilledIconButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.gallery_pick_photo),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayColor = Color.Black.copy(alpha = 0.6f)
            val frameSize = size.minDimension * 0.7f
            val frameLeft = (size.width - frameSize) / 2
            val frameTop = (size.height - frameSize) / 2

            drawRect(color = overlayColor)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(24.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Text(
            text = stringResource(R.string.scan_instruction),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 380.dp, start = 32.dp, end = 32.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

