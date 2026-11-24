package com.korkutsoftware.hilalim;

import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
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

public class NamazRehberiActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_namaz_rehberi);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        setupPostureCards();

        RecyclerView rv = findViewById(R.id.rv_guide);
        rv.setLayoutManager(new LinearLayoutManager(this));
        CategoryAdapter adapter = new CategoryAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<GuideCategory> categories = loadCategories();
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed() && categories != null) {
                    adapter.setItems(categories);
                }
            });
        });
    }

    private void setupPostureCards() {
        findViewById(R.id.card_posture_kiyam).setOnClickListener(v ->
                showPostureDialog(
                        getString(R.string.posture_kiyam),
                        "Kıbleye karşı ayaklar 4 parmak açık şekilde dik durulur. İftitah tekbirinde erkekler başparmakları kulak memesine, kadınlar omuz hizasına kaldırır; ardından erkekler göbek altında, kadınlar göğüs üstünde bağlar.",
                        R.drawable.img_posture_kiyam
                ));

        findViewById(R.id.card_posture_ruku).setOnClickListener(v ->
                showPostureDialog(
                        getString(R.string.posture_ruku),
                        "\"Allahü ekber\" diyerek eğilinir. Eller dizlere konur, sırt ve baş aynı hizada tutulur. En az bir tesbih süresi durulup 3 kez \"Sübhâne rabbiyel-azîm\" denir.",
                        R.drawable.img_posture_ruku
                ));

        findViewById(R.id.card_posture_secde).setOnClickListener(v ->
                showPostureDialog(
                        getString(R.string.posture_secde),
                        "Önce dizler, sonra eller, ardından alın ve burun yere konur. 3 kez \"Sübhâne rabbiyel-a'lâ\" denir. Erkekler dirsekleri yerden ve yanlardan uzak, kadınlar vücuda yakın tutar.",
                        R.drawable.img_posture_secde
                ));

        findViewById(R.id.card_posture_kade).setOnClickListener(v ->
                showPostureDialog(
                        getString(R.string.posture_kade),
                        "Erkekler sol ayağı yere yatırıp üstüne oturur, sağ ayak parmaklarını kıbleye diker. Kadınlar ayakları sağ tarafa çıkararak oturur. Ettehiyyâtü, Salli-Bârik ve Rabbenâ okunup selam verilir.",
                        R.drawable.img_posture_kade
                ));
    }

    private void showPostureDialog(String title, String description, int imageRes) {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(imageRes);
        imageView.setAdjustViewBounds(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        imageView.setPadding(pad, pad, pad, 0);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(imageView)
                .setMessage(description)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private List<GuideCategory> loadCategories() {
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
            return new Gson().fromJson(json, new TypeToken<List<GuideCategory>>() {}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static class GuideCategory {
        String id;
        String title;
        String subtitle;
        String icon;
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<GuideCategory> items;

        CategoryAdapter(List<GuideCategory> items) {
            this.items = items;
        }

        void setItems(List<GuideCategory> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_namaz_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GuideCategory item = items.get(position);
            holder.txtTitle.setText(item.title);
            holder.txtSubtitle.setText(item.subtitle);
            holder.imgIcon.setImageResource(resolveCategoryIcon(item.icon));
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(NamazRehberiActivity.this, NamazDetailActivity.class);
                intent.putExtra("category_id", item.id);
                intent.putExtra("category_title", item.title);
                intent.putExtra("category_subtitle", item.subtitle);
                startActivity(intent);
            });
        }

        private int resolveCategoryIcon(String iconKey) {
            if (iconKey == null) return R.drawable.ic_mosque;
            switch (iconKey) {
                case "imsak":
                    return R.drawable.ic_imsak;
                case "ogle":
                    return R.drawable.ic_ogle;
                case "ikindi":
                    return R.drawable.ic_ikindi;
                case "aksam":
                    return R.drawable.ic_aksam;
                case "yatsi":
                    return R.drawable.ic_yatsi;
                case "quran":
                    return R.drawable.ic_book;
                case "info":
                    return R.drawable.ic_info_rounded;
                default:
                    return R.drawable.ic_mosque;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgIcon;
            TextView txtTitle, txtSubtitle;

            ViewHolder(View v) {
                super(v);
                imgIcon = v.findViewById(R.id.img_category_icon);
                txtTitle = v.findViewById(R.id.txt_category_title);
                txtSubtitle = v.findViewById(R.id.txt_category_subtitle);
            }
        }
    }
}
