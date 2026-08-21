package com.example.calendarnotes.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Note

object TasksPicker {
    fun createIntent(
        context: Context,
        tasks: List<Note>,
        initiallySelected: Set<Long>
    ): Intent? {
        if (tasks.isEmpty()) {
            Toast.makeText(context, R.string.add_tasks_first, Toast.LENGTH_SHORT).show()
            return null
        }
        return SelectTasksActivity.createIntent(context, initiallySelected)
    }

    fun selectedIdsFromResult(data: Intent?): List<Long> {
        return SelectTasksActivity.selectedIdsFromResult(data)
    }
}
