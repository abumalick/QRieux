package net.hilson.qrieux

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hilson.qrieux.scanner.CameraPreview
import net.hilson.qrieux.scanner.scanBarcodeFromUri
import net.hilson.qrieux.ui.HelpScreen
import net.hilson.qrieux.ui.HistoryScreen
import net.hilson.qrieux.ui.HistoryGenerateDetail
import net.hilson.qrieux.ui.HistoryScanDetailScreen
import net.hilson.qrieux.ui.OnboardingScreen
import net.hilson.qrieux.ui.PermissionScreen
import net.hilson.qrieux.ui.QrGeneratorScreen
import net.hilson.qrieux.ui.ScanResultOverlay
import net.hilson.qrieux.ui.theme.QRieuxTheme
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.history.HistoryEntryType
import net.hilson.qrieux.history.addHistoryEntry
import net.hilson.qrieux.history.reverseParseToFormData
import net.hilson.qrieux.util.QrContentType
import net.hilson.qrieux.vibrate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import org.jetbrains.compose.resources.stringResource as sharedStringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*

private enum class AppMode {
    Scan,
    Generate,
    History,
    Help
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App(
    sharedImageUri: Uri? = null,
    shareTimestamp: Long = 0L,
    screenshotBackground: String? = null,
    sharedText: String? = null,
    shareTextTimestamp: Long = 0L,
    demoCamera: Boolean = false
) {
    QRieuxTheme {
        val context = LocalContext.current
        val platformContext = AndroidContext(context)
        val scope = rememberCoroutineScope()
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
        var scannedContent by remember { mutableStateOf<QrContentType?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val noQrFoundMessage = stringResource(R.string.gallery_no_qr_found)
        var appMode by remember { mutableStateOf(if (sharedText != null) AppMode.Generate else AppMode.Scan) }
        var showOnboarding by remember {
            mutableStateOf(!demoCamera && sharedImageUri == null && sharedText == null && !isOnboardingCompleted(platformContext))
        }
        var historyDetailEntry by remember { mutableStateOf<HistoryEntry?>(null) }
        var pendingEditEntry by remember { mutableStateOf<HistoryEntry?>(null) }

        LaunchedEffect(shareTimestamp) {
            if (shareTimestamp > 0L) {
                sharedImageUri?.let { uri ->
                    appMode = AppMode.Scan
                    delay(100)
                    val result = scanBarcodeFromUri(context, uri)
                    if (result != null) {
                        vibrate(AndroidContext(context))
                        scannedContent = QrContentType.fromRawValue(result)
                        addHistoryEntry(platformContext, HistoryEntry(generateUuid(), currentTimeMillis(), HistoryEntryType.SCAN, result))
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
                        addHistoryEntry(platformContext, HistoryEntry(generateUuid(), currentTimeMillis(), HistoryEntryType.SCAN, result))
                    } else {
                        snackbarHostState.showSnackbar(noQrFoundMessage)
                    }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Outer scaffold has no topBar — zero contentWindowInsets so paddingValues.top is 0,
            // otherwise the systemBars default leaves a blank status-bar strip above each screen.
            // Inner Scaffolds / windowInsetsPadding handle their own top insets.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (!showOnboarding) {
                    NavigationBar {
                        NavigationBarItem(
                            modifier = Modifier.semantics { testTag = "tab_scan" },
                            selected = appMode == AppMode.Scan,
                            onClick = { appMode = AppMode.Scan },
                            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_scan)) }
                        )
                        NavigationBarItem(
                            modifier = Modifier.semantics { testTag = "tab_create" },
                            selected = appMode == AppMode.Generate,
                            onClick = { appMode = AppMode.Generate; pendingEditEntry = null },
                            icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_create)) }
                        )
                        NavigationBarItem(
                            modifier = Modifier.semantics { testTag = "tab_history" },
                            selected = appMode == AppMode.History,
                            onClick = { appMode = AppMode.History; historyDetailEntry = null },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_history)) }
                        )
                        NavigationBarItem(
                            modifier = Modifier.semantics { testTag = "tab_help" },
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
                    val editEntry = pendingEditEntry
                    val editData = editEntry?.let { reverseParseToFormData(it.rawValue, it.generatorType) }
                    key(shareTextTimestamp, editEntry?.id) {
                        QrGeneratorScreen(
                            platformContext = platformContext,
                            modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues),
                            initialText = if (editEntry == null) sharedText else null,
                            initialType = editData?.first,
                            initialForm = editData?.second,
                            onGenerated = { payload, type ->
                                addHistoryEntry(platformContext, HistoryEntry(
                                    generateUuid(), currentTimeMillis(),
                                    HistoryEntryType.GENERATE, payload, type.name
                                ))
                            }
                        )
                    }
                }
                appMode == AppMode.History -> {
                    val detail = historyDetailEntry
                    if (detail != null) {
                        when (detail.type) {
                            HistoryEntryType.SCAN -> {
                                HistoryScanDetailScreen(
                                    entry = detail,
                                    platformContext = platformContext,
                                    onBack = { historyDetailEntry = null },
                                    modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues)
                                )
                            }
                            HistoryEntryType.GENERATE -> {
                                HistoryGenerateDetail(
                                    entry = detail,
                                    platformContext = platformContext,
                                    onBack = { historyDetailEntry = null },
                                    onEditInCreateTab = {
                                        pendingEditEntry = detail
                                        historyDetailEntry = null
                                        appMode = AppMode.Generate
                                    },
                                    modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues)
                                )
                            }
                        }
                    } else {
                        HistoryScreen(
                            platformContext = platformContext,
                            onEntryClick = { historyDetailEntry = it },
                            modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues)
                        )
                    }
                }
                appMode == AppMode.Help -> {
                    HelpScreen(modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues))
                }
                cameraPermissionState.status.isGranted -> {
                    // Scanner is edge-to-edge: camera extends behind the status bar/cutout.
                    // CameraButtons and ScanResultOverlay apply their own top insets.
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraPreview(
                            onQrCodeDetected = { rawValue ->
                                scannedContent = QrContentType.fromRawValue(rawValue)
                                addHistoryEntry(platformContext, HistoryEntry(generateUuid(), currentTimeMillis(), HistoryEntryType.SCAN, rawValue))
                            },
                            isScanning = scannedContent == null,
                            onGalleryClick = {
                                pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxSize(),
                            demoMode = demoCamera,
                            demoBackgroundPath = screenshotBackground
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
