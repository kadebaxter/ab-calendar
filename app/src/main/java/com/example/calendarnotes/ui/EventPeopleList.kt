package com.example.calendarnotes.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Person

object EventPeopleList {
    fun bind(
        container: LinearLayout,
        emptyView: TextView,
        people: List<Person>
    ) {
        container.removeAllViews()
        if (people.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyView.visibility = View.GONE
        container.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(container.context)
        people.forEach { person ->
            val row = inflater.inflate(R.layout.item_event_person, container, false)
            val tvStar = row.findViewById<TextView>(R.id.tvStatusStar)
            val statusDot = row.findViewById<View>(R.id.statusColorDot)
            row.findViewById<TextView>(R.id.tvPersonName).text = person.name
            row.findViewById<TextView>(R.id.tvPersonStatus).text = person.status.label

            val color = try {
                Color.parseColor(person.status.colorHex)
            } catch (_: Exception) {
                Color.GRAY
            }

            if (person.status.showStar) {
                tvStar.visibility = View.VISIBLE
                statusDot.visibility = View.GONE
                tvStar.setTextColor(color)
            } else {
                tvStar.visibility = View.GONE
                statusDot.visibility = View.VISIBLE
                val dot = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                statusDot.background = dot
            }
            container.addView(row)
        }
    }
}
