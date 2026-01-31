package net.hilson.qrieux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.hilson.qrieux.IosContext
import net.hilson.qrieux.copyToClipboard
import net.hilson.qrieux.dialPhone
import net.hilson.qrieux.openUrl
import net.hilson.qrieux.sendEmail
import net.hilson.qrieux.shareText
import net.hilson.qrieux.util.QrContentType
import platform.Foundation.NSURL

@Composable
fun ScanResultOverlay(
    contentType: QrContentType,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val platformContext = IosContext()
    val toastCopied = stringResource(Res.string.toast_copied)
    val rawValue = when (contentType) {
        is QrContentType.Url -> contentType.url
        is QrContentType.Email -> contentType.email
        is QrContentType.Phone -> contentType.phone
        is QrContentType.Text -> contentType.text
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.scan_result_title),
                color = Color.White,
                fontSize = 28.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = rawValue,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val onCopied = { onShowMessage(toastCopied) }
            when (contentType) {
                is QrContentType.Url -> UrlActions(contentType.url, onDismiss, platformContext, onCopied)
                is QrContentType.Email -> EmailActions(contentType.email, onDismiss, platformContext, onCopied)
                is QrContentType.Phone -> PhoneActions(contentType.phone, onDismiss, platformContext, onCopied)
                is QrContentType.Text -> TextActions(contentType.text, onDismiss, platformContext, onCopied)
            }
        }
    }
}

@Composable
private fun UrlActions(url: String, onDismiss: () -> Unit, context: IosContext, onCopied: () -> Unit) {
    val uri = NSURL.URLWithString(url)
    val isSafeScheme = uri?.scheme?.lowercase() in listOf("http", "https")

    val clipboardLabel = stringResource(Res.string.clipboard_label_qr)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSafeScheme) {
            ActionButton(stringResource(Res.string.action_open_browser), Icons.Default.OpenInBrowser) {
                openUrl(context, url)
            }
        }
        ActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(context, url, clipboardLabel)
            onCopied()
        }
        ActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
            shareText(context, url, "")
        }
        SecondaryButton(stringResource(Res.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun EmailActions(email: String, onDismiss: () -> Unit, context: IosContext, onCopied: () -> Unit) {
    val clipboardLabel = stringResource(Res.string.clipboard_label_qr)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(Res.string.action_send_email), Icons.AutoMirrored.Filled.Send) {
            sendEmail(context, email)
        }
        ActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(context, email, clipboardLabel)
            onCopied()
        }
        ActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
            shareText(context, email, "")
        }
        SecondaryButton(stringResource(Res.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun PhoneActions(phone: String, onDismiss: () -> Unit, context: IosContext, onCopied: () -> Unit) {
    val clipboardLabel = stringResource(Res.string.clipboard_label_qr)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(Res.string.action_call), Icons.Default.Phone) {
            dialPhone(context, phone)
        }
        ActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(context, phone, clipboardLabel)
            onCopied()
        }
        ActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
            shareText(context, phone, "")
        }
        SecondaryButton(stringResource(Res.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun TextActions(text: String, onDismiss: () -> Unit, context: IosContext, onCopied: () -> Unit) {
    val clipboardLabel = stringResource(Res.string.clipboard_label_qr)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(Res.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(context, text, clipboardLabel)
            onCopied()
        }
        ActionButton(stringResource(Res.string.action_share), Icons.Default.Share) {
            shareText(context, text, "")
        }
        SecondaryButton(stringResource(Res.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}
