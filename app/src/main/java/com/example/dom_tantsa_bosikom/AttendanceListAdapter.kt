package com.example.dom_tantsa_bosikom

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.Attendance

class AttendanceListAdapter(
    private val attendanceList: List<Attendance>
) : BaseAdapter() {

    override fun getCount(): Int = attendanceList.size

    override fun getItem(position: Int): Attendance = attendanceList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val attendance = attendanceList[position]

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        text1.text = attendance.date
        text2.text = "${attendance.childName} - ${attendance.getDisplayName()}"

        if (attendance.isPresent) {
            text1.setTextColor(Color.parseColor("#4CAF50"))
            text2.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            text1.setTextColor(Color.parseColor("#F44336"))
            text2.setTextColor(Color.parseColor("#F44336"))
        }

        return view
    }
}