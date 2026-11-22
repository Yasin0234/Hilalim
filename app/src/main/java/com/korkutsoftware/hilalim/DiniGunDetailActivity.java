package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class DiniGunDetailActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dini_gun_detail);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String dayName = getIntent().getStringExtra("day_name");
        String dayDate = getIntent().getStringExtra("day_date");
        String guideId = getIntent().getStringExtra("guide_id");

        TextView txtTitle = findViewById(R.id.txt_detail_title);
        TextView txtDate = findViewById(R.id.txt_detail_date);
        txtTitle.setText(dayName != null ? dayName : getString(R.string.tool_dini_gunler));
        txtDate.setText(dayDate != null ? dayDate : "");

        RecyclerView rv = findViewById(R.id.rv_sections);
        rv.setLayoutManager(new LinearLayoutManager(this));
        SectionAdapter adapter = new SectionAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SectionItem> sections = loadSections(guideId);
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed() && sections != null) {
                    adapter.setItems(sections);
                }
            });
        });
    }

    private List<SectionItem> loadSections(String guideId) {
        if (guideId == null || guideId.isEmpty()) return new ArrayList<>();
        try (InputStream is = getAssets().open("dini_gunler_detay.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, GuideDetail>>() {}.getType();
            Map<String, GuideDetail> map = new Gson().fromJson(json, mapType);
            if (map != null && map.containsKey(guideId)) {
                GuideDetail detail = map.get(guideId);
                if (detail != null && detail.sections != null) {
                    return detail.sections;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static class GuideDetail {
        String title;
        List<SectionItem> sections;
    }

    private static class SectionItem {
        String heading;
        String icon;
        String body;
    }

    private static class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.ViewHolder> {
        private final List<SectionItem> items;

        SectionAdapter(List<SectionItem> items) {
            this.items = items;
        }

        void setItems(List<SectionItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dini_gun_section, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SectionItem item = items.get(position);
            holder.txtHeading.setText(item.heading);
            holder.txtBody.setText(item.body);
            holder.imgIcon.setImageResource(resolveIcon(item.icon));
        }

        private int resolveIcon(String iconKey) {
            if (iconKey == null) return R.drawable.ic_info_rounded;
            switch (iconKey) {
                case "star":
                    return R.drawable.ic_diamond;
                case "check":
                    return R.drawable.ic_calendar_event;
                case "dua":
                    return R.drawable.ic_pray;
                case "quran":
                    return R.drawable.ic_book;
                case "heart":
                    return R.drawable.ic_heart;
                case "summary":
                    return R.drawable.ic_mosque;
                default:
                    return R.drawable.ic_info_rounded;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgIcon;
            TextView txtHeading, txtBody;

            ViewHolder(View v) {
                super(v);
                imgIcon = v.findViewById(R.id.img_section_icon);
                txtHeading = v.findViewById(R.id.txt_section_heading);
                txtBody = v.findViewById(R.id.txt_section_body);
            }
        }
    }
}
