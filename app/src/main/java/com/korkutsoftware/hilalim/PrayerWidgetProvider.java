package com.korkutsoftware.hilalim;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.util.AppCalculators;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PrayerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, PrayerWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        if (ids != null && ids.length > 0) {
            for (int id : ids) {
                updateAppWidget(context, manager, id);
            }
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_times);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        String city = prefs.getString("last_city", null);
        String district = prefs.getString("last_district", null);
        if (city != null && district != null) {
            views.setTextViewText(R.id.widget_location, city + ", " + district);
        } else {
            views.setTextViewText(R.id.widget_location, context.getString(R.string.app_name));
        }

        PrayerResponse.Data data = com.korkutsoftware.hilalim.util.PrayerCacheHelper.getTodayData(context);
        if (data != null && data.timings != null) {
            bindTimings(context, views, data.timings);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void bindTimings(Context context, RemoteViews views, PrayerResponse.Timings timings) {
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
        int[] timeIds = {
                R.id.widget_time_fajr, R.id.widget_time_sunrise, R.id.widget_time_dhuhr,
                R.id.widget_time_asr, R.id.widget_time_maghrib, R.id.widget_time_isha
        };
        int[] labelIds = {
                R.id.widget_lbl_fajr, R.id.widget_lbl_sunrise, R.id.widget_lbl_dhuhr,
                R.id.widget_lbl_asr, R.id.widget_lbl_maghrib, R.id.widget_lbl_isha
        };

        int normalColor = ContextCompat.getColor(context, R.color.text_primary);
        int mutedColor = ContextCompat.getColor(context, R.color.text_muted);
        int goldColor = ContextCompat.getColor(context, R.color.secondary);

        for (int i = 0; i < 6; i++) {
            views.setTextViewText(timeIds[i], times[i].isEmpty() ? "--:--" : times[i]);
            views.setTextColor(timeIds[i], normalColor);
            views.setTextColor(labelIds[i], mutedColor);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        Calendar now = Calendar.getInstance();
        int nextIndex = -1;
        long nextTargetMs = -1L;

        try {
            for (int i = 0; i < times.length; i++) {
                if (times[i].isEmpty()) continue;
                Date pDate = sdf.parse(times[i]);
                if (pDate != null) {
                    Calendar pCal = Calendar.getInstance();
                    pCal.setTime(pDate);
                    pCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    if (pCal.after(now)) {
                        nextIndex = i;
                        nextTargetMs = pCal.getTimeInMillis();
                        break;
                    }
                }
            }

            if (nextIndex == -1 && !times[0].isEmpty()) {
                Date imsakDate = sdf.parse(times[0]);
                if (imsakDate != null) {
                    Calendar imsakCal = Calendar.getInstance();
                    imsakCal.setTime(imsakDate);
                    imsakCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    imsakCal.add(Calendar.DAY_OF_MONTH, 1);
                    nextIndex = 0;
                    nextTargetMs = imsakCal.getTimeInMillis();
                }
            }
        } catch (Exception ignored) {
        }

        if (nextIndex >= 0 && nextTargetMs > 0) {
            views.setTextViewText(R.id.widget_next_prayer, names[nextIndex]);
            views.setTextColor(timeIds[nextIndex], goldColor);
            views.setTextColor(labelIds[nextIndex], goldColor);

            long remainingMs = Math.max(0L, nextTargetMs - System.currentTimeMillis());
            long base = SystemClock.elapsedRealtime() + remainingMs;
            views.setChronometer(R.id.widget_countdown, base, null, true);
            views.setChronometerCountDown(R.id.widget_countdown, true);
        }
    }
}
