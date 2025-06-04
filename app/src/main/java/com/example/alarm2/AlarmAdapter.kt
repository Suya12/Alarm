package com.example.alarm2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alarm2.model.AlarmData

class AlarmAdapter(
    private val alarmList: List<AlarmData>,
    private val onCancelClick: (AlarmData) -> Unit
    ) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmTimeText: TextView = itemView.findViewById(R.id.alarmTimeText)
        val cancelBtn: Button = itemView.findViewById(R.id.cancelBtn) // 알람 취소 버튼
        val missionTypeTextView: TextView = itemView.findViewById(R.id.missionTypeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarmList[position]
        holder.alarmTimeText.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        holder.missionTypeTextView.text = alarm.missionType

        holder.cancelBtn.setOnClickListener{
            onCancelClick(alarm) // 알람 취소 요청 콜백 호출
        }
    }

    override fun getItemCount(): Int = alarmList.size
}
