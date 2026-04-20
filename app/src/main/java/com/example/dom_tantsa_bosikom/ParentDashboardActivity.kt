package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ParentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var textViewWelcome: TextView
    private lateinit var buttonLogout: ImageButton
    private lateinit var buttonMyChildren: Button
    private lateinit var buttonAttendance: Button
    private lateinit var buttonSchedule: Button
    private lateinit var buttonAddChild: Button
    private lateinit var btnNews: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        textViewWelcome = findViewById(R.id.textViewWelcome)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonMyChildren = findViewById(R.id.buttonMyChildren)
        buttonAttendance = findViewById(R.id.buttonAttendance)
        buttonSchedule = findViewById(R.id.buttonSchedule)
        buttonAddChild = findViewById(R.id.buttonAddChild)
        btnNews = findViewById(R.id.btnNews)

        loadUserInfo()
        setupClickListeners()
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        if (uid == null) {
            textViewWelcome.text = "Добро пожаловать, родитель!"
            return
        }

        database.reference
            .child("users")
            .child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    val email = currentUser.email

                    val displayName = when {
                        !name.isNullOrBlank() -> name
                        !fullName.isNullOrBlank() -> fullName
                        !email.isNullOrBlank() -> email
                        else -> "родитель"
                    }

                    textViewWelcome.text = "Добро пожаловать, $displayName!"
                }

                override fun onCancelled(error: DatabaseError) {
                    val email = currentUser.email
                    val displayName = if (!email.isNullOrBlank()) email else "родитель"
                    textViewWelcome.text = "Добро пожаловать, $displayName!"
                }
            })
    }

    private fun setupClickListeners() {
        buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        buttonMyChildren.setOnClickListener {
            startActivity(Intent(this, MyChildrenActivity::class.java))
        }

        buttonAttendance.setOnClickListener {
            startActivity(Intent(this, ParentAttendanceActivity::class.java))
        }

        buttonSchedule.setOnClickListener {
            startActivity(Intent(this, ParentScheduleActivity::class.java))
        }

        buttonAddChild.setOnClickListener {
            startActivity(Intent(this, AddChildParentActivity::class.java))
        }

        btnNews.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java).apply {
                putExtra("role", "parent")
            })
        }
    }
}