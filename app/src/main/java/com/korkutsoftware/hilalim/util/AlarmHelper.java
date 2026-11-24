package com.korkutsoftware.hilalim.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.korkutsoftware.hilalim.NotificationReceiver;
import com.korkutsoftware.hilalim.api.PrayerResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmHelper {

    private static final String TAG = "AlarmHelper";

    public static final String[] PRAYER_KEYS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};

    public static final int TYPE_EXACT = 0;
    public static final int TYPE_PRE_45 = 1;
    public static final int TYPE_PRE_15 = 2;

    // Offsets in milliseconds relative to prayer time
    private static final long[] TYPE_OFFSETS_MS = {
            0L,                       // Exact
            -45L * 60L * 1000L,       // 45 mins before
            -15L * 60L * 1000L        // 15 mins before
    };

    @SuppressLint("ScheduleExactAlarm")
    public static void setAlarms(Context context, PrayerResponse.Timings timings) {
        if (context == null || timings == null) return;

        // Clear existing alarms first
        cancelAllAlarms(context);

        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notif_enabled", true)) {
            OngoingPrayerNotificationHelper.updateOrCancel(context);
            return;
        }
        boolean pre45Enabled = prefs.getBoolean("notif_pre_45", true);
        boolean pre15Enabled = prefs.getBoolean("notif_pre_15", true);

        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
        PrayerResponse.Timings tomorrowTimings = PrayerCacheHelper.getTimingsForDateOrFallback(context, tomorrowCal, timings);

        String[] times = extractTimes(timings);
        String[] tomorrowTimes = extractTimes(tomorrowTimings);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        long nowMs = System.currentTimeMillis();

        for (int i = 0; i < times.length; i++) {
            if (times[i] == null || times[i].isEmpty()) continue;
            if (!prefs.getBoolean("notif_prayer_" + PRAYER_KEYS[i], true)) continue;
            try {
                Date parsedTime = sdf.parse(times[i]);
                if (parsedTime == null) continue;

                Calendar prayerCal = Calendar.getInstance();
                prayerCal.setTime(parsedTime);

                Calendar baseCal = Calendar.getInstance();
                baseCal.set(Calendar.HOUR_OF_DAY, prayerCal.get(Calendar.HOUR_OF_DAY));
                baseCal.set(Calendar.MINUTE, prayerCal.get(Calendar.MINUTE));
                baseCal.set(Calendar.SECOND, 0);
                baseCal.set(Calendar.MILLISECOND, 0);

                long basePrayerTimeMs = baseCal.getTimeInMillis();

                // Prepare tomorrow's exact prayer time if available in monthly cache
                Calendar tomorrowBaseCal = (Calendar) baseCal.clone();
                tomorrowBaseCal.add(Calendar.DAY_OF_MONTH, 1);
                if (tomorrowTimes[i] != null && !tomorrowTimes[i].isEmpty()) {
                    Date tParsed = sdf.parse(tomorrowTimes[i]);
                    if (tParsed != null) {
                        Calendar tPrayer = Calendar.getInstance();
                        tPrayer.setTime(tParsed);
                        tomorrowBaseCal.set(Calendar.HOUR_OF_DAY, tPrayer.get(Calendar.HOUR_OF_DAY));
                        tomorrowBaseCal.set(Calendar.MINUTE, tPrayer.get(Calendar.MINUTE));
                    }
                }
                long tomorrowPrayerTimeMs = tomorrowBaseCal.getTimeInMillis();

                for (int type = 0; type < TYPE_OFFSETS_MS.length; type++) {
                    if (type == TYPE_PRE_45 && !pre45Enabled) continue;
                    if (type == TYPE_PRE_15 && !pre15Enabled) continue;
                    long triggerMs = basePrayerTimeMs + TYPE_OFFSETS_MS[type];

                    // If time has passed today, schedule using tomorrow's cached time
                    if (triggerMs <= nowMs) {
                        triggerMs = tomorrowPrayerTimeMs + TYPE_OFFSETS_MS[type];
                    }

                    int requestCode = (i * 10) + type;

                    Intent intent = new Intent(context, NotificationReceiver.class);
                    intent.putExtra("prayer_key", PRAYER_KEYS[i]);
                    intent.putExtra("type_index", type);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    scheduleSingleAlarm(alarmManager, triggerMs, pendingIntent);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing prayer time: " + times[i], e);
            }
        }
        OngoingPrayerNotificationHelper.updateOrCancel(context);
    }

    private static String[] extractTimes(PrayerResponse.Timings timings) {
        if (timings == null) {
            return new String[]{"", "", "", "", "", ""};
        }
        return new String[]{
                cleanTime(timings.fajr),
                cleanTime(timings.sunrise),
                cleanTime(timings.dhuhr),
                cleanTime(timings.asr),
                cleanTime(timings.maghrib),
                cleanTime(timings.isha)
        };
    }

    private static String cleanTime(String timeStr) {
        if (timeStr == null) return "";
        return timeStr.split(" ")[0];
    }

    @SuppressLint("ScheduleExactAlarm")
    private static void scheduleSingleAlarm(AlarmManager alarmManager, long triggerMs, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent);
        }
    }

    public static void cancelAllAlarms(Context context) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < PRAYER_KEYS.length; i++) {
            for (int type = 0; type < TYPE_OFFSETS_MS.length; type++) {
                int requestCode = (i * 10) + type;
                Intent intent = new Intent(context, NotificationReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
    }

    public static void rescheduleAlarmsFromCache(Context context) {
        if (context == null) return;
        PrayerResponse.Data data = PrayerCacheHelper.getTodayData(context);
        if (data != null && data.timings != null) {
            setAlarms(context, data.timings);
        } else {
            OngoingPrayerNotificationHelper.updateOrCancel(context);
        }
    }

    public static boolean hasExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public static void openAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            context.startActivity(intent);
        }
    }
}
