package com.example.calendarnotes.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.calendarnotes.ui.fragments.CalendarFragment
import com.example.calendarnotes.ui.fragments.CategoriesFragment
import com.example.calendarnotes.ui.fragments.NotesFragment
import com.example.calendarnotes.ui.fragments.TodosFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CalendarFragment()
            1 -> TodosFragment()
            2 -> NotesFragment()
            3 -> CategoriesFragment()
            else -> CalendarFragment()
        }
    }
}
