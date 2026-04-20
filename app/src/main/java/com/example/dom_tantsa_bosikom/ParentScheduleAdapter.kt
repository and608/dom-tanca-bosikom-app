package com.example.dom_tantsa_bosikom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dom_tantsa_bosikom.models.ParentScheduleItem

class ParentScheduleAdapter(
    private val items: List<ParentScheduleItem>
) : RecyclerView.Adapter<ParentScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textChildName: TextView = itemView.findViewById(R.id.textChildName)
        val textGroupName: TextView = itemView.findViewById(R.id.textGroupName)
        val textTeacherName: TextView = itemView.findViewById(R.id.textTeacherName)
        val textSchedule: TextView = itemView.findViewById(R.id.textSchedule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parent_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = items[position]
        holder.textChildName.text = item.childName
        holder.textGroupName.text = "Группа: ${item.groupName}"
        holder.textTeacherName.text = "Преподаватель: ${item.teacherName}"
        holder.textSchedule.text = "Расписание: ${item.schedule}"
    }

    override fun getItemCount(): Int = items.size
}