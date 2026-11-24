package com.korkutsoftware.hilalim.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.korkutsoftware.hilalim.MainActivity;
import com.korkutsoftware.hilalim.NotificationSettingsActivity;
import com.korkutsoftware.hilalim.R;
import com.korkutsoftware.hilalim.api.PrayerResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a persistent low-priority notification bar showing the next prayer time
 * and full daily schedule, toggleable from NotificationSettingsActivity.
 */
public class OngoingPrayerNotificationHelper {

    public static final String PREF_ONGOING_ENABLED = "notif_ongoing_enabled";
    private static final String CHANNEL_ID = "Hilalim_Ongoing_Channel";
    private static final int NOTIFICATION_ID = 9050;

    public static void updateOrCancel(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(PREF_ONGOING_ENABLED, true)
                && prefs.getBoolean("notif_enabled", true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (!enabled) {
            manager.cancel(NOTIFICATION_ID);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        PrayerResponse.Data todayData = PrayerCacheHelper.getTodayData(context);
        if (todayData == null || todayData.timings == null) return;

        ensureChannel(context, manager);
        PrayerResponse.Timings timings = todayData.timings;

        String[] names = {
                context.getString(R.string.imsak),
                context.getString(R.string.gunes),
                context.getString(R.string.ogle),
                context.getString(R.string.ikindi),
                context.getString(R.string.aksam),
                context.getString(R.string.yatsi)
        };
        String[] times = {
                AppCalculators.cleanPrayerTime(timings.fajr),
                AppCalculators.cleanPrayerTime(timings.sunrise),
                AppCalculators.cleanPrayerTime(timings.dhuhr),
                AppCalculators.cleanPrayerTime(timings.asr),
                AppCalculators.cleanPrayerTime(timings.maghrib),
                AppCalculators.cleanPrayerTime(timings.isha)
        };

        int nextIdx = findNextPrayerIndex(times);
        String nextName = names[nextIdx];
        String nextTime = times[nextIdx].isEmpty() ? "--:--" : times[nextIdx];

        if (nextIdx == 0) {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            PrayerResponse.Timings tomorrowTimings = PrayerCacheHelper.getTimingsForDateOrFallback(context, tomorrow, timings);
            if (tomorrowTimings != null && !AppCalculators.cleanPrayerTime(tomorrowTimings.fajr).isEmpty()) {
                nextTime = AppCalculators.cleanPrayerTime(tomorrowTimings.fajr);
            }
        }

        String city = prefs.getString("last_city", null);
        String district = prefs.getString("last_district", null);
        String locationLabel = (city != null && district != null) ? city + ", " + district : context.getString(R.string.app_name);

        String title = context.getString(R.string.ongoing_notif_title_format, nextName, nextTime);
        String summary = String.format(
                Locale.getDefault(),
                "%s %s • %s %s • %s %s • %s %s • %s %s • %s %s",
                names[0], times[0], names[1], times[1], names[2], times[2],
                names[3], times[3], names[4], times[4], names[5], times[5]
        );

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                context, 91, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent settingsIntent = new Intent(context, NotificationSettingsActivity.class);
        PendingIntent settingsPending = PendingIntent.getActivity(
                context, 92, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle(title + "  (" + locationLabel + ")")
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(summary))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openPending)
                .addAction(R.drawable.ic_settings_rounded, context.getString(R.string.nav_notifications), settingsPending);

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private static void ensureChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.ongoing_notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(context.getString(R.string.ongoing_notif_channel_desc));
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    private static int findNextPrayerIndex(String[] times) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        Calendar now = Calendar.getInstance();
        try {
            for (int i = 0; i < times.length; i++) {
                if (times[i] == null || times[i].isEmpty()) continue;
                Date parsed = sdf.parse(times[i]);
                if (parsed == null) continue;
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsed);
                cal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                if (cal.after(now)) {
                    return i;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
