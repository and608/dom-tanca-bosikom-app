package com.example.dom_tantsa_bosikom.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.example.dom_tantsa_bosikom.R
import com.example.dom_tantsa_bosikom.models.Child

class ChildrenInGroupAdapter(
    private val context: Context,
    private val childrenList: MutableList<Child>,
    private val onRemoveClick: (Child) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = childrenList.size

    override fun getItem(position: Int): Child = childrenList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.item_child_in_group, parent, false)

            holder = ViewHolder(
                view.findViewById(R.id.child_name),
                view.findViewById(R.id.child_birthdate),
                view.findViewById(R.id.button_remove)
            )

            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val child = getItem(position)

        holder.nameTextView.text = child.name
        holder.birthDateTextView.text = "Дата рождения: ${child.birthDate.ifBlank { "-" }}"

        holder.removeButton.setOnClickListener {
            onRemoveClick(child)
        }

        return view
    }

    private data class ViewHolder(
        val nameTextView: TextView,
        val birthDateTextView: TextView,
        val removeButton: ImageButton
    )
}