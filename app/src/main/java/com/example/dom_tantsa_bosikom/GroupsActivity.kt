package com.example.dom_tantsa_bosikom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupsActivity : AppCompatActivity() {

    private lateinit var listViewGroups: ListView
    private lateinit var buttonCreateGroup: Button
    private lateinit var buttonManageGroup: Button
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: GroupAdapter

    private val groupsList = mutableListOf<Group>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        listViewGroups = findViewById(R.id.listViewGroups)
        buttonCreateGroup = findViewById(R.id.buttonCreateGroup)
        buttonManageGroup = findViewById(R.id.buttonManageGroup)

        adapter = GroupAdapter(this, groupsList)
        listViewGroups.adapter = adapter

        buttonCreateGroup.setOnClickListener {
            startActivity(Intent(this, AddGroupActivity::class.java))
        }

        buttonManageGroup.setOnClickListener {
            startActivity(Intent(this, ManageGroupActivity::class.java))
        }

        listViewGroups.setOnItemClickListener { _, _, position, _ ->
            val selectedGroup = groupsList[position]
            val intent = Intent(this, GroupDetailActivity::class.java)
            intent.putExtra("groupId", selectedGroup.id)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    private fun loadGroups() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("groups")
            .orderByChild("teacherId")
            .equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groupsList.clear()

                    for (groupSnapshot in snapshot.children) {
                        val group = groupSnapshot.getValue(Group::class.java)
                        if (group != null) {
                            val groupWithId = group.copy(id = groupSnapshot.key ?: "")
                            groupsList.add(groupWithId)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroupsActivity,
                        "Ошибка загрузки групп: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}