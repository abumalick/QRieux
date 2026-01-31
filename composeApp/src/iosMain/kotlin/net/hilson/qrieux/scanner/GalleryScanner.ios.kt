package net.hilson.qrieux.scanner

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGImageRef
import platform.UIKit.UIImage
import platform.Vision.*

@OptIn(ExperimentalForeignApi::class)
fun scanBarcodeFromImage(image: UIImage): String? {
    val cgImage: CGImageRef = image.CGImage ?: return null

    var result: String? = null

    val request = VNDetectBarcodesRequest { request, error ->
        if (error != null) return@VNDetectBarcodesRequest

        val results = request?.results as? List<VNBarcodeObservation> ?: return@VNDetectBarcodesRequest
        result = results.firstOrNull()?.payloadStringValue
    }

    val handler = VNImageRequestHandler(
        cGImage = cgImage,
        options = emptyMap<Any?, Any?>()
    )

    try {
        handler.performRequests(listOf(request), null)
    } catch (_: Exception) {}

    return result
}
