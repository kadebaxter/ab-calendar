package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Person

class PersonAdapter(
    private val onPersonClick: (Person) -> Unit
) : RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    private var people: List<Person> = emptyList()

    class PersonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStar: TextView = view.findViewById(R.id.tvStatusStar)
        val statusColorDot: View = view.findViewById(R.id.statusColorDot)
        val tvName: TextView = view.findViewById(R.id.tvPersonName)
        val tvStatus: TextView = view.findViewById(R.id.tvPersonStatus)
        val tvPhone: TextView = view.findViewById(R.id.tvPersonPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
        return PersonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = people[position]
        val status = person.status
        val color = try {
            Color.parseColor(status.colorHex)
        } catch (_: Exception) {
            Color.GRAY
        }

        holder.tvName.text = person.name
        holder.tvStatus.text = status.label
        holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))

        if (status.showStar) {
            // Exclusive / engaged / married: leading colored star
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

        holder.itemView.setOnClickListener { onPersonClick(person) }
    }

    override fun getItemCount() = people.size

    fun updatePeople(newPeople: List<Person>) {
        people = newPeople
        notifyDataSetChanged()
    }
}
