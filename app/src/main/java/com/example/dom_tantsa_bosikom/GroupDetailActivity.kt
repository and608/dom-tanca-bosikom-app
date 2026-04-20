package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Group
import com.google.firebase.database.*

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var groupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)

        val tvGroupName = findViewById<TextView>(R.id.tvGroupName)
        val tvTeacherName = findViewById<TextView>(R.id.tvTeacherName)
        val tvAge = findViewById<TextView>(R.id.tvAge)
        val tvSchedule = findViewById<TextView>(R.id.tvSchedule)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)

        val buttonEditGroup = findViewById<Button>(R.id.buttonEditGroup)
        val buttonDeleteGroup = findViewById<Button>(R.id.buttonDeleteGroup)
        val buttonGroupChildren = findViewById<Button>(R.id.buttonGroupChildren)
        val buttonGroupSchedule = findViewById<Button>(R.id.buttonGroupSchedule)
        val buttonGroupAttendance = findViewById<Button>(R.id.buttonGroupAttendance)
        val buttonGroupStatistics = findViewById<Button>(R.id.buttonGroupStatistics)

        textHeaderTitle.text = "Детали группы"
        buttonBack.setOnClickListener { finish() }

        groupId = intent.getStringExtra("groupId") ?: ""

        if (groupId.isBlank()) {
            Toast.makeText(this, "groupId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        loadGroup(
            tvGroupName = tvGroupName,
            tvTeacherName = tvTeacherName,
            tvAge = tvAge,
            tvSchedule = tvSchedule,
            tvDescription = tvDescription
        )

        buttonEditGroup.setOnClickListener {
            val intent = Intent(this, EditGroupActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }

        buttonDeleteGroup.setOnClickListener {
            showDeleteGroupDialog()
        }

        buttonGroupChildren.setOnClickListener {
            val intent = Intent(this, GroupChildrenActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }

        buttonGroupSchedule.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }

        buttonGroupAttendance.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            intent.putExtra("groupId", groupId)
            intent.putExtra("openAttendanceMode", true)
            startActivity(intent)
        }

        buttonGroupStatistics.setOnClickListener {
            val intent = Intent(this, TeacherStatisticsActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val tvGroupName = findViewById<TextView>(R.id.tvGroupName)
        val tvTeacherName = findViewById<TextView>(R.id.tvTeacherName)
        val tvAge = findViewById<TextView>(R.id.tvAge)
        val tvSchedule = findViewById<TextView>(R.id.tvSchedule)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)

        loadGroup(
            tvGroupName = tvGroupName,
            tvTeacherName = tvTeacherName,
            tvAge = tvAge,
            tvSchedule = tvSchedule,
            tvDescription = tvDescription
        )
    }

    private fun loadGroup(
        tvGroupName: TextView,
        tvTeacherName: TextView,
        tvAge: TextView,
        tvSchedule: TextView,
        tvDescription: TextView
    ) {
        database.child("groups").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)

                    if (group != null) {
                        tvGroupName.text = group.name
                        tvTeacherName.text = "Преподаватель: ${group.teacherName}"
                        tvAge.text = "Возраст: ${group.ageRange}"
                        tvSchedule.text = "Расписание: ${group.schedule}"
                        tvDescription.text = "Описание: ${group.description}"
                    } else {
                        Toast.makeText(
                            this@GroupDetailActivity,
                            "Группа не найдена",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroupDetailActivity,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удаление группы")
            .setMessage("Вы уверены, что хотите удалить группу? У детей этой группы будет очищена привязка к группе.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteGroup()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteGroup() {
        database.child("children")
            .orderByChild("groupId")
            .equalTo(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = hashMapOf<String, Any?>()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        val childId = childSnapshot.key ?: continue

                        updates["/children/$childId/groupId"] = ""

                        if (child != null && child.parentId.isNotBlank()) {
                            updates["/users/${child.parentId}/children/$childId/groupId"] = ""
                        }
                    }

                    updates["/groups/$groupId"] = null
                    updates["/group_children/$groupId"] = null

                    database.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@GroupDetailActivity,
                                "Группа удалена",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@GroupDetailActivity,
                                "Ошибка удаления: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroupDetailActivity,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}