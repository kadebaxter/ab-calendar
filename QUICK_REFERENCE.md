## Quick Reference: Using Event Notifications

### For Users

**Automatic Notifications**
- Every calendar event automatically gets a notification 30 minutes before it starts
- No setup needed - it just works!
- Notifications work even when the app is closed

**What You'll See**
- Notification appears 30 minutes before your event
- Shows: Event title, description, and start time
- Tap the notification to open the app

### For Developers

**Creating Events**
```kotlin
// Notifications are enabled by default
viewModel.addCalendarEvent(
    title = "Meeting",
    description = "Project discussion",
    startTime = startTimeMillis,
    endTime = endTimeMillis,
    categoryId = categoryId
)
```

**Custom Notification Time**
```kotlin
// Example: Notify 1 hour before instead of 30 minutes
val event = event.copy(notificationMinutesBefore = 60)
viewModel.updateCalendarEvent(event)
```

**Disable Notifications**
```kotlin
val event = event.copy(notificationEnabled = false)
viewModel.updateCalendarEvent(event)
```

**Common Time Options** (from NotificationConstants)
```kotlin
NOTIFY_5_MINUTES = 5
NOTIFY_15_MINUTES = 15
NOTIFY_30_MINUTES = 30  // Default
NOTIFY_1_HOUR = 60
NOTIFY_2_HOURS = 120
NOTIFY_1_DAY = 1440
```

### Architecture

```
User creates event
    ↓
ViewModel.addCalendarEvent()
    ↓
Repository saves to database
    ↓
NotificationScheduler.scheduleNotification()
    ↓
AlarmManager sets alarm for (startTime - 30 min)
    ↓
[Time passes...]
    ↓
AlarmManager triggers at scheduled time
    ↓
NotificationReceiver.onReceive()
    ↓
Notification displayed to user
```

### Important Notes

1. **First Launch**: App requests notification permission on Android 13+
2. **Database Version**: Updated to v2 - existing data is preserved
3. **Reboot Safe**: BootReceiver reschedules all notifications after reboot
4. **Battery Efficient**: Uses `setExactAndAllowWhileIdle()` for Doze mode compatibility

### Files to Know

- **NotificationScheduler.kt** - Main scheduling logic
- **NotificationReceiver.kt** - Displays notifications
- **NotificationConstants.kt** - Predefined time options
- **CalendarEvent.kt** - Model with notification fields
- **NOTIFICATIONS_GUIDE.md** - Full documentation
