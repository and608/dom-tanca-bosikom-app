package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Group
import com.example.dom_tantsa_bosikom.models.User
import com.google.firebase.database.*

class TeachersAdminActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var listViewTeachers: ListView
    private lateinit var buttonAddTeacher: Button
    private lateinit var buttonBack: Button

    private val teachersList = mutableListOf<User>()
    private val groupsList = mutableListOf<Group>()

    private lateinit var adapter: TeacherAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teachers_admin)

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        listViewTeachers = findViewById(R.id.listViewTeachers)
        buttonAddTeacher = findViewById(R.id.buttonAddTeacher)
        buttonBack = findViewById(R.id.buttonBack)

        adapter = TeacherAdminAdapter()
        listViewTeachers.adapter = adapter

        buttonAddTeacher.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        buttonBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        database.reference.child("groups")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(groupsSnapshot: DataSnapshot) {
                    groupsList.clear()

                    for (groupSnap in groupsSnapshot.children) {
                        val group = groupSnap.getValue(Group::class.java)
                        if (group != null) {
                            groupsList.add(group)
                        }
                    }

                    loadTeachers()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@TeachersAdminActivity,
                        "Ошибка загрузки групп: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadTeachers() {
        database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usersSnapshot: DataSnapshot) {
                    teachersList.clear()

                    for (userSnap in usersSnapshot.children) {
                        val user = userSnap.getValue(User::class.java)
                        if (user != null && user.role == "teacher") {
                            teachersList.add(user)
                        }
                    }

                    teachersList.sortBy { it.fullName.lowercase() }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@TeachersAdminActivity,
                        "Ошибка загрузки преподавателей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun getGroupsForTeacher(teacherId: String): List<Group> {
        return groupsList.filter { it.teacherId == teacherId }
    }

    private fun confirmDeleteTeacher(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Удаление преподавателя")
            .setMessage(
                "Удалить преподавателя \"${user.fullName}\"?\n\n" +
                        "Он будет удалён из списка пользователей и снят со всех групп."
            )
            .setPositiveButton("Удалить") { _, _ ->
                deleteTeacher(user)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteTeacher(user: User) {
        val teacherGroups = getGroupsForTeacher(user.uid)

        if (teacherGroups.isEmpty()) {
            deleteTeacherFromUsers(user)
            return
        }

        var updatesDone = 0
        var hasError = false

        for (group in teacherGroups) {
            val updates = mapOf<String, Any>(
                "teacherId" to "",
                "teacherName" to ""
            )

            database.reference.child("groups").child(group.id)
                .updateChildren(updates)
                .addOnSuccessListener {
                    updatesDone++
                    if (updatesDone == teacherGroups.size && !hasError) {
                        deleteTeacherFromUsers(user)
                    }
                }
                .addOnFailureListener { e ->
                    hasError = true
                    Toast.makeText(
                        this,
                        "Ошибка снятия с группы ${group.name}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun deleteTeacherFromUsers(user: User) {
        database.reference.child("users").child(user.uid)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Преподаватель удалён",
                    Toast.LENGTH_SHORT
                ).show()
                loadData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Ошибка удаления преподавателя: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    inner class TeacherAdminAdapter : BaseAdapter() {

        override fun getCount(): Int = teachersList.size

        override fun getItem(position: Int): Any = teachersList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@TeachersAdminActivity)
                .inflate(R.layout.item_teacher_admin, parent, false)

            val teacher = teachersList[position]
            val teacherGroups = getGroupsForTeacher(teacher.uid)

            val textTeacherName = view.findViewById<TextView>(R.id.textTeacherName)
            val textTeacherEmail = view.findViewById<TextView>(R.id.textTeacherEmail)
            val textTeacherPhone = view.findViewById<TextView>(R.id.textTeacherPhone)
            val textTeacherGroups = view.findViewById<TextView>(R.id.textTeacherGroups)
            val buttonDeleteTeacher = view.findViewById<Button>(R.id.buttonDeleteTeacher)

            textTeacherName.text = if (teacher.fullName.isNotBlank()) {
                teacher.fullName
            } else {
                "Без имени"
            }

            textTeacherEmail.text = if (teacher.email.isNotBlank()) {
                "Email: ${teacher.email}"
            } else {
                "Email: не указан"
            }

            textTeacherPhone.text = if (teacher.phone.isNotBlank()) {
                "Телефон: ${teacher.phone}"
            } else {
                "Телефон: не указан"
            }

            textTeacherGroups.text = if (teacherGroups.isNotEmpty()) {
                "Группы: " + teacherGroups.joinToString(", ") { it.name }
            } else {
                "Группы: не назначены"
            }

            buttonDeleteTeacher.setOnClickListener {
                confirmDeleteTeacher(teacher)
            }

            return view
        }
    }
}