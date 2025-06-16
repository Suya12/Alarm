package com.example.alarm2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alarm2.databinding.ActivityLabelSelectBinding

class LabelSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLabelSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)  // ❗️ ViewBinding 적용 시 반드시 필요

        val labelList = intent.getStringArrayListExtra("labelList") ?: arrayListOf()
        Log.d("LabelSelectActivity", "labelList: $labelList")

        binding.spinnerLabels.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labelList
        )

        binding.btnConfirmSelection.setOnClickListener {
            val selected = binding.spinnerLabels.selectedItem as? String
            if (selected.isNullOrBlank()) {
                Toast.makeText(this, "객체를 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent().apply {
                putExtra("selectedLabel", selected)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

    }
}
