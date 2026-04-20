package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnAdminLogin: Button
    private lateinit var tvGoToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnAdminLogin = findViewById(R.id.btnAdminLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        auth.currentUser?.let { currentUser ->
            openScreenByRole(currentUser.uid)
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

        btnAdminLogin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните email и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    openScreenByRole(uid)
                } else {
                    Toast.makeText(this, "Ошибка получения UID", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка входа: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openScreenByRole(uid: String) {
        database.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Пользователь не найден в базе данных",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        return
                    }

                    val role = snapshot.child("role").getValue(String::class.java)?.lowercase() ?: ""

                    if (role.isEmpty()) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Роль пользователя не указана",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        return
                    }

                    if (role == "admin" || role == "manager") {
                        auth.signOut()
                        Toast.makeText(
                            this@LoginActivity,
                            "Для руководителя используйте отдельный вход",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this@LoginActivity, AdminLoginActivity::class.java))
                        return
                    }

                    RoleNavigator.openByRole(this@LoginActivity, role)
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Ошибка базы данных: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}