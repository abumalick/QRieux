package net.hilson.qrieux

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.getString
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import net.hilson.qrieux.scanner.CameraPreview
import net.hilson.qrieux.scanner.ScanOverlay
import net.hilson.qrieux.scanner.scanBarcodeFromImage
import net.hilson.qrieux.ui.OnboardingScreen
import net.hilson.qrieux.ui.PermissionScreen
import net.hilson.qrieux.ui.QrGeneratorScreen
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
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private enum class AppMode {
    Scan,
    Generate
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun App(sharedImage: UIImage? = null) {
    QRieuxTheme {
        val screenshotContent = NSProcessInfo.processInfo.environment["SCREENSHOT_CONTENT"] as? String
        if (screenshotContent != null) {
            val bgPath = NSProcessInfo.processInfo.environment["SCREENSHOT_BACKGROUND"] as? String
            val bgBitmap = remember(bgPath) { bgPath?.let { loadImageBitmap(it) } }
            Scaffold(containerColor = Color.Black) { _ ->
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    bgBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (screenshotContent == "__SCANNER__") {
                        val originalDirection = LocalLayoutDirection.current
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) { ScanOverlay() }
                                val btnColors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    contentColor = Color.White
                                )
                                FilledIconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 24.dp).size(64.dp),
                                    colors = btnColors
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides originalDirection) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, null, Modifier.size(32.dp))
                                    }
                                }
                                FilledIconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 24.dp).size(64.dp),
                                    colors = btnColors
                                ) { Icon(Icons.Default.FlashOn, null, Modifier.size(32.dp)) }
                                FilledIconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).size(64.dp),
                                    colors = btnColors
                                ) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(32.dp)) }
                                Button(
                                    onClick = {},
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(24.dp)
                                        .height(net.hilson.qrieux.ui.theme.QRieuxUiConfig.controlHeight),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(28.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = org.jetbrains.compose.resources.stringResource(Res.string.action_create_qr),
                                        fontSize = net.hilson.qrieux.ui.theme.QRieuxUiConfig.buttonSize
                                    )
                                }
                            }
                        }
                    } else if (screenshotContent == "__GENERATOR__") {
                        QrGeneratorScreen(
                            platformContext = IosContext(),
                            onBack = {},
                            screenshotPayload = "https://www.wikipedia.org"
                        )
                    } else {
                        ScanResultOverlay(
                            contentType = QrContentType.fromRawValue(screenshotContent),
                            onDismiss = {},
                        )
                    }
                }
            }
            return@QRieuxTheme
        }

        val platformContext = IosContext()
        val scope = rememberCoroutineScope()
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }
        var cameraPermissionGranted by remember { mutableStateOf(false) }
        var showRationale by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        var appMode by remember { mutableStateOf(AppMode.Scan) }
        var showOnboarding by remember {
            mutableStateOf(sharedImage == null && !isOnboardingCompleted(platformContext))
        }

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
            if (appMode == AppMode.Generate) {
                QrGeneratorScreen(
                    platformContext = platformContext,
                    onBack = { appMode = AppMode.Scan }
                )
            } else if (showOnboarding) {
                OnboardingScreen(onFinish = {
                    setOnboardingCompleted(platformContext)
                    showOnboarding = false
                })
            } else if (cameraPermissionGranted) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        onQrCodeDetected = { rawValue ->
                            scannedContent = QrContentType.fromRawValue(rawValue)
                        },
                        isScanning = scannedContent == null,
                        onGalleryClick = { showPhotoPicker = true },
                        onCreateQrClick = { appMode = AppMode.Generate },
                        onHelpClick = { showOnboarding = true },
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
                    onCreateQr = { appMode = AppMode.Generate },
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

@OptIn(ExperimentalForeignApi::class)
private fun loadImageBitmap(path: String): ImageBitmap? {
    val uiImage = UIImage.imageWithContentsOfFile(path) ?: return null
    val data = UIImagePNGRepresentation(uiImage) ?: return null
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
