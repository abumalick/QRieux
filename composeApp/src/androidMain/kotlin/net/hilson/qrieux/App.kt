package net.hilson.qrieux

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import net.hilson.qrieux.ui.theme.QRieuxTheme
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import net.hilson.qrieux.scanner.CameraPreview
import net.hilson.qrieux.ui.PermissionScreen
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.util.QrContentType

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App() {
    QRieuxTheme {
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }

        if (cameraPermissionState.status.isGranted) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    onQrCodeDetected = { rawValue ->
                        if (scannedContent == null) {
                            scannedContent = QrContentType.fromRawValue(rawValue)
                        }
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
