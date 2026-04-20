package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.AttendanceRecord
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Schedule
import com.google.firebase.database.*

class AttendanceActivity : AppCompatActivity() {

    private lateinit var listViewChildren: ListView
    private lateinit var database: DatabaseReference
    private lateinit var attendanceAdapter: AttendanceAdapter

    private val childList = mutableListOf<Child>()
    private val attendanceList = mutableListOf<AttendanceAdapter.AttendanceData>()

    private var groupId: String = ""
    private var scheduleItemId: String = ""
    private var scheduleDateDisplay: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        val buttonBack = findViewById<ImageButton?>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView?>(R.id.textHeaderTitle)

        textHeaderTitle?.text = "Посещаемость"
        buttonBack?.setOnClickListener { finish() }

        listViewChildren = findViewById(R.id.listViewChildren)

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        groupId = intent.getStringExtra("groupId") ?: ""
        scheduleItemId = intent.getStringExtra("scheduleItemId") ?: ""

        if (groupId.isBlank() || scheduleItemId.isBlank()) {
            Toast.makeText(this, "Не переданы данные группы или занятия", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        attendanceAdapter = AttendanceAdapter(attendanceList) { position, status ->
            saveAttendance(
                child = attendanceList[position].child,
                status = status,
                notes = ""
            )
        }

        listViewChildren.adapter = attendanceAdapter

        loadScheduleInfo()
    }

    private fun loadScheduleInfo() {
        database.child("schedules").child(groupId).child(scheduleItemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val schedule = snapshot.getValue(Schedule::class.java)

                    scheduleDateDisplay = if (schedule != null) {
                        "${getDayName(schedule.dayOfWeek)} ${schedule.startTime}-${schedule.endTime}"
                    } else {
                        "Дата не указана"
                    }

                    loadChildren()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Ошибка загрузки занятия: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadChildren()
                }
            })
    }

    private fun loadChildren() {
        database.child("children")
            .orderByChild("groupId")
            .equalTo(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childList.clear()
                    attendanceList.clear()

                    if (!snapshot.exists()) {
                        attendanceAdapter.notifyDataSetChanged()

                        Toast.makeText(
                            this@AttendanceActivity,
                            "В группе пока нет детей",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    database.child("attendance").child(groupId).child(scheduleItemId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(attendanceSnapshot: DataSnapshot) {
                                for (childSnapshot in snapshot.children) {
                                    val child = childSnapshot.getValue(Child::class.java)

                                    if (child != null) {
                                        val fixedChild = if (child.id.isBlank()) {
                                            child.copy(id = childSnapshot.key ?: "")
                                        } else {
                                            child
                                        }

                                        childList.add(fixedChild)

                                        val attendanceRecord = attendanceSnapshot
                                            .child(fixedChild.id)
                                            .getValue(AttendanceRecord::class.java)

                                        val status = attendanceRecord?.status ?: ""

                                        attendanceList.add(
                                            AttendanceAdapter.AttendanceData(
                                                child = fixedChild,
                                                status = status,
                                                attendanceId = fixedChild.id
                                            )
                                        )
                                    }
                                }

                                attendanceAdapter.notifyDataSetChanged()

                                if (attendanceList.isEmpty()) {
                                    Toast.makeText(
                                        this@AttendanceActivity,
                                        "Не удалось загрузить данные детей",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@AttendanceActivity,
                                    "Ошибка загрузки посещаемости: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Ошибка загрузки детей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun saveAttendance(child: Child, status: String, notes: String) {
        val attendanceRef = database
            .child("attendance")
            .child(groupId)
            .child(scheduleItemId)
            .child(child.id)

        val record = AttendanceRecord(
            id = child.id,
            childId = child.id,
            childName = child.name,
            dateDisplay = scheduleDateDisplay,
            status = status,
            notes = notes,
            markedAt = System.currentTimeMillis()
        )

        attendanceRef.setValue(record)
            .addOnSuccessListener {
                val statusText = when (status) {
                    "present" -> "был"
                    "absent" -> "не был"
                    "sick" -> "болел"
                    else -> status
                }

                Toast.makeText(
                    this,
                    "${child.name}: $statusText",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Ошибка сохранения: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            7 -> "Воскресенье"
            else -> "День"
        }
    }
}