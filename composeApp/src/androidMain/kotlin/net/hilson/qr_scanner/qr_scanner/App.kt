package net.hilson.qr_scanner.qr_scanner

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import net.hilson.qr_scanner.qr_scanner.scanner.CameraPreview
import net.hilson.qr_scanner.qr_scanner.ui.PermissionScreen
import net.hilson.qr_scanner.qr_scanner.ui.ScanResultSheet
import net.hilson.qr_scanner.qr_scanner.util.QrContentType

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App() {
    MaterialTheme {
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }
        var isScanning by remember { mutableStateOf(true) }

        if (cameraPermissionState.status.isGranted) {
            if (isScanning) {
                CameraPreview(
                    onQrCodeDetected = { rawValue ->
                        if (isScanning) {
                            isScanning = false
                            scannedContent = QrContentType.fromRawValue(rawValue)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            scannedContent?.let { content ->
                ScanResultSheet(
                    contentType = content,
                    onDismiss = {
                        scannedContent = null
                        isScanning = true
                    }
                )
            }
        } else {
            PermissionScreen(
                showRationale = cameraPermissionState.status.shouldShowRationale,
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
    }
}
