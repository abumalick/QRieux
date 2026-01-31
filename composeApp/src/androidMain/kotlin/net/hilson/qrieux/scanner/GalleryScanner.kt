package net.hilson.qrieux.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val options = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
    .build()

suspend fun scanBarcodeFromUri(context: Context, uri: Uri): String? {
    return suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            BarcodeScanning.getClient(options).process(image)
                .addOnSuccessListener { barcodes ->
                    continuation.resume(barcodes.firstOrNull()?.rawValue)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}
