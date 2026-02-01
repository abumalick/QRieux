package net.hilson.qrieux.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import net.hilson.qrieux.IosContext
import net.hilson.qrieux.vibrate
import platform.AVFoundation.*
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

        FilledIconButton(
            onClick = {
                flashEnabled = !flashEnabled
                cameraView.setFlash(flashEnabled)
            },
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
            text = stringResource(Res.string.scan_instruction),
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
