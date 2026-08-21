package com.example.calendarnotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Note

class SelectableTaskAdapter(
    private val onSelectionChanged: (Set<Long>) -> Unit
) : RecyclerView.Adapter<SelectableTaskAdapter.ViewHolder>() {

    private var tasks: List<Note> = emptyList()
    private val selectedIds = linkedSetOf<Long>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvStatus: TextView = view.findViewById(R.id.tvTaskStatus)
        val checkbox: CheckBox = view.findViewById(R.id.cbTaskSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        val selected = task.id in selectedIds
        holder.tvTitle.text = task.title
        holder.tvStatus.text = if (task.isCompleted) {
            holder.itemView.context.getString(R.string.task_completed)
        } else {
            holder.itemView.context.getString(R.string.task_open)
        }
        holder.checkbox.isChecked = selected
        holder.itemView.isActivated = selected
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val id = tasks[pos].id
            if (!selectedIds.add(id)) selectedIds.remove(id)
            notifyItemChanged(pos)
            onSelectionChanged(selectedIds.toSet())
        }
    }

    override fun getItemCount() = tasks.size

    fun submit(tasks: List<Note>, selected: Set<Long>) {
        this.tasks = tasks
        selectedIds.clear()
        selectedIds.addAll(selected)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptySet())
    }

    fun selectedIds(): List<Long> = selectedIds.toList()
}
