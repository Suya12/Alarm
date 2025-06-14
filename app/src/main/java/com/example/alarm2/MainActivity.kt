package com.example.alarm2

import android.app.AlarmManager
import android.app.PendingIntent
import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alarm2.databinding.ActivityMainBinding
import com.example.alarm2.model.AlarmData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var alarmAdapter: AlarmAdapter

    private var uniqueRequestCode = 0
    private val alarmList = mutableListOf<AlarmData>()
    private val missionTypes = listOf("math", "camera", "button")
    private var selectedCameraLabel: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initRecyclerView()
        registerAlarmFiredReceiver()
        initAddAlarmButton()
        loadSavedAlarms()
    }

    private fun initViews() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, missionTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.missionSpinner.adapter = adapter

        binding.setAlarmBtn.setOnClickListener {
            if (hasAlarmPermission()) return@setOnClickListener

            val missionType = binding.missionSpinner.selectedItem.toString()
            if (missionType == "camera") {
                val setupIntent = Intent(this, AlarmActivity::class.java).apply {
                    action = "SETUP_CAMERA_MISSION_ACTION"
                }
                cameraMissionSetupLauncher.launch(setupIntent)
            } else {
                setAlarm(missionType, answerLabel = "")
            }
        }
    }

    private fun initRecyclerView() {
        alarmAdapter = AlarmAdapter(alarmList) { alarm -> cancelAlarm(alarm) }
        binding.alarmRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.alarmRecyclerView.adapter = alarmAdapter
    }

    private fun registerAlarmFiredReceiver() {
        val filter = IntentFilter("com.example.alarm2.ALARM_FIRED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmFiredReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(alarmFiredReceiver, filter)
        }
    }

    private fun initAddAlarmButton() {
        val addAlarmButton: FloatingActionButton = findViewById(R.id.addAlarmButton)
        addAlarmButton.setOnClickListener {
            val intent = Intent(this, AddAlarmActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setAlarm(missionType: String, answerLabel: String) {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val requestCode = ++uniqueRequestCode

        val alarmData = AlarmData(hour, minute, requestCode, missionType, answerLabel)
        alarmList.add(alarmData)
        alarmAdapter.notifyItemInserted(alarmList.size - 1)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarmData", alarmData)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        Toast.makeText(this, "알람 설정 완료: $hour:$minute ($missionType)", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm(alarmData: AlarmData) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmData.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : TypeToken<MutableList<AlarmData>>() {}.type
        val alarmListFromPrefs: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        alarmListFromPrefs.removeAll { it.requestCode == alarmData.requestCode }

        val newJson = gson.toJson(alarmListFromPrefs)
        sharedPref.edit().putString("alarms", newJson).apply()

        alarmList.removeAll { it.requestCode == alarmData.requestCode }
        alarmAdapter.notifyDataSetChanged()

        Toast.makeText(this, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedAlarms() {
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : TypeToken<MutableList<AlarmData>>() {}.type
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

    private val cameraMissionSetupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("MainActivity", "resultCode: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val label = result.data?.getStringExtra("selectedLabel")
            Log.d("MainActivity", "CameraMissionActivity로부터 받은 라벨: $label")

            if (label != null) {
                selectedCameraLabel = label
                setAlarm("camera", answerLabel = label)
            } else {
                Toast.makeText(this, "카메라 객체 선택 실패", Toast.LENGTH_SHORT).show()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(alarmFiredReceiver)
    }

    override fun onResume() {
        super.onResume()
        loadSavedAlarms()
    }
}
