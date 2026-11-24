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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NamazDetailActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_namaz_detail);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String categoryId = getIntent().getStringExtra("category_id");
        String title = getIntent().getStringExtra("category_title");
        String subtitle = getIntent().getStringExtra("category_subtitle");

        TextView txtTitle = findViewById(R.id.txt_detail_title);
        TextView txtSubtitle = findViewById(R.id.txt_detail_subtitle);
        txtTitle.setText(title != null ? title : getString(R.string.namaz_rehberi_title));
        txtSubtitle.setText(subtitle != null ? subtitle : "");

        RecyclerView rv = findViewById(R.id.rv_detail_sections);
        rv.setLayoutManager(new LinearLayoutManager(this));
        DetailAdapter adapter = new DetailAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Section> sections = loadSections(categoryId);
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed() && sections != null) {
                    adapter.setItems(sections);
                }
            });
        });
    }

    private List<Section> loadSections(String categoryId) {
        if (categoryId == null) return new ArrayList<>();
        try (InputStream is = getAssets().open("namaz_rehberi.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            List<Category> categories = new Gson().fromJson(json, new TypeToken<List<Category>>() {}.getType());
            if (categories != null) {
                for (Category c : categories) {
                    if (categoryId.equals(c.id) && c.sections != null) {
                        return c.sections;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static class Category {
        String id;
        List<Section> sections;
    }

    private static class Section {
        String heading;
        String posture;
        String arabic;
        String body;
    }

    private static class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.ViewHolder> {
        private final List<Section> items;

        DetailAdapter(List<Section> items) {
            this.items = items;
        }

        void setItems(List<Section> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_namaz_detail_section, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Section item = items.get(position);
            holder.txtHeading.setText(item.heading);
            holder.txtBody.setText(item.body);

            if (item.arabic != null && !item.arabic.trim().isEmpty()) {
                holder.cardArabic.setVisibility(View.VISIBLE);
                holder.txtArabic.setText(item.arabic);
            } else {
                holder.cardArabic.setVisibility(View.GONE);
            }

            int postureRes = resolvePostureDrawable(item.posture);
            if (postureRes != 0) {
                holder.cardPosture.setVisibility(View.VISIBLE);
                holder.imgPosture.setImageResource(postureRes);
            } else {
                holder.cardPosture.setVisibility(View.GONE);
            }
        }

        private int resolvePostureDrawable(String posture) {
            if (posture == null) return 0;
            switch (posture) {
                case "kiyam":
                    return R.drawable.img_posture_kiyam;
                case "ruku":
                    return R.drawable.img_posture_ruku;
                case "secde":
                    return R.drawable.img_posture_secde;
                case "kade":
                    return R.drawable.img_posture_kade;
                default:
                    return 0;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtHeading, txtArabic, txtBody;
            View cardPosture, cardArabic;
            ImageView imgPosture;

            ViewHolder(View v) {
                super(v);
                txtHeading = v.findViewById(R.id.txt_section_heading);
                txtArabic = v.findViewById(R.id.txt_section_arabic);
                txtBody = v.findViewById(R.id.txt_section_body);
                cardPosture = v.findViewById(R.id.card_posture_img);
                cardArabic = v.findViewById(R.id.card_arabic_box);
                imgPosture = v.findViewById(R.id.img_posture);
            }
        }
    }
}
