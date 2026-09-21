package net.hilson.qrieux.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.hilson.qrieux.AndroidContext
import net.hilson.qrieux.R
import net.hilson.qrieux.openAppSettings
import org.jetbrains.compose.resources.stringResource as sharedStringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.permission_continue

@Composable
fun PermissionScreen(
    showRationale: Boolean,
    onRequestPermission: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val platformContext = AndroidContext(context)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
            // Stacked, the explanation and the two 72dp buttons are taller than a
            // landscape phone minus the tab bar, and Open Settings — the way back
            // for someone who denied the permission for good — is what falls off
            // the end. Beside the text the buttons get the whole height.
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        PermissionText()
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PermissionActions(onRequestPermission, platformContext)
                }
            }
        } else {
            // Centered while it fits, scrolling once it does not.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp)
            ) {
                PermissionText()
                Spacer(modifier = Modifier.height(48.dp))
                PermissionActions(onRequestPermission, platformContext)
            }
        }
    }
}

@Composable
private fun PermissionText() {
    Text(
        text = stringResource(R.string.permission_camera_required_title),
        color = Color.White,
        fontSize = 28.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.permission_camera_required_desc),
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 20.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PermissionActions(
    onRequestPermission: () -> Unit,
    platformContext: AndroidContext
) {
    Button(
        onClick = onRequestPermission,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = sharedStringResource(Res.string.permission_continue),
            fontSize = 22.sp
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = { openAppSettings(platformContext) },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        )
    ) {
        Text(
            text = stringResource(R.string.permission_open_settings),
            fontSize = 22.sp
        )
    }
}
