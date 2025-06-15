package com.example.alarm2

import android.annotation.SuppressLint
import android.app.AlarmManager
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
import java.util.Calendar


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
        Log.d("AlarmReceiver", "onReceive 호출됨 alarmData: $alarmData")

        if (alarmData == null) {
            Log.d("AlarmReceiver", "AlarmData is null")
            return
        }

        // 알람 SharedPreferences에서 삭제
        if (alarmData.repeatDays.isNullOrEmpty()) {
            removeAlarmFromPrefs(context, alarmData.requestCode)
        }

        // 🔔 Notification 띄우기
        showNotification(context, alarmData)

        // 🔄 MainActivity에 알람 울림 브로드캐스트 전송
        val broadcastIntent = Intent("com.example.alarm2.ALARM_FIRED").apply {
            putExtra("requestCode", alarmData.requestCode)
        }
        context.sendBroadcast(broadcastIntent)

        Log.d("AlarmReceiver","브로드캐스트 후")

        // --- 다음 반복 알람 예약 ---
        if (!alarmData.repeatDays.isNullOrEmpty()) {
            val nextAlarmTime = getNextAlarmTime(alarmData.hour, alarmData.minute, alarmData.repeatDays)
            if (nextAlarmTime != null) {
                Log.d("AlarmReceiver", "다음 반복 알람 예약: ${nextAlarmTime.time}")
                scheduleNextAlarm(context, alarmData, nextAlarmTime)
            } else {
                Log.d("AlarmReceiver", "다음 알람 시간을 계산할 수 없습니다.")
            }
        } else {
            Log.d("AlarmReceiver", "반복 요일이 비어있어 반복 알람 예약 안함")
        }
    }

    /**
     * 알람이 울릴 때 Notification을 생성하고 표시합니다.
     * @param context Context
     * @param alarmData 알람 데이터
     */
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

        // ▶ 사용자가 눌렀을 때 AlarmActivity로 이동
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent) // 🔔 클릭 시 AlarmActivity 실행
            .setOngoing(true)
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

    /**
     * 주어진 시간과 반복 요일에 따라 다음 알람 시간을 계산합니다.
     * @param hour 알람 시간 (0-23)
     * @param minute 알람 분 (0-59)
     * @param repeatDays 반복 요일 리스트 (1: 일요일, 2: 월요일, ..., 7: 토요일)
     * @return 다음 알람 시간의 Calendar 객체 또는 null
     */

    // AddAlarmActivity에 있는 dayOfWeekMap과 동일한 맵을 AlarmReceiver에도 정의하거나 전달해야 함
    val dayOfWeekMap = mapOf(
        "SUN" to Calendar.SUNDAY,
        "MON" to Calendar.MONDAY,
        "TUE" to Calendar.TUESDAY,
        "WED" to Calendar.WEDNESDAY,
        "THU" to Calendar.THURSDAY,
        "FRI" to Calendar.FRIDAY,
        "SAT" to Calendar.SATURDAY
    )

    fun getNextAlarmTime(hour: Int, minute: Int, repeatDays: Set<String>): Calendar? {
        if (repeatDays.isEmpty()) return null

        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var minDiff = Long.MAX_VALUE
        var nextAlarm: Calendar? = null

        for (dayStr in repeatDays) {
            val dayOfWeek = dayOfWeekMap[dayStr] ?: continue  // 문자열 → Int 변환

            val candidate = alarmTime.clone() as Calendar
            val currentDay = now.get(Calendar.DAY_OF_WEEK)

            var daysUntil = (dayOfWeek - currentDay + 7) % 7
            if (daysUntil == 0 && candidate.timeInMillis <= now.timeInMillis) {
                daysUntil = 7
            }
            candidate.add(Calendar.DAY_OF_YEAR, daysUntil)

            val diff = candidate.timeInMillis - now.timeInMillis
            if (diff < minDiff) {
                minDiff = diff
                nextAlarm = candidate
            }
        }

        return nextAlarm
    }

    /**
     * 다음 알람을 예약합니다.
     * @param context Context
     * @param alarmData 알람 데이터
     * @param calendar 알람이 울릴 시간의 Calendar 객체
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNextAlarm(context: Context, alarmData: AlarmData, calendar: Calendar) {
        Log.d("AlarmReceiver", "AlarmManager 예약 시간 (ms): ${calendar.timeInMillis}, 날짜: ${calendar.time}")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmData", alarmData)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmData.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}