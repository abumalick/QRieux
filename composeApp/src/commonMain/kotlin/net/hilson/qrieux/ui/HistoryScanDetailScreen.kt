package net.hilson.qrieux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.hilson.qrieux.PlatformContext
import net.hilson.qrieux.addContact
import net.hilson.qrieux.connectToWifi
import net.hilson.qrieux.copyToClipboard
import net.hilson.qrieux.dialPhone
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.openUrl
import net.hilson.qrieux.sendEmail
import net.hilson.qrieux.shareText
import net.hilson.qrieux.util.QrContentType
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.action_call
import qr_scanner.composeapp.generated.resources.action_add_contact
import qr_scanner.composeapp.generated.resources.action_connect_wifi
import qr_scanner.composeapp.generated.resources.action_copy
import qr_scanner.composeapp.generated.resources.action_open_browser
import qr_scanner.composeapp.generated.resources.action_send_email
import qr_scanner.composeapp.generated.resources.action_share
import qr_scanner.composeapp.generated.resources.clipboard_label_qr
import qr_scanner.composeapp.generated.resources.history_back
import qr_scanner.composeapp.generated.resources.navigate_back
import qr_scanner.composeapp.generated.resources.scan_result_title
import qr_scanner.composeapp.generated.resources.toast_copied
import qr_scanner.composeapp.generated.resources.wifi_copy_password

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScanDetailScreen(
    entry: HistoryEntry,
    platformContext: PlatformContext,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentType = remember(entry) { QrContentType.fromRawValue(entry.rawValue) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMsg = stringResource(Res.string.toast_copied)
    val clipLabel = stringResource(Res.string.clipboard_label_qr)

    val rawDisplay = when (contentType) {
        is QrContentType.Url -> contentType.url
        is QrContentType.Email -> contentType.email
        is QrContentType.Phone -> contentType.phone
        is QrContentType.Wifi -> contentType.ssid
        is QrContentType.Contact -> buildString {
            append(contentType.fullName)
            if (contentType.phone.isNotEmpty()) append("\n${contentType.phone}")
            if (contentType.email.isNotEmpty()) append("\n${contentType.email}")
            if (contentType.organization.isNotEmpty()) append("\n${contentType.organization}")
        }
        is QrContentType.Text -> contentType.text
    }

    fun onCopy(text: String) {
        copyToClipboard(platformContext, text, clipLabel)
        scope.launch { snackbarHostState.showSnackbar(copiedMsg) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.scan_result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = rawDisplay,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (contentType) {
                    is QrContentType.Url -> {
                        val safe = contentType.url.startsWith("http://", true) ||
                                contentType.url.startsWith("https://", true)
                        if (safe) {
                            DetailActionButton(stringResource(Res.string.action_open_browser), Icons.Default.OpenInBrowser) {
                                openUrl(platformContext, contentType.url)
                            }
                        }
                        DetailActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
                            onCopy(contentType.url)
                        }
                        DetailActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
                            shareText(platformContext, contentType.url, "")
                        }
                    }
                    is QrContentType.Email -> {
                        DetailActionButton(stringResource(Res.string.action_send_email), Icons.AutoMirrored.Filled.Send) {
                            sendEmail(platformContext, contentType.email)
                        }
                        DetailActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
                            onCopy(contentType.email)
                        }
                        DetailActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
                            shareText(platformContext, contentType.email, "")
                        }
                    }
                    is QrContentType.Phone -> {
                        DetailActionButton(stringResource(Res.string.action_call), Icons.Default.Phone) {
                            dialPhone(platformContext, contentType.phone)
                        }
                        DetailActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
                            onCopy(contentType.phone)
                        }
                        DetailActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
                            shareText(platformContext, contentType.phone, "")
                        }
                    }
                    is QrContentType.Wifi -> {
                        var connecting by remember { mutableStateOf(false) }
                        DetailActionButton(
                            text = stringResource(Res.string.action_connect_wifi),
                            icon = Icons.Default.Wifi,
                            enabled = !connecting
                        ) {
                            connecting = true
                            connectToWifi(platformContext, contentType.ssid, contentType.password, contentType.authType, contentType.hidden) {
                                connecting = false
                            }
                        }
                        if (contentType.password.isNotEmpty()) {
                            DetailActionButton(stringResource(Res.string.wifi_copy_password), Icons.Default.ContentCopy) {
                                onCopy(contentType.password)
                            }
                        }
                        DetailActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
                            shareText(platformContext, contentType.ssid, "")
                        }
                    }
                    is QrContentType.Contact -> {
                        DetailActionButton(stringResource(Res.string.action_add_contact), Icons.Default.PersonAdd) {
                            addContact(platformContext, contentType.rawVCard)
                        }
                        if (contentType.phone.isNotEmpty()) {
                            DetailActionButton(stringResource(Res.string.action_call), Icons.Default.Phone) {
                                dialPhone(platformContext, contentType.phone)
                            }
                        }
                        if (contentType.email.isNotEmpty()) {
                            DetailActionButton(stringResource(Res.string.action_send_email), Icons.AutoMirrored.Filled.Send) {
                                sendEmail(platformContext, contentType.email)
                            }
                        }
                        DetailActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
                            shareText(platformContext, contentType.rawVCard, "")
                        }
                    }
                    is QrContentType.Text -> {
                        DetailActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
                            onCopy(contentType.text)
                        }
                        DetailActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
                            shareText(platformContext, contentType.text, "")
                        }
                    }
                }

                DetailSecondaryButton(
                    text = stringResource(Res.string.history_back),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = text, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DetailSecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = text, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}
