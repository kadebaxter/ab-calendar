package com.example.calendarnotes.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.MainActivity
import com.example.calendarnotes.R
import com.example.calendarnotes.data.AppPreferences
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.data.models.PersonOrdering
import com.example.calendarnotes.ui.AddEditEventActivity
import com.example.calendarnotes.ui.DayScheduleScrollView
import com.example.calendarnotes.ui.EventDetailActivity
import com.example.calendarnotes.ui.HasOverflowMenu
import com.example.calendarnotes.ui.adapters.AgendaEventAdapter
import com.example.calendarnotes.ui.adapters.DayScheduleAdapter
import com.example.calendarnotes.ui.adapters.DaySelectorAdapter
import com.example.calendarnotes.ui.adapters.MonthCalendarAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.example.calendarnotes.viewmodel.GoogleCalendarSyncResult
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class CalendarViewMode { DAY, WEEK, MONTH }

class CalendarFragment : Fragment(), HasOverflowMenu {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var prefs: AppPreferences
    private lateinit var dayViewContainer: View
    private lateinit var weekMonthContainer: View
    private lateinit var svDaySchedule: DayScheduleScrollView
    private lateinit var llDaySchedule: LinearLayout
    private lateinit var rvDaySelector: RecyclerView
    private lateinit var llAllDayEvents: LinearLayout
    private lateinit var rvAgenda: RecyclerView
    private lateinit var rvMonthGrid: RecyclerView
    private lateinit var monthViewContainer: View
    private lateinit var weekdayHeaderRow: LinearLayout
    private lateinit var tvAgendaEmpty: TextView
    private lateinit var dayScheduleAdapter: DayScheduleAdapter
    private lateinit var daySelectorAdapter: DaySelectorAdapter
    private lateinit var daySelectorLayoutManager: LinearLayoutManager
    private lateinit var agendaAdapter: AgendaEventAdapter
    private lateinit var monthGridAdapter: MonthCalendarAdapter
    private var currentDate: Calendar = Calendar.getInstance()
    private var ignoreNextTimeSlotClick = false
    private var isAnimatingDayChange = false
    private var shouldScrollDayToMorning = true
    private var viewMode: CalendarViewMode = CalendarViewMode.DAY
    private var filterCategoryId: Long? = null
    private var filterPersonId: Long? = null
    private var personEventIds: Set<Long> = emptySet()

    // Handler for updating current time indicator
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            dayScheduleAdapter.refreshCurrentTimeIndicator()
            timeUpdateHandler.postDelayed(this, 60000) // Update every minute
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CalendarNotesViewModel::class.java]
        prefs = AppPreferences(requireContext())

        dayViewContainer = view.findViewById(R.id.dayViewContainer)
        weekMonthContainer = view.findViewById(R.id.weekMonthContainer)
        svDaySchedule = view.findViewById(R.id.svDaySchedule)
        llDaySchedule = view.findViewById(R.id.llDaySchedule)
        rvDaySelector = view.findViewById(R.id.rvDaySelector)
        llAllDayEvents = view.findViewById(R.id.llAllDayEvents)
        rvAgenda = view.findViewById(R.id.rvAgenda)
        rvMonthGrid = view.findViewById(R.id.rvMonthGrid)
        monthViewContainer = view.findViewById(R.id.monthViewContainer)
        weekdayHeaderRow = view.findViewById(R.id.weekdayHeaderRow)
        tvAgendaEmpty = view.findViewById(R.id.tvAgendaEmpty)

        DaySelectorAdapter.startOfDay(currentDate)

        setupDaySchedule()
        setupDaySelector()
        setupAgendaAndMonth()
        setupDaySwipeGesture()
        applyViewMode()
        selectDate(currentDate, scrollDaySelector = true, smoothScroll = false)
        (activity as? MainActivity)?.syncCalendarViewMode(viewMode)

        observeViewModel()
    }

    private fun setupAgendaAndMonth() {
        agendaAdapter = AgendaEventAdapter { event -> openEventDetail(event.id) }
        rvAgenda.layoutManager = LinearLayoutManager(requireContext())
        rvAgenda.adapter = agendaAdapter

        monthGridAdapter = MonthCalendarAdapter { selectedDate ->
            selectDate(selectedDate, scrollDaySelector = true, smoothScroll = false)
            if (viewMode == CalendarViewMode.MONTH) {
                loadEventsForDate()
            }
        }
        rvMonthGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvMonthGrid.adapter = monthGridAdapter
        rebuildWeekdayHeaders()
    }

    private fun rebuildWeekdayHeaders() {
        weekdayHeaderRow.removeAllViews()
        val labels = weekdayLabels(prefs.weekStartDay)
        labels.forEach { label ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = label
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                textSize = 12f
            }
            weekdayHeaderRow.addView(tv)
        }
    }

    private fun weekdayLabels(weekStartDay: Int): List<String> {
        val all = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val startIndex = if (weekStartDay == Calendar.MONDAY) 1 else 0
        return all.drop(startIndex) + all.take(startIndex)
    }

    override fun showOverflowMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_calendar_overflow, menu)
            menu.findItem(R.id.action_clear_filters)?.isVisible =
                filterCategoryId != null || filterPersonId != null
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_filter_category -> {
                        showCategoryFilterDialog()
                        true
                    }
                    R.id.action_filter_person -> {
                        showPersonFilterDialog()
                        true
                    }
                    R.id.action_clear_filters -> {
                        filterCategoryId = null
                        filterPersonId = null
                        personEventIds = emptySet()
                        loadEventsForDate()
                        true
                    }
                    R.id.action_sync_google -> {
                        syncGoogleCalendar(force = true)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    /** Bottom-nav Calendar tap: day view on today (replaces Go to today / Jump to date). */
    fun showDayViewHome() {
        shouldScrollDayToMorning = true
        setCalendarViewMode(CalendarViewMode.DAY)
        selectDate(Calendar.getInstance(), scrollDaySelector = true, smoothScroll = true)
    }

    fun currentViewMode(): CalendarViewMode = viewMode

    fun setCalendarViewMode(mode: CalendarViewMode) {
        if (viewMode == mode) {
            (activity as? MainActivity)?.syncCalendarViewMode(mode)
            return
        }
        viewMode = mode
        applyViewMode()
        loadEventsForDate()
        (activity as? MainActivity)?.syncCalendarViewMode(mode)
    }

    private fun applyViewMode() {
        val day = viewMode == CalendarViewMode.DAY
        val week = viewMode == CalendarViewMode.WEEK
        val month = viewMode == CalendarViewMode.MONTH

        // Separate containers avoid ConstraintLayout GONE leftovers when
        // switching Week/Month back to Day (header/day-strip stuck at top).
        dayViewContainer.visibility = if (day) View.VISIBLE else View.GONE
        weekMonthContainer.visibility = if (week || month) View.VISIBLE else View.GONE
        monthViewContainer.visibility = if (month) View.VISIBLE else View.GONE
        tvAgendaEmpty.visibility = View.GONE

        if (day) {
            // Reset any in-progress swipe animation transforms.
            svDaySchedule.animate().cancel()
            svDaySchedule.translationX = 0f
            svDaySchedule.alpha = 1f
            isAnimatingDayChange = false
        }
        if (month) {
            rebuildWeekdayHeaders()
            refreshMonthGrid()
        }
    }

    private fun showCategoryFilterDialog() {
        val categories = viewModel.categories.value.orEmpty()
        val labels = mutableListOf(getString(R.string.all_categories))
        labels.addAll(categories.map { it.name })
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_filter_category)
            .setItems(labels.toTypedArray()) { _, which ->
                filterCategoryId = if (which == 0) null else categories[which - 1].id
                loadEventsForDate()
            }
            .show()
    }

    private fun showPersonFilterDialog() {
        val people = PersonOrdering.sorted(
            people = viewModel.people.value.orEmpty(),
            lastEventByPersonId = viewModel.lastEventByPersonId.value.orEmpty()
        )
        val labels = mutableListOf(getString(R.string.all_people))
        labels.addAll(people.map { it.name })
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_filter_person)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == 0) {
                    filterPersonId = null
                    personEventIds = emptySet()
                    loadEventsForDate()
                } else {
                    val person = people[which - 1]
                    filterPersonId = person.id
                    viewModel.getEventsForPerson(person.id) { events ->
                        if (!isAdded) return@getEventsForPerson
                        personEventIds = events.map { it.id }.toSet()
                        loadEventsForDate()
                    }
                }
            }
            .show()
    }

    private fun setupDaySchedule() {
        dayScheduleAdapter = DayScheduleAdapter(
            scrollView = svDaySchedule,
            container = llDaySchedule,
            baseDate = currentDate,
            onTimeSlotClick = { hour ->
                if (ignoreNextTimeSlotClick) {
                    ignoreNextTimeSlotClick = false
                } else {
                    openAddEvent(hour)
                }
            },
            onEventClick = { event ->
                openEventDetail(event.id)
            },
            onEventTimeChanged = { event, newStartTime, newEndTime ->
                handleEventTimeChanged(event, newStartTime, newEndTime)
            }
        )
    }

    private fun setupDaySelector() {
        daySelectorAdapter = DaySelectorAdapter { selectedDay ->
            selectDate(selectedDay, scrollDaySelector = true, smoothScroll = true)
        }
        daySelectorLayoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvDaySelector.layoutManager = daySelectorLayoutManager
        rvDaySelector.adapter = daySelectorAdapter
        daySelectorAdapter.setDateRange(currentDate)
    }
    
    private fun handleEventTimeChanged(event: CalendarEvent, newStartTime: Long, newEndTime: Long) {
        if (event.isFromGoogle) {
            Toast.makeText(requireContext(), R.string.google_event_readonly_hint, Toast.LENGTH_SHORT).show()
            loadEventsForDate()
            return
        }
        viewModel.updateEventTime(event.id, newStartTime, newEndTime)
        loadEventsForDate()
    }

    private fun syncGoogleCalendar(force: Boolean) {
        if (!viewModel.isGoogleCalendarConnected()) {
            Toast.makeText(requireContext(), R.string.settings_google_not_connected, Toast.LENGTH_SHORT).show()
            return
        }
        if (force) {
            Toast.makeText(requireContext(), R.string.google_sync_in_progress, Toast.LENGTH_SHORT).show()
        }
        val callback = fun(result: GoogleCalendarSyncResult) {
            if (!isAdded) return
            if (result.success) {
                if (force || result.inserted + result.updated + result.removed > 0) {
                    if (result.message != "Recently synced") {
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.google_sync_success,
                                result.inserted,
                                result.updated,
                                result.removed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (force && result.message != "Auto-sync is off") {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.google_sync_failed, result.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        if (force) {
            viewModel.syncGoogleCalendar(callback)
        } else {
            viewModel.syncGoogleCalendarIfDue(force = false, onResult = callback)
        }
    }

    fun openMonthPicker() {
        showMonthCalendarDialog()
    }

    private fun selectDate(
        date: Calendar,
        scrollDaySelector: Boolean = true,
        smoothScroll: Boolean = true
    ) {
        // Keep the same Calendar instance so DayScheduleAdapter's baseDate stays in sync.
        currentDate.timeInMillis = date.timeInMillis
        DaySelectorAdapter.startOfDay(currentDate)

        if (!daySelectorAdapter.containsDate(currentDate)) {
            daySelectorAdapter.setDateRange(currentDate)
        }

        val selectedIndex = daySelectorAdapter.setSelectedDate(currentDate)
        updateDateDisplay()
        loadEventsForDate()

        if (scrollDaySelector && selectedIndex >= 0) {
            scrollDaySelectorTo(selectedIndex, smoothScroll)
        }
    }

    private fun scrollDaySelectorTo(index: Int, smoothScroll: Boolean) {
        rvDaySelector.post {
            if (!isAdded) return@post
            val itemWidth = resources.getDimensionPixelSize(R.dimen.day_selector_item_width)
            val offset = ((rvDaySelector.width - itemWidth) / 2).coerceAtLeast(0)
            if (smoothScroll) {
                rvDaySelector.smoothScrollToPosition(index)
            } else {
                daySelectorLayoutManager.scrollToPositionWithOffset(index, offset)
            }
        }
    }

    private fun changeDayFromSwipe(deltaDays: Int) {
        if (viewMode != CalendarViewMode.DAY || isAnimatingDayChange) return

        // Suppress the time-slot click that can fire at the end of the same gesture.
        ignoreNextTimeSlotClick = true
        isAnimatingDayChange = true

        val next = currentDate.clone() as Calendar
        next.add(Calendar.DAY_OF_MONTH, deltaDays)

        val width = svDaySchedule.width.toFloat().coerceAtLeast(1f)
        // Swipe left (next day): current content exits left; swipe right exits right.
        val exitX = if (deltaDays > 0) -width else width

        svDaySchedule.animate().cancel()
        svDaySchedule.animate()
            .translationX(exitX)
            .alpha(0.65f)
            .setDuration(DAY_SWIPE_ANIM_MS)
            .withEndAction {
                if (!isAdded) {
                    isAnimatingDayChange = false
                    ignoreNextTimeSlotClick = false
                    return@withEndAction
                }
                selectDate(next, scrollDaySelector = true, smoothScroll = true)
                svDaySchedule.translationX = -exitX
                svDaySchedule.alpha = 0.65f
                svDaySchedule.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(DAY_SWIPE_ANIM_MS)
                    .withEndAction {
                        isAnimatingDayChange = false
                        ignoreNextTimeSlotClick = false
                    }
                    .start()
            }
            .start()
    }

    private fun setupDaySwipeGesture() {
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        var startX = 0f
        var startY = 0f
        var trackingHorizontalSwipe = false
        var dayChangedThisGesture = false

        fun maybeChangeDay(endX: Float) {
            if (dayChangedThisGesture) return
            val dx = endX - startX
            if (abs(dx) < SWIPE_DISTANCE_THRESHOLD) return
            dayChangedThisGesture = true
            // Swipe right -> previous day; swipe left -> next day
            changeDayFromSwipe(if (dx > 0) -1 else 1)
        }

        val gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null || dayChangedThisGesture) return false

                    val dx = e2.x - e1.x
                    val dy = e2.y - e1.y
                    if (abs(dx) < SWIPE_DISTANCE_THRESHOLD ||
                        abs(dx) < abs(dy) ||
                        abs(velocityX) < SWIPE_VELOCITY_THRESHOLD
                    ) {
                        return false
                    }

                    dayChangedThisGesture = true
                    changeDayFromSwipe(if (dx > 0) -1 else 1)
                    return true
                }
            }
        )

        // Intercept horizontal day swipes before hour rows; vertical scroll stays native.
        // Event drag calls requestDisallowInterceptTouchEvent so this won't steal it.
        svDaySchedule.interceptTouch = { e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.x
                    startY = e.y
                    trackingHorizontalSwipe = false
                    dayChangedThisGesture = false
                    gestureDetector.onTouchEvent(e)
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    gestureDetector.onTouchEvent(e)
                    if (!trackingHorizontalSwipe) {
                        val dx = abs(e.x - startX)
                        val dy = abs(e.y - startY)
                        if (dx > touchSlop && dx > dy) {
                            trackingHorizontalSwipe = true
                        }
                    }
                    trackingHorizontalSwipe
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!trackingHorizontalSwipe) {
                        gestureDetector.onTouchEvent(e)
                    }
                    trackingHorizontalSwipe
                }

                else -> false
            }
        }

        svDaySchedule.handleTouch = { e ->
            if (!trackingHorizontalSwipe) {
                false
            } else {
                gestureDetector.onTouchEvent(e)
                when (e.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        maybeChangeDay(e.x)
                        trackingHorizontalSwipe = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        trackingHorizontalSwipe = false
                        true
                    }
                    else -> true
                }
            }
        }
    }

    companion object {
        private const val SWIPE_DISTANCE_THRESHOLD = 120
        private const val SWIPE_VELOCITY_THRESHOLD = 200
        private const val DAY_SWIPE_ANIM_MS = 160L
        private const val MORNING_SCROLL_HOUR = 6
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val title = dateFormat.format(currentDate.time)
        (activity as? MainActivity)?.updateCalendarHeaderTitle(title)
    }

    private fun loadEventsForDate() {
        val (start, end) = when (viewMode) {
            CalendarViewMode.DAY -> dayRange(currentDate)
            CalendarViewMode.WEEK -> weekRange(currentDate)
            CalendarViewMode.MONTH -> dayRange(currentDate)
        }

        viewModel.getEventsForDateRange(start, end) { events ->
            if (!isAdded) return@getEventsForDateRange
            val filtered = applyFilters(events)
            when (viewMode) {
                CalendarViewMode.DAY -> updateDaySchedule(filtered)
                CalendarViewMode.WEEK -> {
                    agendaAdapter.submit(filtered)
                    tvAgendaEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                }
                CalendarViewMode.MONTH -> {
                    refreshMonthGrid()
                    agendaAdapter.submit(filtered)
                    tvAgendaEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun applyFilters(events: List<CalendarEvent>): List<CalendarEvent> {
        return events.filter { event ->
            val categoryOk = filterCategoryId == null || event.categoryId == filterCategoryId
            val personOk = filterPersonId == null || event.id in personEventIds
            categoryOk && personOk
        }
    }

    private fun dayRange(date: Calendar): Pair<Long, Long> {
        val start = date.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        val end = start.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        return start.timeInMillis to end.timeInMillis
    }

    private fun weekRange(date: Calendar): Pair<Long, Long> {
        val start = date.clone() as Calendar
        DaySelectorAdapter.startOfDay(start)
        val weekStart = prefs.weekStartDay
        while (start.get(Calendar.DAY_OF_WEEK) != weekStart) {
            start.add(Calendar.DAY_OF_MONTH, -1)
        }
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, 6)
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        return start.timeInMillis to end.timeInMillis
    }

    private fun refreshMonthGrid() {
        if (!::monthGridAdapter.isInitialized) return
        val eventsMap = getEventsMapForMonth(currentDate)
        monthGridAdapter.updateCalendar(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate,
            eventsMap,
            prefs.weekStartDay
        )
    }

    private fun updateDaySchedule(events: List<CalendarEvent>) {
        val categoryColors = viewModel.categories.value?.associate { it.id to it.color } ?: emptyMap()
        val allDay = events.filter { it.displaysAsAllDay() }
            .sortedWith(compareBy({ it.startTime }, { it.title.lowercase() }))
        val timed = events.filterNot { it.displaysAsAllDay() }
        bindAllDayStrip(allDay, categoryColors)
        dayScheduleAdapter.updateSchedule(timed, categoryColors)
        maybeScrollDayToMorning()
    }

    /** Puts 6:00 AM at the top of the day grid on first open / Calendar home. */
    private fun maybeScrollDayToMorning() {
        if (!shouldScrollDayToMorning || viewMode != CalendarViewMode.DAY) return
        shouldScrollDayToMorning = false
        if (!isAdded) return
        dayScheduleAdapter.scrollToHour(MORNING_SCROLL_HOUR)
    }

    private fun bindAllDayStrip(
        events: List<CalendarEvent>,
        categoryColors: Map<Long, String>
    ) {
        llAllDayEvents.removeAllViews()
        if (events.isEmpty()) {
            llAllDayEvents.visibility = View.GONE
            return
        }
        llAllDayEvents.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(requireContext())
        val radius = 6f * resources.displayMetrics.density
        events.forEach { event ->
            val bar = inflater.inflate(R.layout.item_all_day_event, llAllDayEvents, false) as TextView
            bar.text = event.title
            val color = try {
                Color.parseColor(categoryColors[event.categoryId] ?: "#9E9E9E")
            } catch (_: Exception) {
                Color.parseColor("#9E9E9E")
            }
            bar.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(color)
            }
            bar.setOnClickListener { openEventDetail(event.id) }
            llAllDayEvents.addView(bar)
        }
    }

    private fun observeViewModel() {
        viewModel.calendarEvents.observe(viewLifecycleOwner) {
            loadEventsForDate()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start updating the current time indicator every minute
        timeUpdateHandler.post(timeUpdateRunnable)
        if (::viewModel.isInitialized) {
            syncGoogleCalendar(force = false)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop updating when fragment is not visible
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
    }

    private fun showMonthCalendarDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_calendar, null)
        val tvMonthYear = dialogView.findViewById<TextView>(R.id.tvMonthYear)
        val btnPreviousMonth = dialogView.findViewById<ImageButton>(R.id.btnPreviousMonth)
        val btnNextMonth = dialogView.findViewById<ImageButton>(R.id.btnNextMonth)
        val rvCalendarGrid = dialogView.findViewById<RecyclerView>(R.id.rvCalendarGrid)
        val weekdayRow = dialogView.findViewById<LinearLayout>(R.id.weekdayHeaderRowDialog)

        weekdayRow.removeAllViews()
        weekdayLabels(prefs.weekStartDay).forEach { label ->
            weekdayRow.addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    text = label
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
            )
        }

        var displayMonth = currentDate.clone() as Calendar

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        val monthCalendarAdapter = MonthCalendarAdapter { selectedDate ->
            selectDate(selectedDate, scrollDaySelector = true, smoothScroll = false)
            dialog.dismiss()
        }

        rvCalendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendarGrid.adapter = monthCalendarAdapter

        fun updateMonthDisplay() {
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            tvMonthYear.text = monthFormat.format(displayMonth.time)

            // Get events for the month to show indicators
            val eventsMap = getEventsMapForMonth(displayMonth)
            monthCalendarAdapter.updateCalendar(
                displayMonth.get(Calendar.YEAR),
                displayMonth.get(Calendar.MONTH),
                currentDate,
                eventsMap,
                prefs.weekStartDay
            )
        }

        btnPreviousMonth.setOnClickListener {
            displayMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay()
        }

        btnNextMonth.setOnClickListener {
            displayMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay()
        }

        updateMonthDisplay()
        dialog.show()
    }

    private fun getEventsMapForMonth(month: Calendar): Map<String, Boolean> {
        val eventsMap = mutableMapOf<String, Boolean>()
        val allEvents = viewModel.calendarEvents.value ?: emptyList()
        
        for (event in allEvents) {
            val eventCalendar = Calendar.getInstance()
            eventCalendar.timeInMillis = event.startTime
            val key = "${eventCalendar.get(Calendar.YEAR)}-${eventCalendar.get(Calendar.MONTH) + 1}-${eventCalendar.get(Calendar.DAY_OF_MONTH)}"
            eventsMap[key] = true
        }
        
        return eventsMap
    }

    private fun openAddEvent(hour: Int = -1) {
        startActivity(
            AddEditEventActivity.createIntent(
                context = requireContext(),
                dayMillis = currentDate.timeInMillis,
                hour = hour
            )
        )
    }

    private fun openEventDetail(eventId: Long) {
        startActivity(EventDetailActivity.createIntent(requireContext(), eventId))
    }
}
