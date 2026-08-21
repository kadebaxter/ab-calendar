package com.example.calendarnotes.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.calendarnotes.ui.fragments.CalendarFragment
import com.example.calendarnotes.ui.fragments.NotesFragment
import com.example.calendarnotes.ui.fragments.PeopleFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CalendarFragment()
            1 -> NotesFragment()
            2 -> PeopleFragment()
            else -> CalendarFragment()
        }
    }
}
