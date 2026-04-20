package com.example.dom_tantsa_bosikom

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.Schedule

class ScheduleAdapter(
    private val scheduleList: List<Schedule>,
    private val onEditClick: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = scheduleList.size

    override fun getItem(position: Int): Schedule = scheduleList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val schedule = scheduleList[position]

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        text1.text = schedule.getDayName()
        text1.setTextColor(Color.parseColor("#333333"))

        text2.text = "${schedule.getTimeRange()} • Каб. ${schedule.room}"
        text2.setTextColor(Color.parseColor("#666666"))

        view.setOnLongClickListener {
            onEditClick(schedule)
            true
        }

        return view
    }

    fun updateData(newList: List<Schedule>) {
        (scheduleList as MutableList).clear()
        (scheduleList as MutableList).addAll(newList)
        notifyDataSetChanged()
    }
}