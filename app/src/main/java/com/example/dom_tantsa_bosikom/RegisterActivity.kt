package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var editTextName: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var radioGroupRole: RadioGroup
    private lateinit var buttonRegister: Button
    private lateinit var textViewGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        editTextName = findViewById(R.id.editTextName)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        radioGroupRole = findViewById(R.id.radioGroupRole)
        buttonRegister = findViewById(R.id.buttonRegister)
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin)

        buttonRegister.setOnClickListener {
            registerUser()
        }

        textViewGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val fullName = editTextName.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        val role = when (radioGroupRole.checkedRadioButtonId) {
            R.id.radioTeacher -> "teacher"
            R.id.radioParent -> "parent"
            else -> ""
        }

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid

                if (uid == null) {
                    Toast.makeText(this, "Ошибка: UID не получен", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = role,
                    childId = "",
                    groupId = ""
                )

                database.reference.child("users").child(uid).setValue(user)
                    .addOnSuccessListener {
                        Log.d("REGISTER", "Данные пользователя сохранены в БД")
                        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()

                        RoleNavigator.openByRole(this, role)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("REGISTER", "Ошибка записи в БД: ${e.message}")
                        Toast.makeText(
                            this,
                            "Ошибка сохранения в базу: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("REGISTER", "Ошибка регистрации: ${e.message}")

                val message = when {
                    e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                        "Этот email уже зарегистрирован"

                    e.message?.contains("badly formatted", ignoreCase = true) == true ->
                        "Некорректный формат email"

                    e.message?.contains("Password should be at least 6 characters", ignoreCase = true) == true ->
                        "Пароль должен содержать не менее 6 символов"

                    else -> "Ошибка регистрации: ${e.message}"
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }
}