package com.example.calendarnotes.ui.utils

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.data.models.CalendarEvent
import java.util.*

/**
 * Handles drag and drop logic for calendar events
 * Follows single responsibility - manages drag state and calculations
 */
class EventDragHandler(
    private val recyclerView: RecyclerView,
    private val baseDate: Calendar,
    private val onEventTimeChanged: (CalendarEvent, Long, Long) -> Unit,
    private val onDragStateChanged: (isDragging: Boolean, draggedEvent: CalendarEvent?) -> Unit
) {
    private var isDragging = false
    private var draggedEvent: CalendarEvent? = null
    private var draggedView: View? = null
    private var initialTouchY = 0f
    private var lastCalculatedTime = 0L
    private var eventInitialStartTime = 0L
    private var eventInitialEndTime = 0L
    private var hasMoved = false
    private var onClickCallback: ((CalendarEvent) -> Unit)? = null
    
    companion object {
        private const val DRAG_THRESHOLD = 8f // pixels to move before considering it a drag
    }
    
    /**
     * Attaches drag functionality to an event view
     */
    fun attachToEventView(
        view: View, 
        event: CalendarEvent, 
        allEvents: List<CalendarEvent>,
        onClickEvent: (CalendarEvent) -> Unit
    ) {
        // Make the entire card surface handle touches, not just child views
        view.isClickable = true
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        
        // Prevent child views (TextViews, etc.) from intercepting touches
        if (view is ViewGroup) {
            view.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
        
        view.setOnTouchListener { v, motionEvent ->
            try {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onClickCallback = onClickEvent
                        handleTouchDown(v, motionEvent, event)
                        true // Claim ownership of all touch events
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (hasMoved || Math.abs(motionEvent.rawY - initialTouchY) > DRAG_THRESHOLD) {
                            if (!isDragging) {
                                startDragging(v)
                            }
                            handleDragMove(motionEvent, allEvents)
                        }
                        true // Keep ownership
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            handleDragEnd(allEvents)
                        } else {
                            // It was a tap - manually trigger click
                            resetVisualState(v)
                            onClickCallback?.invoke(event)
                        }
                        true // Always consume the event
                    }
                    else -> false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    private fun resetVisualState(view: View) {
        view.elevation = 1f * view.context.resources.displayMetrics.density
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        draggedView = null
        draggedEvent = null
        hasMoved = false
        isDragging = false
    }
    
    private fun handleTouchDown(view: View, motionEvent: MotionEvent, event: CalendarEvent) {
        draggedView = view
        draggedEvent = event
        initialTouchY = motionEvent.rawY
        eventInitialStartTime = event.startTime
        eventInitialEndTime = event.endTime
        lastCalculatedTime = event.startTime
        hasMoved = false
        isDragging = false
        
        // Give more obvious visual feedback on touch down
        view.elevation = 6f * view.context.resources.displayMetrics.density
        view.alpha = 0.85f
    }
    
    private fun startDragging(view: View) {
        isDragging = true
        hasMoved = true
        
        // Prevent RecyclerView from scrolling during drag
        recyclerView.requestDisallowInterceptTouchEvent(true)
        
        // Stronger visual feedback when actually dragging
        view.elevation = 16f * view.context.resources.displayMetrics.density
        view.alpha = 0.75f
        view.scaleX = 1.05f
        view.scaleY = 1.05f
        
        draggedEvent?.let { onDragStateChanged(true, it) }
    }
    
    private fun handleDragMove(motionEvent: MotionEvent, allEvents: List<CalendarEvent>) {
        val view = draggedView ?: return
        
        // Calculate how much the finger has moved in pixels
        val deltaY = motionEvent.rawY - initialTouchY
        
        // Update view position smoothly
        view.translationY = deltaY
        
        // Convert pixel movement to time change
        val density = view.context.resources.displayMetrics.density
        val deltaMinutes = (deltaY / density).toLong() // 1dp = 1 minute
        
        // Calculate new time
        val newStartTime = eventInitialStartTime + (deltaMinutes * 60 * 1000)
        lastCalculatedTime = TimeConverter.snapToInterval(newStartTime)
        
        // Trigger preview update
        draggedEvent?.let { onDragStateChanged(true, it) }
    }
    
    private fun handleDragEnd(allEvents: List<CalendarEvent>) {
        val view = draggedView ?: return
        val event = draggedEvent ?: return
        
        // Reset view appearance
        view.elevation = 1f * view.context.resources.displayMetrics.density
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
        
        // Allow RecyclerView scrolling again
        recyclerView.requestDisallowInterceptTouchEvent(false)
        
        // Use the last calculated time
        val finalStartTime = lastCalculatedTime
        val duration = eventInitialEndTime - eventInitialStartTime
        val finalEndTime = finalStartTime + duration
        
        // Validate times (must be within the same day)
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = finalStartTime
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = finalEndTime
        
        val baseCal = baseDate.clone() as Calendar
        baseCal.set(Calendar.HOUR_OF_DAY, 0)
        baseCal.set(Calendar.MINUTE, 0)
        baseCal.set(Calendar.SECOND, 0)
        baseCal.set(Calendar.MILLISECOND, 0)
        
        val endOfDay = baseCal.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        
        if (startCal.get(Calendar.DAY_OF_YEAR) == baseDate.get(Calendar.DAY_OF_YEAR) &&
            endCal.get(Calendar.DAY_OF_YEAR) == baseDate.get(Calendar.DAY_OF_YEAR) &&
            finalStartTime >= baseCal.timeInMillis &&
            finalEndTime <= endOfDay.timeInMillis) {
            // Valid time, update
            onEventTimeChanged(event, finalStartTime, finalEndTime)
        }
        
        // Clear drag state
        isDragging = false
        draggedEvent = null
        draggedView = null
        hasMoved = false
        
        onDragStateChanged(false, null)
    }
    
    fun getCurrentDragInfo(): Pair<Boolean, CalendarEvent?> {
        return Pair(isDragging, draggedEvent)
    }
}
