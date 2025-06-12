package com.example.alarm2

import android.content.Intent
import android.media.Ringtone
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

    private var ringtone: Ringtone? = null
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

            if (missionType == "camera") {
                showFindObjectMission(alarmData?.answerLabel)
            } else if (missionType == "math") {
                showMathMission()
            } else {
                showButtonMission()
            }

            // ✅ 서비스는 카메라든 뭐든 항상 실행
            val serviceIntent = Intent(this, AlarmService::class.java)
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
                // 객체 선택 실패 - 다시 Mission 화면으로 돌아감
                showFindObjectMission(currentAnswerLabel)
            }
        } else {
            // 객체 감지 실패 - 다시 Mission 화면으로 돌아감
            showFindObjectMission(currentAnswerLabel)
        }
    }

    private fun showButtonMission() {
        val binding = MissionButtonBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showMathMission() {
        val binding = MissionMathBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        val a = (1..99).random()
        val b = (1..99).random()
        val operators = listOf("+" , "-")
        val operator = operators.random()
        val questionText: String
        val answer: Int
        when(operator) {
            "+" -> {
                questionText = "$a + $b = ?"
                answer = a + b
            }
            "-" -> {
                if(a > b) {
                    questionText = "$a - $b = ?"
                    answer = a - b
                } else {
                    questionText = "$b - $a = ?"
                    answer = b - a
                }
            }
            else -> {
                questionText = "오류"
                answer = 0
            }
        }

        binding.tvQuestion.text = "문제: $questionText"

        binding.btnSubmitAnswer.setOnClickListener {
            val userInput = binding.etAnswer.text.toString().toIntOrNull()
            if (userInput == answer) {
                Toast.makeText(this, "알람이 해제되었습니다!", Toast.LENGTH_SHORT).show()
                val stopIntent = Intent(this, AlarmService::class.java)
                stopService(stopIntent)

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

        // 알람은 이미 울리고 있으므로 서비스는 그대로 둠
        binding.btnStartObjectFinding.setOnClickListener {
            val intent = Intent(this, CameraMissionActivity::class.java).apply {
                putExtra("mode", "mission")
                putExtra("answerLabel", answerLabel)
                putExtra("alarmData", alarmData)
            }
            startActivity(intent)
            finish()
        }

    }



    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }
}
