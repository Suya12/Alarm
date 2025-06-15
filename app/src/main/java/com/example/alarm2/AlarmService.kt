package com.example.alarm2

import android.app.Notification
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
        val notification = createSilentNotification() // 아래에 정의된 함수
        val requestCode = intent?.getIntExtra("requestCode", 1) ?: 1

        startForeground(requestCode, notification)

        // 벨소리 울리기
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)

        // 혹시 이전에 재생중이면 멈추고 다시 재생하도록
        if (ringtone?.isPlaying == true) {
            ringtone?.stop()
        }
        ringtone?.play()

        return START_STICKY
    }

    private fun createSilentNotification(): Notification {
        val channelId = "alarm_silent_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Silent Alarm Channel",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 대체 아이콘 넣어도 됨
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSilent(true)
            .build()
    }



    override fun onDestroy() {
        Log.d("AlarmService", "onDestroy 호출, 알람 소리 중지")
        super.onDestroy()
        ringtone?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}