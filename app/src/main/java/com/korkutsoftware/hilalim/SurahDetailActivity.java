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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.util.QuranPrefsHelper;
import com.korkutsoftware.hilalim.util.ShareCardHelper;
import android.content.Intent;
import android.widget.SeekBar;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;
import com.korkutsoftware.hilalim.data.db.AppDatabase;
import com.korkutsoftware.hilalim.data.db.QuranVerseEntity;
import com.korkutsoftware.hilalim.util.QuranImportHelper;
import com.korkutsoftware.hilalim.util.QuranReciterHelper;

import java.util.Locale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SurahDetailActivity extends BaseActivity implements QuranAudioService.AudioStateListener {

    private RecyclerView rv;
    private LinearLayoutManager layoutManager;
    private ProgressBar progressBar;
    private TextView txtNoContent, txtTitle, txtSubtitle;
    private View btnPrevSurah, btnNextSurah;
    private int currentSurahId = 1;
    private String currentSurahName = "";
    private int targetVerseId = 0;
    private List<SurahInfo> allSurahInfos = null;
    private List<SurahVerses> allSurahVerses = null;
    private VerseAdapter currentAdapter = null;
    private View miniPlayerCard;
    private ImageView btnMiniPlayPause;
    private TextView txtMiniTitle, txtMiniReciter, txtMiniRepeat, txtMiniTime;
    private SeekBar seekMiniAudio;
    private boolean userSeeking = false;
    private int playingVerseId = -1;

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
        targetVerseId = getIntent().getIntExtra("verse_id", 0);
        if (targetVerseId <= 0 && currentSurahId == QuranPrefsHelper.getLastSurahId(this)) {
            targetVerseId = QuranPrefsHelper.getLastVerseId(this);
        }
        currentSurahName = surahName != null ? surahName : getString(R.string.surah_detail_title);

        txtTitle = findViewById(R.id.txt_surah_name_main);
        txtSubtitle = findViewById(R.id.txt_surah_subtitle);
        txtTitle.setText(currentSurahName);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        View btnTextSettings = findViewById(R.id.btn_text_settings);
        if (btnTextSettings != null) {
            btnTextSettings.setOnClickListener(v -> showTextSettingsDialog());
        }

        View btnReciter = findViewById(R.id.btn_reciter);
        if (btnReciter != null) {
            btnReciter.setOnClickListener(v -> showReciterDialog());
        }
        setupMiniPlayer();

        rv = findViewById(R.id.rv_verses);
        progressBar = findViewById(R.id.progress_loading);
        txtNoContent = findViewById(R.id.txt_no_content);

        layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    persistVisibleVerse();
                }
            }
        });

        btnPrevSurah = findViewById(R.id.btn_prev_surah);
        btnNextSurah = findViewById(R.id.btn_next_surah);
        btnPrevSurah.setOnClickListener(v -> navigateSurah(-1));
        btnNextSurah.setOnClickListener(v -> navigateSurah(1));
        updateNavigationButtons();
        QuranPrefsHelper.saveLastRead(this, currentSurahId, currentSurahName, Math.max(1, targetVerseId));

        loadSurahListAsync();
        loadDataAsync(currentSurahId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        QuranAudioService.addListener(this);
        syncMiniPlayerInitialState();
    }

    @Override
    protected void onStop() {
        QuranAudioService.removeListener(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        persistVisibleVerse();
        super.onPause();
    }

    private void persistVisibleVerse() {
        if (currentAdapter == null || layoutManager == null) return;
        int firstPos = layoutManager.findFirstVisibleItemPosition();
        if (firstPos >= 0 && firstPos < currentAdapter.items.size()) {
            int verseId = currentAdapter.items.get(firstPos).id;
            QuranPrefsHelper.saveLastRead(this, currentSurahId, currentSurahName, verseId);
        }
    }

    private void showTextSettingsDialog() {
        int currentLevel = QuranPrefsHelper.getFontLevel(this);
        boolean showTr = QuranPrefsHelper.isShowTranslation(this);

        String[] sizeLabels = {
                getString(R.string.font_size_small),
                getString(R.string.font_size_medium),
                getString(R.string.font_size_large),
                getString(R.string.font_size_xlarge)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.quran_text_settings)
                .setSingleChoiceItems(sizeLabels, currentLevel, (dialog, which) -> {
                    QuranPrefsHelper.setFontLevel(this, which);
                    if (currentAdapter != null) {
                        currentAdapter.notifyDataSetChanged();
                    }
                    dialog.dismiss();
                })
                .setNeutralButton(
                        showTr ? R.string.quran_hide_translation : R.string.quran_show_translation,
                        (dialog, which) -> {
                            QuranPrefsHelper.setShowTranslation(this, !showTr);
                            if (currentAdapter != null) {
                                currentAdapter.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupMiniPlayer() {
        miniPlayerCard = findViewById(R.id.mini_player_card);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        txtMiniTitle = findViewById(R.id.txt_mini_title);
        txtMiniReciter = findViewById(R.id.txt_mini_reciter);
        txtMiniRepeat = findViewById(R.id.btn_mini_repeat);
        txtMiniTime = findViewById(R.id.txt_mini_time);
        seekMiniAudio = findViewById(R.id.seek_mini_audio);

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_TOGGLE_PLAY_PAUSE));
        }
        View btnPrev = findViewById(R.id.btn_mini_prev);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_PREV_VERSE));
        }
        View btnNext = findViewById(R.id.btn_mini_next);
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_NEXT_VERSE));
        }
        View btnClose = findViewById(R.id.btn_mini_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_STOP));
        }
        if (txtMiniRepeat != null) {
            txtMiniRepeat.setOnClickListener(v -> {
                int current = QuranPrefsHelper.getRepeatCount(this);
                int next = current == 1 ? 3 : (current == 3 ? 5 : 1);
                QuranPrefsHelper.setRepeatCount(this, next);
                txtMiniRepeat.setText(getString(R.string.quran_repeat_format, next));
            });
        }
        if (seekMiniAudio != null) {
            seekMiniAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    userSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    userSeeking = false;
                    Intent intent = new Intent(SurahDetailActivity.this, QuranAudioService.class);
                    intent.setAction(QuranAudioService.ACTION_SEEK_TO);
                    intent.putExtra(QuranAudioService.EXTRA_SEEK_POS_MS, (long) seekBar.getProgress());
                    startService(intent);
                }
            });
        }
    }

    private void syncMiniPlayerInitialState() {
        if (QuranAudioService.isActive()) {
            onAudioStateChanged(
                    true,
                    QuranAudioService.isPlayingNow(),
                    QuranAudioService.getCurrentSurahId(),
                    QuranAudioService.getCurrentVerseId(),
                    QuranAudioService.getCurrentSurahName(),
                    0L,
                    0L,
                    QuranPrefsHelper.getRepeatCount(this)
            );
        } else if (miniPlayerCard != null) {
            miniPlayerCard.setVisibility(View.GONE);
            playingVerseId = -1;
        }
    }

    private void showReciterDialog() {
        int currentIdx = QuranPrefsHelper.getReciterIndex(this);
        String[] reciters = QuranReciterHelper.getReciterNames(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.quran_reciter_select)
                .setSingleChoiceItems(reciters, currentIdx, (dialog, which) -> {
                    QuranPrefsHelper.setReciterIndex(this, which);
                    if (QuranAudioService.isActive()) {
                        sendAudioAction(QuranAudioService.ACTION_RELOAD_SETTINGS);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void playVerse(int verseId) {
        if (currentAdapter == null || currentAdapter.items.isEmpty()) return;
        int maxVerses = currentAdapter.items.size();
        if (QuranAudioService.isActive()
                && QuranAudioService.getCurrentSurahId() == currentSurahId
                && QuranAudioService.getCurrentVerseId() == verseId) {
            sendAudioAction(QuranAudioService.ACTION_TOGGLE_PLAY_PAUSE);
            return;
        }
        Intent intent = new Intent(this, QuranAudioService.class);
        intent.setAction(QuranAudioService.ACTION_PLAY_VERSE);
        intent.putExtra(QuranAudioService.EXTRA_SURAH_ID, currentSurahId);
        intent.putExtra(QuranAudioService.EXTRA_SURAH_NAME, currentSurahName);
        intent.putExtra(QuranAudioService.EXTRA_VERSE_ID, verseId);
        intent.putExtra(QuranAudioService.EXTRA_MAX_VERSES, maxVerses);
        ContextCompat.startForegroundService(this, intent);
    }

    private void sendAudioAction(String action) {
        Intent intent = new Intent(this, QuranAudioService.class);
        intent.setAction(action);
        startService(intent);
    }

    @Override
    public void onAudioStateChanged(boolean active, boolean playing, int surahId, int verseId,
                                    String surahName, long positionMs, long durationMs, int repeatTarget) {
        mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || miniPlayerCard == null) return;
            if (!active) {
                miniPlayerCard.setVisibility(View.GONE);
                if (playingVerseId != -1) {
                    playingVerseId = -1;
                    if (currentAdapter != null) currentAdapter.notifyDataSetChanged();
                }
                return;
            }
            miniPlayerCard.setVisibility(View.VISIBLE);
            if (btnMiniPlayPause != null) {
                btnMiniPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            }
            if (txtMiniTitle != null) {
                txtMiniTitle.setText(surahName + " • " + getString(R.string.quran_verse_format, verseId));
            }
            if (txtMiniReciter != null) {
                txtMiniReciter.setText(QuranReciterHelper.getReciterName(this, QuranPrefsHelper.getReciterIndex(this)));
            }
            if (txtMiniRepeat != null) {
                txtMiniRepeat.setText(getString(R.string.quran_repeat_format, repeatTarget));
            }
            if (seekMiniAudio != null && !userSeeking) {
                int max = durationMs > 0 ? (int) durationMs : 100;
                seekMiniAudio.setMax(max);
                seekMiniAudio.setProgress(durationMs > 0 ? (int) Math.min(positionMs, durationMs) : 0);
            }
            if (txtMiniTime != null) {
                txtMiniTime.setText(durationMs > 0
                        ? formatMs(positionMs) + " / " + formatMs(durationMs)
                        : getString(R.string.quran_audio_loading));
            }

            int newHighlight = (surahId == currentSurahId) ? verseId : -1;
            if (newHighlight != playingVerseId) {
                playingVerseId = newHighlight;
                if (currentAdapter != null) {
                    currentAdapter.notifyDataSetChanged();
                    if (playingVerseId > 0) {
                        for (int i = 0; i < currentAdapter.items.size(); i++) {
                            if (currentAdapter.items.get(i).id == playingVerseId) {
                                rv.smoothScrollToPosition(i);
                                break;
                            }
                        }
                    }
                }
            } else if (currentAdapter != null) {
                currentAdapter.notifyDataSetChanged();
            }
        });
    }

    private String formatMs(long ms) {
        long totalSec = Math.max(0L, ms / 1000L);
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    private void navigateSurah(int delta) {
        int targetId = currentSurahId + delta;
        if (targetId >= 1 && targetId <= 114) {
            currentSurahId = targetId;
            targetVerseId = 1;
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
                    QuranPrefsHelper.saveLastRead(this, surahId, info.name, Math.max(1, targetVerseId));
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
                    currentAdapter = new VerseAdapter(verses);
                    rv.setAdapter(currentAdapter);
                    if (targetVerseId > 1) {
                        for (int i = 0; i < verses.size(); i++) {
                            if (verses.get(i).id == targetVerseId) {
                                int pos = i;
                                rv.post(() -> layoutManager.scrollToPositionWithOffset(pos, 16));
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    private List<VerseItem> loadVersesFromJson(int surahId) {
        try {
            if (QuranImportHelper.isImported(this)) {
                List<QuranVerseEntity> dbVerses = AppDatabase.getInstance(this).quranDao().getVersesBySurah(surahId);
                if (dbVerses != null && !dbVerses.isEmpty()) {
                    List<VerseItem> mapped = new ArrayList<>(dbVerses.size());
                    for (QuranVerseEntity e : dbVerses) {
                        VerseItem vi = new VerseItem();
                        vi.id = e.verseId;
                        vi.ar = e.arText;
                        vi.tr = e.trText;
                        mapped.add(vi);
                    }
                    return mapped;
                }
            }
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
        final List<VerseItem> items;

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
            int fontLevel = QuranPrefsHelper.getFontLevel(SurahDetailActivity.this);
            boolean showTr = QuranPrefsHelper.isShowTranslation(SurahDetailActivity.this);

            holder.txtId.setText(String.valueOf(item.id));
            holder.txtAr.setText(item.ar);
            holder.txtAr.setTextSize(QuranPrefsHelper.getArabicTextSizeSp(fontLevel));
            holder.txtTr.setText(item.tr);
            holder.txtTr.setTextSize(QuranPrefsHelper.getTranslationTextSizeSp(fontLevel));
            holder.txtTr.setVisibility(showTr ? View.VISIBLE : View.GONE);
            if (holder.divider != null) {
                holder.divider.setVisibility(showTr ? View.VISIBLE : View.GONE);
            }

            boolean bookmarked = QuranPrefsHelper.isBookmarked(SurahDetailActivity.this, currentSurahId, item.id);
            updateBookmarkTint(holder.btnBookmark, bookmarked);

            if (holder.btnBookmark != null) {
                holder.btnBookmark.setOnClickListener(v -> {
                    boolean nowSaved = QuranPrefsHelper.toggleBookmark(
                            SurahDetailActivity.this,
                            new QuranPrefsHelper.Bookmark(currentSurahId, currentSurahName, item.id, item.ar, item.tr)
                    );
                    updateBookmarkTint(holder.btnBookmark, nowSaved);
                    Toast.makeText(
                            SurahDetailActivity.this,
                            nowSaved ? R.string.quran_bookmark_added : R.string.quran_bookmark_removed,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }

            if (holder.btnShare != null) {
                holder.btnShare.setOnClickListener(v -> ShareCardHelper.showShareOptionsDialog(
                        SurahDetailActivity.this,
                        getString(R.string.quran_main_title),
                        item.ar,
                        item.tr,
                        currentSurahName + " (" + currentSurahId + ":" + item.id + ")"
                ));
            }

            if (holder.btnCopy != null) {
                holder.btnCopy.setOnClickListener(v -> copyVerse(item));
            }

            boolean isThisPlayingVerse = (item.id == playingVerseId);
            if (holder.cardVerse != null) {
                int strokeWidthPx = Math.round(getResources().getDisplayMetrics().density * (isThisPlayingVerse ? 2 : 1));
                holder.cardVerse.setStrokeWidth(strokeWidthPx);
                holder.cardVerse.setStrokeColor(ContextCompat.getColor(
                        SurahDetailActivity.this,
                        isThisPlayingVerse ? R.color.gold : R.color.card_stroke
                ));
                int baseCardBg = ContextCompat.getColor(SurahDetailActivity.this, R.color.card_bg);
                int bg = isThisPlayingVerse
                        ? ColorUtils.compositeColors(
                                ContextCompat.getColor(SurahDetailActivity.this, R.color.selected_lang_bg),
                                baseCardBg)
                        : baseCardBg;
                holder.cardVerse.setCardBackgroundColor(bg);
            }
            if (holder.btnPlay != null) {
                boolean showPause = isThisPlayingVerse && QuranAudioService.isPlayingNow();
                holder.btnPlay.setImageResource(showPause ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
                holder.btnPlay.setOnClickListener(v -> playVerse(item.id));
            }
        }

        private void updateBookmarkTint(ImageView icon, boolean active) {
            if (icon == null) return;
            int color = ContextCompat.getColor(
                    SurahDetailActivity.this,
                    active ? R.color.secondary : R.color.text_muted
            );
            icon.setColorFilter(color);
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
            MaterialCardView cardVerse;
            TextView txtId, txtAr, txtTr;
            View divider, btnCopy, btnShare;
            ImageView btnBookmark, btnPlay;

            ViewHolder(View v) {
                super(v);
                cardVerse = v.findViewById(R.id.card_verse);
                txtId = v.findViewById(R.id.txt_verse_id);
                txtAr = v.findViewById(R.id.txt_verse_ar);
                txtTr = v.findViewById(R.id.txt_verse_tr);
                divider = v.findViewById(R.id.divider_verse);
                btnPlay = v.findViewById(R.id.btn_play_verse);
                btnBookmark = v.findViewById(R.id.btn_bookmark_verse);
                btnShare = v.findViewById(R.id.btn_share_verse);
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
