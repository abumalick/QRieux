package net.hilson.qrieux.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    var sampleSize = 1
    while (maxOf(width, height) / sampleSize > maxEdge) {
        sampleSize *= 2
    }
    return sampleSize
}

fun decodeBarcodeFromBitmap(bitmap: Bitmap): String? {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return QrDecoder.decodeArgbPixels(pixels, bitmap.width, bitmap.height)
}

// Decoding allocates several full-size pixel buffers, so a phone-camera photo is
// downsampled first; a QR code stays readable well below full sensor resolution.
private const val MAX_GALLERY_EDGE = 2048

suspend fun scanBarcodeFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_GALLERY_EDGE)
        }
        context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?.let { decodeBarcodeFromBitmap(it) }
    } catch (e: Exception) {
        null
    }
}
