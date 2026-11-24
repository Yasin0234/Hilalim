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
import com.korkutsoftware.hilalim.api.MonthlyPrayerResponse;
import com.korkutsoftware.hilalim.api.PrayerApiService;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.api.RetrofitClient;
import com.korkutsoftware.hilalim.util.AlarmHelper;
import com.korkutsoftware.hilalim.util.AppCalculators;
import com.korkutsoftware.hilalim.util.LocaleHelper;
import com.korkutsoftware.hilalim.util.OngoingPrayerNotificationHelper;
import com.korkutsoftware.hilalim.util.PrayerCacheHelper;
import com.korkutsoftware.hilalim.util.ShareCardHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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

        View dateCard = v.findViewById(R.id.date_card);
        if (dateCard != null) {
            dateCard.setOnClickListener(view -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), LocationActivity.class));
                }
            });
        }

        View btnMonthlyTimes = v.findViewById(R.id.btn_home_monthly_times);
        if (btnMonthlyTimes != null) {
            btnMonthlyTimes.setOnClickListener(view -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), AylikVakitlerActivity.class));
                }
            });
        }

        View btnCopyVerse = v.findViewById(R.id.btn_copy_home_verse);
        if (btnCopyVerse != null) {
            btnCopyVerse.setOnClickListener(view -> copyDailyVerse(v));
        }

        View btnShareVerse = v.findViewById(R.id.btn_share_home_verse);
        if (btnShareVerse != null) {
            btnShareVerse.setOnClickListener(view -> shareDailyVerse(v));
        }

        View fabMosque = v.findViewById(R.id.fab_mosque);
        if (fabMosque != null) {
            fabMosque.setOnClickListener(view -> openNearestMosquesOnMap());
        }

        updateDailyVerse(v);
        setupRandomDuaCard(v);
    }

    private final List<DualarActivity.DuaItem> homeDuaPool = new java.util.ArrayList<>();
    private final Random duaRandom = new Random();
    private DualarActivity.DuaItem currentHomeDua = null;

    private void setupRandomDuaCard(View root) {
        View cardDua = root.findViewById(R.id.card_daily_dua);
        View btnRefresh = root.findViewById(R.id.btn_refresh_dua);
        View btnShareDua = root.findViewById(R.id.btn_share_home_dua);
        if (cardDua != null) {
            cardDua.setOnClickListener(view -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), DualarActivity.class));
                }
            });
        }
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(view -> showRandomDua(root));
        }
        if (btnShareDua != null) {
            btnShareDua.setOnClickListener(view -> shareDailyDua(root));
        }

        Context ctx = getContext();
        if (ctx == null) return;
        Context appContext = ctx.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DualarActivity.DuaItem> loaded = DualarActivity.readDuasFromAssets(appContext);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || loaded == null || loaded.isEmpty()) return;
                homeDuaPool.clear();
                homeDuaPool.addAll(loaded);
                showRandomDua(root);
            });
        });
    }

    private void showRandomDua(View root) {
        if (homeDuaPool.isEmpty()) return;
        DualarActivity.DuaItem dua = homeDuaPool.get(duaRandom.nextInt(homeDuaPool.size()));
        currentHomeDua = dua;
        TextView txtTitle = root.findViewById(R.id.txt_home_dua_title);
        TextView txtArabic = root.findViewById(R.id.txt_home_dua_arabic);
        TextView txtOkunus = root.findViewById(R.id.txt_home_dua_okunus);
        TextView txtAnlam = root.findViewById(R.id.txt_home_dua_anlam);
        TextView txtKaynak = root.findViewById(R.id.txt_home_dua_kaynak);
        if (txtTitle != null) txtTitle.setText(dua.title);
        if (txtArabic != null) txtArabic.setText(dua.arabic);
        if (txtOkunus != null) txtOkunus.setText(dua.okunus);
        if (txtAnlam != null) txtAnlam.setText(dua.anlam);
        if (txtKaynak != null) txtKaynak.setText(dua.kaynak);
    }

    private void shareDailyDua(View root) {
        if (getContext() == null) return;
        if (currentHomeDua != null) {
            ShareCardHelper.showShareOptionsDialog(
                    getContext(),
                    currentHomeDua.title,
                    currentHomeDua.arabic,
                    currentHomeDua.anlam,
                    currentHomeDua.kaynak
            );
            return;
        }
        TextView txtArabic = root.findViewById(R.id.txt_home_dua_arabic);
        TextView txtAnlam = root.findViewById(R.id.txt_home_dua_anlam);
        ShareCardHelper.showShareOptionsDialog(
                getContext(),
                getString(R.string.gunun_duasi_title),
                txtArabic != null ? txtArabic.getText().toString() : "",
                txtAnlam != null ? txtAnlam.getText().toString() : "",
                ""
        );
    }

    private void updateDailyVerse(View root) {
        TextView txtContent = root.findViewById(R.id.txt_verse_content);
        TextView txtRef = root.findViewById(R.id.txt_verse_ref);
        if (txtContent == null || txtRef == null) return;

        String[] texts = getResources().getStringArray(R.array.daily_verses_text);
        String[] refs = getResources().getStringArray(R.array.daily_verses_ref);
        if (texts.length == 0 || texts.length != refs.length) return;
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int idx = dayOfYear % texts.length;
        txtContent.setText(texts[idx]);
        txtRef.setText(refs[idx]);
    }

    private void shareDailyVerse(View root) {
        if (getContext() == null) return;
        TextView txtContent = root.findViewById(R.id.txt_verse_content);
        TextView txtRef = root.findViewById(R.id.txt_verse_ref);
        if (txtContent == null || txtRef == null) return;
        ShareCardHelper.showShareOptionsDialog(
                getContext(),
                getString(R.string.gunun_ayeti_title),
                null,
                txtContent.getText().toString(),
                txtRef.getText().toString()
        );
    }

    private void copyDailyVerse(View root) {
        if (getContext() == null) return;
        TextView txtContent = root.findViewById(R.id.txt_verse_content);
        TextView txtRef = root.findViewById(R.id.txt_verse_ref);
        if (txtContent == null || txtRef == null) return;
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        String text = txtContent.getText() + "\n" + txtRef.getText();
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.gunun_ayeti_title), text));
        android.widget.Toast.makeText(getContext(), R.string.quran_verse_copied, android.widget.Toast.LENGTH_SHORT).show();
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
        PrayerResponse.Data data = PrayerCacheHelper.getTodayData(getActivity());
        if (data != null) {
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

        String locationKey;
        if (manualCity != null && manualDistrict != null) {
            txtLocation.setText(String.format("%s, %s", manualCity, manualDistrict));
            locationKey = "manual:" + manualCity + "," + manualDistrict;
        } else if (lat != 0 && lng != 0) {
            updateCityName(lat, lng);
            locationKey = "coords:" + lat + "," + lng;
        } else {
            txtLocation.setText(R.string.select_location);
            locationKey = "default:Istanbul,Fatih";
        }

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastFetchedDate = prefs.getString("last_fetched_date", "");
        String lastFetchedLoc = prefs.getString("last_fetched_location", "");
        prefetchMonthlyCacheIfNeeded(locationKey);
        if (currentTimings != null && todayStr.equals(lastFetchedDate) && locationKey.equals(lastFetchedLoc)) {
            return;
        }

        if (manualCity != null && manualDistrict != null) {
            if (lat != 0 && lng != 0) {
                fetchPrayerTimings(lat, lng, locationKey);
            } else {
                fetchPrayerTimingsByCity(manualCity, manualDistrict, locationKey);
            }
        } else if (lat != 0 && lng != 0) {
            fetchPrayerTimings(lat, lng, locationKey);
        } else {
            fetchPrayerTimingsByCity("İstanbul", "Fatih", locationKey);
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

    private void fetchPrayerTimings(double lat, double lng, String locationKey) {
        PrayerApiService apiService = RetrofitClient.getApiService();
        apiService.getTimings(lat, lng, 13).enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<PrayerResponse> call, @NonNull Response<PrayerResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    cacheData(response.body().data, locationKey);
                    updateUI(response.body().data, true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PrayerResponse> call, @NonNull Throwable t) {
                showNetworkErrorIfEmpty();
            }
        });
    }

    private void fetchPrayerTimingsByCity(String city, String district, String locationKey) {
        PrayerApiService apiService = RetrofitClient.getApiService();
        String address = district + "," + city + ",Turkey";
        apiService.getTimingsByAddress(address, 13).enqueue(new Callback<PrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<PrayerResponse> call, @NonNull Response<PrayerResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    PrayerResponse.Data data = response.body().data;
                    saveMetaCoordinatesIfNeeded(data);
                    cacheData(data, locationKey);
                    updateUI(data, true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PrayerResponse> call, @NonNull Throwable t) {
                showNetworkErrorIfEmpty();
            }
        });
    }

    private void prefetchMonthlyCacheIfNeeded(String locationKey) {
        Context ctx = getContext();
        if (ctx == null) return;
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        if (PrayerCacheHelper.hasMonthlyCacheFor(ctx, locationKey, month, year)) {
            return;
        }
        Context appContext = ctx.getApplicationContext();
        PrayerCacheHelper.LocationSpec spec = PrayerCacheHelper.getCurrentLocationSpec(appContext);
        PrayerCacheHelper.createMonthlyCall(spec, month, year).enqueue(new Callback<MonthlyPrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<MonthlyPrayerResponse> call, @NonNull Response<MonthlyPrayerResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    PrayerCacheHelper.saveMonthlyCalendar(appContext, response.body().data, locationKey, month, year);
                    if (isAdded() && currentTimings != null) {
                        AlarmHelper.setAlarms(appContext, currentTimings);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MonthlyPrayerResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void showNetworkErrorIfEmpty() {
        if (isAdded() && getView() != null && currentTimings == null) {
            Snackbar.make(getView(), R.string.error_prayer_times_load, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry, v -> loadFreshData())
                    .show();
        }
    }

    private void saveMetaCoordinatesIfNeeded(PrayerResponse.Data data) {
        if (getActivity() == null || data == null || data.meta == null) return;
        if (data.meta.latitude == 0 && data.meta.longitude == 0) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        if (prefs.getString("last_city", null) != null) {
            prefs.edit()
                    .putFloat("last_lat", (float) data.meta.latitude)
                    .putFloat("last_lng", (float) data.meta.longitude)
                    .apply();
        }
    }

    private void cacheData(PrayerResponse.Data data, String locationKey) {
        if (getActivity() == null) return;
        String json = new Gson().toJson(data);
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        SharedPreferences prefs = getActivity().getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("cached_prayer_data", json)
                .putString("last_fetched_date", todayStr)
                .putString("last_fetched_location", locationKey)
                .apply();
    }

    private void updateUI(PrayerResponse.Data data, boolean setAlarms) {
        if (data == null || data.timings == null) return;
        currentTimings = data.timings;

        txtImsak.setText(cleanTime(data.timings.fajr));
        txtGunes.setText(cleanTime(data.timings.sunrise));
        txtOgle.setText(cleanTime(data.timings.dhuhr));
        txtIkindi.setText(cleanTime(data.timings.asr));
        txtAksam.setText(cleanTime(data.timings.maghrib));
        txtYatsi.setText(cleanTime(data.timings.isha));

        if (data.date != null && data.date.hijri != null && data.date.hijri.month != null) {
            String lang = getContext() != null ? LocaleHelper.getLanguage(getContext()) : "tr";
            String localizedMonth = AppCalculators.localizeHijriMonth(
                    data.date.hijri.month.number, data.date.hijri.month.en, lang);
            String hijriStr = String.format("%s %s %s",
                    data.date.hijri.day,
                    localizedMonth,
                    data.date.hijri.year);
            txtHijri.setText(hijriStr);
        }

        startTimer();
        if (getContext() != null) {
            PrayerWidgetProvider.updateAllWidgets(getContext());
            OngoingPrayerNotificationHelper.updateOrCancel(getContext());
            if (setAlarms) {
                AlarmHelper.setAlarms(getContext(), data.timings);
            }
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
        String[] times = {
                cleanTime(currentTimings.fajr),
                cleanTime(currentTimings.sunrise),
                cleanTime(currentTimings.dhuhr),
                cleanTime(currentTimings.asr),
                cleanTime(currentTimings.maghrib),
                cleanTime(currentTimings.isha)
        };

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        Calendar now = Calendar.getInstance();
        
        try {
            boolean found = false;
            for (int i = 0; i < times.length; i++) {
                if (times[i].isEmpty()) continue;
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
                        Date prevDate = times[prevIndex].isEmpty() ? null : sdf.parse(times[prevIndex]);
                        if (prevDate != null) {
                            Calendar prevCal = Calendar.getInstance();
                            prevCal.setTime(prevDate);
                            prevCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                            
                            if (i == 0) { // If next is Imsak, previous was Yatsi yesterday
                                prevCal.add(Calendar.DAY_OF_MONTH, -1);
                            }

                            long totalInterval = pCal.getTimeInMillis() - prevCal.getTimeInMillis();
                            long elapsed = now.getTimeInMillis() - prevCal.getTimeInMillis();
                            if (totalInterval > 0 && prayerProgressBar != null) {
                                prayerProgressBar.setProgress(AppCalculators.computeProgressPercent(elapsed, totalInterval));
                            }
                        }
                        
                        found = true;
                        break;
                    }
                }
            }

            if (!found && !times[0].isEmpty()) {
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
                    Date yatsiDate = times[5].isEmpty() ? null : sdf.parse(times[5]);
                    if (yatsiDate != null) {
                        Calendar yatsiCal = Calendar.getInstance();
                        yatsiCal.setTime(yatsiDate);
                        yatsiCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                        
                        long totalInterval = imsakCal.getTimeInMillis() - yatsiCal.getTimeInMillis();
                        long elapsed = now.getTimeInMillis() - yatsiCal.getTimeInMillis();
                        if (totalInterval > 0 && prayerProgressBar != null) {
                            prayerProgressBar.setProgress(AppCalculators.computeProgressPercent(elapsed, totalInterval));
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String cleanTime(String timeStr) {
        return AppCalculators.cleanPrayerTime(timeStr);
    }

    private void highlightPrayer(int nextIndex) {
        int currentIndex = (nextIndex == 0) ? 5 : nextIndex - 1;
        
        View[] cards = {cardImsak, cardGunes, cardOgle, cardIkindi, cardAksam, cardYatsi};
        int activeStrokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary);
        int defaultStrokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.card_stroke);
        float density = getResources().getDisplayMetrics().density;
        int activeStrokePx = (int) (2 * density);
        int defaultStrokePx = Math.max(1, (int) density);
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;
            if (i == currentIndex) {
                cards[i].setAlpha(1.0f);
                cards[i].setScaleX(1.015f);
                cards[i].setScaleY(1.015f);
                if (cards[i] instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) cards[i];
                    card.setStrokeColor(activeStrokeColor);
                    card.setStrokeWidth(activeStrokePx);
                }
            } else {
                cards[i].setAlpha(0.88f);
                cards[i].setScaleX(1.0f);
                cards[i].setScaleY(1.0f);
                if (cards[i] instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) cards[i];
                    card.setStrokeColor(defaultStrokeColor);
                    card.setStrokeWidth(defaultStrokePx);
                }
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