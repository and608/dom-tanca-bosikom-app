package com.example.dom_tantsa_bosikom

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.AttendanceRecord

class AttendanceHistoryAdapter(
    private val records: List<AttendanceRecord>
) : BaseAdapter() {

    override fun getCount(): Int = records.size

    override fun getItem(position: Int): AttendanceRecord = records[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attendance_history, parent, false)

            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val record = records[position]

        holder.textViewDate.text = record.dateDisplay

        when (record.status) {
            "present" -> {
                holder.textViewStatusIcon.text = "✅"
                holder.textViewStatus.text = "Был"
                holder.textViewStatus.setTextColor(Color.parseColor("#2E7D32"))
            }

            "absent" -> {
                holder.textViewStatusIcon.text = "❌"
                holder.textViewStatus.text = "Не был"
                holder.textViewStatus.setTextColor(Color.parseColor("#C62828"))
            }

            "sick" -> {
                holder.textViewStatusIcon.text = "\uD83E\uDD12"
                holder.textViewStatus.text = "Болел"
                holder.textViewStatus.setTextColor(Color.parseColor("#EF6C00"))
            }

            else -> {
                holder.textViewStatusIcon.text = "—"
                holder.textViewStatus.text = "Не указано"
                holder.textViewStatus.setTextColor(Color.GRAY)
            }
        }

        if (record.notes.isNotEmpty()) {
            holder.textViewNotes.text = "📝 ${record.notes}"
            holder.textViewNotes.visibility = View.VISIBLE
        } else {
            holder.textViewNotes.visibility = View.GONE
        }

        return view
    }

    private class ViewHolder(view: View) {
        val textViewDate: TextView = view.findViewById(R.id.textViewDate)
        val textViewStatusIcon: TextView = view.findViewById(R.id.textViewStatusIcon)
        val textViewStatus: TextView = view.findViewById(R.id.textViewStatus)
        val textViewNotes: TextView = view.findViewById(R.id.textViewNotes)
    }
}