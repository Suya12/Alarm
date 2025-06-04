package com.example.alarm2

import android.content.Intent
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.util.Log
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
        Log.d("AlarmActivity", "onCreate 호출됨")

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        Log.d("AlarmActivity", "기본 Window flags 설정 완료")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            Log.d("AlarmActivity", "setShowWhenLocked & setTurnScreenOn 호출됨 (>= Oreo MR1)")
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            Log.d("AlarmActivity", "Window flags 설정됨 (< Oreo MR1)")
        }

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("AlarmActivity", "setContentView 설정됨")

        container = binding.missionContainer

        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
        Log.d("AlarmActivity", "받은 alarmData: $alarmData")

        val missionType = alarmData?.missionType
        Log.d("AlarmActivity", "미션 타입: $missionType")

        when(missionType) {
            "math" -> {
                Log.d("AlarmActivity", "Math 미션 실행")
                showMathMission()
            }
            "camera" -> {
                Log.d("AlarmActivity", "Camera 미션 실행")
                val intent = Intent(this, CameraMissionActivity::class.java)
                startActivity(intent)
            }
            else -> {
                Log.d("AlarmActivity", "기본 Button 미션 실행")
                showButtonMission()
            }
        }

        val serviceIntent = Intent(this, AlarmService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Log.d("AlarmActivity", "AlarmService 실행됨")
    }

    private fun showButtonMission() {
        Log.d("AlarmActivity", "showButtonMission 호출됨")
        val binding = MissionButtonBinding.inflate(layoutInflater)
        container.removeAllViews()
        container.addView(binding.root)

        val stopIntent = Intent(this, AlarmService::class.java)
        stopService(stopIntent)
        Log.d("AlarmActivity", "AlarmService 중지됨")

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Log.d("AlarmActivity", "MainActivity로 이동")

        finish()
    }

    private fun showMathMission() {
        Log.d("AlarmActivity", "showMathMission 호출됨")
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

        Log.d("AlarmActivity", "출제된 문제: $questionText, 정답: $answer")

        binding.tvQuestion.text = "문제: $questionText"

        binding.btnSubmitAnswer.setOnClickListener {
            val userInput = binding.etAnswer.text.toString().toIntOrNull()
            Log.d("AlarmActivity", "사용자 입력: $userInput")

            if(userInput == answer) {
                Toast.makeText(this, "정답입니다!", Toast.LENGTH_SHORT).show()
                val stopIntent = Intent(this, AlarmService::class.java)
                stopService(stopIntent)
                Log.d("AlarmActivity", "정답 → AlarmService 중지됨")

                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.d("AlarmActivity", "MainActivity로 이동")

                finish()
            } else {
                Toast.makeText(this, "틀렸습니다. 다시 시도하세요!", Toast.LENGTH_SHORT).show()
                Log.d("AlarmActivity", "틀린 정답 입력됨")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmActivity", "onDestroy 호출됨")
        ringtone?.stop()
    }
}
