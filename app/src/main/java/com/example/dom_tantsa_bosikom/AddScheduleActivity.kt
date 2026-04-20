package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.ScheduleItem
import com.google.firebase.database.FirebaseDatabase

class AddScheduleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        val buttonBack = findViewById<ImageButton?>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView?>(R.id.textHeaderTitle)

        val editTextDate = findViewById<EditText>(R.id.editTextDate)
        val editTextTime = findViewById<EditText>(R.id.editTextTime)
        val editTextTopic = findViewById<EditText>(R.id.editTextTopic)
        val buttonSaveSchedule = findViewById<Button>(R.id.buttonSaveSchedule)

        textHeaderTitle?.text = "Добавить занятие"
        buttonBack?.setOnClickListener { finish() }

        val groupId = intent.getStringExtra("groupId") ?: ""

        if (groupId.isBlank()) {
            Toast.makeText(this, "groupId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        buttonSaveSchedule.setOnClickListener {
            val date = editTextDate.text.toString().trim()
            val time = editTextTime.text.toString().trim()
            val topic = editTextTopic.text.toString().trim()

            if (date.isEmpty() || time.isEmpty() || topic.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance(
                "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
            ).reference

            val scheduleId = database.child("schedules").child(groupId).push().key

            if (scheduleId == null) {
                Toast.makeText(this, "Не удалось создать ID занятия", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val item = ScheduleItem(
                id = scheduleId,
                groupId = groupId,
                date = date,
                time = time,
                topic = topic,
                createdAt = System.currentTimeMillis()
            )

            database.child("schedules").child(groupId).child(scheduleId)
                .setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(this, "Занятие добавлено", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}