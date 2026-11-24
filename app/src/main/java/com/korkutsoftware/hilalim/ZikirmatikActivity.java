package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ZikirmatikActivity extends BaseActivity {

    public static final String EXTRA_CUSTOM_ZIKIR_TR = "extra_custom_zikir_tr";
    public static final String EXTRA_CUSTOM_ZIKIR_AR = "extra_custom_zikir_ar";
    public static final String EXTRA_CUSTOM_TARGET = "extra_custom_target";

    private static final String KEY_CUSTOM_ZIKIRLER = "custom_zikirler_json";
    private static final String KEY_DAILY_DATE = "zikir_daily_date";
    private static final String KEY_DAILY_COUNT = "zikir_daily_count";
    private static final String KEY_TOTAL_COUNT = "zikir_total_count";

    private int count = 0;
    private int target = 33; // Default 33
    private int selectedIndex = 0;
    private boolean tesbihatMode = false;
    private int tesbihatStep = 0; // 0: Subhanallah (33), 1: Elhamdulillah (33), 2: Allahu Ekber (33)

    private TextView txtCount, txtTarget, txtZikirAr, txtZikirTr, txtStats;
    private MaterialButton btnTesbihatMode;
    private SharedPreferences prefs;
    private final List<ZikirItem> zikirList = new ArrayList<>();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zikirmatik);
        applySystemBarInsets(findViewById(android.R.id.content));

        prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        reloadZikirList();

        selectedIndex = prefs.getInt("selected_zikir_index", 0);
        if (selectedIndex < 0 || selectedIndex >= zikirList.size()) {
            selectedIndex = 0;
        }
        target = prefs.getInt("zikir_target", 33);

        txtCount = findViewById(R.id.txt_count);
        txtTarget = findViewById(R.id.txt_target);
        txtZikirAr = findViewById(R.id.txt_zikir_ar);
        txtZikirTr = findViewById(R.id.txt_zikir_tr);
        txtStats = findViewById(R.id.txt_zikir_stats);
        btnTesbihatMode = findViewById(R.id.btn_tesbihat_mode);

        handleIncomingCustomZikir();
        loadSelectedZikirState();
        updateTargetUI();
        updateStatsUI();

        findViewById(R.id.card_select_zikir).setOnClickListener(v -> showZikirSelectorDialog());
        findViewById(R.id.btn_count).setOnClickListener(v -> incrementCount());
        findViewById(R.id.btn_reset).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        if (btnTesbihatMode != null) {
            btnTesbihatMode.setOnClickListener(v -> toggleTesbihatMode());
        }

        findViewById(R.id.btn_target_33).setOnClickListener(v -> setTarget(33));
        findViewById(R.id.btn_target_99).setOnClickListener(v -> setTarget(99));
        findViewById(R.id.btn_target_100).setOnClickListener(v -> setTarget(100));
        findViewById(R.id.btn_target_free).setOnClickListener(v -> setTarget(0));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            incrementCount();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void handleIncomingCustomZikir() {
        String customTr = getIntent().getStringExtra(EXTRA_CUSTOM_ZIKIR_TR);
        String customAr = getIntent().getStringExtra(EXTRA_CUSTOM_ZIKIR_AR);
        int customTarget = getIntent().getIntExtra(EXTRA_CUSTOM_TARGET, 0);

        if (!TextUtils.isEmpty(customTr)) {
            int foundIdx = -1;
            for (int i = 0; i < zikirList.size(); i++) {
                if (customTr.equalsIgnoreCase(zikirList.get(i).tr)) {
                    foundIdx = i;
                    break;
                }
            }
            if (foundIdx == -1) {
                addCustomZikir(customTr, customAr != null ? customAr : "");
                foundIdx = zikirList.size() - 1;
            }
            selectedIndex = foundIdx;
            prefs.edit().putInt("selected_zikir_index", selectedIndex).apply();
            if (customTarget > 0) {
                target = customTarget;
                prefs.edit().putInt("zikir_target", target).apply();
            }
        }
    }

    private void reloadZikirList() {
        zikirList.clear();
        zikirList.addAll(loadZikirlerFromAssets());
        String customJson = prefs.getString(KEY_CUSTOM_ZIKIRLER, null);
        if (customJson != null) {
            try {
                List<ZikirItem> custom = gson.fromJson(customJson, new TypeToken<List<ZikirItem>>() {}.getType());
                if (custom != null) {
                    zikirList.addAll(custom);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void addCustomZikir(String tr, String ar) {
        int nextId = 1000;
        for (ZikirItem item : zikirList) {
            if (item.id >= nextId) nextId = item.id + 1;
        }
        ZikirItem newItem = new ZikirItem(nextId, tr.trim(), ar != null ? ar.trim() : "");
        List<ZikirItem> customOnly = new ArrayList<>();
        String existingJson = prefs.getString(KEY_CUSTOM_ZIKIRLER, null);
        if (existingJson != null) {
            try {
                List<ZikirItem> loaded = gson.fromJson(existingJson, new TypeToken<List<ZikirItem>>() {}.getType());
                if (loaded != null) customOnly.addAll(loaded);
            } catch (Exception ignored) {
            }
        }
        customOnly.add(newItem);
        prefs.edit().putString(KEY_CUSTOM_ZIKIRLER, gson.toJson(customOnly)).apply();
        zikirList.add(newItem);
    }

    private List<ZikirItem> loadZikirlerFromAssets() {
        try (InputStream is = getAssets().open("zikirler.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            List<ZikirItem> loaded = gson.fromJson(json, new TypeToken<List<ZikirItem>>() {}.getType());
            if (loaded != null && !loaded.isEmpty()) {
                return loaded;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<ZikirItem> fallback = new ArrayList<>();
        fallback.add(new ZikirItem(1, "Subhanallah", "سُبْحَانَ اللّٰهِ"));
        return fallback;
    }

    private String getCountPrefKey(ZikirItem item) {
        return "zikir_count_" + item.id;
    }

    private void toggleTesbihatMode() {
        tesbihatMode = !tesbihatMode;
        if (tesbihatMode) {
            tesbihatStep = 0;
            target = 33;
            selectedIndex = 0;
            count = 0;
            updateTesbihatStepDisplay();
            updateTargetUI();
            if (btnTesbihatMode != null) {
                btnTesbihatMode.setBackgroundColor(ContextCompat.getColor(this, R.color.gold));
                btnTesbihatMode.setTextColor(ContextCompat.getColor(this, R.color.deep_charcoal));
            }
            Toast.makeText(this, R.string.tesbihat_started, Toast.LENGTH_SHORT).show();
        } else {
            if (btnTesbihatMode != null) {
                btnTesbihatMode.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                btnTesbihatMode.setTextColor(ContextCompat.getColor(this, R.color.gold));
            }
            loadSelectedZikirState();
            updateTargetUI();
        }
        vibrateNormal();
    }

    private void updateTesbihatStepDisplay() {
        if (tesbihatStep >= 0 && tesbihatStep < zikirList.size()) {
            ZikirItem item = zikirList.get(tesbihatStep);
            if (txtZikirAr != null) txtZikirAr.setText(item.ar);
            if (txtZikirTr != null) {
                txtZikirTr.setText(item.tr + " (" + (tesbihatStep + 1) + "/3)");
            }
        }
        updateCountUI();
    }

    private void loadSelectedZikirState() {
        if (zikirList.isEmpty()) return;
        ZikirItem current = zikirList.get(selectedIndex);
        if (txtZikirAr != null) txtZikirAr.setText(TextUtils.isEmpty(current.ar) ? current.tr : current.ar);
        if (txtZikirTr != null) txtZikirTr.setText(current.tr);

        String key = getCountPrefKey(current);
        if (prefs.contains(key)) {
            count = prefs.getInt(key, 0);
        } else if (current.id == 1) {
            count = prefs.getInt("zikir_count", 0);
        } else {
            count = 0;
        }
        updateCountUI();
    }

    private void showZikirSelectorDialog() {
        if (zikirList.isEmpty()) return;
        String[] labels = new String[zikirList.size()];
        for (int i = 0; i < zikirList.size(); i++) {
            ZikirItem item = zikirList.get(i);
            int saved = prefs.contains(getCountPrefKey(item))
                    ? prefs.getInt(getCountPrefKey(item), 0)
                    : (item.id == 1 ? prefs.getInt("zikir_count", 0) : 0);
            String prefix = !TextUtils.isEmpty(item.ar) ? item.ar + "\n" : "";
            labels[i] = prefix + item.tr + " (" + saved + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_zikir_title)
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    if (tesbihatMode) toggleTesbihatMode();
                    selectedIndex = which;
                    prefs.edit().putInt("selected_zikir_index", selectedIndex).apply();
                    loadSelectedZikirState();
                    vibrateNormal();
                    dialog.dismiss();
                })
                .setNeutralButton(R.string.add_custom_zikir, (dialog, which) -> showAddCustomZikirDialog())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAddCustomZikirDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);

        EditText etName = new EditText(this);
        etName.setHint(R.string.custom_zikir_name_hint);
        layout.addView(etName);

        EditText etArabic = new EditText(this);
        etArabic.setHint(R.string.custom_zikir_arabic_hint);
        layout.addView(etArabic);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_custom_zikir)
                .setView(layout)
                .setPositiveButton(R.string.btn_save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String ar = etArabic.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.custom_zikir_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addCustomZikir(name, ar);
                    selectedIndex = zikirList.size() - 1;
                    prefs.edit().putInt("selected_zikir_index", selectedIndex).apply();
                    if (tesbihatMode) toggleTesbihatMode();
                    else loadSelectedZikirState();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void incrementCount() {
        count++;
        recordStatsIncrement();
        updateStatsUI();

        if (tesbihatMode) {
            if (count >= 33) {
                vibrateTargetReached();
                if (tesbihatStep < 2) {
                    tesbihatStep++;
                    count = 0;
                    updateTesbihatStepDisplay();
                } else {
                    count = 33;
                    updateCountUI();
                    Toast.makeText(this, R.string.tesbihat_completed, Toast.LENGTH_LONG).show();
                    toggleTesbihatMode();
                }
            } else {
                updateCountUI();
                vibrateNormal();
            }
            return;
        }

        updateCountUI();
        saveCurrentCount();

        if (target > 0 && count == target) {
            vibrateTargetReached();
            Toast.makeText(this, R.string.target_reached_msg, Toast.LENGTH_LONG).show();
        } else {
            vibrateNormal();
        }
    }

    private void recordStatsIncrement() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDate = prefs.getString(KEY_DAILY_DATE, "");
        int daily = today.equals(savedDate) ? prefs.getInt(KEY_DAILY_COUNT, 0) : 0;
        int total = prefs.getInt(KEY_TOTAL_COUNT, 0);
        prefs.edit()
                .putString(KEY_DAILY_DATE, today)
                .putInt(KEY_DAILY_COUNT, daily + 1)
                .putInt(KEY_TOTAL_COUNT, total + 1)
                .apply();
    }

    private void updateStatsUI() {
        if (txtStats == null) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDate = prefs.getString(KEY_DAILY_DATE, "");
        int daily = today.equals(savedDate) ? prefs.getInt(KEY_DAILY_COUNT, 0) : 0;
        int total = prefs.getInt(KEY_TOTAL_COUNT, 0);
        txtStats.setText(getString(R.string.zikir_stats_format, daily, total));
    }

    private void saveCurrentCount() {
        SharedPreferences.Editor editor = prefs.edit().putInt("zikir_count", count);
        if (!zikirList.isEmpty()) {
            editor.putInt(getCountPrefKey(zikirList.get(selectedIndex)), count);
        }
        editor.apply();
    }

    private void setTarget(int newTarget) {
        if (tesbihatMode) {
            toggleTesbihatMode();
        }
        this.target = newTarget;
        prefs.edit().putInt("zikir_target", target).apply();
        updateTargetUI();
        vibrateNormal();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(R.string.reset_confirm_msg)
                .setPositiveButton(R.string.yes, (dialog, which) -> resetCount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void resetCount() {
        count = 0;
        updateCountUI();
        if (!tesbihatMode) {
            saveCurrentCount();
        }
        vibrateNormal();
    }

    private void updateCountUI() {
        if (txtCount != null) {
            txtCount.setText(String.valueOf(count));
        }
    }

    private void updateTargetUI() {
        if (txtTarget != null) {
            if (target > 0) {
                txtTarget.setText(String.format(getString(R.string.target_label), target));
            } else {
                txtTarget.setText(String.format("%s: %s", getString(R.string.target_label).split(":")[0], getString(R.string.target_free)));
            }
        }
    }

    private void vibrateNormal() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(40);
            }
        }
    }

    private void vibrateTargetReached() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 100, 100, 200};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(300);
            }
        }
    }

    private static class ZikirItem {
        int id;
        String tr;
        String ar;

        ZikirItem(int id, String tr, String ar) {
            this.id = id;
            this.tr = tr;
            this.ar = ar;
        }
    }
}
