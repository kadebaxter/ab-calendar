package com.example.calendarnotes.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.ui.adapters.DayScheduleAdapter
import com.example.calendarnotes.ui.adapters.MonthCalendarAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var rvDaySchedule: RecyclerView
    private lateinit var dayScheduleAdapter: DayScheduleAdapter
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvCurrentYear: TextView
    private lateinit var btnPreviousDay: ImageButton
    private lateinit var btnNextDay: ImageButton
    private lateinit var btnShowCalendar: ImageButton
    private var currentDate: Calendar = Calendar.getInstance()
    
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

        rvDaySchedule = view.findViewById(R.id.rvDaySchedule)
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate)
        tvCurrentYear = view.findViewById(R.id.tvCurrentYear)
        btnPreviousDay = view.findViewById(R.id.btnPreviousDay)
        btnNextDay = view.findViewById(R.id.btnNextDay)
        btnShowCalendar = view.findViewById(R.id.btnShowCalendar)

        setupDaySchedule()
        setupNavigation()
        updateDateDisplay()
        loadEventsForDate()

        observeViewModel()
    }

    private fun setupDaySchedule() {
        dayScheduleAdapter = DayScheduleAdapter(
            recyclerView = rvDaySchedule,
            baseDate = currentDate,
            onTimeSlotClick = { hour ->
                showAddEventDialog(hour)
            },
            onEventClick = { event ->
                showEventDetailsDialog(event)
            },
            onEventTimeChanged = { event, newStartTime, newEndTime ->
                handleEventTimeChanged(event, newStartTime, newEndTime)
            }
        )
        rvDaySchedule.layoutManager = LinearLayoutManager(requireContext())
        rvDaySchedule.adapter = dayScheduleAdapter
    }
    
    private fun handleEventTimeChanged(event: CalendarEvent, newStartTime: Long, newEndTime: Long) {
        // Update the event in the database
        viewModel.updateEventTime(event.id, newStartTime, newEndTime)
        // Reload events to refresh the view
        loadEventsForDate()
    }

    private fun setupNavigation() {
        btnPreviousDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateDisplay()
            loadEventsForDate()
        }

        btnNextDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateDisplay()
            loadEventsForDate()
        }

        btnShowCalendar.setOnClickListener {
            showMonthCalendarDialog()
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        
        tvCurrentDate.text = dateFormat.format(currentDate.time)
        tvCurrentYear.text = yearFormat.format(currentDate.time)
    }

    private fun loadEventsForDate() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        viewModel.getEventsForDateRange(startOfDay, endOfDay) { events ->
            updateDaySchedule(events)
        }
    }

    private fun updateDaySchedule(events: List<CalendarEvent>) {
        val categoryColors = viewModel.categories.value?.associate { it.id to it.color } ?: emptyMap()
        dayScheduleAdapter.updateSchedule(events, categoryColors)
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

        var displayMonth = currentDate.clone() as Calendar
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        val monthCalendarAdapter = MonthCalendarAdapter { selectedDate ->
            currentDate = selectedDate
            updateDateDisplay()
            loadEventsForDate()
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
                eventsMap
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

    private fun showEventDetailsDialog(event: CalendarEvent) {
        val timeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
        val startTime = timeFormat.format(Date(event.startTime))
        val endTime = timeFormat.format(Date(event.endTime))
        
        AlertDialog.Builder(requireContext())
            .setTitle(event.title)
            .setMessage("${event.description}\n\nStart: $startTime\nEnd: $endTime")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCalendarEvent(event.id)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddEventDialog(hour: Int = -1) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEventTitle)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEventDescription)
        val btnStartDate = dialogView.findViewById<android.widget.Button>(R.id.btnStartDate)
        val btnStartTime = dialogView.findViewById<android.widget.Button>(R.id.btnStartTime)
        val btnEndDate = dialogView.findViewById<android.widget.Button>(R.id.btnEndDate)
        val btnEndTime = dialogView.findViewById<android.widget.Button>(R.id.btnEndTime)
        val spinnerCategory = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerCategory)

        // Use current date and clicked hour, or default to current time
        var startTime = currentDate.clone() as Calendar
        if (hour >= 0) {
            startTime.set(Calendar.HOUR_OF_DAY, hour)
            startTime.set(Calendar.MINUTE, 0)
        }
        
        // Set end time to 1 hour after start time
        var endTime = startTime.clone() as Calendar
        endTime.add(Calendar.HOUR_OF_DAY, 1)

        // Initialize button texts
        btnStartDate.text = SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(startTime.time)
        btnStartTime.text = String.format("%02d:%02d", startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE))
        btnEndDate.text = SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(endTime.time)
        btnEndTime.text = String.format("%02d:%02d", endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE))

        // Setup category spinner
        val categories = viewModel.categories.value ?: emptyList()
        val categoryNames = listOf("None") + categories.map { it.name }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        btnStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    startTime.set(year, month, day)
                    btnStartDate.text = "${month + 1}/$day/$year"
                },
                startTime.get(Calendar.YEAR),
                startTime.get(Calendar.MONTH),
                startTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnStartTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    startTime.set(Calendar.HOUR_OF_DAY, hour)
                    startTime.set(Calendar.MINUTE, minute)
                    btnStartTime.text = String.format("%02d:%02d", hour, minute)
                },
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE),
                false
            ).show()
        }

        btnEndDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    endTime.set(year, month, day)
                    btnEndDate.text = "${month + 1}/$day/$year"
                },
                endTime.get(Calendar.YEAR),
                endTime.get(Calendar.MONTH),
                endTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnEndTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    endTime.set(Calendar.HOUR_OF_DAY, hour)
                    endTime.set(Calendar.MINUTE, minute)
                    btnEndTime.text = String.format("%02d:%02d", hour, minute)
                },
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString()
                val description = etDescription.text.toString()
                val selectedPosition = spinnerCategory.selectedItemPosition
                val categoryId = if (selectedPosition > 0 && categories.isNotEmpty()) {
                    categories[selectedPosition - 1].id
                } else {
                    null
                }
                if (title.isNotBlank()) {
                    viewModel.addCalendarEvent(title, description, startTime.timeInMillis, endTime.timeInMillis, categoryId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
