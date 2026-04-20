package com.example.dom_tantsa_bosikom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dom_tantsa_bosikom.models.News

class NewsAdapter(
    private val newsList: List<News>,
    private val isAdmin: Boolean,
    private val onEditClick: (News) -> Unit,
    private val onDeleteClick: (News) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNewsTitle: TextView = itemView.findViewById(R.id.textNewsTitle)
        val textNewsText: TextView = itemView.findViewById(R.id.textNewsText)
        val textNewsMeta: TextView = itemView.findViewById(R.id.textNewsMeta)
        val layoutAdminActions: LinearLayout = itemView.findViewById(R.id.layoutAdminActions)
        val btnEditNews: Button = itemView.findViewById(R.id.btnEditNews)
        val btnDeleteNews: Button = itemView.findViewById(R.id.btnDeleteNews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]

        holder.textNewsTitle.text = news.title
        holder.textNewsText.text = news.text
        holder.textNewsMeta.text = "${news.author} • ${news.date}"

        if (isAdmin) {
            holder.layoutAdminActions.visibility = View.VISIBLE
            holder.btnEditNews.setOnClickListener {
                onEditClick(news)
            }
            holder.btnDeleteNews.setOnClickListener {
                onDeleteClick(news)
            }
        } else {
            holder.layoutAdminActions.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = newsList.size
}