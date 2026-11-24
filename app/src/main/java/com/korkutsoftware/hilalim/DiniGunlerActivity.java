package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.util.AppCalculators;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class DiniGunlerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dini_gunler);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_days);
        rv.setLayoutManager(new LinearLayoutManager(this));

        DiniGunAdapter adapter = new DiniGunAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DiniGunItem> items = loadDaysFromJson();
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed() && items != null) {
                    adapter.setItems(items);
                    int nextPos = adapter.getNextUpcomingIndex();
                    if (nextPos > 0) {
                        rv.post(() -> rv.scrollToPosition(nextPos));
                    }
                }
            });
        });
    }

    private List<DiniGunItem> loadDaysFromJson() {
        try (InputStream is = getAssets().open("dini_gunler.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            return new Gson().fromJson(json, new TypeToken<List<DiniGunItem>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static class DiniGunItem {
        String name, date, guide_id;
    }

    private class DiniGunAdapter extends RecyclerView.Adapter<DiniGunAdapter.ViewHolder> {
        private final List<DiniGunItem> items;
        private final Calendar today = Calendar.getInstance();
        private int nextUpcomingIndex = -1;

        DiniGunAdapter(List<DiniGunItem> items) {
            this.items = items;
        }

        void setItems(List<DiniGunItem> newItems) {
            items.clear();
            items.addAll(newItems);
            nextUpcomingIndex = -1;
            int minNonNegative = Integer.MAX_VALUE;
            for (int i = 0; i < items.size(); i++) {
                int delta = AppCalculators.computeDaysUntilTurkishDate(items.get(i).date, today);
                if (delta >= 0 && delta < minNonNegative) {
                    minNonNegative = delta;
                    nextUpcomingIndex = i;
                }
            }
            notifyDataSetChanged();
        }

        int getNextUpcomingIndex() {
            return nextUpcomingIndex;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dini_gun, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DiniGunItem item = items.get(position);
            holder.txtName.setText(item.name);
            holder.txtDate.setText(item.date);

            int daysDelta = AppCalculators.computeDaysUntilTurkishDate(item.date, today);
            if (daysDelta == Integer.MIN_VALUE) {
                holder.txtDaysLeft.setVisibility(View.GONE);
                holder.itemView.setAlpha(1.0f);
            } else if (daysDelta == 0) {
                holder.txtDaysLeft.setVisibility(View.VISIBLE);
                holder.txtDaysLeft.setText(R.string.today_badge);
                holder.itemView.setAlpha(1.0f);
            } else if (daysDelta > 0) {
                holder.txtDaysLeft.setVisibility(View.VISIBLE);
                holder.txtDaysLeft.setText(getString(R.string.days_left_format, daysDelta));
                holder.itemView.setAlpha(1.0f);
            } else {
                holder.txtDaysLeft.setVisibility(View.VISIBLE);
                holder.txtDaysLeft.setText(R.string.days_passed_label);
                holder.itemView.setAlpha(0.6f);
            }

            if (holder.card != null) {
                boolean isNext = position == nextUpcomingIndex;
                float density = getResources().getDisplayMetrics().density;
                holder.card.setStrokeColor(ContextCompat.getColor(
                        DiniGunlerActivity.this,
                        isNext ? R.color.secondary : R.color.card_stroke
                ));
                holder.card.setStrokeWidth((int) ((isNext ? 2f : 1f) * density));
            }

            holder.itemView.setOnClickListener(v -> {
                if (item.guide_id != null && !item.guide_id.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(DiniGunlerActivity.this, DiniGunDetailActivity.class);
                    intent.putExtra("day_name", item.name);
                    intent.putExtra("day_date", item.date);
                    intent.putExtra("guide_id", item.guide_id);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView txtName, txtDate, txtDaysLeft;

            ViewHolder(View v) {
                super(v);
                card = v.findViewById(R.id.card_dini_gun);
                txtName = v.findViewById(R.id.txt_day_name);
                txtDate = v.findViewById(R.id.txt_day_date);
                txtDaysLeft = v.findViewById(R.id.txt_days_left);
            }
        }
    }
}
