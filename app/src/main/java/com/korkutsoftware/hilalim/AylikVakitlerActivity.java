package com.korkutsoftware.hilalim;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.korkutsoftware.hilalim.api.MonthlyPrayerResponse;
import com.korkutsoftware.hilalim.api.PrayerApiService;
import com.korkutsoftware.hilalim.api.PrayerResponse;
import com.korkutsoftware.hilalim.api.RetrofitClient;
import com.korkutsoftware.hilalim.util.AppCalculators;

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AylikVakitlerActivity extends BaseActivity {

    private RecyclerView rvMonthly;
    private ProgressBar progressBar;
    private TextView txtError;

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
        rvMonthly.setLayoutManager(new LinearLayoutManager(this));

        loadMonthlyTimings();
    }

    private void loadMonthlyTimings() {
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        float lat = prefs.getFloat("last_lat", 0);
        float lng = prefs.getFloat("last_lng", 0);
        String manualCity = prefs.getString("last_city", null);
        String manualDistrict = prefs.getString("last_district", null);

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        PrayerApiService apiService = RetrofitClient.getApiService();
        Call<MonthlyPrayerResponse> call;
        if (lat != 0 && lng != 0) {
            call = apiService.getMonthlyCalendar(lat, lng, 13, month, year);
        } else if (manualCity != null && manualDistrict != null) {
            call = apiService.getMonthlyCalendarByAddress(manualDistrict + "," + manualCity + ",Turkey", 13, month, year);
        } else {
            call = apiService.getMonthlyCalendarByAddress("Fatih,İstanbul,Turkey", 13, month, year);
        }

        call.enqueue(new Callback<MonthlyPrayerResponse>() {
            @Override
            public void onResponse(@NonNull Call<MonthlyPrayerResponse> call, @NonNull Response<MonthlyPrayerResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    rvMonthly.setVisibility(View.VISIBLE);
                    rvMonthly.setAdapter(new MonthlyAdapter(response.body().data));
                } else {
                    txtError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MonthlyPrayerResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                txtError.setVisibility(View.VISIBLE);
            }
        });
    }

    private class MonthlyAdapter extends RecyclerView.Adapter<MonthlyAdapter.ViewHolder> {
        private final List<PrayerResponse.Data> items;

        MonthlyAdapter(List<PrayerResponse.Data> items) {
            this.items = items;
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
                dateLabel += " (" + day.date.hijri.day + " " + day.date.hijri.month.en + ")";
            }
            holder.txtDate.setText(dateLabel);

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
            TextView txtDate, txtImsak, txtGunes, txtOgle, txtIkindi, txtAksam, txtYatsi;

            ViewHolder(View v) {
                super(v);
                txtDate = v.findViewById(R.id.txt_day_date);
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
