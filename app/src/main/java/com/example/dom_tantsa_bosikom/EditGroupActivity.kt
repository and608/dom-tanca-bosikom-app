package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditGroupActivity : AppCompatActivity() {

    private lateinit var etGroupName: EditText
    private lateinit var etGroupAge: EditText
    private lateinit var etGroupSchedule: EditText
    private lateinit var etGroupDescription: EditText
    private lateinit var btnUpdateGroup: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var groupId: String = ""
    private var teacherId: String = ""
    private var teacherName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_group)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        etGroupName = findViewById(R.id.etGroupName)
        etGroupAge = findViewById(R.id.etGroupAge)
        etGroupSchedule = findViewById(R.id.etGroupSchedule)
        etGroupDescription = findViewById(R.id.etGroupDescription)
        btnUpdateGroup = findViewById(R.id.btnUpdateGroup)

        groupId = intent.getStringExtra("groupId") ?: ""

        if (groupId.isBlank()) {
            Toast.makeText(this, "Группа не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadGroupData()

        btnUpdateGroup.setOnClickListener {
            updateGroup()
        }
    }

    private fun loadGroupData() {
        database.child("groups").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)

                    if (group == null) {
                        Toast.makeText(
                            this@EditGroupActivity,
                            "Данные группы не найдены",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return
                    }

                    etGroupName.setText(group.name)
                    etGroupAge.setText(group.ageRange)
                    etGroupSchedule.setText(group.schedule)
                    etGroupDescription.setText(group.description)
                    teacherName = group.teacherName
                    teacherId = group.teacherId
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@EditGroupActivity,
                        "Ошибка загрузки: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun updateGroup() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val name = etGroupName.text.toString().trim()
        val age = etGroupAge.text.toString().trim()
        val schedule = etGroupSchedule.text.toString().trim()
        val description = etGroupDescription.text.toString().trim()

        if (name.isEmpty()) {
            etGroupName.error = "Введите название группы"
            etGroupName.requestFocus()
            return
        }

        if (age.isEmpty()) {
            etGroupAge.error = "Введите возраст"
            etGroupAge.requestFocus()
            return
        }

        if (schedule.isEmpty()) {
            etGroupSchedule.error = "Введите расписание"
            etGroupSchedule.requestFocus()
            return
        }

        btnUpdateGroup.isEnabled = false

        val updatedGroup = Group(
            id = groupId,
            name = name,
            teacherId = teacherId,
            teacherName = teacherName,
            ageRange = age,
            schedule = schedule,
            description = description
        )

        database.child("groups").child(groupId).setValue(updatedGroup)
            .addOnSuccessListener {
                Toast.makeText(this, "Группа обновлена", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnUpdateGroup.isEnabled = true
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}