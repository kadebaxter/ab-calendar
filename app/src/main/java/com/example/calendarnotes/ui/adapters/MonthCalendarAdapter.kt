package com.example.calendarnotes.ui.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import java.util.*

data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasEvents: Boolean,
    val date: Calendar
)

class MonthCalendarAdapter(
    private val onDayClick: (Calendar) -> Unit
) : RecyclerView.Adapter<MonthCalendarAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
        val eventIndicator: View = view.findViewById(R.id.eventIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        if (day.isCurrentMonth) {
            holder.tvDayNumber.text = day.dayOfMonth.toString()
            holder.tvDayNumber.alpha = 1.0f
        } else {
            holder.tvDayNumber.text = day.dayOfMonth.toString()
            holder.tvDayNumber.alpha = 0.3f
        }

        // Highlight today
        if (day.isToday) {
            holder.tvDayNumber.setBackgroundColor(Color.parseColor("#FFB3E5FC"))
            holder.tvDayNumber.setTextColor(Color.WHITE)
            holder.tvDayNumber.setTypeface(null, Typeface.BOLD)
        } else if (day.isSelected) {
            holder.tvDayNumber.setBackgroundColor(Color.parseColor("#FFB3E5FC"))
            holder.tvDayNumber.setTextColor(Color.WHITE)
            holder.tvDayNumber.setTypeface(null, Typeface.BOLD)
        } else {
            holder.tvDayNumber.setBackgroundColor(Color.TRANSPARENT)
            holder.tvDayNumber.setTextColor(Color.WHITE)
            holder.tvDayNumber.setTypeface(null, Typeface.NORMAL)
        }

        // Show event indicator
        if (day.hasEvents && day.isCurrentMonth) {
            holder.eventIndicator.visibility = View.VISIBLE
        } else {
            holder.eventIndicator.visibility = View.GONE
        }

        holder.tvDayNumber.setOnClickListener {
            onDayClick(day.date)
        }
    }

    override fun getItemCount() = days.size

    fun updateCalendar(year: Int, month: Int, selectedDate: Calendar, eventsMap: Map<String, Boolean>) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Get previous month info
        val prevMonthCalendar = calendar.clone() as Calendar
        prevMonthCalendar.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val daysList = mutableListOf<CalendarDay>()

        // Add days from previous month
        for (i in 0 until firstDayOfWeek) {
            val dayNum = daysInPrevMonth - firstDayOfWeek + i + 1
            val dayCalendar = Calendar.getInstance()
            dayCalendar.set(prevMonthCalendar.get(Calendar.YEAR), prevMonthCalendar.get(Calendar.MONTH), dayNum)
            
            daysList.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    isCurrentMonth = false,
                    isToday = false,
                    isSelected = false,
                    hasEvents = false,
                    date = dayCalendar
                )
            )
        }

        // Add days from current month
        for (day in 1..daysInMonth) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.set(year, month, day)
            
            val isToday = today.get(Calendar.YEAR) == year &&
                    today.get(Calendar.MONTH) == month &&
                    today.get(Calendar.DAY_OF_MONTH) == day
            
            val isSelected = selectedDate.get(Calendar.YEAR) == year &&
                    selectedDate.get(Calendar.MONTH) == month &&
                    selectedDate.get(Calendar.DAY_OF_MONTH) == day
            
            val dateKey = "$year-${month + 1}-$day"
            val hasEvents = eventsMap[dateKey] == true
            
            daysList.add(
                CalendarDay(
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = isToday,
                    isSelected = isSelected,
                    hasEvents = hasEvents,
                    date = dayCalendar
                )
            )
        }

        // Add days from next month to fill the grid
        val remainingDays = 42 - daysList.size // 6 rows * 7 days
        val nextMonthCalendar = calendar.clone() as Calendar
        nextMonthCalendar.add(Calendar.MONTH, 1)
        
        for (day in 1..remainingDays) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.set(nextMonthCalendar.get(Calendar.YEAR), nextMonthCalendar.get(Calendar.MONTH), day)
            
            daysList.add(
                CalendarDay(
                    dayOfMonth = day,
                    isCurrentMonth = false,
                    isToday = false,
                    isSelected = false,
                    hasEvents = false,
                    date = dayCalendar
                )
            )
        }

        days = daysList
        notifyDataSetChanged()
    }
}
