package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.adapters.ChildrenInGroupAdapter
import com.example.dom_tantsa_bosikom.models.Child
import com.google.firebase.database.*

class GroupChildrenActivity : AppCompatActivity() {

    private lateinit var listViewGroupChildren: ListView
    private lateinit var database: DatabaseReference
    private lateinit var adapter: ChildrenInGroupAdapter

    private val childrenList = mutableListOf<Child>()

    private var groupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_children)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)

        textHeaderTitle.text = "Дети группы"
        buttonBack.setOnClickListener { finish() }

        listViewGroupChildren = findViewById(R.id.listViewGroupChildren)
        groupId = intent.getStringExtra("groupId") ?: ""

        if (groupId.isBlank()) {
            Toast.makeText(this, "Группа не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        adapter = ChildrenInGroupAdapter(
            context = this,
            childrenList = childrenList,
            onRemoveClick = { child ->
                showDeleteDialog(child)
            }
        )

        listViewGroupChildren.adapter = adapter

        listViewGroupChildren.setOnItemClickListener { _, _, position, _ ->
            val child = childrenList[position]

            val intent = Intent(this, ChildProfileActivity::class.java)
            intent.putExtra("childId", child.id)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroupChildren()
    }

    private fun loadGroupChildren() {
        database.child("children")
            .orderByChild("groupId")
            .equalTo(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenList.clear()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null) {
                            val fixedChild = if (child.id.isBlank()) {
                                child.copy(id = childSnapshot.key ?: "")
                            } else {
                                child
                            }

                            childrenList.add(fixedChild)
                        }
                    }

                    adapter.notifyDataSetChanged()

                    if (childrenList.isEmpty()) {
                        Toast.makeText(
                            this@GroupChildrenActivity,
                            "В этой группе пока нет детей",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroupChildrenActivity,
                        "Ошибка загрузки детей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showDeleteDialog(child: Child) {
        AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Удалить ребёнка из группы?")
            .setPositiveButton("Да") { _, _ ->
                removeChildFromGroup(child)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun removeChildFromGroup(child: Child) {
        if (child.id.isBlank()) {
            Toast.makeText(this, "Некорректный ID ребёнка", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("children").child(child.id).child("groupId")
            .setValue("")
            .addOnSuccessListener {

                if (child.parentId.isNotBlank()) {
                    database.child("users")
                        .child(child.parentId)
                        .child("children")
                        .child(child.id)
                        .child("groupId")
                        .setValue("")
                }

                Toast.makeText(this, "Ребёнок удалён из группы", Toast.LENGTH_SHORT).show()
                loadGroupChildren()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}