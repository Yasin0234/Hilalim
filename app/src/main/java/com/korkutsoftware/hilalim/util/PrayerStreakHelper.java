package com.korkutsoftware.hilalim.util;

import com.korkutsoftware.hilalim.data.db.PrayerLogEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PrayerStreakHelper {

    public static final String FAJR = "fajr";
    public static final String DHUHR = "dhuhr";
    public static final String ASR = "asr";
    public static final String MAGHRIB = "maghrib";
    public static final String ISHA = "isha";
    public static final String WITR = "witr";

    public static final String[] ALL_6_PRAYERS = {FAJR, DHUHR, ASR, MAGHRIB, ISHA, WITR};
    public static final String[] MAIN_5_PRAYERS = {FAJR, DHUHR, ASR, MAGHRIB, ISHA};

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * Checks if all 5 farz prayers of the day are recorded as prayed or prayed in congregation.
     */
    public static boolean isDayCompleted(List<PrayerLogEntity> dayLogs) {
        if (dayLogs == null || dayLogs.isEmpty()) return false;
        Set<String> completed = new HashSet<>();
        for (PrayerLogEntity log : dayLogs) {
            if (log.status == PrayerLogEntity.STATUS_PRAYED || log.status == PrayerLogEntity.STATUS_CONGREGATION) {
                completed.add(log.prayerKey);
            }
        }
        for (String p : MAIN_5_PRAYERS) {
            if (!completed.contains(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns how many of the 5 main prayers were prayed for this day (0 to 5).
     */
    public static int getDayCompletedCount(List<PrayerLogEntity> dayLogs) {
        if (dayLogs == null || dayLogs.isEmpty()) return 0;
        int count = 0;
        Set<String> counted = new HashSet<>();
        for (PrayerLogEntity log : dayLogs) {
            if ((log.status == PrayerLogEntity.STATUS_PRAYED || log.status == PrayerLogEntity.STATUS_CONGREGATION)
                    && !counted.contains(log.prayerKey)) {
                for (String p : MAIN_5_PRAYERS) {
                    if (p.equals(log.prayerKey)) {
                        count++;
                        counted.add(log.prayerKey);
                        break;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Calculates consecutive days streak ending at today (or yesterday if today is not completed yet).
     */
    public static int calculateStreak(List<PrayerLogEntity> allLogs, Calendar referenceDate) {
        if (allLogs == null || allLogs.isEmpty() || referenceDate == null) return 0;

        Map<String, List<PrayerLogEntity>> mapByDate = new HashMap<>();
        for (PrayerLogEntity log : allLogs) {
            if (!mapByDate.containsKey(log.dateKey)) {
                mapByDate.put(log.dateKey, new java.util.ArrayList<>());
            }
            mapByDate.get(log.dateKey).add(log);
        }

        Calendar cursor = (Calendar) referenceDate.clone();
        String todayKey = DATE_FORMAT.format(cursor.getTime());
        boolean todayCompleted = isDayCompleted(mapByDate.get(todayKey));

        int streak = 0;
        if (todayCompleted) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        } else {
            // Check if streak is unbroken up to yesterday
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }

        while (true) {
            String dateKey = DATE_FORMAT.format(cursor.getTime());
            if (isDayCompleted(mapByDate.get(dateKey))) {
                streak++;
                cursor.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Calculates completion rate for the month so far (0% to 100%).
     */
    public static int calculateMonthlySuccessRate(List<PrayerLogEntity> monthLogs, int dayOfMonth) {
        if (dayOfMonth <= 0) return 0;
        int totalExpected = dayOfMonth * 5;
        if (monthLogs == null || monthLogs.isEmpty()) return 0;

        int totalPrayed = 0;
        for (PrayerLogEntity log : monthLogs) {
            if (log.status == PrayerLogEntity.STATUS_PRAYED || log.status == PrayerLogEntity.STATUS_CONGREGATION) {
                for (String p : MAIN_5_PRAYERS) {
                    if (p.equals(log.prayerKey)) {
                        totalPrayed++;
                        break;
                    }
                }
            }
        }
        int percent = Math.round(((float) totalPrayed / (float) totalExpected) * 100f);
        return Math.max(0, Math.min(100, percent));
    }

    /**
     * Sum of 6 kaza prayer debts.
     */
    public static int calculateTotalKazaBorcu(int fajr, int dhuhr, int asr, int maghrib, int isha, int witr) {
        return Math.max(0, fajr)
                + Math.max(0, dhuhr)
                + Math.max(0, asr)
                + Math.max(0, maghrib)
                + Math.max(0, isha)
                + Math.max(0, witr);
    }

    /**
     * Calculates completion percentage for kaza debt.
     */
    public static int calculateKazaProgress(int currentRemaining, int initialTotal) {
        if (initialTotal <= 0 || currentRemaining >= initialTotal) return 0;
        if (currentRemaining <= 0) return 100;
        int completed = initialTotal - currentRemaining;
        int percent = Math.round(((float) completed / (float) initialTotal) * 100f);
        return Math.max(0, Math.min(100, percent));
    }
}
