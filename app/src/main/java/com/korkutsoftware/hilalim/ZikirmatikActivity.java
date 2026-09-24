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

public class ZikirmatikActivity extends BaseActivity {

    private int count = 0;
    private int target = 33; // Default 33
    private TextView txtCount, txtTarget;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zikirmatik);

        prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        count = prefs.getInt("zikir_count", 0);
        target = prefs.getInt("zikir_target", 33);

        txtCount = findViewById(R.id.txt_count);
        txtTarget = findViewById(R.id.txt_target);

        updateCountUI();
        updateTargetUI();

        findViewById(R.id.btn_count).setOnClickListener(v -> incrementCount());
        findViewById(R.id.btn_reset).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_target_33).setOnClickListener(v -> setTarget(33));
        findViewById(R.id.btn_target_99).setOnClickListener(v -> setTarget(99));
        findViewById(R.id.btn_target_100).setOnClickListener(v -> setTarget(100));
        findViewById(R.id.btn_target_free).setOnClickListener(v -> setTarget(0));
    }

    private void incrementCount() {
        count++;
        updateCountUI();
        prefs.edit().putInt("zikir_count", count).apply();

        if (target > 0 && count == target) {
            vibrateTargetReached();
            Toast.makeText(this, R.string.target_reached_msg, Toast.LENGTH_LONG).show();
        } else {
            vibrateNormal();
        }
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
        prefs.edit().putInt("zikir_count", count).apply();
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
}
