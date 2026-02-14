package net.hilson.qrieux.util

sealed class QrContentType {
    data class Url(val url: String) : QrContentType()
    data class Email(val email: String) : QrContentType()
    data class Phone(val phone: String) : QrContentType()
    data class Wifi(val ssid: String, val password: String, val authType: String, val hidden: Boolean) : QrContentType()
    data class Text(val text: String) : QrContentType()

    companion object {
        private val URL_PATTERN = Regex("^https?://.*", RegexOption.IGNORE_CASE)
        private val EMAIL_PATTERN = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private val PHONE_PATTERN = Regex("^\\+?[0-9\\s\\-()]{7,}$")

        fun fromRawValue(raw: String): QrContentType {
            val trimmed = raw.trim()
            return when {
                trimmed.startsWith("WIFI:", ignoreCase = true) -> parseWifi(trimmed) ?: Text(trimmed)
                URL_PATTERN.matches(trimmed) -> Url(trimmed)
                trimmed.startsWith("mailto:", ignoreCase = true) -> Email(trimmed.substring(7))
                EMAIL_PATTERN.matches(trimmed) -> Email(trimmed)
                trimmed.startsWith("tel:", ignoreCase = true) -> Phone(trimmed.substring(4))
                PHONE_PATTERN.matches(trimmed) -> Phone(trimmed)
                else -> Text(trimmed)
            }
        }

        // Format: WIFI:T:WPA;S:MyNetwork;P:MyPassword;H:true;;
        private fun parseWifi(raw: String): Wifi? {
            val body = raw.substring(5).removeSuffix(";;").removeSuffix(";")
            val fields = splitUnescaped(body, ';')
            var authType = ""
            var ssid = ""
            var password = ""
            var hidden = false
            for (field in fields) {
                val colonIdx = field.indexOf(':')
                if (colonIdx < 0) continue
                val key = field.substring(0, colonIdx)
                val value = unescape(field.substring(colonIdx + 1))
                when (key.uppercase()) {
                    "T" -> authType = value.uppercase()
                    "S" -> ssid = value
                    "P" -> password = value
                    "H" -> hidden = value.equals("true", ignoreCase = true)
                }
            }
            if (ssid.isEmpty()) return null
            return Wifi(ssid, password, authType, hidden)
        }

        private fun splitUnescaped(s: String, delimiter: Char): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            var i = 0
            while (i < s.length) {
                if (s[i] == '\\' && i + 1 < s.length) {
                    current.append(s[i])
                    current.append(s[i + 1])
                    i += 2
                } else if (s[i] == delimiter) {
                    parts.add(current.toString())
                    current.clear()
                    i++
                } else {
                    current.append(s[i])
                    i++
                }
            }
            if (current.isNotEmpty()) parts.add(current.toString())
            return parts
        }

        private fun unescape(s: String): String {
            val sb = StringBuilder()
            var i = 0
            while (i < s.length) {
                if (s[i] == '\\' && i + 1 < s.length) {
                    sb.append(s[i + 1])
                    i += 2
                } else {
                    sb.append(s[i])
                    i++
                }
            }
            return sb.toString()
        }
    }
}
