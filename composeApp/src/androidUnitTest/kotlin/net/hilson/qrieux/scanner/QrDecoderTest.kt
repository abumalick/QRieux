package net.hilson.qrieux.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QrDecoderTest {

    @Test
    fun `decodes a QR code from a camera luminance plane`() {
        val matrix = encode("https://example.com/hello", BarcodeFormat.QR_CODE, 300, 300)

        val result = QrDecoder.decodeLuminancePlane(
            plane = matrix.toLuminancePlane(rowStride = matrix.width),
            rowStride = matrix.width,
            width = matrix.width,
            height = matrix.height
        )

        assertEquals("https://example.com/hello", result)
    }

    @Test
    fun `decodes a QR code when the luminance plane rows are padded`() {
        val matrix = encode("padded-row-frame", BarcodeFormat.QR_CODE, 300, 300)
        val rowStride = matrix.width + 48

        val result = QrDecoder.decodeLuminancePlane(
            plane = matrix.toLuminancePlane(rowStride = rowStride),
            rowStride = rowStride,
            width = matrix.width,
            height = matrix.height
        )

        assertEquals("padded-row-frame", result)
    }

    @Test
    fun `returns null when the camera frame holds no barcode`() {
        val blank = ByteArray(300 * 300) { WHITE }

        val result = QrDecoder.decodeLuminancePlane(blank, rowStride = 300, width = 300, height = 300)

        assertNull(result)
    }

    @Test
    fun `decodes a QR code from gallery image pixels`() {
        val matrix = encode("gallery@example.com", BarcodeFormat.QR_CODE, 300, 300)

        val result = QrDecoder.decodeArgbPixels(
            pixels = matrix.toArgbPixels(),
            width = matrix.width,
            height = matrix.height
        )

        assertEquals("gallery@example.com", result)
    }

    @Test
    fun `decodes a one-dimensional barcode held sideways`() {
        val matrix = encode("9781234567897", BarcodeFormat.EAN_13, 400, 200).apply { rotate90() }

        val result = QrDecoder.decodeLuminancePlane(
            plane = matrix.toLuminancePlane(rowStride = matrix.width),
            rowStride = matrix.width,
            width = matrix.width,
            height = matrix.height
        )

        assertEquals("9781234567897", result)
    }

    @Test
    fun `returns null when the gallery image holds no barcode`() {
        val blank = IntArray(300 * 300) { 0xFFFFFFFF.toInt() }

        val result = QrDecoder.decodeArgbPixels(blank, width = 300, height = 300)

        assertNull(result)
    }
}

private const val BLACK: Byte = 0
private const val WHITE: Byte = -1 // 0xFF unsigned

private fun encode(content: String, format: BarcodeFormat, width: Int, height: Int): BitMatrix =
    MultiFormatWriter().encode(content, format, width, height)

private fun BitMatrix.toLuminancePlane(rowStride: Int): ByteArray {
    val plane = ByteArray(rowStride * height) { WHITE }
    for (y in 0 until height) {
        for (x in 0 until width) {
            plane[y * rowStride + x] = if (this[x, y]) BLACK else WHITE
        }
    }
    return plane
}

private fun BitMatrix.toArgbPixels(): IntArray {
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (this[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return pixels
}
