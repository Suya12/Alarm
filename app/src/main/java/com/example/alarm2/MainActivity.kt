package com.example.alarm2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var setAlarmBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker = findViewById(R.id.timePicker)
        setAlarmBtn = findViewById(R.id.setAlarmBtn)

        setAlarmBtn.setOnClickListener{
            // 권한 설정.
            if (checkAlarmPermission()) return@setOnClickListener
            // 알람 실행.
            val calendarForAlarm = Calendar.getInstance() // 알람 설정 시점의 시간
            setAlarm()
        }
    }

    private fun checkAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

            if (!alarmManager.canScheduleExactAlarms()) {
                // 권한이 없으면 설정 페이지로 이동
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return true
            }
        }
        return false
    }

    private fun setAlarm() {
        val calendar = Calendar.getInstance()

        // 타임피커에서 선택한 시간과 분을 캘린더에 설정
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        calendar.set(Calendar.MINUTE, timePicker.minute)
        calendar.set(Calendar.SECOND, 0) // 초는 0초로 맞춤

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0 , intent, PendingIntent.FLAG_IMMUTABLE )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        // 알람 설정 확인 메세지.
        Toast.makeText(
            this,
            "알람이 ${timePicker.hour}시 ${timePicker.minute}분에 설정되었습니다.",
            Toast.LENGTH_SHORT
        ).show()
    }
}