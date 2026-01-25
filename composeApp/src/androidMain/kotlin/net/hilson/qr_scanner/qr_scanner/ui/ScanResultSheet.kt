package net.hilson.qr_scanner.qr_scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.hilson.qr_scanner.qr_scanner.R
import net.hilson.qr_scanner.qr_scanner.util.QrContentType

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
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = rawValue,
                    modifier = Modifier.padding(20.dp),
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
private fun UrlActions(url: String, onDismiss: () -> Unit, context: Context) {
    val uri = Uri.parse(url)
    val isSafeScheme = uri.scheme?.lowercase() in listOf("http", "https")
    val toastCannotOpen = stringResource(R.string.toast_cannot_open)
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val toastCannotShare = stringResource(R.string.toast_cannot_share)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSafeScheme) {
            ActionButton(stringResource(R.string.action_open_browser)) {
                launchIntent(context, Intent(Intent.ACTION_VIEW, uri), toastCannotOpen)
                onDismiss()
            }
        }
        ActionButton(stringResource(R.string.action_copy)) {
            copyToClipboard(context, url, clipboardLabel, toastCopied)
            onDismiss()
        }
        ActionButton(stringResource(R.string.action_share)) {
            shareText(context, url, shareTitle, toastCannotShare)
            onDismiss()
        }
        SecondaryButton(stringResource(R.string.action_scan_again), onDismiss)
    }
}

@Composable
private fun EmailActions(email: String, onDismiss: () -> Unit, context: Context) {
    val toastCannotOpen = stringResource(R.string.toast_cannot_open)
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val toastCannotShare = stringResource(R.string.toast_cannot_share)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(R.string.action_send_email)) {
            launchIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")), toastCannotOpen)
            onDismiss()
        }
        ActionButton(stringResource(R.string.action_copy)) {
            copyToClipboard(context, email, clipboardLabel, toastCopied)
            onDismiss()
        }
        ActionButton(stringResource(R.string.action_share)) {
            shareText(context, email, shareTitle, toastCannotShare)
            onDismiss()
        }
        SecondaryButton(stringResource(R.string.action_scan_again), onDismiss)
    }
}

@Composable
private fun PhoneActions(phone: String, onDismiss: () -> Unit, context: Context) {
    val toastCannotOpen = stringResource(R.string.toast_cannot_open)
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val toastCannotShare = stringResource(R.string.toast_cannot_share)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(R.string.action_call)) {
            launchIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")), toastCannotOpen)
            onDismiss()
        }
        ActionButton(stringResource(R.string.action_copy)) {
            copyToClipboard(context, phone, clipboardLabel, toastCopied)
            onDismiss()
        }
        ActionButton(stringResource(R.string.action_share)) {
            shareText(context, phone, shareTitle, toastCannotShare)
            onDismiss()
        }
        SecondaryButton(stringResource(R.string.action_scan_again), onDismiss)
    }
}

@Composable
private fun TextActions(text: String, onDismiss: () -> Unit, context: Context) {
    val toastCopied = stringResource(R.string.toast_copied)
    val clipboardLabel = stringResource(R.string.clipboard_label_qr)
    val shareTitle = stringResource(R.string.action_share)
    val toastCannotShare = stringResource(R.string.toast_cannot_share)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(stringResource(R.string.action_copy)) {
            copyToClipboard(context, text, clipboardLabel, toastCopied)
            onDismiss()
        }
        ActionButton(stringResource(R.string.action_share)) {
            shareText(context, text, shareTitle, toastCannotShare)
            onDismiss()
        }
        SecondaryButton(stringResource(R.string.action_scan_again), onDismiss)
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
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
        Text(text = text, fontSize = 22.sp)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
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
        Text(text = text, fontSize = 22.sp)
    }
}

private fun launchIntent(context: Context, intent: Intent, errorMessage: String) {
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, text: String, label: String, successMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String, shareTitle: String, errorMessage: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, shareTitle))
    } catch (e: Exception) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
