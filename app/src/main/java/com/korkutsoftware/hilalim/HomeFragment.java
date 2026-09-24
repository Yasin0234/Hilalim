package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.android.material.snackbar.Snackbar;
import com.korkutsoftware.hilalim.api.PrayerApiService;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.api.RetrofitClient;
import com.korkutsoftware.hilalim.util.AlarmHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView txtLocation, txtHijri, txtNextPrayerName, txtNextPrayerTime;
    private TextView txtImsak, txtGunes, txtOgle, txtIkindi, txtAksam, txtYatsi;
    private View cardImsak, cardGunes, cardOgle, cardIkindi, cardAksam, cardYatsi;
    private ProgressBar prayerProgressBar;
    
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private PrayerResponse.Timings currentTimings;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadCachedData();
        loadFreshData();
        checkAlarmPermission();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFreshData();
    }

    private void checkAlarmPermission() {
        Context context = getContext();
        if (context != null && !AlarmHelper.hasExactAlarmPermission(context)) {
            Snackbar.make(requireView(), R.string.exact_alarm_permission_required, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.allow, v -> AlarmHelper.openAlarmSettings(context))
                    .show();
        }
    }

    private void initViews(View v) {
        txtLocation = v.findViewById(R.id.txt_location_display);
        txtHijri = v.findViewById(R.id.txt_hijri_date);
        txtNextPrayerName = v.findViewById(R.id.txt_next_prayer_name);
        txtNextPrayerTime = v.findViewById(R.id.txt_next_prayer_countdown);
        
        txtImsak = v.findViewById(R.id.txt_imsak_time);
        txtGunes = v.findViewById(R.id.txt_gunes_time);
        txtOgle = v.findViewById(R.id.txt_ogle_time);
        txtIkindi = v.findViewById(R.id.txt_ikindi_time);
        txtAksam = v.findViewById(R.id.txt_aksam_time);
        txtYatsi = v.findViewById(R.id.txt_yatsi_time);

        cardImsak = v.findViewById(R.id.card_imsak);
        cardGunes = v.findViewById(R.id.card_gunes);
        cardOgle = v.findViewById(R.id.card_ogle);
        cardIkindi = v.findViewById(R.id.card_ikindi);
        cardAksam = v.findViewById(R.id.card_aksam);
        cardYatsi = v.findViewById(R.id.card_yatsi);
        
        prayerProgressBar = v.findViewById(R.id.prayer_progress);

        View fabMosque = v.findViewById(R.id.fab_mosque);
        if (fabMosque != null) {
            fabMosque.setOnClickListener(view -> openNearestMosquesOnMap());
        }
    }

    private void openNearestMosquesOnMap() {
        if (getContext() == null) return;
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=cami");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/cami/"));
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/cami/"));
            startActivity(webIntent);
        }
    }

    private void loadCachedData() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        String cachedJson = prefs.getString("cached_prayer_data", null);
        if (cachedJson != null) {
            PrayerResponse.Data data = new Gson().fromJson(cachedJson, PrayerResponse.Data.class);
            updateUI(data, false);
        }
    }

    private void loadFreshData() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        float lat = prefs.getFloat("last_lat", 0);
        float lng = prefs.getFloat("last_lng", 0);
        String manualCity = prefs.getString("last_city", null);
        String manualDistrict = prefs.getString("last_district", null);

        if (manualCity != null && manualDistrict != null) {
            // Manuel konum seçilmişse direkt onu yaz
            txtLocation.setText(String.format("%s, %s", manualCity, manualDistrict));
            if (lat != 0 && lng != 0) {
                fetchPrayerTimings(lat, lng);
            } else {
                fetchPrayerTimingsByCity(manualCity, manualDistrict);
            }
        } else if (lat != 0 && lng != 0) {
            // Otomatik konum seçilmişse Geocoder ile ismi bul
            updateCityName(lat, lng);
            fetchPrayerTimings(lat, lng);
        } else {
            // Henüz konum seçilmemişse varsayılan varsayılan şehir çek ve konum ayarlama uyarısı ver
            txtLocation.setText(R.string.select_location);
            fetchPrayerTimingsByCity("İstanbul", "Fatih");
        }
    }

    private void updateCityName(double lat, double lng) {
        if (getContext() == null) return;
        Context appContext = getContext().getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getAdminArea();
                    String country = addresses.get(0).getCountryName();
                    String locationDisplay = String.format("%s, %s", 
                            city != null ? city : "...", 
                            country != null ? country : "");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded() && txtLocation != null) {
                            txtLocation.setText(locationDisplay);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Geocoder Error", e);
            }
        });
    }

    private void fetchPrayerTimings(double lat, double lng) {
        PrayerApiService apiService = RetrofitClient.getApiService();
        apiService.getTimings(lat, lng, 13).enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<PrayerResponse> call, @NonNull Response<PrayerResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    cacheData(response.body().data);
                    updateUI(response.body().data, true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PrayerResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void fetchPrayerTimingsByCity(String city, String district) {
        PrayerApiService apiService = RetrofitClient.getApiService();
        String address = district + "," + city + ",Turkey";
        apiService.getTimingsByAddress(address, 13).enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<PrayerResponse> call, @NonNull Response<PrayerResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    cacheData(response.body().data);
                    updateUI(response.body().data, true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PrayerResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void cacheData(PrayerResponse.Data data) {
        if (getActivity() == null) return;
        String json = new Gson().toJson(data);
        SharedPreferences prefs = getActivity().getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("cached_prayer_data", json).apply();
    }

    private void updateUI(PrayerResponse.Data data, boolean setAlarms) {
        if (data == null) return;
        currentTimings = data.timings;

        txtImsak.setText(data.timings.fajr);
        txtGunes.setText(data.timings.sunrise);
        txtOgle.setText(data.timings.dhuhr);
        txtIkindi.setText(data.timings.asr);
        txtAksam.setText(data.timings.maghrib);
        txtYatsi.setText(data.timings.isha);

        if (data.date != null && data.date.hijri != null) {
            String hijriStr = String.format("%s %s %s", 
                    data.date.hijri.day, 
                    data.date.hijri.month.en, 
                    data.date.hijri.year);
            txtHijri.setText(hijriStr);
        }

        startTimer();
        if (setAlarms && getContext() != null) {
            AlarmHelper.setAlarms(getContext(), data.timings);
        }
    }

    private void startTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
        
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateCountdown() {
        if (currentTimings == null || getContext() == null) return;

        String[] names = {
                getString(R.string.imsak),
                getString(R.string.gunes),
                getString(R.string.ogle),
                getString(R.string.ikindi),
                getString(R.string.aksam),
                getString(R.string.yatsi)
        };
        String[] times = {currentTimings.fajr, currentTimings.sunrise, currentTimings.dhuhr, 
                         currentTimings.asr, currentTimings.maghrib, currentTimings.isha};

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar now = Calendar.getInstance();
        
        try {
            boolean found = false;
            for (int i = 0; i < times.length; i++) {
                Date pDate = sdf.parse(times[i]);
                if (pDate != null) {
                    Calendar pCal = Calendar.getInstance();
                    pCal.setTime(pDate);
                    pCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

                    if (pCal.after(now)) {
                        long diff = pCal.getTimeInMillis() - now.getTimeInMillis();
                        txtNextPrayerName.setText(names[i]);
                        txtNextPrayerTime.setText(formatTime(diff));
                        highlightPrayer(i);
                        
                        // Progress Bar Logic
                        int prevIndex = (i == 0) ? times.length - 1 : i - 1;
                        Date prevDate = sdf.parse(times[prevIndex]);
                        if (prevDate != null) {
                            Calendar prevCal = Calendar.getInstance();
                            prevCal.setTime(prevDate);
                            prevCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                            
                            if (i == 0) { // If next is Imsak, previous was Yatsi yesterday
                                prevCal.add(Calendar.DAY_OF_MONTH, -1);
                            }

                            long totalInterval = pCal.getTimeInMillis() - prevCal.getTimeInMillis();
                            long elapsed = now.getTimeInMillis() - prevCal.getTimeInMillis();
                            int progress = (int) ((elapsed * 100) / totalInterval);
                            if (prayerProgressBar != null) {
                                prayerProgressBar.setProgress(progress);
                            }
                        }
                        
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                // After Yatsi, next is Imsak tomorrow
                Date imsakDate = sdf.parse(times[0]);
                if (imsakDate != null) {
                    Calendar imsakCal = Calendar.getInstance();
                    imsakCal.setTime(imsakDate);
                    imsakCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    imsakCal.add(Calendar.DAY_OF_MONTH, 1);

                    long diff = imsakCal.getTimeInMillis() - now.getTimeInMillis();
                    txtNextPrayerName.setText(names[0]);
                    txtNextPrayerTime.setText(formatTime(diff));
                    highlightPrayer(0);

                    // Progress Bar for Yatsi -> Imsak
                    Date yatsiDate = sdf.parse(times[5]);
                    if (yatsiDate != null) {
                        Calendar yatsiCal = Calendar.getInstance();
                        yatsiCal.setTime(yatsiDate);
                        yatsiCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                        
                        long totalInterval = imsakCal.getTimeInMillis() - yatsiCal.getTimeInMillis();
                        long elapsed = now.getTimeInMillis() - yatsiCal.getTimeInMillis();
                        int progress = (int) ((elapsed * 100) / totalInterval);
                        if (prayerProgressBar != null) {
                            prayerProgressBar.setProgress(progress);
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void highlightPrayer(int nextIndex) {
        int currentIndex = (nextIndex == 0) ? 5 : nextIndex - 1;
        
        View[] cards = {cardImsak, cardGunes, cardOgle, cardIkindi, cardAksam, cardYatsi};
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;
            if (i == currentIndex) {
                cards[i].setAlpha(1.0f);
                cards[i].setScaleX(1.02f);
                cards[i].setScaleY(1.02f);
            } else {
                cards[i].setAlpha(0.6f);
                cards[i].setScaleX(1.0f);
                cards[i].setScaleY(1.0f);
            }
        }
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }
}