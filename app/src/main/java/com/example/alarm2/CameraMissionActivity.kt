package com.example.alarm2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alarm2.databinding.ActivityCameraBinding
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class CameraMissionActivity: AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var binding: ActivityCameraBinding
    private lateinit var imageCapture: ImageCapture
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        previewView = binding.previewView
        overlayView = binding.overlayView

        // 카메라 권한 확인
        if( ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // 카메라 권한 요청
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            startCamera()
        }

        // 사진 캡쳐
        binding.btnCapture.setOnClickListener {
            //takePhoto()
            analyzeAssetImage()
        }
    }

    private fun startCamera() {
        // 백그라운드 처리를 위한 미래 객체
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener( {
            // 미래 객체 준비 완료되면 실제 객체 받아옴
            val cameraProvider = cameraProviderFuture.get()

            // 카메라 미리보기 객체
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // 사진 캡쳐를 위한 객체
            imageCapture = ImageCapture.Builder().build()

            // 후면 카메라 설정
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // 이전 바인딩된 카메라가 있다면 해제하고 새로 만든 preview, imageCapture 객체 binding
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "카메라 시작 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            // 콜백 함수
            object : ImageCapture.OnImageCapturedCallback() {
                // 사진 정상적으로 저장됨
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    // TODO: 여기서 YOLO 처리 후, 결과에 따라 알람 종료 여부 판단
                    val analyzer = YoloAnalyzer(this@CameraMissionActivity) { results ->
                        if (results.isEmpty()) {
                            Toast.makeText(this@CameraMissionActivity, "객체를 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            val labels = results.map { it.second }
                            labels.forEach { label ->
                                println("감지된 객체: $label")
                            }

                            Toast.makeText(this@CameraMissionActivity, "감지됨: ${labels.joinToString()}", Toast.LENGTH_SHORT).show()

                            // "apple" in labels
                            if (true) {
                                val stopIntent = Intent(this@CameraMissionActivity, AlarmService::class.java)
                                stopService(stopIntent)

                                val intent = Intent(this@CameraMissionActivity, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }

                    if (bitmap == null) {
                        Log.e("Camera", "Bitmap 변환 실패. image 닫음")
                        return
                    } else {
                        analyzer.analyzeBitmap(bitmap)
                    }

                }
                // 사진 저장 실패
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraMissionActivity, "사진 저장 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val format = image.format
        Log.d("Camera", "Image format: $format, planes: ${image.planes.size}")

        return when {
            // JPEG 처리
            format == ImageFormat.JPEG && image.planes.size == 1 -> {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            // YUV 처리
            format == ImageFormat.YUV_420_888 && image.planes.size >= 3 -> {
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
                val jpegBytes = out.toByteArray()
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            }

            else -> {
                Log.e("Camera", "지원하지 않는 이미지 포맷 또는 plane 부족. format=$format")
                null
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    fun analyzeAssetImage() {
        try {
            val inputStream = assets.open("test.jpg")
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val analyzer = YoloAnalyzer(this) { results ->
                if (results.isEmpty()) {
                    Toast.makeText(this, "객체를 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val labels = results.map { it.second }
                    labels.forEach { label ->
                        Log.d("YOLO", "감지된 객체: $label")
                    }
                    Toast.makeText(this, "감지됨: ${labels.joinToString()}", Toast.LENGTH_SHORT).show()
                }

                // 화면에 박스 그리기 (선택)
                binding.overlayView.results = results
            }

            analyzer.analyzeBitmap(bitmap)

        } catch (e: Exception) {
            Log.e("YOLO", "이미지 분석 실패: ${e.message}")
            Toast.makeText(this, "이미지 분석 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}