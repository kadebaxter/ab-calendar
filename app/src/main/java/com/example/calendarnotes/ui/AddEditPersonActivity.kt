package com.example.calendarnotes.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Person
import com.example.calendarnotes.data.models.PersonStatus
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddEditPersonActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var spinnerStatus: Spinner

    private var personId: Long = -1L
    private var existing: Person? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person_form)

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        personId = intent.getLongExtra(EXTRA_PERSON_ID, -1L)

        etName = findViewById(R.id.etPersonName)
        etPhone = findViewById(R.id.etPersonPhone)
        etEmail = findViewById(R.id.etPersonEmail)
        etAddress = findViewById(R.id.etPersonAddress)
        etNotes = findViewById(R.id.etPersonNotes)
        spinnerStatus = findViewById(R.id.spinnerPersonStatus)

        val statuses = PersonStatus.entries
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses.map { it.label })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter

        AppHeader.setupBackHeader(
            this,
            title = getString(if (personId > 0) R.string.edit_person else R.string.add_person)
        )
        findViewById<MaterialButton>(R.id.btnSavePerson).setOnClickListener { save(statuses) }

        if (personId > 0) {
            viewModel.getPersonById(personId) { person ->
                if (person == null) {
                    Toast.makeText(this, "Person not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@getPersonById
                }
                existing = person
                etName.setText(person.name)
                etPhone.setText(person.phone)
                etEmail.setText(person.email)
                etAddress.setText(person.address)
                etNotes.setText(person.notes)
                spinnerStatus.setSelection(statuses.indexOf(person.status).coerceAtLeast(0))
                AppHeader.setTitle(this, person.name)
            }
        }
    }

    private fun save(statuses: List<PersonStatus>) {
        val name = etName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        val person = Person(
            id = existing?.id ?: 0,
            name = name,
            phone = etPhone.text?.toString().orEmpty(),
            email = etEmail.text?.toString().orEmpty(),
            address = etAddress.text?.toString().orEmpty(),
            notes = etNotes.text?.toString().orEmpty(),
            status = statuses[spinnerStatus.selectedItemPosition],
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        if (existing == null) {
            viewModel.addPerson(person)
        } else {
            viewModel.updatePerson(person)
        }
        finish()
    }

    companion object {
        private const val EXTRA_PERSON_ID = "extra_person_id"

        fun createIntent(context: Context, personId: Long = -1L): Intent {
            return Intent(context, AddEditPersonActivity::class.java)
                .putExtra(EXTRA_PERSON_ID, personId)
        }
    }
}
