package com.korkutsoftware.hilalim.util;

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
}
