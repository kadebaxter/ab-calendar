package com.example.calendarnotes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.calendarnotes.ui.HasOverflowMenu
import com.example.calendarnotes.ui.SearchActivity
import com.example.calendarnotes.ui.SettingsActivity
import com.example.calendarnotes.ui.adapters.ViewPagerAdapter
import com.example.calendarnotes.ui.fragments.CalendarFragment
import com.example.calendarnotes.ui.fragments.CalendarViewMode
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var ivHeaderChevron: ImageView
    private lateinit var btnHeaderTitle: View
    private lateinit var btnHeaderNav: ImageButton
    private lateinit var btnHeaderOverflow: ImageButton
    private lateinit var calendarViewSwitcher: LinearLayout
    private lateinit var btnViewDay: TextView
    private lateinit var btnViewWeek: TextView
    private lateinit var btnViewMonth: TextView

    private var calendarHeaderTitle: String = ""
    private var calendarViewMode: CalendarViewMode = CalendarViewMode.DAY

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // User denied permission - notifications won't work
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]

        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        ivHeaderChevron = findViewById(R.id.ivHeaderChevron)
        btnHeaderTitle = findViewById(R.id.btnHeaderTitle)
        btnHeaderNav = findViewById(R.id.btnHeaderNav)
        btnHeaderOverflow = findViewById(R.id.btnHeaderOverflow)
        calendarViewSwitcher = findViewById(R.id.calendarViewSwitcher)
        btnViewDay = findViewById(R.id.btnViewDay)
        btnViewWeek = findViewById(R.id.btnViewWeek)
        btnViewMonth = findViewById(R.id.btnViewMonth)

        setupHeader()
        setupViewPager()
        requestNotificationPermission()
        handleOpenTabIntent(intent)
        updateHeaderForPage(viewPager.currentItem)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenTabIntent(intent)
    }

    private fun handleOpenTabIntent(intent: Intent?) {
        val tab = intent?.getIntExtra(EXTRA_OPEN_TAB, -1) ?: -1
        if (tab in 0..2) {
            viewPager.setCurrentItem(tab, false)
            bottomNavigation.selectedItemId = menuIdForPosition(tab)
        }
    }

    private fun setupHeader() {
        btnHeaderNav.setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                menuInflater.inflate(R.menu.menu_hamburger, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_search -> {
                            startActivity(Intent(this@MainActivity, SearchActivity::class.java))
                            true
                        }
                        R.id.action_settings -> {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            true
                        }
                        R.id.action_export -> {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.export_coming_soon,
                                Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                        R.id.action_about -> {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle(R.string.menu_about)
                                .setMessage(R.string.about_message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
        btnHeaderOverflow.setOnClickListener { anchor ->
            (currentTabFragment() as? HasOverflowMenu)?.showOverflowMenu(anchor)
        }
        btnHeaderTitle.setOnClickListener {
            if (viewPager.currentItem == 0) {
                findCalendarFragment()?.openMonthPicker()
            }
        }
        btnViewDay.setOnClickListener {
            findCalendarFragment()?.setCalendarViewMode(CalendarViewMode.DAY)
        }
        btnViewWeek.setOnClickListener {
            findCalendarFragment()?.setCalendarViewMode(CalendarViewMode.WEEK)
        }
        btnViewMonth.setOnClickListener {
            findCalendarFragment()?.setCalendarViewMode(CalendarViewMode.MONTH)
        }
    }

    fun syncCalendarViewMode(mode: CalendarViewMode) {
        calendarViewMode = mode
        updateCalendarViewSwitcherUi()
    }

    private fun currentTabFragment(): androidx.fragment.app.Fragment? {
        return supportFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
    }

    fun updateCalendarHeaderTitle(title: String) {
        calendarHeaderTitle = title
        if (viewPager.currentItem == 0) {
            applyHeader(title, showChevron = true, showViewSwitcher = true)
        }
    }

    private fun updateHeaderForPage(position: Int) {
        when (position) {
            0 -> {
                applyHeader(
                    title = calendarHeaderTitle.ifBlank { getString(R.string.nav_calendar) },
                    showChevron = true,
                    showViewSwitcher = true
                )
                findCalendarFragment()?.currentViewMode()?.let { calendarViewMode = it }
                updateCalendarViewSwitcherUi()
            }
            1 -> applyHeader(getString(R.string.nav_notes), showChevron = false, showViewSwitcher = false)
            2 -> applyHeader(getString(R.string.nav_people), showChevron = false, showViewSwitcher = false)
            else -> applyHeader(getString(R.string.app_name), showChevron = false, showViewSwitcher = false)
        }
    }

    private fun applyHeader(title: String, showChevron: Boolean, showViewSwitcher: Boolean) {
        tvHeaderTitle.text = title
        ivHeaderChevron.visibility = if (showChevron) View.VISIBLE else View.GONE
        btnHeaderTitle.isClickable = showChevron
        btnHeaderTitle.isFocusable = showChevron
        calendarViewSwitcher.visibility = if (showViewSwitcher) View.VISIBLE else View.GONE
    }

    private fun updateCalendarViewSwitcherUi() {
        styleViewTab(btnViewDay, calendarViewMode == CalendarViewMode.DAY)
        styleViewTab(btnViewWeek, calendarViewMode == CalendarViewMode.WEEK)
        styleViewTab(btnViewMonth, calendarViewMode == CalendarViewMode.MONTH)
    }

    private fun styleViewTab(tab: TextView, selected: Boolean) {
        tab.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        tab.alpha = if (selected) 1f else 0.65f
    }

    private fun findCalendarFragment(): CalendarFragment? {
        return supportFragmentManager.findFragmentByTag("f0") as? CalendarFragment
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Calendar uses horizontal swipe for day navigation, so disable
        // ViewPager tab swiping while that tab is active.
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewPager.isUserInputEnabled = position != 0
                bottomNavigation.selectedItemId = menuIdForPosition(position)
                updateHeaderForPage(position)
            }
        })

        bottomNavigation.setOnItemSelectedListener { item ->
            val position = positionForMenuId(item.itemId)
            if (position >= 0 && viewPager.currentItem != position) {
                viewPager.setCurrentItem(position, false)
            }
            if (item.itemId == R.id.nav_calendar) {
                findCalendarFragment()?.showDayViewHome()
            }
            true
        }
        bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_calendar) {
                findCalendarFragment()?.showDayViewHome()
            }
        }
    }

    private fun menuIdForPosition(position: Int): Int {
        return when (position) {
            0 -> R.id.nav_calendar
            1 -> R.id.nav_notes
            2 -> R.id.nav_people
            else -> R.id.nav_calendar
        }
    }

    private fun positionForMenuId(itemId: Int): Int {
        return when (itemId) {
            R.id.nav_calendar -> 0
            R.id.nav_notes -> 1
            R.id.nav_people -> 2
            else -> -1
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
    }
}
