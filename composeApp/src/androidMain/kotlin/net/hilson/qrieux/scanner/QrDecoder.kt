package net.hilson.qrieux.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

object QrDecoder {

    // The formats the app advertises on its help screen and asks iOS for. Naming them
    // stops ZXing also building readers for MaxiCode and GS1 DataBar, which the app
    // never offers, and pays for most of the inverted pass below.
    private val SUPPORTED_FORMATS = listOf(
        BarcodeFormat.QR_CODE,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.AZTEC,
        BarcodeFormat.PDF_417,
        BarcodeFormat.EAN_8,
        BarcodeFormat.EAN_13,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR
    )

    // ALSO_INVERTED makes ZXing retry with the black matrix flipped, which reaches the
    // 2D readers only: the 1D readers threshold each row straight from the luminance
    // and never see the flip. On the rotated pass it would repeat the 1D scan, which
    // cannot match, to buy a 2D retry the upright pass has already made — so it goes
    // on the upright pass alone.
    private val UPRIGHT_HINTS = hints(alsoInverted = true)
    private val ROTATED_HINTS = hints(alsoInverted = false)

    private fun hints(alsoInverted: Boolean): Map<DecodeHintType, Any> =
        EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, SUPPORTED_FORMATS)
            if (alsoInverted) put(DecodeHintType.ALSO_INVERTED, true)
        }

    fun decodeLuminancePlane(
        plane: ByteArray,
        rowStride: Int,
        width: Int,
        height: Int
    ): String? = decodeLuminance(compactRows(plane, rowStride, width, height), width, height)

    fun decodeArgbPixels(
        pixels: IntArray,
        width: Int,
        height: Int
    ): String? = decodeLuminance(toLuminance(pixels), width, height)

    // One-dimensional barcodes are only scanned along image rows, so a code that sits
    // upright on screen is sideways in the sensor buffer and needs the second pass.
    private fun decodeLuminance(luminance: ByteArray, width: Int, height: Int): String? =
        read(luminance, width, height, UPRIGHT_HINTS)
            ?: read(rotate90(luminance, width, height), height, width, ROTATED_HINTS)

    private fun read(
        luminance: ByteArray,
        width: Int,
        height: Int,
        hints: Map<DecodeHintType, Any>
    ): String? {
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return try {
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (e: ReaderException) {
            null
        }
    }

    private fun compactRows(plane: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
        if (rowStride == width) return plane
        val compact = ByteArray(width * height)
        for (y in 0 until height) {
            plane.copyInto(compact, y * width, y * rowStride, y * rowStride + width)
        }
        return compact
    }

    private fun toLuminance(pixels: IntArray): ByteArray = ByteArray(pixels.size) { i ->
        val pixel = pixels[i]
        val red = (pixel shr 16) and 0xFF
        val greenTimesTwo = (pixel shr 7) and 0x1FE
        val blue = pixel and 0xFF
        ((red + greenTimesTwo + blue) / 4).toByte()
    }

    private fun rotate90(luminance: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(luminance.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotated[(width - 1 - x) * height + y] = luminance[y * width + x]
            }
        }
        return rotated
    }
}
