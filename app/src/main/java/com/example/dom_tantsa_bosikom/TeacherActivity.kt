package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TeacherActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        val textWelcome = findViewById<TextView>(R.id.textWelcome)
        val textEmail = findViewById<TextView>(R.id.textEmail)

        val menuGroups = findViewById<LinearLayout>(R.id.menuGroups)
        val menuSchedule = findViewById<LinearLayout>(R.id.menuSchedule)
        val menuAttendance = findViewById<LinearLayout>(R.id.menuAttendance)
        val menuStatistics = findViewById<LinearLayout>(R.id.menuStatistics)
        val menuNews = findViewById<LinearLayout>(R.id.menuNews)
        val buttonLogout = findViewById<Button>(R.id.buttonLogout)

        loadTeacherInfo(textWelcome, textEmail)

        menuGroups.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
        }

        menuSchedule.setOnClickListener {
            Toast.makeText(this, "Сначала выберите группу", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, GroupsActivity::class.java))
        }

        menuAttendance.setOnClickListener {
            Toast.makeText(this, "Сначала выберите группу и занятие", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, GroupsActivity::class.java))
        }

        menuStatistics.setOnClickListener {
            Toast.makeText(this, "Сначала выберите группу", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, GroupsActivity::class.java))
        }

        menuNews.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java).apply {
                putExtra("role", "teacher")
            })
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadTeacherInfo(textWelcome: TextView, textEmail: TextView) {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid == null) {
            textWelcome.text = "Добро пожаловать, преподаватель!"
            textEmail.text = ""
            return
        }

        database.reference
            .child("users")
            .child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    val email = user.email

                    val displayName = when {
                        !name.isNullOrBlank() -> name
                        !fullName.isNullOrBlank() -> fullName
                        !email.isNullOrBlank() -> email
                        else -> "преподаватель"
                    }

                    textWelcome.text = "Добро пожаловать, $displayName!"
                    textEmail.text = email ?: ""
                }

                override fun onCancelled(error: DatabaseError) {
                    val email = user.email
                    val displayName = if (!email.isNullOrBlank()) email else "преподаватель"

                    textWelcome.text = "Добро пожаловать, $displayName!"
                    textEmail.text = email ?: ""
                }
            })
    }
}