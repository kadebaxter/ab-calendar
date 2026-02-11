# UI Example: Adding Notification Settings to Event Dialog

This is an example of how you could add UI controls to customize notification settings when creating or editing events.

## Example: Add to Event Creation Dialog

### 1. Add UI Elements to Your Layout

```xml
<!-- In your event dialog layout XML -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    
    <!-- Existing event fields (title, description, etc.) -->
    
    <!-- Notification Settings Section -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Notification Settings"
        android:textStyle="bold"
        android:layout_marginTop="16dp"
        android:layout_marginBottom="8dp" />
    
    <!-- Enable/Disable Notification Switch -->
    <androidx.appcompat.widget.SwitchCompat
        android:id="@+id/switchNotification"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Enable notification"
        android:checked="true" />
    
    <!-- Notification Time Picker -->
    <Spinner
        android:id="@+id/spinnerNotificationTime"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:enabled="true" />
    
</LinearLayout>
```

### 2. Kotlin Code for the Dialog

```kotlin
class AddEventDialog : DialogFragment() {
    
    private lateinit var switchNotification: SwitchCompat
    private lateinit var spinnerNotificationTime: Spinner
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_event, null)
        
        // Initialize views
        switchNotification = view.findViewById(R.id.switchNotification)
        spinnerNotificationTime = view.findViewById(R.id.spinnerNotificationTime)
        
        // Set up notification time options
        setupNotificationSpinner()
        
        // Enable/disable time picker based on switch
        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            spinnerNotificationTime.isEnabled = isChecked
        }
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Add Event")
            .setView(view)
            .setPositiveButton("Save") { _, _ -> saveEvent() }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupNotificationSpinner() {
        val options = NotificationConstants.getNotificationTimeOptions()
        val labels = options.map { it.first }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificationTime.adapter = adapter
        
        // Set default to 30 minutes (index 2)
        spinnerNotificationTime.setSelection(2)
    }
    
    private fun saveEvent() {
        // Get notification settings
        val notificationEnabled = switchNotification.isChecked
        val selectedPosition = spinnerNotificationTime.selectedItemPosition
        val notificationMinutes = NotificationConstants.getNotificationTimeOptions()[selectedPosition].second
        
        // Get other event data (title, times, etc.)
        val title = etTitle.text.toString()
        val description = etDescription.text.toString()
        val startTime = selectedStartTime // From your date/time pickers
        val endTime = selectedEndTime
        val categoryId = selectedCategoryId
        
        // Save using ViewModel with custom notification settings
        // Since addCalendarEvent doesn't support custom settings yet,
        // you would need to modify it or use a different approach:
        
        // Option 1: Add the event, then update notification settings
        viewModel.addCalendarEvent(title, description, startTime, endTime, categoryId)
        // Then get the event ID and update:
        // val event = event.copy(
        //     notificationEnabled = notificationEnabled,
        //     notificationMinutesBefore = notificationMinutes
        // )
        // viewModel.updateCalendarEvent(event)
        
        // Option 2: Modify addCalendarEvent to accept notification parameters (recommended)
    }
}
```

### 3. Update ViewModel Method (Recommended)

Modify `addCalendarEvent` in `CalendarNotesViewModel.kt` to accept notification parameters:

```kotlin
fun addCalendarEvent(
    title: String,
    description: String,
    startTime: Long,
    endTime: Long,
    categoryId: Long?,
    notificationEnabled: Boolean = true,  // New parameter
    notificationMinutesBefore: Int = 30   // New parameter
) {
    viewModelScope.launch {
        val eventId = withContext(Dispatchers.IO) {
            repository.insertCalendarEvent(
                CalendarEvent(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    categoryId = categoryId,
                    notificationEnabled = notificationEnabled,
                    notificationMinutesBefore = notificationMinutesBefore
                )
            )
        }
        
        if (eventId > 0) {
            withContext(Dispatchers.IO) {
                val event = repository.getCalendarEventById(eventId)
                event?.let {
                    NotificationScheduler.scheduleNotification(getApplication(), it)
                }
            }
        }
        
        loadCalendarEvents()
    }
}
```

Then call it from your dialog:

```kotlin
viewModel.addCalendarEvent(
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    categoryId = categoryId,
    notificationEnabled = notificationEnabled,
    notificationMinutesBefore = notificationMinutes
)
```

## Simpler Alternative: Settings Screen

Instead of per-event settings, you could add a global preference:

```kotlin
// In a SettingsActivity or PreferenceFragment
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Add preferences from XML
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
```

```xml
<!-- res/xml/preferences.xml -->
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
    <PreferenceCategory android:title="Notifications">
        
        <SwitchPreferenceCompat
            android:key="notifications_enabled"
            android:title="Enable event notifications"
            android:defaultValue="true" />
        
        <ListPreference
            android:key="default_notification_time"
            android:title="Default notification time"
            android:entries="@array/notification_time_labels"
            android:entryValues="@array/notification_time_values"
            android:defaultValue="30"
            android:dependency="notifications_enabled" />
            
    </PreferenceCategory>
</PreferenceScreen>
```

Then read these preferences when creating events:

```kotlin
val prefs = PreferenceManager.getDefaultSharedPreferences(context)
val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
val defaultMinutes = prefs.getString("default_notification_time", "30")?.toIntOrNull() ?: 30
```

## Summary

- **Per-Event Settings**: More flexible but requires more UI work
- **Global Settings**: Simpler to implement, affects all events
- **Hybrid Approach**: Global default with per-event override option

Choose based on your app's requirements and user needs!
