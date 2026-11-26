package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.korkutsoftware.hilalim.util.AlarmHelper;
import com.korkutsoftware.hilalim.util.OngoingPrayerNotificationHelper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.korkutsoftware.hilalim.util.DndHelper;

public class NotificationSettingsActivity extends BaseActivity {

    private SharedPreferences prefs;
    private MaterialButton btnTestEzan;
    private TextView txtEzanSoundValue, txtPreMinutesValue, txtPreCustomLabel;
    private MaterialSwitch switchDnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_settings);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);

        bindSwitch(R.id.switch_master, "notif_enabled", true);
        bindSwitch(R.id.switch_ongoing_notif, OngoingPrayerNotificationHelper.PREF_ONGOING_ENABLED, true);
        bindSwitch(R.id.switch_ezan, "notif_ezan_enabled", true);
        bindSwitch(R.id.switch_pre_45, "notif_pre_45", true);
        bindSwitch(R.id.switch_pre_15, "notif_pre_15", true);

        bindSwitch(R.id.switch_fajr, "notif_prayer_fajr", true);
        bindSwitch(R.id.switch_sunrise, "notif_prayer_sunrise", true);
        bindSwitch(R.id.switch_dhuhr, "notif_prayer_dhuhr", true);
        bindSwitch(R.id.switch_asr, "notif_prayer_asr", true);
        bindSwitch(R.id.switch_maghrib, "notif_prayer_maghrib", true);
        bindSwitch(R.id.switch_isha, "notif_prayer_isha", true);

        txtEzanSoundValue = findViewById(R.id.txt_ezan_sound_value);
        txtPreMinutesValue = findViewById(R.id.txt_pre_minutes_value);
        txtPreCustomLabel = findViewById(R.id.txt_pre_custom_label);
        switchDnd = findViewById(R.id.switch_dnd);

        View rowEzanSound = findViewById(R.id.row_ezan_sound);
        if (rowEzanSound != null) {
            rowEzanSound.setOnClickListener(v -> showEzanSoundDialog());
        }
        View rowPreMinutes = findViewById(R.id.row_pre_minutes);
        if (rowPreMinutes != null) {
            rowPreMinutes.setOnClickListener(v -> showPreMinutesDialog());
        }

        setupDndSwitch();
        refreshSummaryTexts();

        btnTestEzan = findViewById(R.id.btn_test_ezan);
        updateTestButtonUI();
        btnTestEzan.setOnClickListener(v -> {
            if (EzanPlayerService.isPlaying()) {
                Intent stopIntent = new Intent(this, EzanPlayerService.class);
                stopIntent.setAction(EzanPlayerService.ACTION_STOP_EZAN);
                startService(stopIntent);
                btnTestEzan.postDelayed(this::updateTestButtonUI, 150);
            } else {
                Intent playIntent = new Intent(this, EzanPlayerService.class);
                playIntent.setAction(EzanPlayerService.ACTION_PLAY_EZAN);
                playIntent.putExtra(EzanPlayerService.EXTRA_PRAYER_NAME, getString(R.string.ogle));
                ContextCompat.startForegroundService(this, playIntent);
                btnTestEzan.postDelayed(this::updateTestButtonUI, 250);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTestButtonUI();
        refreshSummaryTexts();
        if (switchDnd != null) {
            boolean enabled = prefs.getBoolean(DndHelper.PREF_DND_ENABLED, false) && DndHelper.hasPolicyAccess(this);
            switchDnd.setChecked(enabled);
        }
    }

    private void updateTestButtonUI() {
        if (btnTestEzan == null) return;
        if (EzanPlayerService.isPlaying()) {
            btnTestEzan.setText(R.string.ezan_test_stop);
            btnTestEzan.setIconResource(R.drawable.ic_stop);
        } else {
            btnTestEzan.setText(R.string.ezan_test_play);
            btnTestEzan.setIconResource(R.drawable.ic_volume_up);
        }
    }

    private void bindSwitch(int viewId, String prefKey, boolean defaultValue) {
        MaterialSwitch sw = findViewById(viewId);
        if (sw == null) return;
        sw.setChecked(prefs.getBoolean(prefKey, defaultValue));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(prefKey, isChecked).apply();
            AlarmHelper.rescheduleAlarmsFromCache(this);
        });
    }

    private void showEzanSoundDialog() {
        String[] options = {
                getString(R.string.ezan_sound_classic),
                getString(R.string.ezan_sound_istanbul),
                getString(R.string.ezan_sound_makkah),
                getString(R.string.ezan_sound_short),
                getString(R.string.ezan_sound_silent)
        };
        int current = prefs.getInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, EzanPlayerService.SOUND_CLASSIC);
        new AlertDialog.Builder(this)
                .setTitle(R.string.notif_ezan_sound_title)
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    prefs.edit().putInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, which).apply();
                    refreshSummaryTexts();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showPreMinutesDialog() {
        final int[] values = {15, 30, 45, 60};
        String[] labels = new String[values.length];
        int currentMin = prefs.getInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, 45);
        int checkedIdx = 2;
        for (int i = 0; i < values.length; i++) {
            labels[i] = getString(R.string.notif_pre_minutes_value, values[i]);
            if (values[i] == currentMin) checkedIdx = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.notif_pre_minutes_title)
                .setSingleChoiceItems(labels, checkedIdx, (dialog, which) -> {
                    prefs.edit().putInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, values[which]).apply();
                    refreshSummaryTexts();
                    AlarmHelper.rescheduleAlarmsFromCache(this);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupDndSwitch() {
        if (switchDnd == null) return;
        boolean current = prefs.getBoolean(DndHelper.PREF_DND_ENABLED, false) && DndHelper.hasPolicyAccess(this);
        switchDnd.setChecked(current);
        switchDnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked && !DndHelper.hasPolicyAccess(this)) {
                buttonView.setChecked(false);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.notif_dnd_permission_title)
                        .setMessage(R.string.notif_dnd_permission_msg)
                        .setPositiveButton(R.string.notif_dnd_go_settings, (d, w) -> DndHelper.openPolicySettings(this))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return;
            }
            prefs.edit().putBoolean(DndHelper.PREF_DND_ENABLED, isChecked).apply();
            AlarmHelper.rescheduleAlarmsFromCache(this);
        });
    }

    private void refreshSummaryTexts() {
        if (txtEzanSoundValue != null) {
            String[] options = {
                    getString(R.string.ezan_sound_classic),
                    getString(R.string.ezan_sound_istanbul),
                    getString(R.string.ezan_sound_makkah),
                    getString(R.string.ezan_sound_short),
                    getString(R.string.ezan_sound_silent)
            };
            int idx = prefs.getInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, EzanPlayerService.SOUND_CLASSIC);
            if (idx < 0 || idx >= options.length) idx = 0;
            txtEzanSoundValue.setText(options[idx]);
        }
        int mins = prefs.getInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, 45);
        if (txtPreMinutesValue != null) {
            txtPreMinutesValue.setText(getString(R.string.notif_pre_minutes_value, mins));
        }
        if (txtPreCustomLabel != null) {
            txtPreCustomLabel.setText(getString(R.string.notif_pre_minutes_dynamic, mins));
        }
    }
}
