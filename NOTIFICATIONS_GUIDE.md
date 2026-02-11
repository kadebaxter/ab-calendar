# Event Notifications Implementation

This document describes the event notification system implemented in the Calendar Notes app.

## Overview

The app now supports automatic notifications that trigger 30 minutes before each calendar event (customizable). Notifications work even when the app is closed or the device is in Doze mode.

## Features

- **Automatic Scheduling**: Notifications are automatically scheduled when creating or updating events
- **Customizable Timing**: Default is 30 minutes before events, but can be customized per event
- **Persistent**: Notifications survive device reboots
- **Battery Efficient**: Uses exact alarms that work even in Doze mode
- **Rich Notifications**: Shows event title, description, and start time

## Architecture

### Components

1. **CalendarEvent Model** (`data/models/CalendarEvent.kt`)
   - Added `notificationEnabled: Boolean` - whether notifications are enabled for this event
   - Added `notificationMinutesBefore: Int` - how many minutes before the event to notify

2. **DatabaseHelper** (`data/database/DatabaseHelper.kt`)
   - Updated to version 2 to add notification columns
   - Handles migration from version 1 to preserve existing data

3. **NotificationScheduler** (`notifications/NotificationScheduler.kt`)
   - Schedules/cancels/updates notifications using AlarmManager
   - Uses `setExactAndAllowWhileIdle()` for precise timing

4. **NotificationReceiver** (`notifications/NotificationReceiver.kt`)
   - BroadcastReceiver that handles alarm triggers
   - Creates and displays notifications using NotificationManager

5. **BootReceiver** (`notifications/BootReceiver.kt`)
   - Reschedules all notifications after device reboot

6. **CalendarNotesApplication** (`CalendarNotesApplication.kt`)
   - Creates notification channel on app startup
   - Required for Android 8.0+ (API 26+)

7. **MainActivity** (`MainActivity.kt`)
   - Requests POST_NOTIFICATIONS permission for Android 13+ (API 33+)

### Permissions

The following permissions are declared in `AndroidManifest.xml`:

- `POST_NOTIFICATIONS` - Required for Android 13+ to show notifications
- `SCHEDULE_EXACT_ALARM` - Allows scheduling precise alarms
- `USE_EXACT_ALARM` - Alternative for alarm scheduling
- `RECEIVE_BOOT_COMPLETED` - Reschedules notifications after reboot

## Usage

### Creating an Event with Notifications

```kotlin
// Notifications are enabled by default with 30-minute advance notice
viewModel.addCalendarEvent(
    title = "Team Meeting",
    description = "Quarterly planning session",
    startTime = eventStartTimeMillis,
    endTime = eventEndTimeMillis,
    categoryId = categoryId
)
```

### Customizing Notification Settings

```kotlin
// Update an event with custom notification settings
val updatedEvent = event.copy(
    notificationEnabled = true,
    notificationMinutesBefore = 60  // 1 hour before
)
viewModel.updateCalendarEvent(updatedEvent)
```

### Disabling Notifications for an Event

```kotlin
val updatedEvent = event.copy(
    notificationEnabled = false
)
viewModel.updateCalendarEvent(updatedEvent)
```

### Available Notification Time Options

The `NotificationConstants` class provides common options:

- 5 minutes before
- 15 minutes before  
- 30 minutes before (default)
- 1 hour before
- 2 hours before
- 1 day before

## How It Works

### When Creating an Event

1. Event is saved to database with notification settings
2. `NotificationScheduler.scheduleNotification()` is called
3. Trigger time is calculated: `eventStartTime - (minutesBefore * 60 * 1000)`
4. If trigger time is in the future, an alarm is scheduled via AlarmManager
5. PendingIntent pointing to NotificationReceiver is created

### When the Notification Triggers

1. AlarmManager fires at the scheduled time
2. NotificationReceiver.onReceive() is called
3. Notification is built with event details
4. NotificationManager displays the notification
5. Tapping the notification opens the app

### When Updating an Event

1. Old notification is cancelled
2. Event is updated in database
3. New notification is scheduled with updated time/settings

### When Deleting an Event

1. Notification is cancelled via AlarmManager
2. Event is deleted from database

### After Device Reboot

1. BootReceiver receives BOOT_COMPLETED broadcast
2. All events are retrieved from database
3. Notifications are rescheduled for all future events

## Testing

### Test Basic Notification

1. Create an event with start time 1 minute in the future
2. Set notification to 30 seconds before (use custom time)
3. Lock your phone
4. Wait for notification to appear

### Test After Reboot

1. Create an event scheduled for tomorrow
2. Reboot your device
3. Notification should still trigger at the scheduled time

### Test Notification Settings

1. Create an event
2. Update notification time to different values
3. Verify notifications trigger at correct times

## Limitations and Considerations

1. **Exact Alarms**: Android 12+ requires SCHEDULE_EXACT_ALARM permission for precise scheduling
2. **Battery Optimization**: Some manufacturers may still restrict alarms despite using setExactAndAllowWhileIdle()
3. **Past Events**: Notifications are not scheduled for events that have already passed
4. **Notification Permission**: Users must grant notification permission on Android 13+

## Future Enhancements

Potential improvements to consider:

- Multiple notifications per event (e.g., 1 day before AND 30 minutes before)
- Custom notification sounds per category
- Notification action buttons (e.g., "Snooze", "Mark as done")
- Recurring events support
- Notification history/logs
- Setting to configure default notification time
- Batch notification scheduling optimization
