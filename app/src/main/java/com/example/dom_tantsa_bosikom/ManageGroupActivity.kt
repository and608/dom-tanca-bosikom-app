package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ManageGroupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var spinnerGroup: Spinner
    private lateinit var spinnerTeacher: Spinner
    private lateinit var textViewCurrentTeacher: TextView
    private lateinit var buttonAssignTeacher: Button

    private lateinit var buttonCreateGroup: Button
    private lateinit var buttonOpenGroup: Button
    private lateinit var buttonGroupAttendance: Button
    private lateinit var buttonGroupStatistics: Button

    private lateinit var textViewStats: TextView
    private lateinit var listViewChildrenInGroup: ListView
    private lateinit var listViewChildrenWithoutGroup: ListView
    private lateinit var buttonBack: Button

    private val groupsList = mutableListOf<Group>()
    private val groupsNames = mutableListOf<String>()

    private val childrenInGroup = mutableListOf<Child>()
    private val childrenWithoutGroup = mutableListOf<Child>()

    private val teachersList = mutableListOf<Pair<String, String>>() // uid to name
    private val teachersNames = mutableListOf<String>()

    private lateinit var adapterInGroup: ChildManageAdapter
    private lateinit var adapterWithoutGroup: ChildManageAdapter

    private var isAdmin = false

    private fun openChildProfile(child: Child) {
        if (child.id.isBlank()) {
            Toast.makeText(this, "Некорректный ID ребёнка", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ChildProfileActivity::class.java)
        intent.putExtra("childId", child.id)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_group)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        isAdmin = intent.getStringExtra("role") == "admin"

        spinnerGroup = findViewById(R.id.spinnerGroup)
        spinnerTeacher = findViewById(R.id.spinnerTeacher)
        textViewCurrentTeacher = findViewById(R.id.textViewCurrentTeacher)
        buttonAssignTeacher = findViewById(R.id.buttonAssignTeacher)

        buttonCreateGroup = findViewById(R.id.buttonCreateGroup)
        buttonOpenGroup = findViewById(R.id.buttonOpenGroup)
        buttonGroupAttendance = findViewById(R.id.buttonGroupAttendance)
        buttonGroupStatistics = findViewById(R.id.buttonGroupStatistics)

        textViewStats = findViewById(R.id.textViewStats)
        listViewChildrenInGroup = findViewById(R.id.listViewChildrenInGroup)
        listViewChildrenWithoutGroup = findViewById(R.id.listViewChildrenWithoutGroup)
        buttonBack = findViewById(R.id.buttonBack)

        adapterInGroup = ChildManageAdapter(
            context = this,
            items = childrenInGroup,
            isInGroup = true,
            onItemClick = { child ->
                openChildProfile(child)
            },
            onActionClick = { child ->
                removeChildFromGroup(child)
            }
        )

        adapterWithoutGroup = ChildManageAdapter(
            context = this,
            items = childrenWithoutGroup,
            isInGroup = false,
            onItemClick = { child ->
                openChildProfile(child)
            },
            onActionClick = { child ->
                addChildToGroup(child)
            }
        )

        listViewChildrenInGroup.adapter = adapterInGroup
        listViewChildrenWithoutGroup.adapter = adapterWithoutGroup

        buttonBack.setOnClickListener {
            finish()
        }

        buttonCreateGroup.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java).apply {
                putExtra("role", "admin")
            })
        }

        buttonOpenGroup.setOnClickListener {
            val selectedGroup = getSelectedGroupOrNull() ?: return@setOnClickListener

            startActivity(Intent(this, GroupDetailActivity::class.java).apply {
                putExtra("groupId", selectedGroup.id)
            })
        }

        buttonGroupAttendance.setOnClickListener {
            val selectedGroup = getSelectedGroupOrNull() ?: return@setOnClickListener

            startActivity(Intent(this, ScheduleActivity::class.java).apply {
                putExtra("groupId", selectedGroup.id)
                putExtra("openAttendanceMode", true)
            })
        }

        buttonGroupStatistics.setOnClickListener {
            val selectedGroup = getSelectedGroupOrNull() ?: return@setOnClickListener

            startActivity(Intent(this, TeacherStatisticsActivity::class.java).apply {
                putExtra("groupId", selectedGroup.id)
            })
        }

        if (isAdmin) {
            spinnerTeacher.visibility = View.VISIBLE
            textViewCurrentTeacher.visibility = View.VISIBLE
            buttonAssignTeacher.visibility = View.VISIBLE

            buttonCreateGroup.visibility = View.VISIBLE
            buttonOpenGroup.visibility = View.VISIBLE
            buttonGroupAttendance.visibility = View.VISIBLE
            buttonGroupStatistics.visibility = View.VISIBLE

            loadTeachers()

            buttonAssignTeacher.setOnClickListener {
                assignTeacherToSelectedGroup()
            }
        } else {
            spinnerTeacher.visibility = View.GONE
            textViewCurrentTeacher.visibility = View.GONE
            buttonAssignTeacher.visibility = View.GONE

            buttonCreateGroup.visibility = View.GONE
            buttonOpenGroup.visibility = View.GONE
            buttonGroupAttendance.visibility = View.GONE
            buttonGroupStatistics.visibility = View.GONE
        }

        loadGroups()
        loadChildrenWithoutGroup()
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
        loadChildrenWithoutGroup()

        if (groupsList.isNotEmpty() && spinnerGroup.selectedItemPosition < groupsList.size) {
            val selectedGroup = groupsList[spinnerGroup.selectedItemPosition]
            loadChildrenInGroup(selectedGroup.id)

            if (isAdmin) {
                updateCurrentTeacherInfo(selectedGroup)
            }
        }
    }

    private fun getSelectedGroupOrNull(): Group? {
        if (groupsList.isEmpty() || spinnerGroup.selectedItemPosition !in groupsList.indices) {
            Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            return null
        }
        return groupsList[spinnerGroup.selectedItemPosition]
    }

    private fun loadGroups() {
        val groupsQuery = if (isAdmin) {
            database.reference.child("groups")
        } else {
            val teacherUid = auth.currentUser?.uid
            if (teacherUid.isNullOrBlank()) {
                Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
                return
            }
            database.reference
                .child("groups")
                .orderByChild("teacherId")
                .equalTo(teacherUid)
        }

        groupsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupsList.clear()
                groupsNames.clear()

                for (groupSnapshot in snapshot.children) {
                    val group = groupSnapshot.getValue(Group::class.java)
                    if (group != null) {
                        val fixedGroup = if (group.id.isBlank()) {
                            group.copy(id = groupSnapshot.key ?: "")
                        } else {
                            group
                        }

                        groupsList.add(fixedGroup)
                        groupsNames.add(fixedGroup.name.ifBlank { "Без названия" })
                    }
                }

                if (groupsNames.isEmpty()) {
                    groupsNames.add("Нет групп")
                    textViewStats.text = "Детей в группе: 0"
                    childrenInGroup.clear()
                    adapterInGroup.notifyDataSetChanged()

                    if (isAdmin) {
                        textViewCurrentTeacher.text = "Преподаватель: не назначен"
                    }
                }

                val spinnerAdapter = ArrayAdapter(
                    this@ManageGroupActivity,
                    android.R.layout.simple_spinner_item,
                    groupsNames
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerGroup.adapter = spinnerAdapter

                spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (groupsList.isNotEmpty() && position < groupsList.size) {
                            val selectedGroup = groupsList[position]
                            loadChildrenInGroup(selectedGroup.id)

                            if (isAdmin) {
                                updateCurrentTeacherInfo(selectedGroup)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                if (groupsList.isNotEmpty()) {
                    val firstGroup = groupsList.first()
                    loadChildrenInGroup(firstGroup.id)

                    if (isAdmin) {
                        updateCurrentTeacherInfo(firstGroup)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ManageGroup", "Ошибка загрузки групп: ${error.message}")
                Toast.makeText(
                    this@ManageGroupActivity,
                    "Ошибка загрузки групп",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadTeachers() {
        database.reference
            .child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    teachersList.clear()
                    teachersNames.clear()

                    for (userSnapshot in snapshot.children) {
                        val role = userSnapshot.child("role").getValue(String::class.java) ?: ""
                        if (role == "teacher") {
                            val uid = userSnapshot.key ?: continue
                            val name =
                                userSnapshot.child("fullName").getValue(String::class.java)
                                    ?: userSnapshot.child("name").getValue(String::class.java)
                                    ?: userSnapshot.child("email").getValue(String::class.java)
                                    ?: "Без имени"

                            teachersList.add(uid to name)
                            teachersNames.add(name)
                        }
                    }

                    if (teachersNames.isEmpty()) {
                        teachersNames.add("Нет преподавателей")
                    }

                    val teacherAdapter = ArrayAdapter(
                        this@ManageGroupActivity,
                        android.R.layout.simple_spinner_item,
                        teachersNames
                    )
                    teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerTeacher.adapter = teacherAdapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ManageGroupActivity,
                        "Ошибка загрузки преподавателей",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateCurrentTeacherInfo(group: Group) {
        val teacherText = if (group.teacherName.isNotBlank()) {
            group.teacherName
        } else {
            "не назначен"
        }

        textViewCurrentTeacher.text = "Преподаватель: $teacherText"

        if (group.teacherId.isNotBlank()) {
            val index = teachersList.indexOfFirst { it.first == group.teacherId }
            if (index >= 0 && index < teachersList.size) {
                spinnerTeacher.setSelection(index)
            }
        }
    }

    private fun assignTeacherToSelectedGroup() {
        if (!isAdmin) return

        if (groupsList.isEmpty() || spinnerGroup.selectedItemPosition !in groupsList.indices) {
            Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            return
        }

        if (teachersList.isEmpty() || spinnerTeacher.selectedItemPosition !in teachersList.indices) {
            Toast.makeText(this, "Выберите преподавателя", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedGroup = groupsList[spinnerGroup.selectedItemPosition]
        val selectedTeacher = teachersList[spinnerTeacher.selectedItemPosition]

        database.reference
            .child("groups")
            .child(selectedGroup.id)
            .child("teacherId")
            .setValue(selectedTeacher.first)
            .continueWithTask {
                database.reference
                    .child("groups")
                    .child(selectedGroup.id)
                    .child("teacherName")
                    .setValue(selectedTeacher.second)
            }
            .addOnSuccessListener {
                val updatedGroup = selectedGroup.copy(
                    teacherId = selectedTeacher.first,
                    teacherName = selectedTeacher.second
                )
                groupsList[spinnerGroup.selectedItemPosition] = updatedGroup
                textViewCurrentTeacher.text = "Преподаватель: ${selectedTeacher.second}"

                Toast.makeText(this, "Преподаватель назначен", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadChildrenInGroup(groupId: String) {
        if (groupId.isBlank()) {
            childrenInGroup.clear()
            adapterInGroup.notifyDataSetChanged()
            textViewStats.text = "Детей в группе: 0"
            return
        }

        database.reference
            .child("children")
            .orderByChild("groupId")
            .equalTo(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenInGroup.clear()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            val fixedChild = if (child.id.isBlank()) {
                                child.copy(id = childSnapshot.key ?: "")
                            } else {
                                child
                            }
                            childrenInGroup.add(fixedChild)
                        }
                    }

                    adapterInGroup.notifyDataSetChanged()
                    textViewStats.text = "Детей в группе: ${childrenInGroup.size}"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ManageGroup", "Ошибка загрузки детей: ${error.message}")
                    Toast.makeText(
                        this@ManageGroupActivity,
                        "Ошибка загрузки детей группы",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadChildrenWithoutGroup() {
        database.reference
            .child("children")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenWithoutGroup.clear()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            val fixedChild = if (child.id.isBlank()) {
                                child.copy(id = childSnapshot.key ?: "")
                            } else {
                                child
                            }

                            if (fixedChild.groupId.isBlank()) {
                                childrenWithoutGroup.add(fixedChild)
                            }
                        }
                    }

                    adapterWithoutGroup.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ManageGroup", "Ошибка загрузки детей: ${error.message}")
                    Toast.makeText(
                        this@ManageGroupActivity,
                        "Ошибка загрузки детей без группы",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun addChildToGroup(child: Child) {
        if (child.id.isBlank()) {
            Toast.makeText(this, "Некорректный ID ребёнка", Toast.LENGTH_SHORT).show()
            return
        }

        if (groupsList.isEmpty() || spinnerGroup.selectedItemPosition >= groupsList.size) {
            Toast.makeText(this, "Выберите группу", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedGroup = groupsList[spinnerGroup.selectedItemPosition]
        val groupId = selectedGroup.id
        val groupName = selectedGroup.name

        if (groupId.isBlank()) {
            Toast.makeText(this, "Некорректный ID группы", Toast.LENGTH_SHORT).show()
            return
        }

        database.reference
            .child("children")
            .child(child.id)
            .child("groupId")
            .setValue(groupId)
            .addOnSuccessListener {

                if (child.parentId.isNotBlank()) {
                    database.reference
                        .child("users")
                        .child(child.parentId)
                        .child("children")
                        .child(child.id)
                        .child("groupId")
                        .setValue(groupId)
                }

                Toast.makeText(
                    this,
                    "${child.name} добавлен в группу \"$groupName\"",
                    Toast.LENGTH_SHORT
                ).show()

                loadChildrenInGroup(groupId)
                loadChildrenWithoutGroup()
            }
            .addOnFailureListener { exception ->
                Log.e("ManageGroup", "Ошибка добавления: ${exception.message}")
                Toast.makeText(this, "Ошибка: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeChildFromGroup(child: Child) {
        if (child.id.isBlank()) {
            Toast.makeText(this, "Некорректный ID ребёнка", Toast.LENGTH_SHORT).show()
            return
        }

        database.reference
            .child("children")
            .child(child.id)
            .child("groupId")
            .setValue("")
            .addOnSuccessListener {

                if (child.parentId.isNotBlank()) {
                    database.reference
                        .child("users")
                        .child(child.parentId)
                        .child("children")
                        .child(child.id)
                        .child("groupId")
                        .setValue("")
                }

                Toast.makeText(this, "${child.name} удалён из группы", Toast.LENGTH_SHORT).show()

                if (groupsList.isNotEmpty() && spinnerGroup.selectedItemPosition < groupsList.size) {
                    val currentGroup = groupsList[spinnerGroup.selectedItemPosition]
                    loadChildrenInGroup(currentGroup.id)
                } else {
                    childrenInGroup.clear()
                    adapterInGroup.notifyDataSetChanged()
                    textViewStats.text = "Детей в группе: 0"
                }

                loadChildrenWithoutGroup()
            }
            .addOnFailureListener { exception ->
                Log.e("ManageGroup", "Ошибка удаления: ${exception.message}")
                Toast.makeText(this, "Ошибка: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

class ChildManageAdapter(
    private val context: ManageGroupActivity,
    private val items: MutableList<Child>,
    private val isInGroup: Boolean,
    private val onItemClick: (Child) -> Unit,
    private val onActionClick: (Child) -> Unit
) : ArrayAdapter<Child>(context, 0, items) {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Child = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_child_manage, parent, false)

        val child = getItem(position)

        val textViewChildName: TextView = view.findViewById(R.id.textViewChildName)
        val textViewBirthDate: TextView = view.findViewById(R.id.textViewBirthDate)
        val buttonAction: Button = view.findViewById(R.id.buttonAction)

        textViewChildName.text = child.name
        textViewBirthDate.text = "Дата рождения: ${child.birthDate.ifBlank { "-" }}"
        buttonAction.text = if (isInGroup) "Удалить" else "Добавить"

        view.isClickable = true
        view.isFocusable = false

        view.setOnClickListener {
            onItemClick(child)
        }

        buttonAction.setOnClickListener {
            onActionClick(child)
        }

        return view
    }
}