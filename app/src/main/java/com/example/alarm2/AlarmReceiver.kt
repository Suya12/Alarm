package com.example.alarm2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.alarm2.model.AlarmData

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // 호출 로그
        Log.d("AlarmReceiver", "onReceive 호출됨")


        // 알람 데이터 선언.
        val alarmData: AlarmData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("alarmData", AlarmData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("alarmData") as? AlarmData
        }

        Log.d("AlarmReceiver", "alarmData : $alarmData")
        if (alarmData == null) return

        // 알람 화면 띄우기
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("alarmData", alarmData)
        }
        context.startActivity(activityIntent)

        Log.d("AlarmReceiver", "알람 화면 띄움.")

        // mainActivity에 알람 울림 알림 브로드캐스트 보내기
        val broadcastIntent = Intent("com.example.alarm2.ALARM_FIRED").apply {
            putExtra("alarmData", alarmData)
        }
        context.sendBroadcast(broadcastIntent)

        Log.d("AlarmReceiver", "브로드캐스트 띄움.")
    }

}