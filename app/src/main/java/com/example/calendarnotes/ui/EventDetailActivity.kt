package com.example.calendarnotes.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.data.models.PersonOrdering
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var tvWhen: TextView
    private lateinit var tvGoogle: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDescription: TextView
    private lateinit var llPeople: LinearLayout
    private lateinit var tvPeopleEmpty: TextView
    private lateinit var llTasks: LinearLayout
    private lateinit var tvTasksEmpty: TextView

    private var eventId: Long = -1L
    private var event: CalendarEvent? = null
    private var linkedPersonIds: List<Long> = emptyList()
    private var linkedTaskIds: List<Long> = emptyList()

    private val timeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    private val selectPeopleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val current = event ?: return@registerForActivityResult
        val ids = PeoplePicker.selectedIdsFromResult(result.data)
        viewModel.setPeopleForEvent(current.id, ids)
        refreshPeople(ids)
    }

    private val selectTasksLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val current = event ?: return@registerForActivityResult
        val ids = TasksPicker.selectedIdsFromResult(result.data)
        viewModel.setNotesForEvent(current.id, ids) {
            refreshTasks(ids)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId < 0) {
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        tvWhen = findViewById(R.id.tvEventDetailWhen)
        tvGoogle = findViewById(R.id.tvEventDetailGoogle)
        tvCategory = findViewById(R.id.tvEventDetailCategory)
        tvDescription = findViewById(R.id.tvEventDetailDescription)
        llPeople = findViewById(R.id.llEventDetailPeople)
        tvPeopleEmpty = findViewById(R.id.tvEventDetailPeopleEmpty)
        llTasks = findViewById(R.id.llEventDetailTasks)
        tvTasksEmpty = findViewById(R.id.tvEventDetailTasksEmpty)

        viewModel.people.observe(this) {
            if (linkedPersonIds.isNotEmpty() || event != null) {
                refreshPeople(linkedPersonIds)
            }
        }
        viewModel.lastEventByPersonId.observe(this) {
            if (linkedPersonIds.isNotEmpty() || event != null) {
                refreshPeople(linkedPersonIds)
            }
        }
        viewModel.notes.observe(this) {
            if (linkedTaskIds.isNotEmpty() || event != null) {
                refreshTasks(linkedTaskIds)
            }
        }
        viewModel.loadNotes()

        AppHeader.setupBackHeader(
            activity = this,
            title = getString(R.string.event_details),
            overflowVisible = false
        )

        findViewById<MaterialButton>(R.id.btnEditEvent).setOnClickListener { openEditor() }
        findViewById<MaterialButton>(R.id.btnDeleteEvent).setOnClickListener { confirmDelete() }
        findViewById<MaterialButton>(R.id.btnSelectEventPeople).setOnClickListener {
            val current = event ?: return@setOnClickListener
            viewModel.getPeopleForEvent(current.id) { linked ->
                val intent = PeoplePicker.createIntent(
                    context = this,
                    people = viewModel.people.value.orEmpty(),
                    initiallySelected = linked.map { it.id }.toSet()
                ) ?: return@getPeopleForEvent
                selectPeopleLauncher.launch(intent)
            }
        }
        findViewById<MaterialButton>(R.id.btnSelectEventTasks).setOnClickListener {
            val current = event ?: return@setOnClickListener
            viewModel.getNotesForEvent(current.id) { linked ->
                val intent = TasksPicker.createIntent(
                    context = this,
                    tasks = viewModel.notes.value.orEmpty(),
                    initiallySelected = linked.map { it.id }.toSet()
                ) ?: return@getNotesForEvent
                selectTasksLauncher.launch(intent)
            }
        }

        viewModel.calendarEvents.observe(this) { events ->
            val updated = events.firstOrNull { it.id == eventId }
            if (updated != null) {
                bindEvent(updated)
            } else if (event != null) {
                finish()
            }
        }

        loadEvent()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.loadCalendarEvents()
            loadEvent()
        }
    }

    private fun loadEvent() {
        viewModel.getCalendarEventById(eventId) { loaded ->
            if (loaded == null) {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                finish()
                return@getCalendarEventById
            }
            bindEvent(loaded)
            viewModel.getPeopleForEvent(loaded.id) { people ->
                refreshPeople(people.map { it.id })
            }
            viewModel.getNotesForEvent(loaded.id) { tasks ->
                refreshTasks(tasks.map { it.id })
            }
        }
    }

    private fun bindEvent(event: CalendarEvent) {
        this.event = event
        AppHeader.setTitle(this, event.title)
        tvWhen.text = if (event.displaysAsAllDay()) {
            val dayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${dayFormat.format(Date(event.startTime))} · ${getString(R.string.event_all_day)}"
        } else {
            "${timeFormat.format(Date(event.startTime))} – ${timeFormat.format(Date(event.endTime))}"
        }
        tvGoogle.visibility = if (event.isFromGoogle) View.VISIBLE else View.GONE

        val categoryName = event.categoryId?.let { catId ->
            viewModel.categories.value.orEmpty().firstOrNull { it.id == catId }?.name
        }
        if (categoryName.isNullOrBlank()) {
            tvCategory.visibility = View.GONE
        } else {
            tvCategory.visibility = View.VISIBLE
            tvCategory.text = categoryName
        }

        if (event.description.isBlank()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = event.description
        }
    }

    private fun refreshPeople(personIds: List<Long>) {
        linkedPersonIds = personIds
        val selected = PersonOrdering.sorted(
            people = viewModel.people.value.orEmpty().filter { it.id in personIds },
            lastEventByPersonId = viewModel.lastEventByPersonId.value.orEmpty()
        )
        EventPeopleList.bind(llPeople, tvPeopleEmpty, selected)
    }

    private fun refreshTasks(taskIds: List<Long>) {
        linkedTaskIds = taskIds
        val selected = viewModel.notes.value.orEmpty()
            .filter { it.id in taskIds }
            .sortedWith(
                compareBy<com.example.calendarnotes.data.models.Note> { it.isCompleted }
                    .thenBy { it.title.lowercase() }
            )
        EventTasksList.bind(llTasks, tvTasksEmpty, selected) { task ->
            startActivity(AddEditNoteActivity.createIntent(this, noteId = task.id))
        }
    }

    private fun openEditor() {
        startActivity(AddEditEventActivity.createIntent(this, eventId = eventId))
    }

    private fun confirmDelete() {
        val current = event ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Delete \"${current.title}\"?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCalendarEvent(current.id)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val EXTRA_EVENT_ID = "extra_event_id"

        fun createIntent(context: Context, eventId: Long): Intent {
            return Intent(context, EventDetailActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
        }
    }
}
