package com.example.dom_tantsa_bosikom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ProgressBar
import android.widget.TextView

class ChildStatisticsAdapter(
    private val statistics: List<ChildStatistics>
) : BaseAdapter() {

    override fun getCount(): Int = statistics.size

    override fun getItem(position: Int): ChildStatistics = statistics[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_child_statistics, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val stat = statistics[position]

        // Имя ребёнка
        holder.textViewChildName.text = stat.childName

        // Детали
        holder.textViewAttendanceDetails.text = "Посещено: ${stat.attendedClasses} из ${stat.totalClasses}"

        // Процент
        holder.textViewPercentage.text = "${stat.percentage}%"

        // Цвет процента
        val color = when {
            stat.percentage >= 90 -> 0xFF4CAF50.toInt() // Зелёный
            stat.percentage >= 70 -> 0xFFFFC107.toInt() // Жёлтый
            else -> 0xFFF44336.toInt() // Красный
        }
        holder.textViewPercentage.setTextColor(color)

        // Прогресс-бар
        holder.progressBarChild.progress = stat.percentage

        return view
    }

    private class ViewHolder(view: View) {
        val textViewChildName: TextView = view.findViewById(R.id.textViewChildName)
        val textViewAttendanceDetails: TextView = view.findViewById(R.id.textViewAttendanceDetails)
        val textViewPercentage: TextView = view.findViewById(R.id.textViewPercentage)
        val progressBarChild: ProgressBar = view.findViewById(R.id.progressBarChild)
    }
}