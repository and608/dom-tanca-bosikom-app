package com.example.dom_tantsa_bosikom

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Payment
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentsActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var listViewPayments: ListView
    private lateinit var buttonBack: Button

    private val childrenList = mutableListOf<Child>()
    private val paymentsMap = mutableMapOf<String, Payment>()

    private lateinit var adapter: PaymentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments)

        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        )

        listViewPayments = findViewById(R.id.listViewPayments)
        buttonBack = findViewById(R.id.buttonBack)

        adapter = PaymentsAdapter()
        listViewPayments.adapter = adapter

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
                            val fixedChild = if (child.id.isBlank()) {
                                child.copy(id = childSnap.key ?: "")
                            } else {
                                child
                            }
                            childrenList.add(fixedChild)
                        }
                    }

                    loadPayments()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@PaymentsActivity,
                        "Ошибка загрузки детей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadPayments() {
        paymentsMap.clear()
        val currentMonth = getCurrentMonth()

        database.reference.child("payments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(paymentsSnapshot: DataSnapshot) {
                    for (paymentSnap in paymentsSnapshot.children) {
                        val payment = paymentSnap.getValue(Payment::class.java)
                        if (payment != null && payment.month == currentMonth) {
                            paymentsMap[payment.childId] = payment
                        }
                    }

                    childrenList.sortBy { it.name.lowercase() }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@PaymentsActivity,
                        "Ошибка загрузки оплат: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun getCurrentMonth(): String {
        val format = SimpleDateFormat("MM_yyyy", Locale.getDefault())
        return format.format(Date())
    }

    inner class PaymentsAdapter : BaseAdapter() {

        override fun getCount(): Int = childrenList.size

        override fun getItem(position: Int): Any = childrenList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@PaymentsActivity)
                .inflate(R.layout.item_payment_admin, parent, false)

            val child = childrenList[position]
            val payment = paymentsMap[child.id]

            val textChildName = view.findViewById<TextView>(R.id.textChildName)
            val textParentName = view.findViewById<TextView>(R.id.textParentName)
            val textBirthDate = view.findViewById<TextView>(R.id.textBirthDate)
            val textMonth = view.findViewById<TextView>(R.id.textMonth)
            val textStatus = view.findViewById<TextView>(R.id.textStatus)

            textChildName.text = if (child.name.isNotBlank()) {
                "Ребёнок: ${child.name}"
            } else {
                "Ребёнок: без имени"
            }

            textParentName.text = if (child.parentName.isNotBlank()) {
                "Родитель: ${child.parentName}"
            } else {
                "Родитель: не указан"
            }

            textBirthDate.text = if (child.birthDate.isNotBlank()) {
                "Дата рождения: ${child.birthDate}"
            } else {
                "Дата рождения: не указана"
            }

            textMonth.text = "Месяц: ${getCurrentMonth()}"

            if (payment?.status == "paid") {
                textStatus.text = "Статус: Оплачено"
                textStatus.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                textStatus.text = "Статус: Не оплачено"
                textStatus.setTextColor(Color.parseColor("#C62828"))
            }

            return view
        }
    }
}