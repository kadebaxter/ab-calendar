package com.example.calendarnotes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Category
import com.example.calendarnotes.data.models.Note
import com.example.calendarnotes.ui.AddEditEventActivity
import com.example.calendarnotes.ui.AddEditNoteActivity
import com.example.calendarnotes.ui.HasOverflowMenu
import com.example.calendarnotes.ui.adapters.CategoryAdapter
import com.example.calendarnotes.ui.adapters.NoteAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

enum class NotesSort { NEWEST, OLDEST, CATEGORY }

class NotesFragment : Fragment(), HasOverflowMenu {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var rvNotes: RecyclerView
    private lateinit var rvCategories: RecyclerView
    private lateinit var toggleNotesView: MaterialButtonToggleGroup
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var sortMode: NotesSort = NotesSort.NEWEST
    private var hideCompleted: Boolean = false
    private var filterCategoryId: Long? = null

    private val categoryColors = listOf(
        "Blue" to "#2196F3",
        "Red" to "#F44336",
        "Green" to "#4CAF50",
        "Orange" to "#FF9800",
        "Purple" to "#9C27B0",
        "Teal" to "#009688"
    )

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
        rvCategories = view.findViewById(R.id.rvCategories)
        toggleNotesView = view.findViewById(R.id.toggleNotesView)
        fabAdd = view.findViewById(R.id.fabAdd)

        setupLists()
        setupToggle()
        setupFab()
        observeViewModel()
    }

    override fun showOverflowMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_notes_overflow, menu)
            when (sortMode) {
                NotesSort.NEWEST -> menu.findItem(R.id.action_sort_newest)?.isChecked = true
                NotesSort.OLDEST -> menu.findItem(R.id.action_sort_oldest)?.isChecked = true
                NotesSort.CATEGORY -> menu.findItem(R.id.action_sort_category)?.isChecked = true
            }
            val toggleItem = menu.findItem(R.id.action_toggle_completed)
            toggleItem?.isChecked = hideCompleted
            toggleItem?.title = getString(
                if (hideCompleted) R.string.menu_show_completed else R.string.menu_hide_completed
            )
            menu.findItem(R.id.action_clear_category_filter)?.isVisible = filterCategoryId != null
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_sort_newest -> {
                        sortMode = NotesSort.NEWEST
                        refreshNotes()
                        true
                    }
                    R.id.action_sort_oldest -> {
                        sortMode = NotesSort.OLDEST
                        refreshNotes()
                        true
                    }
                    R.id.action_sort_category -> {
                        sortMode = NotesSort.CATEGORY
                        refreshNotes()
                        true
                    }
                    R.id.action_toggle_completed -> {
                        hideCompleted = !hideCompleted
                        refreshNotes()
                        true
                    }
                    R.id.action_filter_category -> {
                        showCategoryFilterDialog()
                        true
                    }
                    R.id.action_clear_category_filter -> {
                        filterCategoryId = null
                        refreshNotes()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showCategoryFilterDialog() {
        val categories = viewModel.categories.value.orEmpty()
        val labels = mutableListOf(getString(R.string.all_categories))
        labels.addAll(categories.map { it.name })
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_filter_category)
            .setItems(labels.toTypedArray()) { _, which ->
                filterCategoryId = if (which == 0) null else categories[which - 1].id
                refreshNotes()
            }
            .show()
    }

    private fun setupLists() {
        noteAdapter = NoteAdapter(
            onNoteClick = { note ->
                startActivity(AddEditNoteActivity.createIntent(requireContext(), note.id))
            },
            onToggleComplete = { note -> viewModel.toggleNoteCompletion(note) },
            onSchedule = { note ->
                startActivity(
                    AddEditEventActivity.createIntent(
                        context = requireContext(),
                        noteId = note.id
                    )
                )
            },
            onDelete = { note -> viewModel.deleteNote(note.id) }
        )
        rvNotes.layoutManager = LinearLayoutManager(requireContext())
        rvNotes.adapter = noteAdapter

        categoryAdapter = CategoryAdapter(
            onEdit = { category -> showCategoryDialog(category) },
            onDelete = { category ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Category")
                    .setMessage("Delete \"${category.name}\"? Tasks keep their content but lose this category.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteCategory(category.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        rvCategories.adapter = categoryAdapter
    }

    private fun setupToggle() {
        toggleNotesView.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val showingNotes = checkedId == R.id.btnViewNotes
            rvNotes.visibility = if (showingNotes) View.VISIBLE else View.GONE
            rvCategories.visibility = if (showingNotes) View.GONE else View.VISIBLE
        }
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            if (toggleNotesView.checkedButtonId == R.id.btnViewCategories) {
                showCategoryDialog(null)
            } else {
                startActivity(AddEditNoteActivity.createIntent(requireContext()))
            }
        }
    }

    private fun observeViewModel() {
        viewModel.notes.observe(viewLifecycleOwner) { refreshNotes() }
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.updateCategories(categories)
            refreshNotes()
        }
    }

    private fun refreshNotes() {
        val categories = viewModel.categories.value.orEmpty()
        var notes = viewModel.notes.value.orEmpty()
        if (hideCompleted) {
            notes = notes.filter { !it.isCompleted }
        }
        filterCategoryId?.let { catId ->
            notes = notes.filter { it.categoryId == catId }
        }
        notes = when (sortMode) {
            NotesSort.NEWEST -> notes.sortedByDescending { it.updatedAt }
            NotesSort.OLDEST -> notes.sortedBy { it.updatedAt }
            NotesSort.CATEGORY -> notes.sortedWith(
                compareBy<Note> { it.categoryId ?: Long.MAX_VALUE }
                    .thenByDescending { it.updatedAt }
            )
        }
        noteAdapter.updateNotes(notes, categories)
    }

    private fun showCategoryDialog(existingCategory: Category?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCategoryName)
        val spinnerColor = dialogView.findViewById<Spinner>(R.id.spinnerColor)

        val colorNames = categoryColors.map { it.first }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColor.adapter = adapter

        existingCategory?.let { category ->
            etName.setText(category.name)
            val colorIndex = categoryColors.indexOfFirst { it.second.equals(category.color, ignoreCase = true) }
            if (colorIndex >= 0) spinnerColor.setSelection(colorIndex)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingCategory == null) "Add Category" else "Edit Category")
            .setView(dialogView)
            .setPositiveButton(if (existingCategory == null) "Add" else "Save") { _, _ ->
                val name = etName.text.toString()
                val color = categoryColors[spinnerColor.selectedItemPosition].second
                if (name.isNotBlank()) {
                    if (existingCategory == null) {
                        viewModel.addCategory(name, color)
                    } else {
                        viewModel.updateCategory(existingCategory.copy(name = name, color = color))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
