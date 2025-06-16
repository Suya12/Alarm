package com.example.alarm2

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.alarm2.databinding.ActivityAlarmBinding
import com.example.alarm2.databinding.MissionButtonBinding
import com.example.alarm2.databinding.MissionMathBinding
import com.example.alarm2.databinding.MissionObjectBinding
import com.example.alarm2.model.AlarmData

class AlarmActivity : AppCompatActivity() {

//    private var ringtone: Ringtone? = null
    private lateinit var binding: ActivityAlarmBinding
    private lateinit var container: FrameLayout
    private var currentAnswerLabel: String? = null
    private var alarmData: AlarmData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        container = binding.missionContainer

        alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
        currentAnswerLabel = alarmData?.answerLabel
        val action = intent.action
        Log.d("AlarmActivity", "alarmData: $alarmData")

        if (action == "SETUP_CAMERA_MISSION_ACTION") {
            // setup 단계
            val intent = Intent(this, CameraMissionActivity::class.java).apply {
                putExtra("mode", "setup")
            }
            cameraMissionLauncher.launch(intent)
        } else {
            val missionType = alarmData?.missionType
            val retry = intent.getBooleanExtra("retry_camera", false)

            when (missionType) {
                "camera" -> showFindObjectMission(alarmData?.answerLabel)
                "math" -> showMathMission()
                else -> showButtonMission()
            }

            // ✅ 알람 서비스 시작
            val serviceIntent = Intent(this, AlarmService::class.java).apply {
                putExtra("requestCode", alarmData?.requestCode ?: 1)  // 기본값 1로 설정
            }
            ContextCompat.startForegroundService(this, serviceIntent)

        }


    }

    private val cameraMissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val label = data?.getStringExtra("selectedLabel")

            if (label != null) {
                Toast.makeText(this, "선택된 객체: $label", Toast.LENGTH_SHORT).show()
                val returnIntent = Intent().apply {
                    putExtra("missionType", "camera")
                    putExtra("selectedLabel", label)
                }
                setResult(RESULT_OK, returnIntent)
                finish()
            } else {
                showFindObjectMission(currentAnswerLabel)
            }
        } else {
            showFindObjectMission(currentAnswerLabel)
        }
    }

    private fun showButtonMission() {
        val binding = MissionButtonBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        binding.stopAlarmBtn.setOnClickListener {
            // 1) 서비스 종료
            Log.d("AlarmActivity", "stopService 호출")
            val stopIntent = Intent(this, AlarmService::class.java)
            stopService(stopIntent)

            // 2) 진행 중 알림 취소
            cancelAlarmNotification()

            // 3) 메인 화면 이동 및 액티비티 종료
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun showMathMission() {
        val binding = MissionMathBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        val a = (1..99).random()
        val b = (1..99).random()
        val operator = listOf("+", "-").random()

        val (questionText, answer) = when (operator) {
            "+" -> "$a + $b = ?" to a + b
            "-" -> if (a > b) "$a - $b = ?" to a - b else "$b - $a = ?" to b - a
            else -> "오류" to 0
        }

        binding.tvQuestion.text = "문제: $questionText"

        binding.btnSubmitAnswer.setOnClickListener {
            val userInput = binding.etAnswer.text.toString().toIntOrNull()
            if (userInput == answer) {
                Toast.makeText(this, "알람이 해제되었습니다!", Toast.LENGTH_SHORT).show()
                val stopIntent = Intent(this, AlarmService::class.java)
                stopService(stopIntent)

                // 알림 취소
                cancelAlarmNotification()

                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "틀렸습니다. 다시 시도하세요!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFindObjectMission(answerLabel: String?) {
        val binding = MissionObjectBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        binding.tvObjectToFind.text = "🔎 찾아야 할 객체: ${answerLabel ?: "알 수 없음"}"

        binding.btnStartObjectFinding.setOnClickListener {
            val intent = Intent(this, CameraMissionActivity::class.java).apply {
                putExtra("mode", "mission")
                putExtra("answerLabel", answerLabel)
                putExtra("alarmData", alarmData)
            }
            startActivity(intent)
            finish()
        }

        val isRetry = intent.getBooleanExtra("retry_camera", false)
        if (isRetry) {
            binding.btnGiveUp.apply {
                visibility = android.view.View.VISIBLE
                setOnClickListener {
                    Toast.makeText(this@AlarmActivity, "수학 미션으로 전환합니다.", Toast.LENGTH_SHORT).show()
                    showMathMission()
                }
            }
        } else {
            binding.btnGiveUp.visibility = android.view.View.GONE
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        ringtone?.stop()
//    }

    private fun cancelAlarmNotification() {
        alarmData?.let { alarm ->
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(alarm.requestCode)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        // 아무것도 하지 않음 → 뒤로가기 막기
        // 또는 토스트로 경고
        Toast.makeText(this, "미션을 완료해야 알람을 끌 수 있어요!", Toast.LENGTH_SHORT).show()
    }
}
