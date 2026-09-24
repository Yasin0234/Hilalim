package com.korkutsoftware.hilalim;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.model.CityData;
import com.korkutsoftware.hilalim.util.AlarmHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class LocationActivity extends BaseActivity {

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private AutoCompleteTextView spinnerCity, spinnerDistrict;
    private final Map<String, List<String>> cityDistricts = new HashMap<>();
    private final List<String> cityNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);

        setupLocationSelection();

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if ((fineLocationGranted != null && fineLocationGranted) || 
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                        fetchAndSaveLocation(() -> {
                            Toast.makeText(this, R.string.location_saved, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        Toast.makeText(this, R.string.toast_location_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btn_auto_location).setOnClickListener(v -> requestLocationPermissions());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            String city = spinnerCity.getText().toString();
            String district = spinnerDistrict.getText().toString();
            if (city.isEmpty() || district.isEmpty()) {
                Toast.makeText(this, R.string.toast_select_city_district, Toast.LENGTH_SHORT).show();
            } else {
                saveManualLocation(city, district);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void setupLocationSelection() {
        loadCityData();

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cityNames);
        spinnerCity.setAdapter(cityAdapter);

        View districtContainer = findViewById(R.id.input_district_container);

        spinnerCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            List<String> districts = cityDistricts.getOrDefault(selectedCity, new ArrayList<>());
            ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, districts);
            
            if (districtContainer != null) {
                districtContainer.setVisibility(View.VISIBLE);
            }
            
            spinnerDistrict.setText("");
            spinnerDistrict.setAdapter(districtAdapter);
        });
    }

    private void loadCityData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream is = getAssets().open("turkey_cities.json")) {
                int size = is.available();
                byte[] buffer = new byte[size];
                int read = is.read(buffer);
                if (read != -1) {
                    String json = new String(buffer, StandardCharsets.UTF_8);
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<CityData>>() {}.getType();
                    List<CityData> cities = gson.fromJson(json, listType);

                    if (cities != null) {
                        List<String> names = new ArrayList<>();
                        Map<String, List<String>> districts = new HashMap<>();
                        for (CityData cityData : cities) {
                            names.add(cityData.getCity());
                            districts.put(cityData.getCity(), cityData.getDistricts());
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            cityNames.clear();
                            cityNames.addAll(names);
                            cityDistricts.clear();
                            cityDistricts.putAll(districts);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        );
    }

    private void requestLocationPermissions() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, R.string.location_disabled, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (Exception ignored) {}
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void saveManualLocation(String city, String district) {
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("last_city", city)
                .putString("last_district", district)
                .putFloat("last_lat", 0) 
                .putFloat("last_lng", 0)
                .apply();
        AlarmHelper.cancelAllAlarms(this);
    }

    @SuppressLint("MissingPermission")
    private void fetchAndSaveLocation(Runnable onComplete) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (onComplete != null) onComplete.run();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (location != null) {
            saveLocationData(location);
            if (onComplete != null) onComplete.run();
        } else {
            // Last known location is null, request single update
            String provider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?
                    LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;

            if (!locationManager.isProviderEnabled(provider)) {
                Toast.makeText(this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
                return;
            }

            Toast.makeText(this, R.string.location_fetching, Toast.LENGTH_SHORT).show();

            locationManager.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location loc) {
                    saveLocationData(loc);
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            }, Looper.getMainLooper());
        }
    }

    private void saveLocationData(Location location) {
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        prefs.edit()
                .putFloat("last_lat", (float) location.getLatitude())
                .putFloat("last_lng", (float) location.getLongitude())
                .remove("last_city")
                .remove("last_district")
                .apply();
        AlarmHelper.cancelAllAlarms(this);
    }
}
