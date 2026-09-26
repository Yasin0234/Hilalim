package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SurahDetailActivity extends BaseActivity {

    private RecyclerView rv;
    private ProgressBar progressBar;
    private TextView txtNoContent, txtTitle;
    private int currentSurahId = 1;
    private List<SurahInfo> allSurahInfos = null;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_surah_detail);

        String surahName = getIntent().getStringExtra("surah_name");
        currentSurahId = getIntent().getIntExtra("surah_id", 1);

        txtTitle = findViewById(R.id.txt_surah_name_main);
        txtTitle.setText(surahName != null ? surahName : "Sure Detayı");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_verses);
        progressBar = findViewById(R.id.progress_loading);
        txtNoContent = findViewById(R.id.txt_no_content);

        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_prev_surah).setOnClickListener(v -> navigateSurah(-1));
        findViewById(R.id.btn_next_surah).setOnClickListener(v -> navigateSurah(1));

        loadSurahListAsync();
        loadDataAsync(currentSurahId);
    }

    private void navigateSurah(int delta) {
        int targetId = currentSurahId + delta;
        if (targetId >= 1 && targetId <= 114) {
            currentSurahId = targetId;
            updateSurahTitle(currentSurahId);
            loadDataAsync(currentSurahId);
        }
    }

    private void updateSurahTitle(int surahId) {
        if (allSurahInfos != null) {
            for (SurahInfo info : allSurahInfos) {
                if (info.id == surahId) {
                    txtTitle.setText(info.name);
                    return;
                }
            }
        }
        txtTitle.setText("Sure " + surahId);
    }

    private void loadSurahListAsync() {
        executorService.execute(() -> {
            try {
                InputStream is = getAssets().open("surahs.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, StandardCharsets.UTF_8);
                allSurahInfos = new Gson().fromJson(json, new TypeToken<List<SurahInfo>>(){}.getType());
                mainHandler.post(() -> updateSurahTitle(currentSurahId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadDataAsync(int surahId) {
        progressBar.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        txtNoContent.setVisibility(View.GONE);

        executorService.execute(() -> {
            List<VerseItem> verses = loadVersesFromJson(surahId);
            
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (verses == null || verses.isEmpty()) {
                    txtNoContent.setVisibility(View.VISIBLE);
                } else {
                    rv.setVisibility(View.VISIBLE);
                    rv.setAdapter(new VerseAdapter(verses));
                }
            });
        });
    }

    private List<VerseItem> loadVersesFromJson(int surahId) {
        try {
            InputStream is = getAssets().open("quran_verses.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int read = is.read(buffer);
            is.close();
            if (read == -1) return new ArrayList<>();
            
            String json = new String(buffer, StandardCharsets.UTF_8);
            List<SurahVerses> allSurahs = new Gson().fromJson(json, new TypeToken<List<SurahVerses>>(){}.getType());
            
            if (allSurahs != null) {
                for (SurahVerses surah : allSurahs) {
                    if (surah.surah_id == surahId) {
                        return surah.verses;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static class SurahInfo {
        int id;
        String name;
    }

    private static class SurahVerses {
        int surah_id;
        List<VerseItem> verses;
    }

    private static class VerseItem {
        int id;
        String ar, tr;
    }

    private class VerseAdapter extends RecyclerView.Adapter<VerseAdapter.ViewHolder> {
        private final List<VerseItem> items;

        VerseAdapter(List<VerseItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verse, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VerseItem item = items.get(position);
            holder.txtId.setText(String.valueOf(item.id));
            holder.txtAr.setText(item.ar);
            holder.txtTr.setText(item.tr);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtId, txtAr, txtTr;

            ViewHolder(View v) {
                super(v);
                txtId = v.findViewById(R.id.txt_verse_id);
                txtAr = v.findViewById(R.id.txt_verse_ar);
                txtTr = v.findViewById(R.id.txt_verse_tr);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}