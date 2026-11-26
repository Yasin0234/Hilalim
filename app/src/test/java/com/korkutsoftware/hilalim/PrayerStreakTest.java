package com.korkutsoftware.hilalim;

import com.korkutsoftware.hilalim.data.db.PrayerLogEntity;
import com.korkutsoftware.hilalim.util.PrayerStreakHelper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrayerStreakTest {

    @Test
    public void testIsDayCompleted_allPrayed() {
        List<PrayerLogEntity> logs = new ArrayList<>();
        String today = "2026-11-26";
        for (String p : PrayerStreakHelper.MAIN_5_PRAYERS) {
            logs.add(new PrayerLogEntity(today, p, PrayerLogEntity.STATUS_PRAYED, System.currentTimeMillis()));
        }
        assertTrue(PrayerStreakHelper.isDayCompleted(logs));
        assertEquals(5, PrayerStreakHelper.getDayCompletedCount(logs));
    }

    @Test
    public void testIsDayCompleted_missingOne() {
        List<PrayerLogEntity> logs = new ArrayList<>();
        String today = "2026-11-26";
        logs.add(new PrayerLogEntity(today, PrayerStreakHelper.FAJR, PrayerLogEntity.STATUS_PRAYED, System.currentTimeMillis()));
        logs.add(new PrayerLogEntity(today, PrayerStreakHelper.DHUHR, PrayerLogEntity.STATUS_CONGREGATION, System.currentTimeMillis()));
        logs.add(new PrayerLogEntity(today, PrayerStreakHelper.ASR, PrayerLogEntity.STATUS_PRAYED, System.currentTimeMillis()));
        logs.add(new PrayerLogEntity(today, PrayerStreakHelper.MAGHRIB, PrayerLogEntity.STATUS_WAITING, System.currentTimeMillis()));
        logs.add(new PrayerLogEntity(today, PrayerStreakHelper.ISHA, PrayerLogEntity.STATUS_PRAYED, System.currentTimeMillis()));

        assertFalse(PrayerStreakHelper.isDayCompleted(logs));
        assertEquals(4, PrayerStreakHelper.getDayCompletedCount(logs));
    }

    @Test
    public void testCalculateStreak_consecutiveDays() {
        Calendar today = Calendar.getInstance();
        List<PrayerLogEntity> allLogs = new ArrayList<>();

        // Add 5 completed days up to today
        Calendar cursor = (Calendar) today.clone();
        for (int d = 0; d < 5; d++) {
            String dateKey = PrayerStreakHelper.DATE_FORMAT.format(cursor.getTime());
            for (String p : PrayerStreakHelper.MAIN_5_PRAYERS) {
                allLogs.add(new PrayerLogEntity(dateKey, p, PrayerLogEntity.STATUS_PRAYED, System.currentTimeMillis()));
            }
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }

        int streak = PrayerStreakHelper.calculateStreak(allLogs, today);
        assertEquals(5, streak);
    }

    @Test
    public void testCalculateMonthlySuccessRate() {
        List<PrayerLogEntity> logs = new ArrayList<>();
        // 10 days, user prayed 40 prayers out of 50 expected
        for (int i = 0; i < 40; i++) {
            logs.add(new PrayerLogEntity("2026-11-01", PrayerStreakHelper.MAIN_5_PRAYERS[i % 5], PrayerLogEntity.STATUS_PRAYED, System.currentTimeMillis()));
        }
        int rate = PrayerStreakHelper.calculateMonthlySuccessRate(logs, 10);
        assertEquals(80, rate);
    }

    @Test
    public void testCalculateTotalKazaBorcu() {
        int total = PrayerStreakHelper.calculateTotalKazaBorcu(10, 10, 10, 10, 10, 10);
        assertEquals(60, total);
    }

    @Test
    public void testCalculateKazaProgress() {
        int progress = PrayerStreakHelper.calculateKazaProgress(50, 100);
        assertEquals(50, progress);
    }
}
