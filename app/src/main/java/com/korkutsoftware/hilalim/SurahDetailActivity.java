package com.korkutsoftware.hilalim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView txtNoContent, txtTitle, txtSubtitle;
    private View btnPrevSurah, btnNextSurah;
    private int currentSurahId = 1;
    private String currentSurahName = "";
    private List<SurahInfo> allSurahInfos = null;
    private List<SurahVerses> allSurahVerses = null;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_surah_detail);
        applySystemBarInsets(findViewById(android.R.id.content));

        String surahName = getIntent().getStringExtra("surah_name");
        currentSurahId = getIntent().getIntExtra("surah_id", 1);
        currentSurahName = surahName != null ? surahName : getString(R.string.surah_detail_title);

        txtTitle = findViewById(R.id.txt_surah_name_main);
        txtSubtitle = findViewById(R.id.txt_surah_subtitle);
        txtTitle.setText(currentSurahName);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_verses);
        progressBar = findViewById(R.id.progress_loading);
        txtNoContent = findViewById(R.id.txt_no_content);

        rv.setLayoutManager(new LinearLayoutManager(this));

        btnPrevSurah = findViewById(R.id.btn_prev_surah);
        btnNextSurah = findViewById(R.id.btn_next_surah);
        btnPrevSurah.setOnClickListener(v -> navigateSurah(-1));
        btnNextSurah.setOnClickListener(v -> navigateSurah(1));
        updateNavigationButtons();
        saveLastReadSurah(currentSurahId, currentSurahName);

        loadSurahListAsync();
        loadDataAsync(currentSurahId);
    }

    private void navigateSurah(int delta) {
        int targetId = currentSurahId + delta;
        if (targetId >= 1 && targetId <= 114) {
            currentSurahId = targetId;
            updateNavigationButtons();
            updateSurahTitle(currentSurahId);
            loadDataAsync(currentSurahId);
        }
    }

    private void updateNavigationButtons() {
        if (btnPrevSurah != null) {
            boolean enabled = currentSurahId > 1;
            btnPrevSurah.setEnabled(enabled);
            btnPrevSurah.setAlpha(enabled ? 1.0f : 0.35f);
        }
        if (btnNextSurah != null) {
            boolean enabled = currentSurahId < 114;
            btnNextSurah.setEnabled(enabled);
            btnNextSurah.setAlpha(enabled ? 1.0f : 0.35f);
        }
    }

    private void saveLastReadSurah(int surahId, String surahName) {
        if (TextUtils.isEmpty(surahName)) return;
        getSharedPreferences(QuranFragment.PREFS_QURAN, MODE_PRIVATE)
                .edit()
                .putInt(QuranFragment.KEY_LAST_SURAH_ID, surahId)
                .putString(QuranFragment.KEY_LAST_SURAH_NAME, surahName)
                .apply();
    }

    private void updateSurahTitle(int surahId) {
        if (allSurahInfos != null) {
            for (SurahInfo info : allSurahInfos) {
                if (info.id == surahId) {
                    currentSurahName = info.name;
                    txtTitle.setText(info.name);
                    if (txtSubtitle != null) {
                        String numberLabel = getString(R.string.quran_surah_format, surahId);
                        txtSubtitle.setText(!TextUtils.isEmpty(info.desc)
                                ? numberLabel + " • " + info.desc
                                : numberLabel);
                        txtSubtitle.setVisibility(View.VISIBLE);
                    }
                    saveLastReadSurah(surahId, info.name);
                    return;
                }
            }
        }
        currentSurahName = getString(R.string.quran_surah_format, surahId);
        txtTitle.setText(currentSurahName);
        if (txtSubtitle != null) {
            txtSubtitle.setVisibility(View.GONE);
        }
    }

    private void loadSurahListAsync() {
        executorService.execute(() -> {
            try (InputStream is = getAssets().open("surahs.json")) {
                int size = is.available();
                byte[] buffer = new byte[size];
                int offset = 0;
                while (offset < size) {
                    int read = is.read(buffer, offset, size - offset);
                    if (read == -1) break;
                    offset += read;
                }
                String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
                allSurahInfos = new Gson().fromJson(json, new TypeToken<List<SurahInfo>>(){}.getType());
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        updateSurahTitle(currentSurahId);
                    }
                });
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
                if (isFinishing() || isDestroyed()) return;
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
            if (allSurahVerses == null) {
                try (InputStream is = getAssets().open("quran_verses.json")) {
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    int offset = 0;
                    while (offset < size) {
                        int read = is.read(buffer, offset, size - offset);
                        if (read == -1) break;
                        offset += read;
                    }
                    String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
                    allSurahVerses = new Gson().fromJson(json, new TypeToken<List<SurahVerses>>(){}.getType());
                }
            }

            if (allSurahVerses != null) {
                for (SurahVerses surah : allSurahVerses) {
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
        String name, desc;
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
            if (holder.btnCopy != null) {
                holder.btnCopy.setOnClickListener(v -> copyVerse(item));
            }
        }

        private void copyVerse(VerseItem item) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            String text = currentSurahName + " (" + currentSurahId + ":" + item.id + ")\n\n"
                    + item.ar + "\n\n" + item.tr;
            clipboard.setPrimaryClip(ClipData.newPlainText(currentSurahName, text));
            Toast.makeText(SurahDetailActivity.this, R.string.quran_verse_copied, Toast.LENGTH_SHORT).show();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtId, txtAr, txtTr;
            View btnCopy;

            ViewHolder(View v) {
                super(v);
                txtId = v.findViewById(R.id.txt_verse_id);
                txtAr = v.findViewById(R.id.txt_verse_ar);
                txtTr = v.findViewById(R.id.txt_verse_tr);
                btnCopy = v.findViewById(R.id.btn_copy_verse);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
