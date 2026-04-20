package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.User
import com.google.firebase.database.*

class ParentsActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var listViewParents: ListView
    private lateinit var buttonBack: Button

    private val parentsList = mutableListOf<User>()
    private val childrenList = mutableListOf<Child>()

    private lateinit var adapter: ParentAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parents)

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        listViewParents = findViewById(R.id.listViewParents)
        buttonBack = findViewById(R.id.buttonBack)

        adapter = ParentAdminAdapter()
        listViewParents.adapter = adapter

        buttonBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        database.reference.child("children")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(childrenSnapshot: DataSnapshot) {
                    childrenList.clear()

                    for (childSnap in childrenSnapshot.children) {
                        val child = childSnap.getValue(Child::class.java)
                        if (child != null) {
                            childrenList.add(child)
                        }
                    }

                    loadParents()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ParentsActivity,
                        "Ошибка загрузки детей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadParents() {
        database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usersSnapshot: DataSnapshot) {
                    parentsList.clear()

                    for (userSnap in usersSnapshot.children) {
                        val user = userSnap.getValue(User::class.java)
                        if (user != null && user.role == "parent") {
                            parentsList.add(user)
                        }
                    }

                    parentsList.sortBy { it.fullName.lowercase() }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ParentsActivity,
                        "Ошибка загрузки родителей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun getChildrenForParent(parentId: String): List<Child> {
        return childrenList.filter { it.parentId == parentId }
    }

    private fun confirmDeleteParent(parent: User) {
        AlertDialog.Builder(this)
            .setTitle("Удаление родителя")
            .setMessage(
                "Удалить родителя \"${parent.fullName}\"?\n\n" +
                        "Он будет удалён из списка пользователей, а у его детей очистится привязка к родителю."
            )
            .setPositiveButton("Удалить") { _, _ ->
                deleteParent(parent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteParent(parent: User) {
        val parentChildren = getChildrenForParent(parent.uid)

        if (parentChildren.isEmpty()) {
            deleteParentFromUsers(parent)
            return
        }

        var updatesDone = 0
        var hasError = false

        for (child in parentChildren) {
            val childId = if (child.id.isNotBlank()) child.id else continue

            val updates = mapOf<String, Any>(
                "parentId" to "",
                "parentName" to ""
            )

            database.reference.child("children").child(childId)
                .updateChildren(updates)
                .addOnSuccessListener {
                    updatesDone++
                    if (updatesDone == parentChildren.size && !hasError) {
                        deleteParentFromUsers(parent)
                    }
                }
                .addOnFailureListener { e ->
                    hasError = true
                    Toast.makeText(
                        this,
                        "Ошибка обновления ребёнка ${child.name}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun deleteParentFromUsers(parent: User) {
        database.reference.child("users").child(parent.uid)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Родитель удалён", Toast.LENGTH_SHORT).show()
                loadData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Ошибка удаления родителя: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    inner class ParentAdminAdapter : BaseAdapter() {

        override fun getCount(): Int = parentsList.size

        override fun getItem(position: Int): Any = parentsList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ParentsActivity)
                .inflate(R.layout.item_parent_admin, parent, false)

            val parentUser = parentsList[position]
            val children = getChildrenForParent(parentUser.uid)

            val textParentName = view.findViewById<TextView>(R.id.textParentName)
            val textParentEmail = view.findViewById<TextView>(R.id.textParentEmail)
            val textParentPhone = view.findViewById<TextView>(R.id.textParentPhone)
            val textParentChildren = view.findViewById<TextView>(R.id.textParentChildren)
            val buttonDeleteParent = view.findViewById<Button>(R.id.buttonDeleteParent)

            textParentName.text = if (parentUser.fullName.isNotBlank()) {
                parentUser.fullName
            } else {
                "Без имени"
            }

            textParentEmail.text = if (parentUser.email.isNotBlank()) {
                "Email: ${parentUser.email}"
            } else {
                "Email: не указан"
            }

            textParentPhone.text = if (parentUser.phone.isNotBlank()) {
                "Телефон: ${parentUser.phone}"
            } else {
                "Телефон: не указан"
            }

            textParentChildren.text = if (children.isNotEmpty()) {
                "Дети: " + children.joinToString(", ") { it.name }
            } else {
                "Дети: нет"
            }

            buttonDeleteParent.setOnClickListener {
                confirmDeleteParent(parentUser)
            }

            return view
        }
    }
}