package com.example.alarm2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.alarm2.AlarmActivity
import com.example.alarm2.model.AlarmData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
        Log.d("AlarmReceiver", "onReceive 호출됨 alarmData: $alarmData")

        if (alarmData == null) {
            Log.d("AlarmReceiver", "AlarmData is null")
            return
        }

        // 알람 SharedPreferences에서 삭제
        removeAlarmFromPrefs(context, alarmData.requestCode)

        Log.d("AlarmReceiver","Notification 전")

        // 🔔 Notification 띄우기
        showNotification(context, alarmData)

        Log.d("AlarmReceiver","Notification 후")

        // 🔄 MainActivity에 알람 울림 브로드캐스트 전송
        val broadcastIntent = Intent("com.example.alarm2.ALARM_FIRED").apply {
            putExtra("requestCode", alarmData.requestCode)
        }
        context.sendBroadcast(broadcastIntent)

        Log.d("AlarmReceiver","브로드캐스트 후")
    }

    private fun showNotification(context: Context, alarmData: AlarmData) {
        val channelId = "alarm_channel"
        val channelName = "Alarm Notifications"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm channel"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ▶ 사용자가 눌렀을 때만 AlarmActivity로 이동
        val contentIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("alarmData", alarmData)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmData.requestCode,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ 알람이 울립니다")
            .setContentText("알람 시간입니다! 터치하여 해제하세요.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent) // 🔔 클릭 시 AlarmActivity 실행
            .build()

        notificationManager.notify(alarmData.requestCode, notification)

        // ▶ 알람 소리 재생을 위한 AlarmService 실행
        val serviceIntent = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }


    private fun removeAlarmFromPrefs(context: Context, requestCode: Int) {
        val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : TypeToken<MutableList<AlarmData>>() {}.type
        val alarmList: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        alarmList.removeAll { it.requestCode == requestCode }
        val newJson = gson.toJson(alarmList)
        sharedPref.edit().putString("alarms", newJson).apply()
    }
}