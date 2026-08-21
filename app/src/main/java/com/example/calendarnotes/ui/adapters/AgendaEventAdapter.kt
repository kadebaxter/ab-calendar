package com.example.calendarnotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendaEventAdapter(
    private val onEventClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class AgendaRow {
        data class DayHeader(val dayStartMillis: Long) : AgendaRow()
        data class EventRow(val event: CalendarEvent) : AgendaRow()
    }

    private var rows: List<AgendaRow> = emptyList()

    private val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val dayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayName: TextView = view.findViewById(R.id.tvAgendaDayName)
        val tvDayDate: TextView = view.findViewById(R.id.tvAgendaDayDate)
    }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWhen: TextView = view.findViewById(R.id.tvAgendaWhen)
        val tvTitle: TextView = view.findViewById(R.id.tvAgendaTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvAgendaDescription)
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is AgendaRow.DayHeader -> VIEW_TYPE_HEADER
            is AgendaRow.EventRow -> VIEW_TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_agenda_day_header, parent, false))
        } else {
            EventViewHolder(inflater.inflate(R.layout.item_agenda_event, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is AgendaRow.DayHeader -> {
                val header = holder as HeaderViewHolder
                val date = Date(row.dayStartMillis)
                header.tvDayName.text = dayNameFormat.format(date)
                header.tvDayDate.text = dayDateFormat.format(date)
            }
            is AgendaRow.EventRow -> {
                val eventHolder = holder as EventViewHolder
                val event = row.event
                eventHolder.tvWhen.text = if (event.displaysAsAllDay()) {
                    eventHolder.itemView.context.getString(R.string.event_all_day)
                } else {
                    timeFormat.format(Date(event.startTime))
                }
                eventHolder.tvTitle.text = event.title
                if (event.description.isNotBlank()) {
                    eventHolder.tvDescription.visibility = View.VISIBLE
                    eventHolder.tvDescription.text = event.description
                } else {
                    eventHolder.tvDescription.visibility = View.GONE
                }
                eventHolder.itemView.setOnClickListener { onEventClick(event) }
            }
        }
    }

    override fun getItemCount() = rows.size

    fun submit(newEvents: List<CalendarEvent>) {
        val sorted = newEvents.sortedWith(
            compareBy<CalendarEvent> { dayStartMillis(it) }
                .thenBy { if (it.displaysAsAllDay()) 0 else 1 }
                .thenBy { it.startTime }
                .thenBy { it.title.lowercase(Locale.getDefault()) }
        )

        val built = mutableListOf<AgendaRow>()
        var lastDayStart = Long.MIN_VALUE
        sorted.forEach { event ->
            val dayStart = dayStartMillis(event)
            if (dayStart != lastDayStart) {
                built.add(AgendaRow.DayHeader(dayStart))
                lastDayStart = dayStart
            }
            built.add(AgendaRow.EventRow(event))
        }
        rows = built
        notifyDataSetChanged()
    }

    private fun dayStartMillis(event: CalendarEvent): Long {
        return Calendar.getInstance().apply {
            timeInMillis = event.startTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EVENT = 1
    }
}
