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

import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.util.AppCalculators;
import com.korkutsoftware.hilalim.util.LocaleHelper;
import com.korkutsoftware.hilalim.util.PrayerCacheHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CountdownWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, CountdownWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        if (ids != null && ids.length > 0) {
            for (int id : ids) {
                updateAppWidget(context, manager, id);
            }
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_countdown_square);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                102,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_square_root, pendingIntent);

        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        String city = prefs.getString("last_city", null);
        String district = prefs.getString("last_district", null);
        if (city != null && district != null) {
            views.setTextViewText(R.id.widget_square_location, city + ", " + district);
        } else {
            views.setTextViewText(R.id.widget_square_location, context.getString(R.string.app_name));
        }

        PrayerResponse.Data data = PrayerCacheHelper.getTodayData(context);
        if (data != null) {
            if (data.date != null && data.date.hijri != null && data.date.hijri.month != null) {
                String lang = LocaleHelper.getLanguage(context);
                String hijriMonth = AppCalculators.localizeHijriMonth(
                        data.date.hijri.month.number, data.date.hijri.month.en, lang
                );
                String hijriText = data.date.hijri.day + " " + hijriMonth + " " + data.date.hijri.year;
                views.setTextViewText(R.id.widget_square_hijri, hijriText);
            }

            if (data.timings != null) {
                bindTimings(context, views, data.timings);
            }
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
            views.setTextViewText(R.id.widget_square_prayer_name, names[nextIndex]);
            views.setTextViewText(R.id.widget_square_target_time, times[nextIndex]);

            long remainingMs = Math.max(0L, nextTargetMs - System.currentTimeMillis());
            long base = SystemClock.elapsedRealtime() + remainingMs;
            views.setChronometer(R.id.widget_square_countdown, base, null, true);
            views.setChronometerCountDown(R.id.widget_square_countdown, true);
        }
    }
}
