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

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvBackToUserLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvBackToUserLogin = findViewById(R.id.tvBackToUserLogin)

        auth.currentUser?.let { currentUser ->
            checkAdminAccess(currentUser.uid)
        }

        btnLogin.setOnClickListener {
            loginAdmin()
        }

        tvBackToUserLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loginAdmin() {
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
                    checkAdminAccess(uid)
                } else {
                    Toast.makeText(this, "Ошибка получения UID", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка входа: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkAdminAccess(uid: String) {
        database.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(
                            this@AdminLoginActivity,
                            "Пользователь не найден в базе данных",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        return
                    }

                    val role = snapshot.child("role").getValue(String::class.java)?.lowercase() ?: ""

                    if (role == "admin" || role == "manager") {
                        val intent = Intent(this@AdminLoginActivity, AdminActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        auth.signOut()
                        Toast.makeText(
                            this@AdminLoginActivity,
                            "У вас нет доступа руководителя",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AdminLoginActivity,
                        "Ошибка базы данных: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}