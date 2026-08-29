package net.hilson.qrieux.scanner

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class GalleryScannerTest {

    @Test
    fun `reads a QR code out of a picked image`() {
        val bitmap = qrBitmap("https://example.com/from-gallery")

        assertEquals("https://example.com/from-gallery", decodeBarcodeFromBitmap(bitmap))
    }

    @Test
    fun `reads a QR code out of an image with no alpha channel`() {
        val bitmap = qrBitmap("rgb-565-source").copy(Bitmap.Config.RGB_565, false)

        assertEquals("rgb-565-source", decodeBarcodeFromBitmap(bitmap))
    }

    @Test
    fun `returns null for a picture holding no barcode`() {
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFFFFFFFF.toInt())
        }

        assertNull(decodeBarcodeFromBitmap(bitmap))
    }
}

private fun qrBitmap(content: String): Bitmap {
    val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300)
    val pixels = IntArray(matrix.width * matrix.height) { i ->
        val x = i % matrix.width
        val y = i / matrix.width
        if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}

class SampleSizeTest {

    @Test
    fun `keeps full resolution for an image that already fits`() {
        assertEquals(1, sampleSizeFor(width = 1080, height = 1920, maxEdge = 2048))
    }

    @Test
    fun `halves an image that is just over the limit`() {
        assertEquals(2, sampleSizeFor(width = 3000, height = 2000, maxEdge = 2048))
    }

    @Test
    fun `quarters a twelve megapixel photo`() {
        assertEquals(4, sampleSizeFor(width = 4000, height = 3000, maxEdge = 1000))
    }

    @Test
    fun `keeps full resolution when the image size is unknown`() {
        assertEquals(1, sampleSizeFor(width = -1, height = -1, maxEdge = 2048))
    }
}
