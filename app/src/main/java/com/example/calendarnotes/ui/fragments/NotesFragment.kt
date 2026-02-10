package com.example.calendarnotes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.ui.adapters.NoteAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.calendarnotes.data.models.Note

class NotesFragment : Fragment() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var rvNotes: RecyclerView
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CalendarNotesViewModel::class.java]

        rvNotes = view.findViewById(R.id.rvNotes)
        val fabAddNote: FloatingActionButton = view.findViewById(R.id.fabAddNote)

        setupRecyclerView()

        fabAddNote.setOnClickListener {
            showAddNoteDialog(null)
        }

        observeViewModel()
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            emptyList(),
            onNoteClick = { note ->
                showAddNoteDialog(note)
            },
            onDelete = { note ->
                viewModel.deleteNote(note.id)
            }
        )
        rvNotes.layoutManager = LinearLayoutManager(requireContext())
        rvNotes.adapter = noteAdapter
    }

    private fun observeViewModel() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.updateNotes(notes)
        }
    }

    private fun showAddNoteDialog(existingNote: Note?) {
        val categories = viewModel.categories.value ?: emptyList()
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_note, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNoteTitle)
        val etContent = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNoteContent)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        // Setup category spinner
        val categoryNames = listOf("None") + categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Pre-fill if editing
        existingNote?.let { note ->
            etTitle.setText(note.title)
            etContent.setText(note.content)
            note.categoryId?.let { catId ->
                val index = categories.indexOfFirst { it.id == catId }
                if (index >= 0) {
                    spinnerCategory.setSelection(index + 1)
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingNote == null) "Add Note" else "Edit Note")
            .setView(dialogView)
            .setPositiveButton(if (existingNote == null) "Add" else "Save") { _, _ ->
                val title = etTitle.text.toString()
                val content = etContent.text.toString()
                val categoryId = if (spinnerCategory.selectedItemPosition == 0) null 
                                else categories[spinnerCategory.selectedItemPosition - 1].id
                
                if (title.isNotBlank()) {
                    if (existingNote == null) {
                        viewModel.addNote(categoryId, title, content)
                    } else {
                        viewModel.updateNote(existingNote.copy(title = title, content = content, categoryId = categoryId))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
