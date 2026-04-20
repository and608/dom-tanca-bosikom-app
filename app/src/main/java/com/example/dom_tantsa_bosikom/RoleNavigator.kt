package com.example.dom_tantsa_bosikom

import android.content.Context
import android.content.Intent
import android.widget.Toast

object RoleNavigator {

    fun openByRole(context: Context, role: String) {
        val intent = when (role.lowercase()) {
            "teacher" -> Intent(context, TeacherActivity::class.java)
            "parent" -> Intent(context, ParentDashboardActivity::class.java)
            "admin", "manager" -> Intent(context, AdminActivity::class.java)
            else -> {
                Toast.makeText(context, "Неизвестная роль: $role", Toast.LENGTH_LONG).show()
                return
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}