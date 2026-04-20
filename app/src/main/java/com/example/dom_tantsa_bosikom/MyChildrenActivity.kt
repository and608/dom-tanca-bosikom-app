package com.example.dom_tantsa_bosikom

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dom_tantsa_bosikom.models.Child
import com.example.dom_tantsa_bosikom.models.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyChildrenActivity : AppCompatActivity() {

    private lateinit var listViewChildren: ListView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val childrenList = mutableListOf<Child>()
    private val paymentsMap = mutableMapOf<String, Payment>()

    private lateinit var adapter: ChildrenPaymentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_children)

        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)

        textHeaderTitle.text = "Мои дети"
        buttonBack.setOnClickListener { finish() }

        listViewChildren = findViewById(R.id.listViewChildren)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        adapter = ChildrenPaymentAdapter()
        listViewChildren.adapter = adapter

        loadChildrenAndPayments()
    }

    private fun loadChildrenAndPayments() {
        val parentUid = auth.currentUser?.uid
        if (parentUid == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("children")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    childrenList.clear()

                    for (childSnapshot in snapshot.children) {
                        val child = childSnapshot.getValue(Child::class.java)
                        if (child != null && child.parentId == parentUid) {
                            val childId = if (child.id.isNotBlank()) {
                                child.id
                            } else {
                                childSnapshot.key ?: ""
                            }

                            val fixedChild = child.copy(id = childId)
                            childrenList.add(fixedChild)
                        }
                    }

                    loadPayments()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MyChildrenActivity,
                        "Ошибка загрузки детей: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadPayments() {
        paymentsMap.clear()

        database.child("payments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentMonth = getCurrentMonth()

                    for (paymentSnapshot in snapshot.children) {
                        val payment = paymentSnapshot.getValue(Payment::class.java)
                        if (payment != null &&
                            payment.month == currentMonth &&
                            payment.childId.isNotBlank()
                        ) {
                            paymentsMap[payment.childId] = payment
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MyChildrenActivity,
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

    private fun payForChild(child: Child) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        if (child.id.isBlank()) {
            Toast.makeText(this, "Ошибка: у ребёнка отсутствует ID", Toast.LENGTH_LONG).show()
            return
        }

        val currentMonth = getCurrentMonth()
        val paymentId = "${child.id}_$currentMonth"
        val parentDisplayName = user.email ?: "Родитель"

        val payment = Payment(
            id = paymentId,
            childId = child.id,
            childName = child.name,
            parentId = user.uid,
            parentName = parentDisplayName,
            month = currentMonth,
            status = "paid"
        )

        database.child("payments").child(paymentId)
            .setValue(payment)
            .addOnSuccessListener {
                paymentsMap[child.id] = payment
                adapter.notifyDataSetChanged()
                Toast.makeText(this@MyChildrenActivity, "Абонемент оплачен", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@MyChildrenActivity,
                    "Ошибка оплаты: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    inner class ChildrenPaymentAdapter : BaseAdapter() {

        override fun getCount(): Int = childrenList.size

        override fun getItem(position: Int): Any = childrenList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@MyChildrenActivity)
                .inflate(R.layout.item_child_payment, parent, false)

            val child = childrenList[position]
            val payment = paymentsMap[child.id]

            val textChildInfo = view.findViewById<TextView>(R.id.textChildInfo)
            val textPaymentStatus = view.findViewById<TextView>(R.id.textPaymentStatus)
            val buttonPay = view.findViewById<Button>(R.id.buttonPay)

            textChildInfo.text =
                "Имя: ${child.name}\n" +
                        "Дата рождения: ${child.birthDate}\n" +
                        "Заметки: ${child.notes}"

            if (payment?.status == "paid") {
                textPaymentStatus.text = "Статус: Оплачено"
                textPaymentStatus.setTextColor(Color.parseColor("#2E7D32"))
                buttonPay.isEnabled = false
                buttonPay.text = "Оплачено"
            } else {
                textPaymentStatus.text = "Статус: Не оплачено"
                textPaymentStatus.setTextColor(Color.parseColor("#C62828"))
                buttonPay.isEnabled = true
                buttonPay.text = "Оплатить абонемент"
            }

            buttonPay.setOnClickListener {
                payForChild(child)
            }

            return view
        }
    }
}