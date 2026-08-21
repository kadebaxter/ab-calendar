package com.example.calendarnotes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DaySelectorAdapter(
    private val onDayClick: (Calendar) -> Unit
) : RecyclerView.Adapter<DaySelectorAdapter.DayViewHolder>() {

    private val days = mutableListOf<Calendar>()
    private var selectedDayStartMillis: Long = startOfDayMillis(Calendar.getInstance())
    private var todayStartMillis: Long = startOfDayMillis(Calendar.getInstance())

    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.daySelectorItem)
        val tvDayName: TextView = view.findViewById(R.id.tvDayName)
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_selector, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        val dayStart = startOfDayMillis(day)
        val isSelected = dayStart == selectedDayStartMillis
        val isToday = dayStart == todayStartMillis

        holder.tvDayName.text = dayNameFormat.format(day.time)
        holder.tvDayNumber.text = day.get(Calendar.DAY_OF_MONTH).toString()
        holder.container.isSelected = isSelected
        // Soft blue tint for today; selected outline takes priority in the drawable.
        holder.container.isActivated = isToday

        holder.container.setOnClickListener {
            onDayClick(day.clone() as Calendar)
        }
    }

    override fun getItemCount(): Int = days.size

    fun setDateRange(centerDate: Calendar, dayRadius: Int = DAY_RADIUS) {
        todayStartMillis = startOfDayMillis(Calendar.getInstance())
        days.clear()
        val start = centerDate.clone() as Calendar
        start.add(Calendar.DAY_OF_MONTH, -dayRadius)
        startOfDay(start)

        for (offset in 0..(dayRadius * 2)) {
            val day = start.clone() as Calendar
            day.add(Calendar.DAY_OF_MONTH, offset)
            days.add(day)
        }
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: Calendar): Int {
        val previousSelected = selectedDayStartMillis
        selectedDayStartMillis = startOfDayMillis(date)

        val previousIndex = indexOfDay(previousSelected)
        val selectedIndex = indexOfDay(selectedDayStartMillis)

        if (previousIndex >= 0) notifyItemChanged(previousIndex)
        if (selectedIndex >= 0) notifyItemChanged(selectedIndex)

        return selectedIndex
    }

    fun indexOfSelected(): Int = indexOfDay(selectedDayStartMillis)

    fun containsDate(date: Calendar): Boolean = indexOfDay(startOfDayMillis(date)) >= 0

    private fun indexOfDay(dayStartMillis: Long): Int {
        return days.indexOfFirst { startOfDayMillis(it) == dayStartMillis }
    }

    companion object {
        const val DAY_RADIUS = 60

        fun startOfDay(calendar: Calendar) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        fun startOfDayMillis(calendar: Calendar): Long {
            val copy = calendar.clone() as Calendar
            startOfDay(copy)
            return copy.timeInMillis
        }
    }
}
