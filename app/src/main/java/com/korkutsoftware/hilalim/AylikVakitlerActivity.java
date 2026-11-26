package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.korkutsoftware.hilalim.api.MonthlyPrayerResponse;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.util.AppCalculators;
import com.korkutsoftware.hilalim.util.LocaleHelper;
import com.korkutsoftware.hilalim.util.PrayerCacheHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AylikVakitlerActivity extends BaseActivity {

    private RecyclerView rvMonthly;
    private ProgressBar progressBar;
    private TextView txtError, txtMonthYear;
    private final Calendar displayedMonth = Calendar.getInstance();
    private String todayGregorianStr;

    private boolean isImsakiyeMode = false;
    private View cardImsakiyeLive;
    private TextView txtImsakiyeTargetLabel;
    private Chronometer chronoImsakiyeCountdown;

    private List<PrayerResponse.Data> currentLoadedDays;
    private MonthlyAdapter monthlyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aylik_vakitler);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share_imsakiye).setOnClickListener(v -> shareImsakiye());

        rvMonthly = findViewById(R.id.rv_monthly);
        progressBar = findViewById(R.id.progress_loading);
        txtError = findViewById(R.id.txt_error);
        txtMonthYear = findViewById(R.id.txt_month_year);
        rvMonthly.setLayoutManager(new LinearLayoutManager(this));

        cardImsakiyeLive = findViewById(R.id.card_imsakiye_live_info);
        txtImsakiyeTargetLabel = findViewById(R.id.txt_imsakiye_target_label);
        chronoImsakiyeCountdown = findViewById(R.id.chrono_imsakiye_countdown);

        todayGregorianStr = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Calendar.getInstance().getTime());

        MaterialButtonToggleGroup toggleMode = findViewById(R.id.toggle_view_mode);
        toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            isImsakiyeMode = (checkedId == R.id.btn_mode_imsakiye);
            cardImsakiyeLive.setVisibility(isImsakiyeMode ? View.VISIBLE : View.GONE);
            if (monthlyAdapter != null) {
                monthlyAdapter.setImsakiyeMode(isImsakiyeMode);
            }
            if (isImsakiyeMode) {
                updateImsakiyeLiveCountdown();
            }
        });

        findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            loadMonthlyTimings();
        });
        findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            loadMonthlyTimings();
        });

        loadMonthlyTimings();
    }

    private void updateMonthHeader() {
        String lang = LocaleHelper.getLanguage(this);
        SimpleDateFormat headerFormat = new SimpleDateFormat("MMMM yyyy", new Locale(lang));
        String title = headerFormat.format(displayedMonth.getTime());
        if (!title.isEmpty()) {
            title = title.substring(0, 1).toUpperCase(new Locale(lang)) + title.substring(1);
        }
        txtMonthYear.setText(title);
    }

    private void loadMonthlyTimings() {
        updateMonthHeader();
        int month = displayedMonth.get(Calendar.MONTH) + 1;
        int year = displayedMonth.get(Calendar.YEAR);
        PrayerCacheHelper.LocationSpec spec = PrayerCacheHelper.getCurrentLocationSpec(this);

        List<PrayerResponse.Data> cached = PrayerCacheHelper.loadMonthlyCalendar(this, spec.locationKey, month, year);
        if (cached != null && !cached.isEmpty()) {
            showList(cached);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvMonthly.setVisibility(View.GONE);
        txtError.setVisibility(View.GONE);

        PrayerCacheHelper.createMonthlyCall(spec, month, year).enqueue(new Callback<MonthlyPrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<MonthlyPrayerResponse> call, @NonNull Response<MonthlyPrayerResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<PrayerResponse.Data> days = response.body().data;
                    PrayerCacheHelper.saveMonthlyCalendar(AylikVakitlerActivity.this, days, spec.locationKey, month, year);
                    showList(days);
                } else {
                    txtError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MonthlyPrayerResponse> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                txtError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showList(List<PrayerResponse.Data> days) {
        currentLoadedDays = days;
        progressBar.setVisibility(View.GONE);
        txtError.setVisibility(View.GONE);
        rvMonthly.setVisibility(View.VISIBLE);
        monthlyAdapter = new MonthlyAdapter(days, isImsakiyeMode);
        rvMonthly.setAdapter(monthlyAdapter);

        if (isImsakiyeMode) {
            updateImsakiyeLiveCountdown();
        }

        int todayPos = -1;
        for (int i = 0; i < days.size(); i++) {
            PrayerResponse.Data d = days.get(i);
            if (d != null && d.date != null && d.date.gregorian != null
                    && todayGregorianStr.equals(d.date.gregorian.date)) {
                todayPos = i;
                break;
            }
        }
        if (todayPos > 0) {
            int scrollTarget = todayPos;
            rvMonthly.post(() -> rvMonthly.scrollToPosition(scrollTarget));
        }
    }

    private void updateImsakiyeLiveCountdown() {
        if (currentLoadedDays == null || currentLoadedDays.isEmpty()) return;

        PrayerResponse.Data todayData = null;
        for (PrayerResponse.Data d : currentLoadedDays) {
            if (d != null && d.date != null && d.date.gregorian != null
                    && todayGregorianStr.equals(d.date.gregorian.date)) {
                todayData = d;
                break;
            }
        }

        if (todayData == null || todayData.timings == null) {
            cardImsakiyeLive.setVisibility(View.GONE);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        Calendar now = Calendar.getInstance();

        try {
            String imsakClean = AppCalculators.cleanPrayerTime(todayData.timings.fajr);
            String aksamClean = AppCalculators.cleanPrayerTime(todayData.timings.maghrib);

            Date imsakDate = sdf.parse(imsakClean);
            Date aksamDate = sdf.parse(aksamClean);

            if (imsakDate != null && aksamDate != null) {
                Calendar imsakCal = Calendar.getInstance();
                imsakCal.setTime(imsakDate);
                imsakCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

                Calendar aksamCal = Calendar.getInstance();
                aksamCal.setTime(aksamDate);
                aksamCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

                long targetMs;
                if (now.before(imsakCal)) {
                    // Before Sahur
                    txtImsakiyeTargetLabel.setText(R.string.sahur_countdown_label);
                    targetMs = imsakCal.getTimeInMillis();
                } else if (now.before(aksamCal)) {
                    // Between Sahur and Iftar
                    txtImsakiyeTargetLabel.setText(R.string.iftar_countdown_label);
                    targetMs = aksamCal.getTimeInMillis();
                } else {
                    // After Iftar, next day Sahur
                    txtImsakiyeTargetLabel.setText(R.string.sahur_countdown_label);
                    imsakCal.add(Calendar.DAY_OF_MONTH, 1);
                    targetMs = imsakCal.getTimeInMillis();
                }

                long remainingMs = Math.max(0L, targetMs - System.currentTimeMillis());
                long base = SystemClock.elapsedRealtime() + remainingMs;
                chronoImsakiyeCountdown.stop();
                chronoImsakiyeCountdown.setBase(base);
                chronoImsakiyeCountdown.setCountDown(true);
                chronoImsakiyeCountdown.start();
                cardImsakiyeLive.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            cardImsakiyeLive.setVisibility(View.GONE);
        }
    }

    private void shareImsakiye() {
        if (currentLoadedDays == null || currentLoadedDays.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("🌙 Hilalim — ").append(txtMonthYear.getText()).append(" İmsakiyesi\n\n");

        for (PrayerResponse.Data d : currentLoadedDays) {
            if (d != null && d.date != null && d.timings != null) {
                String dayStr = d.date.readable != null ? d.date.readable : "";
                String imsak = AppCalculators.cleanPrayerTime(d.timings.fajr);
                String iftar = AppCalculators.cleanPrayerTime(d.timings.maghrib);
                sb.append("• ").append(dayStr).append(" | Sahur: ").append(imsak)
                        .append(" - İftar: ").append(iftar).append("\n");
            }
        }

        sb.append("\nHilalim Mobil Uygulaması ile paylaşıldı.");

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_imsakiye)));
    }

    private class MonthlyAdapter extends RecyclerView.Adapter<MonthlyAdapter.ViewHolder> {
        private final List<PrayerResponse.Data> items;
        private boolean imsakiyeMode;
        private final String lang = LocaleHelper.getLanguage(AylikVakitlerActivity.this);
        private final int activeStroke = ContextCompat.getColor(AylikVakitlerActivity.this, R.color.secondary);
        private final int defaultStroke = ContextCompat.getColor(AylikVakitlerActivity.this, R.color.card_stroke);
        private final int activeBg = ContextCompat.getColor(AylikVakitlerActivity.this, R.color.selected_lang_bg);
        private final int defaultBg;

        MonthlyAdapter(List<PrayerResponse.Data> items, boolean imsakiyeMode) {
            this.items = items;
            this.imsakiyeMode = imsakiyeMode;
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, tv, true);
            defaultBg = tv.resourceId != 0 ? ContextCompat.getColor(AylikVakitlerActivity.this, tv.resourceId)
                    : ContextCompat.getColor(AylikVakitlerActivity.this, R.color.card_bg);
        }

        void setImsakiyeMode(boolean mode) {
            this.imsakiyeMode = mode;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_aylik_vakit, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PrayerResponse.Data day = items.get(position);
            String dateLabel = day.date != null ? day.date.readable : "";
            if (day.date != null && day.date.hijri != null && day.date.hijri.month != null) {
                String hijriMonth = AppCalculators.localizeHijriMonth(
                        day.date.hijri.month.number, day.date.hijri.month.en, lang);
                dateLabel += " (" + day.date.hijri.day + " " + hijriMonth + ")";
            }
            holder.txtDate.setText(dateLabel);

            boolean isToday = day.date != null && day.date.gregorian != null
                    && todayGregorianStr.equals(day.date.gregorian.date);
            holder.txtTodayBadge.setVisibility(isToday ? View.VISIBLE : View.GONE);
            if (holder.cardDay != null) {
                float density = getResources().getDisplayMetrics().density;
                holder.cardDay.setStrokeColor(isToday ? activeStroke : defaultStroke);
                holder.cardDay.setStrokeWidth((int) ((isToday ? 2f : 1f) * density));
                holder.cardDay.setCardBackgroundColor(isToday ? activeBg : defaultBg);
            }

            if (day.timings != null) {
                if (imsakiyeMode) {
                    holder.layoutNormal.setVisibility(View.GONE);
                    holder.layoutImsakiye.setVisibility(View.VISIBLE);
                    holder.txtImsakiyeSahurTime.setText(AppCalculators.cleanPrayerTime(day.timings.fajr));
                    holder.txtImsakiyeIftarTime.setText(AppCalculators.cleanPrayerTime(day.timings.maghrib));
                } else {
                    holder.layoutNormal.setVisibility(View.VISIBLE);
                    holder.layoutImsakiye.setVisibility(View.GONE);
                    holder.txtImsak.setText(getString(R.string.imsak) + "\n" + AppCalculators.cleanPrayerTime(day.timings.fajr));
                    holder.txtGunes.setText(getString(R.string.gunes) + "\n" + AppCalculators.cleanPrayerTime(day.timings.sunrise));
                    holder.txtOgle.setText(getString(R.string.ogle) + "\n" + AppCalculators.cleanPrayerTime(day.timings.dhuhr));
                    holder.txtIkindi.setText(getString(R.string.ikindi) + "\n" + AppCalculators.cleanPrayerTime(day.timings.asr));
                    holder.txtAksam.setText(getString(R.string.aksam) + "\n" + AppCalculators.cleanPrayerTime(day.timings.maghrib));
                    holder.txtYatsi.setText(getString(R.string.yatsi) + "\n" + AppCalculators.cleanPrayerTime(day.timings.isha));
                }
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardDay;
            TextView txtDate, txtTodayBadge;
            View layoutNormal, layoutImsakiye;
            TextView txtImsak, txtGunes, txtOgle, txtIkindi, txtAksam, txtYatsi;
            TextView txtImsakiyeSahurTime, txtImsakiyeIftarTime;

            ViewHolder(View v) {
                super(v);
                cardDay = v.findViewById(R.id.card_monthly_day);
                txtDate = v.findViewById(R.id.txt_day_date);
                txtTodayBadge = v.findViewById(R.id.txt_today_badge);

                layoutNormal = v.findViewById(R.id.layout_normal_timings);
                layoutImsakiye = v.findViewById(R.id.layout_imsakiye_timings);

                txtImsak = v.findViewById(R.id.txt_imsak);
                txtGunes = v.findViewById(R.id.txt_gunes);
                txtOgle = v.findViewById(R.id.txt_ogle);
                txtIkindi = v.findViewById(R.id.txt_ikindi);
                txtAksam = v.findViewById(R.id.txt_aksam);
                txtYatsi = v.findViewById(R.id.txt_yatsi);

                txtImsakiyeSahurTime = v.findViewById(R.id.txt_imsakiye_sahur_time);
                txtImsakiyeIftarTime = v.findViewById(R.id.txt_imsakiye_iftar_time);
            }
        }
    }
}
