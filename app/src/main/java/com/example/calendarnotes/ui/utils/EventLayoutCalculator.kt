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
    
    /**
     * Calculate layout info for an event given its column assignment
     */
    fun calculateEventLayout(
        event: CalendarEvent,
        column: Int,
        totalColumns: Int,
        containerWidth: Int,
        density: Float,
        baseDate: java.util.Calendar
    ): EventLayoutInfo {
        val spacingDp = 4
        val spacingPx = (spacingDp * density).toInt()
        val cardMarginDp = 2 // The card has 2dp margin on all sides
        val cardMarginPx = (cardMarginDp * density).toInt()
        
        // Calculate dimensions - account for card margins
        val columnWidth = containerWidth / totalColumns
        val leftMarginPx = column * columnWidth
        val widthPx = columnWidth - spacingPx - (cardMarginPx * 2) // Remove both left and right margins
        
        // Calculate vertical positioning
        val durationMinutes = (event.endTime - event.startTime) / (1000 * 60)
        val heightPerMinute = 1f
        val eventHeightDp = (durationMinutes * heightPerMinute).toInt().coerceAtLeast(50)
        val heightPx = (eventHeightDp * density).toInt()
        
        val eventStartCal = java.util.Calendar.getInstance()
        eventStartCal.timeInMillis = event.startTime
        val minutesPastHour = eventStartCal.get(java.util.Calendar.MINUTE)
        val topMarginDp = minutesPastHour * 1f
        val topMarginPx = (topMarginDp * density).toInt()
        
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
