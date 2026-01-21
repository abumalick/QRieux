package net.hilson.qr_scanner.qr_scanner.util

sealed class QrContentType {
    data class Url(val url: String) : QrContentType()
    data class Email(val email: String) : QrContentType()
    data class Phone(val phone: String) : QrContentType()
    data class Text(val text: String) : QrContentType()

    companion object {
        private val URL_PATTERN = Regex("^https?://.*", RegexOption.IGNORE_CASE)
        private val EMAIL_PATTERN = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        private val PHONE_PATTERN = Regex("^\\+?[0-9\\s\\-()]{7,}$")

        fun fromRawValue(raw: String): QrContentType {
            val trimmed = raw.trim()
            return when {
                URL_PATTERN.matches(trimmed) -> Url(trimmed)
                trimmed.startsWith("mailto:", ignoreCase = true) -> Email(trimmed.removePrefix("mailto:"))
                EMAIL_PATTERN.matches(trimmed) -> Email(trimmed)
                trimmed.startsWith("tel:", ignoreCase = true) -> Phone(trimmed.removePrefix("tel:"))
                PHONE_PATTERN.matches(trimmed) -> Phone(trimmed)
                else -> Text(trimmed)
            }
        }
    }
}
