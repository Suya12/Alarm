package com.example.alarm2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "onReceive 호출됨")
        val requestCode = intent.getIntExtra("requestCode", -1)

        // 알람 화면 띄우기
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(activityIntent)

        // mainActivity에 알람 울림 알림 브로드캐스트 보내기
        val broadcastIntent = Intent("com.example.alarm2.ALARM_FIRED").apply {
            putExtra("requestCode",requestCode)
        }
        context.sendBroadcast(broadcastIntent)
    }

}