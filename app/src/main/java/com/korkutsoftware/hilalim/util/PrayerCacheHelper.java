package com.korkutsoftware.hilalim.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.api.PrayerApiService;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.api.MonthlyPrayerResponse;
import com.korkutsoftware.hilalim.api.RetrofitClient;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

/**
 * Manages single-day and 30-day offline prayer time caches so alarms, widgets,
 * HomeFragment and AylikVakitlerActivity share accurate daily prayer times offline.
 */
public class PrayerCacheHelper {

    private static final String TAG = "PrayerCacheHelper";
    private static final String PREFS_NAME = "hilalim_prefs";
    private static final String KEY_DAILY_CACHE = "cached_prayer_data";
    private static final String KEY_MONTHLY_CACHE = "cached_monthly_prayer_data";
    private static final String KEY_MONTHLY_META = "cached_monthly_meta";

    private static final Gson GSON = new Gson();

    public static class LocationSpec {
        public final float lat;
        public final float lng;
        public final String city;
        public final String district;
        public final String locationKey;

        public LocationSpec(float lat, float lng, String city, String district, String locationKey) {
            this.lat = lat;
            this.lng = lng;
            this.city = city;
            this.district = district;
            this.locationKey = locationKey;
        }
    }

    public static LocationSpec getCurrentLocationSpec(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        float lat = prefs.getFloat("last_lat", 0f);
        float lng = prefs.getFloat("last_lng", 0f);
        String manualCity = prefs.getString("last_city", null);
        String manualDistrict = prefs.getString("last_district", null);

        String locationKey;
        if (manualCity != null && manualDistrict != null) {
            locationKey = "manual:" + manualCity + "," + manualDistrict;
        } else if (lat != 0f && lng != 0f) {
            locationKey = "coords:" + lat + "," + lng;
        } else {
            locationKey = "default:Istanbul,Fatih";
        }
        return new LocationSpec(lat, lng, manualCity, manualDistrict, locationKey);
    }

    public static Call<MonthlyPrayerResponse> createMonthlyCall(LocationSpec spec, int month, int year) {
        PrayerApiService apiService = RetrofitClient.getApiService();
        if (spec.lat != 0f && spec.lng != 0f) {
            return apiService.getMonthlyCalendar(spec.lat, spec.lng, 13, month, year);
        } else if (spec.city != null && spec.district != null) {
            return apiService.getMonthlyCalendarByAddress(spec.district + "," + spec.city + ",Turkey", 13, month, year);
        } else {
            return apiService.getMonthlyCalendarByAddress("Fatih,İstanbul,Turkey", 13, month, year);
        }
    }

    public static String buildMonthlyMetaKey(String locationKey, int month, int year) {
        return locationKey + "|" + year + "-" + month;
    }

    public static void saveMonthlyCalendar(Context context, List<PrayerResponse.Data> days,
                                           String locationKey, int month, int year) {
        if (context == null || days == null || days.isEmpty()) return;
        try {
            String json = GSON.toJson(days);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_MONTHLY_CACHE, json)
                    .putString(KEY_MONTHLY_META, buildMonthlyMetaKey(locationKey, month, year))
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving monthly calendar", e);
        }
    }

    public static boolean hasMonthlyCacheFor(Context context, String locationKey, int month, int year) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String expected = buildMonthlyMetaKey(locationKey, month, year);
        return expected.equals(prefs.getString(KEY_MONTHLY_META, "")) && prefs.contains(KEY_MONTHLY_CACHE);
    }

    public static List<PrayerResponse.Data> loadMonthlyCalendar(Context context, String locationKey, int month, int year) {
        if (context == null || !hasMonthlyCacheFor(context, locationKey, month, year)) return null;
        return readMonthlyList(context);
    }

    private static List<PrayerResponse.Data> readMonthlyList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_MONTHLY_CACHE, null);
        if (json == null) return null;
        try {
            Type type = new TypeToken<List<PrayerResponse.Data>>() {}.getType();
            return GSON.fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing monthly cache", e);
            return null;
        }
    }

    public static PrayerResponse.Data getDataForDate(Context context, Calendar cal) {
        if (context == null || cal == null) return null;
        String targetDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.getTime());
        List<PrayerResponse.Data> monthly = readMonthlyList(context);
        if (monthly != null) {
            for (PrayerResponse.Data item : monthly) {
                if (item != null && item.date != null && item.date.gregorian != null
                        && targetDate.equals(item.date.gregorian.date)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static PrayerResponse.Data getTodayData(Context context) {
        if (context == null) return null;
        PrayerResponse.Data fromMonthly = getDataForDate(context, Calendar.getInstance());
        if (fromMonthly != null && fromMonthly.timings != null) {
            return fromMonthly;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedJson = prefs.getString(KEY_DAILY_CACHE, null);
        if (cachedJson != null) {
            try {
                return GSON.fromJson(cachedJson, PrayerResponse.Data.class);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing daily cache", e);
            }
        }
        return null;
    }

    public static PrayerResponse.Timings getTimingsForDateOrFallback(Context context, Calendar cal,
                                                                     PrayerResponse.Timings fallback) {
        PrayerResponse.Data day = getDataForDate(context, cal);
        if (day != null && day.timings != null) {
            return day.timings;
        }
        return fallback;
    }
}
