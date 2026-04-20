package com.example.dom_tantsa_bosikom

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dom_tantsa_bosikom.models.News
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var layoutAddNews: LinearLayout
    private lateinit var etNewsTitle: EditText
    private lateinit var etNewsText: EditText
    private lateinit var btnPublishNews: Button
    private lateinit var recyclerNews: RecyclerView

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val newsList = mutableListOf<News>()
    private lateinit var adapter: NewsAdapter

    private var isAdmin = false
    private var editingNewsId: String? = null
    private var currentUserEmail = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        btnBack = findViewById(R.id.btnBack)
        layoutAddNews = findViewById(R.id.layoutAddNews)
        etNewsTitle = findViewById(R.id.etNewsTitle)
        etNewsText = findViewById(R.id.etNewsText)
        btnPublishNews = findViewById(R.id.btnPublishNews)
        recyclerNews = findViewById(R.id.recyclerNews)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dom-tantsa-bosikom-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("news")

        currentUserEmail = auth.currentUser?.email ?: "admin"

        val role = intent.getStringExtra("role") ?: ""
        isAdmin = role == "admin"

        layoutAddNews.visibility = if (isAdmin) View.VISIBLE else View.GONE

        btnBack.setOnClickListener {
            finish()
        }

        adapter = NewsAdapter(
            newsList = newsList,
            isAdmin = isAdmin,
            onEditClick = { news -> startEditing(news) },
            onDeleteClick = { news -> deleteNews(news) }
        )

        recyclerNews.layoutManager = LinearLayoutManager(this)
        recyclerNews.adapter = adapter

        btnPublishNews.setOnClickListener {
            Toast.makeText(this, "Нажатие на кнопку есть", Toast.LENGTH_SHORT).show()
            publishOrUpdateNews()
        }

        loadNews()
    }

    private fun publishOrUpdateNews() {
        val title = etNewsTitle.text.toString().trim()
        val text = etNewsText.text.toString().trim()

        if (title.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))

        if (editingNewsId == null) {
            val id = database.push().key ?: return

            val news = News(
                id = id,
                title = title,
                text = text,
                author = currentUserEmail,
                date = formattedDate,
                timestamp = timestamp
            )

            database.child(id).setValue(news)
                .addOnSuccessListener {
                    Toast.makeText(this, "Новость опубликована", Toast.LENGTH_SHORT).show()
                    clearInputs()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка публикации: ${e.message}", Toast.LENGTH_LONG).show()
                }

        } else {
            val id = editingNewsId!!

            val updatedNews = News(
                id = id,
                title = title,
                text = text,
                author = currentUserEmail,
                date = formattedDate,
                timestamp = timestamp
            )

            database.child(id).setValue(updatedNews)
                .addOnSuccessListener {
                    Toast.makeText(this, "Новость обновлена", Toast.LENGTH_SHORT).show()
                    clearInputs()
                    editingNewsId = null
                    btnPublishNews.text = "Опубликовать"
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadNews() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                newsList.clear()

                for (child in snapshot.children) {
                    val news = child.getValue(News::class.java)
                    if (news != null) {
                        newsList.add(news)
                    }
                }

                newsList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NewsActivity, "Ошибка загрузки новостей: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun startEditing(news: News) {
        editingNewsId = news.id
        etNewsTitle.setText(news.title)
        etNewsText.setText(news.text)
        btnPublishNews.text = "Сохранить изменения"
        layoutAddNews.visibility = View.VISIBLE
    }

    private fun deleteNews(news: News) {
        AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Удалить эту новость?")
            .setPositiveButton("Да") { _, _ ->
                database.child(news.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Новость удалена", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun clearInputs() {
        etNewsTitle.text.clear()
        etNewsText.text.clear()
    }
}