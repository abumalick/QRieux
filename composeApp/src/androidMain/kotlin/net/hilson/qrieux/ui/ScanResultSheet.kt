package net.hilson.qrieux.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.hilson.qrieux.R
import net.hilson.qrieux.util.QrContentType
import net.hilson.qrieux.AndroidContext
import net.hilson.qrieux.copyToClipboard
import net.hilson.qrieux.dialPhone
import net.hilson.qrieux.openUrl
import net.hilson.qrieux.sendEmail
import net.hilson.qrieux.shareText
import net.hilson.qrieux.showToast

@Composable
fun ScanResultOverlay(
    contentType: QrContentType,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
                text = stringResource(R.string.scan_result_title),
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

            when (contentType) {
                is QrContentType.Url -> UrlActions(contentType.url, onDismiss, context)
                is QrContentType.Email -> EmailActions(contentType.email, onDismiss, context)
                is QrContentType.Phone -> PhoneActions(contentType.phone, onDismiss, context)
                is QrContentType.Text -> TextActions(contentType.text, onDismiss, context)
            }
        }
    }
}

@Composable
private fun UrlActions(url: String, onDismiss: () -> Unit, context: android.content.Context) {
    val uri = Uri.parse(url)
    val isSafeScheme = uri.scheme?.lowercase() in listOf("http", "https")
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val platformContext = AndroidContext(context)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSafeScheme) {
            ActionButton(stringResource(R.string.action_open_browser), Icons.Default.OpenInBrowser) {
                openUrl(platformContext, url)
            }
        }
        ActionButton(stringResource(R.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(platformContext, url, clipboardLabel)
            showToast(platformContext, toastCopied)
        }
        ActionButton(stringResource(R.string.action_share), Icons.Default.Share) {
            shareText(platformContext, url, shareTitle)
        }
        SecondaryButton(stringResource(R.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun EmailActions(email: String, onDismiss: () -> Unit, context: android.content.Context) {
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val platformContext = AndroidContext(context)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(R.string.action_send_email), Icons.AutoMirrored.Filled.Send) {
            sendEmail(platformContext, email)
        }
        ActionButton(stringResource(R.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(platformContext, email, clipboardLabel)
            showToast(platformContext, toastCopied)
        }
        ActionButton(stringResource(R.string.action_share), Icons.Default.Share) {
            shareText(platformContext, email, shareTitle)
        }
        SecondaryButton(stringResource(R.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun PhoneActions(phone: String, onDismiss: () -> Unit, context: android.content.Context) {
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val platformContext = AndroidContext(context)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(R.string.action_call), Icons.Default.Phone) {
            dialPhone(platformContext, phone)
        }
        ActionButton(stringResource(R.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(platformContext, phone, clipboardLabel)
            showToast(platformContext, toastCopied)
        }
        ActionButton(stringResource(R.string.action_share), Icons.Default.Share) {
            shareText(platformContext, phone, shareTitle)
        }
        SecondaryButton(stringResource(R.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
    }
}

@Composable
private fun TextActions(text: String, onDismiss: () -> Unit, context: android.content.Context) {
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val platformContext = AndroidContext(context)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(R.string.action_copy), Icons.Default.ContentCopy) {
            copyToClipboard(platformContext, text, clipboardLabel)
            showToast(platformContext, toastCopied)
        }
        ActionButton(stringResource(R.string.action_share), Icons.Default.Share) {
            shareText(platformContext, text, shareTitle)
        }
        SecondaryButton(stringResource(R.string.action_scan_again), Icons.Default.QrCodeScanner, onDismiss)
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

