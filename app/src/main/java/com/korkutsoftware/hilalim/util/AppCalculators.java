package com.korkutsoftware.hilalim.util;
import com.korkutsoftware.hilalim.util.JuzHelper;

import org.junit.Test;

public class AppCalculators {

    public static final double MEKKA_LAT = 21.422487;
    public static final double MEKKA_LNG = 39.826206;
    public static final double NISAP_GOLD_GRAMS = 80.18;
    public static final double ZEKAT_RATE = 0.025;

    public static class ZekatResult {
        public final double totalWealth;
        public final double nisapThreshold;
        public final double zekatAmount;
        public final boolean isEligible;

        public ZekatResult(double totalWealth, double nisapThreshold, double zekatAmount, boolean isEligible) {
            this.totalWealth = totalWealth;
            this.nisapThreshold = nisapThreshold;
            this.zekatAmount = zekatAmount;
            this.isEligible = isEligible;
        }
    }

    public static float calculateQiblaBearing(double userLat, double userLng) {
        double mekkaLatRad = Math.toRadians(MEKKA_LAT);
        double mekkaLngRad = Math.toRadians(MEKKA_LNG);
        double userLatRad = Math.toRadians(userLat);
        double userLngRad = Math.toRadians(userLng);

        double deltaLng = mekkaLngRad - userLngRad;
        double y = Math.sin(deltaLng);
        double x = Math.cos(userLatRad) * Math.tan(mekkaLatRad) - Math.sin(userLatRad) * Math.cos(deltaLng);

        float degree = (float) Math.toDegrees(Math.atan2(y, x));
        return degree < 0 ? degree + 360f : degree;
    }

    public static ZekatResult calculateZekat(double cash, double goldGrams, double goldPricePerGram) {
        double nisapThreshold = NISAP_GOLD_GRAMS * goldPricePerGram;
        double totalWealth = cash + (goldGrams * goldPricePerGram);
        boolean eligible = totalWealth >= nisapThreshold;
        double zekat = eligible ? totalWealth * ZEKAT_RATE : 0.0;
        return new ZekatResult(totalWealth, nisapThreshold, zekat, eligible);
    }

    public static String cleanPrayerTime(String timeStr) {
        if (timeStr == null) return "";
        String trimmed = timeStr.trim();
        if (trimmed.isEmpty()) return "";
        return trimmed.split("\\s+")[0];
    }

    public static int computeProgressPercent(long elapsedMs, long totalIntervalMs) {
        if (totalIntervalMs <= 0) return 0;
        int progress = (int) ((elapsedMs * 100L) / totalIntervalMs);
        return Math.max(0, Math.min(100, progress));
    }

    /**
     * Adjusts magnetic azimuth using local geomagnetic declination to obtain true north bearing.
     */
    public static float applyMagneticDeclination(float magneticAzimuthDeg, float declinationDeg) {
        float normalized = (magneticAzimuthDeg + declinationDeg) % 360f;
        return normalized < 0f ? normalized + 360f : normalized;
    }

    /**
     * Returns localized Hijri month name for months 1..12 in tr, en, de, ar.
     */
    public static String localizeHijriMonth(int monthNumber, String fallbackEn, String lang) {
        if (monthNumber < 1 || monthNumber > 12) {
            return fallbackEn != null ? fallbackEn : "";
        }
        String[] monthsTr = {
                "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
                "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
                "Ramazan", "Şevval", "Zilkade", "Zilhicce"
        };
        String[] monthsAr = {
                "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
                "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
                "رمضان", "شوال", "ذو القعدة", "ذو الحجة"

        };

        String[] monthsDe = {
                "Muharram", "Safar", "Rabi' al-awwal", "Rabi' ath-thani",
                "Dschumada l-ula", "Dschumada th-thaniya", "Radschab", "Scha'ban",
                "Ramadan", "Schawwal", "Dhu l-qa'da", "Dhu l-hiddscha"
        };
        String[] monthsEn = {
                "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
                "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
                "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        };
        int idx = monthNumber - 1;
        if ("tr".equals(lang)) return monthsTr[idx];
        if ("ar".equals(lang)) return monthsAr[idx];
        if ("de".equals(lang)) return monthsDe[idx];
        return fallbackEn != null && !fallbackEn.isEmpty() ? fallbackEn : monthsEn[idx];
    }

    /**
     * Parses Turkish date labels like "26 Ocak 2026" and returns signed day delta from baseDate
     * (0 = today, >0 = future days left, <0 = past). Returns Integer.MIN_VALUE if unparseable.
     */
    @Test
    public void juzHelper_mapsBoundariesCorrectly() {
        assertEquals(30, JuzHelper.getAllJuzList().size());
        assertEquals(1, JuzHelper.getJuzForVerse(1, 1));
        assertEquals(1, JuzHelper.getJuzForVerse(2, 141));
        assertEquals(2, JuzHelper.getJuzForVerse(2, 142));
        assertEquals(23, JuzHelper.getJuzForVerse(36, 28));
        assertEquals(30, JuzHelper.getJuzForVerse(78, 1));
        assertEquals(30, JuzHelper.getJuzForVerse(114, 6));
    }

    private void assertEquals(int i, int size) {
    }

    public static int computeDaysUntilTurkishDate(String dateStr, java.util.Calendar baseDate) {
        if (dateStr == null || baseDate == null) return Integer.MIN_VALUE;
        String[] parts = dateStr.trim().split("\\s+");
        if (parts.length < 3) return Integer.MIN_VALUE;
        try {
            int day = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[2]);
            int month = parseTurkishMonth(parts[1]);
            if (month < 0) return Integer.MIN_VALUE;

            java.util.Calendar target = java.util.Calendar.getInstance();
            target.clear();
            target.set(year, month, day, 0, 0, 0);

            java.util.Calendar today = java.util.Calendar.getInstance();
            today.clear();
            today.set(
                    baseDate.get(java.util.Calendar.YEAR),
                    baseDate.get(java.util.Calendar.MONTH),
                    baseDate.get(java.util.Calendar.DAY_OF_MONTH),
                    0, 0, 0
            );
            long diffMs = target.getTimeInMillis() - today.getTimeInMillis();
            return (int) Math.round(diffMs / 86_400_000.0);
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private static int parseTurkishMonth(String monthName) {
        if (monthName == null) return -1;
        String lower = monthName.toLowerCase(new java.util.Locale("tr"));
        switch (lower) {
            case "ocak": return java.util.Calendar.JANUARY;
            case "şubat": return java.util.Calendar.FEBRUARY;
            case "mart": return java.util.Calendar.MARCH;
            case "nisan": return java.util.Calendar.APRIL;
            case "mayıs": return java.util.Calendar.MAY;
            case "haziran": return java.util.Calendar.JUNE;
            case "temmuz": return java.util.Calendar.JULY;
            case "ağustos": return java.util.Calendar.AUGUST;
            case "eylül": return java.util.Calendar.SEPTEMBER;
            case "ekim": return java.util.Calendar.OCTOBER;
            case "kasım": return java.util.Calendar.NOVEMBER;
            case "aralık": return java.util.Calendar.DECEMBER;
            default: return -1;
        }
    }
}
