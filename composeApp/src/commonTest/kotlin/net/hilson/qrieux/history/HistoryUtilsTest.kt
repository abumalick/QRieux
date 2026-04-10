package net.hilson.qrieux.history

import net.hilson.qrieux.generator.QrGeneratorFormData
import net.hilson.qrieux.generator.QrGeneratorType
import net.hilson.qrieux.generator.QrWifiSecurity
import net.hilson.qrieux.history.percentEncode
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryUtilsTest {

    @Test
    fun reverseParseWebsite() {
        val (type, form) = reverseParseToFormData("https://example.com", "Website")
        assertEquals(QrGeneratorType.Website, type)
        assertEquals("https://example.com", form.website)
    }

    @Test
    fun reverseParseEmail() {
        val (type, form) = reverseParseToFormData("mailto:test@example.com", "Email")
        assertEquals(QrGeneratorType.Email, type)
        assertEquals("test@example.com", form.email)
    }

    @Test
    fun reverseParsePhone() {
        val (type, form) = reverseParseToFormData("tel:+1234567890", "Phone")
        assertEquals(QrGeneratorType.Phone, type)
        assertEquals("+1234567890", form.phone)
    }

    @Test
    fun reverseParseText() {
        val (type, form) = reverseParseToFormData("Hello world", "Text")
        assertEquals(QrGeneratorType.Text, type)
        assertEquals("Hello world", form.text)
    }

    @Test
    fun reverseParseWifi() {
        val (type, form) = reverseParseToFormData("WIFI:T:WPA;S:MyNetwork;P:secret123;;", "Wifi")
        assertEquals(QrGeneratorType.Wifi, type)
        assertEquals("MyNetwork", form.wifiSsid)
        assertEquals("secret123", form.wifiPassword)
        assertEquals(QrWifiSecurity.WpaWpa2, form.wifiSecurity)
    }

    @Test
    fun reverseParseNullType() {
        val (type, form) = reverseParseToFormData("hello", null)
        assertEquals(QrGeneratorType.Text, type)
        assertEquals("hello", form.text)
    }

    @Test
    fun displayLabelUrl() {
        val entry = HistoryEntry("1", 0, HistoryEntryType.SCAN, "https://example.com/path")
        assertEquals("https://example.com/path", displayLabel(entry))
    }

    @Test
    fun percentEncodePreservesAlphanumeric() {
        assertEquals("hello123", percentEncode("hello123"))
    }

    @Test
    fun percentEncodeEncodesSlashAndColon() {
        assertEquals("https%3A%2F%2Fexample.com", percentEncode("https://example.com"))
    }

    @Test
    fun percentEncodeEncodesSpaces() {
        assertEquals("hello%20world", percentEncode("hello world"))
    }

    @Test
    fun percentEncodeWifiPayload() {
        val wifi = "WIFI:T:WPA;S:MyNet;P:pass123;;"
        val encoded = percentEncode(wifi)
        assert(!encoded.contains("%%")) { "Should not double-encode %" }
        assert(encoded.contains("%3A")) { "Colons should be encoded" }
        assert(encoded.contains("%3B")) { "Semicolons should be encoded" }
    }

    @Test
    fun percentEncodeMultiByteUtf8() {
        val encoded = percentEncode("café")
        assertEquals("caf%C3%A9", encoded)
    }

    @Test
    fun percentEncodeChinese() {
        val encoded = percentEncode("你好")
        assert(encoded.startsWith("%")) { "Chinese chars should be percent-encoded: $encoded" }
        assert(!encoded.contains("你")) { "Raw multi-byte chars should not appear" }
    }

    @Test
    fun displayLabelMultiline() {
        val entry = HistoryEntry("1", 0, HistoryEntryType.SCAN, "line1\nline2\nline3")
        assertEquals("line1", displayLabel(entry))
    }
}
