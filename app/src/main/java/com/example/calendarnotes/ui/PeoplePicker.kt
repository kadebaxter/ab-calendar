package com.example.calendarnotes.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.Person

object PeoplePicker {
    fun createIntent(
        context: Context,
        people: List<Person>,
        initiallySelected: Set<Long>
    ): Intent? {
        if (people.isEmpty()) {
            Toast.makeText(context, R.string.add_people_first, Toast.LENGTH_SHORT).show()
            return null
        }
        return SelectPeopleActivity.createIntent(context, initiallySelected)
    }

    fun selectedIdsFromResult(data: Intent?): List<Long> {
        return SelectPeopleActivity.selectedIdsFromResult(data)
    }
}
