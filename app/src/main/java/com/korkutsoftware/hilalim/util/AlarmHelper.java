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

        String[] times = {
                cleanTime(timings.fajr),
                cleanTime(timings.sunrise),
                cleanTime(timings.dhuhr),
                cleanTime(timings.asr),
                cleanTime(timings.maghrib),
                cleanTime(timings.isha)
        };

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        long nowMs = System.currentTimeMillis();

        for (int i = 0; i < times.length; i++) {
            if (times[i] == null || times[i].isEmpty()) continue;
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

                for (int type = 0; type < TYPE_OFFSETS_MS.length; type++) {
                    long triggerMs = basePrayerTimeMs + TYPE_OFFSETS_MS[type];

                    // If time has passed today, schedule for tomorrow
                    if (triggerMs <= nowMs) {
                        Calendar tomorrowCal = (Calendar) baseCal.clone();
                        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
                        triggerMs = tomorrowCal.getTimeInMillis() + TYPE_OFFSETS_MS[type];
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
        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        String cachedJson = prefs.getString("cached_prayer_data", null);
        if (cachedJson != null) {
            try {
                PrayerResponse.Data data = new Gson().fromJson(cachedJson, PrayerResponse.Data.class);
                if (data != null && data.timings != null) {
                    setAlarms(context, data.timings);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling alarms from cache", e);
            }
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
