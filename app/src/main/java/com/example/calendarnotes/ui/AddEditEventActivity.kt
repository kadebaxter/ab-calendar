package com.example.calendarnotes.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.data.models.PersonOrdering
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditEventActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var tilTitle: TextInputLayout
    private lateinit var btnStartDate: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnEndTime: Button
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSelectPeople: Button
    private lateinit var llSelectedPeople: LinearLayout
    private lateinit var tvSelectedPeopleEmpty: TextView
    private lateinit var eventTasksSection: LinearLayout
    private lateinit var btnSelectTasks: Button
    private lateinit var llSelectedTasks: LinearLayout
    private lateinit var tvSelectedTasksEmpty: TextView
    private lateinit var toggleCreateMode: MaterialButtonToggleGroup

    private var eventId: Long = -1L
    private var noteId: Long = -1L
    private var existingEvent: CalendarEvent? = null
    private var startTime: Calendar = Calendar.getInstance()
    private var endTime: Calendar = Calendar.getInstance()
    private val selectedPersonIds = mutableListOf<Long>()
    private val selectedTaskIds = mutableListOf<Long>()
    private var createAsTask = false

    private val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())

    private val selectPeopleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        selectedPersonIds.clear()
        selectedPersonIds.addAll(PeoplePicker.selectedIdsFromResult(result.data))
        refreshSelectedPeople()
    }

    private val selectTasksLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        selectedTaskIds.clear()
        selectedTaskIds.addAll(TasksPicker.selectedIdsFromResult(result.data))
        refreshSelectedTasks()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_form)

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)

        bindViews()
        val isEdit = eventId > 0
        val isSchedulingTask = !isEdit && noteId > 0
        AppHeader.setupBackHeader(
            this,
            title = getString(
                when {
                    isEdit -> R.string.edit_event
                    isSchedulingTask -> R.string.schedule_task
                    else -> R.string.add_event
                }
            )
        )

        setupCreateModeToggle(isEdit = isEdit, isSchedulingTask = isSchedulingTask)
        findViewById<MaterialButton>(R.id.btnSaveEvent).setOnClickListener { save() }

        var pendingCategoryId: Long? = null
        viewModel.categories.observe(this) {
            setupCategorySpinner(pendingCategoryId ?: existingEvent?.categoryId)
        }
        viewModel.people.observe(this) { refreshSelectedPeople() }
        viewModel.lastEventByPersonId.observe(this) { refreshSelectedPeople() }
        viewModel.notes.observe(this) { refreshSelectedTasks() }
        viewModel.loadNotes()

        if (isEdit) {
            loadExistingEvent { categoryId ->
                pendingCategoryId = categoryId
                setupCategorySpinner(categoryId)
            }
        } else {
            initNewEventDefaults()
            if (isSchedulingTask) {
                selectedTaskIds.clear()
                selectedTaskIds.add(noteId)
                eventTasksSection.visibility = View.GONE
                prefillFromNote(noteId) { categoryId ->
                    pendingCategoryId = categoryId
                    setupCategorySpinner(categoryId)
                }
            } else {
                setupCategorySpinner(null)
                applyCreateModeUi()
            }
            refreshSelectedPeople()
            refreshSelectedTasks()
            bindDateTimeButtons()
            setupPickers()
        }
    }

    private fun bindViews() {
        etTitle = findViewById(R.id.etEventTitle)
        etDescription = findViewById(R.id.etEventDescription)
        tilTitle = findViewById(R.id.tilEventTitle)
        btnStartDate = findViewById(R.id.btnStartDate)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndDate = findViewById(R.id.btnEndDate)
        btnEndTime = findViewById(R.id.btnEndTime)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnSelectPeople = findViewById(R.id.btnSelectPeople)
        llSelectedPeople = findViewById(R.id.llSelectedPeople)
        tvSelectedPeopleEmpty = findViewById(R.id.tvSelectedPeopleEmpty)
        eventTasksSection = findViewById(R.id.eventTasksSection)
        btnSelectTasks = findViewById(R.id.btnSelectTasks)
        llSelectedTasks = findViewById(R.id.llSelectedTasks)
        tvSelectedTasksEmpty = findViewById(R.id.tvSelectedTasksEmpty)
        toggleCreateMode = findViewById(R.id.toggleCreateMode)
    }

    private fun setupCreateModeToggle(isEdit: Boolean, isSchedulingTask: Boolean) {
        if (isEdit || isSchedulingTask) {
            toggleCreateMode.visibility = View.GONE
            createAsTask = false
            return
        }
        toggleCreateMode.visibility = View.VISIBLE
        toggleCreateMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            createAsTask = checkedId == R.id.btnModeTask
            applyCreateModeUi()
        }
        createAsTask = toggleCreateMode.checkedButtonId == R.id.btnModeTask
        applyCreateModeUi()
    }

    private fun applyCreateModeUi() {
        if (eventId > 0 || noteId > 0) return
        if (createAsTask) {
            AppHeader.setTitle(this, getString(R.string.add_task))
            tilTitle.hint = getString(R.string.task_title)
            eventTasksSection.visibility = View.GONE
        } else {
            AppHeader.setTitle(this, getString(R.string.add_event))
            tilTitle.hint = getString(R.string.event_title)
            eventTasksSection.visibility = View.VISIBLE
        }
    }

    private fun initNewEventDefaults() {
        val dayMillis = intent.getLongExtra(EXTRA_DAY_MILLIS, 0L)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        startTime = Calendar.getInstance()
        if (dayMillis > 0L) {
            startTime.timeInMillis = dayMillis
        }
        if (hour >= 0) {
            startTime.set(Calendar.HOUR_OF_DAY, hour)
            startTime.set(Calendar.MINUTE, 0)
            startTime.set(Calendar.SECOND, 0)
            startTime.set(Calendar.MILLISECOND, 0)
        }
        endTime = startTime.clone() as Calendar
        endTime.add(Calendar.HOUR_OF_DAY, 1)
    }

    private fun loadExistingEvent(onCategory: (Long?) -> Unit) {
        viewModel.getCalendarEventById(eventId) { event ->
            if (event == null) {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                finish()
                return@getCalendarEventById
            }
            existingEvent = event
            etTitle.setText(event.title)
            etDescription.setText(event.description)
            startTime.timeInMillis = event.startTime
            endTime.timeInMillis = event.endTime
            noteId = event.noteId ?: -1L
            onCategory(event.categoryId)
            bindDateTimeButtons()
            setupPickers()
            eventTasksSection.visibility = View.VISIBLE
            if (event.isFromGoogle) {
                applyGoogleReadOnlyUi()
            }
            viewModel.getPeopleForEvent(event.id) { people ->
                selectedPersonIds.clear()
                selectedPersonIds.addAll(people.map { it.id })
                refreshSelectedPeople()
            }
            viewModel.getNotesForEvent(event.id) { tasks ->
                selectedTaskIds.clear()
                selectedTaskIds.addAll(tasks.map { it.id })
                refreshSelectedTasks()
            }
        }
    }

    private fun applyGoogleReadOnlyUi() {
        etTitle.isEnabled = false
        etDescription.isEnabled = false
        btnStartDate.isEnabled = false
        btnStartTime.isEnabled = false
        btnEndDate.isEnabled = false
        btnEndTime.isEnabled = false
        if (existingEvent?.displaysAsAllDay() == true) {
            btnStartTime.text = getString(R.string.event_all_day)
            btnEndTime.text = getString(R.string.event_all_day)
        }
        Toast.makeText(this, R.string.google_event_readonly_hint, Toast.LENGTH_LONG).show()
    }

    private fun prefillFromNote(id: Long, onCategory: (Long?) -> Unit) {
        viewModel.getNoteById(id) { note ->
            if (note == null) return@getNoteById
            etTitle.setText(note.title)
            etDescription.setText(note.content)
            onCategory(note.categoryId)
        }
    }

    private fun setupCategorySpinner(selectedCategoryId: Long?) {
        val categories = viewModel.categories.value.orEmpty()
        val names = listOf("None") + categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        selectedCategoryId?.let { catId ->
            val index = categories.indexOfFirst { it.id == catId }
            if (index >= 0) spinnerCategory.setSelection(index + 1)
        }
    }

    private fun bindDateTimeButtons() {
        btnStartDate.text = dateFormat.format(startTime.time)
        btnStartTime.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            startTime.get(Calendar.HOUR_OF_DAY),
            startTime.get(Calendar.MINUTE)
        )
        btnEndDate.text = dateFormat.format(endTime.time)
        btnEndTime.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            endTime.get(Calendar.HOUR_OF_DAY),
            endTime.get(Calendar.MINUTE)
        )
    }

    private fun setupPickers() {
        btnStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    startTime.set(year, month, day)
                    btnStartDate.text = dateFormat.format(startTime.time)
                },
                startTime.get(Calendar.YEAR),
                startTime.get(Calendar.MONTH),
                startTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        btnStartTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    startTime.set(Calendar.HOUR_OF_DAY, hour)
                    startTime.set(Calendar.MINUTE, minute)
                    btnStartTime.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                },
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE),
                false
            ).show()
        }
        btnEndDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    endTime.set(year, month, day)
                    btnEndDate.text = dateFormat.format(endTime.time)
                },
                endTime.get(Calendar.YEAR),
                endTime.get(Calendar.MONTH),
                endTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        btnEndTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    endTime.set(Calendar.HOUR_OF_DAY, hour)
                    endTime.set(Calendar.MINUTE, minute)
                    btnEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                },
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE),
                false
            ).show()
        }
        btnSelectPeople.setOnClickListener {
            val intent = PeoplePicker.createIntent(
                context = this,
                people = viewModel.people.value.orEmpty(),
                initiallySelected = selectedPersonIds.toSet()
            ) ?: return@setOnClickListener
            selectPeopleLauncher.launch(intent)
        }
        btnSelectTasks.setOnClickListener {
            val intent = TasksPicker.createIntent(
                context = this,
                tasks = viewModel.notes.value.orEmpty(),
                initiallySelected = selectedTaskIds.toSet()
            ) ?: return@setOnClickListener
            selectTasksLauncher.launch(intent)
        }
    }

    private fun refreshSelectedPeople() {
        if (!::llSelectedPeople.isInitialized) return
        btnSelectPeople.text = getString(R.string.select_people_short)
        val selected = PersonOrdering.sorted(
            people = viewModel.people.value.orEmpty().filter { it.id in selectedPersonIds },
            lastEventByPersonId = viewModel.lastEventByPersonId.value.orEmpty()
        )
        EventPeopleList.bind(llSelectedPeople, tvSelectedPeopleEmpty, selected)
    }

    private fun refreshSelectedTasks() {
        if (!::llSelectedTasks.isInitialized) return
        btnSelectTasks.text = getString(R.string.select_tasks_short)
        val selected = viewModel.notes.value.orEmpty()
            .filter { it.id in selectedTaskIds }
            .sortedWith(
                compareBy<com.example.calendarnotes.data.models.Note> { it.isCompleted }
                    .thenBy { it.title.lowercase() }
            )
        EventTasksList.bind(llSelectedTasks, tvSelectedTasksEmpty, selected)
    }

    private fun selectedCategoryId(): Long? {
        val categories = viewModel.categories.value.orEmpty()
        val position = spinnerCategory.selectedItemPosition
        return if (position > 0 && categories.isNotEmpty()) {
            categories[position - 1].id
        } else {
            null
        }
    }

    private fun save() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }
        val description = etDescription.text?.toString().orEmpty()
        val categoryId = selectedCategoryId()

        if (eventId > 0) {
            val existing = existingEvent ?: return
            val updated = if (existing.isFromGoogle) {
                existing.copy(categoryId = categoryId)
            } else {
                existing.copy(
                    title = title,
                    description = description,
                    startTime = startTime.timeInMillis,
                    endTime = endTime.timeInMillis,
                    categoryId = categoryId
                )
            }
            viewModel.updateCalendarEvent(
                updated,
                personIds = selectedPersonIds.toList(),
                noteIds = selectedTaskIds.toList()
            ) {
                finish()
            }
            return
        }

        if (createAsTask && noteId <= 0) {
            viewModel.addNoteReturningId(categoryId, title, description) { newTaskId ->
                if (newTaskId <= 0) {
                    Toast.makeText(this, "Could not create task", Toast.LENGTH_SHORT).show()
                    return@addNoteReturningId
                }
                viewModel.addCalendarEvent(
                    title = title,
                    description = description,
                    startTime = startTime.timeInMillis,
                    endTime = endTime.timeInMillis,
                    categoryId = categoryId,
                    personIds = selectedPersonIds.toList(),
                    noteIds = listOf(newTaskId)
                ) {
                    finish()
                }
            }
            return
        }

        val linkedFromTask = noteId.takeIf { it > 0 }
        val taskIds = when {
            linkedFromTask != null -> listOf(linkedFromTask)
            else -> selectedTaskIds.toList()
        }
        viewModel.addCalendarEvent(
            title = title,
            description = description,
            startTime = startTime.timeInMillis,
            endTime = endTime.timeInMillis,
            categoryId = categoryId,
            personIds = selectedPersonIds.toList(),
            noteIds = taskIds
        ) {
            finish()
        }
    }

    companion object {
        private const val EXTRA_EVENT_ID = "extra_event_id"
        private const val EXTRA_NOTE_ID = "extra_note_id"
        private const val EXTRA_DAY_MILLIS = "extra_day_millis"
        private const val EXTRA_HOUR = "extra_hour"

        fun createIntent(
            context: Context,
            eventId: Long = -1L,
            noteId: Long = -1L,
            dayMillis: Long = 0L,
            hour: Int = -1
        ): Intent {
            return Intent(context, AddEditEventActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
                .putExtra(EXTRA_NOTE_ID, noteId)
                .putExtra(EXTRA_DAY_MILLIS, dayMillis)
                .putExtra(EXTRA_HOUR, hour)
        }
    }
}
