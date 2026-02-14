package net.hilson.qr_scanner.qr_scanner

import net.hilson.qrieux.util.QrContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QrContentTypeTest {

    @Test
    fun `wifi WPA2 with password`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA2;S:MyNetwork;P:secret123;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("MyNetwork", result.ssid)
        assertEquals("secret123", result.password)
        assertEquals("WPA2", result.authType)
        assertEquals(false, result.hidden)
    }

    @Test
    fun `wifi WPA with hidden network`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA;S:HiddenNet;P:pass;H:true;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("HiddenNet", result.ssid)
        assertEquals("pass", result.password)
        assertEquals("WPA", result.authType)
        assertEquals(true, result.hidden)
    }

    @Test
    fun `wifi open network no password`() {
        val result = QrContentType.fromRawValue("WIFI:T:nopass;S:FreeWiFi;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("FreeWiFi", result.ssid)
        assertEquals("", result.password)
        assertEquals("NOPASS", result.authType)
    }

    @Test
    fun `wifi WEP`() {
        val result = QrContentType.fromRawValue("WIFI:T:WEP;S:OldRouter;P:wepkey;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("OldRouter", result.ssid)
        assertEquals("wepkey", result.password)
        assertEquals("WEP", result.authType)
    }

    @Test
    fun `wifi escaped semicolon in password`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA;S:Net;P:pass\\;word;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("pass;word", result.password)
    }

    @Test
    fun `wifi escaped colon in SSID`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA;S:My\\:Network;P:pass;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("My:Network", result.ssid)
    }

    @Test
    fun `wifi escaped backslash`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA;S:Net;P:pass\\\\word;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("pass\\word", result.password)
    }

    @Test
    fun `wifi missing SSID falls back to Text`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA;P:password;;")
        assertIs<QrContentType.Text>(result)
    }

    @Test
    fun `wifi case insensitive prefix parses all fields`() {
        val result = QrContentType.fromRawValue("wifi:T:WPA;S:Net;P:pass;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("Net", result.ssid)
        assertEquals("pass", result.password)
        assertEquals("WPA", result.authType)
    }

    @Test
    fun `wifi mixed case prefix`() {
        val result = QrContentType.fromRawValue("Wifi:T:WPA2;S:MyNet;P:secret;;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("WPA2", result.authType)
    }

    @Test
    fun `wifi without trailing double semicolons`() {
        val result = QrContentType.fromRawValue("WIFI:T:WPA;S:Net;P:pass;")
        assertIs<QrContentType.Wifi>(result)
        assertEquals("Net", result.ssid)
        assertEquals("pass", result.password)
    }

    @Test
    fun `url still works`() {
        val result = QrContentType.fromRawValue("https://example.com")
        assertIs<QrContentType.Url>(result)
        assertEquals("https://example.com", result.url)
    }

    @Test
    fun `email still works`() {
        val result = QrContentType.fromRawValue("test@example.com")
        assertIs<QrContentType.Email>(result)
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun `phone still works`() {
        val result = QrContentType.fromRawValue("tel:+1234567890")
        assertIs<QrContentType.Phone>(result)
        assertEquals("+1234567890", result.phone)
    }

    @Test
    fun `plain text still works`() {
        val result = QrContentType.fromRawValue("just some text")
        assertIs<QrContentType.Text>(result)
        assertEquals("just some text", result.text)
    }
}
