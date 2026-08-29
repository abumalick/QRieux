package net.hilson.qrieux.scanner

import android.graphics.Rect
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QrAnalyzerTest {

    @Test
    fun `reports the barcode found in a frame`() {
        val detected = mutableListOf<String>()
        val analyzer = QrAnalyzer { detected.add(it) }

        analyzer.analyze(frameContaining("https://example.com/scanned"))

        assertEquals(listOf("https://example.com/scanned"), detected)
    }

    @Test
    fun `reports nothing while scanning is disabled`() {
        val detected = mutableListOf<String>()
        val analyzer = QrAnalyzer { detected.add(it) }
        analyzer.isEnabled = false

        analyzer.analyze(frameContaining("https://example.com/ignored"))

        assertEquals(emptyList(), detected)
    }

    @Test
    fun `closes a frame it decoded`() {
        val frame = frameContaining("https://example.com/closed")

        QrAnalyzer { }.analyze(frame)

        assertTrue(frame.wasClosed, "camera stalls if analysed frames are not released")
    }

    @Test
    fun `closes a frame it ignored while disabled`() {
        val frame = frameContaining("https://example.com/closed")

        QrAnalyzer { }.apply { isEnabled = false }.analyze(frame)

        assertTrue(frame.wasClosed, "camera stalls if skipped frames are not released")
    }

    @Test
    fun `closes a frame holding no barcode`() {
        val frame = FakeImageProxy(ByteArray(200 * 200) { -1 }, rowStride = 200, width = 200, height = 200)

        QrAnalyzer { }.analyze(frame)

        assertTrue(frame.wasClosed, "camera stalls if empty frames are not released")
    }

    @Test
    fun `reads frames whose rows carry stride padding`() {
        val detected = mutableListOf<String>()
        val matrix = MultiFormatWriter().encode("padded-frame", BarcodeFormat.QR_CODE, 300, 300)
        val frame = FakeImageProxy(
            plane = matrix.toPaddedPlane(padding = 64),
            rowStride = matrix.width + 64,
            width = matrix.width,
            height = matrix.height
        )

        QrAnalyzer { detected.add(it) }.analyze(frame)

        assertEquals(listOf("padded-frame"), detected)
    }
}

private fun frameContaining(content: String): FakeImageProxy {
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300)
    return FakeImageProxy(
        plane = matrix.toPaddedPlane(padding = 0),
        rowStride = matrix.width,
        width = matrix.width,
        height = matrix.height
    )
}

private fun BitMatrix.toPaddedPlane(padding: Int): ByteArray {
    val rowStride = width + padding
    val plane = ByteArray(rowStride * height) { -1 }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (this[x, y]) plane[y * rowStride + x] = 0
        }
    }
    return plane
}

private class FakeImageProxy(
    plane: ByteArray,
    private val rowStride: Int,
    private val width: Int,
    private val height: Int
) : ImageProxy {

    var wasClosed = false
        private set

    private val planes = arrayOf<ImageProxy.PlaneProxy>(FakePlaneProxy(plane, rowStride))

    override fun close() {
        wasClosed = true
    }

    override fun getCropRect(): Rect = Rect(0, 0, width, height)
    override fun setCropRect(rect: Rect?) = Unit
    override fun getFormat(): Int = android.graphics.ImageFormat.YUV_420_888
    override fun getHeight(): Int = height
    override fun getWidth(): Int = width
    override fun getPlanes(): Array<ImageProxy.PlaneProxy> = planes
    override fun getImageInfo(): ImageInfo = throw UnsupportedOperationException()
    override fun getImage(): android.media.Image? = null
}

private class FakePlaneProxy(
    private val data: ByteArray,
    private val rowStride: Int
) : ImageProxy.PlaneProxy {
    override fun getRowStride(): Int = rowStride
    override fun getPixelStride(): Int = 1
    override fun getBuffer(): ByteBuffer = ByteBuffer.wrap(data)
}
