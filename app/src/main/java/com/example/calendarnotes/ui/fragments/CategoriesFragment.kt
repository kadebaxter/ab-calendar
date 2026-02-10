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
import com.example.calendarnotes.data.models.Category
import com.example.calendarnotes.ui.adapters.CategoryAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CategoriesFragment : Fragment() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var rvCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CalendarNotesViewModel::class.java]

        rvCategories = view.findViewById(R.id.rvCategories)
        val fabAddCategory: FloatingActionButton = view.findViewById(R.id.fabAddCategory)

        setupRecyclerView()

        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        observeViewModel()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            emptyList(),
            onAddSubCategory = { category ->
                showAddSubCategoryDialog(category)
            },
            onDelete = { category ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Category")
                    .setMessage("Are you sure? This will delete all sub-categories and todos in this category.")
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

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.updateCategories(categories)
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCategoryName)
        val spinnerColor = dialogView.findViewById<Spinner>(R.id.spinnerColor)

        val colors = listOf(
            "Blue" to "#2196F3",
            "Red" to "#F44336",
            "Green" to "#4CAF50",
            "Orange" to "#FF9800",
            "Purple" to "#9C27B0",
            "Teal" to "#009688"
        )
        
        val colorNames = colors.map { it.first }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColor.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString()
                val color = colors[spinnerColor.selectedItemPosition].second
                if (name.isNotBlank()) {
                    viewModel.addCategory(name, color)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddSubCategoryDialog(category: Category) {
        val input = EditText(requireContext())
        input.hint = "Sub-category name"
        input.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Sub-category to ${category.name}")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    viewModel.addSubCategory(category.id, name)
                    Toast.makeText(requireContext(), "Sub-category added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
