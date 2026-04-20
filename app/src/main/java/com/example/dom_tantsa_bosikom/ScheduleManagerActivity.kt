package com.example.dom_tantsa_bosikom

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.dom_tantsa_bosikom.models.Group
import com.example.dom_tantsa_bosikom.models.Schedule

class ScheduleManagerActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var spinnerGroup: Spinner
    private lateinit var listViewSchedule: ListView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var buttonBack: ImageButton
    private lateinit var buttonAdd: ImageButton

    private val groupsList = mutableListOf<Group>()
    private val groupsNames = mutableListOf<String>()

    private val scheduleList = mutableListOf<Schedule>()
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_manager)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app")

        spinnerGroup = findViewById(R.id.spinnerGroup)
        listViewSchedule = findViewById(R.id.listViewSchedule)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        buttonBack = findViewById(R.id.buttonBack)
        buttonAdd = findViewById(R.id.buttonAdd)

        scheduleAdapter = ScheduleAdapter(
            scheduleList,
            onEditClick = { schedule -> showEditDialog(schedule) },
            onDeleteClick = { schedule -> showDeleteDialog(schedule) }
        )
        listViewSchedule.adapter = scheduleAdapter

        loadGroups()

        spinnerGroup.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (groupsList.isNotEmpty() && position < groupsList.size) {
                    loadSchedule(groupsList[position].id)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        buttonBack.setOnClickListener {
            finish()
        }

        buttonAdd.setOnClickListener {
            if (groupsList.isNotEmpty() && spinnerGroup.selectedItemPosition < groupsList.size) {
                showAddDialog()
            } else {
                Toast.makeText(this, "Нет доступных групп", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGroups() {
        val teacherUid = auth.currentUser?.uid ?: return

        database.reference
            .child("groups")
            .orderByChild("teacherUid")
            .equalTo(teacherUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groupsList.clear()
                    groupsNames.clear()

                    for (groupSnapshot in snapshot.children) {
                        val group = groupSnapshot.getValue(Group::class.java)
                        if (group != null) {
                            groupsList.add(group)
                            groupsNames.add(group.name)
                        }
                    }

                    if (groupsNames.isEmpty()) {
                        groupsNames.add("Нет групп")
                    }

                    val adapter = ArrayAdapter(
                        this@ScheduleManagerActivity,
                        android.R.layout.simple_spinner_item,
                        groupsNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerGroup.adapter = adapter

                    if (groupsList.isNotEmpty()) {
                        loadSchedule(groupsList[0].id)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ScheduleManager", "Ошибка загрузки групп: ${error.message}")
                    Toast.makeText(this@ScheduleManagerActivity, "Ошибка загрузки групп", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadSchedule(groupId: String) {
        database.reference
            .child("schedule")
            .child(groupId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scheduleList.clear()

                    for (scheduleSnapshot in snapshot.children) {
                        val schedule = scheduleSnapshot.getValue(Schedule::class.java)
                        if (schedule != null) {
                            scheduleList.add(schedule)
                        }
                    }

                    scheduleAdapter.updateData(scheduleList)
                    updateEmptyState()

                    Log.d("ScheduleManager", "Загружено занятий: ${scheduleList.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ScheduleManager", "Ошибка загрузки расписания: ${error.message}")
                }
            })
    }

    private fun updateEmptyState() {
        if (scheduleList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            listViewSchedule.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            listViewSchedule.visibility = View.VISIBLE
        }
    }

    private fun showAddDialog() {
        val groupId = groupsList[spinnerGroup.selectedItemPosition].id

        ScheduleEditDialog(this, null) { newSchedule ->
            saveSchedule(groupId, newSchedule)
        }.show()
    }

    private fun showEditDialog(schedule: Schedule) {
        val groupId = groupsList[spinnerGroup.selectedItemPosition].id

        ScheduleEditDialog(this, schedule) { updatedSchedule ->
            updateSchedule(groupId, updatedSchedule)
        }.show()
    }

    private fun showDeleteDialog(schedule: Schedule) {
        AlertDialog.Builder(this)
            .setTitle("Удаление занятия")
            .setMessage("Удалить занятие?\n\n${schedule.getDayName()}\n${schedule.getTimeRange()}")
            .setPositiveButton("Удалить") { _, _ ->
                deleteSchedule(schedule)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveSchedule(groupId: String, schedule: Schedule) {
        val ref = database.reference
            .child("schedule")
            .child(groupId)
            .push()

        val newSchedule = schedule.copy(
            id = ref.key ?: "",
            groupId = groupId
        )

        ref.setValue(newSchedule)
            .addOnSuccessListener {
                Toast.makeText(this, "Занятие добавлено", Toast.LENGTH_SHORT).show()
                Log.d("ScheduleManager", "Занятие сохранено")
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("ScheduleManager", "Ошибка: ${exception.message}")
            }
    }

    private fun updateSchedule(groupId: String, schedule: Schedule) {
        database.reference
            .child("schedule")
            .child(groupId)
            .child(schedule.id)
            .setValue(schedule)
            .addOnSuccessListener {
                Toast.makeText(this, "Занятие обновлено", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteSchedule(schedule: Schedule) {
        val groupId = groupsList[spinnerGroup.selectedItemPosition].id

        database.reference
            .child("schedule")
            .child(groupId)
            .child(schedule.id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Занятие удалено", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}