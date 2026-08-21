package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Category
import com.example.calendarnotes.data.models.Note
import com.google.android.material.card.MaterialCardView

class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onToggleComplete: (Note) -> Unit,
    private val onSchedule: (Note) -> Unit,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()
    private var categoriesById: Map<Long, Category> = emptyMap()

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorBar: View = view.findViewById(R.id.noteColorBar)
        val cbComplete: CheckBox = view.findViewById(R.id.cbNoteComplete)
        val tvTitle: TextView = view.findViewById(R.id.tvNoteTitle)
        val tvContent: TextView = view.findViewById(R.id.tvNoteContent)
        val tvCategory: TextView = view.findViewById(R.id.tvNoteCategory)
        val btnSchedule: ImageButton = view.findViewById(R.id.btnScheduleNote)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        val category = note.categoryId?.let { categoriesById[it] }

        holder.tvTitle.text = note.title
        holder.tvContent.text = note.content
        holder.tvContent.visibility = if (note.content.isBlank()) View.GONE else View.VISIBLE

        if (category != null) {
            holder.tvCategory.visibility = View.VISIBLE
            holder.tvCategory.text = category.name
            try {
                val color = Color.parseColor(category.color)
                holder.colorBar.setBackgroundColor(color)
                (holder.itemView as? MaterialCardView)?.setCardBackgroundColor(
                    Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))
                )
            } catch (_: Exception) {
                holder.colorBar.setBackgroundColor(Color.GRAY)
            }
        } else {
            holder.tvCategory.visibility = View.GONE
            holder.colorBar.setBackgroundColor(Color.GRAY)
        }

        holder.cbComplete.setOnCheckedChangeListener(null)
        holder.cbComplete.isChecked = note.isCompleted
        applyCompletedStyle(holder, note.isCompleted)
        holder.cbComplete.setOnCheckedChangeListener { _, _ ->
            onToggleComplete(note)
        }

        holder.itemView.setOnClickListener { onNoteClick(note) }
        holder.btnSchedule.setOnClickListener { onSchedule(note) }
        holder.btnDelete.setOnClickListener { onDelete(note) }
    }

    private fun applyCompletedStyle(holder: NoteViewHolder, completed: Boolean) {
        val flags = if (completed) {
            holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        holder.tvTitle.paintFlags = flags
        holder.itemView.alpha = if (completed) 0.55f else 1f
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>, categories: List<Category>) {
        notes = newNotes
        categoriesById = categories.associateBy { it.id }
        notifyDataSetChanged()
    }
}
