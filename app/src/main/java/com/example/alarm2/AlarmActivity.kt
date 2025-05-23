package com.example.alarm2

import android.content.Intent
import android.media.Ringtone
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.alarm2.databinding.ActivityAlarmBinding
import com.example.alarm2.databinding.MissionButtonBinding
import com.example.alarm2.databinding.MissionMathBinding
import com.example.alarm2.model.AlarmData

class AlarmActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private lateinit var binding: ActivityAlarmBinding
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 컨테이너 연결
        container = binding.missionContainer

        // 알람 데이터 가져오기
        val alarmData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("alarmData", AlarmData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("alarmData") as? AlarmData
        }

        Log.d("AlarmActivity", "missionType: ${alarmData?.missionType}")

        // 미션 함수 호출
        val missionType = intent.getStringExtra("MISSION_TYPE")
        when(missionType) {
            "math" -> showMathMission()
            "camera" -> {
                val intent = Intent(this, CameraMissionActivity::class.java)
                startActivity(intent)
            }
            else -> showButtonMission()
        }

    }

    private fun showButtonMission() {
        val binding = MissionButtonBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        // 버튼 누르면 벨소리 중지 + 화면 닫기
        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)

        // MainActivity로 전환
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showMathMission() {
        // mission math xml 연결
        val binding = MissionMathBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        // 문제 만들기
        val a = (1..99).random()
        val b = (1..99).random()
        val operators = listOf("+" , "-")
        val operator = operators.random()
        val questionText: String
        val answer: Int
        when(operator) {
            "+" -> {questionText = "$a + $b = ?"
                answer = a + b }
            "-" -> {
                if(a>b) {
                    questionText = "$a - $b = ?"
                    answer = a - b
                }
                else {
                    questionText = "$b - $a = ?"
                    answer = b - a
                }
            }
            else -> {questionText = "오류"
                answer = 0 }
        }

        // 만든 문제 띄우기
        binding.tvQuestion.text = "문제: $questionText"

        binding.btnSubmitAnswer.setOnClickListener {
            val userInput = binding.etAnswer.text.toString().toIntOrNull()
            if( userInput == answer) {
                Toast.makeText(this, "정답입니다!", Toast.LENGTH_SHORT).show()
                // 알람 종료
                val stopIntent = Intent(this, AlarmService::class.java)
                stopService(stopIntent)

                // MainActivity로 전환
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "틀렸습니다. 다시 시도하세요!", Toast.LENGTH_SHORT).show()
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }
}