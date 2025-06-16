package com.example.alarm2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.alarm2.model.AlarmData
import java.util.Calendar


class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData
        Log.d("AlarmReceiver", "onReceive 호출됨 alarmData: $alarmData")

        if (alarmData == null) {
            Log.d("AlarmReceiver", "AlarmData is null")
            return
        }

        // ✅ 반복 요일이 없을 때만 삭제
        if (alarmData.repeatDays.isNullOrEmpty()) {
            removeAlarmFromPrefs(context, alarmData.requestCode)
        } else {
            // ✅ 반복 요일이 있다면 다음 알람을 예약
            val nextAlarmTime = getNextAlarmTime(alarmData.hour, alarmData.minute, alarmData.repeatDays)
            if (nextAlarmTime != null) {
                scheduleNextAlarm(context, alarmData, nextAlarmTime)
            }
        }

        // 알람 화면 띄우기
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("alarmData", alarmData)
        }
        context.startActivity(activityIntent)

        // 메인화면 갱신용 브로드캐스트
        val broadcastIntent = Intent("com.example.alarm2.ALARM_FIRED").apply {
            putExtra("requestCode", alarmData.requestCode)
        }
        context.sendBroadcast(broadcastIntent)
    }

    // SharedPreferences에서 알람 제거
    private fun removeAlarmFromPrefs(context: Context, requestCode: Int) {
        val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : com.google.gson.reflect.TypeToken<MutableList<AlarmData>>() {}.type
        val alarmList: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        alarmList.removeAll { it.requestCode == requestCode }

        val newJson = gson.toJson(alarmList)
        sharedPref.edit().putString("alarms", newJson).apply()
    }

    // 다음 알람 시간을 계산
    private fun getNextAlarmTime(hour: Int, minute: Int, repeatDays: Set<String>): Calendar? {
        val dayOfWeekMap = mapOf(
            "SUN" to Calendar.SUNDAY,
            "MON" to Calendar.MONDAY,
            "TUE" to Calendar.TUESDAY,
            "WED" to Calendar.WEDNESDAY,
            "THU" to Calendar.THURSDAY,
            "FRI" to Calendar.FRIDAY,
            "SAT" to Calendar.SATURDAY
        )

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
            val dayOfWeek = dayOfWeekMap[dayStr] ?: continue
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

    // 다음 알람 예약
    private fun scheduleNextAlarm(context: Context, alarmData: AlarmData, calendar: Calendar) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$context.packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }
        else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Log.d("AlarmReceiver", "다음 반복 알람 예약됨: ${calendar.time}")
    }
}
