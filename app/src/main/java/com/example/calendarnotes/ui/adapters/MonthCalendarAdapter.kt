package com.example.calendarnotes.ui.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import java.util.Calendar

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
        val dayCell: View = view.findViewById(R.id.dayCell)
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
        val eventIndicator: View = view.findViewById(R.id.eventIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        holder.tvDayNumber.text = day.dayOfMonth.toString()
        holder.tvDayNumber.alpha = if (day.isCurrentMonth) 1.0f else 0.35f
        holder.tvDayNumber.setTypeface(null, if (day.isToday || day.isSelected) Typeface.BOLD else Typeface.NORMAL)

        // Same semantics as day strip: activated = today fill, selected = cyan border
        holder.dayCell.isActivated = day.isToday
        holder.dayCell.isSelected = day.isSelected

        holder.eventIndicator.visibility =
            if (day.hasEvents && day.isCurrentMonth) View.VISIBLE else View.GONE

        holder.dayCell.setOnClickListener {
            onDayClick(day.date)
        }
    }

    override fun getItemCount() = days.size

    fun updateCalendar(
        year: Int,
        month: Int,
        selectedDate: Calendar,
        eventsMap: Map<String, Boolean>,
        weekStartDay: Int = Calendar.SUNDAY
    ) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)

        val firstDow = calendar.get(Calendar.DAY_OF_WEEK)
        val leadingDays = (firstDow - weekStartDay + 7) % 7
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val prevMonthCalendar = calendar.clone() as Calendar
        prevMonthCalendar.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayStart = DaySelectorAdapter.startOfDayMillis(Calendar.getInstance())
        val selectedStart = DaySelectorAdapter.startOfDayMillis(selectedDate)
        val daysList = mutableListOf<CalendarDay>()

        for (i in 0 until leadingDays) {
            val dayNum = daysInPrevMonth - leadingDays + i + 1
            val dayCalendar = Calendar.getInstance()
            dayCalendar.set(
                prevMonthCalendar.get(Calendar.YEAR),
                prevMonthCalendar.get(Calendar.MONTH),
                dayNum
            )
            DaySelectorAdapter.startOfDay(dayCalendar)
            val dayStart = dayCalendar.timeInMillis

            daysList.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    isCurrentMonth = false,
                    isToday = dayStart == todayStart,
                    isSelected = dayStart == selectedStart,
                    hasEvents = false,
                    date = dayCalendar
                )
            )
        }

        for (day in 1..daysInMonth) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.set(year, month, day)
            DaySelectorAdapter.startOfDay(dayCalendar)
            val dayStart = dayCalendar.timeInMillis

            val dateKey = "$year-${month + 1}-$day"
            daysList.add(
                CalendarDay(
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = dayStart == todayStart,
                    isSelected = dayStart == selectedStart,
                    hasEvents = eventsMap[dateKey] == true,
                    date = dayCalendar
                )
            )
        }

        val remainingDays = 42 - daysList.size
        val nextMonthCalendar = calendar.clone() as Calendar
        nextMonthCalendar.add(Calendar.MONTH, 1)

        for (day in 1..remainingDays) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.set(
                nextMonthCalendar.get(Calendar.YEAR),
                nextMonthCalendar.get(Calendar.MONTH),
                day
            )
            DaySelectorAdapter.startOfDay(dayCalendar)
            val dayStart = dayCalendar.timeInMillis

            daysList.add(
                CalendarDay(
                    dayOfMonth = day,
                    isCurrentMonth = false,
                    isToday = dayStart == todayStart,
                    isSelected = dayStart == selectedStart,
                    hasEvents = false,
                    date = dayCalendar
                )
            )
        }

        days = daysList
        notifyDataSetChanged()
    }
}
