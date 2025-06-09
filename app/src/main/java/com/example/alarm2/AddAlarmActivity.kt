package com.example.alarm2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alarm2.AlarmReceiver
import com.example.alarm2.R
import com.example.alarm2.model.AlarmData
import java.util.Calendar
import android.util.Log
import androidx.core.app.ActivityCompat

class AddAlarmActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var missionSpinner: Spinner
    private lateinit var confirmBtn: Button

    private val missionTypes = listOf("button", "shaking", "math", "camera")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_alarm)

        timePicker = findViewById(R.id.timePicker)
        missionSpinner = findViewById(R.id.missionSpinner)
        confirmBtn = findViewById(R.id.confirmBtn)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, missionTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        missionSpinner.adapter = adapter

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        confirmBtn.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val missionType = missionSpinner.selectedItem.toString()

            val requestCode = System.currentTimeMillis().toInt()
            val newAlarm = AlarmData(hour, minute, requestCode, missionType)

            saveAlarm(newAlarm)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager.canScheduleExactAlarms()) {
                    setAlarm(newAlarm)
                } else {
                    Toast.makeText(this, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }
            } else {
                setAlarm(newAlarm)
            }

            val resultIntent = Intent().apply {
                putExtra("hour", hour)
                putExtra("minute", minute)
                putExtra("missionType", missionType)
                putExtra("requestCode", requestCode)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun saveAlarm(alarm: AlarmData) {
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : com.google.gson.reflect.TypeToken<MutableList<AlarmData>>() {}.type
        val alarmList: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        alarmList.add(alarm)
        val newJson = gson.toJson(alarmList)
        sharedPref.edit().putString("alarms", newJson).apply()

        Toast.makeText(this, "알람이 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun setAlarm(alarm: AlarmData) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarmData", alarm) // ✅ 반드시 이걸 넣어야 AlarmReceiver에서 alarmData 받음
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // 정확한 알람 요청
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
        // 로그 추가
        Log.d("AlarmManager", "알람 등록됨: requestCode=${alarm.requestCode}, time=${calendar.timeInMillis}")
    }

    // 알람 복원 메서드
    fun restoreAlarms(context: Context) {
        val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : com.google.gson.reflect.TypeToken<MutableList<AlarmData>>() {}.type
        val alarmList: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        for (alarm in alarmList) {
            // setAlarm은 context에 맞게 호출 (Activity/Service 등)
            (context as? AddAlarmActivity)?.setAlarm(alarm)
            // 또는 setAlarm을 static/companion object로 옮겨서 호출
        }
    }


}
