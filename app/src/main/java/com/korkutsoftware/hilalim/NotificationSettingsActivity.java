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

public class NotificationSettingsActivity extends BaseActivity {

    private SharedPreferences prefs;
    private MaterialButton btnTestEzan;

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
}
