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

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var editTextGroupName: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextSchedule: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonSaveGroup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)

        textHeaderTitle.text = "Создание группы"
        buttonBack.setOnClickListener { finish() }

        editTextGroupName = findViewById(R.id.editTextGroupName)
        editTextAge = findViewById(R.id.editTextAge)
        editTextSchedule = findViewById(R.id.editTextSchedule)
        editTextDescription = findViewById(R.id.editTextDescription)
        buttonSaveGroup = findViewById(R.id.buttonSaveGroup)

        buttonSaveGroup.setOnClickListener {
            saveGroup()
        }
    }

    private fun saveGroup() {
        val name = editTextGroupName.text.toString().trim()
        val ageRange = editTextAge.text.toString().trim()
        val schedule = editTextSchedule.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

        if (name.isEmpty() || ageRange.isEmpty() || schedule.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        val groupId = database.child("groups").push().key
        if (groupId == null) {
            Toast.makeText(this, "Ошибка создания ID группы", Toast.LENGTH_SHORT).show()
            return
        }

        val group = Group(
            id = groupId,
            name = name,
            teacherId = "",
            teacherName = "",
            ageRange = ageRange,
            schedule = schedule,
            description = description
        )

        database.child("groups").child(groupId)
            .setValue(group)
            .addOnSuccessListener {
                Toast.makeText(this, "Группа создана", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}