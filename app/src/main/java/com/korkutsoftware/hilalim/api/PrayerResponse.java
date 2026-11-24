package com.korkutsoftware.hilalim.api;

import com.google.gson.annotations.SerializedName;

public class PrayerResponse {
    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("timings")
        public Timings timings;
        
        @SerializedName("date")
        public DateInfo date;

        @SerializedName("meta")
        public Meta meta;
    }

    public static class Meta {
        @SerializedName("latitude")
        public double latitude;
        @SerializedName("longitude")
        public double longitude;
    }

    public static class Timings {
        @SerializedName("Fajr")
        public String fajr;
        @SerializedName("Sunrise")
        public String sunrise;
        @SerializedName("Dhuhr")
        public String dhuhr;
        @SerializedName("Asr")
        public String asr;
        @SerializedName("Maghrib")
        public String maghrib;
        @SerializedName("Isha")
        public String isha;
    }
    
    public static class DateInfo {
        @SerializedName("hijri")
        public Hijri hijri;
        @SerializedName("gregorian")
        public Gregorian gregorian;
        @SerializedName("readable")
        public String readable;
    }

    public static class Gregorian {
        @SerializedName("date")
        public String date;
    }

    public static class Hijri {
        @SerializedName("day")
        public String day;
        @SerializedName("month")
        public Month month;
        @SerializedName("year")
        public String year;
    }

    public static class Month {
        @SerializedName("number")
        public int number;
        @SerializedName("en")
        public String en;
    }
}