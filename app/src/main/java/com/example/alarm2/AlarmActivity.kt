package com.example.alarm2

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AlarmActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val stopAlarmBtn = findViewById<Button>(R.id.stopAlarmBtn)

        // Ringtone 재생
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        ringtone?.play()

        // 버튼 누르면 벨소리 중지 + 화면 닫기
        stopAlarmBtn.setOnClickListener {
            ringtone?.stop()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }
}