package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.korkutsoftware.hilalim.util.PrayerStreakHelper;

public class KazaTakipActivity extends BaseActivity {

    private static final String PREFS_NAME = "hilalim_kaza_prefs";

    private static final String KEY_FAJR = "kaza_fajr";
    private static final String KEY_DHUHR = "kaza_dhuhr";
    private static final String KEY_ASR = "kaza_asr";
    private static final String KEY_MAGHRIB = "kaza_maghrib";
    private static final String KEY_ISHA = "kaza_isha";
    private static final String KEY_WITR = "kaza_witr";
    private static final String KEY_FASTING = "kaza_fasting";
    private static final String KEY_INITIAL_TOTAL = "kaza_initial_total";

    private SharedPreferences prefs;

    private TextView txtTotalSummary, txtProgressLabel;
    private ProgressBar progressBar;
    private MaterialButton btnCalcAction;

    private LinearLayout layoutNamazSection, layoutOrucSection;
    private TextView txtFastingCount;

    private View cardFajr, cardDhuhr, cardAsr, cardMaghrib, cardIsha, cardWitr;

    private boolean isFastingTab = false;

    // Undo state container
    private static class UndoState {
        int fajr, dhuhr, asr, maghrib, isha, witr, fasting;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kaza_takip);
        applySystemBarInsets(findViewById(android.R.id.content));

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        txtTotalSummary = findViewById(R.id.txt_kaza_total_summary);
        txtProgressLabel = findViewById(R.id.txt_kaza_progress_label);
        progressBar = findViewById(R.id.progress_kaza_bar);
        btnCalcAction = findViewById(R.id.btn_kaza_calc_action);

        layoutNamazSection = findViewById(R.id.layout_section_namaz);
        layoutOrucSection = findViewById(R.id.layout_section_oruc);
        txtFastingCount = findViewById(R.id.txt_fasting_count);

        cardFajr = findViewById(R.id.card_counter_fajr);
        cardDhuhr = findViewById(R.id.card_counter_dhuhr);
        cardAsr = findViewById(R.id.card_counter_asr);
        cardMaghrib = findViewById(R.id.card_counter_maghrib);
        cardIsha = findViewById(R.id.card_counter_isha);
        cardWitr = findViewById(R.id.card_counter_witr);

        setupPrayerCard(cardFajr, getString(R.string.imsak), KEY_FAJR);
        setupPrayerCard(cardDhuhr, getString(R.string.ogle), KEY_DHUHR);
        setupPrayerCard(cardAsr, getString(R.string.ikindi), KEY_ASR);
        setupPrayerCard(cardMaghrib, getString(R.string.aksam), KEY_MAGHRIB);
        setupPrayerCard(cardIsha, getString(R.string.yatsi), KEY_ISHA);
        setupPrayerCard(cardWitr, getString(R.string.vakit_vitir), KEY_WITR);

        setupFastingSection();

        MaterialButtonToggleGroup tabToggle = findViewById(R.id.toggle_kaza_tabs);
        tabToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            isFastingTab = (checkedId == R.id.btn_tab_oruc);
            layoutNamazSection.setVisibility(isFastingTab ? View.GONE : View.VISIBLE);
            layoutOrucSection.setVisibility(isFastingTab ? View.VISIBLE : View.GONE);
            updateUI();
        });

        findViewById(R.id.btn_quick_drop_day).setOnClickListener(v -> dropOneFullDay());
        btnCalcAction.setOnClickListener(v -> showCalculateDialog());

        updateUI();
    }

    private void setupPrayerCard(View card, String title, String key) {
        TextView txtTitle = card.findViewById(R.id.txt_kaza_name);
        TextView txtCount = card.findViewById(R.id.txt_kaza_count);
        MaterialButton btnDec = card.findViewById(R.id.btn_kaza_decrement);
        MaterialButton btnInc = card.findViewById(R.id.btn_kaza_increment);

        txtTitle.setText(title);

        btnDec.setOnClickListener(v -> {
            int current = prefs.getInt(key, 0);
            if (current > 0) {
                UndoState state = captureState();
                prefs.edit().putInt(key, current - 1).apply();
                vibrateLight();
                updateUI();
                showUndoSnackbar(getString(R.string.kaza_dropped_undo, title), state);
            }
        });

        btnInc.setOnClickListener(v -> {
            int current = prefs.getInt(key, 0);
            prefs.edit().putInt(key, current + 1).apply();
            vibrateLight();
            updateUI();
        });
    }

    private void setupFastingSection() {
        findViewById(R.id.btn_fasting_decrement).setOnClickListener(v -> {
            int current = prefs.getInt(KEY_FASTING, 0);
            if (current > 0) {
                UndoState state = captureState();
                prefs.edit().putInt(KEY_FASTING, current - 1).apply();
                vibrateLight();
                updateUI();
                showUndoSnackbar(getString(R.string.kaza_fasting_dropped_undo), state);
            }
        });

        findViewById(R.id.btn_fasting_increment).setOnClickListener(v -> {
            int current = prefs.getInt(KEY_FASTING, 0);
            prefs.edit().putInt(KEY_FASTING, current + 1).apply();
            vibrateLight();
            updateUI();
        });

        findViewById(R.id.btn_fasting_add_month).setOnClickListener(v -> {
            int current = prefs.getInt(KEY_FASTING, 0);
            prefs.edit().putInt(KEY_FASTING, current + 30).apply();
            vibrateLight();
            updateUI();
        });

        findViewById(R.id.btn_fasting_reset).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_kaza_reset)
                    .setMessage(R.string.kaza_fasting_reset_confirm)
                    .setPositiveButton(R.string.dialog_kaza_reset, (dialog, which) -> {
                        prefs.edit().putInt(KEY_FASTING, 0).apply();
                        updateUI();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void dropOneFullDay() {
        int fajr = prefs.getInt(KEY_FAJR, 0);
        int dhuhr = prefs.getInt(KEY_DHUHR, 0);
        int asr = prefs.getInt(KEY_ASR, 0);
        int maghrib = prefs.getInt(KEY_MAGHRIB, 0);
        int isha = prefs.getInt(KEY_ISHA, 0);
        int witr = prefs.getInt(KEY_WITR, 0);

        if (fajr == 0 && dhuhr == 0 && asr == 0 && maghrib == 0 && isha == 0 && witr == 0) {
            return;
        }

        UndoState state = captureState();

        prefs.edit()
                .putInt(KEY_FAJR, Math.max(0, fajr - 1))
                .putInt(KEY_DHUHR, Math.max(0, dhuhr - 1))
                .putInt(KEY_ASR, Math.max(0, asr - 1))
                .putInt(KEY_MAGHRIB, Math.max(0, maghrib - 1))
                .putInt(KEY_ISHA, Math.max(0, isha - 1))
                .putInt(KEY_WITR, Math.max(0, witr - 1))
                .apply();

        vibrateLight();
        updateUI();
        showUndoSnackbar(getString(R.string.kaza_day_dropped_undo), state);
    }

    private UndoState captureState() {
        UndoState state = new UndoState();
        state.fajr = prefs.getInt(KEY_FAJR, 0);
        state.dhuhr = prefs.getInt(KEY_DHUHR, 0);
        state.asr = prefs.getInt(KEY_ASR, 0);
        state.maghrib = prefs.getInt(KEY_MAGHRIB, 0);
        state.isha = prefs.getInt(KEY_ISHA, 0);
        state.witr = prefs.getInt(KEY_WITR, 0);
        state.fasting = prefs.getInt(KEY_FASTING, 0);
        return state;
    }

    private void restoreState(UndoState state) {
        if (state == null) return;
        prefs.edit()
                .putInt(KEY_FAJR, state.fajr)
                .putInt(KEY_DHUHR, state.dhuhr)
                .putInt(KEY_ASR, state.asr)
                .putInt(KEY_MAGHRIB, state.maghrib)
                .putInt(KEY_ISHA, state.isha)
                .putInt(KEY_WITR, state.witr)
                .putInt(KEY_FASTING, state.fasting)
                .apply();
        vibrateLight();
        updateUI();
    }

    private void showUndoSnackbar(String message, UndoState previousState) {
        View root = findViewById(R.id.coordinator_kaza);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> restoreState(previousState))
                    .show();
        }
    }

    private void showCalculateDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.dialog_kaza_years_hint);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_kaza_calc_title)
                .setMessage(R.string.dialog_kaza_calc_message)
                .setView(input)
                .setPositiveButton(R.string.dialog_kaza_apply, (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            int years = Integer.parseInt(text);
                            if (years > 0) {
                                int daysToAdd = years * 365;
                                int newFajr = prefs.getInt(KEY_FAJR, 0) + daysToAdd;
                                int newDhuhr = prefs.getInt(KEY_DHUHR, 0) + daysToAdd;
                                int newAsr = prefs.getInt(KEY_ASR, 0) + daysToAdd;
                                int newMaghrib = prefs.getInt(KEY_MAGHRIB, 0) + daysToAdd;
                                int newIsha = prefs.getInt(KEY_ISHA, 0) + daysToAdd;
                                int newWitr = prefs.getInt(KEY_WITR, 0) + daysToAdd;

                                int newTotal = PrayerStreakHelper.calculateTotalKazaBorcu(
                                        newFajr, newDhuhr, newAsr, newMaghrib, newIsha, newWitr
                                );

                                prefs.edit()
                                        .putInt(KEY_FAJR, newFajr)
                                        .putInt(KEY_DHUHR, newDhuhr)
                                        .putInt(KEY_ASR, newAsr)
                                        .putInt(KEY_MAGHRIB, newMaghrib)
                                        .putInt(KEY_ISHA, newIsha)
                                        .putInt(KEY_WITR, newWitr)
                                        .putInt(KEY_INITIAL_TOTAL, newTotal)
                                        .apply();

                                updateUI();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                })
                .setNeutralButton(R.string.dialog_kaza_reset, (dialog, which) -> {
                    prefs.edit()
                            .putInt(KEY_FAJR, 0)
                            .putInt(KEY_DHUHR, 0)
                            .putInt(KEY_ASR, 0)
                            .putInt(KEY_MAGHRIB, 0)
                            .putInt(KEY_ISHA, 0)
                            .putInt(KEY_WITR, 0)
                            .putInt(KEY_INITIAL_TOTAL, 0)
                            .apply();
                    updateUI();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateUI() {
        int fajr = prefs.getInt(KEY_FAJR, 0);
        int dhuhr = prefs.getInt(KEY_DHUHR, 0);
        int asr = prefs.getInt(KEY_ASR, 0);
        int maghrib = prefs.getInt(KEY_MAGHRIB, 0);
        int isha = prefs.getInt(KEY_ISHA, 0);
        int witr = prefs.getInt(KEY_WITR, 0);
        int fasting = prefs.getInt(KEY_FASTING, 0);
        int initialTotal = prefs.getInt(KEY_INITIAL_TOTAL, 0);

        updateCardCount(cardFajr, fajr);
        updateCardCount(cardDhuhr, dhuhr);
        updateCardCount(cardAsr, asr);
        updateCardCount(cardMaghrib, maghrib);
        updateCardCount(cardIsha, isha);
        updateCardCount(cardWitr, witr);

        txtFastingCount.setText(String.valueOf(fasting));

        if (!isFastingTab) {
            int totalNamaz = PrayerStreakHelper.calculateTotalKazaBorcu(fajr, dhuhr, asr, maghrib, isha, witr);
            txtTotalSummary.setText(getString(R.string.kaza_total_debt_format, totalNamaz));

            int progress = PrayerStreakHelper.calculateKazaProgress(totalNamaz, initialTotal);
            progressBar.setProgress(progress);
            txtProgressLabel.setText(getString(R.string.kaza_progress_format, progress));
            btnCalcAction.setVisibility(View.VISIBLE);
        } else {
            txtTotalSummary.setText(getString(R.string.kaza_total_fasting_format, fasting));
            progressBar.setProgress(fasting > 0 ? 50 : 100);
            txtProgressLabel.setText(fasting == 0 ? "%100 Tamamlandı" : "Kaza borcu mevcut");
            btnCalcAction.setVisibility(View.GONE);
        }
    }

    private void updateCardCount(View card, int count) {
        if (card != null) {
            TextView txtCount = card.findViewById(R.id.txt_kaza_count);
            if (txtCount != null) {
                txtCount.setText(String.valueOf(count));
            }
        }
    }

    private void vibrateLight() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(30);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
