package net.hilson.qrieux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.getString
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import net.hilson.qrieux.scanner.CameraPreview
import net.hilson.qrieux.scanner.scanBarcodeFromImage
import net.hilson.qrieux.ui.PermissionScreen
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.ui.theme.QRieuxTheme
import net.hilson.qrieux.util.QrContentType
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.PhotosUI.*
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
fun App(sharedImage: UIImage? = null) {
    QRieuxTheme {
        val scope = rememberCoroutineScope()
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }
        var cameraPermissionGranted by remember { mutableStateOf(false) }
        var showRationale by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        var showPhotoPicker by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            cameraPermissionGranted = status == AVAuthorizationStatusAuthorized
            showRationale = status != AVAuthorizationStatusNotDetermined && status != AVAuthorizationStatusAuthorized
        }

        LaunchedEffect(sharedImage) {
            sharedImage?.let { image ->
                val result = scanBarcodeFromImage(image)
                if (result != null) {
                    vibrate(IosContext())
                    scannedContent = QrContentType.fromRawValue(result)
                } else {
                    snackbarHostState.showSnackbar(getString(Res.string.gallery_no_qr_found))
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black
        ) { _ ->
            if (cameraPermissionGranted) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        onQrCodeDetected = { rawValue ->
                            scannedContent = QrContentType.fromRawValue(rawValue)
                        },
                        isScanning = scannedContent == null,
                        onGalleryClick = { showPhotoPicker = true },
                        modifier = Modifier.fillMaxSize()
                    )

                    scannedContent?.let { content ->
                        ScanResultOverlay(
                            contentType = content,
                            onDismiss = { scannedContent = null },
                            onShowMessage = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    }
                }
            } else {
                PermissionScreen(
                    showRationale = showRationale,
                    onRequestPermission = {
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            dispatch_async(dispatch_get_main_queue()) {
                                cameraPermissionGranted = granted
                                showRationale = !granted
                            }
                        }
                    }
                )
            }
        }

        if (showPhotoPicker) {
            PhotoPickerLauncher(
                onImageSelected = { image ->
                    showPhotoPicker = false
                    image?.let {
                        scope.launch {
                            val result = scanBarcodeFromImage(it)
                            if (result != null) {
                                vibrate(IosContext())
                                scannedContent = QrContentType.fromRawValue(result)
                            } else {
                                snackbarHostState.showSnackbar(getString(Res.string.gallery_no_qr_found))
                            }
                        }
                    }
                },
                onDismiss = { showPhotoPicker = false }
            )
        }
    }
}
