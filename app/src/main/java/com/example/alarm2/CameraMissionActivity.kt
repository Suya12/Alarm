package com.example.alarm2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alarm2.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class CameraMissionActivity: AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var imageCapture: ImageCapture
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            takePhoto()
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
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // 사진 파일 저장 경로 지정
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            // 콜백 함수
            object : ImageCapture.OnImageSavedCallback {
                // 사진 정상적으로 저장됨
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@CameraMissionActivity, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()

                    // TODO: 여기서 YOLO 처리 후, 결과에 따라 알람 종료 여부 판단

                    // 알람 종료
                    val stopIntent = Intent(this@CameraMissionActivity, AlarmService::class.java)
                    stopService(stopIntent)

                    val intent = Intent(this@CameraMissionActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                // 사진 저장 실패
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraMissionActivity, "사진 저장 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private val outputDirectory: File
        get() {
            // 외부 저장소 중 앱 전용 폴더 가져오기
            val externalDir = externalMediaDirs.firstOrNull()

            // 외부 저장소 하위에 alarm_photos 폴더 만들기
            val folder = externalDir?.let {
                File(it, "alarm_photos").apply { mkdirs() }
            }

            // 외부 저장소 폴더가 있으면 사용, 아니면 내부 저장소로 대체
            return if (folder != null && folder.exists()) folder else filesDir
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
}