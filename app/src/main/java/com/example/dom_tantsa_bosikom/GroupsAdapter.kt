package com.example.dom_tantsa_bosikom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.Group

class GroupAdapter(
    private val context: Context,
    private val groups: List<Group>
) : BaseAdapter() {

    override fun getCount(): Int = groups.size

    override fun getItem(position: Int): Any = groups[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_group, parent, false)

        val group = groups[position]

        val textName = view.findViewById<TextView>(R.id.textGroupName)
        val textInfo = view.findViewById<TextView>(R.id.textGroupInfo)

        textName.text = group.name
        textInfo.text = "Возраст: ${group.ageRange} • ${group.schedule}"

        return view
    }
}