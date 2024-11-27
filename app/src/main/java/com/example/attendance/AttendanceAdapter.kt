package com.example.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(private val attendanceList: List<AttendanceRecord>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewEnrollment: TextView = itemView.findViewById(R.id.textViewEnrollment)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val checkBoxStatus: CheckBox = itemView.findViewById(R.id.checkBoxStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = attendanceList[position]
        holder.textViewEnrollment.text = record.enrollmentNo
        holder.textViewName.text = record.name
        holder.checkBoxStatus.isChecked = record.status

        holder.checkBoxStatus.setOnCheckedChangeListener { _, isChecked ->
            record.status = isChecked
        }
    }

    override fun getItemCount(): Int = attendanceList.size

    fun getUpdatedAttendance(): List<AttendanceRecord> = attendanceList
}