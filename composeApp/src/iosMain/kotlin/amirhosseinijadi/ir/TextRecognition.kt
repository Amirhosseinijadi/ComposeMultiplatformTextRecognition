package amirhosseinijadi.ir

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
fun recognizeText(image: UIImage?, onTextGenerated: (text: String) -> Unit) {

    val cgImage = image?.CGImage

    val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())

    val request = VNRecognizeTextRequest { request, error ->
        if (error != null) {
            return@VNRecognizeTextRequest
        }

        val observations = request?.results as? List<VNRecognizedTextObservation> ?: emptyList()

        val recognizedText = observations.joinToString(", ") { observation ->
            val topCandidate: VNRecognizedText =
                observation.topCandidates(1u).firstOrNull() as VNRecognizedText
            topCandidate.string
        }

        val queue = dispatch_queue_create("background", null)

        dispatch_async(queue) {
            onTextGenerated(recognizedText)
        }
    }.apply { recognitionLevel = VNRequestTextRecognitionLevelAccurate }

    try {
        handler.performRequests(listOf(request), error = null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}