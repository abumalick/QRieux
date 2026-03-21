package net.hilson.qrieux.generator

enum class QrGeneratorType {
    Website,
    Email,
    Phone,
    Wifi,
    Text
}

enum class QrWifiSecurity(
    val encodedValue: String,
    val requiresPassword: Boolean
) {
    WpaWpa2("WPA", true),
    Wep("WEP", true),
    None("nopass", false)
}

data class QrGeneratorFormData(
    val website: String = "",
    val email: String = "",
    val phone: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: QrWifiSecurity = QrWifiSecurity.WpaWpa2,
    val wifiHidden: Boolean = false,
    val text: String = ""
)

enum class QrGeneratorValidationError {
    InvalidWebsite,
    InvalidEmail,
    InvalidPhone,
    MissingWifiName,
    MissingWifiPassword,
    BlankText
}

data class QrGeneratorBuildResult(
    val payload: String? = null,
    val error: QrGeneratorValidationError? = null
)

private val WEBSITE_PATTERN = Regex("^https?://.+", RegexOption.IGNORE_CASE)
private val EMAIL_PATTERN = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
private val PHONE_PATTERN = Regex("^\\+?[0-9\\s\\-()]{7,}$")

fun buildQrPayload(type: QrGeneratorType, form: QrGeneratorFormData): QrGeneratorBuildResult =
    when (type) {
        QrGeneratorType.Website -> buildWebsitePayload(form.website)
        QrGeneratorType.Email -> buildEmailPayload(form.email)
        QrGeneratorType.Phone -> buildPhonePayload(form.phone)
        QrGeneratorType.Wifi -> buildWifiPayload(form.wifiSsid, form.wifiPassword, form.wifiSecurity, form.wifiHidden)
        QrGeneratorType.Text -> buildTextPayload(form.text)
    }

fun hasInputFor(type: QrGeneratorType, form: QrGeneratorFormData): Boolean =
    when (type) {
        QrGeneratorType.Website -> form.website.isNotEmpty()
        QrGeneratorType.Email -> form.email.isNotEmpty()
        QrGeneratorType.Phone -> form.phone.isNotEmpty()
        QrGeneratorType.Wifi -> form.wifiSsid.isNotEmpty() || form.wifiPassword.isNotEmpty() || form.wifiHidden
        QrGeneratorType.Text -> form.text.isNotEmpty()
    }

fun normalizeWebsiteInput(website: String): String {
    val trimmed = website.trim()
    if (trimmed.isEmpty()) {
        return trimmed
    }

    return when {
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> "https://$trimmed"
    }
}

private fun buildWebsitePayload(website: String): QrGeneratorBuildResult {
    val normalized = normalizeWebsiteInput(website)
    if (!WEBSITE_PATTERN.matches(normalized)) {
        return QrGeneratorBuildResult(error = QrGeneratorValidationError.InvalidWebsite)
    }
    return QrGeneratorBuildResult(payload = normalized)
}

private fun buildEmailPayload(email: String): QrGeneratorBuildResult {
    val trimmed = email.trim()
    if (!EMAIL_PATTERN.matches(trimmed)) {
        return QrGeneratorBuildResult(error = QrGeneratorValidationError.InvalidEmail)
    }
    return QrGeneratorBuildResult(payload = "mailto:$trimmed")
}

private fun buildPhonePayload(phone: String): QrGeneratorBuildResult {
    val trimmed = phone.trim()
    if (!PHONE_PATTERN.matches(trimmed)) {
        return QrGeneratorBuildResult(error = QrGeneratorValidationError.InvalidPhone)
    }
    return QrGeneratorBuildResult(payload = "tel:$trimmed")
}

private fun buildWifiPayload(
    ssid: String,
    password: String,
    security: QrWifiSecurity,
    hidden: Boolean
): QrGeneratorBuildResult {
    if (ssid.trim().isEmpty()) {
        return QrGeneratorBuildResult(error = QrGeneratorValidationError.MissingWifiName)
    }
    if (security.requiresPassword && password.isEmpty()) {
        return QrGeneratorBuildResult(error = QrGeneratorValidationError.MissingWifiPassword)
    }

    val payload = buildString {
        append("WIFI:")
        append("T:${security.encodedValue};")
        append("S:${escapeWifiValue(ssid)};")
        if (security.requiresPassword) {
            append("P:${escapeWifiValue(password)};")
        }
        if (hidden) {
            append("H:true;")
        }
        append(';')
    }
    return QrGeneratorBuildResult(payload = payload)
}

private fun buildTextPayload(text: String): QrGeneratorBuildResult {
    if (text.trim().isEmpty()) {
        return QrGeneratorBuildResult(error = QrGeneratorValidationError.BlankText)
    }
    return QrGeneratorBuildResult(payload = text)
}

private fun escapeWifiValue(value: String): String =
    buildString(value.length) {
        value.forEach { char ->
            if (char == '\\' || char == ';' || char == ',' || char == ':') {
                append('\\')
            }
            append(char)
        }
    }
