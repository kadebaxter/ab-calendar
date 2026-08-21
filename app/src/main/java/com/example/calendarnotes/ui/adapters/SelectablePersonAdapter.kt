package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Person

class SelectablePersonAdapter(
    private val onSelectionChanged: (Set<Long>) -> Unit
) : RecyclerView.Adapter<SelectablePersonAdapter.ViewHolder>() {

    private var people: List<Person> = emptyList()
    private val selectedIds = linkedSetOf<Long>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStar: TextView = view.findViewById(R.id.tvStatusStar)
        val statusColorDot: View = view.findViewById(R.id.statusColorDot)
        val tvName: TextView = view.findViewById(R.id.tvPersonName)
        val tvStatus: TextView = view.findViewById(R.id.tvPersonStatus)
        val tvPhone: TextView = view.findViewById(R.id.tvPersonPhone)
        val checkbox: CheckBox = view.findViewById(R.id.cbPersonSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val person = people[position]
        val selected = person.id in selectedIds
        val color = try {
            Color.parseColor(person.status.colorHex)
        } catch (_: Exception) {
            Color.GRAY
        }

        holder.tvName.text = person.name
        holder.tvStatus.text = person.status.label
        holder.checkbox.isChecked = selected
        holder.itemView.isActivated = selected

        if (person.status.showStar) {
            holder.tvStar.visibility = View.VISIBLE
            holder.statusColorDot.visibility = View.GONE
            holder.tvStar.setTextColor(color)
        } else {
            holder.tvStar.visibility = View.GONE
            holder.statusColorDot.visibility = View.VISIBLE
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            holder.statusColorDot.background = dot
        }

        if (person.phone.isNotBlank()) {
            holder.tvPhone.visibility = View.VISIBLE
            holder.tvPhone.text = person.phone
        } else {
            holder.tvPhone.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            toggle(people[position].id)
            notifyItemChanged(position)
            onSelectionChanged(selectedIds.toSet())
        }
    }

    override fun getItemCount() = people.size

    fun submit(people: List<Person>, selected: Set<Long>) {
        this.people = people
        selectedIds.clear()
        selectedIds.addAll(selected)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptySet())
    }

    fun selectedIds(): List<Long> = selectedIds.toList()

    private fun toggle(personId: Long) {
        if (!selectedIds.add(personId)) {
            selectedIds.remove(personId)
        }
    }
}
