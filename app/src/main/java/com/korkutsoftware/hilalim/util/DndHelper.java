package com.korkutsoftware.hilalim.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/**
 * Namaz vakti sonrasında telefonu geçici olarak Rahatsız Etmeyin (DND) moduna alır ve geri döndürür.
 */
public class DndHelper {

    public static final String PREF_DND_ENABLED = "notif_dnd_enabled";
    private static final String KEY_PREV_FILTER = "dnd_prev_filter";
    private static final String KEY_DND_ACTIVE_BY_APP = "dnd_active_by_app";

    public static boolean hasPolicyAccess(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    public static void openPolicySettings(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void enableDndForPrayer(Context context) {
        if (!hasPolicyAccess(context)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        int current = nm.getCurrentInterruptionFilter();
        if (current != NotificationManager.INTERRUPTION_FILTER_ALL) {
            return; // Kullanıcı zaten kendisi DND açmışsa dokunma
        }
        prefs.edit()
                .putInt(KEY_PREV_FILTER, current)
                .putBoolean(KEY_DND_ACTIVE_BY_APP, true)
                .apply();
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
    }

    public static void restoreDndAfterPrayer(Context context) {
        if (!hasPolicyAccess(context)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_DND_ACTIVE_BY_APP, false)) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int prev = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL);
            nm.setInterruptionFilter(prev);
        }
        prefs.edit().putBoolean(KEY_DND_ACTIVE_BY_APP, false).apply();
    }
}