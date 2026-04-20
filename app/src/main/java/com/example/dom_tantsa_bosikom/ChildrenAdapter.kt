package com.example.dom_tantsa_bosikom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.Child

class ChildrenAdapter(
    private val children: List<Child>
) : BaseAdapter() {

    override fun getCount(): Int = children.size

    override fun getItem(position: Int): Any = children[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val child = children[position]

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        text1.text = child.name
        text2.text = "Дата рождения: ${child.birthDate}"

        return view
    }
}