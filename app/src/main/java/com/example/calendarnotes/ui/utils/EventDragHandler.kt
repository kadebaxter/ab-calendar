package com.example.calendarnotes.ui.utils

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import java.util.Calendar

/**
 * Handles drag-to-move and top/bottom resize for day-view event cards.
 * Scale: 1dp = 1 minute (matches EventLayoutCalculator).
 */
class EventDragHandler(
    private val scrollParent: ViewGroup,
    private val baseDate: Calendar,
    private val onEventTimeChanged: (CalendarEvent, Long, Long) -> Unit,
    private val onDragStateChanged: (isDragging: Boolean, draggedEvent: CalendarEvent?) -> Unit
) {
    enum class DragMode { MOVE, RESIZE_START, RESIZE_END }

    private var isDragging = false
    private var dragMode: DragMode = DragMode.MOVE
    private var draggedEvent: CalendarEvent? = null
    private var draggedView: View? = null
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var eventInitialStartTime = 0L
    private var eventInitialEndTime = 0L
    private var originalTopMargin = 0
    private var originalHeight = 0
    private var previewStartTime = 0L
    private var previewEndTime = 0L
    private var hasMoved = false
    /** Once the finger moves past tap slop, release must not open the event. */
    private var suppressClick = false
    private var onClickCallback: ((CalendarEvent) -> Unit)? = null

    companion object {
        /** Intentional drag / not-a-tap distance (dp). */
        private const val TAP_SLOP_DP = 24f
        private const val MIN_DURATION_MS = 15L * 60L * 1000L
    }

    fun attachToEventView(
        view: View,
        event: CalendarEvent,
        allEvents: List<CalendarEvent>,
        onClickEvent: (CalendarEvent) -> Unit
    ) {
        view.isClickable = true
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        if (view is ViewGroup) {
            view.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        val body = view.findViewById<View>(R.id.eventBody) ?: view
        val topHandle = view.findViewById<View>(R.id.resizeHandleTop)
        val bottomHandle = view.findViewById<View>(R.id.resizeHandleBottom)

        body.isClickable = true
        if (body is ViewGroup) {
            body.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }

        body.setOnTouchListener { _, motionEvent ->
            dispatchTouch(view, motionEvent, event, DragMode.MOVE, onClickEvent)
        }
        topHandle?.setOnTouchListener { _, motionEvent ->
            dispatchTouch(view, motionEvent, event, DragMode.RESIZE_START, onClickEvent)
        }
        bottomHandle?.setOnTouchListener { _, motionEvent ->
            dispatchTouch(view, motionEvent, event, DragMode.RESIZE_END, onClickEvent)
        }
    }

    /**
     * Used for both direct handle/body touches and proxied touches from later
     * hour rows that sit on top of a multi-hour overflowing event.
     */
    fun dispatchTouch(
        view: View,
        motionEvent: MotionEvent,
        event: CalendarEvent,
        mode: DragMode,
        onClickEvent: (CalendarEvent) -> Unit
    ): Boolean {
        return try {
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onClickCallback = onClickEvent
                    handleTouchDown(view, motionEvent, event, mode)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val slop = tapSlopPx(view)
                    val dx = kotlin.math.abs(motionEvent.rawX - initialTouchX)
                    val dy = kotlin.math.abs(motionEvent.rawY - initialTouchY)
                    if (!suppressClick && (dx > slop || dy > slop)) {
                        // Past tap slop: this is no longer a click (horizontal or vertical).
                        suppressClick = true
                    }
                    // Vertical movement past slop starts move/resize.
                    if (hasMoved || dy > slop) {
                        if (!isDragging) {
                            startDragging(view)
                        }
                        handleDragMove(motionEvent)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        handleDragEnd()
                    } else {
                        // Only a clean UP with no drag slop opens. CANCEL means the
                        // gesture was aborted (e.g. stolen) — never treat that as a tap.
                        val shouldOpen =
                            !suppressClick && motionEvent.actionMasked == MotionEvent.ACTION_UP
                        resetVisualState(view)
                        if (shouldOpen) {
                            onClickCallback?.invoke(event)
                        }
                    }
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun tapSlopPx(view: View): Float {
        return TAP_SLOP_DP * view.resources.displayMetrics.density
    }

    private fun resetVisualState(view: View) {
        view.elevation = 1f * view.context.resources.displayMetrics.density
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
        draggedView = null
        draggedEvent = null
        hasMoved = false
        isDragging = false
        suppressClick = false
        scrollParent.requestDisallowInterceptTouchEvent(false)
    }

    private fun handleTouchDown(view: View, motionEvent: MotionEvent, event: CalendarEvent, mode: DragMode) {
        dragMode = mode
        draggedView = view
        draggedEvent = event
        initialTouchX = motionEvent.rawX
        initialTouchY = motionEvent.rawY
        eventInitialStartTime = event.startTime
        eventInitialEndTime = event.endTime
        previewStartTime = event.startTime
        previewEndTime = event.endTime
        hasMoved = false
        isDragging = false
        suppressClick = false

        val lp = view.layoutParams as? FrameLayout.LayoutParams
        originalTopMargin = lp?.topMargin ?: 0
        originalHeight = view.height.takeIf { it > 0 } ?: (lp?.height ?: 0)

        // Claim the gesture immediately so the day ScrollView can't steal MOVE/CANCEL
        // before we reach tap slop (that was aborting drags and opening the event).
        scrollParent.requestDisallowInterceptTouchEvent(true)

        (view.parent as? ViewGroup)?.bringChildToFront(view)
        view.elevation = 6f * view.context.resources.displayMetrics.density
        view.alpha = 0.85f
    }

    private fun startDragging(view: View) {
        isDragging = true
        hasMoved = true
        scrollParent.requestDisallowInterceptTouchEvent(true)
        view.elevation = 16f * view.context.resources.displayMetrics.density
        view.alpha = 0.75f
        if (dragMode == DragMode.MOVE) {
            view.scaleX = 1.05f
            view.scaleY = 1.05f
        }
        draggedEvent?.let { onDragStateChanged(true, it) }
    }

    private fun handleDragMove(motionEvent: MotionEvent) {
        val view = draggedView ?: return
        val density = view.context.resources.displayMetrics.density
        val deltaY = motionEvent.rawY - initialTouchY
        val deltaMinutes = (deltaY / density).toLong()
        val deltaMs = deltaMinutes * 60L * 1000L

        when (dragMode) {
            DragMode.MOVE -> {
                view.translationY = deltaY
                previewStartTime = TimeConverter.snapToInterval(eventInitialStartTime + deltaMs)
                val duration = eventInitialEndTime - eventInitialStartTime
                previewEndTime = previewStartTime + duration
            }
            DragMode.RESIZE_START -> {
                var newStart = TimeConverter.snapToInterval(eventInitialStartTime + deltaMs)
                val maxStart = eventInitialEndTime - MIN_DURATION_MS
                if (newStart > maxStart) newStart = maxStart
                newStart = clampToDayStart(newStart)
                previewStartTime = newStart
                previewEndTime = eventInitialEndTime
                applyResizePreview(view, previewStartTime, previewEndTime)
            }
            DragMode.RESIZE_END -> {
                var newEnd = TimeConverter.snapToInterval(eventInitialEndTime + deltaMs)
                val minEnd = eventInitialStartTime + MIN_DURATION_MS
                if (newEnd < minEnd) newEnd = minEnd
                newEnd = clampToDayEnd(newEnd)
                previewStartTime = eventInitialStartTime
                previewEndTime = newEnd
                applyResizePreview(view, previewStartTime, previewEndTime)
            }
        }

        draggedEvent?.let { onDragStateChanged(true, it) }
    }

    private fun applyResizePreview(view: View, start: Long, end: Long) {
        val density = view.context.resources.displayMetrics.density
        val startDeltaMin = ((start - eventInitialStartTime) / 60000L).toInt()
        val durationMin = ((end - start) / 60000L).toInt().coerceAtLeast(15)
        val lp = view.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.topMargin = originalTopMargin + (startDeltaMin * density).toInt()
        lp.height = (durationMin * density).toInt()
        view.layoutParams = lp
        view.translationY = 0f
        view.requestLayout()
    }

    private fun handleDragEnd() {
        val view = draggedView ?: return
        val event = draggedEvent ?: return

        view.elevation = 1f * view.context.resources.displayMetrics.density
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
        scrollParent.requestDisallowInterceptTouchEvent(false)

        var finalStart = previewStartTime
        var finalEnd = previewEndTime

        // Re-validate MOVE against day bounds (duration preserved).
        if (dragMode == DragMode.MOVE) {
            val duration = eventInitialEndTime - eventInitialStartTime
            finalStart = TimeConverter.snapToInterval(previewStartTime)
            finalEnd = finalStart + duration
        }

        if (isWithinDay(finalStart, finalEnd) && finalEnd - finalStart >= MIN_DURATION_MS) {
            if (finalStart != eventInitialStartTime || finalEnd != eventInitialEndTime) {
                onEventTimeChanged(event, finalStart, finalEnd)
            }
        }

        isDragging = false
        draggedEvent = null
        draggedView = null
        hasMoved = false
        suppressClick = false
        onDragStateChanged(false, null)
    }

    private fun dayBounds(): Pair<Long, Long> {
        val start = baseDate.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        val end = start.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 0)
        end.set(Calendar.MILLISECOND, 0)
        return start.timeInMillis to end.timeInMillis
    }

    private fun clampToDayStart(time: Long): Long {
        val (dayStart, _) = dayBounds()
        return time.coerceAtLeast(dayStart)
    }

    private fun clampToDayEnd(time: Long): Long {
        val (_, dayEnd) = dayBounds()
        return time.coerceAtMost(dayEnd)
    }

    private fun isWithinDay(start: Long, end: Long): Boolean {
        val (dayStart, dayEnd) = dayBounds()
        return start >= dayStart && end <= dayEnd
    }

    fun getCurrentDragInfo(): Pair<Boolean, CalendarEvent?> {
        return Pair(isDragging, draggedEvent)
    }
}
