package net.hilson.qrieux.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readValue
import kotlinx.cinterop.usePinned
import net.hilson.qrieux.IosContext
import net.hilson.qrieux.ui.theme.QRieuxUiConfig
import net.hilson.qrieux.vibrate
import platform.AVFoundation.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.CoreGraphics.CGRectZero
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_HIGH

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun DemoCameraPreview(
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundPath: String? = null
) {
    val bgBitmap = remember(backgroundPath) {
        backgroundPath?.let { path ->
            val uiImage = platform.UIKit.UIImage.imageWithContentsOfFile(path) ?: return@let null
            val data = platform.UIKit.UIImagePNGRepresentation(uiImage) ?: return@let null
            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
        }
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

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun LiveCameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isScanning: Boolean,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var flashEnabled by remember { mutableStateOf(false) }

    val cameraView = remember {
        MetadataCameraView(
            onBarcodeDetected = { payload ->
                vibrate(IosContext())
                onQrCodeDetected(payload)
            }
        )
    }

    LaunchedEffect(isScanning) {
        cameraView.isEnabled = isScanning
    }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationWillEnterForegroundNotification,
            null,
            NSOperationQueue.mainQueue
        ) { _ ->
            flashEnabled = false
            cameraView.setFlash(false)
        }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    // Force LTR so button positions don't flip in RTL locales (camera UI is ergonomic, not directional)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxSize()) {
            UIKitView(
                factory = { cameraView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    (view as? MetadataCameraView)?.updateLayout()
                },
                onRelease = { view ->
                    (view as? MetadataCameraView)?.stopCamera()
                }
            )

            ScanOverlay()
            CameraButtons(
                flashEnabled = flashEnabled,
                onFlashClick = {
                    flashEnabled = !flashEnabled
                    cameraView.setFlash(flashEnabled)
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
                .padding(top = 56.dp, end = 24.dp)
                .size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (flashEnabled) Color.White else Color.Black.copy(alpha = 0.5f),
                contentColor = if (flashEnabled) Color.Black else Color.White
            )
        ) {
            Icon(
                imageVector = if (flashEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                contentDescription = stringResource(
                    if (flashEnabled) Res.string.flash_turn_off else Res.string.flash_turn_on
                ),
                modifier = Modifier.size(32.dp)
            )
        }

        FilledIconButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 56.dp, start = 24.dp)
                .size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = stringResource(Res.string.gallery_pick_photo),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class MetadataCameraView(
    private val onBarcodeDetected: (String) -> Unit
) : UIView(frame = CGRectZero.readValue()) {

    private var captureSession: AVCaptureSession? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var device: AVCaptureDevice? = null
    var isEnabled: Boolean = true

    private val metadataDelegate = MetadataDelegate { payload ->
        if (isEnabled) {
            isEnabled = false
            dispatch_async(dispatch_get_main_queue()) {
                onBarcodeDetected(payload)
            }
        }
    }

    init {
        backgroundColor = UIColor.blackColor
        setupCamera()
    }

    private fun setupCamera() {
        val session = AVCaptureSession()
        session.sessionPreset = AVCaptureSessionPresetHigh

        val videoDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (videoDevice == null) return

        device = videoDevice

        val input = AVCaptureDeviceInput.deviceInputWithDevice(videoDevice, null)
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        }

        val metadataOutput = AVCaptureMetadataOutput()
        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(metadataDelegate, dispatch_get_main_queue())
            metadataOutput.metadataObjectTypes = listOf(
                AVMetadataObjectTypeQRCode,
                AVMetadataObjectTypeEAN8Code,
                AVMetadataObjectTypeEAN13Code,
                AVMetadataObjectTypeCode128Code,
                AVMetadataObjectTypeCode39Code,
                AVMetadataObjectTypeCode93Code,
                AVMetadataObjectTypeUPCECode,
                AVMetadataObjectTypePDF417Code,
                AVMetadataObjectTypeAztecCode,
                AVMetadataObjectTypeDataMatrixCode
            )
        }

        val preview = AVCaptureVideoPreviewLayer(session = session)
        preview.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(preview)

        previewLayer = preview
        captureSession = session

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
            session.startRunning()
        }
    }

    fun updateLayout() {
        previewLayer?.frame = bounds
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }

    fun setFlash(enabled: Boolean) {
        device?.let { dev ->
            if (dev.hasTorch) {
                try {
                    dev.lockForConfiguration(null)
                    dev.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                    dev.unlockForConfiguration()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopCamera() {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
            captureSession?.stopRunning()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class MetadataDelegate(
    private val onDetected: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        val metadataObject = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
        val payload = metadataObject?.stringValue ?: return
        onDetected(payload)
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

        val textShadow = Shadow(
            color = Color.Black,
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameTopOffset)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(Res.string.scan_instruction),
                modifier = Modifier.padding(bottom = 16.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = textShadow
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = frameTopOffset + frameSize + 16.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.scan_tip_formats),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    shadow = textShadow
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.scan_tip_gallery),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    shadow = textShadow
                )
            )
        }
    }
}
