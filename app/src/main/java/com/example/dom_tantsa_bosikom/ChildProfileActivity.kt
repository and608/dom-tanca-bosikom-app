package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.User
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent
import android.widget.Button

class ChildProfileActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    private lateinit var textChildName: TextView
    private lateinit var textChildAge: TextView
    private lateinit var textChildBirthDate: TextView
    private lateinit var textParentName: TextView
    private lateinit var textParentPhone: TextView
    private lateinit var textNotes: TextView
    private lateinit var buttonAttendance: Button
    private lateinit var buttonStatistics: Button

    private var childId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_profile)

        val buttonBack = findViewById<ImageButton?>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView?>(R.id.textHeaderTitle)

        textHeaderTitle?.text = "Профиль ребёнка"
        buttonBack?.setOnClickListener { finish() }

        textChildName = findViewById(R.id.textChildName)
        textChildAge = findViewById(R.id.textChildAge)
        textChildBirthDate = findViewById(R.id.textChildBirthDate)
        textParentName = findViewById(R.id.textParentName)
        textParentPhone = findViewById(R.id.textParentPhone)
        textNotes = findViewById(R.id.textNotes)
        buttonAttendance = findViewById(R.id.buttonAttendance)
        buttonStatistics = findViewById(R.id.buttonStatistics)

        childId = intent.getStringExtra("childId") ?: ""

        if (childId.isBlank()) {
            Toast.makeText(this, "childId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        buttonAttendance.setOnClickListener {
            val intent = Intent(this, ChildAttendanceActivity::class.java)
            intent.putExtra("childId", childId)
            startActivity(intent)
        }

        buttonStatistics.setOnClickListener {
            val intent = Intent(this, ChildStatisticsActivity::class.java)
            intent.putExtra("childId", childId)
            startActivity(intent)
        }

        loadChildProfile()
    }

    private fun loadChildProfile() {
        database.child("children").child(childId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val child = snapshot.getValue(Child::class.java)

                    if (child == null) {
                        Toast.makeText(
                            this@ChildProfileActivity,
                            "Ребёнок не найден",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return
                    }

                    showChildData(child)
                    loadParentData(child)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChildProfileActivity,
                        "Ошибка загрузки ребёнка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showChildData(child: Child) {
        textChildName.text = "Имя: ${child.name}"

        val ageText = calculateAge(child.birthDate)
        textChildAge.text = "Возраст: $ageText"

        textChildBirthDate.text = "Дата рождения: ${child.birthDate.ifBlank { "-" }}"
        textNotes.text = "Примечания: ${child.notes.ifBlank { "-" }}"

        if (child.parentId.isBlank()) {
            textParentName.text = "ФИО родителя: ${child.parentName.ifBlank { "-" }}"
            textParentPhone.text = "Телефон: -"
        }
    }

    private fun loadParentData(child: Child) {
        if (child.parentId.isBlank()) return

        database.child("users").child(child.parentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val parent = snapshot.getValue(User::class.java)

                    if (parent != null) {
                        textParentName.text = "ФИО родителя: ${parent.fullName.ifBlank { child.parentName.ifBlank { "-" } }}"
                        textParentPhone.text = "Телефон: ${parent.phone.ifBlank { "-" }}"
                    } else {
                        textParentName.text = "ФИО родителя: ${child.parentName.ifBlank { "-" }}"
                        textParentPhone.text = "Телефон: -"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    textParentName.text = "ФИО родителя: ${child.parentName.ifBlank { "-" }}"
                    textParentPhone.text = "Телефон: -"
                    Toast.makeText(
                        this@ChildProfileActivity,
                        "Ошибка загрузки родителя: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun calculateAge(birthDate: String): String {
        if (birthDate.isBlank()) return "-"

        return try {
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val birth = formatter.parse(birthDate) ?: return "-"

            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance()
            birthCal.time = birth

            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)

            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            if (age < 0) "-" else "$age"
        } catch (e: Exception) {
            "-"
        }
    }
}