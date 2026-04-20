package com.example.dom_tantsa_bosikom

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.example.dom_tantsa_bosikom.models.Schedule

class ScheduleEditDialog(
    private val context: Context,
    private val existingSchedule: Schedule?,
    private val onSave: (Schedule) -> Unit
) {

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_schedule, null)

        val spinnerDayOfWeek = view.findViewById<Spinner>(R.id.spinnerDayOfWeek)
        val editTextStartTime = view.findViewById<EditText>(R.id.editTextStartTime)
        val editTextEndTime = view.findViewById<EditText>(R.id.editTextEndTime)
        val editTextRoom = view.findViewById<EditText>(R.id.editTextRoom)

        val days = arrayOf(
            "Понедельник", "Вторник", "Среда", "Четверг",
            "Пятница", "Суббота", "Воскресенье"
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDayOfWeek.adapter = adapter

        existingSchedule?.let {
            val dayIndex = (it.dayOfWeek - 1).coerceIn(0, 6)
            spinnerDayOfWeek.setSelection(dayIndex)
            editTextStartTime.setText(it.startTime)
            editTextEndTime.setText(it.endTime)
            editTextRoom.setText(it.room)
        }

        AlertDialog.Builder(context)
            .setTitle(if (existingSchedule == null) "Новое занятие" else "Редактирование занятия")
            .setView(view)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val startTime = editTextStartTime.text.toString().trim()
                        val endTime = editTextEndTime.text.toString().trim()
                        val room = editTextRoom.text.toString().trim()

                        if (startTime.isEmpty()) {
                            editTextStartTime.error = "Введите время начала"
                            return@setOnClickListener
                        }

                        if (endTime.isEmpty()) {
                            editTextEndTime.error = "Введите время окончания"
                            return@setOnClickListener
                        }

                        if (room.isEmpty()) {
                            editTextRoom.error = "Введите кабинет"
                            return@setOnClickListener
                        }

                        val schedule = Schedule(
                            id = existingSchedule?.id ?: "",
                            groupId = existingSchedule?.groupId ?: "",
                            dayOfWeek = spinnerDayOfWeek.selectedItemPosition + 1,
                            startTime = startTime,
                            endTime = endTime,
                            room = room,
                            notes = existingSchedule?.notes ?: "",
                            isActive = true
                        )

                        onSave(schedule)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }
}