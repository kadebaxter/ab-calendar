package com.example.calendarnotes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calendarnotes.R
import com.example.calendarnotes.data.models.PersonOrdering
import com.example.calendarnotes.data.models.PersonStatus
import com.example.calendarnotes.ui.AddEditPersonActivity
import com.example.calendarnotes.ui.HasOverflowMenu
import com.example.calendarnotes.ui.PersonDetailActivity
import com.example.calendarnotes.ui.adapters.PersonAdapter
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

enum class PeopleSort { NAME, STATUS }

class PeopleFragment : Fragment(), HasOverflowMenu {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var rvPeople: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var personAdapter: PersonAdapter

    private var sortMode: PeopleSort = PeopleSort.NAME
    private var filterStatus: PersonStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_people, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CalendarNotesViewModel::class.java]

        rvPeople = view.findViewById(R.id.rvPeople)
        tvEmpty = view.findViewById(R.id.tvPeopleEmpty)
        val fabAdd: FloatingActionButton = view.findViewById(R.id.fabAddPerson)

        personAdapter = PersonAdapter(
            onPersonClick = { person ->
                startActivity(PersonDetailActivity.createIntent(requireContext(), person.id))
            }
        )
        rvPeople.layoutManager = LinearLayoutManager(requireContext())
        rvPeople.adapter = personAdapter

        fabAdd.setOnClickListener {
            startActivity(AddEditPersonActivity.createIntent(requireContext()))
        }

        viewModel.people.observe(viewLifecycleOwner) { refreshPeople() }
        viewModel.lastEventByPersonId.observe(viewLifecycleOwner) { refreshPeople() }
    }

    override fun showOverflowMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_people_overflow, menu)
            when (sortMode) {
                PeopleSort.NAME -> menu.findItem(R.id.action_sort_name)?.isChecked = true
                PeopleSort.STATUS -> menu.findItem(R.id.action_sort_status)?.isChecked = true
            }
            menu.findItem(R.id.action_clear_status_filter)?.isVisible = filterStatus != null
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_sort_name -> {
                        sortMode = PeopleSort.NAME
                        refreshPeople()
                        true
                    }
                    R.id.action_sort_status -> {
                        sortMode = PeopleSort.STATUS
                        refreshPeople()
                        true
                    }
                    R.id.action_filter_status -> {
                        showStatusFilterDialog()
                        true
                    }
                    R.id.action_clear_status_filter -> {
                        filterStatus = null
                        refreshPeople()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showStatusFilterDialog() {
        val statuses = PersonStatus.entries
        val labels = mutableListOf(getString(R.string.all_statuses))
        labels.addAll(statuses.map { it.label })
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_filter_status)
            .setItems(labels.toTypedArray()) { _, which ->
                filterStatus = if (which == 0) null else statuses[which - 1]
                refreshPeople()
            }
            .show()
    }

    private fun refreshPeople() {
        var people = viewModel.people.value.orEmpty()
        filterStatus?.let { status ->
            people = people.filter { it.status == status }
        }
        people = PersonOrdering.sorted(
            people = people,
            lastEventByPersonId = viewModel.lastEventByPersonId.value.orEmpty(),
            preferStatusOrdinal = sortMode == PeopleSort.STATUS
        )
        personAdapter.updatePeople(people)
        tvEmpty.visibility = if (people.isEmpty()) View.VISIBLE else View.GONE
    }

}
