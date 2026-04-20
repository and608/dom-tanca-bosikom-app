package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Group
import com.example.dom_tantsa_bosikom.models.ParentScheduleItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ParentScheduleActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var buttonBack: ImageButton
    private lateinit var recyclerViewSchedule: RecyclerView
    private lateinit var textEmpty: TextView

    private val scheduleItems = mutableListOf<ParentScheduleItem>()
    private lateinit var adapter: ParentScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_schedule)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        buttonBack = findViewById(R.id.buttonBack)
        recyclerViewSchedule = findViewById(R.id.recyclerViewSchedule)
        textEmpty = findViewById(R.id.textEmpty)

        adapter = ParentScheduleAdapter(scheduleItems)
        recyclerViewSchedule.layoutManager = LinearLayoutManager(this)
        recyclerViewSchedule.adapter = adapter

        buttonBack.setOnClickListener {
            finish()
        }

        loadScheduleForParent()
    }

    private fun loadScheduleForParent() {
        val parentId = auth.currentUser?.uid

        if (parentId.isNullOrEmpty()) {
            showEmptyState()
            return
        }

        database.reference.child("children")
            .orderByChild("parentId")
            .equalTo(parentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        scheduleItems.clear()
                        adapter.notifyDataSetChanged()
                        showEmptyState()
                        return
                    }

                    val childrenList = mutableListOf<Child>()
                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            childrenList.add(child)
                        }
                    }

                    if (childrenList.isEmpty()) {
                        scheduleItems.clear()
                        adapter.notifyDataSetChanged()
                        showEmptyState()
                        return
                    }

                    loadGroupsForChildren(childrenList)
                }

                override fun onCancelled(error: DatabaseError) {
                    showEmptyState()
                }
            })
    }

    private fun loadGroupsForChildren(childrenList: List<Child>) {
        database.reference.child("groups")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupsMap = mutableMapOf<String, Group>()

                    for (groupSnapshot in snapshot.children) {
                        val group = groupSnapshot.getValue(Group::class.java)
                        if (group != null) {
                            val firebaseKey = groupSnapshot.key.orEmpty()
                            val groupKey = if (group.id.isNotBlank()) group.id else firebaseKey
                            groupsMap[groupKey] = group
                        }
                    }

                    scheduleItems.clear()

                    for (child in childrenList) {
                        val group = groupsMap[child.groupId]

                        val groupName = group?.name?.takeIf { it.isNotBlank() } ?: "Группа не указана"
                        val teacherName = group?.teacherName?.takeIf { it.isNotBlank() } ?: "Не указан"
                        val schedule = group?.schedule?.takeIf { it.isNotBlank() } ?: "Расписание не заполнено"

                        scheduleItems.add(
                            ParentScheduleItem(
                                childName = child.name.ifBlank { "Без имени" },
                                groupName = groupName,
                                teacherName = teacherName,
                                schedule = schedule
                            )
                        )
                    }

                    adapter.notifyDataSetChanged()

                    if (scheduleItems.isEmpty()) {
                        showEmptyState()
                    } else {
                        recyclerViewSchedule.visibility = View.VISIBLE
                        textEmpty.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showEmptyState()
                }
            })
    }

    private fun showEmptyState() {
        recyclerViewSchedule.visibility = View.GONE
        textEmpty.visibility = View.VISIBLE
    }
}