package com.example.alarm2

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alarm2.model.AlarmData
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var setAlarmBtn: Button
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var missionSpinner: Spinner

    private val missionTypes = listOf("math", "camera", "button") // 🔸 미션 목록
    private val alarmList = mutableListOf<AlarmData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initRecyclerView()

        // 알람 울림 수신 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                alarmFiredReceiver,
                IntentFilter("com.example.alarm2.ALARM_FIRED"),
                Context.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") // 여기는 suppress 해도 괜찮음
            registerReceiver(
                alarmFiredReceiver,
                IntentFilter("com.example.alarm2.ALARM_FIRED")
            )
        }

    }

    private fun initViews() {
        timePicker = findViewById(R.id.timePicker)
        setAlarmBtn = findViewById(R.id.setAlarmBtn)
        missionSpinner = findViewById(R.id.missionSpinner)

        // 🔸 스피너에 어댑터 설정
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, missionTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        missionSpinner.adapter = adapter

        setAlarmBtn.setOnClickListener {
            if (!hasAlarmPermission()) {
                setAlarm()
            }
        }
    }

    private fun initRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.alarmRecyclerView)
        alarmAdapter = AlarmAdapter(alarmList) { alarm -> cancelAlarm(alarm) }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = alarmAdapter
    }

    private fun hasAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return true
            }
        }
        return false
    }

    private fun setAlarm() {
        val hour = timePicker.hour
        val minute = timePicker.minute
        val requestCode = alarmList.size + 1
        val selectedMission = missionSpinner.selectedItem.toString() // 🔸 선택된 미션 타입

        // 알람 데이터 객체를 생성해서 알람 리스트에 추가.
        val newAlarm = AlarmData(hour, minute, requestCode, selectedMission)
        alarmList.add(newAlarm)
        alarmAdapter.notifyItemInserted(alarmList.size - 1)

        // 알람 시각을 설정
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarmData", newAlarm)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(alarmData: AlarmData) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, alarmData.requestCode, intent, PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val index = alarmList.indexOf(alarmData)
        if (index != -1) {
            alarmList.removeAt(index)
            alarmAdapter.notifyItemRemoved(index)
        }

        Toast.makeText(this, "알람 취소: ${alarmData.hour}시 ${alarmData.minute}분", Toast.LENGTH_SHORT).show()
    }
    private val alarmFiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val requestCode = intent?.getIntExtra("requestCode", -1) ?: return

            val index = alarmList.indexOfFirst { it.requestCode == requestCode }
            if (index != -1) {
                alarmList.removeAt(index)
                alarmAdapter.notifyItemRemoved(index)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(alarmFiredReceiver)
    }


}
