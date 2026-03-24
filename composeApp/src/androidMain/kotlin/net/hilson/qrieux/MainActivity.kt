package net.hilson.qrieux

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private val sharedUri = mutableStateOf<Uri?>(null)
    private val shareTimestamp = mutableLongStateOf(0L)
    private val sharedText = mutableStateOf<String?>(null)
    private val shareTextTimestamp = mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)

        val screenshotContent = intent?.getStringExtra("SCREENSHOT_CONTENT")
        val screenshotBg = intent?.getStringExtra("SCREENSHOT_BACKGROUND")

        setContent {
            App(
                sharedImageUri = sharedUri.value,
                shareTimestamp = shareTimestamp.longValue,
                screenshotContent = screenshotContent,
                screenshotBackground = screenshotBg,
                sharedText = sharedText.value,
                shareTextTimestamp = shareTextTimestamp.longValue
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return

        val mimeType = intent.type ?: ""
        when {
            mimeType.startsWith("image/") -> {
                sharedUri.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                shareTimestamp.longValue = System.currentTimeMillis()
            }
            mimeType == "text/x-vcard" || mimeType == "text/vcard" -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let {
                    sharedText.value = readAndStripVCard(it)
                    shareTextTimestamp.longValue = System.currentTimeMillis()
                }
            }
            mimeType.startsWith("text/") -> {
                sharedText.value = intent.getStringExtra(Intent.EXTRA_TEXT)
                shareTextTimestamp.longValue = System.currentTimeMillis()
            }
        }
    }

    private fun readAndStripVCard(uri: Uri): String? {
        val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
        val allowedPrefixes = listOf(
            "BEGIN:", "END:", "VERSION:", "N:", "N;", "FN:", "FN;",
            "TEL:", "TEL;", "EMAIL:", "EMAIL;", "ORG:", "ORG;",
            "TITLE:", "TITLE;", "ADR:", "ADR;", "URL:", "URL;",
            "BDAY:", "NOTE:"
        )
        val lines = raw.lines()
        val result = mutableListOf<String>()
        var skipContinuation = false
        for (line in lines) {
            if (line.isBlank()) continue
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (!skipContinuation) result.add(line)
                continue
            }
            val upper = line.trim().uppercase()
            val allowed = allowedPrefixes.any { upper.startsWith(it) }
            skipContinuation = !allowed
            if (allowed) result.add(line.trim())
        }
        return result.joinToString("\r\n")
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
