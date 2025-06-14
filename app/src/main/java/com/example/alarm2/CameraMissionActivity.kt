package com.example.alarm2

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.alarm2.databinding.ActivityCameraBinding
import com.example.alarm2.model.AlarmData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraMissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var preview: Preview
    private lateinit var cameraExecutor: ExecutorService
    private var mode: String = "mission"

    companion object {
        private const val REQUEST_SELECT_LABEL = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = intent.getStringExtra("mode") ?: "setup"
        Log.d("CameraMission", "현재 모드: $mode")
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

            preview.setSurfaceProvider(binding.previewView.surfaceProvider)

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap = YoloAnalyzer.imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        analyzeImage(bitmap)
                    } else {
                        Toast.makeText(this@CameraMissionActivity, "이미지 변환 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraMissionActivity, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun analyzeImage(bitmap: Bitmap) {
        val analyzer = YoloAnalyzer(this@CameraMissionActivity) { results ->
            val labels = results.map { it.second }.distinct()

            if (mode == "setup") {
                if (labels.isEmpty()) {
                    Toast.makeText(this, "감지된 객체가 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    return@YoloAnalyzer  // 화면 유지
                }

                // 카메라 중단
                ProcessCameraProvider.getInstance(this@CameraMissionActivity).get().unbindAll()

                val intent = Intent(this@CameraMissionActivity, LabelSelectActivity::class.java).apply {
                    putStringArrayListExtra("labelList", ArrayList(labels))
                }
                startActivityForResult(intent, REQUEST_SELECT_LABEL)
            }
            else {
                val answer = intent.getStringExtra("answerLabel")
                Log.d("CameraMission", "받은 answerLabel: $answer")
                Log.d("CameraMission", "lables: $labels")

                if (labels.contains(answer)) {
                    stopService(Intent(this@CameraMissionActivity, AlarmService::class.java))
                    val intent = Intent(this@CameraMissionActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

                    Toast.makeText(this@CameraMissionActivity, "알람이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                } else {
                    Toast.makeText(this@CameraMissionActivity, "대상 객체가 감지되지 않았습니다.", Toast.LENGTH_SHORT).show()

                    val retryIntent = Intent(this@CameraMissionActivity, AlarmActivity::class.java).apply {
                        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
                        Log.d("AlarmActivity", "alarmData: $alarmData")
                        putExtra("alarmData", alarmData) // 다시 넘겨줘야 함
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(retryIntent)
                }
                finish()

            }
        }

        analyzer.analyzeBitmap(bitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_LABEL && resultCode == Activity.RESULT_OK) {
            val selectedLabel = data?.getStringExtra("selectedLabel")
            // Log
            Log.d("CameraMission", "선택된 라벨: $selectedLabel")

            val resultIntent = Intent().apply {
                putExtra("selectedLabel", selectedLabel)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
