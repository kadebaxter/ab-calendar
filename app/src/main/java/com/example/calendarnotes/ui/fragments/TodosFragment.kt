package com.example.calendarnotes.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.calendarnotes.data.models.TodoItem
import com.example.calendarnotes.ui.adapters.TodoAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class TodosFragment : Fragment() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var rvTodos: RecyclerView
    private lateinit var todoAdapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_todos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CalendarNotesViewModel::class.java]

        rvTodos = view.findViewById(R.id.rvTodos)
        val fabAddTodo: FloatingActionButton = view.findViewById(R.id.fabAddTodo)

        setupRecyclerView()

        fabAddTodo.setOnClickListener {
            showAddTodoDialog()
        }

        observeViewModel()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            emptyList(),
            emptyMap(),
            onToggleComplete = { todo ->
                viewModel.toggleTodoCompletion(todo)
            },
            onSchedule = { todo ->
                showScheduleTodoDialog(todo)
            },
            onDelete = { todo ->
                viewModel.deleteTodoItem(todo.id)
            }
        )
        rvTodos.layoutManager = LinearLayoutManager(requireContext())
        rvTodos.adapter = todoAdapter
    }

    private fun observeViewModel() {
        viewModel.todoItems.observe(viewLifecycleOwner) { todos ->
            val categoryNames = viewModel.categories.value?.associate { it.id to it.name } ?: emptyMap()
            todoAdapter.updateTodos(todos, categoryNames)
        }
        
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val todos = viewModel.todoItems.value ?: emptyList()
            val categoryNames = categories.associate { it.id to it.name }
            todoAdapter.updateTodos(todos, categoryNames)
        }
    }

    private fun showAddTodoDialog() {
        val categories = viewModel.categories.value ?: emptyList()
        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "Please create a category first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_todo, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTodoTitle)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTodoDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val rgPriority = dialogView.findViewById<RadioGroup>(R.id.rgPriority)

        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add Todo")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                val categoryId = categories[spinnerCategory.selectedItemPosition].id
                val priority = when (rgPriority.checkedRadioButtonId) {
                    R.id.rbHigh -> 2
                    R.id.rbMedium -> 1
                    else -> 0
                }
                
                if (title.isNotBlank()) {
                    viewModel.addTodoItem(categoryId, null, title, description, priority)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showScheduleTodoDialog(todo: TodoItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEventTitle)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEventDescription)
        val btnStartDate = dialogView.findViewById<android.widget.Button>(R.id.btnStartDate)
        val btnStartTime = dialogView.findViewById<android.widget.Button>(R.id.btnStartTime)
        val btnEndDate = dialogView.findViewById<android.widget.Button>(R.id.btnEndDate)
        val btnEndTime = dialogView.findViewById<android.widget.Button>(R.id.btnEndTime)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        // Setup category spinner
        val categories = viewModel.categories.value ?: emptyList()
        val categoryNames = categories.map { it.name }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        
        // Pre-select the todo's category
        val todoCategoryIndex = categories.indexOfFirst { it.id == todo.categoryId }
        if (todoCategoryIndex >= 0) {
            spinnerCategory.setSelection(todoCategoryIndex)
        }
        
        // Disable category selection (can't change it when scheduling)
        spinnerCategory.isEnabled = false

        // Pre-fill with todo info
        etTitle.setText(todo.title)
        etDescription.setText(todo.description)

        // Use current date and time as default
        var startTime = Calendar.getInstance()
        
        // Set end time to 1 hour after start time
        var endTime = startTime.clone() as Calendar
        endTime.add(Calendar.HOUR_OF_DAY, 1)

        btnStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    startTime.set(year, month, day)
                    btnStartDate.text = "${month + 1}/$day/$year"
                },
                startTime.get(Calendar.YEAR),
                startTime.get(Calendar.MONTH),
                startTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnStartTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    startTime.set(Calendar.HOUR_OF_DAY, hour)
                    startTime.set(Calendar.MINUTE, minute)
                    btnStartTime.text = String.format("%02d:%02d", hour, minute)
                },
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE),
                false
            ).show()
        }

        btnEndDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    endTime.set(year, month, day)
                    btnEndDate.text = "${month + 1}/$day/$year"
                },
                endTime.get(Calendar.YEAR),
                endTime.get(Calendar.MONTH),
                endTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnEndTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    endTime.set(Calendar.HOUR_OF_DAY, hour)
                    endTime.set(Calendar.MINUTE, minute)
                    btnEndTime.text = String.format("%02d:%02d", hour, minute)
                },
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Schedule Todo")
            .setView(dialogView)
            .setPositiveButton("Schedule") { _, _ ->
                viewModel.createEventFromTodo(todo.id, startTime.timeInMillis, endTime.timeInMillis)
                Toast.makeText(requireContext(), "Todo scheduled!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
