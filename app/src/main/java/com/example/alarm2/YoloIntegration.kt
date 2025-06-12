package com.example.alarm2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

typealias DetectionResult = Pair<RectF, String>

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var results: List<DetectionResult> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        textSize = 48f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((rect, label) in results) {
            canvas.drawRect(rect, paint)
            canvas.drawText(label, rect.left, rect.top - 10, paint)
        }
    }
}

class YoloAnalyzer(
    context: Context,
    private val onResults: (List<DetectionResult>) -> Unit
) {
    private val interpreter: Interpreter
    private val inputSize = 640

    private val labels = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
        "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag",
        "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat",
        "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
        "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot",
        "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed", "dining table",
        "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
        "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    )

    init {
        val model = loadModelFile(context, "yolov5s-fp16.tflite")
        interpreter = Interpreter(model)
    }

    fun analyzeBitmap(bitmap: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        val output = Array(1) { Array(25200) { FloatArray(85) } }
        interpreter.run(byteBuffer, output)
        val results = postProcess(output[0], bitmap.width, bitmap.height)
        Log.d("camera", "detected labels: $results")
        onResults(results)
    }

    fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy) ?: return

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        val output = Array(1) { Array(25200) { FloatArray(85) } }
        interpreter.run(byteBuffer, output)

        val results = postProcess(output[0], bitmap.width, bitmap.height)
        onResults(results)
        imageProxy.close()
    }


    companion object {
        fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
            val format = image.format
            Log.d("Camera", "Image format: $format, planes: ${image.planes.size}")

            return if (format == ImageFormat.JPEG && image.planes.size == 1) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else if (format == ImageFormat.YUV_420_888 && image.planes.size >= 3) {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer
                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
                val jpegData = out.toByteArray()
                BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            } else {
                Log.e("Camera", "지원하지 않는 이미지 포멧입니다: $format")
                null
            }
        }
    }

    private fun calculateIoU(a: RectF, b: RectF): Float {
        val intersectionLeft = maxOf(a.left, b.left)
        val intersectionTop = maxOf(a.top, b.top)
        val intersectionRight = minOf(a.right, b.right)
        val intersectionBottom = minOf(a.bottom, b.bottom)

        val intersectionArea = maxOf(0f, intersectionRight - intersectionLeft) *
                maxOf(0f, intersectionBottom - intersectionTop)

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        val unionArea = areaA + areaB - intersectionArea
        return if (unionArea <= 0f) 0f else (intersectionArea / unionArea)
    }

    private fun postProcess(output: Array<FloatArray>, imageWidth: Int, imageHeight: Int): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val threshold = 0.25f
        val iouThreshold = 0.45f

        // 1. 모든 탐지 결과를 confidence 기준으로 필터링
        val boxes = mutableListOf<DetectionResult>()

        for (detection in output) {
            val x = detection[0]
            val y = detection[1]
            val w = detection[2]
            val h = detection[3]
            val objectness = detection[4]

            val classScores = detection.copyOfRange(5, detection.size)
            val maxClassScore = classScores.maxOrNull() ?: 0f
            val classIndex = classScores.toList().indexOf(maxClassScore)
            val confidence = objectness * maxClassScore

            if (confidence > threshold && classIndex in labels.indices) {
                val left = (x - w / 2) * imageWidth
                val top = (y - h / 2) * imageHeight
                val right = (x + w / 2) * imageWidth
                val bottom = (y + h / 2) * imageHeight

                val rect = RectF(left, top, right, bottom)
                val label = labels[classIndex]
                boxes.add(rect to label)
            }
        }

        // 2. NMS 수행
        val selected = mutableListOf<DetectionResult>()
        val used = BooleanArray(boxes.size)

        for (i in boxes.indices) {
            if (used[i]) continue
            val (rectA, labelA) = boxes[i]
            selected.add(boxes[i])
            used[i] = true

            for (j in i + 1 until boxes.size) {
                if (used[j]) continue
                val (rectB, _) = boxes[j]
                val iou = calculateIoU(rectA, rectB)
                if (iou > iouThreshold) {
                    used[j] = true
                }
            }
        }

        return selected
    }


    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
