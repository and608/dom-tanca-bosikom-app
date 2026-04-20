package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminActivity : AppCompatActivity() {

    private lateinit var textAdminWelcome: TextView
    private lateinit var cardGroups: LinearLayout
    private lateinit var cardTeachers: LinearLayout
    private lateinit var cardParents: LinearLayout
    private lateinit var cardNews: LinearLayout
    private lateinit var cardPayments: LinearLayout
    private lateinit var cardLogout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        textAdminWelcome = findViewById(R.id.textAdminWelcome)
        cardGroups = findViewById(R.id.cardGroups)
        cardTeachers = findViewById(R.id.cardTeachers)
        cardParents = findViewById(R.id.cardParents)
        cardNews = findViewById(R.id.cardNews)
        cardPayments = findViewById(R.id.cardPayments)
        cardLogout = findViewById(R.id.cardLogout)

        loadAdminName()

        cardGroups.setOnClickListener {
            startActivity(Intent(this, ManageGroupActivity::class.java).apply {
                putExtra("role", "admin")
            })
        }

        cardTeachers.setOnClickListener {
            startActivity(Intent(this, TeachersAdminActivity::class.java))
        }

        cardParents.setOnClickListener {
            startActivity(Intent(this, ParentsActivity::class.java))
        }

        cardNews.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java).apply {
                putExtra("role", "admin")
            })
        }

        cardPayments.setOnClickListener {
            startActivity(Intent(this, PaymentsActivity::class.java))
        }

        cardLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadAdminName() {
        val user = auth.currentUser

        if (user == null) {
            textAdminWelcome.text = "Добро пожаловать, руководитель!"
            return
        }

        database.child("users").child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java)

                    if (!fullName.isNullOrEmpty()) {
                        textAdminWelcome.text = "Добро пожаловать, $fullName!"
                    } else {
                        val email = user.email ?: "руководитель"
                        textAdminWelcome.text = "Добро пожаловать, $email!"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    val email = user.email ?: "руководитель"
                    textAdminWelcome.text = "Добро пожаловать, $email!"
                }
            })
    }
}