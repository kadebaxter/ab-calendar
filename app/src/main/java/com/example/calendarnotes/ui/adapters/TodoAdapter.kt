package com.example.calendarnotes.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.TodoItem

class TodoAdapter(
    private var todos: List<TodoItem>,
    private var categoryNames: Map<Long, String>,
    private val onToggleComplete: (TodoItem) -> Unit,
    private val onSchedule: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbCompleted: CheckBox = view.findViewById(R.id.cbCompleted)
        val tvTitle: TextView = view.findViewById(R.id.tvTodoTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvTodoDescription)
        val tvCategory: TextView = view.findViewById(R.id.tvTodoCategory)
        val btnSchedule: ImageButton = view.findViewById(R.id.btnScheduleTodo)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTodo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]
        holder.tvTitle.text = todo.title
        holder.tvDescription.text = todo.description
        holder.tvCategory.text = categoryNames[todo.categoryId] ?: "Unknown"
        holder.cbCompleted.isChecked = todo.isCompleted

        // Strike-through completed items
        if (todo.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.cbCompleted.setOnCheckedChangeListener { _, _ ->
            onToggleComplete(todo)
        }

        holder.btnSchedule.setOnClickListener {
            onSchedule(todo)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(todo)
        }
    }

    override fun getItemCount() = todos.size

    fun updateTodos(newTodos: List<TodoItem>, newCategoryNames: Map<Long, String>) {
        todos = newTodos
        categoryNames = newCategoryNames
        notifyDataSetChanged()
    }
}
