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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Note
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditNoteActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var switchAddToCalendar: MaterialSwitch
    private lateinit var calendarScheduleSection: LinearLayout
    private lateinit var btnStartDate: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnEndTime: Button
    private lateinit var btnScheduleTask: MaterialButton
    private lateinit var btnSave: MaterialButton

    private var noteId: Long = -1L
    private var existing: Note? = null
    private var startTime: Calendar = Calendar.getInstance()
    private var endTime: Calendar = Calendar.getInstance()
    private var saving = false

    private val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_form)

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)

        bindViews()
        initScheduleDefaults()
        bindDateTimeButtons()
        setupPickers()

        AppHeader.setupBackHeader(
            this,
            title = getString(if (noteId > 0) R.string.edit_task else R.string.add_task)
        )
        btnSave.setOnClickListener { save() }
        btnScheduleTask.setOnClickListener {
            if (noteId <= 0) return@setOnClickListener
            startActivity(AddEditEventActivity.createIntent(this, noteId = noteId))
        }
        switchAddToCalendar.setOnCheckedChangeListener { _, checked ->
            calendarScheduleSection.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // Inline scheduling is for creating a new task from the Tasks tab.
        val isNew = noteId <= 0
        switchAddToCalendar.visibility = if (isNew) View.VISIBLE else View.GONE
        calendarScheduleSection.visibility = View.GONE

        viewModel.categories.observe(this) { categories ->
            val names = listOf("None") + categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
            existing?.categoryId?.let { catId ->
                val index = categories.indexOfFirst { it.id == catId }
                if (index >= 0) spinnerCategory.setSelection(index + 1)
            }
        }

        if (!isNew) {
            viewModel.getNoteById(noteId) { note ->
                if (note == null) {
                    Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@getNoteById
                }
                existing = note
                etTitle.setText(note.title)
                etContent.setText(note.content)
                AppHeader.setTitle(this, note.title.ifBlank { getString(R.string.edit_task) })
                btnScheduleTask.visibility = View.VISIBLE
                val categories = viewModel.categories.value.orEmpty()
                note.categoryId?.let { catId ->
                    val index = categories.indexOfFirst { it.id == catId }
                    if (index >= 0) spinnerCategory.setSelection(index + 1)
                }
            }
        }
    }

    private fun bindViews() {
        etTitle = findViewById(R.id.etNoteTitle)
        etContent = findViewById(R.id.etNoteContent)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        switchAddToCalendar = findViewById(R.id.switchAddToCalendar)
        calendarScheduleSection = findViewById(R.id.calendarScheduleSection)
        btnStartDate = findViewById(R.id.btnTaskStartDate)
        btnStartTime = findViewById(R.id.btnTaskStartTime)
        btnEndDate = findViewById(R.id.btnTaskEndDate)
        btnEndTime = findViewById(R.id.btnTaskEndTime)
        btnScheduleTask = findViewById(R.id.btnScheduleTask)
        btnSave = findViewById(R.id.btnSaveNote)
    }

    private fun initScheduleDefaults() {
        startTime = Calendar.getInstance()
        startTime.set(Calendar.SECOND, 0)
        startTime.set(Calendar.MILLISECOND, 0)
        // Round up to the next hour for a sensible default slot.
        if (startTime.get(Calendar.MINUTE) > 0) {
            startTime.add(Calendar.HOUR_OF_DAY, 1)
            startTime.set(Calendar.MINUTE, 0)
        }
        endTime = startTime.clone() as Calendar
        endTime.add(Calendar.HOUR_OF_DAY, 1)
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
    }

    private fun selectedCategoryId(): Long? {
        val categories = viewModel.categories.value.orEmpty()
        return if (spinnerCategory.selectedItemPosition == 0) {
            null
        } else {
            categories.getOrNull(spinnerCategory.selectedItemPosition - 1)?.id
        }
    }

    private fun save() {
        if (saving) return
        val title = etTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }
        val content = etContent.text?.toString().orEmpty()
        val categoryId = selectedCategoryId()
        val addToCalendar = existing == null && switchAddToCalendar.isChecked

        if (existing == null) {
            saving = true
            btnSave.isEnabled = false
            viewModel.addNoteReturningId(categoryId, title, content) { newId ->
                if (newId <= 0) {
                    saving = false
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Could not create task", Toast.LENGTH_SHORT).show()
                    return@addNoteReturningId
                }
                if (!addToCalendar) {
                    finish()
                    return@addNoteReturningId
                }
                viewModel.addCalendarEvent(
                    title = title,
                    description = content,
                    startTime = startTime.timeInMillis,
                    endTime = endTime.timeInMillis,
                    categoryId = categoryId,
                    noteIds = listOf(newId)
                ) {
                    finish()
                }
            }
        } else {
            viewModel.updateNote(
                existing!!.copy(
                    title = title,
                    content = content,
                    categoryId = categoryId
                )
            )
            finish()
        }
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"

        fun createIntent(context: Context, noteId: Long = -1L): Intent {
            return Intent(context, AddEditNoteActivity::class.java)
                .putExtra(EXTRA_NOTE_ID, noteId)
        }
    }
}
