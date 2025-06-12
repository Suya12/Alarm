package com.example.alarm2.model

import java.io.Serializable

data class AlarmData (
    val hour: Int,
    val minute: Int,
    val requestCode: Int,
    val missionType: String,
    val answerLabel: String? = null, // camera 미션에서만 사용
) : Serializable