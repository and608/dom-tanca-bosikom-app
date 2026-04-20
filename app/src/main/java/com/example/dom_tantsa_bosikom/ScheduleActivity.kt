package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.example.dom_tantsa_bosikom.models.Schedule


class ScheduleActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var listViewSchedule: ListView
    private lateinit var buttonAddSchedule: Button

    private val scheduleList = mutableListOf<Schedule>()
    private val displayList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private var groupId: String = ""
    private var openAttendanceMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)
        listViewSchedule = findViewById(R.id.listViewSchedule)
        buttonAddSchedule = findViewById(R.id.buttonAddSchedule)

        textHeaderTitle.text = "Расписание"
        buttonBack.setOnClickListener { finish() }

        groupId = intent.getStringExtra("groupId") ?: ""
        openAttendanceMode = intent.getBooleanExtra("openAttendanceMode", false)

        if (groupId.isBlank()) {
            Toast.makeText(this, "groupId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listViewSchedule.adapter = adapter

        if (openAttendanceMode) {
            textHeaderTitle.text = "Выберите занятие"
            buttonAddSchedule.visibility = View.GONE
        }

        buttonAddSchedule.setOnClickListener {
            ScheduleEditDialog(
                context = this,
                existingSchedule = null
            ) { schedule ->
                saveSchedule(schedule)
            }.show()
        }

        listViewSchedule.setOnItemClickListener { _, _, position, _ ->
            val item = scheduleList[position]

            if (openAttendanceMode) {
                val intent = Intent(this, AttendanceActivity::class.java)
                intent.putExtra("groupId", groupId)
                intent.putExtra("scheduleItemId", item.id)
                startActivity(intent)
            } else {
                ScheduleEditDialog(
                    context = this,
                    existingSchedule = item
                ) { updatedSchedule ->
                    saveSchedule(updatedSchedule)
                }.show()
            }
        }

        listViewSchedule.setOnItemLongClickListener { _, _, position, _ ->
            val item = scheduleList[position]
            deleteSchedule(item)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        loadSchedule()
    }

    private fun loadSchedule() {
        database.child("schedules").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scheduleList.clear()
                    displayList.clear()

                    for (child in snapshot.children) {
                        val item = child.getValue(Schedule::class.java)
                        if (item != null) {
                            val fixedItem = item.copy(
                                id = if (item.id.isNotBlank()) item.id else child.key ?: "",
                                groupId = if (item.groupId.isNotBlank()) item.groupId else groupId
                            )
                            scheduleList.add(fixedItem)
                            displayList.add(formatSchedule(fixedItem))
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ScheduleActivity,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun saveSchedule(schedule: Schedule) {
        val key = if (schedule.id.isBlank()) {
            database.child("schedules").child(groupId).push().key ?: return
        } else {
            schedule.id
        }

        val scheduleToSave = schedule.copy(
            id = key,
            groupId = groupId
        )

        database.child("schedules").child(groupId).child(key)
            .setValue(scheduleToSave)
            .addOnSuccessListener {
                Toast.makeText(this, "Занятие сохранено", Toast.LENGTH_SHORT).show()
                loadSchedule()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteSchedule(schedule: Schedule) {
        if (schedule.id.isBlank()) return

        database.child("schedules").child(groupId).child(schedule.id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Занятие удалено", Toast.LENGTH_SHORT).show()
                loadSchedule()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun formatSchedule(schedule: Schedule): String {
        val dayName = when (schedule.dayOfWeek) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            7 -> "Воскресенье"
            else -> "Неизвестно"
        }

        return "$dayName, ${schedule.startTime}-${schedule.endTime}, кабинет ${schedule.room}"
    }
}