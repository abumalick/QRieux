package net.hilson.qrieux

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hilson.qrieux.scanner.CameraPreview
import net.hilson.qrieux.scanner.scanBarcodeFromUri
import net.hilson.qrieux.ui.PermissionScreen
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.ui.theme.QRieuxTheme
import net.hilson.qrieux.util.QrContentType
import net.hilson.qrieux.vibrate

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App(sharedImageUri: Uri? = null, shareTimestamp: Long = 0L) {
    QRieuxTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val noQrFoundMessage = stringResource(R.string.gallery_no_qr_found)

        LaunchedEffect(shareTimestamp) {
            if (shareTimestamp > 0L) {
                sharedImageUri?.let { uri ->
                    delay(100) // ensure UI is ready
                    val result = scanBarcodeFromUri(context, uri)
                    if (result != null) {
                        vibrate(AndroidContext(context))
                        scannedContent = QrContentType.fromRawValue(result)
                    } else {
                        snackbarHostState.showSnackbar(noQrFoundMessage)
                    }
                }
            }
        }

        val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
            uri?.let {
                scope.launch {
                    val result = scanBarcodeFromUri(context, it)
                    if (result != null) {
                        vibrate(AndroidContext(context))
                        scannedContent = QrContentType.fromRawValue(result)
                    } else {
                        snackbarHostState.showSnackbar(noQrFoundMessage)
                    }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (cameraPermissionState.status.isGranted) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    CameraPreview(
                        onQrCodeDetected = { rawValue ->
                            scannedContent = QrContentType.fromRawValue(rawValue)
                        },
                        isScanning = scannedContent == null,
                        onGalleryClick = {
                            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    scannedContent?.let { content ->
                        ScanResultOverlay(
                            contentType = content,
                            onDismiss = { scannedContent = null }
                        )
                    }
                }
            } else {
                PermissionScreen(
                    showRationale = cameraPermissionState.status.shouldShowRationale,
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }
    }
}
