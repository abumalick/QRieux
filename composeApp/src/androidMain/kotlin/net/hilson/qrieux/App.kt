package net.hilson.qrieux

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hilson.qrieux.scanner.CameraPreview
import net.hilson.qrieux.scanner.ScanOverlay
import net.hilson.qrieux.scanner.scanBarcodeFromUri
import net.hilson.qrieux.ui.HelpScreen
import net.hilson.qrieux.ui.OnboardingScreen
import net.hilson.qrieux.ui.PermissionScreen
import net.hilson.qrieux.ui.QrGeneratorScreen
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.ui.theme.QRieuxTheme
import net.hilson.qrieux.util.QrContentType
import net.hilson.qrieux.vibrate
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import org.jetbrains.compose.resources.stringResource as sharedStringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*

private enum class AppMode {
    Scan,
    Generate,
    Help
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App(
    sharedImageUri: Uri? = null,
    shareTimestamp: Long = 0L,
    screenshotContent: String? = null,
    screenshotBackground: String? = null,
    sharedText: String? = null,
    shareTextTimestamp: Long = 0L
) {
    QRieuxTheme {
        // Screenshot mode
        if (screenshotContent != null) {
            val bgBitmap = remember(screenshotBackground) {
                screenshotBackground?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
            }
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
                                    modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).size(64.dp),
                                    colors = btnColors
                                ) { Icon(Icons.Default.FlashOn, null, Modifier.size(32.dp)) }
                                FilledIconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.TopStart).padding(24.dp).size(64.dp),
                                    colors = btnColors
                                ) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(32.dp)) }
                            }
                        }
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

        val context = LocalContext.current
        val platformContext = AndroidContext(context)
        val scope = rememberCoroutineScope()
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val noQrFoundMessage = stringResource(R.string.gallery_no_qr_found)
        var appMode by remember { mutableStateOf(if (sharedText != null) AppMode.Generate else AppMode.Scan) }
        var showOnboarding by remember {
            mutableStateOf(sharedImageUri == null && sharedText == null && !isOnboardingCompleted(platformContext))
        }

        LaunchedEffect(shareTimestamp) {
            if (shareTimestamp > 0L) {
                sharedImageUri?.let { uri ->
                    appMode = AppMode.Scan
                    delay(100)
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

        LaunchedEffect(shareTextTimestamp) {
            if (shareTextTimestamp > 0L && sharedText != null) {
                appMode = AppMode.Generate
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!showOnboarding) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = appMode == AppMode.Scan,
                            onClick = { appMode = AppMode.Scan },
                            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_scan)) }
                        )
                        NavigationBarItem(
                            selected = appMode == AppMode.Generate,
                            onClick = { appMode = AppMode.Generate },
                            icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_create)) }
                        )
                        NavigationBarItem(
                            selected = appMode == AppMode.Help,
                            onClick = { appMode = AppMode.Help },
                            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_help)) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            when {
                showOnboarding -> {
                    OnboardingScreen(onFinish = {
                        setOnboardingCompleted(platformContext)
                        showOnboarding = false
                    })
                }
                appMode == AppMode.Generate -> {
                    key(shareTextTimestamp) {
                        QrGeneratorScreen(
                            platformContext = platformContext,
                            modifier = Modifier.padding(paddingValues),
                            initialText = sharedText
                        )
                    }
                }
                appMode == AppMode.Help -> {
                    HelpScreen(modifier = Modifier.padding(paddingValues))
                }
                cameraPermissionState.status.isGranted -> {
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
                }
                else -> {
                    PermissionScreen(
                        showRationale = cameraPermissionState.status.shouldShowRationale,
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
            }
        }
    }
}
