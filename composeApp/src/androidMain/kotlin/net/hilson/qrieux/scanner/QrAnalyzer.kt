package net.hilson.qrieux.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class QrAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    var isEnabled: Boolean = true

    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (!isEnabled) return
            val plane = imageProxy.planes.firstOrNull() ?: return
            val luminance = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
            val value = QrDecoder.decodeLuminancePlane(
                plane = luminance,
                rowStride = plane.rowStride,
                width = imageProxy.width,
                height = imageProxy.height
            )
            if (value != null && isEnabled) onQrCodeDetected(value)
        } finally {
            imageProxy.close()
        }
    }
}
