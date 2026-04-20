package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddGroupActivity : AppCompatActivity() {

    private lateinit var etGroupName: EditText
    private lateinit var etGroupAge: EditText
    private lateinit var etSchedule: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSaveGroup: Button

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase.getInstance(
        "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_group)

        auth = FirebaseAuth.getInstance()

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)

        textHeaderTitle.text = "Создание группы"
        buttonBack.setOnClickListener {
            finish()
        }

        etGroupName = findViewById(R.id.etGroupName)
        etGroupAge = findViewById(R.id.etGroupAge)
        etSchedule = findViewById(R.id.etSchedule)
        etDescription = findViewById(R.id.etDescription)
        btnSaveGroup = findViewById(R.id.btnSaveGroup)

        btnSaveGroup.setOnClickListener {
            saveGroup()
        }
    }

    private fun saveGroup() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val name = etGroupName.text.toString().trim()
        val age = etGroupAge.text.toString().trim()
        val schedule = etSchedule.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (name.isEmpty()) {
            etGroupName.error = "Введите название группы"
            etGroupName.requestFocus()
            return
        }

        if (age.isEmpty()) {
            etGroupAge.error = "Введите возрастную категорию"
            etGroupAge.requestFocus()
            return
        }

        if (schedule.isEmpty()) {
            etSchedule.error = "Введите расписание"
            etSchedule.requestFocus()
            return
        }

        btnSaveGroup.isEnabled = false

        val groupId = db.child("groups").push().key

        if (groupId == null) {
            btnSaveGroup.isEnabled = true
            Toast.makeText(this, "Не удалось создать ID группы", Toast.LENGTH_SHORT).show()
            return
        }

        val role = intent.getStringExtra("role") ?: ""

        val teacherId: String
        val teacherName: String

        if (role == "admin") {
            teacherId = ""
            teacherName = ""
        } else {
            teacherId = currentUser.uid
            teacherName = currentUser.displayName
                ?: currentUser.email
                        ?: "Преподаватель"
        }

        val group = Group(
            id = groupId,
            name = name,
            teacherId = teacherId,
            teacherName = teacherName,
            ageRange = age,
            schedule = schedule,
            description = description
        )

        db.child("groups").child(groupId)
            .setValue(group)
            .addOnSuccessListener {
                Toast.makeText(this, "Группа успешно добавлена", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnSaveGroup.isEnabled = true
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}