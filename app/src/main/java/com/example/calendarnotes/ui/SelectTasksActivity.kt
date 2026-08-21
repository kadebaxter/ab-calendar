package com.example.calendarnotes.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.ui.adapters.SelectableTaskAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton

class SelectTasksActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var adapter: SelectableTaskAdapter
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClear: MaterialButton
    private lateinit var btnDone: MaterialButton
    private val selectedIds = linkedSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_people)

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        tvCount = findViewById(R.id.tvSelectionCount)
        tvEmpty = findViewById(R.id.tvSelectPeopleEmpty)
        btnClear = findViewById(R.id.btnClearSelection)
        btnDone = findViewById(R.id.btnDoneSelecting)
        tvEmpty.setText(R.string.select_tasks_empty)

        AppHeader.setupBackHeader(this, getString(R.string.select_tasks))
        selectedIds.addAll(intent.getLongArrayExtra(EXTRA_SELECTED_IDS)?.toList().orEmpty())

        adapter = SelectableTaskAdapter { ids ->
            selectedIds.clear()
            selectedIds.addAll(ids)
            updateSelectionUi()
        }

        val rvTasks = findViewById<RecyclerView>(R.id.rvSelectPeople)
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter

        btnClear.setOnClickListener { adapter.clearSelection() }
        btnDone.setOnClickListener {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_SELECTED_IDS, adapter.selectedIds().toLongArray())
            )
            finish()
        }

        viewModel.notes.observe(this) { refreshList() }
        viewModel.loadNotes()
        updateSelectionUi()
    }

    private fun refreshList() {
        val tasks = viewModel.notes.value.orEmpty()
            .sortedWith(
                compareBy<com.example.calendarnotes.data.models.Note> { it.isCompleted }
                    .thenByDescending { it.updatedAt }
                    .thenBy { it.title.lowercase() }
            )
        adapter.submit(tasks, selectedIds)
        tvEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        updateSelectionUi()
    }

    private fun updateSelectionUi() {
        val count = selectedIds.size
        tvCount.text = when (count) {
            0 -> getString(R.string.tasks_selected_none)
            1 -> getString(R.string.tasks_selected_one)
            else -> getString(R.string.tasks_selected_count, count)
        }
        btnClear.isEnabled = count > 0
        btnDone.text = if (count == 0) {
            getString(R.string.done)
        } else {
            getString(R.string.done_with_count, count)
        }
        AppHeader.setTitle(
            this,
            if (count == 0) getString(R.string.select_tasks)
            else getString(R.string.tasks_selected_count, count)
        )
    }

    companion object {
        const val EXTRA_SELECTED_IDS = "extra_selected_ids"

        fun createIntent(context: Context, selectedIds: Collection<Long>): Intent {
            return Intent(context, SelectTasksActivity::class.java)
                .putExtra(EXTRA_SELECTED_IDS, selectedIds.toLongArray())
        }

        fun selectedIdsFromResult(data: Intent?): List<Long> {
            return data?.getLongArrayExtra(EXTRA_SELECTED_IDS)?.toList().orEmpty()
        }
    }
}
