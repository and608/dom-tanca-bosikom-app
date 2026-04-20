package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.AttendanceRecord
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Schedule
import com.google.firebase.database.*

class TeacherStatisticsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    private lateinit var textTotalChildren: TextView
    private lateinit var textTotalLessons: TextView
    private lateinit var textTotalMarks: TextView
    private lateinit var textPresentCount: TextView
    private lateinit var textAbsentCount: TextView
    private lateinit var textAttendancePercent: TextView
    private lateinit var textAttendanceHistory: TextView
    private lateinit var textChildStats: TextView

    private var groupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_statistics)

        val buttonBack = findViewById<ImageButton?>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView?>(R.id.textHeaderTitle)

        textHeaderTitle?.text = "Статистика"
        buttonBack?.setOnClickListener { finish() }

        textTotalChildren = findViewById(R.id.textTotalChildren)
        textTotalLessons = findViewById(R.id.textTotalLessons)
        textTotalMarks = findViewById(R.id.textTotalMarks)
        textPresentCount = findViewById(R.id.textPresentCount)
        textAbsentCount = findViewById(R.id.textAbsentCount)
        textAttendancePercent = findViewById(R.id.textAttendancePercent)
        textAttendanceHistory = findViewById(R.id.textAttendanceHistory)
        textChildStats = findViewById(R.id.textChildStats)

        groupId = intent.getStringExtra("groupId") ?: ""

        if (groupId.isBlank()) {
            Toast.makeText(this, "groupId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        loadStats()
    }

    private fun loadStats() {
        database.child("children")
            .orderByChild("groupId")
            .equalTo(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(childrenSnapshot: DataSnapshot) {
                    val totalChildren = childrenSnapshot.childrenCount.toInt()

                    val childrenMap = mutableMapOf<String, Child>()

                    for (childSnapshot in childrenSnapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            val fixedChild = if (child.id.isBlank()) {
                                child.copy(id = childSnapshot.key ?: "")
                            } else {
                                child
                            }
                            childrenMap[fixedChild.id] = fixedChild
                        }
                    }

                    database.child("schedules").child(groupId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(scheduleSnapshot: DataSnapshot) {
                                val totalLessons = scheduleSnapshot.childrenCount.toInt()
                                val scheduleMap = mutableMapOf<String, String>()

                                for (scheduleChild in scheduleSnapshot.children) {
                                    val scheduleId = scheduleChild.key ?: continue
                                    val schedule = scheduleChild.getValue(Schedule::class.java)

                                    val lessonTitle = if (schedule != null) {
                                        "${getDayName(schedule.dayOfWeek)} ${schedule.startTime}-${schedule.endTime}"
                                    } else {
                                        "Занятие"
                                    }

                                    scheduleMap[scheduleId] = lessonTitle
                                }

                                loadAttendanceStats(
                                    totalChildren = totalChildren,
                                    totalLessons = totalLessons,
                                    scheduleMap = scheduleMap,
                                    childrenMap = childrenMap
                                )
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@TeacherStatisticsActivity,
                                    "Ошибка загрузки расписания: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@TeacherStatisticsActivity,
                        "Ошибка загрузки детей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadAttendanceStats(
        totalChildren: Int,
        totalLessons: Int,
        scheduleMap: Map<String, String>,
        childrenMap: Map<String, Child>
    ) {
        database.child("attendance").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(attendanceSnapshot: DataSnapshot) {
                    var presentCount = 0
                    var absentCount = 0
                    var sickCount = 0
                    var totalMarks = 0

                    val historyBuilder = StringBuilder()
                    val childStatsBuilder = StringBuilder()

                    val childStatsMap = mutableMapOf<String, Triple<Int, Int, Int>>()

                    for ((childId, _) in childrenMap) {
                        childStatsMap[childId] = Triple(0, 0, 0)
                    }

                    if (!attendanceSnapshot.exists()) {
                        showStats(
                            totalChildren = totalChildren,
                            totalLessons = totalLessons,
                            totalMarks = 0,
                            presentCount = 0,
                            absentCount = 0,
                            sickCount = 0
                        )
                        textAttendanceHistory.text = "Нет данных о посещаемости"
                        textChildStats.text = "Нет данных по детям"
                        return
                    }

                    for (scheduleSnapshot in attendanceSnapshot.children) {
                        val scheduleId = scheduleSnapshot.key ?: continue
                        val lessonTitle = scheduleMap[scheduleId] ?: "Занятие"

                        historyBuilder.append("📅 ").append(lessonTitle).append("\n")

                        for (childAttendanceSnapshot in scheduleSnapshot.children) {
                            val record = childAttendanceSnapshot.getValue(AttendanceRecord::class.java)

                            if (record != null) {
                                totalMarks++

                                val childId = record.childId
                                val currentStats = childStatsMap[childId] ?: Triple(0, 0, 0)

                                val statusText = when (record.status) {
                                    "present" -> {
                                        presentCount++
                                        childStatsMap[childId] = Triple(
                                            currentStats.first + 1,
                                            currentStats.second,
                                            currentStats.third
                                        )
                                        "был(а)"
                                    }
                                    "absent" -> {
                                        absentCount++
                                        childStatsMap[childId] = Triple(
                                            currentStats.first,
                                            currentStats.second + 1,
                                            currentStats.third
                                        )
                                        "не был(а)"
                                    }
                                    "sick" -> {
                                        sickCount++
                                        childStatsMap[childId] = Triple(
                                            currentStats.first,
                                            currentStats.second,
                                            currentStats.third + 1
                                        )
                                        "болел(а)"
                                    }
                                    else -> record.status
                                }

                                val childName = if (record.childName.isNotBlank()) {
                                    record.childName
                                } else {
                                    childrenMap[childId]?.name ?: "Ребёнок"
                                }

                                historyBuilder.append("• ")
                                    .append(childName)
                                    .append(" — ")
                                    .append(statusText)
                                    .append("\n")
                            }
                        }

                        historyBuilder.append("\n")
                    }

                    for ((childId, stats) in childStatsMap) {
                        val childName = childrenMap[childId]?.name ?: "Ребёнок"

                        childStatsBuilder.append(childName)
                            .append(" — был(а): ").append(stats.first)
                            .append(", не был(а): ").append(stats.second)
                            .append(", болел(а): ").append(stats.third)
                            .append("\n")
                    }

                    showStats(
                        totalChildren = totalChildren,
                        totalLessons = totalLessons,
                        totalMarks = totalMarks,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        sickCount = sickCount
                    )

                    textAttendanceHistory.text =
                        if (historyBuilder.isNotEmpty()) historyBuilder.toString()
                        else "Нет данных о посещаемости"

                    textChildStats.text =
                        if (childStatsBuilder.isNotEmpty()) childStatsBuilder.toString()
                        else "Нет данных по детям"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@TeacherStatisticsActivity,
                        "Ошибка загрузки посещаемости: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showStats(
        totalChildren: Int,
        totalLessons: Int,
        totalMarks: Int,
        presentCount: Int,
        absentCount: Int,
        sickCount: Int
    ) {
        val percent = if (totalMarks > 0) {
            (presentCount * 100.0 / totalMarks)
        } else {
            0.0
        }

        textTotalChildren.text = "Детей в группе: $totalChildren"
        textTotalLessons.text = "Занятий в расписании: $totalLessons"
        textTotalMarks.text = "Всего отметок: $totalMarks"
        textPresentCount.text = "Были: $presentCount"
        textAbsentCount.text = "Не были: $absentCount"
        textAttendancePercent.text =
            "Посещаемость: ${"%.1f".format(percent)}%\nБолели: $sickCount"
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
            else -> "Неизвестно"
        }
    }
}