package net.hilson.qrieux.scanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

object QrDecoder {

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
        read(luminance, width, height)
            ?: read(rotate90(luminance, width, height), height, width)

    private fun read(luminance: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return try {
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
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
