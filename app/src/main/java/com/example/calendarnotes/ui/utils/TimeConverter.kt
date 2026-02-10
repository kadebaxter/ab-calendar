package com.example.calendarnotes.ui.utils

import java.util.*

/**
 * Utility class for converting between pixel positions and time values
 * Follows single responsibility principle - only handles time/position conversions
 */
object TimeConverter {
    private const val MINUTES_PER_HOUR = 60
    private const val DP_PER_MINUTE = 1f
    
    /**
     * Converts a Y-position in pixels to time in milliseconds
     * @param yPositionPx The Y position in pixels from the top of the day view
     * @param density Display density for dp to px conversion
     * @param baseDate The calendar date (at midnight) to calculate from
     * @return Time in milliseconds
     */
    fun pixelToTime(yPositionPx: Float, density: Float, baseDate: Calendar): Long {
        val yPositionDp = yPositionPx / density
        val totalMinutes = (yPositionDp / DP_PER_MINUTE).toInt()
        
        val resultCal = baseDate.clone() as Calendar
        resultCal.add(Calendar.MINUTE, totalMinutes)
        return resultCal.timeInMillis
    }
    
    /**
     * Converts time to Y-position in pixels
     * @param timeMillis The time in milliseconds
     * @param baseDate The calendar date (at midnight) to calculate from
     * @param density Display density for dp to px conversion
     * @return Y position in pixels
     */
    fun timeToPixel(timeMillis: Long, baseDate: Calendar, density: Float): Float {
        val eventCal = Calendar.getInstance()
        eventCal.timeInMillis = timeMillis
        
        val minutesSinceMidnight = eventCal.get(Calendar.HOUR_OF_DAY) * MINUTES_PER_HOUR + 
                                   eventCal.get(Calendar.MINUTE)
        val positionDp = minutesSinceMidnight * DP_PER_MINUTE
        return positionDp * density
    }
    
    /**
     * Snaps time to nearest interval (e.g., 15 minutes)
     * @param timeMillis The time to snap
     * @param snapIntervalMinutes The interval in minutes (default 15)
     * @return Snapped time in milliseconds
     */
    fun snapToInterval(timeMillis: Long, snapIntervalMinutes: Int = 15): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        
        val minutes = cal.get(Calendar.MINUTE)
        val snappedMinutes = (minutes / snapIntervalMinutes) * snapIntervalMinutes
        
        cal.set(Calendar.MINUTE, snappedMinutes)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        return cal.timeInMillis
    }
}
