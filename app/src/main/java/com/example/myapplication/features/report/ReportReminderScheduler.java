package com.example.myapplication.features.report;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.HashSet;
import java.util.Set;

public final class ReportReminderScheduler {

    private static final String PREFS_NAME = "report_reminders";
    private static final String KEY_SET = "scheduled_set";

    private ReportReminderScheduler() {}

    public static int scheduleReminder(Context context, long triggerAtMillis, String title, String message) {
        int requestCode = generateRequestCode(context, triggerAtMillis, title, message);
        Intent intent = new Intent(context, ReportAlarmReceiver.class);
        intent.putExtra(ReportAlarmReceiver.EXTRA_REQUEST_CODE, requestCode);
        intent.putExtra(ReportAlarmReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReportAlarmReceiver.EXTRA_MESSAGE, message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            scheduleWithBestEffort(alarmManager, triggerAtMillis, pendingIntent);
        }

        saveReminder(context, requestCode, triggerAtMillis, title, message);
        return requestCode;
    }

    public static void restoreScheduledReminders(Context context) {
        Set<String> entries = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SET, new HashSet<>());
        if (entries == null || entries.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (String entry : new HashSet<>(entries)) {
            String[] parts = entry.split("\\|", 4);
            if (parts.length < 4) {
                continue;
            }
            int requestCode;
            long triggerAt;
            try {
                requestCode = Integer.parseInt(parts[0]);
                triggerAt = Long.parseLong(parts[1]);
            } catch (NumberFormatException ex) {
                continue;
            }
            if (triggerAt <= now) {
                removeReminder(context, requestCode);
                continue;
            }
            Intent intent = new Intent(context, ReportAlarmReceiver.class);
            intent.putExtra(ReportAlarmReceiver.EXTRA_REQUEST_CODE, requestCode);
            intent.putExtra(ReportAlarmReceiver.EXTRA_TITLE, parts[2]);
            intent.putExtra(ReportAlarmReceiver.EXTRA_MESSAGE, parts[3]);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                scheduleWithBestEffort(alarmManager, triggerAt, pendingIntent);
            }
        }
    }

    public static void removeReminder(Context context, int requestCode) {
        Set<String> entries = new HashSet<>(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SET, new HashSet<>()));
        boolean changed = entries.removeIf(v -> v.startsWith(requestCode + "|"));
        if (changed) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(KEY_SET, entries)
                    .apply();
        }
    }

    private static void saveReminder(Context context, int requestCode, long triggerAtMillis, String title, String message) {
        Set<String> entries = new HashSet<>(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SET, new HashSet<>()));
        entries.removeIf(v -> v.startsWith(requestCode + "|"));
        entries.add(requestCode + "|" + triggerAtMillis + "|" + safe(title) + "|" + safe(message));

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SET, entries)
                .apply();
    }

    private static int generateRequestCode(Context context, long triggerAtMillis, String title, String message) {
        Set<String> entries = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SET, new HashSet<>());
        int candidate = Math.abs((triggerAtMillis + "|" + safe(title) + "|" + safe(message)).hashCode());
        if (candidate == 0) {
            candidate = 1;
        }

        Set<String> safeEntries = entries != null ? entries : new HashSet<>();
        while (containsRequestCode(safeEntries, candidate)) {
            candidate++;
            if (candidate <= 0) {
                candidate = 1;
            }
        }
        return candidate;
    }

    private static boolean containsRequestCode(Set<String> entries, int requestCode) {
        String prefix = requestCode + "|";
        for (String entry : entries) {
            if (entry != null && entry.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleWithBestEffort(AlarmManager alarmManager,
            long triggerAtMillis,
            PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("|", "/");
    }
}
