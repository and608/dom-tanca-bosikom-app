package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddChildParentActivity : AppCompatActivity() {

    private lateinit var etChildName: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var etNotes: EditText
    private lateinit var spinnerGroups: Spinner
    private lateinit var buttonSave: Button

    private lateinit var auth: FirebaseAuth

    private val database = FirebaseDatabase.getInstance(
        "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_child_parent)

        auth = FirebaseAuth.getInstance()

        etChildName = findViewById(R.id.etChildName)
        etBirthDate = findViewById(R.id.etBirthDate)
        etNotes = findViewById(R.id.etNotes)
        spinnerGroups = findViewById(R.id.spinnerGroups)
        buttonSave = findViewById(R.id.buttonSaveChild)

        // Раз группа больше не выбирается при создании ребёнка,
        // можно скрыть spinner, если он пока есть в layout
        spinnerGroups.visibility = View.GONE

        buttonSave.setOnClickListener {
            saveChild()
        }
    }

    private fun saveChild() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val name = etChildName.text.toString().trim()
        val birthDate = etBirthDate.text.toString().trim()
        val notes = etNotes.text.toString().trim()
        val parentId = currentUser.uid
        val parentName = currentUser.displayName ?: currentUser.email ?: "Родитель"

        if (name.isEmpty()) {
            etChildName.error = "Введите имя ребёнка"
            etChildName.requestFocus()
            return
        }

        if (birthDate.isEmpty()) {
            etBirthDate.error = "Введите дату рождения"
            etBirthDate.requestFocus()
            return
        }

        buttonSave.isEnabled = false

        val childId = database.child("children").push().key
        if (childId == null) {
            buttonSave.isEnabled = true
            Toast.makeText(this, "Ошибка создания ID", Toast.LENGTH_SHORT).show()
            return
        }

        val child = Child(
            id = childId,
            name = name,
            birthDate = birthDate,
            parentId = parentId,
            parentName = parentName,
            groupId = "",
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        val updates = hashMapOf<String, Any>(
            "/children/$childId" to child,
            "/users/$parentId/children/$childId" to child
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Ребёнок добавлен", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                buttonSave.isEnabled = true
                Log.e("AddChildParent", "Ошибка: ${exception.message}", exception)
                Toast.makeText(this, "Ошибка: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}