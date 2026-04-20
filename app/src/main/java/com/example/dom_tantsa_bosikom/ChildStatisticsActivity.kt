package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.AttendanceRecord
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.database.*

class ChildStatisticsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    private lateinit var textTotalClasses: TextView
    private lateinit var textPresentCount: TextView
    private lateinit var textAbsentCount: TextView
    private lateinit var textSickCount: TextView
    private lateinit var textAttendancePercent: TextView

    private var childId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_statistics)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)

        textHeaderTitle.text = "Статистика ребёнка"
        buttonBack.setOnClickListener { finish() }

        textTotalClasses = findViewById(R.id.textTotalClasses)
        textPresentCount = findViewById(R.id.textPresentCount)
        textAbsentCount = findViewById(R.id.textAbsentCount)
        textSickCount = findViewById(R.id.textSickCount)
        textAttendancePercent = findViewById(R.id.textAttendancePercent)

        childId = intent.getStringExtra("childId") ?: ""

        if (childId.isBlank()) {
            Toast.makeText(this, "childId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        loadChildAndStatistics()
    }

    private fun loadChildAndStatistics() {
        database.child("children").child(childId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val child = snapshot.getValue(Child::class.java)

                    if (child == null) {
                        Toast.makeText(
                            this@ChildStatisticsActivity,
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
                        showEmptyStatistics("Ребёнок не состоит в группе")
                        return
                    }

                    loadStatisticsByGroup(realChild.groupId, realChild.id)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChildStatisticsActivity,
                        "Ошибка загрузки ребёнка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadStatisticsByGroup(groupId: String, childId: String) {
        database.child("attendance").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showEmptyStatistics("Нет данных о посещаемости")
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
                        showEmptyStatistics("Нет отметок посещаемости")
                        return
                    }

                    var presentCount = 0
                    var absentCount = 0
                    var sickCount = 0

                    for (record in records) {
                        when (record.status) {
                            "present" -> presentCount++
                            "absent" -> absentCount++
                            "sick" -> sickCount++
                        }
                    }

                    val totalClasses = records.size
                    val attendancePercent = if (totalClasses > 0) {
                        (presentCount * 100) / totalClasses
                    } else {
                        0
                    }

                    textTotalClasses.text = "Всего отмеченных занятий: $totalClasses"
                    textPresentCount.text = "Присутствовал: $presentCount"
                    textAbsentCount.text = "Отсутствовал: $absentCount"
                    textSickCount.text = "Болел: $sickCount"
                    textAttendancePercent.text = "Процент посещаемости: $attendancePercent%"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChildStatisticsActivity,
                        "Ошибка загрузки статистики: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showEmptyStatistics(message: String) {
        textTotalClasses.text = message
        textPresentCount.text = "Присутствовал: 0"
        textAbsentCount.text = "Отсутствовал: 0"
        textSickCount.text = "Болел: 0"
        textAttendancePercent.text = "Процент посещаемости: 0%"
    }
}