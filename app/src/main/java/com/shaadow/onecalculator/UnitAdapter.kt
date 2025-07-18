package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UnitAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<UnitViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return UnitViewHolder(view)
    }
    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }
    override fun getItemCount() = items.size
}

class UnitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(text: String, onClick: (String) -> Unit) {
        (itemView as TextView).text = text
        itemView.setOnClickListener { onClick(text) }
    }
} 