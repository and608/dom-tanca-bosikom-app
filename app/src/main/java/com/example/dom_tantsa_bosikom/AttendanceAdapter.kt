package com.example.dom_tantsa_bosikom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.Child

class AttendanceAdapter(
    private val attendanceList: MutableList<AttendanceData>,
    private val onAttendanceChanged: (Int, String) -> Unit
) : BaseAdapter() {

    data class AttendanceData(
        val child: Child,
        var status: String,   // present / absent / sick
        val attendanceId: String
    )

    override fun getCount(): Int = attendanceList.size

    override fun getItem(position: Int): AttendanceData = attendanceList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)

        val data = attendanceList[position]

        val textName = view.findViewById<TextView>(R.id.textChildName)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupAttendance)
        val radioPresent = view.findViewById<RadioButton>(R.id.radioPresent)
        val radioAbsent = view.findViewById<RadioButton>(R.id.radioAbsent)
        val radioSick = view.findViewById<RadioButton>(R.id.radioSick)

        textName.text = data.child.name

        radioGroup.setOnCheckedChangeListener(null)

        when (data.status) {
            "present" -> radioPresent.isChecked = true
            "absent" -> radioAbsent.isChecked = true
            "sick" -> radioSick.isChecked = true
            else -> radioAbsent.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newStatus = when (checkedId) {
                R.id.radioPresent -> "present"
                R.id.radioAbsent -> "absent"
                R.id.radioSick -> "sick"
                else -> "absent"
            }
            data.status = newStatus
            onAttendanceChanged(position, newStatus)
        }

        return view
    }

    fun updateData(newList: List<AttendanceData>) {
        attendanceList.clear()
        attendanceList.addAll(newList)
        notifyDataSetChanged()
    }
}