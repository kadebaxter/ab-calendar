package com.example.calendarnotes.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Note

object EventTasksList {
    fun bind(
        container: LinearLayout,
        emptyView: TextView,
        tasks: List<Note>,
        onTaskClick: ((Note) -> Unit)? = null
    ) {
        container.removeAllViews()
        if (tasks.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        emptyView.visibility = View.GONE
        container.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(container.context)
        tasks.forEach { task ->
            val row = inflater.inflate(R.layout.item_event_task, container, false)
            val title = row.findViewById<TextView>(R.id.tvTaskTitle)
            val status = row.findViewById<TextView>(R.id.tvTaskStatus)
            title.text = task.title
            status.text = if (task.isCompleted) {
                container.context.getString(R.string.task_completed)
            } else {
                container.context.getString(R.string.task_open)
            }
            if (onTaskClick != null) {
                row.setOnClickListener { onTaskClick(task) }
            }
            container.addView(row)
        }
    }
}
