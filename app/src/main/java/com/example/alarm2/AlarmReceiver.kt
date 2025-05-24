package com.example.alarm2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm2.model.AlarmData

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val alarmData = intent.getSerializableExtra("alarmData") as? AlarmData

        Log.d("AlarmReceiver", "onReceive 호출됨 alarmData: $alarmData")

        // null이면 종료
        if (alarmData == null) {
            Log.d( "AlarmReceiver","AlarmData is null")
            return
        }

        // 알람 화면 띄우기
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("alarmData", alarmData)
        }
        context.startActivity(activityIntent)

        // mainActivity에 울린 알람 리스트에서 삭제를 위한 브로드캐스트 보내기
        val broadcastIntent = Intent("com.example.alarm2.ALARM_FIRED").apply {
            putExtra("requestCode", alarmData.requestCode)
        }
        context.sendBroadcast(broadcastIntent)
    }

}