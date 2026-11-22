package com.korkutsoftware.hilalim.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.model.CityData;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class LocationHelper {

    private static final String TAG = "LocationHelper";
    public static final String PREFS_NAME = "hilalim_prefs";

    public interface CityDataCallback {
        void onLoaded(List<String> cityNames, Map<String, List<String>> cityDistricts);
    }

    public interface LocationResultCallback {
        void onLocationFound(Location location);
        void onLocationFailed();
    }

    public static void loadCitiesAsync(@NonNull Context context, @NonNull CityDataCallback callback) {
        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream is = appContext.getAssets().open("turkey_cities.json")) {
                int size = is.available();
                byte[] buffer = new byte[size];
                int offset = 0;
                while (offset < size) {
                    int read = is.read(buffer, offset, size - offset);
                    if (read == -1) break;
                    offset += read;
                }
                String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
                Type listType = new TypeToken<List<CityData>>() {}.getType();
                List<CityData> cities = new Gson().fromJson(json, listType);
                if (cities != null) {
                    List<String> names = new ArrayList<>();
                    Map<String, List<String>> districts = new HashMap<>();
                    for (CityData cityData : cities) {
                        names.add(cityData.getCity());
                        districts.put(cityData.getCity(), cityData.getDistricts());
                    }
                    mainHandler.post(() -> callback.onLoaded(names, districts));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading city data", e);
            }
        });
    }

    public static boolean isLocationEnabled(@NonNull Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        );
    }

    public static void saveManualLocation(@NonNull Context context, @NonNull String city, @NonNull String district) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("last_city", city)
                .putString("last_district", district)
                .putFloat("last_lat", 0)
                .putFloat("last_lng", 0)
                .remove("last_fetched_date")
                .remove("last_fetched_location")
                .apply();
        AlarmHelper.cancelAllAlarms(context);
    }

    public static void saveCoordinates(@NonNull Context context, @NonNull Location location) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("last_lat", (float) location.getLatitude())
                .putFloat("last_lng", (float) location.getLongitude())
                .remove("last_city")
                .remove("last_district")
                .remove("last_fetched_date")
                .remove("last_fetched_location")
                .apply();
        AlarmHelper.cancelAllAlarms(context);
    }

    @SuppressLint("MissingPermission")
    public static void fetchCurrentLocation(
            @NonNull Context context,
            @NonNull CancellationTokenSource cancellationTokenSource,
            @NonNull LocationResultCallback callback
    ) {
        boolean hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasCoarse && !hasFine) {
            callback.onLocationFailed();
            return;
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        fusedClient.getLastLocation().addOnSuccessListener(lastLocation -> {
            if (lastLocation != null) {
                saveCoordinates(context, lastLocation);
                callback.onLocationFound(lastLocation);
            } else {
                fusedClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.getToken()
                ).addOnSuccessListener(currentLocation -> {
                    if (currentLocation != null) {
                        saveCoordinates(context, currentLocation);
                        callback.onLocationFound(currentLocation);
                    } else {
                        callback.onLocationFailed();
                    }
                }).addOnFailureListener(e -> callback.onLocationFailed());
            }
        }).addOnFailureListener(e -> callback.onLocationFailed());
    }
}
