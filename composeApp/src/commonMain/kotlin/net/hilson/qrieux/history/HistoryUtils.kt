package net.hilson.qrieux.history

import net.hilson.qrieux.generator.QrGeneratorFormData
import net.hilson.qrieux.generator.QrGeneratorType
import net.hilson.qrieux.generator.QrWifiSecurity
import net.hilson.qrieux.util.QrContentType

fun reverseParseToFormData(
    rawValue: String,
    generatorType: String?
): Pair<QrGeneratorType, QrGeneratorFormData> {
    val type = generatorType?.let {
        try { QrGeneratorType.valueOf(it) } catch (_: Exception) { null }
    } ?: QrGeneratorType.Text

    val form = when (type) {
        QrGeneratorType.Website -> QrGeneratorFormData(website = rawValue)
        QrGeneratorType.Email -> QrGeneratorFormData(email = rawValue.removePrefix("mailto:"))
        QrGeneratorType.Phone -> QrGeneratorFormData(phone = rawValue.removePrefix("tel:"))
        QrGeneratorType.Wifi -> parseWifiToForm(rawValue)
        QrGeneratorType.Text -> QrGeneratorFormData(text = rawValue)
    }
    return type to form
}

private fun parseWifiToForm(rawValue: String): QrGeneratorFormData {
    val parsed = QrContentType.fromRawValue(rawValue)
    if (parsed is QrContentType.Wifi) {
        return QrGeneratorFormData(
            wifiSsid = parsed.ssid,
            wifiPassword = parsed.password,
            wifiSecurity = when (parsed.authType.uppercase()) {
                "WPA" -> QrWifiSecurity.WpaWpa2
                "WEP" -> QrWifiSecurity.Wep
                else -> QrWifiSecurity.None
            },
            wifiHidden = parsed.hidden
        )
    }
    return QrGeneratorFormData(text = rawValue)
}

fun percentEncode(s: String): String = buildString {
    for (b in s.encodeToByteArray()) {
        val v = b.toInt() and 0xFF
        if (v in 0x30..0x39 || v in 0x41..0x5A || v in 0x61..0x7A || v.toChar() in "-._~") {
            append(v.toChar())
        } else {
            append("%${v.toString(16).uppercase().padStart(2, '0')}")
        }
    }
}

fun displayLabel(entry: HistoryEntry): String {
    val content = QrContentType.fromRawValue(entry.rawValue)
    val value = when (content) {
        is QrContentType.Url -> content.url
        is QrContentType.Email -> content.email
        is QrContentType.Phone -> content.phone
        is QrContentType.Wifi -> content.ssid
        is QrContentType.Contact -> content.fullName.ifEmpty { content.phone.ifEmpty { content.email } }
        is QrContentType.Text -> content.text
    }
    return value.lineSequence().first().take(80)
}
