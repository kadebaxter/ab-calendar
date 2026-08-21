package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.ui.DayScheduleScrollView
import com.example.calendarnotes.ui.utils.EventDragHandler
import com.example.calendarnotes.ui.utils.EventLayoutCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Day grid with all 24 hour rows always attached inside a [DayScheduleScrollView].
 * Multi-hour cards live on their start-hour row and overflow into later hours.
 */
class DayScheduleAdapter(
    private val scrollView: DayScheduleScrollView,
    private val container: LinearLayout,
    private val baseDate: Calendar,
    private val onTimeSlotClick: (Int) -> Unit,
    private val onEventClick: (CalendarEvent) -> Unit,
    private val onEventTimeChanged: (CalendarEvent, Long, Long) -> Unit
) {
    private data class HourRow(
        val hour: Int,
        val root: View,
        val tvTimeLabel: TextView,
        val eventsContainer: ViewGroup,
        val eventContentArea: View,
        val currentTimeIndicator: View
    )

    private val hourRows = mutableListOf<HourRow>()
    private var allEvents: List<CalendarEvent> = emptyList()
    private var eventColumnsById: Map<Long, Int> = emptyMap()
    private var categoryColors: Map<Long, String> = emptyMap()
    private var isDragging = false
    private val eventViewsById = mutableMapOf<Long, View>()

    /** Gesture started on a later hour row, forwarded to an overflowing event card. */
    private var proxyActive = false
    private var proxyEventView: View? = null
    private var proxyEvent: CalendarEvent? = null
    private var proxyMode: EventDragHandler.DragMode = EventDragHandler.DragMode.MOVE

    /** Bumped on every schedule update so delayed layout posts can't add stale cards. */
    private var scheduleEpoch: Long = 0L

    private val dragHandler = EventDragHandler(
        scrollParent = scrollView,
        baseDate = baseDate,
        onEventTimeChanged = onEventTimeChanged,
        onDragStateChanged = { dragging, _ ->
            isDragging = dragging
            if (!dragging) {
                // Rebuild after drag so the card sits on its (possibly new) start hour.
                renderEvents()
            }
        }
    )

    init {
        buildHourRows()
    }

    private fun buildHourRows() {
        container.removeAllViews()
        hourRows.clear()
        val inflater = LayoutInflater.from(container.context)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        for (hour in 0..23) {
            val root = inflater.inflate(R.layout.item_time_slot, container, false)
            val tvTimeLabel = root.findViewById<TextView>(R.id.tvTimeLabel)
            val eventsContainer = root.findViewById<ViewGroup>(R.id.eventsContainer)
            val eventContentArea = root.findViewById<View>(R.id.eventContentArea)
            val currentTimeIndicator = root.findViewById<View>(R.id.currentTimeIndicator)

            val labelCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
            }
            tvTimeLabel.text = timeFormat.format(labelCal.time)

            val row = HourRow(
                hour = hour,
                root = root,
                tvTimeLabel = tvTimeLabel,
                eventsContainer = eventsContainer,
                eventContentArea = eventContentArea,
                currentTimeIndicator = currentTimeIndicator
            )
            bindRowInteractions(row)
            hourRows.add(row)
            container.addView(root)
        }
    }

    private fun bindRowInteractions(row: HourRow) {
        var lastTouchY = 0f
        var lastTouchXInContainer = 0f
        row.eventContentArea.setOnTouchListener { v, motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchY = motionEvent.y
                    lastTouchXInContainer = motionEvent.x - row.eventsContainer.left
                    val height = v.height.coerceAtLeast(1)
                    val minuteInHour = ((motionEvent.y / height) * 60f).toInt().coerceIn(0, 59)
                    // Hit-test by time AND column so empty space beside a left-lane
                    // event is not treated as that event.
                    val covering = findEventCovering(
                        hour = row.hour,
                        minuteInHour = minuteInHour,
                        xInEventsContainer = lastTouchXInContainer,
                        containerWidth = row.eventsContainer.width
                    ) ?: run {
                        clearProxy()
                        return@setOnTouchListener false
                    }
                    val startHour = eventStartHour(covering)
                    // Same-hour cards own their own touches via the event view.
                    if (startHour == row.hour) {
                        clearProxy()
                        return@setOnTouchListener false
                    }
                    val eventView = eventViewsById[covering.id] ?: run {
                        clearProxy()
                        return@setOnTouchListener false
                    }
                    proxyActive = true
                    proxyEventView = eventView
                    proxyEvent = covering
                    proxyMode = dragModeForOverflowTouch(covering, row.hour, minuteInHour)
                    scrollView.requestDisallowInterceptTouchEvent(true)
                    dragHandler.dispatchTouch(
                        eventView,
                        motionEvent,
                        covering,
                        proxyMode,
                        onEventClick
                    )
                }
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (!proxyActive) return@setOnTouchListener false
                    val eventView = proxyEventView
                    val event = proxyEvent
                    if (eventView == null || event == null) {
                        clearProxy()
                        return@setOnTouchListener false
                    }
                    dragHandler.dispatchTouch(
                        eventView,
                        motionEvent,
                        event,
                        proxyMode,
                        onEventClick
                    )
                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP ||
                        motionEvent.actionMasked == MotionEvent.ACTION_CANCEL
                    ) {
                        clearProxy()
                    }
                    true
                }
                else -> false
            }
        }
        row.eventContentArea.setOnClickListener {
            // Event cards handle their own tap/drag; this is only for empty slot taps.
            // Ignore if a drag just ended (isDragging flag can clear before click delivers).
            if (proxyActive || isDragging || dragHandler.getCurrentDragInfo().first) {
                return@setOnClickListener
            }
            val height = row.eventContentArea.height.coerceAtLeast(1)
            val minuteInHour = ((lastTouchY / height) * 60f).toInt().coerceIn(0, 59)
            val eventAtPoint = findEventCovering(
                hour = row.hour,
                minuteInHour = minuteInHour,
                xInEventsContainer = lastTouchXInContainer,
                containerWidth = row.eventsContainer.width
            )
            if (eventAtPoint != null) {
                // Covered by an overflowing card — open only via the card's tap path.
                return@setOnClickListener
            }
            onTimeSlotClick(row.hour)
        }
    }

    private fun clearProxy() {
        proxyActive = false
        proxyEventView = null
        proxyEvent = null
        proxyMode = EventDragHandler.DragMode.MOVE
    }

    private fun eventStartHour(event: CalendarEvent): Int {
        return Calendar.getInstance().apply { timeInMillis = event.startTime }
            .get(Calendar.HOUR_OF_DAY)
    }

    private fun dragModeForOverflowTouch(
        event: CalendarEvent,
        hour: Int,
        minuteInHour: Int
    ): EventDragHandler.DragMode {
        val touchMs = Calendar.getInstance().apply {
            timeInMillis = baseDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minuteInHour)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val minsToEnd = ((event.endTime - touchMs) / 60_000L).toInt()
        val minsFromStart = ((touchMs - event.startTime) / 60_000L).toInt()
        return when {
            minsToEnd <= HANDLE_ZONE_MINUTES -> EventDragHandler.DragMode.RESIZE_END
            minsFromStart <= HANDLE_ZONE_MINUTES -> EventDragHandler.DragMode.RESIZE_START
            else -> EventDragHandler.DragMode.MOVE
        }
    }

    /**
     * Event under a point in an hour row. Requires both time overlap and that [xInEventsContainer]
     * falls inside the event's column lane — empty lanes beside a card are not hits.
     */
    private fun findEventCovering(
        hour: Int,
        minuteInHour: Int,
        xInEventsContainer: Float,
        containerWidth: Int
    ): CalendarEvent? {
        if (containerWidth <= 0) return null
        val instant = Calendar.getInstance().apply {
            timeInMillis = baseDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minuteInHour)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val density = container.resources.displayMetrics.density

        // Include end instant so the bottom grip of an event ending on :00/:15 still hits.
        return allEvents
            .filter { instant >= it.startTime && instant <= it.endTime }
            .filter { event ->
                val column = eventColumnsById[event.id] ?: 0
                val totalColumns = columnCountForEvent(event)
                val (left, right) = EventLayoutCalculator.horizontalBounds(
                    column = column,
                    totalColumns = totalColumns,
                    containerWidth = containerWidth,
                    density = density
                )
                xInEventsContainer >= left && xInEventsContainer < right
            }
            .maxByOrNull { eventColumnsById[it.id] ?: 0 }
    }

    fun updateSchedule(events: List<CalendarEvent>, colors: Map<Long, String>) {
        scheduleEpoch++
        clearProxy()
        categoryColors = colors
        // All-day (and legacy ~24h Google imports) render above the grid, not as hour blocks.
        val timedEvents = events.filterNot { it.displaysAsAllDay() }
        allEvents = timedEvents
        eventColumnsById = computeEventColumns(timedEvents)
        renderEvents()
        refreshCurrentTimeIndicator()
    }

    private fun renderEvents() {
        val epoch = scheduleEpoch
        eventViewsById.clear()
        hourRows.forEach { it.eventsContainer.removeAllViews() }

        fun placeEvents() {
            if (epoch != scheduleEpoch || isDragging) return
            for (event in allEvents) {
                val startHour = eventStartHour(event)
                val row = hourRows.getOrNull(startHour) ?: continue
                // Only host events that start on the displayed day.
                val eventCal = Calendar.getInstance().apply { timeInMillis = event.startTime }
                if (eventCal.get(Calendar.YEAR) != baseDate.get(Calendar.YEAR) ||
                    eventCal.get(Calendar.DAY_OF_YEAR) != baseDate.get(Calendar.DAY_OF_YEAR)
                ) {
                    continue
                }
                val column = eventColumnsById[event.id] ?: 0
                val maxColumns = columnCountForEvent(event)
                val eventView = createEventView(row, event, column, maxColumns)
                dragHandler.attachToEventView(eventView, event, allEvents, onEventClick)
                row.eventsContainer.addView(eventView)
                eventViewsById[event.id] = eventView
            }
        }

        val sampleWidth = hourRows.firstOrNull()?.eventsContainer?.width ?: 0
        if (sampleWidth == 0) {
            container.post { placeEvents() }
        } else {
            placeEvents()
        }
    }

    private fun createEventView(
        row: HourRow,
        event: CalendarEvent,
        column: Int,
        maxColumns: Int
    ): View {
        val eventView = LayoutInflater.from(row.root.context)
            .inflate(R.layout.item_schedule_event, row.eventsContainer, false)

        val textContainer = eventView.findViewById<LinearLayout>(R.id.eventTextContainer)
        val tvTitle = eventView.findViewById<TextView>(R.id.tvEventTitle)
        val tvTime = eventView.findViewById<TextView>(R.id.tvEventTime)
        val colorBar = eventView.findViewById<View>(R.id.eventColorBar)

        tvTitle.text = event.title

        val eventTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val startTime = eventTimeFormat.format(Date(event.startTime))
        val endTime = eventTimeFormat.format(Date(event.endTime))
        tvTime.text = "$startTime - $endTime"

        val containerWidth = row.eventsContainer.width.coerceAtLeast(1)
        val density = row.root.context.resources.displayMetrics.density
        val layoutInfo = EventLayoutCalculator.calculateEventLayout(
            event = event,
            column = column,
            totalColumns = maxColumns,
            containerWidth = containerWidth,
            density = density
        )

        applyEventTextLayout(
            textContainer = textContainer,
            tvTitle = tvTitle,
            tvTime = tvTime,
            heightPx = layoutInfo.heightPx,
            widthPx = layoutInfo.widthPx,
            density = density
        )

        val layoutParams = FrameLayout.LayoutParams(
            layoutInfo.widthPx,
            layoutInfo.heightPx
        )
        layoutParams.topMargin = layoutInfo.topMarginPx
        layoutParams.leftMargin = layoutInfo.leftMarginPx
        eventView.layoutParams = layoutParams

        applyEventColor(event, eventView, tvTitle, colorBar)

        val showHandles = layoutInfo.heightPx >= (36 * density)
        eventView.findViewById<View>(R.id.resizeHandleTop)?.visibility =
            if (showHandles) View.VISIBLE else View.GONE
        eventView.findViewById<View>(R.id.resizeHandleBottom)?.visibility =
            if (showHandles) View.VISIBLE else View.GONE

        return eventView
    }

    private fun applyEventTextLayout(
        textContainer: LinearLayout,
        tvTitle: TextView,
        tvTime: TextView,
        heightPx: Int,
        widthPx: Int,
        density: Float
    ) {
        val isShortCard = heightPx < (SHORT_CARD_MAX_HEIGHT_DP * density)
        val isNarrowCard = widthPx < (INLINE_TIME_MIN_WIDTH_DP * density)
        val showTime = !(isShortCard && isNarrowCard)

        val titleParams = tvTitle.layoutParams as LinearLayout.LayoutParams
        val timeParams = tvTime.layoutParams as LinearLayout.LayoutParams

        textContainer.orientation = LinearLayout.HORIZONTAL
        textContainer.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START

        val gapPx = (6 * density).toInt()
        val textAreaWidth = (widthPx - (18 * density).toInt()).coerceAtLeast(1)

        titleParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        titleParams.weight = 0f
        titleParams.marginEnd = 0
        tvTitle.maxLines = 1
        tvTitle.ellipsize = android.text.TextUtils.TruncateAt.END

        if (showTime) {
            tvTime.visibility = View.VISIBLE
            val timeMaxWidth = (textAreaWidth * 0.45f).toInt().coerceAtLeast((56 * density).toInt())
            tvTime.maxWidth = timeMaxWidth
            timeParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            timeParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            timeParams.weight = 0f
            timeParams.marginStart = gapPx
            timeParams.topMargin = 0
            tvTime.layoutParams = timeParams

            titleParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            tvTitle.maxWidth = (textAreaWidth - timeMaxWidth - gapPx).coerceAtLeast((40 * density).toInt())
            tvTitle.layoutParams = titleParams
        } else {
            tvTime.visibility = View.GONE
            tvTime.maxWidth = Int.MAX_VALUE
            timeParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            timeParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            timeParams.weight = 0f
            timeParams.marginStart = 0
            timeParams.topMargin = 0
            tvTime.layoutParams = timeParams

            titleParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            tvTitle.maxWidth = Int.MAX_VALUE
            tvTitle.layoutParams = titleParams
        }
    }

    private fun applyEventColor(
        event: CalendarEvent,
        eventView: View,
        tvTitle: TextView,
        colorBar: View
    ) {
        val hex = event.categoryId?.let { categoryColors[it] } ?: DEFAULT_EVENT_COLOR
        val colorInt = try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            Color.parseColor(DEFAULT_EVENT_COLOR)
        }
        colorBar.setBackgroundColor(colorInt)
        val bodyFill = lightenColor(colorInt, 0.72f)
        (eventView as? com.google.android.material.card.MaterialCardView)
            ?.setCardBackgroundColor(bodyFill)
        tvTitle.setTextColor(darkenColor(colorInt, 0.35f))
        eventView.findViewById<TextView>(R.id.tvEventTime)
            ?.setTextColor(darkenColor(colorInt, 0.2f))
    }

    private fun computeEventColumns(events: List<CalendarEvent>): Map<Long, Int> {
        val sorted = events.sortedWith(compareBy({ it.startTime }, { it.id }))
        val columnsByIndex = EventLayoutCalculator.assignColumns(sorted)
        return sorted.mapIndexed { index, event ->
            event.id to (columnsByIndex[index] ?: 0)
        }.toMap()
    }

    private fun columnCountForEvent(event: CalendarEvent): Int {
        val overlapping = allEvents.filter { candidate ->
            EventLayoutCalculator.eventsOverlap(candidate, event)
        }
        if (overlapping.isEmpty()) return 1
        return overlapping.maxOf { eventColumnsById[it.id] ?: 0 } + 1
    }

    fun refreshCurrentTimeIndicator() {
        val now = Calendar.getInstance()
        val isToday = now.get(Calendar.YEAR) == baseDate.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == baseDate.get(Calendar.DAY_OF_YEAR)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        hourRows.forEach { row ->
            if (!isToday || row.hour != currentHour) {
                row.currentTimeIndicator.visibility = View.GONE
                return@forEach
            }
            row.currentTimeIndicator.visibility = View.VISIBLE
            row.currentTimeIndicator.post {
                val slotHeight = row.eventContentArea.height
                if (slotHeight <= 0) return@post
                val topMargin = (slotHeight * (currentMinute / 60f)).toInt()
                val params = row.currentTimeIndicator.layoutParams as FrameLayout.LayoutParams
                params.topMargin = topMargin
                row.currentTimeIndicator.layoutParams = params
            }
        }
    }

    fun scrollToHour(hour: Int) {
        val row = hourRows.getOrNull(hour.coerceIn(0, 23)) ?: return
        scrollView.post {
            scrollView.scrollTo(0, row.root.top)
        }
    }

    companion object {
        private const val SHORT_CARD_MAX_HEIGHT_DP = 45
        private const val INLINE_TIME_MIN_WIDTH_DP = 148
        private const val DEFAULT_EVENT_COLOR = "#9E9E9E"
        private const val HANDLE_ZONE_MINUTES = 20
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val newRed = (red + (255 - red) * factor).toInt()
        val newGreen = (green + (255 - green) * factor).toInt()
        val newBlue = (blue + (255 - blue) * factor).toInt()
        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val red = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val green = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val blue = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(red, green, blue)
    }
}
