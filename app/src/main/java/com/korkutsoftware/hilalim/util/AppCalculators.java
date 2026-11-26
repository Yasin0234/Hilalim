package com.korkutsoftware.hilalim.util;

import java.util.Calendar;
import java.util.Locale;

public final class AppCalculators {

    private AppCalculators() {}

    public static final double MEKKA_LAT = 21.422487;
    public static final double MEKKA_LNG = 39.826206;
    public static final double NISAP_GOLD_GRAMS = 80.18;
    public static final double ZEKAT_RATE = 0.025;

    private static final String[] HIJRI_MONTHS_TR = {
            "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
            "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
            "Ramazan", "Şevval", "Zilkade", "Zilhicce"
    };

    private static final String[] HIJRI_MONTHS_AR = {
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    private static final String[] HIJRI_MONTHS_DE = {
            "Muharram", "Safar", "Rabi' al-awwal", "Rabi' ath-thani",
            "Dschumada l-ula", "Dschumada th-thaniya", "Radschab", "Scha'ban",
            "Ramadan", "Schawwal", "Dhu l-qa'da", "Dhu l-hiddscha"
    };

    private static final String[] HIJRI_MONTHS_EN = {
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    };

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

        @Override
        public String toString() {
            return "ZekatResult{totalWealth=" + totalWealth +
                    ", nisapThreshold=" + nisapThreshold +
                    ", zekatAmount=" + zekatAmount +
                    ", isEligible=" + isEligible + '}';
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
        int spaceIdx = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                spaceIdx = i;
                break;
            }
        }
        return spaceIdx == -1 ? trimmed : trimmed.substring(0, spaceIdx);
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
        int idx = monthNumber - 1;
        if ("tr".equals(lang)) return HIJRI_MONTHS_TR[idx];
        if ("ar".equals(lang)) return HIJRI_MONTHS_AR[idx];
        if ("de".equals(lang)) return HIJRI_MONTHS_DE[idx];
        return (fallbackEn != null && !fallbackEn.isEmpty()) ? fallbackEn : HIJRI_MONTHS_EN[idx];
    }

    /**
     * Parses Turkish date labels like "26 Ocak 2026" and returns signed day delta from baseDate
     * (0 = today, >0 = future days left, <0 = past). Returns Integer.MIN_VALUE if unparseable.
     */
    public static int computeDaysUntilTurkishDate(String dateStr, Calendar baseDate) {
        if (dateStr == null || baseDate == null) return Integer.MIN_VALUE;
        String[] parts = dateStr.trim().split("\\s+");
        if (parts.length < 3) return Integer.MIN_VALUE;
        try {
            int day = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[2]);
            int month = parseTurkishMonth(parts[1]);
            if (month < 0) return Integer.MIN_VALUE;

            Calendar target = Calendar.getInstance();
            target.clear();
            target.set(year, month, day, 0, 0, 0);

            Calendar today = Calendar.getInstance();
            today.clear();
            today.set(
                    baseDate.get(Calendar.YEAR),
                    baseDate.get(Calendar.MONTH),
                    baseDate.get(Calendar.DAY_OF_MONTH),
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
        switch (monthName.toLowerCase(new Locale("tr"))) {
            case "ocak": return Calendar.JANUARY;
            case "şubat": return Calendar.FEBRUARY;
            case "mart": return Calendar.MARCH;
            case "nisan": return Calendar.APRIL;
            case "mayıs": return Calendar.MAY;
            case "haziran": return Calendar.JUNE;
            case "temmuz": return Calendar.JULY;
            case "ağustos": return Calendar.AUGUST;
            case "eylül": return Calendar.SEPTEMBER;
            case "ekim": return Calendar.OCTOBER;
            case "kasım": return Calendar.NOVEMBER;
            case "aralık": return Calendar.DECEMBER;
            default: return -1;
        }
    }
}