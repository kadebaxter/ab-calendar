package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Category

class CategoryAdapter(
    private var categories: List<Category>,
    private val onAddSubCategory: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorIndicator: View = view.findViewById(R.id.colorIndicator)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvSubCategoryCount: TextView = view.findViewById(R.id.tvSubCategoryCount)
        val btnAddSubCategory: ImageButton = view.findViewById(R.id.btnAddSubCategory)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name
        holder.tvSubCategoryCount.text = "Tap to view sub-categories"
        
        try {
            holder.colorIndicator.setBackgroundColor(Color.parseColor(category.color))
        } catch (e: Exception) {
            holder.colorIndicator.setBackgroundColor(Color.BLUE)
        }

        holder.btnAddSubCategory.setOnClickListener {
            onAddSubCategory(category)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(category)
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
