package net.hilson.qrieux.scanner

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGImageRef
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.UIKit.UIImage
import platform.Vision.*

@OptIn(ExperimentalForeignApi::class)
fun scanBarcodeFromImage(image: UIImage): String? {
    val cgImage: CGImageRef = image.CGImage ?: return null

    var result: String? = null

    val request = VNDetectBarcodesRequest { request, error ->
        if (error != null) {
            println("Vision barcode detection error: ${error.localizedDescription}")
            return@VNDetectBarcodesRequest
        }
        val results = request?.results as? List<VNBarcodeObservation> ?: return@VNDetectBarcodesRequest
        result = results.firstOrNull()?.payloadStringValue
    }

    val handler = VNImageRequestHandler(
        cGImage = cgImage,
        options = emptyMap<Any?, Any?>()
    )

    try {
        handler.performRequests(listOf(request), null)
    } catch (e: Exception) {
        println("Vision barcode scan failed: ${e.message}")
    }

    if (result != null) return result

    // Fallback: CIDetector is CPU-based and works on simulator where
    // Vision's ML inference context may not be available
    val ciImage = CIImage(cGImage = cgImage)
    val detector = CIDetector.detectorOfType(CIDetectorTypeQRCode, context = null, options = null)
    val features = detector?.featuresInImage(ciImage) ?: return null
    return (features.firstOrNull() as? CIQRCodeFeature)?.messageString
}
