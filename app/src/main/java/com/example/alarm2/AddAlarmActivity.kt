package com.example.alarm2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.alarm2.model.AlarmData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class AddAlarmActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var missionSpinner: Spinner
    private lateinit var confirmBtn: Button

    private lateinit var checkMon: CheckBox
    private lateinit var checkTue: CheckBox
    private lateinit var checkWed: CheckBox
    private lateinit var checkThu: CheckBox
    private lateinit var checkFri: CheckBox
    private lateinit var checkSat: CheckBox
    private lateinit var checkSun: CheckBox

    private val missionTypes = listOf("button", "shaking", "math", "camera")

    private var selectedLabel: String? = null

    private val setupCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            selectedLabel = data?.getStringExtra("selectedLabel")

            if (selectedLabel != null) {
                Toast.makeText(this, "선택된 객체: $selectedLabel", Toast.LENGTH_SHORT).show()
                finalizeAlarmCreation()
            } else {
                Toast.makeText(this, "객체 선택 실패", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "카메라 미션이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_alarm)

        timePicker = findViewById(R.id.timePicker)
        missionSpinner = findViewById(R.id.missionSpinner)
        confirmBtn = findViewById(R.id.confirmBtn)

        checkMon = findViewById(R.id.checkMon)
        checkTue = findViewById(R.id.checkTue)
        checkWed = findViewById(R.id.checkWed)
        checkThu = findViewById(R.id.checkThu)
        checkFri = findViewById(R.id.checkFri)
        checkSat = findViewById(R.id.checkSat)
        checkSun = findViewById(R.id.checkSun)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, missionTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        missionSpinner.adapter = adapter

        // 알림 권한 요청
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
            val missionType = missionSpinner.selectedItem.toString()

            if (missionType == "camera") {
                val intent = Intent(this, CameraMissionActivity::class.java)
                intent.putExtra("mode", "setup")
                setupCameraLauncher.launch(intent)
            } else {
                finalizeAlarmCreation()
            }
        }
    }

    private fun finalizeAlarmCreation() {
        val hour = timePicker.hour
        val minute = timePicker.minute
        val missionType = missionSpinner.selectedItem.toString()
        val requestCode = System.currentTimeMillis().toInt()
        val repeatDays = collectCheckedDays()

        val newAlarm = AlarmData(
            hour = hour,
            minute = minute,
            requestCode = requestCode,
            missionType = missionType,
            answerLabel = selectedLabel,
            repeatDays = repeatDays
        )

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

    private fun collectCheckedDays(): MutableSet<String> {
        val days = mutableSetOf<String>()
        if (checkMon.isChecked) days.add("MON")
        if (checkTue.isChecked) days.add("TUE")
        if (checkWed.isChecked) days.add("WED")
        if (checkThu.isChecked) days.add("THU")
        if (checkFri.isChecked) days.add("FRI")
        if (checkSat.isChecked) days.add("SAT")
        if (checkSun.isChecked) days.add("SUN")
        return days
    }

    private fun saveAlarm(alarm: AlarmData) {
        val sharedPref = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : TypeToken<MutableList<AlarmData>>() {}.type
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
            putExtra("alarmData", alarm)
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

        Log.d("AlarmManager", "알람 등록됨: requestCode=${alarm.requestCode}, time=${calendar.timeInMillis}")
    }

    fun restoreAlarms(context: Context) {
        val sharedPref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("alarms", null)
        val type = object : TypeToken<MutableList<AlarmData>>() {}.type
        val alarmList: MutableList<AlarmData> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        for (alarm in alarmList) {
            (context as? AddAlarmActivity)?.setAlarm(alarm)
        }
    }
}
