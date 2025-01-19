package amirhosseinijadi.ir

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureStillImageOutput
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecJPEG
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.position
import platform.CoreGraphics.CGRectMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreVideo.CVImageBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

@Composable
actual fun CameraScreen(
    modifier: Modifier,
    onTextGenerated: (String?) -> Unit
) {
    TextScannerView(
        modifier = modifier,
        onTextRecognized = onTextGenerated
    )
}


class UIViewCapturer(
    private val onTextRecognized: (String) -> Unit
) : UIViewController("UIViewCapturer", null), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    private var frameCounter = 0

    @OptIn(ExperimentalForeignApi::class)
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        if (didOutputSampleBuffer == null || frameCounter++ % 50 != 0) return

        val uiImage = convertSampleBufferToUIImage(didOutputSampleBuffer)

        uiImage?.let { image ->
            recognizeText(image = image) { text ->
                onTextRecognized(text)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun convertSampleBufferToUIImage(sampleBuffer: CMSampleBufferRef?): UIImage? {
    if (sampleBuffer == null) return null

    val imageBuffer: CVImageBufferRef = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return null

    val ciImage = CIImage.imageWithCVPixelBuffer(imageBuffer)
    val ciContext = CIContext()

    val cgImage = ciContext.createCGImage(ciImage, ciImage.extent)

    return UIImage(cgImage)
}


@OptIn(ExperimentalForeignApi::class)
@Composable
fun TextScannerView(
    modifier: Modifier,
    onTextRecognized: (String) -> Unit,
) {
    val device = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo).firstOrNull {
        (it as AVCaptureDevice).position == AVCaptureDevicePositionBack
    } as? AVCaptureDevice

    val input =
        device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) } as AVCaptureDeviceInput

    val output = AVCaptureStillImageOutput().apply {
        outputSettings = mapOf(AVVideoCodecKey to AVVideoCodecJPEG)
    }


    val session = AVCaptureSession()
    device.activeVideoMinFrameDuration = CMTimeMakeWithSeconds(0.0, 0)
    device.activeVideoMaxFrameDuration = CMTimeMakeWithSeconds(0.0, 0)


    session.sessionPreset = AVCaptureSessionPresetPhoto

    session.addInput(input)
    session.addOutput(output)

    val videoOutput = AVCaptureVideoDataOutput()
    videoOutput.videoSettings = mapOf(kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA)
    videoOutput.alwaysDiscardsLateVideoFrames = true

    session.addOutput(videoOutput)

    UIKitViewController(
        onRelease = {
            session.stopRunning()
        },
        factory = {
            setupCamera(
                session = session,
                videoOutput = videoOutput,
                onTextRecognized = onTextRecognized
            )
        },
        modifier = modifier,
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun setupCamera(
    session: AVCaptureSession,
    videoOutput: AVCaptureVideoDataOutput,
    onTextRecognized: (String) -> Unit,
): UIViewCapturer {
    val cameraPreviewLayer = AVCaptureVideoPreviewLayer(session = session)
    val controller = UIViewCapturer(
        onTextRecognized = onTextRecognized,
    )
    val container = UIView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0))
    controller.setView(container)
    container.layer.addSublayer(cameraPreviewLayer)
    dispatch_async(dispatch_get_main_queue()) {
        cameraPreviewLayer.setFrame(container.bounds)
    }
    cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill

    val queue = dispatch_queue_create("background", null)

    dispatch_async(queue) {
        session.startRunning()
    }

    videoOutput.setSampleBufferDelegate(
        controller,
        queue = queue
    )

    dispatch_async(dispatch_get_main_queue()) {
        container.layer.addSublayer(cameraPreviewLayer)
    }

    return controller
}