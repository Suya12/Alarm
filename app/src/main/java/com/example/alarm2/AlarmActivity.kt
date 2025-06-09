package com.example.alarm2

import android.content.Intent
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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

        container = binding.missionContainer

        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
        val missionType = alarmData?.missionType

        when(missionType) {
            "math" -> showMathMission()
            "camera" -> {
                val intent = Intent(this, CameraMissionActivity::class.java)
                startActivity(intent)
            }
            else -> showButtonMission()
        }

        // 🔇 AlarmService는 이미 Receiver에서 실행되므로 생략
        // val serviceIntent = Intent(this, AlarmService::class.java)
        // ContextCompat.startForegroundService(this, serviceIntent)
    }


    private fun showButtonMission() {
        val binding = MissionButtonBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        binding.stopAlarmBtn.setOnClickListener {
            val stopIntent = Intent(this, AlarmService::class.java)
            stopService(stopIntent)

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
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