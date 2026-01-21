package net.hilson.qr_scanner.qr_scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.hilson.qr_scanner.qr_scanner.util.QrContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultSheet(
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Scanned Result",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = rawValue,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton("Open in Browser") {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            onDismiss()
        }
        ActionButton("Copy") {
            copyToClipboard(context, url)
            onDismiss()
        }
        ActionButton("Share") {
            shareText(context, url)
            onDismiss()
        }
        SecondaryButton("Scan Again", onDismiss)
    }
}

@Composable
private fun EmailActions(email: String, onDismiss: () -> Unit, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton("Send Email") {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
            onDismiss()
        }
        ActionButton("Copy") {
            copyToClipboard(context, email)
            onDismiss()
        }
        ActionButton("Share") {
            shareText(context, email)
            onDismiss()
        }
        SecondaryButton("Scan Again", onDismiss)
    }
}

@Composable
private fun PhoneActions(phone: String, onDismiss: () -> Unit, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton("Call") {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            onDismiss()
        }
        ActionButton("Copy") {
            copyToClipboard(context, phone)
            onDismiss()
        }
        ActionButton("Share") {
            shareText(context, phone)
            onDismiss()
        }
        SecondaryButton("Scan Again", onDismiss)
    }
}

@Composable
private fun TextActions(text: String, onDismiss: () -> Unit, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton("Copy") {
            copyToClipboard(context, text)
            onDismiss()
        }
        ActionButton("Share") {
            shareText(context, text)
            onDismiss()
        }
        SecondaryButton("Scan Again", onDismiss)
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, fontSize = 20.sp)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, fontSize = 20.sp)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
