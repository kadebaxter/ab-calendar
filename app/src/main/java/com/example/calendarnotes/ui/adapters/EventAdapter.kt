package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: List<CalendarEvent>,
    private val categoryColors: Map<Long, String>,
    private val onDelete: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorIndicator: View = view.findViewById(R.id.eventColorIndicator)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvTime: TextView = view.findViewById(R.id.tvEventTime)
        val tvDescription: TextView = view.findViewById(R.id.tvEventDescription)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.tvTitle.text = event.title
        holder.tvDescription.text = event.description
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTime = timeFormat.format(Date(event.startTime))
        val endTime = timeFormat.format(Date(event.endTime))
        holder.tvTime.text = "$startTime - $endTime"

        // Set color indicator
        event.categoryId?.let { catId ->
            categoryColors[catId]?.let { color ->
                try {
                    holder.colorIndicator.setBackgroundColor(Color.parseColor(color))
                } catch (e: Exception) {
                    holder.colorIndicator.setBackgroundColor(Color.BLUE)
                }
            }
        }

        holder.btnDelete.setOnClickListener {
            onDelete(event)
        }
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<CalendarEvent>, newCategoryColors: Map<Long, String>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
