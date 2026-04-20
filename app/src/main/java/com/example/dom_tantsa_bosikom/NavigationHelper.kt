package com.example.dom_tantsa_bosikom

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object NavigationHelper {

    private const val DATABASE_URL = "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"

    fun navigateByRole(context: Context, onComplete: (() -> Unit)? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance(DATABASE_URL)
            .reference
            .child("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.value as? String ?: "parent"

                val intent = when (role) {
                    "admin" -> Intent(context, AdminActivity::class.java)
                    "teacher" -> Intent(context, TeacherActivity::class.java)
                    else -> Intent(context, ParentDashboardActivity::class.java)
                }

                context.startActivity(intent)
                onComplete?.invoke()
            }
            .addOnFailureListener {
                val intent = Intent(context, ParentDashboardActivity::class.java)
                context.startActivity(intent)
                onComplete?.invoke()
            }
    }
}