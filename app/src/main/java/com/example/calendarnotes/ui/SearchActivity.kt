package com.example.calendarnotes.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.MainActivity
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.PersonOrdering
import com.example.calendarnotes.ui.adapters.SearchResultAdapter
import com.example.calendarnotes.ui.adapters.SearchResultItem
import com.example.calendarnotes.ui.adapters.SearchResultType
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var adapter: SearchResultAdapter
    private lateinit var tvEmpty: TextView
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]
        tvEmpty = findViewById(R.id.tvSearchEmpty)

        findViewById<TextView>(R.id.tvHeaderTitle).text = getString(R.string.menu_search)
        findViewById<View>(R.id.ivHeaderChevron).visibility = View.GONE
        findViewById<View>(R.id.btnHeaderTitle).isClickable = false
        findViewById<ImageButton>(R.id.btnHeaderOverflow).visibility = View.INVISIBLE
        val btnNav = findViewById<ImageButton>(R.id.btnHeaderNav)
        btnNav.setImageResource(R.drawable.ic_arrow_back)
        btnNav.contentDescription = getString(R.string.navigate_back)
        btnNav.setOnClickListener { finish() }

        adapter = SearchResultAdapter { item ->
            when (item.type) {
                SearchResultType.PERSON -> {
                    startActivity(PersonDetailActivity.createIntent(this, item.id))
                }
                SearchResultType.EVENT -> {
                    startActivity(EventDetailActivity.createIntent(this, item.id))
                }
                SearchResultType.NOTE -> {
                    startActivity(AddEditNoteActivity.createIntent(this, item.id))
                }
            }
        }

        val rv = findViewById<RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                runSearch(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                runSearch(newText.orEmpty())
                return true
            }
        })

        // Re-run search once data finishes loading into the ViewModel.
        viewModel.calendarEvents.observe(this) { runSearch(searchView.query?.toString().orEmpty()) }
        viewModel.notes.observe(this) { runSearch(searchView.query?.toString().orEmpty()) }
        viewModel.people.observe(this) { runSearch(searchView.query?.toString().orEmpty()) }

        searchView.requestFocus()
    }

    private fun runSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            adapter.submit(emptyList())
            tvEmpty.visibility = View.GONE
            return
        }

        val q = query.lowercase(Locale.getDefault())
        val results = mutableListOf<SearchResultItem>()

        viewModel.calendarEvents.value.orEmpty().forEach { event ->
            if (event.title.lowercase(Locale.getDefault()).contains(q) ||
                event.description.lowercase(Locale.getDefault()).contains(q)
            ) {
                results.add(
                    SearchResultItem(
                        type = SearchResultType.EVENT,
                        id = event.id,
                        title = event.title,
                        subtitle = dateFormat.format(Date(event.startTime)),
                        typeLabel = getString(R.string.search_section_events)
                    )
                )
            }
        }

        viewModel.notes.value.orEmpty().forEach { note ->
            if (note.title.lowercase(Locale.getDefault()).contains(q) ||
                note.content.lowercase(Locale.getDefault()).contains(q)
            ) {
                results.add(
                    SearchResultItem(
                        type = SearchResultType.NOTE,
                        id = note.id,
                        title = note.title,
                        subtitle = note.content.take(80),
                        typeLabel = getString(R.string.search_section_notes)
                    )
                )
            }
        }

        PersonOrdering.sorted(
            people = viewModel.people.value.orEmpty(),
            lastEventByPersonId = viewModel.lastEventByPersonId.value.orEmpty()
        ).forEach { person ->
            val haystack = listOf(
                person.name,
                person.phone,
                person.email,
                person.address,
                person.notes,
                person.status.label
            ).joinToString(" ").lowercase(Locale.getDefault())
            if (haystack.contains(q)) {
                results.add(
                    SearchResultItem(
                        type = SearchResultType.PERSON,
                        id = person.id,
                        title = person.name,
                        subtitle = person.status.label,
                        typeLabel = getString(R.string.search_section_people)
                    )
                )
            }
        }

        adapter.submit(results)
        tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }
}
