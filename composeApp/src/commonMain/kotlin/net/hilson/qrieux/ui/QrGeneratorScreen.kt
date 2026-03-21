package net.hilson.qrieux.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.hilson.qrieux.PlatformContext
import net.hilson.qrieux.dismissPlatformInput
import net.hilson.qrieux.generateQrCode
import net.hilson.qrieux.generator.QrGeneratorFormData
import net.hilson.qrieux.generator.QrGeneratorType
import net.hilson.qrieux.generator.QrGeneratorValidationError
import net.hilson.qrieux.generator.QrWifiSecurity
import net.hilson.qrieux.generator.buildQrPayload
import net.hilson.qrieux.generator.hasInputFor
import net.hilson.qrieux.generator.normalizeWebsiteInput
import net.hilson.qrieux.shareImage
import net.hilson.qrieux.ui.theme.QRieuxUiConfig
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.action_back_to_scan
import qr_scanner.composeapp.generated.resources.generator_description
import qr_scanner.composeapp.generated.resources.generator_field_email
import qr_scanner.composeapp.generated.resources.generator_field_phone
import qr_scanner.composeapp.generated.resources.generator_field_text
import qr_scanner.composeapp.generated.resources.generator_field_website
import qr_scanner.composeapp.generated.resources.generator_field_wifi_hidden
import qr_scanner.composeapp.generated.resources.generator_field_wifi_name
import qr_scanner.composeapp.generated.resources.generator_field_wifi_password
import qr_scanner.composeapp.generated.resources.generator_field_wifi_security
import qr_scanner.composeapp.generated.resources.generator_invalid_email
import qr_scanner.composeapp.generated.resources.generator_invalid_phone
import qr_scanner.composeapp.generated.resources.generator_invalid_text
import qr_scanner.composeapp.generated.resources.generator_invalid_website
import qr_scanner.composeapp.generated.resources.generator_invalid_wifi_name
import qr_scanner.composeapp.generated.resources.generator_invalid_wifi_password
import qr_scanner.composeapp.generated.resources.generator_preview_hint
import qr_scanner.composeapp.generated.resources.generator_share_qr
import qr_scanner.composeapp.generated.resources.generator_title
import qr_scanner.composeapp.generated.resources.generator_type_email
import qr_scanner.composeapp.generated.resources.generator_type_phone
import qr_scanner.composeapp.generated.resources.generator_type_text
import qr_scanner.composeapp.generated.resources.generator_type_website
import qr_scanner.composeapp.generated.resources.generator_type_wifi
import qr_scanner.composeapp.generated.resources.generator_wifi_security_none
import qr_scanner.composeapp.generated.resources.generator_wifi_security_wep
import qr_scanner.composeapp.generated.resources.generator_wifi_security_wpa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(
    platformContext: PlatformContext,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenshotPayload: String? = null
) {
    var selectedType by remember {
        mutableStateOf(if (screenshotPayload != null) QrGeneratorType.Website else QrGeneratorType.Text)
    }
    var form by remember {
        mutableStateOf(
            if (screenshotPayload != null) QrGeneratorFormData(website = screenshotPayload)
            else QrGeneratorFormData()
        )
    }
    var generatedQr by remember { mutableStateOf<net.hilson.qrieux.GeneratedQrCode?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    val payloadResult = remember(selectedType, form) {
        buildQrPayload(selectedType, form)
    }
    val showValidation = remember(selectedType, form) {
        hasInputFor(selectedType, form)
    }
    val shareLabel = stringResource(Res.string.generator_share_qr)

    LaunchedEffect(payloadResult.payload) {
        generatedQr = null
        val payload = payloadResult.payload
        if (payload == null) {
            isGenerating = false
            return@LaunchedEffect
        }

        isGenerating = true
        generatedQr = withContext(Dispatchers.Default) {
            generateQrCode(payload, size = 768)
        }
        isGenerating = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.generator_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back_to_scan)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .dismissKeyboardOnBackgroundTap(platformContext)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.generator_description),
                    style = largeBodyTextStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TypeChooser(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedType) {
                        QrGeneratorType.Website -> WebsiteField(
                            value = form.website,
                            onValueChange = { form = form.copy(website = it) }
                        )

                        QrGeneratorType.Email -> SingleLineField(
                            label = stringResource(Res.string.generator_field_email),
                            value = form.email,
                            onValueChange = { form = form.copy(email = it) },
                            keyboardType = KeyboardType.Email
                        )

                        QrGeneratorType.Phone -> SingleLineField(
                            label = stringResource(Res.string.generator_field_phone),
                            value = form.phone,
                            onValueChange = { form = form.copy(phone = it) },
                            keyboardType = KeyboardType.Phone
                        )

                        QrGeneratorType.Wifi -> WifiForm(
                            form = form,
                            onFormChange = { form = it }
                        )

                        QrGeneratorType.Text -> LargeTextArea(
                            label = stringResource(Res.string.generator_field_text),
                            value = form.text,
                            onValueChange = { form = form.copy(text = it) }
                        )
                    }

                    ValidationMessage(
                        error = payloadResult.error,
                        visible = showValidation
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.White, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val qr = generatedQr
                    when {
                        qr != null -> {
                            Image(
                                bitmap = qr.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        isGenerating -> CircularProgressIndicator()

                        else -> {
                            Text(
                                text = stringResource(Res.string.generator_preview_hint),
                                modifier = Modifier.padding(24.dp),
                                style = largeBodyTextStyle(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Button(
                    onClick = { generatedQr?.let { shareImage(platformContext, it.pngData, shareLabel) } },
                    enabled = generatedQr != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(QRieuxUiConfig.controlHeight),
                    shape = RoundedCornerShape(QRieuxUiConfig.controlCornerRadius)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = shareLabel,
                        fontSize = QRieuxUiConfig.buttonSize
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChooser(
    selectedType: QrGeneratorType,
    onTypeSelected: (QrGeneratorType) -> Unit
) {
    SelectionField(
        options = listOf(
            SelectionOption(QrGeneratorType.Text, stringResource(Res.string.generator_type_text)),
            SelectionOption(QrGeneratorType.Website, stringResource(Res.string.generator_type_website)),
            SelectionOption(QrGeneratorType.Email, stringResource(Res.string.generator_type_email)),
            SelectionOption(QrGeneratorType.Phone, stringResource(Res.string.generator_type_phone)),
            SelectionOption(QrGeneratorType.Wifi, stringResource(Res.string.generator_type_wifi))
        ),
        selectedValue = selectedType,
        onOptionSelected = onTypeSelected
    )
}

@Composable
private fun WifiForm(
    form: QrGeneratorFormData,
    onFormChange: (QrGeneratorFormData) -> Unit
) {
    SingleLineField(
        label = stringResource(Res.string.generator_field_wifi_name),
        value = form.wifiSsid,
        onValueChange = { onFormChange(form.copy(wifiSsid = it)) }
    )
    SingleLineField(
        label = stringResource(Res.string.generator_field_wifi_password),
        value = form.wifiPassword,
        onValueChange = { onFormChange(form.copy(wifiPassword = it)) }
    )
    Text(
        text = stringResource(Res.string.generator_field_wifi_security),
        style = largeBodyTextStyle()
    )
    SelectionField(
        options = listOf(
            SelectionOption(QrWifiSecurity.WpaWpa2, stringResource(Res.string.generator_wifi_security_wpa)),
            SelectionOption(QrWifiSecurity.Wep, stringResource(Res.string.generator_wifi_security_wep)),
            SelectionOption(QrWifiSecurity.None, stringResource(Res.string.generator_wifi_security_none))
        ),
        selectedValue = form.wifiSecurity,
        onOptionSelected = { onFormChange(form.copy(wifiSecurity = it)) }
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = form.wifiHidden,
            onCheckedChange = { onFormChange(form.copy(wifiHidden = it)) }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.generator_field_wifi_hidden),
            style = largeBodyTextStyle()
        )
    }
}

@Composable
private fun LargeTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(QRieuxUiConfig.largeTextAreaHeight),
        label = { Text(text = label, style = supportingTextStyle()) },
        textStyle = largeBodyTextStyle(),
        shape = RoundedCornerShape(QRieuxUiConfig.controlCornerRadius),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun WebsiteField(
    value: String,
    onValueChange: (String) -> Unit
) {
    SingleLineField(
        label = stringResource(Res.string.generator_field_website),
        value = value,
        onValueChange = onValueChange,
        keyboardType = KeyboardType.Uri,
        modifier = Modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                val normalizedValue = normalizeWebsiteInput(value)
                if (normalizedValue != value) {
                    onValueChange(normalizedValue)
                }
            }
        }
    )
}

@Composable
private fun SingleLineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QRieuxUiConfig.controlHeight),
        label = { Text(text = label, style = supportingTextStyle()) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = largeBodyTextStyle(),
        shape = RoundedCornerShape(QRieuxUiConfig.controlCornerRadius),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun <T> SelectionField(
    options: List<SelectionOption<T>>,
    selectedValue: T,
    onOptionSelected: (T) -> Unit
) {
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)

    PlatformSelectionField(
        selectedIndex = selectedIndex,
        optionLabels = options.map { it.label },
        onOptionSelected = { index -> onOptionSelected(options[index].value) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ValidationMessage(
    error: QrGeneratorValidationError?,
    visible: Boolean
) {
    if (!visible || error == null) return

    val message = when (error) {
        QrGeneratorValidationError.InvalidWebsite -> stringResource(Res.string.generator_invalid_website)
        QrGeneratorValidationError.InvalidEmail -> stringResource(Res.string.generator_invalid_email)
        QrGeneratorValidationError.InvalidPhone -> stringResource(Res.string.generator_invalid_phone)
        QrGeneratorValidationError.MissingWifiName -> stringResource(Res.string.generator_invalid_wifi_name)
        QrGeneratorValidationError.MissingWifiPassword -> stringResource(Res.string.generator_invalid_wifi_password)
        QrGeneratorValidationError.BlankText -> stringResource(Res.string.generator_invalid_text)
    }

    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = supportingTextStyle()
    )
}

private data class SelectionOption<T>(
    val value: T,
    val label: String
)

@Composable
private fun largeBodyTextStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(fontSize = QRieuxUiConfig.bodySize)

@Composable
private fun supportingTextStyle(): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(fontSize = QRieuxUiConfig.supportingSize)

private fun Modifier.dismissKeyboardOnBackgroundTap(platformContext: PlatformContext): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    pointerInput(focusManager, keyboardController) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Final)
            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)

            if (up != null && !down.isConsumed && !up.isConsumed) {
                focusManager.clearFocus()
                keyboardController?.hide()
                dismissPlatformInput(platformContext)
            }
        }
    }
}
