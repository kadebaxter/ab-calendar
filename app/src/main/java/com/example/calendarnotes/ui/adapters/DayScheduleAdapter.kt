package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.ui.utils.EventDragHandler
import com.example.calendarnotes.ui.utils.EventLayoutCalculator
import java.text.SimpleDateFormat
import java.util.*

data class TimeSlot(
    val hour: Int,
    val events: List<CalendarEvent>
)

class DayScheduleAdapter(
    private val recyclerView: RecyclerView,
    private val baseDate: Calendar,
    private val onTimeSlotClick: (Int) -> Unit,
    private val onEventClick: (CalendarEvent) -> Unit,
    private val onEventTimeChanged: (CalendarEvent, Long, Long) -> Unit
) : RecyclerView.Adapter<DayScheduleAdapter.TimeSlotViewHolder>() {

    private var timeSlots: List<TimeSlot> = emptyList()
    private var allEvents: List<CalendarEvent> = emptyList()
    private var categoryColors: Map<Long, String> = emptyMap()
    private var isDragging = false
    private var draggedEvent: CalendarEvent? = null
    
    private val dragHandler = EventDragHandler(
        recyclerView = recyclerView,
        baseDate = baseDate,
        onEventTimeChanged = onEventTimeChanged,
        onDragStateChanged = { dragging, event ->
            isDragging = dragging
            draggedEvent = event
            if (!dragging) {
                // Only refresh layout after drag ends
                notifyDataSetChanged()
            }
            // Don't call notifyDataSetChanged during drag - it destroys views
        }
    )

    class TimeSlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTimeLabel: TextView = view.findViewById(R.id.tvTimeLabel)
        val eventsContainer: ViewGroup = view.findViewById(R.id.eventsContainer)
        val eventContentArea: View = view.findViewById(R.id.eventContentArea)
        val currentTimeIndicator: View = view.findViewById(R.id.currentTimeIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        
        // Format time label
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, timeSlot.hour)
        calendar.set(Calendar.MINUTE, 0)
        holder.tvTimeLabel.text = timeFormat.format(calendar.time)

        // Clear previous events
        holder.eventsContainer.removeAllViews()

        // Get all events to consider for overlap calculation
        val eventsToDisplay = if (isDragging && draggedEvent != null) {
            // Include the dragged event at its current position for preview
            timeSlot.events
        } else {
            timeSlot.events
        }

        // Assign columns using the utility
        val eventColumns = EventLayoutCalculator.assignColumns(eventsToDisplay)
        val maxColumns = if (eventColumns.isEmpty()) 1 else eventColumns.values.maxOrNull()!! + 1

        // Wait for layout to complete before positioning events
        if (holder.eventsContainer.width == 0) {
            holder.eventsContainer.post {
                addEventsToContainer(holder, eventsToDisplay, eventColumns, maxColumns)
            }
        } else {
            addEventsToContainer(holder, eventsToDisplay, eventColumns, maxColumns)
        }

        // Make the time slot clickable to add new events
        holder.eventContentArea.setOnClickListener {
            if (timeSlot.events.isEmpty()) {
                onTimeSlotClick(timeSlot.hour)
            }
        }

        // Show current time indicator if this is today and the current hour
        showCurrentTimeIndicator(holder, timeSlot.hour)
    }

    private fun showCurrentTimeIndicator(holder: TimeSlotViewHolder, hour: Int) {
        val now = Calendar.getInstance()
        val today = Calendar.getInstance()
        today.timeInMillis = baseDate.timeInMillis
        
        // Check if this is today
        val isToday = now.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                     now.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        
        if (!isToday) {
            holder.currentTimeIndicator.visibility = View.GONE
            return
        }
        
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        
        // Show indicator only if current time is within this hour slot
        if (currentHour == hour) {
            holder.currentTimeIndicator.visibility = View.VISIBLE
            
            // Position the indicator based on minutes (0-59 minutes within the hour slot height)
            holder.currentTimeIndicator.post {
                val slotHeight = holder.eventContentArea.height
                val minutePercent = currentMinute / 60f
                val topMargin = (slotHeight * minutePercent).toInt()
                
                val params = holder.currentTimeIndicator.layoutParams as android.widget.FrameLayout.LayoutParams
                params.topMargin = topMargin
                holder.currentTimeIndicator.layoutParams = params
            }
        } else {
            holder.currentTimeIndicator.visibility = View.GONE
        }
    }
    
    private fun addEventsToContainer(
        holder: TimeSlotViewHolder,
        events: List<CalendarEvent>,
        eventColumns: Map<Int, Int>,
        maxColumns: Int
    ) {
        // Clear any existing views first
        holder.eventsContainer.removeAllViews()
        
        // Add events for this time slot
        for ((index, event) in events.withIndex()) {
            val eventView = createEventView(
                holder,
                event,
                index,
                eventColumns[index] ?: 0,
                maxColumns
            )
            
            // Attach drag handler with click callback - it handles both dragging and clicking
            dragHandler.attachToEventView(eventView, event, allEvents) { clickedEvent ->
                onEventClick(clickedEvent)
            }
            
            holder.eventsContainer.addView(eventView)
        }
    }
    
    private fun createEventView(
        holder: TimeSlotViewHolder,
        event: CalendarEvent,
        index: Int,
        column: Int,
        maxColumns: Int
    ): View {
        val eventView = LayoutInflater.from(holder.itemView.context)
            .inflate(R.layout.item_schedule_event, holder.eventsContainer, false)

        val tvTitle = eventView.findViewById<TextView>(R.id.tvEventTitle)
        val tvTime = eventView.findViewById<TextView>(R.id.tvEventTime)
        val colorBar = eventView.findViewById<View>(R.id.eventColorBar)

        tvTitle.text = event.title

        val eventTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val startTime = eventTimeFormat.format(Date(event.startTime))
        val endTime = eventTimeFormat.format(Date(event.endTime))
        tvTime.text = "$startTime - $endTime"
        
        // Calculate layout using the utility with safety check
        val containerWidth = holder.eventsContainer.width.coerceAtLeast(1) // Prevent division by zero
        val density = holder.itemView.context.resources.displayMetrics.density
        val layoutInfo = EventLayoutCalculator.calculateEventLayout(
            event = event,
            column = column,
            totalColumns = maxColumns,
            containerWidth = containerWidth,
            density = density,
            baseDate = baseDate
        )
        
        // Apply layout
        val layoutParams = android.widget.FrameLayout.LayoutParams(
            layoutInfo.widthPx,
            layoutInfo.heightPx
        )
        layoutParams.topMargin = layoutInfo.topMarginPx
        layoutParams.leftMargin = layoutInfo.leftMarginPx
        eventView.layoutParams = layoutParams

        // Set color
        applyEventColor(event, eventView, tvTitle, colorBar)
        
        return eventView
    }
    
    private fun applyEventColor(
        event: CalendarEvent,
        eventView: View,
        tvTitle: TextView,
        colorBar: View
    ) {
        event.categoryId?.let { catId ->
            categoryColors[catId]?.let { color ->
                try {
                    val colorInt = Color.parseColor(color)
                    colorBar.setBackgroundColor(colorInt)
                    val lightBackground = lightenColor(colorInt, 0.92f)
                    (eventView as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(lightBackground)
                    tvTitle.setTextColor(Color.BLACK)
                } catch (e: Exception) {
                    colorBar.setBackgroundColor(Color.BLUE)
                    (eventView as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                    tvTitle.setTextColor(Color.BLACK)
                }
            }
        }
    }

    override fun getItemCount() = timeSlots.size

    fun updateSchedule(events: List<CalendarEvent>, colors: Map<Long, String>) {
        categoryColors = colors
        allEvents = events
        
        // Create time slots for 24 hours
        val slots = mutableListOf<TimeSlot>()
        for (hour in 0..23) {
            val hourEvents = events.filter { event ->
                val eventCalendar = Calendar.getInstance()
                eventCalendar.timeInMillis = event.startTime
                eventCalendar.get(Calendar.HOUR_OF_DAY) == hour
            }.sortedBy { it.startTime }
            
            slots.add(TimeSlot(hour, hourEvents))
        }
        
        timeSlots = slots
        notifyDataSetChanged()
    }

    fun refreshCurrentTimeIndicator() {
        // Refresh only the visible items to update the current time line
        notifyDataSetChanged()
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        // Blend the color with white to make it much lighter
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        
        val newRed = (red + (255 - red) * factor).toInt()
        val newGreen = (green + (255 - green) * factor).toInt()
        val newBlue = (blue + (255 - blue) * factor).toInt()
        
        return Color.rgb(newRed, newGreen, newBlue)
    }
}
