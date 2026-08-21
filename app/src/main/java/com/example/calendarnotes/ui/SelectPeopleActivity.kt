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
import com.example.calendarnotes.data.models.PersonOrdering
import com.example.calendarnotes.ui.adapters.SelectablePersonAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton

class SelectPeopleActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var adapter: SelectablePersonAdapter
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

        AppHeader.setupBackHeader(this, getString(R.string.select_people))

        selectedIds.clear()
        selectedIds.addAll(intent.getLongArrayExtra(EXTRA_SELECTED_IDS)?.toList().orEmpty())

        adapter = SelectablePersonAdapter { ids ->
            selectedIds.clear()
            selectedIds.addAll(ids)
            updateSelectionUi()
        }

        val rvPeople = findViewById<RecyclerView>(R.id.rvSelectPeople)
        rvPeople.layoutManager = LinearLayoutManager(this)
        rvPeople.adapter = adapter

        btnClear.setOnClickListener { adapter.clearSelection() }
        btnDone.setOnClickListener {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_SELECTED_IDS, adapter.selectedIds().toLongArray())
            )
            finish()
        }

        viewModel.people.observe(this) { refreshList() }
        viewModel.lastEventByPersonId.observe(this) { refreshList() }
        updateSelectionUi()
    }

    private fun refreshList() {
        val sorted = PersonOrdering.sorted(
            people = viewModel.people.value.orEmpty(),
            lastEventByPersonId = viewModel.lastEventByPersonId.value.orEmpty()
        )
        adapter.submit(sorted, selectedIds)
        tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        updateSelectionUi()
    }

    private fun updateSelectionUi() {
        val count = selectedIds.size
        tvCount.text = when (count) {
            0 -> getString(R.string.people_selected_none)
            1 -> getString(R.string.people_selected_one)
            else -> getString(R.string.people_selected_count, count)
        }
        btnClear.isEnabled = count > 0
        btnDone.text = if (count == 0) {
            getString(R.string.done)
        } else {
            getString(R.string.done_with_count, count)
        }
        AppHeader.setTitle(
            this,
            if (count == 0) getString(R.string.select_people)
            else getString(R.string.people_selected_count, count)
        )
    }

    companion object {
        const val EXTRA_SELECTED_IDS = "extra_selected_ids"

        fun createIntent(context: Context, selectedIds: Collection<Long>): Intent {
            return Intent(context, SelectPeopleActivity::class.java)
                .putExtra(EXTRA_SELECTED_IDS, selectedIds.toLongArray())
        }

        fun selectedIdsFromResult(data: Intent?): List<Long> {
            return data?.getLongArrayExtra(EXTRA_SELECTED_IDS)?.toList().orEmpty()
        }
    }
}
