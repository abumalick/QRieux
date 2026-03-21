package net.hilson.qrieux.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import net.hilson.qrieux.util.QrContentType

class QrGeneratorTest {

    @Test
    fun `website payload round trips as url`() {
        val form = QrGeneratorFormData(website = "https://example.com/path?q=1")

        val result = buildQrPayload(QrGeneratorType.Website, form)

        assertEquals("https://example.com/path?q=1", result.payload)
        assertEquals(QrContentType.Url("https://example.com/path?q=1"), QrContentType.fromRawValue(result.payload!!))
    }

    @Test
    fun `website payload adds https when missing`() {
        val form = QrGeneratorFormData(website = "example.com/path")

        val result = buildQrPayload(QrGeneratorType.Website, form)

        assertEquals("https://example.com/path", result.payload)
        assertEquals(QrContentType.Url("https://example.com/path"), QrContentType.fromRawValue(result.payload!!))
    }

    @Test
    fun `email payload round trips as email`() {
        val form = QrGeneratorFormData(email = "person@example.com")

        val result = buildQrPayload(QrGeneratorType.Email, form)

        assertEquals("mailto:person@example.com", result.payload)
        assertEquals(QrContentType.Email("person@example.com"), QrContentType.fromRawValue(result.payload!!))
    }

    @Test
    fun `phone payload round trips as phone`() {
        val form = QrGeneratorFormData(phone = "+1 555 123 4567")

        val result = buildQrPayload(QrGeneratorType.Phone, form)

        assertEquals("tel:+1 555 123 4567", result.payload)
        assertEquals(QrContentType.Phone("+1 555 123 4567"), QrContentType.fromRawValue(result.payload!!))
    }

    @Test
    fun `wifi payload escapes values and round trips`() {
        val form = QrGeneratorFormData(
            wifiSsid = "Cafe;Guest:5G",
            wifiPassword = "p@ss\\word;123",
            wifiSecurity = QrWifiSecurity.WpaWpa2
        )

        val result = buildQrPayload(QrGeneratorType.Wifi, form)

        assertEquals(
            "WIFI:T:WPA;S:Cafe\\;Guest\\:5G;P:p@ss\\\\word\\;123;;",
            result.payload
        )
        assertEquals(
            QrContentType.Wifi(
                ssid = "Cafe;Guest:5G",
                password = "p@ss\\word;123",
                authType = "WPA",
                hidden = false
            ),
            QrContentType.fromRawValue(result.payload!!)
        )
    }

    @Test
    fun `open hidden wifi payload round trips`() {
        val form = QrGeneratorFormData(
            wifiSsid = "HiddenNet",
            wifiSecurity = QrWifiSecurity.None,
            wifiHidden = true
        )

        val result = buildQrPayload(QrGeneratorType.Wifi, form)

        assertEquals("WIFI:T:nopass;S:HiddenNet;H:true;;", result.payload)
        assertEquals(
            QrContentType.Wifi(
                ssid = "HiddenNet",
                password = "",
                authType = "NOPASS",
                hidden = true
            ),
            QrContentType.fromRawValue(result.payload!!)
        )
    }

    @Test
    fun `text payload preserves unicode content`() {
        val text = "مرحبا QRieux 你好"
        val result = buildQrPayload(QrGeneratorType.Text, QrGeneratorFormData(text = text))

        assertEquals(text, result.payload)
        assertEquals(QrContentType.Text(text), QrContentType.fromRawValue(result.payload!!))
    }

    @Test
    fun `invalid website disables payload`() {
        val result = buildQrPayload(QrGeneratorType.Website, QrGeneratorFormData(website = "   "))

        assertNull(result.payload)
        assertEquals(QrGeneratorValidationError.InvalidWebsite, result.error)
    }

    @Test
    fun `invalid email disables payload`() {
        val result = buildQrPayload(QrGeneratorType.Email, QrGeneratorFormData(email = "bad-email"))

        assertNull(result.payload)
        assertEquals(QrGeneratorValidationError.InvalidEmail, result.error)
    }

    @Test
    fun `invalid phone disables payload`() {
        val result = buildQrPayload(QrGeneratorType.Phone, QrGeneratorFormData(phone = "12345"))

        assertNull(result.payload)
        assertEquals(QrGeneratorValidationError.InvalidPhone, result.error)
    }

    @Test
    fun `blank wifi name disables payload`() {
        val result = buildQrPayload(
            QrGeneratorType.Wifi,
            QrGeneratorFormData(wifiPassword = "secret", wifiSecurity = QrWifiSecurity.WpaWpa2)
        )

        assertNull(result.payload)
        assertEquals(QrGeneratorValidationError.MissingWifiName, result.error)
    }

    @Test
    fun `missing wifi password disables protected network payload`() {
        val result = buildQrPayload(
            QrGeneratorType.Wifi,
            QrGeneratorFormData(wifiSsid = "Office", wifiSecurity = QrWifiSecurity.WpaWpa2)
        )

        assertNull(result.payload)
        assertEquals(QrGeneratorValidationError.MissingWifiPassword, result.error)
    }

    @Test
    fun `blank text disables payload`() {
        val result = buildQrPayload(QrGeneratorType.Text, QrGeneratorFormData(text = "   "))

        assertNull(result.payload)
        assertEquals(QrGeneratorValidationError.BlankText, result.error)
    }

    @Test
    fun `shared url produces valid website payload`() {
        val sharedUrl = "https://example.com/page?q=hello&lang=en"
        val result = buildQrPayload(QrGeneratorType.Website, QrGeneratorFormData(website = sharedUrl))

        assertEquals(sharedUrl, result.payload)
        assertNull(result.error)
    }

    @Test
    fun `shared text with special chars produces valid payload`() {
        val sharedText = "Hello & welcome! Check: https://example.com"
        val result = buildQrPayload(QrGeneratorType.Text, QrGeneratorFormData(text = sharedText))

        assertEquals(sharedText, result.payload)
        assertNull(result.error)
    }

    @Test
    fun `vcard qr is parsed as contact`() {
        val vcard = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:John Doe\r\nTEL:+1234567890\r\nEMAIL:john@example.com\r\nORG:Acme Inc\r\nEND:VCARD"
        val result = QrContentType.fromRawValue(vcard)

        assertIs<QrContentType.Contact>(result)
        val contact = result as QrContentType.Contact
        assertEquals("John Doe", contact.fullName)
        assertEquals("+1234567890", contact.phone)
        assertEquals("john@example.com", contact.email)
        assertEquals("Acme Inc", contact.organization)
        assertEquals(vcard, contact.rawVCard)
    }

    @Test
    fun `vcard with typed fields is parsed correctly`() {
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:Jane Smith\nTEL;TYPE=CELL:+9876543210\nEMAIL;TYPE=WORK:jane@corp.com\nEND:VCARD"
        val result = QrContentType.fromRawValue(vcard)

        assertIs<QrContentType.Contact>(result)
        val contact = result as QrContentType.Contact
        assertEquals("Jane Smith", contact.fullName)
        assertEquals("+9876543210", contact.phone)
        assertEquals("jane@corp.com", contact.email)
    }

    @Test
    fun `vcard with only name is parsed as contact`() {
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:Solo Name\nEND:VCARD"
        val result = QrContentType.fromRawValue(vcard)

        assertIs<QrContentType.Contact>(result)
        assertEquals("Solo Name", (result as QrContentType.Contact).fullName)
    }

    @Test
    fun `empty vcard falls back to text`() {
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nEND:VCARD"
        val result = QrContentType.fromRawValue(vcard)

        assertIs<QrContentType.Text>(result)
    }

    @Test
    fun `vcard content produces valid text payload`() {
        val vcard = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:John Doe\r\nTEL:+1234567890\r\nEMAIL:john@example.com\r\nEND:VCARD"
        val result = buildQrPayload(QrGeneratorType.Text, QrGeneratorFormData(text = vcard))

        assertEquals(vcard, result.payload)
        assertNull(result.error)
    }

    @Test
    fun `website normalizes http url from share`() {
        val result = buildQrPayload(
            QrGeneratorType.Website,
            QrGeneratorFormData(website = "http://example.com")
        )

        assertEquals("http://example.com", result.payload)
        assertNull(result.error)
    }
}
