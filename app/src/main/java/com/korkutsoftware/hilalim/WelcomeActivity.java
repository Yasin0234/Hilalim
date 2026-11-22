package com.korkutsoftware.hilalim;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.card.MaterialCardView;
import com.korkutsoftware.hilalim.util.LocaleHelper;
import com.korkutsoftware.hilalim.util.LocationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WelcomeActivity extends BaseActivity {

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private View stepGreeting, stepLanguage, stepLocation, onboardingScroll;
    private TextView tvGreetingAnimated;
    private AutoCompleteTextView spinnerCity, spinnerDistrict;
    
    private String greetingText;
    private int greetingCharIndex = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String selectedLang = "tr";

    private final Map<String, List<String>> cityDistricts = new HashMap<>();
    private final List<String> cityNames = new ArrayList<>();
    private ArrayAdapter<String> cityAdapter;

    private boolean greetingFinished = false;
    private boolean languageOnly = false;
    private static final String KEY_GREETING_FINISHED = "greeting_finished";
    private final CancellationTokenSource locationCancelToken = new CancellationTokenSource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        if (savedInstanceState != null) {
            greetingFinished = savedInstanceState.getBoolean(KEY_GREETING_FINISHED, false);
        }
        languageOnly = getIntent().getBooleanExtra("language_only", false);

        greetingText = getString(R.string.welcome_greeting);
        stepGreeting = findViewById(R.id.step_greeting);
        stepLanguage = findViewById(R.id.step_language);
        stepLocation = findViewById(R.id.step_location);
        onboardingScroll = findViewById(R.id.onboarding_scroll);
        tvGreetingAnimated = findViewById(R.id.tv_greeting_animated);
        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);

        setupLocationSelection();

        findViewById(R.id.btn_start).setOnClickListener(v -> proceedToNextStep());

        findViewById(R.id.card_tr).setOnClickListener(v -> selectLanguage("tr"));
        findViewById(R.id.card_en).setOnClickListener(v -> selectLanguage("en"));
        findViewById(R.id.card_ar).setOnClickListener(v -> selectLanguage("ar"));
        findViewById(R.id.card_de).setOnClickListener(v -> selectLanguage("de"));
        findViewById(R.id.btn_lang_continue).setOnClickListener(v -> updateLocale(selectedLang));

        String initialLang = LocaleHelper.isLanguageSelected(this) ? LocaleHelper.getLanguage(this) : "tr";
        selectLanguage(LocaleHelper.isSupported(initialLang) ? initialLang : "tr");

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if ((fineLocationGranted != null && fineLocationGranted) || 
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                        completeOnboarding(false);
                    } else {
                        Toast.makeText(this, R.string.toast_location_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btn_auto_location).setOnClickListener(v -> requestLocationPermissions());
        findViewById(R.id.btn_finish).setOnClickListener(v -> {
            String city = spinnerCity.getText().toString();
            String district = spinnerDistrict.getText().toString();
            if (city.isEmpty() || district.isEmpty()) {
                Toast.makeText(this, R.string.toast_select_city_district, Toast.LENGTH_SHORT).show();
            } else {
                LocationHelper.saveManualLocation(this, city, district);
                completeOnboarding(true);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            View logoCard = findViewById(R.id.logo_card);
            if (logoCard != null) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) logoCard.getLayoutParams();
                lp.topMargin = systemBars.top + (int)(120 * getResources().getDisplayMetrics().density);
                logoCard.setLayoutParams(lp);
            }
            return insets;
        });

        if (languageOnly) {
            showLanguageStep();
        } else if (greetingFinished) {
            stepGreeting.setVisibility(View.GONE);
            if (LocaleHelper.isLanguageSelected(this)) {
                showLocationStep();
            } else {
                showLanguageStep();
            }
        } else {
            startGreetingAnimation();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_GREETING_FINISHED, greetingFinished);
    }

    private void startGreetingAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (greetingCharIndex <= greetingText.length()) {
                    tvGreetingAnimated.setText(greetingText.substring(0, greetingCharIndex));
                    greetingCharIndex++;
                    handler.postDelayed(this, 100);
                } else {
                    greetingFinished = true;
                    handler.postDelayed(() -> proceedToNextStep(), 1000);
                }
            }
        }, 500);
    }

    private void proceedToNextStep() {
        showLanguageStep();
    }

    private void showLanguageStep() {
        stepGreeting.setVisibility(View.GONE);
        onboardingScroll.setVisibility(View.VISIBLE);
        stepLocation.setVisibility(View.GONE);
        stepLanguage.setVisibility(View.VISIBLE);
    }

    private void showLocationStep() {
        stepGreeting.setVisibility(View.GONE);
        onboardingScroll.setVisibility(View.VISIBLE);
        stepLanguage.setVisibility(View.GONE);
        stepLocation.setVisibility(View.VISIBLE);
    }

    private void setupLocationSelection() {
        LocationHelper.loadCitiesAsync(this, (names, districts) -> {
            if (isFinishing() || isDestroyed()) return;
            cityNames.clear();
            cityNames.addAll(names);
            cityDistricts.clear();
            cityDistricts.putAll(districts);
            cityAdapter = new ArrayAdapter<>(WelcomeActivity.this, android.R.layout.simple_dropdown_item_1line, cityNames);
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
            String[] permissions = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
            locationPermissionLauncher.launch(permissions);
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void selectLanguage(String lang) {
        selectedLang = lang;

        resetCardState(R.id.card_tr, R.id.radio_tr);
        resetCardState(R.id.card_en, R.id.radio_en);
        resetCardState(R.id.card_ar, R.id.radio_ar);
        resetCardState(R.id.card_de, R.id.radio_de);

        int cardId = 0, radioId = 0;
        switch (lang) {
            case "tr": cardId = R.id.card_tr; radioId = R.id.radio_tr; break;
            case "en": cardId = R.id.card_en; radioId = R.id.radio_en; break;
            case "ar": cardId = R.id.card_ar; radioId = R.id.radio_ar; break;
            case "de": cardId = R.id.card_de; radioId = R.id.radio_de; break;
        }

        if (cardId != 0) {
            MaterialCardView card = findViewById(cardId);
            RadioButton radio = findViewById(radioId);
            
            card.setStrokeColor(ContextCompat.getColor(this, R.color.sacred_gold));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.selected_lang_bg));
            radio.setChecked(true);
        }
    }

    private void resetCardState(int cardId, int radioId) {
        MaterialCardView card = findViewById(cardId);
        RadioButton radio = findViewById(radioId);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true);
        int outlineColor = ContextCompat.getColor(this, typedValue.resourceId);

        card.setStrokeColor(outlineColor);
        card.setCardBackgroundColor(Color.TRANSPARENT);
        radio.setChecked(false);
    }

    private void updateLocale(String lang) {
        LocaleHelper.setLocale(this, lang);
        if (languageOnly) {
            finish();
            return;
        }
        greetingFinished = true;
        recreate();
    }

    private void completeOnboarding(boolean isManual) {
        if (!isManual) {
            Toast.makeText(this, R.string.location_fetching, Toast.LENGTH_SHORT).show();
            LocationHelper.fetchCurrentLocation(this, locationCancelToken, new LocationHelper.LocationResultCallback() {
                @Override
                public void onLocationFound(Location location) {
                    if (!isFinishing() && !isDestroyed()) {
                        navigateToDashboard();
                    }
                }

                @Override
                public void onLocationFailed() {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(WelcomeActivity.this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    }
                }
            });
        } else {
            navigateToDashboard();
        }
    }

    private void navigateToDashboard() {
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();

        startActivity(new Intent(this, Dashboard.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationCancelToken.cancel();
        handler.removeCallbacksAndMessages(null);
    }
}
