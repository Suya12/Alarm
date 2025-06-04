package com.example.alarm2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
class AlarmService : Service() {

    private var ringtone: Ringtone? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("AlarmService", "onStartCommand 호출됨")

        // 포그라운드 서비스 알림 (필수, Android 8 이상)
        val channelId = "alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("알람 실행 중")
            .setContentText("알람이 울리고 있습니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 임시 아이콘, 아이콘 반드시 필요.
            .build()

        startForeground(1, notification)

        // Ringtone 재생
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        ringtone?.play()

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}