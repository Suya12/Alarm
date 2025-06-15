package com.example.alarm2


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alarm2.model.AlarmData
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableListOf<AlarmData>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val filter = IntentFilter("com.example.alarm2.ALARM_FIRED")
        val flags = Context.RECEIVER_NOT_EXPORTED // 내 앱 내부에서만 사용 (외부 앱에 공개하지 않음)
        registerReceiver(alarmFiredReceiver, filter, flags)

        initRecyclerView()
        initAddAlarmButton()

        loadSavedAlarms()
    }

    // 알람 리스트 보여주는 리사이클러 뷰
    private fun initRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.alarmRecyclerView)
        alarmAdapter = AlarmAdapter(alarmList) { alarm -> cancelAlarm(alarm) }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = alarmAdapter
    }

    // 알람 추가 버튼
    private fun initAddAlarmButton() {
        val addAlarmButton: FloatingActionButton = findViewById(R.id.addAlarmButton)
        addAlarmButton.setOnClickListener {
            val intent = Intent(this, AddAlarmActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cancelAlarm(alarmData: AlarmData) {
        // 1. 예약된 알람 취소
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            alarmData.requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // 2. SharedPreferences에서 해당 알람 삭제
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : com.google.gson.reflect.TypeToken<MutableList<AlarmData>>() {}.type
        val alarmListFromPrefs: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // requestCode 기준으로 삭제
        alarmListFromPrefs.removeAll { it.requestCode == alarmData.requestCode }

        // 다시 저장
        val newJson = gson.toJson(alarmListFromPrefs)
        sharedPref.edit().putString("alarms", newJson).apply()

        // 3. 현재 화면의 리스트에서도 제거
        alarmList.removeAll { it.requestCode == alarmData.requestCode }
        alarmAdapter.notifyDataSetChanged()

        android.widget.Toast.makeText(this, "알람이 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
    }

    // ✅ SharedPreferences에서 알람 불러오기
    private fun loadSavedAlarms() {
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : com.google.gson.reflect.TypeToken<MutableList<AlarmData>>() {}.type
        val savedAlarms: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        alarmList.clear()
        alarmList.addAll(savedAlarms)
        alarmAdapter.notifyDataSetChanged()
    }

    private val alarmFiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val requestCode = intent?.getIntExtra("requestCode", -1) ?: return
            alarmList.removeAll { it.requestCode == requestCode }
            alarmAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(alarmFiredReceiver)
    }


    // AddAlarmActivity에서 돌아올 때 최신 데이터 반영
    override fun onResume() {
        super.onResume()
        loadSavedAlarms()
    }

}