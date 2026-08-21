package com.example.calendarnotes.ui.utils

import com.example.calendarnotes.data.models.CalendarEvent

/**
 * Calculates layout positions for events, handling overlaps
 * Follows single responsibility principle - only handles layout calculations
 */
data class EventLayoutInfo(
    val column: Int,
    val totalColumns: Int,
    val leftMarginPx: Int,
    val widthPx: Int,
    val topMarginPx: Int,
    val heightPx: Int
)

object EventLayoutCalculator {
    private const val DP_PER_MINUTE = 1f
    private const val SPACING_DP = 4

    /**
     * Checks if two events overlap in time
     */
    fun eventsOverlap(event1: CalendarEvent, event2: CalendarEvent): Boolean {
        return event1.startTime < event2.endTime && event2.startTime < event1.endTime
    }
    
    /**
     * Assigns columns to a list of events to avoid overlaps
     * Returns a map of event index to column number
     */
    fun assignColumns(events: List<CalendarEvent>): Map<Int, Int> {
        if (events.isEmpty()) return emptyMap()
        
        val columnAssignments = mutableMapOf<Int, Int>()
        val columnEndTimes = mutableListOf<Long>()
        
        for ((index, event) in events.withIndex()) {
            var column = 0
            for (i in columnEndTimes.indices) {
                if (columnEndTimes[i] <= event.startTime) {
                    column = i
                    columnEndTimes[i] = event.endTime
                    break
                }
                column = i + 1
            }
            
            if (column >= columnEndTimes.size) {
                columnEndTimes.add(event.endTime)
            }
            
            columnAssignments[index] = column
        }
        
        return columnAssignments
    }
    
    /** Horizontal card bounds inside the events container (matches [calculateEventLayout]). */
    fun horizontalBounds(
        column: Int,
        totalColumns: Int,
        containerWidth: Int,
        density: Float
    ): Pair<Int, Int> {
        val columns = totalColumns.coerceAtLeast(1)
        val spacingPx = (SPACING_DP * density).toInt()
        val columnWidth = containerWidth / columns
        val left = column * columnWidth + spacingPx / 2
        val width = (columnWidth - spacingPx).coerceAtLeast(1)
        return left to (left + width)
    }

    /**
     * Calculate layout info for an event given its column assignment.
     * Cards are hosted on their start-hour row and may overflow into later hours.
     */
    fun calculateEventLayout(
        event: CalendarEvent,
        column: Int,
        totalColumns: Int,
        containerWidth: Int,
        density: Float
    ): EventLayoutInfo {
        val (leftMarginPx, right) = horizontalBounds(column, totalColumns, containerWidth, density)
        val widthPx = (right - leftMarginPx).coerceAtLeast(1)

        // 1dp per minute matches the 60dp hour rows. Keep height strictly within
        // the event duration so back-to-back events (e.g. 1:00-1:30 / 1:30-...)
        // meet at the boundary instead of overlapping.
        val durationMinutes = ((event.endTime - event.startTime) / (1000 * 60))
            .toInt()
            .coerceAtLeast(1)
        val heightPx = (durationMinutes * DP_PER_MINUTE * density).toInt().coerceAtLeast(1)

        val eventStartCal = java.util.Calendar.getInstance()
        eventStartCal.timeInMillis = event.startTime
        val minutesPastHour = eventStartCal.get(java.util.Calendar.MINUTE)
        val topMarginPx = (minutesPastHour * DP_PER_MINUTE * density).toInt()

        return EventLayoutInfo(
            column = column,
            totalColumns = totalColumns,
            leftMarginPx = leftMarginPx,
            widthPx = widthPx,
            topMarginPx = topMarginPx,
            heightPx = heightPx
        )
    }
    
    /**
     * Find which events would overlap with a given time range
     */
    fun findOverlappingEvents(
        allEvents: List<CalendarEvent>,
        startTime: Long,
        endTime: Long
    ): List<CalendarEvent> {
        val tempEvent = CalendarEvent(
            id = -1,
            title = "",
            description = "",
            startTime = startTime,
            endTime = endTime,
            categoryId = null
        )
        
        return allEvents.filter { event ->
            eventsOverlap(event, tempEvent)
        }
    }
}
