package com.korkutsoftware.hilalim;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.CancellationTokenSource;
import com.korkutsoftware.hilalim.util.LocationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationActivity extends BaseActivity {

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private AutoCompleteTextView spinnerCity, spinnerDistrict;
    private final Map<String, List<String>> cityDistricts = new HashMap<>();
    private final List<String> cityNames = new ArrayList<>();
    private ArrayAdapter<String> cityAdapter;
    private final CancellationTokenSource locationCancelToken = new CancellationTokenSource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);
        applySystemBarInsets(findViewById(android.R.id.content));

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
                        Toast.makeText(this, R.string.location_fetching, Toast.LENGTH_SHORT).show();
                        LocationHelper.fetchCurrentLocation(this, locationCancelToken, new LocationHelper.LocationResultCallback() {
                            @Override
                            public void onLocationFound(Location location) {
                                if (!isFinishing() && !isDestroyed()) {
                                    Toast.makeText(LocationActivity.this, R.string.location_saved, Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            }

                            @Override
                            public void onLocationFailed() {
                                if (!isFinishing() && !isDestroyed()) {
                                    Toast.makeText(LocationActivity.this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                                }
                            }
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
                LocationHelper.saveManualLocation(this, city, district);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void setupLocationSelection() {
        LocationHelper.loadCitiesAsync(this, (names, districts) -> {
            if (isFinishing() || isDestroyed()) return;
            cityNames.clear();
            cityNames.addAll(names);
            cityDistricts.clear();
            cityDistricts.putAll(districts);
            cityAdapter = new ArrayAdapter<>(LocationActivity.this, android.R.layout.simple_dropdown_item_1line, cityNames);
            spinnerCity.setAdapter(cityAdapter);
        });

        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cityNames);
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

    private void requestLocationPermissions() {
        if (!LocationHelper.isLocationEnabled(this)) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationCancelToken.cancel();
    }
}
