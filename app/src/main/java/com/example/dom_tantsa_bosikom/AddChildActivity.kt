package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddChildActivity : AppCompatActivity() {

    private lateinit var etChildName: EditText
    private lateinit var etChildAge: EditText
    private lateinit var btnSaveChild: Button

    private lateinit var auth: FirebaseAuth

    private val database = FirebaseDatabase.getInstance(
        "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_child)

        auth = FirebaseAuth.getInstance()

        etChildName = findViewById(R.id.etChildName)
        etChildAge = findViewById(R.id.etChildAge)
        btnSaveChild = findViewById(R.id.btnSaveChild)

        btnSaveChild.setOnClickListener {
            saveChild()
        }
    }

    private fun saveChild() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        val childName = etChildName.text.toString().trim()
        val childAge = etChildAge.text.toString().trim()

        if (childName.isEmpty()) {
            etChildName.error = "Введите имя ребёнка"
            etChildName.requestFocus()
            return
        }

        if (childAge.isEmpty()) {
            etChildAge.error = "Введите возраст ребёнка"
            etChildAge.requestFocus()
            return
        }

        val childId = database.child("children").push().key
        if (childId == null) {
            Toast.makeText(this, "Ошибка создания записи", Toast.LENGTH_SHORT).show()
            return
        }

        val child = Child(
            id = childId,
            name = childName,
            birthDate = childAge,
            parentId = currentUser.uid,
            parentName = currentUser.displayName ?: currentUser.email ?: "Родитель",
            groupId = "",
            notes = "",
            createdAt = System.currentTimeMillis()
        )

        database.child("children").child(childId).setValue(child)
            .addOnSuccessListener {
                Toast.makeText(this, "Ребёнок добавлен", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}