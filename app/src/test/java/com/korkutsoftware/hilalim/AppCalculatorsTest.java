package com.korkutsoftware.hilalim;

import com.korkutsoftware.hilalim.util.AppCalculators;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppCalculatorsTest {

    @Test
    public void qiblaBearing_fromIstanbul_isSouthEast() {
        // Istanbul ~ 41.0082 N, 28.9784 E -> Makkah ~ 151.6 deg
        float bearing = AppCalculators.calculateQiblaBearing(41.0082, 28.9784);
        assertTrue("Bearing should be between 145 and 160 for Istanbul, got " + bearing,
                bearing > 145f && bearing < 160f);
    }

    @Test
    public void qiblaBearing_isAlwaysIn0To360Range() {
        float bearing = AppCalculators.calculateQiblaBearing(40.7128, -74.0060); // New York
        assertTrue(bearing >= 0f && bearing < 360f);
    }

    @Test
    public void zekat_belowNisap_returnsZero() {
        AppCalculators.ZekatResult res = AppCalculators.calculateZekat(10000.0, 10.0, 3000.0);
        // Nisap = 80.18 * 3000 = 240,540; wealth = 10,000 + 30,000 = 40,000
        assertFalse(res.isEligible);
        assertEquals(0.0, res.zekatAmount, 0.001);
        assertEquals(240540.0, res.nisapThreshold, 0.01);
    }

    @Test
    public void zekat_aboveNisap_calculates2Point5Percent() {
        AppCalculators.ZekatResult res = AppCalculators.calculateZekat(100000.0, 100.0, 3000.0);
        // Wealth = 100,000 + 300,000 = 400,000 >= 240,540; 2.5% = 10,000
        assertTrue(res.isEligible);
        assertEquals(400000.0, res.totalWealth, 0.001);
        assertEquals(10000.0, res.zekatAmount, 0.001);
    }

    @Test
    public void cleanPrayerTime_stripsTimezoneAndWhitespace() {
        assertEquals("05:43", AppCalculators.cleanPrayerTime("05:43 (EET)"));
        assertEquals("13:21", AppCalculators.cleanPrayerTime("  13:21 (+03) "));
        assertEquals("", AppCalculators.cleanPrayerTime(null));
        assertEquals("", AppCalculators.cleanPrayerTime("   "));
    }

    @Test
    public void computeProgressPercent_handlesZeroAndClamping() {
        assertEquals(0, AppCalculators.computeProgressPercent(1000, 0));
        assertEquals(0, AppCalculators.computeProgressPercent(-500, 1000));
        assertEquals(50, AppCalculators.computeProgressPercent(500, 1000));
        assertEquals(100, AppCalculators.computeProgressPercent(1500, 1000));
    }

    @Test
    public void applyMagneticDeclination_normalizesAcrossZeroAnd360() {
        assertEquals(5.5f, AppCalculators.applyMagneticDeclination(359.5f, 6.0f), 0.01f);
        assertEquals(356.0f, AppCalculators.applyMagneticDeclination(2.0f, -6.0f), 0.01f);
        assertEquals(156.0f, AppCalculators.applyMagneticDeclination(150.0f, 6.0f), 0.01f);
    }

    @Test
    public void localizeHijriMonth_supportsAllFourLanguages() {
        assertEquals("Ramazan", AppCalculators.localizeHijriMonth(9, "Ramaḍān", "tr"));
        assertEquals("رمضان", AppCalculators.localizeHijriMonth(9, "Ramaḍān", "ar"));
        assertEquals("Ramadan", AppCalculators.localizeHijriMonth(9, "Ramaḍān", "de"));
        assertEquals("Ramaḍān", AppCalculators.localizeHijriMonth(9, "Ramaḍān", "en"));
        assertEquals("Şaban", AppCalculators.localizeHijriMonth(8, "Sha'ban", "tr"));
    }

    @Test
    public void computeDaysUntilTurkishDate_handlesFutureTodayPast() {
        java.util.Calendar base = java.util.Calendar.getInstance();
        base.set(2026, java.util.Calendar.MARCH, 20, 10, 0, 0);

        assertEquals(0, AppCalculators.computeDaysUntilTurkishDate("20 Mart 2026", base));
        assertEquals(26, AppCalculators.computeDaysUntilTurkishDate("15 Nisan 2026", base));
        assertEquals(-18, AppCalculators.computeDaysUntilTurkishDate("2 Mart 2026", base));
        assertEquals(Integer.MIN_VALUE, AppCalculators.computeDaysUntilTurkishDate("invalid", base));
    }
}
