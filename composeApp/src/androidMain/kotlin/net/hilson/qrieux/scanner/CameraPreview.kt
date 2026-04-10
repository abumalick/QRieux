package net.hilson.qrieux.scanner

import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import net.hilson.qrieux.AndroidContext
import net.hilson.qrieux.R
import net.hilson.qrieux.vibrate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary

@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isScanning: Boolean,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
    demoMode: Boolean = false,
    demoBackgroundPath: String? = null
) {
    if (demoMode) {
        DemoCameraPreview(
            onGalleryClick = onGalleryClick,
            modifier = modifier,
            backgroundPath = demoBackgroundPath
        )
    } else {
        LiveCameraPreview(
            onQrCodeDetected = onQrCodeDetected,
            isScanning = isScanning,
            onGalleryClick = onGalleryClick,
            modifier = modifier
        )
    }
}

@Composable
private fun DemoCameraPreview(
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundPath: String? = null
) {
    val bgBitmap = remember(backgroundPath) {
        backgroundPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxSize()) {
            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF1A1A2E))
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF16213E), Color(0xFF0F3460)),
                            center = center,
                            radius = size.minDimension * 0.8f
                        )
                    )
                }
            }

            Box(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                ScanOverlay()
            }
            CameraButtons(
                flashEnabled = false,
                onFlashClick = {},
                onGalleryClick = onGalleryClick
            )
        }
    }
}

@Composable
private fun LiveCameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isScanning: Boolean,
    onGalleryClick: () -> Unit,
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                flashEnabled = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cameraExecutor = remember(lifecycleOwner) { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Force LTR so button positions don't flip in RTL locales (camera UI is ergonomic, not directional)
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
            CameraButtons(
                flashEnabled = flashEnabled,
                onFlashClick = {
                    flashEnabled = !flashEnabled
                    camera?.cameraControl?.enableTorch(flashEnabled)
                },
                onGalleryClick = onGalleryClick
            )
        }
    }
}

@Composable
private fun CameraButtons(
    flashEnabled: Boolean,
    onFlashClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        FilledIconButton(
            onClick = onFlashClick,
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
                .align(Alignment.TopStart)
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

@Composable
internal fun ScanOverlay() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val frameSize = minOf(maxWidth, maxHeight) * 0.7f
        val frameTopOffset = (maxHeight - frameSize) / 2

        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayColor = Color.Black.copy(alpha = 0.6f)
            val frameSizePx = size.minDimension * 0.7f
            val frameLeft = (size.width - frameSizePx) / 2
            val frameTop = (size.height - frameSizePx) / 2

            drawRect(color = overlayColor)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSizePx, frameSizePx),
                cornerRadius = CornerRadius(24.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameSizePx, frameSizePx),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameTopOffset)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(R.string.scan_instruction),
                modifier = Modifier.padding(bottom = 16.dp),
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
}
