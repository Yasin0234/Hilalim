package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.korkutsoftware.hilalim.api.MonthlyPrayerResponse;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.util.AppCalculators;
import com.korkutsoftware.hilalim.util.LocaleHelper;
import com.korkutsoftware.hilalim.util.PrayerCacheHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aylik_vakitler);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvMonthly = findViewById(R.id.rv_monthly);
        progressBar = findViewById(R.id.progress_loading);
        txtError = findViewById(R.id.txt_error);
        txtMonthYear = findViewById(R.id.txt_month_year);
        rvMonthly.setLayoutManager(new LinearLayoutManager(this));

        todayGregorianStr = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Calendar.getInstance().getTime());

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
        progressBar.setVisibility(View.GONE);
        txtError.setVisibility(View.GONE);
        rvMonthly.setVisibility(View.VISIBLE);
        rvMonthly.setAdapter(new MonthlyAdapter(days));

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

    private class MonthlyAdapter extends RecyclerView.Adapter<MonthlyAdapter.ViewHolder> {
        private final List<PrayerResponse.Data> items;
        private final String lang = LocaleHelper.getLanguage(AylikVakitlerActivity.this);
        private final int activeStroke = ContextCompat.getColor(AylikVakitlerActivity.this, R.color.secondary);
        private final int defaultStroke = ContextCompat.getColor(AylikVakitlerActivity.this, R.color.card_stroke);
        private final int activeBg = ContextCompat.getColor(AylikVakitlerActivity.this, R.color.selected_lang_bg);
        private final int defaultBg;

        MonthlyAdapter(List<PrayerResponse.Data> items) {
            this.items = items;
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, tv, true);
            defaultBg = tv.resourceId != 0 ? ContextCompat.getColor(AylikVakitlerActivity.this, tv.resourceId)
                    : ContextCompat.getColor(AylikVakitlerActivity.this, R.color.card_bg);
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
                holder.txtImsak.setText(getString(R.string.imsak) + "\n" + AppCalculators.cleanPrayerTime(day.timings.fajr));
                holder.txtGunes.setText(getString(R.string.gunes) + "\n" + AppCalculators.cleanPrayerTime(day.timings.sunrise));
                holder.txtOgle.setText(getString(R.string.ogle) + "\n" + AppCalculators.cleanPrayerTime(day.timings.dhuhr));
                holder.txtIkindi.setText(getString(R.string.ikindi) + "\n" + AppCalculators.cleanPrayerTime(day.timings.asr));
                holder.txtAksam.setText(getString(R.string.aksam) + "\n" + AppCalculators.cleanPrayerTime(day.timings.maghrib));
                holder.txtYatsi.setText(getString(R.string.yatsi) + "\n" + AppCalculators.cleanPrayerTime(day.timings.isha));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardDay;
            TextView txtDate, txtTodayBadge, txtImsak, txtGunes, txtOgle, txtIkindi, txtAksam, txtYatsi;

            ViewHolder(View v) {
                super(v);
                cardDay = v.findViewById(R.id.card_monthly_day);
                txtDate = v.findViewById(R.id.txt_day_date);
                txtTodayBadge = v.findViewById(R.id.txt_today_badge);
                txtImsak = v.findViewById(R.id.txt_imsak);
                txtGunes = v.findViewById(R.id.txt_gunes);
                txtOgle = v.findViewById(R.id.txt_ogle);
                txtIkindi = v.findViewById(R.id.txt_ikindi);
                txtAksam = v.findViewById(R.id.txt_aksam);
                txtYatsi = v.findViewById(R.id.txt_yatsi);
            }
        }
    }
}
