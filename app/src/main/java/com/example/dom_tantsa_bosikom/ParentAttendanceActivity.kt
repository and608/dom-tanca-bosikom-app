package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Attendance
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ParentAttendanceActivity : AppCompatActivity() {

    private lateinit var textHeaderTitle: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var buttonBack: ImageButton
    private lateinit var spinnerChild: Spinner
    private lateinit var listViewAttendance: ListView
    private lateinit var layoutEmpty: LinearLayout

    private val childrenList = mutableListOf<Child>()
    private val childrenNames = mutableListOf<String>()
    private val attendanceList = mutableListOf<Attendance>()

    private var selectedChildId: String = ""
    private lateinit var attendanceAdapter: AttendanceListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_attendance)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        buttonBack = findViewById(R.id.buttonBack)
        textHeaderTitle = findViewById(R.id.textHeaderTitle)
        spinnerChild = findViewById(R.id.spinnerChild)
        listViewAttendance = findViewById(R.id.listViewAttendance)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        textHeaderTitle.text = "Посещаемость"

        attendanceAdapter = AttendanceListAdapter(attendanceList)
        listViewAttendance.adapter = attendanceAdapter

        buttonBack.setOnClickListener {
            finish()
        }

        spinnerChild.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position < childrenList.size) {
                    selectedChildId = childrenList[position].id
                    loadAttendance()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadChildren()
    }

    private fun loadChildren() {
        val parentUid = auth.currentUser?.uid ?: return

        database.reference
            .child("children")
            .orderByChild("parentId")
            .equalTo(parentUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenList.clear()
                    childrenNames.clear()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            val childId = if (child.id.isNotBlank()) {
                                child.id
                            } else {
                                childSnapshot.key ?: ""
                            }

                            val fixedChild = child.copy(id = childId)
                            childrenList.add(fixedChild)
                            childrenNames.add(fixedChild.name)
                        }
                    }

                    val adapter = ArrayAdapter(
                        this@ParentAttendanceActivity,
                        android.R.layout.simple_spinner_item,
                        childrenNames.ifEmpty { listOf("Нет детей") }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerChild.adapter = adapter

                    if (childrenList.isEmpty()) {
                        selectedChildId = ""
                        attendanceList.clear()
                        attendanceAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    } else {
                        selectedChildId = childrenList[0].id
                        loadAttendance()
                    }

                    Log.d("ParentAttendance", "Найдено детей: ${childrenList.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ParentAttendance", "Ошибка загрузки детей: ${error.message}")
                }
            })
    }

    private fun loadAttendance() {
        if (selectedChildId.isEmpty()) {
            attendanceList.clear()
            attendanceAdapter.notifyDataSetChanged()
            updateEmptyState()
            return
        }

        database.reference
            .child("attendance")
            .orderByChild("childId")
            .equalTo(selectedChildId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    attendanceList.clear()

                    for (attendanceSnapshot in snapshot.children) {
                        val attendance = attendanceSnapshot.getValue(Attendance::class.java)
                        if (attendance != null) {
                            attendanceList.add(attendance)
                        }
                    }

                    attendanceList.sortByDescending { it.date }

                    attendanceAdapter.notifyDataSetChanged()
                    updateEmptyState()

                    Log.d(
                        "ParentAttendance",
                        "Загружено записей посещаемости: ${attendanceList.size}"
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ParentAttendance", "Ошибка загрузки посещаемости: ${error.message}")
                }
            })
    }

    private fun updateEmptyState() {
        if (attendanceList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            listViewAttendance.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            listViewAttendance.visibility = View.VISIBLE
        }
    }
}