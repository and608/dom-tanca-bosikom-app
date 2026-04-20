package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.database.*

class AddChildToGroupActivity : AppCompatActivity() {

    private lateinit var listViewChildren: ListView
    private lateinit var database: DatabaseReference

    private val childrenList = mutableListOf<Child>()
    private val displayList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private var groupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_child_to_group)

        val buttonBack = findViewById<ImageButton?>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView?>(R.id.textHeaderTitle)

        textHeaderTitle?.text = "Добавить ребенка"
        buttonBack?.setOnClickListener { finish() }

        listViewChildren = findViewById(R.id.listViewChildren)
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        groupId = intent.getStringExtra("groupId") ?: ""

        if (groupId.isBlank()) {
            Toast.makeText(this, "Ошибка: groupId не передан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayList
        )
        listViewChildren.adapter = adapter

        listViewChildren.setOnItemClickListener { _, _, position, _ ->
            val selectedChild = childrenList[position]
            addChildToGroup(selectedChild)
        }

        loadChildrenWithoutGroup()
    }

    private fun loadChildrenWithoutGroup() {
        database.child("children")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenList.clear()
                    displayList.clear()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)

                        if (child != null) {
                            val childId = if (child.id.isNotBlank()) {
                                child.id
                            } else {
                                childSnapshot.key ?: ""
                            }

                            val fixedChild = child.copy(id = childId)

                            if (fixedChild.groupId.isBlank()) {
                                childrenList.add(fixedChild)

                                val displayText = buildString {
                                    append(fixedChild.name)

                                    if (fixedChild.birthDate.isNotBlank()) {
                                        append("\nДата рождения: ")
                                        append(fixedChild.birthDate)
                                    }

                                    if (fixedChild.parentName.isNotBlank()) {
                                        append("\nРодитель: ")
                                        append(fixedChild.parentName)
                                    }

                                    if (fixedChild.notes.isNotBlank()) {
                                        append("\nЗаметки: ")
                                        append(fixedChild.notes)
                                    }
                                }

                                displayList.add(displayText)
                            }
                        }
                    }

                    adapter.notifyDataSetChanged()

                    if (childrenList.isEmpty()) {
                        Toast.makeText(
                            this@AddChildToGroupActivity,
                            "Нет детей без группы",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AddChildToGroupActivity,
                        "Ошибка загрузки: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun addChildToGroup(child: Child) {
        if (child.id.isBlank()) {
            Toast.makeText(this, "Ошибка: id ребенка пустой", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "/children/${child.id}/groupId" to groupId
        )

        if (child.parentId.isNotBlank()) {
            updates["/users/${child.parentId}/children/${child.id}/groupId"] = groupId
        }

        updates["/group_children/$groupId/${child.id}"] = true

        database.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Ребенок добавлен в группу", Toast.LENGTH_SHORT).show()
                loadChildrenWithoutGroup()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Ошибка добавления: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}