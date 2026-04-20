package com.example.dom_tantsa_bosikom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.example.dom_tantsa_bosikom.models.Child

class ChildSelectAdapter(
    private val context: Context,
    private val children: MutableList<Child>,
    private val selectedIds: MutableSet<String>
) : BaseAdapter() {

    override fun getCount(): Int = children.size

    override fun getItem(position: Int): Child = children[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_child_select, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val child = children[position]

        holder.name.text = child.name
        holder.birthDate.text = child.birthDate

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedIds.contains(child.id)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(child.id)
            } else {
                selectedIds.remove(child.id)
            }
        }

        view.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }

        return view
    }

    fun updateList(newChildren: List<Child>) {
        children.clear()
        children.addAll(newChildren)
        notifyDataSetChanged()
    }

    private class ViewHolder(view: View) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val name: TextView = view.findViewById(R.id.textViewChildName)
        val birthDate: TextView = view.findViewById(R.id.textViewChildBirthDate)
    }
}