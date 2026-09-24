package com.korkutsoftware.hilalim;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.korkutsoftware.hilalim.util.AlarmHelper;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "Hilalim_Prayer_Channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Re-schedule prayer alarms after device reboot
            AlarmHelper.rescheduleAlarmsFromCache(context);
            return;
        }

        String prayerKey = intent.getStringExtra("prayer_key");
        int typeIndex = intent.getIntExtra("type_index", AlarmHelper.TYPE_EXACT);

        if (prayerKey != null) {
            showNotification(context, prayerKey, typeIndex);
        }
    }

    private void showNotification(Context context, String prayerKey, int typeIndex) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        int prayerResId;
        switch (prayerKey) {
            case "fajr":
                prayerResId = R.string.imsak;
                break;
            case "sunrise":
                prayerResId = R.string.gunes;
                break;
            case "dhuhr":
                prayerResId = R.string.ogle;
                break;
            case "asr":
                prayerResId = R.string.ikindi;
                break;
            case "maghrib":
                prayerResId = R.string.aksam;
                break;
            case "isha":
                prayerResId = R.string.yatsi;
                break;
            default:
                prayerResId = R.string.imsak;
                break;
        }

        String prayerName = context.getString(prayerResId);

        int titleResId;
        int contentResId;

        if (typeIndex == AlarmHelper.TYPE_PRE_45) {
            titleResId = R.string.notification_pre_45_title;
            contentResId = R.string.notification_pre_45_content;
        } else if (typeIndex == AlarmHelper.TYPE_PRE_15) {
            titleResId = R.string.notification_pre_15_title;
            contentResId = R.string.notification_pre_15_content;
        } else {
            titleResId = R.string.notification_exact_title;
            contentResId = R.string.notification_exact_content;
        }

        String titleText = context.getString(titleResId);
        String contentText = context.getString(contentResId, prayerName);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = (int) (System.currentTimeMillis() % 100000);
        notificationManager.notify(notificationId, builder.build());
    }
}
