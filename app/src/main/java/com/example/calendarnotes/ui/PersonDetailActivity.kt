package com.example.calendarnotes.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Person
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonDetailActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvStatusStar: TextView
    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvContactHistory: TextView
    private lateinit var tvEventHistory: TextView

    private var personId: Long = -1L
    private var person: Person? = null

    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    private val eventDateFormat = SimpleDateFormat("MMM d h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person_detail)

        personId = intent.getLongExtra(EXTRA_PERSON_ID, -1L)
        if (personId < 0) {
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        bindViews()
        setupHeader()

        findViewById<MaterialButton>(R.id.btnEditPerson).setOnClickListener {
            startActivity(AddEditPersonActivity.createIntent(this, personId))
        }
        findViewById<MaterialButton>(R.id.btnAddHistory).setOnClickListener {
            person?.let { showAddHistoryDialog(it) }
        }
        findViewById<MaterialButton>(R.id.btnAddToEvent).setOnClickListener {
            person?.let { showAddToEventDialog(it) }
        }

        viewModel.people.observe(this) { people ->
            val updated = people.firstOrNull { it.id == personId }
            if (updated != null) {
                bindPerson(updated)
            } else if (person != null) {
                // Person was deleted while this screen is open.
                finish()
            }
        }

        loadPerson()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized && personId > 0) {
            viewModel.loadPeople()
            loadPerson()
        }
    }

    private fun bindViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvStatusStar = findViewById(R.id.tvDetailStatusStar)
        statusDot = findViewById(R.id.detailStatusDot)
        tvStatus = findViewById(R.id.tvDetailStatus)
        tvPhone = findViewById(R.id.tvDetailPhone)
        tvEmail = findViewById(R.id.tvDetailEmail)
        tvAddress = findViewById(R.id.tvDetailAddress)
        tvNotes = findViewById(R.id.tvDetailNotes)
        tvContactHistory = findViewById(R.id.tvContactHistory)
        tvEventHistory = findViewById(R.id.tvEventHistory)
    }

    private fun setupHeader() {
        val btnNav = findViewById<ImageButton>(R.id.btnHeaderNav)
        btnNav.setImageResource(R.drawable.ic_arrow_back)
        btnNav.contentDescription = getString(R.string.navigate_back)
        btnNav.setOnClickListener { finish() }

        findViewById<View>(R.id.ivHeaderChevron).visibility = View.GONE
        findViewById<View>(R.id.btnHeaderTitle).isClickable = false
        findViewById<ImageButton>(R.id.btnHeaderOverflow).setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                menuInflater.inflate(R.menu.menu_person_detail_overflow, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit_person -> {
                            startActivity(AddEditPersonActivity.createIntent(this@PersonDetailActivity, personId))
                            true
                        }
                        R.id.action_delete_person -> {
                            confirmDeletePerson()
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    private fun confirmDeletePerson() {
        val current = person ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Person")
            .setMessage("Delete ${current.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePerson(current.id)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadPerson() {
        viewModel.getPersonById(personId) { loaded ->
            if (loaded == null) {
                Toast.makeText(this, "Person not found", Toast.LENGTH_SHORT).show()
                finish()
                return@getPersonById
            }
            bindPerson(loaded)
            refreshHistory()
        }
    }

    private fun bindPerson(person: Person) {
        this.person = person
        tvHeaderTitle.text = person.name
        tvStatus.text = person.status.label

        val color = try {
            Color.parseColor(person.status.colorHex)
        } catch (_: Exception) {
            Color.GRAY
        }

        if (person.status.showStar) {
            tvStatusStar.visibility = View.VISIBLE
            statusDot.visibility = View.GONE
            tvStatusStar.setTextColor(color)
        } else {
            tvStatusStar.visibility = View.GONE
            statusDot.visibility = View.VISIBLE
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            statusDot.background = dot
        }

        bindOptionalLine(tvPhone, person.phone, prefix = "Phone: ")
        bindOptionalLine(tvEmail, person.email, prefix = "Email: ")
        bindOptionalLine(tvAddress, person.address, prefix = "Address: ")
        if (person.notes.isNotBlank()) {
            tvNotes.visibility = View.VISIBLE
            tvNotes.text = person.notes
        } else {
            tvNotes.visibility = View.GONE
        }
    }

    private fun bindOptionalLine(view: TextView, value: String, prefix: String) {
        if (value.isNotBlank()) {
            view.visibility = View.VISIBLE
            view.text = prefix + value
        } else {
            view.visibility = View.GONE
        }
    }

    private fun refreshHistory() {
        viewModel.getContactHistory(personId) { history ->
            tvContactHistory.text = if (history.isEmpty()) {
                getString(R.string.no_contact_history)
            } else {
                history.joinToString("\n") {
                    "• ${dateFormat.format(Date(it.timestamp))}: ${it.summary}"
                }
            }
        }
        viewModel.getEventsForPerson(personId) { events ->
            tvEventHistory.text = if (events.isEmpty()) {
                getString(R.string.no_event_history)
            } else {
                events.joinToString("\n") {
                    "• ${dateFormat.format(Date(it.startTime))}: ${it.title}"
                }
            }
        }
    }

    private fun showAddHistoryDialog(person: Person) {
        val input = EditText(this).apply {
            hint = "What happened?"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_history)
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val summary = input.text.toString().trim()
                if (summary.isNotBlank()) {
                    viewModel.addContactHistory(person.id, summary) {
                        refreshHistory()
                        Toast.makeText(this, "History added", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddToEventDialog(person: Person) {
        val events = viewModel.calendarEvents.value.orEmpty()
            .sortedByDescending { it.startTime }
        if (events.isEmpty()) {
            Toast.makeText(this, "No events yet. Create one on Calendar first.", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = events.map {
            "${it.title} (${eventDateFormat.format(Date(it.startTime))})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.add_to_event)
            .setItems(labels) { _, which ->
                viewModel.addPersonToEvent(events[which].id, person.id) {
                    refreshHistory()
                    Toast.makeText(this, "Added to ${events[which].title}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val EXTRA_PERSON_ID = "extra_person_id"

        fun createIntent(context: Context, personId: Long): Intent {
            return Intent(context, PersonDetailActivity::class.java).putExtra(EXTRA_PERSON_ID, personId)
        }
    }
}
