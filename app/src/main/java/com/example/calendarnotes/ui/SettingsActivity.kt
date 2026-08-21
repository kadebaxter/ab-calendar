package com.example.calendarnotes.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.example.calendarnotes.R
import com.example.calendarnotes.data.AppPreferences
import com.example.calendarnotes.google.GoogleCalendarAuth
import com.example.calendarnotes.viewmodel.CalendarNotesViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: AppPreferences
    private lateinit var viewModel: CalendarNotesViewModel
    private lateinit var tvGoogleAccount: TextView
    private lateinit var tvGoogleLastSync: TextView
    private lateinit var switchAutoSync: MaterialSwitch
    private lateinit var btnConnect: MaterialButton
    private lateinit var btnSync: MaterialButton
    private lateinit var btnDisconnect: MaterialButton

    private val reminderOptions = listOf(0, 5, 10, 15, 30, 60)
    private val syncTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.onGoogleSignedIn(account.email)
            refreshGoogleUi()
            Toast.makeText(this, R.string.google_sync_in_progress, Toast.LENGTH_SHORT).show()
            runSync(showToastAlways = true)
        } catch (_: ApiException) {
            Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show()
            AlertDialog.Builder(this)
                .setTitle(R.string.settings_google_calendar)
                .setMessage(R.string.settings_google_setup_help)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = AppPreferences(this)
        viewModel = ViewModelProvider(this)[CalendarNotesViewModel::class.java]

        findViewById<TextView>(R.id.tvHeaderTitle).text = getString(R.string.settings_title)
        findViewById<View>(R.id.ivHeaderChevron).visibility = View.GONE
        findViewById<View>(R.id.btnHeaderTitle).isClickable = false
        findViewById<ImageButton>(R.id.btnHeaderOverflow).visibility = View.INVISIBLE
        val btnNav = findViewById<ImageButton>(R.id.btnHeaderNav)
        btnNav.setImageResource(R.drawable.ic_arrow_back)
        btnNav.contentDescription = getString(R.string.navigate_back)
        btnNav.setOnClickListener { finish() }

        tvGoogleAccount = findViewById(R.id.tvGoogleAccount)
        tvGoogleLastSync = findViewById(R.id.tvGoogleLastSync)
        switchAutoSync = findViewById(R.id.switchGoogleAutoSync)
        btnConnect = findViewById(R.id.btnGoogleConnect)
        btnSync = findViewById(R.id.btnGoogleSync)
        btnDisconnect = findViewById(R.id.btnGoogleDisconnect)

        setupReminderSpinner()
        setupWeekStartSpinner()
        setupThemeSpinner()
        setupGoogleSection()
        refreshGoogleUi()
    }

    private fun setupGoogleSection() {
        switchAutoSync.isChecked = viewModel.googleAutoSyncEnabled()
        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGoogleAutoSyncEnabled(isChecked)
        }

        btnConnect.setOnClickListener {
            googleSignInLauncher.launch(GoogleCalendarAuth.getClient(this).signInIntent)
        }
        btnSync.setOnClickListener { runSync(showToastAlways = true) }
        btnDisconnect.setOnClickListener { confirmDisconnect() }
    }

    private fun refreshGoogleUi() {
        val account = GoogleCalendarAuth.lastSignedInAccount(this)
        val connected = GoogleCalendarAuth.hasCalendarAccess(account)
        if (connected && account != null) {
            viewModel.onGoogleSignedIn(account.email)
            tvGoogleAccount.text = getString(
                R.string.settings_google_connected_as,
                account.email ?: getString(R.string.settings_google_calendar)
            )
            btnConnect.visibility = View.GONE
            btnSync.visibility = View.VISIBLE
            btnDisconnect.visibility = View.VISIBLE
        } else {
            tvGoogleAccount.text = getString(R.string.settings_google_not_connected)
            btnConnect.visibility = View.VISIBLE
            btnSync.visibility = View.GONE
            btnDisconnect.visibility = View.GONE
        }

        val lastSync = viewModel.googleLastSyncMillis()
        tvGoogleLastSync.text = if (lastSync > 0L) {
            getString(R.string.settings_google_last_sync, syncTimeFormat.format(Date(lastSync)))
        } else {
            getString(R.string.settings_google_last_sync_never)
        }
    }

    private fun runSync(showToastAlways: Boolean) {
        btnSync.isEnabled = false
        viewModel.syncGoogleCalendar { result ->
            btnSync.isEnabled = true
            refreshGoogleUi()
            if (result.success) {
                if (showToastAlways || result.inserted + result.updated + result.removed > 0) {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.google_sync_success,
                            result.inserted,
                            result.updated,
                            result.removed
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.google_sync_failed, result.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmDisconnect() {
        AlertDialog.Builder(this)
            .setTitle(R.string.google_disconnect_title)
            .setMessage(R.string.google_disconnect_message)
            .setPositiveButton(R.string.google_disconnect_keep) { _, _ ->
                disconnect(removeImported = false)
            }
            .setNegativeButton(R.string.google_disconnect_remove) { _, _ ->
                disconnect(removeImported = true)
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    private fun disconnect(removeImported: Boolean) {
        GoogleCalendarAuth.getClient(this).signOut().addOnCompleteListener {
            viewModel.disconnectGoogleCalendar(removeImported) {
                refreshGoogleUi()
            }
        }
    }

    private fun setupReminderSpinner() {
        val labels = reminderOptions.map { minutes ->
            when (minutes) {
                0 -> getString(R.string.reminder_at_time)
                60 -> getString(R.string.reminder_hour)
                else -> getString(R.string.reminder_minutes, minutes)
            }
        }
        val spinner = findViewById<Spinner>(R.id.spinnerReminder)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinner.setSelection(reminderOptions.indexOf(prefs.reminderMinutesBefore).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.reminderMinutesBefore = reminderOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupWeekStartSpinner() {
        val labels = listOf(
            getString(R.string.settings_week_sunday),
            getString(R.string.settings_week_monday)
        )
        val spinner = findViewById<Spinner>(R.id.spinnerWeekStart)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinner.setSelection(if (prefs.weekStartDay == Calendar.MONDAY) 1 else 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.weekStartDay = if (position == 1) Calendar.MONDAY else Calendar.SUNDAY
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupThemeSpinner() {
        val modes = listOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        )
        val labels = listOf(
            getString(R.string.settings_theme_system),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark)
        )
        val spinner = findViewById<Spinner>(R.id.spinnerTheme)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinner.setSelection(modes.indexOf(prefs.themeMode).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = modes[position]
                if (prefs.themeMode != mode) {
                    prefs.themeMode = mode
                    prefs.applyTheme()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
}
