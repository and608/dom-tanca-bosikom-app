package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.AttendanceRecord
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.database.*

class ChildAttendanceActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var listViewAttendance: ListView
    private lateinit var adapter: ArrayAdapter<String>

    private val attendanceList = mutableListOf<String>()
    private var childId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_attendance)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)
        listViewAttendance = findViewById(R.id.listViewAttendance)

        textHeaderTitle.text = "Посещаемость ребёнка"
        buttonBack.setOnClickListener { finish() }

        childId = intent.getStringExtra("childId") ?: ""

        if (childId.isBlank()) {
            Toast.makeText(this, "childId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, attendanceList)
        listViewAttendance.adapter = adapter

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        loadChildAndAttendance()
    }

    private fun loadChildAndAttendance() {
        database.child("children").child(childId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val child = snapshot.getValue(Child::class.java)

                    if (child == null) {
                        Toast.makeText(
                            this@ChildAttendanceActivity,
                            "Ребёнок не найден",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return
                    }

                    val realChild = if (child.id.isBlank()) {
                        child.copy(id = snapshot.key ?: "")
                    } else {
                        child
                    }

                    if (realChild.groupId.isBlank()) {
                        attendanceList.clear()
                        attendanceList.add("Ребёнок не состоит в группе")
                        adapter.notifyDataSetChanged()
                        return
                    }

                    loadAttendanceByGroup(realChild.groupId, realChild.id)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChildAttendanceActivity,
                        "Ошибка загрузки ребёнка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadAttendanceByGroup(groupId: String, childId: String) {
        database.child("attendance").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    attendanceList.clear()

                    if (!snapshot.exists()) {
                        attendanceList.add("Нет данных о посещаемости")
                        adapter.notifyDataSetChanged()
                        return
                    }

                    val records = mutableListOf<AttendanceRecord>()

                    for (scheduleSnapshot in snapshot.children) {
                        val record = scheduleSnapshot.child(childId)
                            .getValue(AttendanceRecord::class.java)

                        if (record != null) {
                            records.add(record)
                        }
                    }

                    if (records.isEmpty()) {
                        attendanceList.add("Нет отметок посещаемости")
                    } else {
                        val sortedRecords = records.sortedByDescending { it.markedAt }

                        for (record in sortedRecords) {
                            val statusText = when (record.status) {
                                "present" -> "Присутствовал"
                                "absent" -> "Отсутствовал"
                                "sick" -> "Болел"
                                else -> record.status.ifBlank { "-" }
                            }

                            attendanceList.add(
                                "Занятие: ${record.dateDisplay.ifBlank { "-" }}\n" +
                                        "Статус: $statusText\n" +
                                        "Примечания: ${record.notes.ifBlank { "-" }}"
                            )
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChildAttendanceActivity,
                        "Ошибка загрузки посещаемости: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}