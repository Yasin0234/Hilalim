package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ZikirmatikActivity extends BaseActivity {

    private int count = 0;
    private int target = 33; // Default 33
    private int selectedIndex = 0;
    private TextView txtCount, txtTarget, txtZikirAr, txtZikirTr;
    private SharedPreferences prefs;
    private List<ZikirItem> zikirList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zikirmatik);
        applySystemBarInsets(findViewById(android.R.id.content));

        prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        zikirList = loadZikirlerFromAssets();
        selectedIndex = prefs.getInt("selected_zikir_index", 0);
        if (selectedIndex < 0 || selectedIndex >= zikirList.size()) {
            selectedIndex = 0;
        }
        target = prefs.getInt("zikir_target", 33);

        txtCount = findViewById(R.id.txt_count);
        txtTarget = findViewById(R.id.txt_target);
        txtZikirAr = findViewById(R.id.txt_zikir_ar);
        txtZikirTr = findViewById(R.id.txt_zikir_tr);

        loadSelectedZikirState();
        updateTargetUI();

        findViewById(R.id.card_select_zikir).setOnClickListener(v -> showZikirSelectorDialog());
        findViewById(R.id.btn_count).setOnClickListener(v -> incrementCount());
        findViewById(R.id.btn_reset).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_target_33).setOnClickListener(v -> setTarget(33));
        findViewById(R.id.btn_target_99).setOnClickListener(v -> setTarget(99));
        findViewById(R.id.btn_target_100).setOnClickListener(v -> setTarget(100));
        findViewById(R.id.btn_target_free).setOnClickListener(v -> setTarget(0));
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
            List<ZikirItem> loaded = new Gson().fromJson(json, new TypeToken<List<ZikirItem>>() {}.getType());
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

    private void loadSelectedZikirState() {
        if (zikirList.isEmpty()) return;
        ZikirItem current = zikirList.get(selectedIndex);
        if (txtZikirAr != null) txtZikirAr.setText(current.ar);
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
            labels[i] = item.ar + "\n" + item.tr + " (" + saved + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_zikir_title)
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    selectedIndex = which;
                    prefs.edit().putInt("selected_zikir_index", selectedIndex).apply();
                    loadSelectedZikirState();
                    vibrateNormal();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void incrementCount() {
        count++;
        updateCountUI();
        saveCurrentCount();

        if (target > 0 && count == target) {
            vibrateTargetReached();
            Toast.makeText(this, R.string.target_reached_msg, Toast.LENGTH_LONG).show();
        } else {
            vibrateNormal();
        }
    }

    private void saveCurrentCount() {
        SharedPreferences.Editor editor = prefs.edit().putInt("zikir_count", count);
        if (!zikirList.isEmpty()) {
            editor.putInt(getCountPrefKey(zikirList.get(selectedIndex)), count);
        }
        editor.apply();
    }

    private void setTarget(int newTarget) {
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
        saveCurrentCount();
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
